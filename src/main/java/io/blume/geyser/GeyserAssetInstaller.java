package io.blume.geyser;

import io.blume.config.BlumeConfig;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public final class GeyserAssetInstaller {

    private static final String GEYSER_PLUGIN = "Geyser-Spigot";
    private static final String PACK_RESOURCE = "bedrock-pack.zip";
    private static final String MAPPINGS_RESOURCE = "blume-geyser-mappings.json";
    private static final String PACK_FILE = "blume-bedrock.zip";
    private static final String MAPPINGS_FILE = "blume-items.json";

    private GeyserAssetInstaller() {
    }

    public static void install(@NotNull Plugin plugin, @NotNull BlumeConfig config, @NotNull Logger log) {
        if (!config.isBedrockPackEnabled()) {
            return;
        }
        if (plugin.getServer().getPluginManager().getPlugin(GEYSER_PLUGIN) == null) {
            log.warning("[Blume] Geyser not installed; Bedrock custom items disabled.");
            return;
        }

        Path geyserRoot = plugin.getDataFolder().toPath().getParent().resolve(GEYSER_PLUGIN);
        try {
            copyResource(plugin, PACK_RESOURCE, geyserRoot.resolve("packs").resolve(PACK_FILE));
            copyResource(plugin, MAPPINGS_RESOURCE, geyserRoot.resolve("custom_mappings").resolve(MAPPINGS_FILE));
            log.info("[Blume] Installed Bedrock pack and item mappings into Geyser folders.");
        } catch (IOException ex) {
            log.severe("[Blume] Failed to install Geyser Bedrock assets: " + ex.getMessage());
        }
    }

    private static void copyResource(
        @NotNull Plugin plugin,
        @NotNull String resource,
        @NotNull Path destination
    ) throws IOException {
        try (InputStream input = plugin.getResource(resource)) {
            if (input == null) {
                throw new IOException(resource + " missing from plugin jar");
            }
            Files.createDirectories(destination.getParent());
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
