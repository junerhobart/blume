package io.blume.ecology.grass;

import io.blume.BlumePlugin;
import io.blume.ecology.EcologyConfig;
import io.blume.ecology.RandomTickSampler;
import io.blume.ecology.SoilSurface;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class NaturalGrassTask extends BukkitRunnable {

    private static final Set<Material> CROP_BLOCKS = Set.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS
    );
    private static final BlockFace[] HORIZONTAL = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    private static final int MIN_SKY_LIGHT = 4;

    private final BlumePlugin plugin;
    private final EcologyConfig config;
    private final RandomTickSampler sampler;

    public NaturalGrassTask(
        @NotNull BlumePlugin plugin,
        @NotNull EcologyConfig config,
        @NotNull RandomTickSampler sampler
    ) {
        this.plugin = plugin;
        this.config = config;
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
                tryGrowAt(world, block.getX(), block.getZ(), block.getY(), random));
        }
    }

    private void tryGrowAt(@NotNull World world, int x, int z, int hintY, @NotNull ThreadLocalRandom random) {
        SoilSurface.Spot spot = SoilSurface.find(world, x, z, hintY);
        if (spot == null || !SoilSurface.touchesGround(spot)) {
            Block surface = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (config.isNaturalGrassInvadeFarmland() && isMatureCrop(surface)) {
                tryInvadeFarmland(surface, random);
            }
            return;
        }

        if (Math.max(spot.ground().getLightFromSky(), spot.plantBlock().getLightFromSky()) < MIN_SKY_LIGHT) {
            return;
        }

        if (!SoilSurface.isShortGrass(spot.plantBlock())) {
            if (random.nextDouble() < config.naturalGrassShortGrassChance()) {
                spot.plantBlock().setType(Material.SHORT_GRASS, true);
            }
            return;
        }

        Block above = spot.plantBlock().getRelative(BlockFace.UP);
        if (above.isEmpty() && random.nextDouble() < config.naturalGrassTallGrassChance()) {
            growTallGrass(spot.plantBlock());
        }
    }

    private void growTallGrass(@NotNull Block bottom) {
        Block top = bottom.getRelative(BlockFace.UP);
        if (!top.isEmpty()) {
            return;
        }
        BlockData lower = Material.TALL_GRASS.createBlockData();
        if (lower instanceof Bisected bisected) {
            bisected.setHalf(Bisected.Half.BOTTOM);
        }
        bottom.setBlockData(lower, true);
        BlockData upper = Material.TALL_GRASS.createBlockData();
        if (upper instanceof Bisected bisected) {
            bisected.setHalf(Bisected.Half.TOP);
        }
        top.setBlockData(upper, true);
    }

    private void tryInvadeFarmland(@NotNull Block crop, @NotNull ThreadLocalRandom random) {
        if (random.nextDouble() >= config.naturalGrassInvadeFarmlandChance()) {
            return;
        }
        if (crop.getRelative(BlockFace.DOWN).getType() != Material.FARMLAND) {
            return;
        }
        for (BlockFace face : HORIZONTAL) {
            if (SoilSurface.isGround(crop.getRelative(face).getType())) {
                crop.setType(Material.SHORT_GRASS, true);
                break;
            }
        }
    }

    private boolean isMatureCrop(@NotNull Block block) {
        if (!CROP_BLOCKS.contains(block.getType())) {
            return false;
        }
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }
}
