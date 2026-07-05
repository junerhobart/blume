package io.blume.enchants;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

public final class Ores {

    private static final Set<Material> ORES = EnumSet.of(
        Material.COAL_ORE,
        Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.COPPER_ORE,
        Material.DEEPSLATE_COPPER_ORE,
        Material.GOLD_ORE,
        Material.DEEPSLATE_GOLD_ORE,
        Material.REDSTONE_ORE,
        Material.DEEPSLATE_REDSTONE_ORE,
        Material.EMERALD_ORE,
        Material.DEEPSLATE_EMERALD_ORE,
        Material.LAPIS_ORE,
        Material.DEEPSLATE_LAPIS_ORE,
        Material.DIAMOND_ORE,
        Material.DEEPSLATE_DIAMOND_ORE,
        Material.NETHER_GOLD_ORE,
        Material.NETHER_QUARTZ_ORE,
        Material.ANCIENT_DEBRIS
    );

    private Ores() {}

    public static @NotNull Set<Material> all() {
        return ORES;
    }

    public static boolean isOre(@NotNull Material material) {
        return ORES.contains(material);
    }
}
