package io.blume.admin.luckperms;

import io.blume.BlumePlugin;
import io.blume.admin.AdminConfig;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;

public final class LuckPermsHook {

    private static final String LP_PROVIDER_CLASS = "net.luckperms.api.LuckPermsProvider";

    private final BlumePlugin plugin;
    private AdminConfig config;
    private VerifiedGroupPromoter promoter;

    public LuckPermsHook(@NotNull BlumePlugin plugin, @NotNull AdminConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void enable() {
        promoter = null;
        if (!config.isLuckPermsEnabled()) {
            return;
        }

        String group = config.luckPermsVerifiedGroup();
        if (group.isBlank()) {
            plugin.getLogger().warning(
                "[Blume] admin.luckperms.enabled is true but verified-group is empty. "
                    + "Create a group (/lp creategroup verified), set verified-group in config, then /blume reload."
            );
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            plugin.getLogger().warning(
                "[Blume] LuckPerms integration is enabled but LuckPerms is not installed. "
                    + "Install LuckPerms from https://luckperms.net/download or set admin.luckperms.enabled to false. "
                    + "Graylist still works without it."
            );
            return;
        }

        try {
            Class.forName(LP_PROVIDER_CLASS);
            promoter = new LuckPermsBridge(plugin);
            plugin.getLogger().info(
                "[Blume] LuckPerms integration active — vouched players join group '" + group + "'."
            );
        } catch (LinkageError | Exception e) {
            plugin.getLogger().log(
                Level.WARNING,
                "[Blume] LuckPerms is installed but its API is not available to Blume. "
                    + "Restart the server after installing LuckPerms, or check paper-plugin.yml join-classpath.",
                e
            );
        }
    }

    public void reload(@NotNull AdminConfig newConfig) {
        this.config = newConfig;
        enable();
    }

    public boolean isConfigured() {
        return config.isLuckPermsEnabled() && !config.luckPermsVerifiedGroup().isBlank();
    }

    public boolean isActive() {
        return promoter != null && promoter.isActive();
    }

    public void promoteToVerified(@NotNull UUID uuid) {
        if (!isActive()) {
            return;
        }
        promoter.promoteToVerified(uuid, config.luckPermsVerifiedGroup());
    }

    public void syncVerifiedPlayers(@NotNull Iterable<UUID> verified) {
        if (!isActive()) {
            return;
        }
        int synced = 0;
        for (UUID uuid : verified) {
            promoteToVerified(uuid);
            synced++;
        }
        if (synced > 0) {
            plugin.getLogger().info(
                "[Blume] Synced " + synced + " verified player(s) to LuckPerms group '"
                    + config.luckPermsVerifiedGroup()
                    + "'."
            );
        }
    }
}
