package io.blume.qol.death;

import io.blume.BlumePlugin;
import io.blume.qol.QolConfig;
import io.blume.qol.QolKeys;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public final class DeathDropListener implements Listener {

    private final BlumePlugin plugin;
    private final QolConfig config;
    private final QolKeys keys;
    private BukkitTask cleanupTask;

    public DeathDropListener(
        @NotNull BlumePlugin plugin,
        @NotNull QolConfig config,
        @NotNull QolKeys keys
    ) {
        this.plugin = plugin;
        this.config = config;
        this.keys = keys;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        long expiryMs = System.currentTimeMillis() + config.deathDropLifetimeMinutes() * 60_000L;
        for (ItemStack stack : event.getDrops()) {
            tagStack(stack, expiryMs);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        Long expiry = item.getItemStack().getPersistentDataContainer().get(
            keys.deathDropExpiry(),
            PersistentDataType.LONG
        );
        if (expiry != null) {
            item.getPersistentDataContainer().set(
                keys.deathDropExpiry(),
                PersistentDataType.LONG,
                expiry
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();
        Long expiry = item.getPersistentDataContainer().get(keys.deathDropExpiry(), PersistentDataType.LONG);
        if (expiry == null) {
            expiry = item.getItemStack().getPersistentDataContainer().get(
                keys.deathDropExpiry(),
                PersistentDataType.LONG
            );
        }
        if (expiry == null) {
            return;
        }
        if (System.currentTimeMillis() < expiry) {
            event.setCancelled(true);
        } else {
            item.remove();
        }
    }

    public void startCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        long intervalTicks = 20L * 60L * 5L;
        cleanupTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Item item : plugin.getServer().getWorlds().stream()
                .flatMap(world -> world.getEntitiesByClass(Item.class).stream())
                .toList()) {
                Long expiry = item.getPersistentDataContainer().get(keys.deathDropExpiry(), PersistentDataType.LONG);
                if (expiry == null) {
                    expiry = item.getItemStack().getPersistentDataContainer().get(
                        keys.deathDropExpiry(),
                        PersistentDataType.LONG
                    );
                }
                if (expiry != null && now >= expiry) {
                    item.remove();
                }
            }
        }, intervalTicks, intervalTicks);
    }

    public void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    private void tagStack(ItemStack stack, long expiryMs) {
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(
            keys.deathDropExpiry(),
            PersistentDataType.LONG,
            expiryMs
        ));
    }
}
