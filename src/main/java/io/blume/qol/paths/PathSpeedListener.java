package io.blume.qol.paths;

import io.blume.qol.QolConfig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public final class PathSpeedListener implements Listener {

    private static final int EFFECT_DURATION_TICKS = 40;

    private final QolConfig config;

    public PathSpeedListener(@NotNull QolConfig config) {
        this.config = config;
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
            return;
        }

        Block below = event.getTo().clone().subtract(0, 0.1, 0).getBlock();
        Material type = below.getType();

        if (config.pathSpeed2Blocks().contains(type)) {
            applySpeed(player, 1);
            return;
        }
        if (config.pathSpeed1Blocks().contains(type)) {
            applySpeed(player, 0);
            return;
        }
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    private void applySpeed(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED,
            EFFECT_DURATION_TICKS,
            amplifier,
            true,
            false,
            true
        ));
    }
}