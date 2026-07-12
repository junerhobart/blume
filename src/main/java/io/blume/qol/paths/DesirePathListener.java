package io.blume.qol.paths;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

public final class DesirePathListener implements Listener {

    private final DesirePathService service;

    public DesirePathListener(@NotNull DesirePathService service) {
        this.service = service;
    }

    // NORMAL, not MONITOR: this handler mutates the world (block trample/erosion),
    // which is forbidden for MONITOR-level listeners.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isInsideVehicle()) {
            return;
        }

        Block ground = event.getTo().clone().subtract(0, 1, 0).getBlock();
        service.recordWalk(ground);
    }
}
