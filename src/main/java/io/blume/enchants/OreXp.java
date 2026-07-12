package io.blume.enchants;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public final class OreXp {

    private OreXp() {}

    public static int forBlock(@NotNull Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> range(0, 2);
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> range(1, 5);
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> range(2, 5);
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> range(3, 7);
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> range(3, 7);
            case NETHER_QUARTZ_ORE -> range(2, 5);
            case NETHER_GOLD_ORE -> range(0, 1);
            default -> 0;
        };
    }

    // Furnace XP for ores whose value comes from smelting rather than mining
    // (vanilla: iron/copper 0.7, gold/nether gold 1.0, ancient debris 2.0).
    public static int smeltBonus(@NotNull Material material) {
        return switch (material) {
            case IRON_ORE, DEEPSLATE_IRON_ORE, COPPER_ORE, DEEPSLATE_COPPER_ORE -> 1;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> 1;
            case ANCIENT_DEBRIS -> 2;
            default -> 0;
        };
    }

    private static int range(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
