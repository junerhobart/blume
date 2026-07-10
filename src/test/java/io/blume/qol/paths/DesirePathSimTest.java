package io.blume.qol.paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DesirePathSimTest {

    private static final DesirePathEngine.DesirePathRules DEFAULT_RULES = DesirePathSimulator.defaultRules();

    @Test
    void dailyCommute_50blocks() {
        int ticks = DesirePathSimulator.tripsToPathAllBlocks(
            DEFAULT_RULES,
            DesirePathSimulator.COMMUTE_PATH_LENGTH,
            DesirePathEngine.TICKS_PER_MC_DAY,
            30 * DesirePathEngine.TICKS_PER_MC_DAY
        );
        assertThat(ticks).isBetween(1, 30 * DesirePathEngine.TICKS_PER_MC_DAY);
    }

    @Test
    void longTrip_every6McDays() {
        int trips = DesirePathSimulator.tripsToPath(
            DEFAULT_RULES,
            DesirePathSimulator.LONG_TRIP_INTERVAL_TICKS,
            200 * DesirePathEngine.TICKS_PER_MC_DAY
        );
        assertThat(trips).isBetween(1, 200);
    }

    @Test
    void longTrip_otherPlayersDoNotDecay() {
        assertThat(DesirePathSimulator.otherPlayersDoNotDecayBlock(
            DEFAULT_RULES,
            DesirePathSimulator.LONG_TRIP_INTERVAL_TICKS
        )).isTrue();
    }

    @Test
    void abandonedPath_regresses() {
        DesirePathEngine.PathState worn = new DesirePathEngine.PathState(
            DesirePathEngine.STAGE_WORN,
            DEFAULT_RULES.walksToPath(),
            1000L
        );
        int ticks = DesirePathSimulator.ticksUntilRegression(
            worn,
            DEFAULT_RULES,
            DEFAULT_RULES.wornTimeoutTicks() + DesirePathEngine.TICKS_PER_MC_DAY
        );
        assertThat(ticks).isBetween(
            DEFAULT_RULES.wornTimeoutTicks(),
            DEFAULT_RULES.wornTimeoutTicks() + DesirePathEngine.TICKS_PER_MC_DAY
        );
    }

    @Test
    void formedPath_stays() {
        DesirePathEngine.Outcome walk = DesirePathEngine.recordWalk(
            new DesirePathEngine.PathState(DesirePathEngine.STAGE_WORN, DEFAULT_RULES.walksToPath(), 1L),
            DEFAULT_RULES,
            2L
        );
        assertThat(walk.effect()).isEqualTo(DesirePathEngine.Effect.BECOME_PATH);
        assertThat(walk.state()).isNull();
    }

    @Test
    void packUnpack_grassStageRoundTrip() {
        DesirePathEngine.PathState state = new DesirePathEngine.PathState(
            DesirePathEngine.STAGE_GRASS,
            3,
            24_000L
        );
        long packed = DesirePathEngine.pack(state);
        DesirePathEngine.PathState restored = DesirePathEngine.unpack(packed);
        assertThat(restored).isEqualTo(state);
    }

    @Test
    void packUnpack_roundTrip() {
        DesirePathEngine.PathState state = new DesirePathEngine.PathState(2, 11, 900L);
        long packed = DesirePathEngine.pack(state);
        DesirePathEngine.PathState restored = DesirePathEngine.unpack(packed);
        assertThat(restored).isEqualTo(state);
    }

    @Test
    void legacyUnpack_migratesOldFormat() {
        long legacy = ((long) DesirePathEngine.STAGE_TRAMPLED << 32) | 5L;
        DesirePathEngine.PathState state = DesirePathEngine.unpack(legacy);
        assertThat(state).isEqualTo(new DesirePathEngine.PathState(DesirePathEngine.STAGE_TRAMPLED, 5, 0L));
    }
}
