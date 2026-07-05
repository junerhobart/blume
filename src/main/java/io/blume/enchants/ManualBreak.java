package io.blume.enchants;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class ManualBreak {

    private ManualBreak() {}

    public static boolean canBreakOneMore(@NotNull Player player, @NotNull ItemStack tool) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        if (tool.getType().getMaxDurability() <= 0) {
            return true;
        }
        if (EnchantChecks.has(tool, BlumeEnchantments.unbreakable())) {
            return true;
        }
        if (!(tool.getItemMeta() instanceof Damageable damageable)) {
            return true;
        }
        return damageable.getDamage() < tool.getType().getMaxDurability();
    }

    public static boolean breakAsPlayer(
        @NotNull Player player,
        @NotNull Block block,
        @NotNull ItemStack tool,
        boolean smeltDrops
    ) {
        if (!canBreakOneMore(player, tool)) {
            return false;
        }

        Collection<ItemStack> drops = block.getDrops(tool, player);
        List<ItemStack> dropList = smeltDrops
            ? SmeltRecipes.smeltDrops(List.copyOf(drops))
            : List.copyOf(drops);
        int xp = OreXp.forBlock(block.getType());
        block.setType(Material.AIR, false);

        var location = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack drop : dropList) {
            if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) {
                continue;
            }
            block.getWorld().dropItemNaturally(location, drop);
        }

        if (xp > 0) {
            player.giveExp(xp);
        }

        damageTool(player, tool);
        return true;
    }

    private static void damageTool(@NotNull Player player, @NotNull ItemStack tool) {
        if (player.getGameMode() == GameMode.CREATIVE
            || tool.getType().getMaxDurability() <= 0
            || EnchantChecks.has(tool, BlumeEnchantments.unbreakable())
            || !(tool.getItemMeta() instanceof Damageable damageable)) {
            return;
        }

        int newDamage = damageable.getDamage() + 1;
        if (newDamage >= tool.getType().getMaxDurability()) {
            player.getInventory().setItemInMainHand(null);
            return;
        }
        damageable.setDamage(newDamage);
        tool.setItemMeta(damageable);
    }
}
