package io.blume.geyser;

import io.blume.config.BlumeConfig;
import org.bukkit.plugin.Plugin;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreReloadEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class GeyserIntegration implements EventRegistrar {

    static final String BEDROCK_FAILURE =
        "[Blume] Bedrock texture pack won't load. Check Geyser is installed and custom content is enabled.";

    private static final String GEYSER_PLUGIN = "Geyser-Spigot";

    private final Plugin plugin;
    private final Logger log;
    private BlumeConfig config;
    private boolean registered;
    private boolean customItemsEventFired;
    private boolean postInitVerified;

    public GeyserIntegration(@NotNull Plugin plugin, @NotNull BlumeConfig config) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.config = config;
    }

    public void enable() {
        customItemsEventFired = false;
        postInitVerified = false;
        unregisterFromGeyser();
        registered = false;

        if (!config.isBedrockPackEnabled()) {
            return;
        }

        if (!isGeyserPresent()) {
            logBedrockFailure();
            return;
        }

        if (config.getBedrockPackUrl().isBlank()) {
            logBedrockFailure();
            return;
        }

        try {
            GeyserApi.api().eventBus().register(this, this);
            registered = true;
        } catch (Exception | LinkageError ex) {
            logBedrockFailure();
        }
    }

    public void disable() {
        unregisterFromGeyser();
        registered = false;
        customItemsEventFired = false;
        postInitVerified = false;
    }

    public void reload(@NotNull BlumeConfig config) {
        disable();
        this.config = config;
        enable();
    }

    public void setConfig(@NotNull BlumeConfig config) {
        this.config = config;
    }

    public boolean isRegistered() {
        return registered;
    }

    @Subscribe
    public void onDefineCustomItems(@NotNull GeyserDefineCustomItemsEvent event) {
        if (!config.isBedrockPackEnabled()) {
            return;
        }
        customItemsEventFired = true;
        for (GeyserItemDefinitions.SeedDefinition seed : GeyserItemDefinitions.SEEDS) {
            event.register(
                GeyserItemDefinitions.WHEAT_SEEDS,
                GeyserItemDefinitions.toDefinition(seed)
            );
        }
    }

    @Subscribe
    public void onDefineResourcePacks(@NotNull GeyserDefineResourcePacksEvent event) {
        if (!config.isBedrockPackEnabled() || config.getBedrockPackUrl().isBlank()) {
            return;
        }
        ResourcePack pack = ResourcePack.create(PackCodec.url(config.getBedrockPackUrl()));
        event.register(pack);
    }

    @Subscribe
    public void onPreReload(@NotNull GeyserPreReloadEvent event) {
        customItemsEventFired = false;
        postInitVerified = false;
    }

    @Subscribe
    public void onPostInitialize(@NotNull GeyserPostInitializeEvent event) {
        verifyBedrockRegistration();
    }

    private void verifyBedrockRegistration() {
        if (postInitVerified) {
            return;
        }
        postInitVerified = true;

        if (!config.isBedrockPackEnabled()) {
            return;
        }
        if (!isGeyserPresent() || !registered || !customItemsEventFired) {
            logBedrockFailure();
        }
    }

    private boolean isGeyserPresent() {
        return plugin.getServer().getPluginManager().getPlugin(GEYSER_PLUGIN) != null;
    }

    private void unregisterFromGeyser() {
        if (!isGeyserPresent()) {
            return;
        }
        try {
            GeyserApi.api().eventBus().unregisterAll(this);
        } catch (Exception | LinkageError ignored) {
            // Geyser may be shutting down
        }
    }

    private void logBedrockFailure() {
        log.severe(BEDROCK_FAILURE);
    }
}
