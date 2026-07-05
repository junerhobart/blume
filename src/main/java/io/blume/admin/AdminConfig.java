package io.blume.admin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AdminConfig {

    private static final String DEFAULT_COMMAND_FORMAT =
        "<gray>[Cmd]</gray> <white>{player}</white>: <yellow>{command}</yellow>";
    private static final String DEFAULT_GRAYLIST_JOIN_MESSAGE =
        "<yellow>You are graylisted.</yellow> You need to be vouched for by another member. "
            + "They can do this by typing <white>/vouch {username}</white>.";

    private final boolean graylistEnabled;
    private final boolean graylistJoinMessageEnabled;
    private final String graylistJoinMessage;
    private final boolean commandBroadcastEnabled;
    private final String commandBroadcastFormat;
    private final Set<String> commandBroadcastExcluded;
    private final boolean luckPermsEnabled;
    private final String luckPermsVerifiedGroup;

    public AdminConfig(@NotNull FileConfiguration cfg) {
        ConfigurationSection admin = cfg.getConfigurationSection("admin");

        graylistEnabled = sectionBool(admin, "graylist.enabled", true);
        graylistJoinMessageEnabled = sectionBool(admin, "graylist.join-message.enabled", true);
        graylistJoinMessage = sectionString(
            admin,
            "graylist.join-message.text",
            DEFAULT_GRAYLIST_JOIN_MESSAGE
        );

        commandBroadcastEnabled = sectionBool(admin, "command-broadcast.enabled", true);
        commandBroadcastFormat = sectionString(
            admin,
            "command-broadcast.format",
            DEFAULT_COMMAND_FORMAT
        );
        commandBroadcastExcluded = parseExcludedCommands(admin);

        luckPermsEnabled = sectionBool(admin, "luckperms.enabled", false);
        luckPermsVerifiedGroup = sectionString(admin, "luckperms.verified-group", "");
    }

    public boolean isGraylistEnabled() {
        return graylistEnabled;
    }

    public boolean isGraylistJoinMessageEnabled() {
        return graylistJoinMessageEnabled;
    }

    public @NotNull String graylistJoinMessage() {
        return graylistJoinMessage;
    }

    public boolean isCommandBroadcastEnabled() {
        return commandBroadcastEnabled;
    }

    public @NotNull String commandBroadcastFormat() {
        return commandBroadcastFormat;
    }

    public @NotNull Set<String> commandBroadcastExcluded() {
        return commandBroadcastExcluded;
    }

    public boolean isLuckPermsEnabled() {
        return luckPermsEnabled;
    }

    public @NotNull String luckPermsVerifiedGroup() {
        return luckPermsVerifiedGroup;
    }

    private static @NotNull Set<String> parseExcludedCommands(ConfigurationSection admin) {
        Set<String> excluded = new HashSet<>();
        excluded.add("msg");
        excluded.add("tell");
        excluded.add("w");
        excluded.add("r");
        excluded.add("me");

        if (admin == null) {
            return excluded;
        }

        List<String> configured = admin.getStringList("command-broadcast.excluded-commands");
        if (configured.isEmpty()) {
            return excluded;
        }

        excluded.clear();
        for (String command : configured) {
            excluded.add(command.toLowerCase(Locale.ROOT));
        }
        return excluded;
    }

    private static boolean sectionBool(ConfigurationSection section, @NotNull String path, boolean def) {
        return section == null ? def : section.getBoolean(path, def);
    }

    private static @NotNull String sectionString(
        ConfigurationSection section,
        @NotNull String path,
        @NotNull String def
    ) {
        if (section == null) {
            return def;
        }
        String value = section.getString(path);
        return value == null || value.isBlank() ? def : value;
    }
}
