package io.blume.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class BlumeConfig {

    private static final String DEFAULT_PROMPT =
        "<green>Blume</green> uses a resource pack for custom items. Please accept.";
    private static final int DEFAULT_PACK_PORT = 8765;

    private final boolean resourcePackEnabled;
    private final String resourcePackUrl;
    private final boolean resourcePackBuiltinHost;
    private final String resourcePackHost;
    private final int resourcePackPort;
    private final boolean resourcePackRequired;
    private final String resourcePackPrompt;
    private final boolean bedrockPackEnabled;
    private final boolean updateCheckEnabled;
    private final String updateGithubRepo;

    public BlumeConfig(
        @NotNull FileConfiguration cfg,
        @NotNull String pluginVersion,
        @NotNull Logger log
    ) {
        resourcePackEnabled = cfg.getBoolean("resource-pack.enabled", true);
        updateGithubRepo = nullToEmpty(cfg.getString("updates.github-repo", "junerhobart/blume"));
        String rawPackUrl = nullToEmpty(cfg.getString("resource-pack.url"));
        resourcePackUrl = ConfigPlaceholders.resolvePackUrl(rawPackUrl, pluginVersion, updateGithubRepo);
        resourcePackBuiltinHost = cfg.getBoolean("resource-pack.builtin-host", false);
        resourcePackHost = nullToEmpty(cfg.getString("resource-pack.host"));
        resourcePackPort = cfg.getInt("resource-pack.port", DEFAULT_PACK_PORT);
        resourcePackRequired = cfg.getBoolean("resource-pack.required", true);
        resourcePackPrompt = cfg.getString("resource-pack.prompt", DEFAULT_PROMPT);
        bedrockPackEnabled = cfg.getBoolean("resource-pack.bedrock.enabled", true);
        updateCheckEnabled = cfg.getBoolean("updates.check", true);

        if (resourcePackEnabled && resourcePackPort <= 0) {
            log.warning("resource-pack.port must be positive. Using " + DEFAULT_PACK_PORT + ".");
        }
    }

    public boolean isResourcePackEnabled() {
        return resourcePackEnabled;
    }

    public @NotNull String getResourcePackUrl() {
        return resourcePackUrl;
    }

    public boolean isResourcePackBuiltinHost() {
        return resourcePackBuiltinHost;
    }

    public @NotNull String getResourcePackHost() {
        return resourcePackHost;
    }

    public int getResourcePackPort() {
        return resourcePackPort > 0 ? resourcePackPort : DEFAULT_PACK_PORT;
    }

    public boolean isResourcePackRequired() {
        return resourcePackRequired;
    }

    public @NotNull String getResourcePackPrompt() {
        return resourcePackPrompt;
    }

    public boolean isBedrockPackEnabled() {
        return bedrockPackEnabled;
    }

    public boolean isUpdateCheckEnabled() {
        return updateCheckEnabled;
    }

    public @NotNull String getUpdateGithubRepo() {
        return updateGithubRepo;
    }

    public boolean isAutoManagedPackUrl(@NotNull String rawUrl) {
        return ConfigPlaceholders.isAutoManagedPackUrl(rawUrl, updateGithubRepo);
    }

    private static @NotNull String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
