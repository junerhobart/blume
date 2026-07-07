// Adapted from Vane (MIT) - https://github.com/oddlama/vane
// Copyright (c) 2020 oddlama

package io.blume.qol.doubledoors;

import io.blume.BlumePlugin;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public final class DoubleDoorsListener implements Listener {

    private final BlumePlugin plugin;

    public DoubleDoorsListener(@NotNull BlumePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.hasBlock() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getPlayer().isSneaking()) {
                return;
            }
            handleDoubleDoor(event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        handleDoubleDoor(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        int now = event.getNewCurrent();
        int old = event.getOldCurrent();
        if (now != old && (now == 0 || old == 0)) {
            handleDoubleDoor(event.getBlock());
        }
    }

    private void handleDoubleDoor(Block block) {
        SingleDoor first = SingleDoor.createFromBlock(block);
        if (first == null) {
            return;
        }
        SingleDoor second = first.getSecondDoor();
        if (second == null) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!first.updateCachedState() || !second.updateCachedState()) {
                return;
            }
            second.setOpen(first.isOpen());
        });
    }
}
