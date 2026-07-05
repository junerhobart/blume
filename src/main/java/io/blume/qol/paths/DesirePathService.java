package io.blume.qol.paths;

import io.blume.qol.QolConfig;
import io.blume.qol.QolKeys;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class DesirePathService {

    private static final int STAGE_GRASS = 0;
    private static final int STAGE_TRAMPLED = 1;
    private static final int STAGE_WORN = 2;

    private final QolConfig config;
    private final QolKeys keys;

    public DesirePathService(@NotNull QolConfig config, @NotNull QolKeys keys) {
        this.config = config;
        this.keys = keys;
    }

    public void recordWalk(@NotNull Block groundBlock) {
        Material type = groundBlock.getType();
        if (type == Material.DIRT_PATH) {
            return;
        }

        PathState state = readState(groundBlock);
        if (state == null) {
            if (type != Material.GRASS_BLOCK) {
                return;
            }
            state = new PathState(STAGE_GRASS, 0);
        } else if (state.stage() == STAGE_WORN && !isWornMaterial(type)) {
            return;
        }

        int walks = state.walks() + 1;
        int stage = state.stage();

        if (stage == STAGE_GRASS) {
            if (walks >= config.walksToTrample()) {
                trampleVegetation(groundBlock);
                writeState(groundBlock, STAGE_TRAMPLED, 0);
                return;
            }
            writeState(groundBlock, STAGE_GRASS, walks);
            return;
        }

        if (stage == STAGE_TRAMPLED) {
            if (walks >= config.walksToWorn()) {
                Material worn = pickWornMaterial();
                groundBlock.setType(worn, false);
                writeState(groundBlock, STAGE_WORN, 0);
                return;
            }
            writeState(groundBlock, STAGE_TRAMPLED, walks);
            return;
        }

        if (stage == STAGE_WORN && walks >= config.walksToPath()) {
            groundBlock.setType(Material.DIRT_PATH, false);
            clearState(groundBlock);
        } else if (stage == STAGE_WORN) {
            writeState(groundBlock, STAGE_WORN, walks);
        }
    }

    private void trampleVegetation(Block grassBlock) {
        Block above = grassBlock.getRelative(0, 1, 0);
        Material type = above.getType();
        if (type.isAir()) {
            return;
        }
        if (type == Material.SHORT_GRASS
            || type == Material.TALL_GRASS
            || type == Material.FERN
            || type == Material.LARGE_FERN
            || type == Material.DEAD_BUSH
            || Tag.FLOWERS.isTagged(type)) {
            above.setType(Material.AIR, false);
        }
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

    @org.jetbrains.annotations.Nullable
    private PathState readState(Block block) {
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        Long packed = pdc.get(keys.desirePathBlock(block.getX(), block.getY(), block.getZ()), PersistentDataType.LONG);
        if (packed == null) {
            return null;
        }
        int stage = (int) (packed >> 32);
        int walks = (int) (packed & 0xFFFFFFFFL);
        return new PathState(stage, walks);
    }

    private void writeState(Block block, int stage, int walks) {
        block.getChunk().getPersistentDataContainer().set(
            keys.desirePathBlock(block.getX(), block.getY(), block.getZ()),
            PersistentDataType.LONG,
            ((long) stage << 32) | (walks & 0xFFFFFFFFL)
        );
    }

    private void clearState(Block block) {
        block.getChunk().getPersistentDataContainer().remove(
            keys.desirePathBlock(block.getX(), block.getY(), block.getZ())
        );
    }

    private record PathState(int stage, int walks) {}
}
