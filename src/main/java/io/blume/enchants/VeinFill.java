package io.blume.enchants;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.BiPredicate;

public final class VeinFill {

    private static final BlockFace[] NEIGHBORS = {
        BlockFace.UP, BlockFace.DOWN,
        BlockFace.NORTH, BlockFace.SOUTH,
        BlockFace.EAST, BlockFace.WEST
    };

    private VeinFill() {}

    public static @NotNull List<Block> collect(
        @NotNull Block origin,
        @NotNull Set<Material> allowed,
        int maxBlocks,
        @NotNull Predicate<Block> include
    ) {
        Material target = origin.getType();
        if (!allowed.contains(target)) {
            return List.of();
        }

        List<Block> found = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            if (maxBlocks >= 0 && found.size() >= maxBlocks) {
                break;
            }
            Block current = queue.poll();
            if (!include.test(current)) {
                continue;
            }
            if (current.getType() != target) {
                continue;
            }
            found.add(current);

            for (BlockFace face : NEIGHBORS) {
                Block neighbor = current.getRelative(face);
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return found;
    }

    public static @NotNull List<Block> collectFamily(
        @NotNull Block origin,
        int maxBlocks,
        @NotNull Predicate<Block> include,
        @NotNull Predicate<Material> isMember,
        @NotNull BiPredicate<Material, Material> sameKind
    ) {
        Material originType = origin.getType();
        if (!isMember.test(originType)) {
            return List.of();
        }

        List<Block> found = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            if (maxBlocks >= 0 && found.size() >= maxBlocks) {
                break;
            }
            Block current = queue.poll();
            if (!include.test(current)) {
                continue;
            }
            Material currentType = current.getType();
            if (!isMember.test(currentType) || !sameKind.test(originType, currentType)) {
                continue;
            }
            found.add(current);

            for (BlockFace face : NEIGHBORS) {
                Block neighbor = current.getRelative(face);
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return found;
    }
}
