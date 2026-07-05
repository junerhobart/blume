package io.blume.admin.command;

import io.blume.admin.AdminConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class CommandBroadcastListener implements Listener {

    private final AdminConfig config;

    public CommandBroadcastListener(@NotNull AdminConfig config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!config.isCommandBroadcastEnabled()) {
            return;
        }

        String message = event.getMessage().trim();
        if (message.isEmpty() || message.charAt(0) != '/') {
            return;
        }

        String commandLine = message.substring(1);
        String root = commandLine.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (config.commandBroadcastExcluded().contains(root)) {
            return;
        }

        MiniMessage miniMessage = MiniMessage.miniMessage();
        String formatted = config.commandBroadcastFormat()
            .replace("{player}", miniMessage.escapeTags(player.getName()))
            .replace("{command}", miniMessage.escapeTags(message));

        var component = miniMessage.deserialize(formatted);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(component);
        }
    }
}
