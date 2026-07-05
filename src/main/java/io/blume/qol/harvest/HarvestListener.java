package io.blume.qol.harvest;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class HarvestListener implements Listener {

    private static final Set<Material> CROPS = EnumSet.of(
        Material.WHEAT,
        Material.BEETROOTS,
        Material.CARROTS,
        Material.POTATOES,
        Material.NETHER_WART
    );

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
        if (requireEmptyHand && player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!CROPS.contains(block.getType())) {
            return;
        }
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return;
        }
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        event.setCancelled(true);

        List<ItemStack> drops = new ArrayList<>(block.getDrops(player.getInventory().getItemInMainHand()));
        ReplantInfo replant = replantInfo(block.getType());
        if (replant == null) {
            dropAll(player, block, drops);
            block.breakNaturally(player.getInventory().getItemInMainHand(), true);
            return;
        }

        if (!consumeOne(drops, replant.material())) {
            dropAll(player, block, drops);
            block.breakNaturally(player.getInventory().getItemInMainHand(), true);
            return;
        }

        dropAll(player, block, drops);
        ageable.setAge(0);
        block.setBlockData(ageable, false);
    }

    private void dropAll(Player player, Block block, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) {
                continue;
            }
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
        }
    }

    private static boolean consumeOne(List<ItemStack> drops, Material material) {
        for (ItemStack drop : drops) {
            if (drop != null && drop.getType() == material && drop.getAmount() > 0) {
                drop.setAmount(drop.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static ReplantInfo replantInfo(Material crop) {
        return switch (crop) {
            case WHEAT -> new ReplantInfo(Material.WHEAT_SEEDS);
            case BEETROOTS -> new ReplantInfo(Material.BEETROOT_SEEDS);
            case CARROTS -> new ReplantInfo(Material.CARROT);
            case POTATOES -> new ReplantInfo(Material.POTATO);
            case NETHER_WART -> new ReplantInfo(Material.NETHER_WART);
            default -> null;
        };
    }

    private record ReplantInfo(Material material) {}
}
