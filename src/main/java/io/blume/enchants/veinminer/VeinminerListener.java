package io.blume.enchants.veinminer;

import io.blume.enchants.BlumeEnchantments;
import io.blume.enchants.EnchantChecks;
import io.blume.enchants.EnchantContext;
import io.blume.enchants.EnchantsConfig;
import io.blume.enchants.ManualBreak;
import io.blume.enchants.Ores;
import io.blume.enchants.VeinFill;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class VeinminerListener implements Listener {

    private final EnchantsConfig config;

    public VeinminerListener(EnchantsConfig config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (EnchantContext.isProcessing()) {
            return;
        }
        Block origin = event.getBlock();
        if (!Ores.isOre(origin.getType())) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.ADVENTURE) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!EnchantChecks.hasKey(tool, BlumeEnchantments.VEINMINER)) {
            return;
        }

        List<Block> vein = VeinFill.collect(
            origin,
            Ores.all(),
            config.veinminerMaxBlocks(),
            block -> true
        );
        if (vein.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        boolean smelt = EnchantChecks.hasKey(tool, BlumeEnchantments.AUTO_SMELT)
            && !EnchantChecks.hasSilkTouch(tool);

        EnchantContext.runWhileProcessing(() -> {
            for (Block block : vein) {
                if (block.getType().isAir()) {
                    continue;
                }
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType().isAir() || !ManualBreak.canBreakOneMore(player, held)) {
                    break;
                }
                ManualBreak.breakAsPlayer(player, block, held, smelt);
            }
        });
    }
}
