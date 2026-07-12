package io.blume.enchants;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SmeltRecipes {

    // Keyed by the item DROPPED from the block (Block#getDrops without silk
    // touch never yields the ore block itself), mapped to its furnace output.
    private static final Map<Material, Material> SMELT = Map.of(
        Material.RAW_IRON, Material.IRON_INGOT,
        Material.RAW_GOLD, Material.GOLD_INGOT,
        Material.RAW_COPPER, Material.COPPER_INGOT,
        Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP
    );

    private SmeltRecipes() {}

    public static @NotNull List<ItemStack> smeltDrops(@NotNull List<ItemStack> drops) {
        List<ItemStack> smelted = new ArrayList<>(drops.size());
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir()) {
                continue;
            }
            if (drop.getType() == Material.GOLD_NUGGET) {
                // Nether gold ore drops nuggets; the furnace output of the ore
                // is one gold ingot per block, regardless of nugget count.
                smelted.add(new ItemStack(Material.GOLD_INGOT, 1));
                continue;
            }
            Material result = SMELT.get(drop.getType());
            if (result == null) {
                smelted.add(drop.clone());
                continue;
            }
            smelted.add(new ItemStack(result, drop.getAmount()));
        }
        return smelted;
    }
}
