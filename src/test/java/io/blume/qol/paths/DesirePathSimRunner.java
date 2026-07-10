package io.blume.qol.paths;

final class DesirePathSimRunner {

    private DesirePathSimRunner() {}

    public static void main(String[] args) {
        DesirePathEngine.DesirePathRules rules = DesirePathSimulator.defaultRules();
        System.out.println("tripInterval -> tripsToPath (defaults 4/8/12, de-erosion 5/8 MC days)");
        for (int mcDays : new int[] {1, 3, 6, 12}) {
            int interval = mcDays * DesirePathEngine.TICKS_PER_MC_DAY;
            int trips = DesirePathSimulator.tripsToPath(rules, interval, 300 * DesirePathEngine.TICKS_PER_MC_DAY);
            System.out.printf("  every %2d MC days: %d trips to path%n", mcDays, trips);
        }

        DesirePathEngine.PathState worn = new DesirePathEngine.PathState(
            DesirePathEngine.STAGE_WORN,
            rules.walksToPath(),
            1000L
        );
        int regressTicks = DesirePathSimulator.ticksUntilRegression(
            worn,
            rules,
            rules.wornTimeoutTicks() + DesirePathEngine.TICKS_PER_MC_DAY
        );
        System.out.printf(
            "abandoned worn path regresses after: %d ticks (%.1f MC days)%n",
            regressTicks,
            regressTicks / (double) DesirePathEngine.TICKS_PER_MC_DAY
        );
    }
}
