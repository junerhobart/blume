package io.blume.qol.paths;

import io.blume.BlumePlugin;
import io.blume.ecology.EcologyKeys;
import io.blume.qol.QolConfig;
import io.blume.qol.QolKeys;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class DesirePathService {

    private static final Set<Material> TRAMPLE_VEGETATION = Set.of(
        Material.SHORT_GRASS,
        Material.TALL_GRASS,
        Material.FERN,
        Material.LARGE_FERN,
        Material.DEAD_BUSH,
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS
    );

    private final QolConfig config;
    private final QolKeys keys;
    private final EcologyKeys ecologyKeys;

    public DesirePathService(@NotNull BlumePlugin plugin, @NotNull QolConfig config, @NotNull QolKeys keys) {
        this.config = config;
        this.keys = keys;
        this.ecologyKeys = new EcologyKeys(plugin);
    }

    public void recordWalk(@NotNull Block groundBlock) {
        Material type = groundBlock.getType();
        if (type == Material.DIRT_PATH) {
            return;
        }

        long now = groundBlock.getWorld().getFullTime();
        DesirePathEngine.DesirePathRules rules = DesirePathEngine.rulesFrom(config);

        DesirePathEngine.PathState state = readState(groundBlock);
        if (state == null) {
            if (type != Material.GRASS_BLOCK) {
                return;
            }
            state = DesirePathEngine.PathState.freshGrass();
        } else if (state.stage() == DesirePathEngine.STAGE_WORN && type == Material.GRASS_BLOCK) {
            state = new DesirePathEngine.PathState(
                DesirePathEngine.STAGE_TRAMPLED,
                state.walks(),
                state.lastTouchedTick()
            );
        } else if (state.stage() == DesirePathEngine.STAGE_WORN && !isWornMaterial(type)) {
            return;
        }

        DesirePathEngine.Outcome outcome = DesirePathEngine.recordWalk(state, rules, now);
        applyOutcome(groundBlock, outcome);
    }

    public void decayChunk(@NotNull Chunk chunk) {
        if (!config.isDesirePathDeErosionEnabled()) {
            return;
        }

        long now = chunk.getWorld().getFullTime();
        DesirePathEngine.DesirePathRules rules = DesirePathEngine.rulesFrom(config);
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        for (NamespacedKey key : pdc.getKeys()) {
            if (!keys.isDesirePathBlockKey(key)) {
                continue;
            }
            int[] coords = keys.parseDesirePathCoords(key);
            if (coords == null) {
                continue;
            }

            Long packed = pdc.get(key, PersistentDataType.LONG);
            if (packed == null) {
                continue;
            }

            DesirePathEngine.PathState state = DesirePathEngine.unpack(packed);
            if (state == null) {
                continue;
            }

            Block block = chunk.getWorld().getBlockAt(coords[0], coords[1], coords[2]);
            DesirePathEngine.Outcome outcome = DesirePathEngine.applyDeErosion(state, rules, now);
            applyOutcome(block, outcome);
        }
    }

    void applyOutcome(@NotNull Block block, @NotNull DesirePathEngine.Outcome outcome) {
        switch (outcome.effect()) {
            case TRAMPLE_VEGETATION -> {
                writeState(block, outcome.state());
                trampleVegetation(block);
            }
            case BECOME_WORN -> {
                writeState(block, outcome.state());
                block.setType(pickWornMaterial(), false);
            }
            case BECOME_PATH -> {
                clearState(block);
                block.setType(Material.DIRT_PATH, false);
            }
            case REGRESS_TO_TRAMPLED -> {
                writeState(block, outcome.state());
                block.setType(Material.GRASS_BLOCK, false);
            }
            case REGRESS_TO_GRASS -> {
                writeState(block, outcome.state());
                block.setType(Material.GRASS_BLOCK, false);
            }
            case NONE -> {
                if (outcome.state() != null) {
                    writeState(block, outcome.state());
                }
            }
        }
    }

    private void trampleVegetation(Block grassBlock) {
        Block above = grassBlock.getRelative(0, 1, 0);
        Material type = above.getType();
        if (type.isAir()) {
            return;
        }
        if (TRAMPLE_VEGETATION.contains(type) || Tag.FLOWERS.isTagged(type)) {
            clearWildCropMetadata(above);
            above.setType(Material.AIR, false);
        }
    }

    private void clearWildCropMetadata(@NotNull Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (!pdc.has(ecologyKeys.wildCrop(block), PersistentDataType.STRING)) {
            return;
        }
        Integer count = pdc.get(ecologyKeys.wildCropCount(), PersistentDataType.INTEGER);
        if (count != null && count > 0) {
            pdc.set(ecologyKeys.wildCropCount(), PersistentDataType.INTEGER, count - 1);
        }
        pdc.remove(ecologyKeys.wildCrop(block));
        pdc.remove(ecologyKeys.cropOrigin(block));
    }

    private Material pickWornMaterial() {
        Map<Material, Integer> weights = config.wornWeights();
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return Material.DIRT;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (Map.Entry<Material, Integer> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }
        return Material.DIRT;
    }

    private static boolean isWornMaterial(Material type) {
        return type == Material.DIRT || type == Material.COARSE_DIRT || type == Material.GRAVEL;
    }

    @Nullable
    private DesirePathEngine.PathState readState(Block block) {
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        Long packed = pdc.get(keys.desirePathBlock(block.getX(), block.getY(), block.getZ()), PersistentDataType.LONG);
        if (packed == null) {
            return null;
        }
        return DesirePathEngine.unpack(packed);
    }

    private void writeState(Block block, @NotNull DesirePathEngine.PathState state) {
        block.getChunk().getPersistentDataContainer().set(
            keys.desirePathBlock(block.getX(), block.getY(), block.getZ()),
            PersistentDataType.LONG,
            DesirePathEngine.pack(state)
        );
    }

    private void clearState(Block block) {
        block.getChunk().getPersistentDataContainer().remove(
            keys.desirePathBlock(block.getX(), block.getY(), block.getZ())
        );
    }
}
