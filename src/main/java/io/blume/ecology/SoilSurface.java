package io.blume.ecology;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class SoilSurface {

    private static final Set<Material> CROP_BLOCKS = Set.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS
    );
    private static final int SCAN_BELOW_SURFACE = 48;
    private static final int SCAN_ABOVE_SURFACE = 8;
    private static final int HINT_SCAN_ABOVE = 12;
    private static final int HINT_SCAN_BELOW = 48;
    private static final int NO_HINT = Integer.MIN_VALUE;

    private SoilSurface() {}

    public record Spot(@NotNull Block ground, @NotNull Block plantBlock, @Nullable Block tallGrassTop) {}

    public static boolean isGround(@NotNull Material type) {
        return Tag.DIRT.isTagged(type)
            || type == Material.GRASS_BLOCK
            || type == Material.DIRT_PATH
            || type == Material.MYCELIUM
            || type == Material.PODZOL
            || type == Material.MOSS_BLOCK
            || type == Material.MUD
            || type == Material.MUDDY_MANGROVE_ROOTS
            || type == Material.ROOTED_DIRT;
    }

    public static boolean isCropBlock(@NotNull Material type) {
        return CROP_BLOCKS.contains(type);
    }

    public static boolean isShortGrass(@NotNull Block block) {
        return block.getType() == Material.SHORT_GRASS;
    }

    public static @Nullable Spot find(@NotNull World world, int x, int z) {
        return find(world, x, z, NO_HINT);
    }

    public static @Nullable Spot find(@NotNull World world, int x, int z, int hintY) {
        int motionY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int surfaceY = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
        int anchorY = hintY == NO_HINT ? Math.max(motionY, surfaceY) : hintY;

        Spot spot = scanColumn(
            world,
            x,
            z,
            Math.min(world.getMaxHeight() - 1, max(motionY, surfaceY, anchorY) + SCAN_ABOVE_SURFACE),
            Math.max(world.getMinHeight(), min(motionY, surfaceY, anchorY) - SCAN_BELOW_SURFACE)
        );
        if (spot != null) {
            return spot;
        }

        if (hintY != NO_HINT) {
            spot = scanColumn(
                world,
                x,
                z,
                Math.min(world.getMaxHeight() - 1, hintY + HINT_SCAN_ABOVE),
                Math.max(world.getMinHeight(), hintY - HINT_SCAN_BELOW)
            );
            if (spot != null) {
                return spot;
            }
        }

        return scanColumn(
            world,
            x,
            z,
            Math.min(world.getMaxHeight() - 1, motionY + SCAN_ABOVE_SURFACE),
            Math.max(world.getMinHeight(), motionY - SCAN_BELOW_SURFACE)
        );
    }

    public static void clearTallGrassTop(@NotNull Spot spot) {
        Block top = spot.tallGrassTop();
        if (top != null && !top.isEmpty()) {
            top.setType(Material.AIR, false);
        }
    }

    public static boolean touchesGround(@NotNull Spot spot) {
        Block cursor = spot.plantBlock();
        while (true) {
            Block below = cursor.getRelative(BlockFace.DOWN);
            if (below.equals(spot.ground())) {
                return true;
            }
            if (!below.isEmpty() && canPlantOver(below)) {
                cursor = below;
                continue;
            }
            return false;
        }
    }

    private static @Nullable Spot scanColumn(@NotNull World world, int x, int z, int maxY, int minY) {
        for (int y = maxY; y >= minY; y--) {
            Block ground = world.getBlockAt(x, y, z);
            if (!isGround(ground.getType())) {
                continue;
            }
            Spot spot = spotOnGround(ground);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    private static @Nullable Spot spotOnGround(@NotNull Block ground) {
        Block above = ground.getRelative(BlockFace.UP);
        if (isTallFernOrGrassBottom(above)) {
            return new Spot(ground, above, above.getRelative(BlockFace.UP));
        }
        Block plantBlock = resolvePlantBlock(ground);
        if (plantBlock == null) {
            return null;
        }
        return new Spot(ground, plantBlock, null);
    }

    private static @Nullable Block resolvePlantBlock(@NotNull Block ground) {
        Block cursor = ground.getRelative(BlockFace.UP);
        if (!cursor.isEmpty() && !canPlantOver(cursor)) {
            return null;
        }

        while (!cursor.isEmpty() && canPlantOver(cursor)) {
            Block next = cursor.getRelative(BlockFace.UP);
            if (next.isEmpty() || !canPlantOver(next)) {
                break;
            }
            cursor = next;
        }

        return cursor.isEmpty() || canPlantOver(cursor) ? cursor : null;
    }

    private static boolean canPlantOver(@NotNull Block block) {
        if (block.isEmpty()) {
            return true;
        }
        Material type = block.getType();
        if (CROP_BLOCKS.contains(type)) {
            return false;
        }
        if (type == Material.SHORT_GRASS
            || type == Material.FERN
            || type == Material.DEAD_BUSH
            || type == Material.MOSS_CARPET
            || type == Material.HANGING_ROOTS
            || type == Material.SMALL_AMETHYST_BUD
            || type == Material.MEDIUM_AMETHYST_BUD
            || type == Material.LARGE_AMETHYST_BUD
            || type == Material.AMETHYST_CLUSTER) {
            return true;
        }
        if (Tag.FLOWERS.isTagged(type)) {
            return true;
        }
        if (Tag.LEAVES.isTagged(type)) {
            return false;
        }
        if (Tag.LOGS.isTagged(type)) {
            return false;
        }
        return block.isReplaceable() && !block.isLiquid();
    }

    private static boolean isTallFernOrGrassBottom(@NotNull Block block) {
        Material type = block.getType();
        if (type != Material.TALL_GRASS && type != Material.LARGE_FERN) {
            return false;
        }
        if (!(block.getBlockData() instanceof Bisected bisected)) {
            return false;
        }
        return bisected.getHalf() == Bisected.Half.BOTTOM;
    }

    private static int max(int a, int b, int c) {
        return Math.max(a, Math.max(b, c));
    }

    private static int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }
}
