package io.blume.listener;

import io.blume.BlumePlugin;
import io.blume.resourcepack.ResourcePackService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerJoinListener implements Listener {

    private final BlumePlugin plugin;
    private final ResourcePackService resourcePackService;

    public PlayerJoinListener(@NotNull BlumePlugin plugin, @NotNull ResourcePackService resourcePackService) {
        this.plugin = plugin;
        this.resourcePackService = resourcePackService;
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> resourcePackService.sendTo(event.getPlayer()),
            1L
        );
    }
}
