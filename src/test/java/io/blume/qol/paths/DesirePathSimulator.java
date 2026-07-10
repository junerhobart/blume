package io.blume.qol.paths;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DesirePathSimulator {

    private DesirePathSimulator() {}

    record BlockSim(@Nullable DesirePathEngine.PathState state, boolean pathFormed) {}

    static final int LONG_TRIP_INTERVAL_TICKS = 6 * DesirePathEngine.TICKS_PER_MC_DAY;
    static final int COMMUTE_PATH_LENGTH = 50;

    static @NotNull DesirePathEngine.DesirePathRules defaultRules() {
        return new DesirePathEngine.DesirePathRules(
            4,
            8,
            12,
            DesirePathEngine.timeoutTicks(5),
            DesirePathEngine.timeoutTicks(8),
            true
        );
    }

    static int tripsToPath(
        @NotNull DesirePathEngine.DesirePathRules rules,
        int tripIntervalTicks,
        int maxTicks
    ) {
        BlockSim block = new BlockSim(DesirePathEngine.PathState.freshGrass(), false);
        long tick = 0L;
        int trips = 0;

        while (tick <= maxTicks) {
            tick += tripIntervalTicks;
            trips++;
            block = walk(block, rules, tick);
            if (block.pathFormed()) {
                return trips;
            }
        }
        return -1;
    }

    static int tripsToPathAllBlocks(
        @NotNull DesirePathEngine.DesirePathRules rules,
        int blockCount,
        int tripIntervalTicks,
        int maxTicks
    ) {
        BlockSim[] blocks = new BlockSim[blockCount];
        for (int i = 0; i < blockCount; i++) {
            blocks[i] = new BlockSim(DesirePathEngine.PathState.freshGrass(), false);
        }

        long tick = 0L;
        while (tick <= maxTicks) {
            tick += tripIntervalTicks;
            for (int i = 0; i < blockCount; i++) {
                blocks[i] = walk(blocks[i], rules, tick + i);
            }
            boolean allPaths = true;
            for (BlockSim block : blocks) {
                if (!block.pathFormed()) {
                    allPaths = false;
                    break;
                }
            }
            if (allPaths) {
                return (int) tick;
            }
        }
        return -1;
    }

    static int ticksUntilRegression(
        @NotNull DesirePathEngine.PathState start,
        @NotNull DesirePathEngine.DesirePathRules rules,
        int maxTicks
    ) {
        BlockSim block = new BlockSim(start, false);
        int startStage = start.stage();
        long tick = start.lastTouchedTick();

        while (tick <= start.lastTouchedTick() + maxTicks) {
            tick++;
            DesirePathEngine.Outcome outcome = DesirePathEngine.applyDeErosion(block.state(), rules, tick);
            block = new BlockSim(outcome.state(), false);
            if (block.state() != null && block.state().stage() < startStage) {
                return (int) (tick - start.lastTouchedTick());
            }
        }
        return -1;
    }

    static boolean otherPlayersDoNotDecayBlock(
        @NotNull DesirePathEngine.DesirePathRules rules,
        int tripIntervalTicks
    ) {
        DesirePathEngine.PathState state = DesirePathEngine.PathState.freshGrass();
        long pathTick = 0L;

        for (int trip = 0; trip < 10; trip++) {
            pathTick += tripIntervalTicks;
            DesirePathEngine.Outcome walk = DesirePathEngine.recordWalk(state, rules, pathTick);
            if (walk.state() == null) {
                return true;
            }
            state = walk.state();

            long otherActivityTick = pathTick + tripIntervalTicks / 2;
            DesirePathEngine.Outcome eroded = DesirePathEngine.applyDeErosion(state, rules, otherActivityTick);
            if (eroded.effect() != DesirePathEngine.Effect.NONE) {
                return false;
            }
        }
        return true;
    }

    private static @NotNull BlockSim walk(
        @NotNull BlockSim block,
        @NotNull DesirePathEngine.DesirePathRules rules,
        long tick
    ) {
        if (block.pathFormed() || block.state() == null) {
            return block;
        }
        DesirePathEngine.Outcome outcome = DesirePathEngine.recordWalk(block.state(), rules, tick);
        if (outcome.effect() == DesirePathEngine.Effect.BECOME_PATH) {
            return new BlockSim(null, true);
        }
        return new BlockSim(outcome.state(), false);
    }
}
