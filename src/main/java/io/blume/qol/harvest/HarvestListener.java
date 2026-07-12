package io.blume.qol.harvest;

import io.blume.enchants.BlumeEnchantments;
import io.blume.enchants.EnchantChecks;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class HarvestListener implements Listener {

    private final boolean requireEmptyHand;

    public HarvestListener(boolean requireEmptyHand) {
        this.requireEmptyHand = requireEmptyHand;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.ADVENTURE) {
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (requireEmptyHand && hand.getType() != Material.AIR) {
            return;
        }
        // SickleListener owns sickle harvesting (radius harvest); skip to avoid double-processing.
        if (EnchantChecks.levelKey(hand, BlumeEnchantments.SICKLE) > 0) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!CropHarvest.isMatureCrop(block)) {
            return;
        }

        event.setCancelled(true);
        CropHarvest.harvestAndReplant(player, block, hand);
    }
}
