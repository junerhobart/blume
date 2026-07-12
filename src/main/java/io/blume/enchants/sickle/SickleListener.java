package io.blume.enchants.sickle;

import io.blume.enchants.BlumeEnchantments;
import io.blume.enchants.EnchantChecks;
import io.blume.enchants.EnchantsConfig;
import io.blume.enchants.ManualBreak;
import io.blume.qol.harvest.CropHarvest;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;

public final class SickleListener implements Listener {

    private final EnchantsConfig config;

    public SickleListener(EnchantsConfig config) {
        this.config = config;
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

        ItemStack tool = player.getInventory().getItemInMainHand();
        int level = EnchantChecks.levelKey(tool, BlumeEnchantments.SICKLE);
        if (level <= 0) {
            return;
        }
        if (!CropHarvest.isMatureCrop(event.getClickedBlock())) {
            return;
        }

        event.setCancelled(true);
        harvestSphere(player, event.getClickedBlock(), tool, config.sickleRadius(level));
    }

    private static void harvestSphere(Player player, Block center, ItemStack tool, int radius) {
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        World world = center.getWorld();
        int radiusSq = radius * radius;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            int dx = x - centerX;
            int dxSq = dx * dx;
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                int dy = y - centerY;
                int dxySq = dxSq + dy * dy;
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    int dz = z - centerZ;
                    if (dxySq + dz * dz > radiusSq) {
                        continue;
                    }
                    // Re-fetch each iteration: the sickle may break mid-harvest.
                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (held.getType().isAir() || !ManualBreak.canBreakOneMore(player, held)) {
                        return;
                    }
                    Block block = world.getBlockAt(x, y, z);
                    if (CropHarvest.isMatureCrop(block)
                        && CropHarvest.harvestAndReplant(player, block, held)) {
                        ManualBreak.damageTool(player, held);
                    }
                }
            }
        }
    }
}
