package io.blume.admin.graylist;

import io.blume.BlumePlugin;
import io.blume.admin.AdminConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class GraylistService {

    public static final String VERIFIED_PERMISSION = "blume.verified";

    private final BlumePlugin plugin;
    private AdminConfig config;
    private final Set<UUID> verifiedPlayers = new HashSet<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private File dataFile;

    public GraylistService(@NotNull BlumePlugin plugin, @NotNull AdminConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void load() {
        verifiedPlayers.clear();
        dataFile = new File(plugin.getDataFolder(), "verified-players.yml");

        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        List<String> uuids = yaml.getStringList("verified");
        for (String raw : uuids) {
            try {
                verifiedPlayers.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid UUID in verified-players.yml: " + raw);
            }
        }
    }

    public void reload(@NotNull AdminConfig newConfig) {
        this.config = newConfig;
        load();
    }

    public boolean isVerified(@NotNull UUID uuid) {
        return verifiedPlayers.contains(uuid);
    }

    public boolean isRestricted(@NotNull Player player) {
        if (!config.isGraylistEnabled()) {
            return false;
        }
        if (player.isOp()) {
            return false;
        }
        if (isVerified(player.getUniqueId())) {
            return false;
        }
        return !player.hasPermission(VERIFIED_PERMISSION);
    }

    public void applyVerifiedPermission(@NotNull Player player, boolean luckPermsActive) {
        if (!isVerified(player.getUniqueId()) || luckPermsActive) {
            return;
        }
        if (player.hasPermission(VERIFIED_PERMISSION)) {
            return;
        }
        attachments.computeIfAbsent(
            player.getUniqueId(),
            uuid -> player.addAttachment(plugin, VERIFIED_PERMISSION, true)
        );
    }

    public void removeAttachment(@NotNull Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            attachment.remove();
        }
    }

    public void syncOnlineVerified(boolean luckPermsActive) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!isVerified(player.getUniqueId())) {
                continue;
            }
            if (luckPermsActive) {
                removeAttachment(player);
            } else {
                applyVerifiedPermission(player, false);
            }
        }
    }

    public void clearAttachments() {
        for (PermissionAttachment attachment : attachments.values()) {
            attachment.remove();
        }
        attachments.clear();
    }

    public boolean vouch(@NotNull UUID uuid) {
        if (verifiedPlayers.add(uuid)) {
            save();
            return true;
        }
        return false;
    }

    private void save() {
        if (dataFile == null) {
            dataFile = new File(plugin.getDataFolder(), "verified-players.yml");
        }

        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("verified", verifiedPlayers.stream().map(UUID::toString).sorted().toList());

        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save verified-players.yml", e);
        }
    }

    public @NotNull Set<UUID> verifiedPlayers() {
        return Collections.unmodifiableSet(verifiedPlayers);
    }
}
