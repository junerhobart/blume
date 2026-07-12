package io.blume.ecology.pollination;

import io.blume.BlumePlugin;
import io.blume.ecology.EcologyConfig;
import io.blume.ecology.RandomTickPace;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Bee;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class BeePollinationListener extends BukkitRunnable {

    private static final int CROP_RADIUS = 3;
    private static final int FLOWER_RADIUS = 2;
    private static final int MAX_CROPS_PER_POLLINATION = 10;
    private static final double BEES_PER_SECOND_AT_VANILLA = 2.0 / 60.0;
    private static final BlockFace[] ADJACENT = {
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
        BlockFace.UP, BlockFace.DOWN
    };
    private static final Set<Material> FLOWER_SOILS = Set.of(
        Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
        Material.PODZOL, Material.MOSS_BLOCK, Material.MYCELIUM
    );

    private final BlumePlugin plugin;
    private final EcologyConfig config;

    public BeePollinationListener(@NotNull BlumePlugin plugin, @NotNull EcologyConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        runTaskTimer(plugin, 40L, 1L);
    }

    @Override
    public void run() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (World world : Bukkit.getWorlds()) {
            if (!RandomTickPace.isActive(world)) {
                continue;
            }
            // Roll first: samples is 0 on almost every tick, and the entity
            // scan is by far the most expensive part of this task.
            int samples = RandomTickPace.rollSamplesPerTick(BEES_PER_SECOND_AT_VANILLA, world, random);
            if (samples <= 0) {
                continue;
            }
            List<Bee> nectarBees = collectNectarBees(world);
            if (nectarBees.isEmpty()) {
                continue;
            }
            for (int i = 0; i < samples; i++) {
                Bee bee = nectarBees.get(random.nextInt(nectarBees.size()));
                pollinateCrop(bee);
                spreadFlower(bee, random);
            }
        }
    }

    private @NotNull List<Bee> collectNectarBees(@NotNull World world) {
        List<Bee> bees = new ArrayList<>();
        for (Bee bee : world.getEntitiesByClass(Bee.class)) {
            if (bee.hasNectar()) {
                bees.add(bee);
            }
        }
        return bees;
    }

    private void pollinateCrop(@NotNull Bee bee) {
        if (bee.getCropsGrownSincePollination() >= MAX_CROPS_PER_POLLINATION) {
            return;
        }
        Block crop = findNearestImmatureCrop(bee);
        if (crop == null) {
            return;
        }
        if (crop.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) {
                ageable.setAge(ageable.getAge() + 1);
                crop.setBlockData(ageable, false);
                bee.setCropsGrownSincePollination(bee.getCropsGrownSincePollination() + 1);
            }
        }
    }

    private @Nullable Block findNearestImmatureCrop(@NotNull Bee bee) {
        Block beeBlock = bee.getLocation().getBlock();
        Block nearest = null;
        double nearestDist = Double.MAX_VALUE;
        int r = CROP_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block block = beeBlock.getRelative(dx, dy, dz);
                    if (!(block.getBlockData() instanceof Ageable ageable)) {
                        continue;
                    }
                    if (ageable.getAge() >= ageable.getMaximumAge()) {
                        continue;
                    }
                    double dist = block.getLocation().distanceSquared(bee.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = block;
                    }
                }
            }
        }
        return nearest;
    }

    private void spreadFlower(@NotNull Bee bee, @NotNull ThreadLocalRandom random) {
        if (random.nextDouble() >= config.pollinationFlowerSpreadChance()) {
            return;
        }
        Block flower = findNearbyFlower(bee);
        if (flower == null) {
            return;
        }
        BlockFace face = ADJACENT[random.nextInt(ADJACENT.length)];
        Block soil = flower.getRelative(face);
        if (!isFlowerSoil(soil)) {
            return;
        }
        Block above = soil.getRelative(BlockFace.UP);
        if (!above.isEmpty()) {
            return;
        }
        Material newFlower = randomFlower(random);
        if (newFlower == null) {
            return;
        }
        placeFlower(above, newFlower);
    }

    private @Nullable Block findNearbyFlower(@NotNull Bee bee) {
        Block beeBlock = bee.getLocation().getBlock();
        List<Block> flowers = new ArrayList<>();
        int r = FLOWER_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block block = beeBlock.getRelative(dx, dy, dz);
                    if (Tag.FLOWERS.isTagged(block.getType())) {
                        flowers.add(block);
                    }
                }
            }
        }
        if (flowers.isEmpty()) {
            return null;
        }
        return flowers.get(ThreadLocalRandom.current().nextInt(flowers.size()));
    }

    private boolean isFlowerSoil(@NotNull Block block) {
        return FLOWER_SOILS.contains(block.getType());
    }

    private @Nullable Material randomFlower(@NotNull ThreadLocalRandom random) {
        List<Material> flowers = new ArrayList<>(Tag.FLOWERS.getValues());
        if (flowers.isEmpty()) {
            return null;
        }
        return flowers.get(random.nextInt(flowers.size()));
    }

    private void placeFlower(@NotNull Block block, @NotNull Material flower) {
        BlockData data = flower.createBlockData();
        block.setBlockData(data, false);
        if (data instanceof Bisected) {
            Block top = block.getRelative(BlockFace.UP);
            if (!top.isEmpty()) {
                block.setType(Material.AIR, false);
                return;
            }
            BlockData upper = flower.createBlockData();
            if (upper instanceof Bisected bisected) {
                bisected.setHalf(Bisected.Half.TOP);
            }
            top.setBlockData(upper, false);
        }
    }
}
