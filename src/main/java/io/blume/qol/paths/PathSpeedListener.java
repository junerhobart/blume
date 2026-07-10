package io.blume.qol.paths;

import io.blume.BlumePlugin;
import io.blume.qol.QolConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PathSpeedListener implements Listener {

    private static final double SPEED_1_BONUS = 0.2;
    private static final double SPEED_2_BONUS = 0.4;

    private final QolConfig config;
    private final NamespacedKey speedKey;

    public PathSpeedListener(@NotNull BlumePlugin plugin, @NotNull QolConfig config) {
        this.config = config;
        this.speedKey = new NamespacedKey(plugin, "path_speed");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
        if (player.isFlying() || player.isGliding()) {
            clearPathSpeed(player);
            return;
        }

        Block below = event.getTo().clone().subtract(0, 0.1, 0).getBlock();
        var type = below.getType();

        if (config.pathSpeed2Blocks().contains(type)) {
            applyPathSpeed(player, SPEED_2_BONUS);
            return;
        }
        if (config.pathSpeed1Blocks().contains(type)) {
            applyPathSpeed(player, SPEED_1_BONUS);
            return;
        }
        clearPathSpeed(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPathSpeed(event.getPlayer());
    }

    private void applyPathSpeed(@NotNull Player player, double bonus) {
        AttributeInstance movement = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        clearPathSpeed(player);
        movement.addModifier(new AttributeModifier(
            speedKey,
            bonus,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1
        ));
    }

    private void clearPathSpeed(@NotNull Player player) {
        AttributeInstance movement = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        movement.getModifiers().stream()
            .filter(modifier -> speedKey.equals(modifier.getKey()))
            .forEach(movement::removeModifier);
    }
}
