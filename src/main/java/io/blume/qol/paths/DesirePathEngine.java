package io.blume.qol.paths;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DesirePathEngine {

    public static final int STAGE_GRASS = 0;
    public static final int STAGE_TRAMPLED = 1;
    public static final int STAGE_WORN = 2;
    public static final int TICKS_PER_MC_DAY = 24_000;
    private static final long FORMAT_V2_MARKER = 1L << 63;

    private DesirePathEngine() {}

    public record PathState(int stage, int walks, long lastTouchedTick) {

        public static @NotNull PathState freshGrass() {
            return new PathState(STAGE_GRASS, 0, 0L);
        }
    }

    public record DesirePathRules(
        int walksToTrample,
        int walksToWorn,
        int walksToPath,
        int trampledTimeoutTicks,
        int wornTimeoutTicks,
        boolean deErosionEnabled
    ) {}

    public enum Effect {
        NONE,
        TRAMPLE_VEGETATION,
        BECOME_WORN,
        BECOME_PATH,
        REGRESS_TO_TRAMPLED,
        REGRESS_TO_GRASS
    }

    public record Outcome(@Nullable PathState state, @NotNull Effect effect) {

        public static @NotNull Outcome unchanged(@NotNull PathState state) {
            return new Outcome(state, Effect.NONE);
        }
    }

    public static long pack(@NotNull PathState state) {
        return FORMAT_V2_MARKER
            | ((long) state.stage() << 56)
            | ((long) state.walks() << 32)
            | (packTick(state.lastTouchedTick()) & 0xFFFFFFFFL);
    }

    public static int packTick(long worldFullTime) {
        return (int) (worldFullTime & 0xFFFFFFFFL);
    }

    public static long unpackTick(int packedTick) {
        return packedTick & 0xFFFFFFFFL;
    }

    public static @Nullable PathState unpack(long packed) {
        if ((packed & FORMAT_V2_MARKER) != 0L) {
            int stage = (int) ((packed >> 56) & 0x7F);
            int walks = (int) ((packed >> 32) & 0xFFFFFF);
            long lastTouchedTick = unpackTick((int) (packed & 0xFFFFFFFFL));
            if (stage < STAGE_GRASS || stage > STAGE_WORN) {
                return null;
            }
            return new PathState(stage, walks, lastTouchedTick);
        }

        int legacyStage = (int) (packed >> 32);
        if (legacyStage >= STAGE_GRASS && legacyStage <= STAGE_WORN) {
            int walks = (int) (packed & 0xFFFFFFFFL);
            if (walks >= 0 && walks < 256) {
                return new PathState(legacyStage, walks, 0L);
            }
        }
        return null;
    }

    public static @NotNull Outcome recordWalk(
        @NotNull PathState state,
        @NotNull DesirePathRules rules,
        long currentTick
    ) {
        int walks = state.walks() + 1;
        int stage = state.stage();

        if (stage == STAGE_GRASS) {
            if (walks >= rules.walksToTrample()) {
                return new Outcome(
                    new PathState(STAGE_TRAMPLED, 0, currentTick),
                    Effect.TRAMPLE_VEGETATION
                );
            }
            return Outcome.unchanged(new PathState(STAGE_GRASS, walks, currentTick));
        }

        if (stage == STAGE_TRAMPLED) {
            if (walks >= rules.walksToWorn()) {
                return new Outcome(
                    new PathState(STAGE_WORN, 0, currentTick),
                    Effect.BECOME_WORN
                );
            }
            return Outcome.unchanged(new PathState(STAGE_TRAMPLED, walks, currentTick));
        }

        if (stage == STAGE_WORN) {
            if (walks >= rules.walksToPath()) {
                return new Outcome(null, Effect.BECOME_PATH);
            }
            return Outcome.unchanged(new PathState(STAGE_WORN, walks, currentTick));
        }

        return Outcome.unchanged(new PathState(stage, walks, currentTick));
    }

    public static @NotNull Outcome applyDeErosion(
        @NotNull PathState state,
        @NotNull DesirePathRules rules,
        long currentTick
    ) {
        if (!rules.deErosionEnabled() || state.lastTouchedTick() <= 0L) {
            return Outcome.unchanged(state);
        }

        long idleTicks = idleTicks(currentTick, state.lastTouchedTick());
        if (state.stage() == STAGE_WORN && idleTicks >= rules.wornTimeoutTicks()) {
            int walks = Math.max(1, rules.walksToWorn() / 2);
            return new Outcome(
                new PathState(STAGE_TRAMPLED, walks, currentTick),
                Effect.REGRESS_TO_TRAMPLED
            );
        }
        if (state.stage() == STAGE_TRAMPLED && idleTicks >= rules.trampledTimeoutTicks()) {
            int walks = Math.max(1, rules.walksToTrample() / 2);
            return new Outcome(
                new PathState(STAGE_GRASS, walks, currentTick),
                Effect.REGRESS_TO_GRASS
            );
        }

        return Outcome.unchanged(state);
    }

    public static long idleTicks(long currentTick, long lastTouchedTick) {
        int current = packTick(currentTick);
        int last = packTick(lastTouchedTick);
        return Integer.toUnsignedLong(current - last);
    }

    public static int timeoutTicks(int mcDays) {
        return mcDays * TICKS_PER_MC_DAY;
    }

    public static @NotNull DesirePathRules rulesFrom(@NotNull io.blume.qol.QolConfig config) {
        return new DesirePathRules(
            config.walksToTrample(),
            config.walksToWorn(),
            config.walksToPath(),
            timeoutTicks(config.desirePathDeErosionTrampledMcDays()),
            timeoutTicks(config.desirePathDeErosionWornMcDays()),
            config.isDesirePathDeErosionEnabled()
        );
    }
}
