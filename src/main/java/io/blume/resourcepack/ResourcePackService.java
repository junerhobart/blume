package io.blume.resourcepack;

import io.blume.BlumePlugin;
import io.blume.config.BlumeConfig;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.logging.Level;

public final class ResourcePackService {

    private final BlumePlugin plugin;
    private BlumeConfig config;

    public ResourcePackService(@NotNull BlumePlugin plugin, @NotNull BlumeConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void reload(@NotNull BlumeConfig config) {
        this.config = config;
    }

    public void sendTo(@NotNull Player player) {
        if (!config.isResourcePackEnabled() || !config.isResourcePackConfigured()) {
            return;
        }

        ResourcePackInfo.Builder packBuilder = ResourcePackInfo.resourcePackInfo()
            .uri(URI.create(config.getResourcePackUrl()));

        String sha1 = config.getResourcePackSha1();
        if (!sha1.isBlank()) {
            packBuilder.hash(sha1);
        }

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
            .packs(packBuilder.build())
            .prompt(MiniMessage.miniMessage().deserialize(config.getResourcePackPrompt()))
            .required(config.isResourcePackRequired())
            .callback((audience, status, info) -> {
                if (plugin.getLogger().isLoggable(Level.FINE)) {
                    plugin.getLogger().fine("Resource pack status for " + player.getName() + ": " + status);
                }
            })
            .build();

        player.sendResourcePacks(request);
    }
}
