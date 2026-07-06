package io.blume.resourcepack;

import io.blume.BlumePlugin;
import io.blume.config.BlumeConfig;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.UUID;

public final class ResourcePackService {

    private static final UUID PACK_ID = UUID.nameUUIDFromBytes(
        "blume-resource-pack".getBytes(java.nio.charset.StandardCharsets.UTF_8)
    );

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
        if (!config.isResourcePackEnabled()) {
            return;
        }
        if (!config.isResourcePackConfigured()) {
            plugin.getLogger().warning(
                "Resource pack enabled but url or sha1 is missing — not sending to " + player.getName()
            );
            return;
        }

        String url = config.getResourcePackUrl();
        String sha1 = config.getResourcePackSha1();
        ResourcePackInfo pack = ResourcePackInfo.resourcePackInfo()
            .id(PACK_ID)
            .uri(URI.create(url))
            .hash(sha1)
            .build();

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
            .packs(pack)
            .prompt(MiniMessage.miniMessage().deserialize(config.getResourcePackPrompt()))
            .required(config.isResourcePackRequired())
            .callback((audience, status, info) -> {
                if (status.intermediate()) {
                    return;
                }
                if (status == ResourcePackStatus.SUCCESSFULLY_LOADED) {
                    plugin.getLogger().info("Resource pack loaded for " + player.getName());
                    return;
                }
                plugin.getLogger().warning(
                    "Resource pack " + status + " for " + player.getName() + " (url=" + url + ", sha1=" + sha1 + ")"
                );
            })
            .build();

        plugin.getLogger().info("Sending resource pack to " + player.getName() + " from " + url);
        player.sendResourcePacks(request);
    }
}
