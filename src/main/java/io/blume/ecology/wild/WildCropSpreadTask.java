package io.blume.ecology.wild;

import io.blume.BlumePlugin;
import io.blume.ecology.EcologyConfig;
import io.blume.ecology.EcologyKeys;
import io.blume.ecology.RandomTickSampler;
import io.blume.ecology.SoilSurface;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class WildCropSpreadTask extends BukkitRunnable {

    private static final Set<Material> CROP_BLOCKS = Set.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS
    );
    private static final Set<Material> VALID_GROUND = Set.of(
        Material.FARMLAND, Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
        Material.ROOTED_DIRT, Material.PODZOL, Material.MYCELIUM
    );
    private static final Set<Material> REPLACEABLE_PLANTS = Set.of(
        Material.SHORT_GRASS, Material.FERN, Material.DEAD_BUSH
    );
    private static final BlockFace[] HORIZONTAL = {
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    };
    private static final int CROP_GROW_DENOMINATOR = 25;
    private static final int MIN_SKY_LIGHT = 4;

    private final BlumePlugin plugin;
    private final EcologyConfig config;
    private final EcologyKeys keys;
    private final WildCropPopulator populator;
    private final RandomTickSampler sampler;

    public WildCropSpreadTask(
        @NotNull BlumePlugin plugin,
        @NotNull EcologyConfig config,
        @NotNull EcologyKeys keys,
        @NotNull WildCropPopulator populator,
        @NotNull RandomTickSampler sampler
    ) {
        this.plugin = plugin;
        this.config = config;
        this.keys = keys;
        this.populator = populator;
        this.sampler = sampler;
    }

    public void start() {
        runTaskTimer(plugin, 40L, 1L);
    }

    @Override
    public void run() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (World world : Bukkit.getWorlds()) {
            sampler.sampleBlocks(world, block ->
                tryTickAt(world, block.getX(), block.getZ(), block.getY(), random));
        }
    }

    private void tryTickAt(
        @NotNull World world,
        int x,
        int z,
        int hintY,
        @NotNull ThreadLocalRandom random
    ) {
        SoilSurface.Spot spot = SoilSurface.find(world, x, z, hintY);
        if (spot == null || !SoilSurface.touchesGround(spot)) {
            return;
        }
        Block crop = spot.plantBlock();
        if (!isWildCrop(crop) || !CROP_BLOCKS.contains(crop.getType())) {
            return;
        }
        if (!hasEnoughLight(spot)) {
            return;
        }
        if (!isMature(crop)) {
            tryGrow(crop, random);
            return;
        }
        if (random.nextDouble() >= config.wildCropsSpreadChance()) {
            return;
        }

        BlockFace face = HORIZONTAL[random.nextInt(HORIZONTAL.length)];
        int targetX = crop.getX() + face.getModX();
        int targetZ = crop.getZ() + face.getModZ();
        SoilSurface.Spot target = SoilSurface.find(world, targetX, targetZ, crop.getY());
        if (target == null || !SoilSurface.touchesGround(target)) {
            return;
        }
        if (!canSpreadTo(target, random)) {
            return;
        }

        placeSpreadCrop(target, crop.getType());
    }

    private void tryGrow(@NotNull Block block, @NotNull ThreadLocalRandom random) {
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return;
        }
        if (ageable.getAge() >= ageable.getMaximumAge()) {
            return;
        }
        if (random.nextInt(CROP_GROW_DENOMINATOR) != 0) {
            return;
        }
        ageable.setAge(ageable.getAge() + 1);
        block.setBlockData(ageable, false);
    }

    private boolean isWildCrop(@NotNull Block block) {
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        return pdc.has(keys.wildCrop(block), PersistentDataType.STRING);
    }

    private boolean isMature(@NotNull Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private boolean canSpreadTo(@NotNull SoilSurface.Spot spot, @NotNull ThreadLocalRandom random) {
        Block ground = spot.ground();
        if (!VALID_GROUND.contains(ground.getType())) {
            return false;
        }
        if (config.isWildBiomeFilter() && !populator.biomeAllowed(spot.plantBlock())) {
            return false;
        }

        Chunk chunk = spot.plantBlock().getChunk();
        if (populator.countWildCrops(chunk) >= config.wildMaxPerChunk()) {
            return false;
        }
        if (!hasEnoughLight(spot)) {
            return false;
        }

        Block plant = spot.plantBlock();
        Material plantType = plant.getType();
        if (CROP_BLOCKS.contains(plantType)) {
            return false;
        }
        if (plant.isEmpty()) {
            return true;
        }
        if (REPLACEABLE_PLANTS.contains(plantType)) {
            return random.nextDouble() < config.wildCropsSpreadIntoGrassChance();
        }
        return false;
    }

    private boolean hasEnoughLight(@NotNull SoilSurface.Spot spot) {
        int plantLight = spot.plantBlock().getLightFromSky();
        int groundLight = spot.ground().getLightFromSky();
        return Math.max(plantLight, groundLight) >= MIN_SKY_LIGHT;
    }

    private void placeSpreadCrop(@NotNull SoilSurface.Spot spot, @NotNull Material crop) {
        SoilSurface.clearTallGrassTop(spot);
        Block block = spot.plantBlock();
        block.setType(crop, false);
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable, false);
        }
        populator.markWild(block);
        populator.incrementCount(block.getChunk());
    }
}
