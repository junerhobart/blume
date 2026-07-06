package io.blume.ecology.wild;

import io.blume.ecology.EcologyConfig;
import io.blume.ecology.EcologyKeys;
import io.blume.ecology.SoilSurface;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class WildCropPopulator {

    private static final List<Material> CROP_TYPES = List.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS
    );
    private static final int MIN_SKY_LIGHT = 4;
    private static final int PATCH_MIN = 3;
    private static final int PATCH_MAX = 7;
    private static final BlockFace[] HORIZONTAL = {
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    };
    private static final String WILD_CROP_PREFIX = "wild_crop_";

    private final EcologyConfig config;
    private final EcologyKeys keys;

    public WildCropPopulator(@NotNull EcologyConfig config, @NotNull EcologyKeys keys) {
        this.config = config;
        this.keys = keys;
    }

    public void populateIfNeeded(@NotNull Chunk chunk) {
        if (isPopulated(chunk)) {
            return;
        }
        populate(chunk);
        markPopulated(chunk);
    }

    private void populate(@NotNull Chunk chunk) {
        World world = chunk.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < config.wildPatchesPerChunk(); i++) {
            if (random.nextDouble() >= config.wildSpawnChance()) {
                continue;
            }
            int x = (chunk.getX() << 4) + random.nextInt(16);
            int z = (chunk.getZ() << 4) + random.nextInt(16);
            SoilSurface.Spot center = SoilSurface.find(world, x, z);
            if (center == null || !SoilSurface.touchesGround(center)) {
                continue;
            }
            if (config.isWildBiomeFilter() && !biomeAllowed(center.plantBlock())) {
                continue;
            }
            if (!hasEnoughLight(center)) {
                continue;
            }

            Material crop = CROP_TYPES.get(random.nextInt(CROP_TYPES.size()));
            spawnPatch(world, center, crop, random);
        }
    }

    private void spawnPatch(
        @NotNull World world,
        @NotNull SoilSurface.Spot center,
        @NotNull Material crop,
        @NotNull ThreadLocalRandom random
    ) {
        int target = random.nextInt(PATCH_MIN, PATCH_MAX + 1);
        Deque<SoilSurface.Spot> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(center);
        int placed = 0;

        while (!queue.isEmpty() && placed < target) {
            SoilSurface.Spot spot = queue.removeFirst();
            String key = spotKey(spot);
            if (!visited.add(key)) {
                continue;
            }
            if (!canPlaceAt(spot)) {
                continue;
            }

            placeWildCrop(spot, crop, random);
            placed++;

            Block ground = spot.ground();
            for (BlockFace face : HORIZONTAL) {
                Block neighborGround = ground.getRelative(face);
                SoilSurface.Spot neighbor = SoilSurface.find(
                    world,
                    neighborGround.getX(),
                    neighborGround.getZ(),
                    neighborGround.getY()
                );
                if (neighbor != null && neighbor.ground().equals(neighborGround)) {
                    queue.addLast(neighbor);
                }
            }
        }
    }

    private boolean canPlaceAt(@NotNull SoilSurface.Spot spot) {
        Block plantBlock = spot.plantBlock();
        if (SoilSurface.isCropBlock(plantBlock.getType())) {
            return false;
        }
        Chunk chunk = plantBlock.getChunk();
        if (countWildCrops(chunk) >= config.wildMaxPerChunk()) {
            return false;
        }
        if (config.isWildBiomeFilter() && !biomeAllowed(plantBlock)) {
            return false;
        }
        return hasEnoughLight(spot);
    }

    private boolean isPopulated(@NotNull Chunk chunk) {
        return chunk.getPersistentDataContainer().has(keys.wildCropsPopulated(), PersistentDataType.BYTE);
    }

    private void markPopulated(@NotNull Chunk chunk) {
        chunk.getPersistentDataContainer().set(keys.wildCropsPopulated(), PersistentDataType.BYTE, (byte) 1);
    }

    private static @NotNull String spotKey(@NotNull SoilSurface.Spot spot) {
        Block plant = spot.plantBlock();
        return plant.getX() + ":" + plant.getY() + ":" + plant.getZ();
    }

    private boolean hasEnoughLight(@NotNull SoilSurface.Spot spot) {
        int plantLight = spot.plantBlock().getLightFromSky();
        int groundLight = spot.ground().getLightFromSky();
        return Math.max(plantLight, groundLight) >= MIN_SKY_LIGHT;
    }

    private boolean biomeAllowed(@NotNull Block block) {
        String biome = block.getBiome().getKey().getKey().toLowerCase();
        for (String allowed : config.wildBiomeAllowlist()) {
            if (biome.contains(allowed)) {
                return true;
            }
        }
        return false;
    }

    private void placeWildCrop(@NotNull SoilSurface.Spot spot, @NotNull Material crop, @NotNull ThreadLocalRandom random) {
        SoilSurface.clearTallGrassTop(spot);
        Block block = spot.plantBlock();
        markWild(block);
        block.setType(crop, false);
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(randomWildAge(ageable, random));
            block.setBlockData(ageable, false);
        }
        incrementCount(block.getChunk());
    }

    private int randomWildAge(@NotNull Ageable ageable, @NotNull ThreadLocalRandom random) {
        return random.nextInt(ageable.getMaximumAge() + 1);
    }

    private void markWild(@NotNull Block block) {
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        pdc.set(keys.wildCrop(block), PersistentDataType.STRING, "true");
        pdc.set(keys.cropOrigin(block), PersistentDataType.STRING, "wild");
    }

    private int countWildCrops(@NotNull Chunk chunk) {
        Integer count = chunk.getPersistentDataContainer().get(keys.wildCropCount(), PersistentDataType.INTEGER);
        return count == null ? 0 : count;
    }

    private void incrementCount(@NotNull Chunk chunk) {
        chunk.getPersistentDataContainer().set(keys.wildCropCount(), PersistentDataType.INTEGER, countWildCrops(chunk) + 1);
    }

    public void reconcileChunk(@NotNull Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String namespace = keys.wildCropCount().getNamespace();
        int live = 0;

        for (NamespacedKey key : new ArrayList<>(pdc.getKeys())) {
            if (!namespace.equals(key.getNamespace()) || !key.getKey().startsWith(WILD_CROP_PREFIX)) {
                continue;
            }
            if (key.getKey().equals("wild_crop_count")) {
                continue;
            }

            Block block = blockFromKey(chunk.getWorld(), key.getKey());
            if (block == null || !SoilSurface.isCropBlock(block.getType())) {
                if (block != null) {
                    removeWildMarkers(pdc, block);
                } else {
                    pdc.remove(key);
                }
                continue;
            }

            if (pdc.has(keys.wildCrop(block), PersistentDataType.STRING)) {
                live++;
            } else {
                pdc.remove(key);
            }
        }

        pdc.set(keys.wildCropCount(), PersistentDataType.INTEGER, live);
    }

    private void removeWildMarkers(@NotNull PersistentDataContainer pdc, @NotNull Block block) {
        pdc.remove(keys.wildCrop(block));
        pdc.remove(keys.cropOrigin(block));
        pdc.remove(keys.pollinationCooldown(block));
        pdc.remove(keys.lastSpread(block));
    }

    private @Nullable Block blockFromKey(@NotNull World world, @NotNull String keyName) {
        if (!keyName.startsWith(WILD_CROP_PREFIX)) {
            return null;
        }
        String[] parts = keyName.substring(WILD_CROP_PREFIX.length()).split("_");
        if (parts.length != 3) {
            return null;
        }
        try {
            return world.getBlockAt(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
