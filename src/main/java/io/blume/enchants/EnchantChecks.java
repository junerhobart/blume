package io.blume.enchants;

import io.papermc.paper.registry.TypedKey;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EnchantChecks {

    private EnchantChecks() {}

    public static boolean has(@Nullable ItemStack item, @Nullable Enchantment enchant) {
        return level(item, enchant) > 0;
    }

    public static int level(@Nullable ItemStack item, @Nullable Enchantment enchant) {
        if (item == null || item.getType().isAir() || enchant == null) {
            return 0;
        }
        Key target = enchant.getKey();
        int best = 0;
        for (var entry : item.getEnchantments().entrySet()) {
            if (entry.getKey().getKey().equals(target)) {
                best = Math.max(best, entry.getValue());
            }
        }
        return best;
    }

    public static boolean hasKey(@Nullable ItemStack item, @NotNull TypedKey<Enchantment> key) {
        return levelKey(item, key) > 0;
    }

    public static int levelKey(@Nullable ItemStack item, @NotNull TypedKey<Enchantment> key) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        Key target = key.key();
        int best = 0;
        for (var entry : item.getEnchantments().entrySet()) {
            if (entry.getKey().getKey().equals(target)) {
                best = Math.max(best, entry.getValue());
            }
        }
        return best;
    }

    public static boolean hasSilkTouch(@NotNull ItemStack tool) {
        return tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
    }
}
