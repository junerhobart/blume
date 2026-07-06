package io.blume.ecology.items;

import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ChickenSeedFeedListener implements Listener {

    private static final int LOVE_MODE_TICKS = 600;

    private final EcologyItems items;

    public ChickenSeedFeedListener(@NotNull EcologyItems items) {
        this.items = items;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFeedChicken(@NotNull PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Chicken chicken)) {
            return;
        }

        EquipmentSlot hand = event.getHand();
        ItemStack stack = hand == EquipmentSlot.OFF_HAND
            ? event.getPlayer().getInventory().getItemInOffHand()
            : event.getPlayer().getInventory().getItemInMainHand();
        if (!items.isAnyEcologySeed(stack)) {
            return;
        }
        if (chicken.getAge() < 0 || !chicken.canBreed() || chicken.isLoveMode()) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);
        chicken.setBreedCause(player.getUniqueId());
        chicken.setLoveModeTicks(LOVE_MODE_TICKS);
        chicken.playEffect(EntityEffect.LOVE_HEARTS);
        player.swingHand(hand);
        player.playSound(chicken.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
        consumeOne(player, hand);
    }

    private static void consumeOne(@NotNull Player player, @NotNull EquipmentSlot hand) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack stack = hand == EquipmentSlot.OFF_HAND
            ? player.getInventory().getItemInOffHand()
            : player.getInventory().getItemInMainHand();
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
        } else if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(ItemStack.empty());
        } else {
            player.getInventory().setItemInMainHand(ItemStack.empty());
        }
    }
}
