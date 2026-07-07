// Adapted from Vane (MIT) - https://github.com/oddlama/vane
// Copyright (c) 2020 oddlama

package io.blume.qol.creeper;

import io.blume.BlumePlugin;
import io.blume.qol.QolConfig;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CreeperRebuildService {

    private final BlumePlugin plugin;
    private final QolConfig config;
    private final List<Rebuilder> rebuilders = new ArrayList<>();

    public CreeperRebuildService(@NotNull BlumePlugin plugin, @NotNull QolConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void rebuild(@NotNull List<Block> blocks) {
        List<BlockState> states = new ArrayList<>(blocks.size());
        for (Block block : blocks) {
            states.add(block.getState());
        }

        for (Block block : blocks) {
            block.setType(Material.AIR, false);
        }

        rebuilders.add(new Rebuilder(states));
    }

    public void shutdown() {
        for (Rebuilder rebuilder : new ArrayList<>(rebuilders)) {
            rebuilder.finishNow();
        }
        rebuilders.clear();
    }

    private long msToTicks(long ms) {
        return Math.max(1L, ms / 50L);
    }

    private final class Rebuilder implements Runnable {

        private final List<BlockState> states;
        private BukkitTask task;
        private long amountRebuild;

        private Rebuilder(List<BlockState> states) {
            this.states = new ArrayList<>(states);
            if (this.states.isEmpty()) {
                return;
            }

            Vector center = new Vector(0, 0, 0);
            int maxY = 0;
            for (BlockState state : this.states) {
                maxY = Math.max(maxY, state.getY());
                center.add(state.getLocation().toVector());
            }
            center.multiply(1.0 / this.states.size());
            center.setY(maxY + 1);

            this.states.sort(new RebuildComparator(center));
            task = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                this,
                msToTicks(config.creeperRebuildDelayMs())
            );
        }

        private void finish() {
            if (task != null) {
                task.cancel();
                task = null;
            }
            rebuilders.remove(this);
        }

        private void rebuildBlock(BlockState state) {
            Block block = state.getBlock();
            amountRebuild++;

            if (block.getType() != Material.AIR) {
                block.setType(Material.AIR, false);
            }

            state.update(true, false);
            state.update(true, false);

            block.getWorld().playSound(
                block.getLocation(),
                block.getBlockSoundGroup().getPlaceSound(),
                SoundCategory.BLOCKS,
                1.0f,
                0.8f
            );
        }

        void finishNow() {
            if (task != null) {
                task.cancel();
                task = null;
            }
            for (BlockState state : states) {
                rebuildBlock(state);
            }
            states.clear();
            finish();
        }

        @Override
        public void run() {
            if (states.isEmpty()) {
                finish();
                return;
            }

            rebuildBlock(states.removeLast());

            long delayMs = Math.max(
                config.creeperRebuildMinDelayMs(),
                (long) (config.creeperRebuildDelayMs()
                    * Math.exp(-amountRebuild * config.creeperRebuildDelayFalloff()))
            );
            task = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                this,
                msToTicks(delayMs)
            );
        }
    }

    private static final class RebuildComparator implements Comparator<BlockState> {

        private final Vector referencePoint;

        private RebuildComparator(Vector referencePoint) {
            this.referencePoint = referencePoint;
        }

        @Override
        public int compare(BlockState a, BlockState b) {
            double da = a.getLocation().toVector().subtract(referencePoint).lengthSquared();
            double db = b.getLocation().toVector().subtract(referencePoint).lengthSquared();
            return Double.compare(da, db);
        }
    }
}
