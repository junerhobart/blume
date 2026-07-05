package io.blume.admin.graylist;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GraylistListener implements Listener {

    private final GraylistService graylist;

    public GraylistListener(@NotNull GraylistService graylist) {
        this.graylist = graylist;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (graylist.isRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (graylist.isRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (graylist.isRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (graylist.isRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!graylist.isRestricted(player)) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
            event.setCancelled(true);
            return;
        }

        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        if (isRestrictedItem(player.getInventory().getItemInMainHand().getType())) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            if (action == Action.RIGHT_CLICK_AIR) {
                event.setCancelled(true);
            }
            return;
        }

        if (!isAllowedInteraction(block.getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (graylist.isRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player player = resolvePlayer(event.getDamager());
        if (player != null && graylist.isRestricted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Player player = resolvePlayer(event.getRemover());
        if (player != null && graylist.isRestricted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        Player player = resolvePlayer(event.getEntity());
        if (player != null && graylist.isRestricted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Player player = resolvePlayer(event.getEntity());
        if (player != null && graylist.isRestricted(player)) {
            event.setCancelled(true);
            event.blockList().clear();
        }
    }

    private static boolean isAllowedInteraction(@NotNull Material material) {
        return Tag.DOORS.isTagged(material)
            || Tag.TRAPDOORS.isTagged(material)
            || Tag.FENCE_GATES.isTagged(material)
            || material == Material.LADDER
            || material == Material.VINE
            || material == Material.SCAFFOLDING
            || material == Material.TWISTING_VINES
            || material == Material.TWISTING_VINES_PLANT
            || material == Material.WEEPING_VINES
            || material == Material.WEEPING_VINES_PLANT
            || material == Material.CAVE_VINES
            || material == Material.CAVE_VINES_PLANT;
    }

    private static boolean isRestrictedItem(@NotNull Material material) {
        return material == Material.FLINT_AND_STEEL
            || material == Material.FIRE_CHARGE
            || material == Material.END_CRYSTAL
            || material.name().endsWith("_BUCKET")
            || Tag.BEDS.isTagged(material)
            || Tag.ITEMS_BOATS.isTagged(material);
    }

    private static @Nullable Player resolvePlayer(@Nullable Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
