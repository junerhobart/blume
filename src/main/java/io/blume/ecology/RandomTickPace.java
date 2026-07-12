package io.blume.ecology;

import org.bukkit.GameRules;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomTickPace {

    public static final int VANILLA_RANDOM_TICK_SPEED = 3;
    private static final int TICKS_PER_SECOND = 20;

    private RandomTickPace() {}

    public static int speed(@NotNull World world) {
        Integer value = world.getGameRuleValue(GameRules.RANDOM_TICK_SPEED);
        return value == null ? VANILLA_RANDOM_TICK_SPEED : value;
    }

    public static boolean isActive(@NotNull World world) {
        return speed(world) > 0;
    }

    public static int rollSamplesPerTick(
        double samplesPerSecondAtVanilla,
        @NotNull World world,
        @NotNull ThreadLocalRandom random
    ) {
        int speed = speed(world);
        if (speed <= 0) {
            return 0;
        }
        double perTick = samplesPerSecondAtVanilla / TICKS_PER_SECOND * speed / VANILLA_RANDOM_TICK_SPEED;
        int whole = (int) perTick;
        if (random.nextDouble() < perTick - whole) {
            whole++;
        }
        return whole;
    }
}
