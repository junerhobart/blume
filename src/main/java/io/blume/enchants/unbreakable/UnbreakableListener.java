package io.blume.enchants.unbreakable;

import io.blume.enchants.BlumeEnchantments;
import io.blume.enchants.EnchantChecks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

public final class UnbreakableListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (EnchantChecks.has(item, BlumeEnchantments.unbreakable())) {
            event.setCancelled(true);
        }
    }
}
