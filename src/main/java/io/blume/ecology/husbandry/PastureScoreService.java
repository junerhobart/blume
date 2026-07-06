package io.blume.ecology.husbandry;

import io.blume.BlumePlugin;
import io.blume.ecology.EcologyConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scores pastures for ethical husbandry bonuses.
 *
 * <p>One 2D flood-fill per unique pasture, shared by every animal inside it.
 * Scan radius is horizontal only; Y can change by one block per step so ramps and
 * small hills stay connected. Hard cap of 4096 visited cells keeps the worst case
 * bounded and predictable.</p>
 */
public final class PastureScoreService {

    private static final int CACHE_TTL_SECONDS = 30;
    private static final int HARD_CELL_CAP = 4096;
    private static final int STAGGER_TICKS = 20;
    private static final int CLEANUP_INTERVAL_TICKS = 200;
    private static final int ENTITY_SEARCH_PADDING_Y = 4;

    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 1, -1};

    private final EcologyConfig config;
    private final Map<RegionKey, PastureCacheEntry> cache = new ConcurrentHashMap<>();
    private final BukkitTask cleanupTask;

    public PastureScoreService(@NotNull BlumePlugin plugin, @NotNull EcologyConfig config) {
        this.config = config;
        this.cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskTimer(plugin, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);
    }

    /**
     * Returns the pasture score for the animal, using a cached shared scan when available.
     */
    public @NotNull PastureScore score(@NotNull LivingEntity animal) {
        if (!isHusbandryAnimal(animal.getType())) {
            return PastureScore.fallback();
        }

        World world = animal.getWorld();
        Location loc = animal.getLocation();
        FloodResult region = floodFill(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (region.cells.isEmpty()) {
            return PastureScore.fallback();
        }

        RegionKey key = new RegionKey(world.getName(), region.minX, region.minZ, region.maxX, region.maxZ);
        long now = System.currentTimeMillis();
        PastureCacheEntry entry = cache.get(key);
        if (entry == null || isExpired(entry, now)) {
            entry = buildEntry(animal, region, now);
            cache.put(key, entry);
        }
        return scoreFor(animal, entry);
    }

    /**
     * True if the block directly under the given location is a configured bad floor.
     */
    public boolean isBadStandingBlock(@NotNull Location location) {
        Block floor = location.getBlock().getRelative(BlockFace.DOWN);
        return config.husbandryBadFlooring().contains(floor.getType());
    }

    public void shutdown() {
        cleanupTask.cancel();
        cache.clear();
    }

    public static boolean isHusbandryAnimal(@NotNull org.bukkit.entity.EntityType type) {
        return type == org.bukkit.entity.EntityType.COW
            || type == org.bukkit.entity.EntityType.SHEEP
            || type == org.bukkit.entity.EntityType.PIG
            || type == org.bukkit.entity.EntityType.CHICKEN
            || type == org.bukkit.entity.EntityType.MOOSHROOM;
    }

    public static @NotNull String speciesKey(@NotNull org.bukkit.entity.EntityType type) {
        return switch (type) {
            case COW, MOOSHROOM -> "cow";
            case SHEEP -> "sheep";
            case PIG -> "pig";
            case CHICKEN -> "chicken";
            default -> type.name().toLowerCase(Locale.ROOT);
        };
    }

    private @NotNull PastureScore scoreFor(@NotNull LivingEntity animal, @NotNull PastureCacheEntry entry) {
        String key = speciesKey(animal.getType());
        int minBlocks = config.husbandryMinBlocksPerAnimal().getOrDefault(key, 12);
        int animalCount = entry.animalCounts.getOrDefault(animal.getType(), 0);
        double ratio = animalCount * (double) minBlocks / Math.max(1, entry.walkableBlocks);

        Tier tier = tierFromDensity(ratio);
        tier = applyFlooring(tier, entry);
        if (entry.water && config.isHusbandryWaterBoost()) {
            tier = Tier.up(tier);
        }
        return new PastureScore(tier, entry.walkableBlocks, animalCount, minBlocks, entry.water);
    }

    private @NotNull Tier tierFromDensity(double ratio) {
        double good = config.husbandryTierMultipliers().getOrDefault("good", 1.5);
        double fair = config.husbandryTierMultipliers().getOrDefault("fair", 2.5);
        if (ratio <= 1.0) {
            return Tier.EXCELLENT;
        }
        if (ratio <= good) {
            return Tier.GOOD;
        }
        if (ratio <= fair) {
            return Tier.FAIR;
        }
        return Tier.POOR;
    }

    private @NotNull Tier applyFlooring(@NotNull Tier tier, @NotNull PastureCacheEntry entry) {
        if (entry.walkableBlocks == 0) {
            return tier;
        }
        double goodFraction = (double) entry.goodFloors / entry.walkableBlocks;
        double badFraction = (double) entry.badFloors / entry.walkableBlocks;
        if (goodFraction >= config.husbandryGoodBoostThreshold()) {
            tier = Tier.up(tier);
        }
        if (badFraction >= config.husbandryBadPenaltyThreshold()) {
            tier = Tier.down(tier);
        }
        return tier;
    }

    private @NotNull FloodResult floodFill(@NotNull World world, int startX, int startY, int startZ) {
        int radius = config.husbandryMaxPastureRadius();
        Set<BlockPos> visited = new HashSet<>(HARD_CELL_CAP);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(HARD_CELL_CAP);
        BlockPos start = new BlockPos(startX, startY, startZ);
        if (isWalkable(world, start)) {
            visited.add(start);
            queue.add(start);
        }

        int minX = startX, maxX = startX, minY = startY, maxY = startY;
        int minZ = startZ, maxZ = startZ;

        while (!queue.isEmpty() && visited.size() < HARD_CELL_CAP) {
            BlockPos current = queue.poll();
            minX = Math.min(minX, current.x);
            maxX = Math.max(maxX, current.x);
            minY = Math.min(minY, current.y);
            maxY = Math.max(maxY, current.y);
            minZ = Math.min(minZ, current.z);
            maxZ = Math.max(maxZ, current.z);

            for (int dir = 0; dir < 4; dir++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = current.x + DX[dir];
                    int ny = current.y + dy;
                    int nz = current.z + DZ[dir];
                    if (Math.max(Math.abs(nx - startX), Math.abs(nz - startZ)) > radius) {
                        continue;
                    }
                    BlockPos next = new BlockPos(nx, ny, nz);
                    if (!visited.contains(next) && isWalkable(world, next)) {
                        visited.add(next);
                        queue.add(next);
                    }
                }
            }
        }
        return new FloodResult(visited, minX, minY, maxX, maxY, minZ, maxZ);
    }

    private boolean isWalkable(@NotNull World world, @NotNull BlockPos cell) {
        Block surface = world.getBlockAt(cell.x, cell.y, cell.z);
        Block head = world.getBlockAt(cell.x, cell.y + 1, cell.z);
        Block floor = world.getBlockAt(cell.x, cell.y - 1, cell.z);
        return floor.getType().isSolid()
            && !isBoundary(surface)
            && !head.getType().isSolid();
    }

    private boolean isBoundary(@NotNull Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Openable openable) {
            return !openable.isOpen();
        }
        return block.getType().isSolid() || isFenceWallDoor(block.getType());
    }

    private boolean isFenceWallDoor(@NotNull Material type) {
        String name = type.name();
        return name.endsWith("_FENCE") || name.endsWith("_WALL") || name.endsWith("_DOOR") || name.endsWith("_GATE");
    }

    private @NotNull PastureCacheEntry buildEntry(
        @NotNull LivingEntity animal,
        @NotNull FloodResult region,
        long now
    ) {
        World world = animal.getWorld();
        int good = 0;
        int bad = 0;
        boolean water = false;
        for (BlockPos cell : region.cells) {
            Block floor = world.getBlockAt(cell.x, cell.y - 1, cell.z);
            Material floorType = floor.getType();
            if (config.husbandryGoodFlooring().contains(floorType)) {
                good++;
            } else if (config.husbandryBadFlooring().contains(floorType)) {
                bad++;
            }
            Block surface = world.getBlockAt(cell.x, cell.y, cell.z);
            if (surface.getType() == Material.WATER || surface.getType() == Material.WATER_CAULDRON) {
                water = true;
            }
        }

        Map<org.bukkit.entity.EntityType, Integer> counts = countAnimals(world, region);
        int stagger = Math.abs(animal.getUniqueId().hashCode() % STAGGER_TICKS);
        return new PastureCacheEntry(region.cells.size(), good, bad, water, counts, now, stagger);
    }

    private @NotNull Map<org.bukkit.entity.EntityType, Integer> countAnimals(
        @NotNull World world,
        @NotNull FloodResult region
    ) {
        Map<org.bukkit.entity.EntityType, Integer> counts = new EnumMap<>(org.bukkit.entity.EntityType.class);
        if (region.cells.isEmpty()) {
            return counts;
        }

        double centerX = (region.minX + region.maxX) / 2.0 + 0.5;
        double centerY = (region.minY + region.maxY) / 2.0 + 0.5;
        double centerZ = (region.minZ + region.maxZ) / 2.0 + 0.5;
        double halfX = (region.maxX - region.minX) / 2.0 + 1.0;
        double halfY = (region.maxY - region.minY) / 2.0 + ENTITY_SEARCH_PADDING_Y;
        double halfZ = (region.maxZ - region.minZ) / 2.0 + 1.0;

        Location center = new Location(world, centerX, centerY, centerZ);
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(center, halfX, halfY, halfZ)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (!isHusbandryAnimal(living.getType())) {
                continue;
            }
            BlockPos cell = groundCell(living);
            if (region.cells.contains(cell)) {
                counts.merge(living.getType(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private @NotNull BlockPos groundCell(@NotNull LivingEntity entity) {
        Location loc = entity.getLocation();
        return new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private boolean isExpired(@NotNull PastureCacheEntry entry, long now) {
        long ttl = CACHE_TTL_SECONDS * 1000L + entry.stagger * 50L;
        return now - entry.timestamp > ttl;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        cache.values().removeIf(entry -> isExpired(entry, now));
    }

    public enum Tier {
        POOR, FAIR, GOOD, EXCELLENT;

        static Tier up(@NotNull Tier tier) {
            return switch (tier) {
                case POOR -> FAIR;
                case FAIR -> GOOD;
                case GOOD, EXCELLENT -> EXCELLENT;
            };
        }

        static Tier down(@NotNull Tier tier) {
            return switch (tier) {
                case EXCELLENT -> GOOD;
                case GOOD -> FAIR;
                case FAIR, POOR -> POOR;
            };
        }
    }

    public record PastureScore(@NotNull Tier tier, int walkableBlocks, int animalCount, int minBlocks, boolean water) {
        public static @NotNull PastureScore fallback() {
            return new PastureScore(Tier.FAIR, 1, 1, 1, false);
        }
    }

    private static final class BlockPos {
        final int x, y, z;

        BlockPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockPos other)) return false;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    private static final class FloodResult {
        final Set<BlockPos> cells;
        final int minX, minY, maxX, maxY, minZ, maxZ;

        FloodResult(Set<BlockPos> cells, int minX, int minY, int maxX, int maxY, int minZ, int maxZ) {
            this.cells = cells;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
    }

    private static final class RegionKey {
        final String world;
        final int minX, minZ, maxX, maxZ;

        RegionKey(String world, int minX, int minZ, int maxX, int maxZ) {
            this.world = world;
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RegionKey other)) return false;
            return minX == other.minX && minZ == other.minZ
                && maxX == other.maxX && maxZ == other.maxZ
                && world.equals(other.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, minX, minZ, maxX, maxZ);
        }
    }

    private static final class PastureCacheEntry {
        final int walkableBlocks;
        final int goodFloors;
        final int badFloors;
        final boolean water;
        final Map<org.bukkit.entity.EntityType, Integer> animalCounts;
        final long timestamp;
        final int stagger;

        PastureCacheEntry(
            int walkableBlocks,
            int goodFloors,
            int badFloors,
            boolean water,
            Map<org.bukkit.entity.EntityType, Integer> animalCounts,
            long timestamp,
            int stagger
        ) {
            this.walkableBlocks = walkableBlocks;
            this.goodFloors = goodFloors;
            this.badFloors = badFloors;
            this.water = water;
            this.animalCounts = animalCounts;
            this.timestamp = timestamp;
            this.stagger = stagger;
        }
    }
}
