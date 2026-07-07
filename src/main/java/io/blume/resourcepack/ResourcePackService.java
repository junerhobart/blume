package io.blume.resourcepack;

import io.blume.BlumePlugin;
import io.blume.resourcepack.JavaResourcePackSource.PackInfo;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.UUID;

public final class ResourcePackService {

    private static final UUID PACK_ID = UUID.nameUUIDFromBytes(
        "blume-resource-pack".getBytes(java.nio.charset.StandardCharsets.UTF_8)
    );

    private final BlumePlugin plugin;
    private String prompt;
    private boolean required;
    private PackInfo packInfo;

    public ResourcePackService(
        @NotNull BlumePlugin plugin,
        @Nullable JavaResourcePackSource host,
        @NotNull String prompt,
        boolean required
    ) {
        this.plugin = plugin;
        this.prompt = prompt;
        this.required = required;
        this.packInfo = host == null ? null : host.packInfo();
    }

    public void reload(@Nullable JavaResourcePackSource host, @NotNull String prompt, boolean required) {
        this.packInfo = host == null ? null : host.packInfo();
        this.prompt = prompt;
        this.required = required;
    }

    public void sendTo(@NotNull Player player) {
        if (packInfo == null) {
            return;
        }

        String url = packInfo.url();
        String sha1 = packInfo.sha1();
        ResourcePackInfo pack = ResourcePackInfo.resourcePackInfo()
            .id(PACK_ID)
            .uri(URI.create(url))
            .hash(sha1)
            .build();

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
            .packs(pack)
            .prompt(MiniMessage.miniMessage().deserialize(prompt))
            .required(required)
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
