package io.blume.enchants.autosmelt;

import io.blume.enchants.BlumeEnchantments;
import io.blume.enchants.EnchantChecks;
import io.blume.enchants.EnchantContext;
import io.blume.enchants.ManualBreak;
import io.blume.enchants.Ores;
import io.blume.enchants.VeinFill;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public final class AutoSmeltListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (EnchantContext.isProcessing()) {
            return;
        }
        if (!Ores.isOre(event.getBlock().getType())) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.ADVENTURE) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!EnchantChecks.hasKey(tool, BlumeEnchantments.AUTO_SMELT) || EnchantChecks.hasSilkTouch(tool)) {
            return;
        }

        if (EnchantChecks.hasKey(tool, BlumeEnchantments.VEINMINER)) {
            if (VeinFill.collect(
                event.getBlock(),
                Ores.all(),
                2,
                block -> true
            ).size() > 1) {
                return;
            }
        }

        event.setCancelled(true);
        if (!ManualBreak.canBreakOneMore(player, tool)) {
            return;
        }
        EnchantContext.runWhileProcessing(() ->
            ManualBreak.breakAsPlayer(player, event.getBlock(), tool, true)
        );
    }
}
