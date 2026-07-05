package io.blume.enchants;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SmeltRecipes {

    private static final Map<Material, Material> SMELT = new HashMap<>();

    static {
        SMELT.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELT.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELT.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELT.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELT.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELT.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        SMELT.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        SMELT.put(Material.RAW_IRON, Material.IRON_INGOT);
        SMELT.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        SMELT.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        SMELT.put(Material.NETHER_GOLD_ORE, Material.GOLD_NUGGET);
    }

    private SmeltRecipes() {}

    public static @NotNull List<ItemStack> smeltDrops(@NotNull List<ItemStack> drops) {
        List<ItemStack> smelted = new ArrayList<>(drops.size());
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir()) {
                continue;
            }
            Material result = SMELT.get(drop.getType());
            if (result == null) {
                smelted.add(drop.clone());
                continue;
            }
            if (drop.getType() == Material.NETHER_GOLD_ORE) {
                smelted.add(new ItemStack(Material.GOLD_NUGGET, drop.getAmount() * 6));
            } else {
                smelted.add(new ItemStack(result, drop.getAmount()));
            }
        }
        return smelted;
    }
}
