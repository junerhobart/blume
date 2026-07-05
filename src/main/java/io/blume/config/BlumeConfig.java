package io.blume.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class BlumeConfig {

    private static final String DEFAULT_PROMPT =
        "<green>Blume</green> uses a resource pack for custom items. Please accept.";

    private final boolean resourcePackEnabled;
    private final String resourcePackUrl;
    private final String resourcePackSha1;
    private final boolean resourcePackRequired;
    private final String resourcePackPrompt;

    public BlumeConfig(@NotNull FileConfiguration cfg, @NotNull Logger log) {
        resourcePackEnabled = cfg.getBoolean("resource-pack.enabled", true);
        resourcePackUrl = nullToEmpty(cfg.getString("resource-pack.url"));
        resourcePackSha1 = nullToEmpty(cfg.getString("resource-pack.sha1"));
        resourcePackRequired = cfg.getBoolean("resource-pack.required", false);
        resourcePackPrompt = cfg.getString("resource-pack.prompt", DEFAULT_PROMPT);

        if (resourcePackEnabled && resourcePackUrl.isBlank()) {
            log.warning("resource-pack.enabled is true but resource-pack.url is empty — pack will not be sent.");
        }
        if (resourcePackEnabled && !resourcePackUrl.isBlank() && resourcePackSha1.isBlank()) {
            log.warning("resource-pack.sha1 is empty — update plugins/Blume/config.yml after rebuilding the pack.");
        }
    }

    public boolean isResourcePackEnabled() {
        return resourcePackEnabled;
    }

    public boolean isResourcePackConfigured() {
        return !resourcePackUrl.isBlank() && !resourcePackSha1.isBlank();
    }

    public @NotNull String getResourcePackUrl() {
        return resourcePackUrl;
    }

    public @NotNull String getResourcePackSha1() {
        return resourcePackSha1;
    }

    public boolean isResourcePackRequired() {
        return resourcePackRequired;
    }

    public @NotNull String getResourcePackPrompt() {
        return resourcePackPrompt;
    }

    private static @NotNull String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
