package io.blume.enchants;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public final class OreXp {

    private OreXp() {}

    public static int forBlock(@NotNull Material material) {
        return switch (material) {
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> range(1, 5);
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> range(2, 5);
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> range(3, 7);
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> range(3, 7);
            case NETHER_QUARTZ_ORE -> range(2, 5);
            default -> 0;
        };
    }

    private static int range(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
