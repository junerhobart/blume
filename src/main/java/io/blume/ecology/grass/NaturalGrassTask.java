package io.blume.ecology.grass;

import io.blume.BlumePlugin;
import io.blume.ecology.EcologyConfig;
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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class NaturalGrassTask extends BukkitRunnable {

    private static final Set<Material> CROP_BLOCKS = Set.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS
    );
    private static final BlockFace[] HORIZONTAL = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    private static final int SAMPLES_PER_WORLD = 96;
    private static final int MIN_SKY_LIGHT = 4;

    private final BlumePlugin plugin;
    private final EcologyConfig config;

    public NaturalGrassTask(@NotNull BlumePlugin plugin, @NotNull EcologyConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        long interval = 20L * Math.max(1, config.naturalGrassTickIntervalSeconds());
        runTaskTimer(plugin, 40L, interval);
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getPlayers().isEmpty() && world.getLoadedChunks().length == 0) {
                continue;
            }
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int samples = Math.min(SAMPLES_PER_WORLD, Math.max(16, world.getPlayers().size() * 24));
            for (Player player : world.getPlayers()) {
                int perPlayer = Math.max(1, samples / world.getPlayers().size());
                for (int i = 0; i < perPlayer; i++) {
                    int x = player.getLocation().getBlockX() + random.nextInt(25) - 12;
                    int z = player.getLocation().getBlockZ() + random.nextInt(25) - 12;
                    tryGrowAt(world, x, z, player.getLocation().getBlockY(), random);
                }
            }
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
