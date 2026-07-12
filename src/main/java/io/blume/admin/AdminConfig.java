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
        "<white>{player}</white>: <yellow>{command}</yellow>";
    private static final String DEFAULT_GRAYLIST_JOIN_MESSAGE =
        "<yellow>You are graylisted.</yellow> A verified member must vouch for you before you can "
            + "interact with the world. They can run <white>/vouch {username}</white>.";

    private final boolean graylistEnabled;
    private final boolean graylistJoinMessageEnabled;
    private final String graylistJoinMessage;
    private final boolean commandBroadcastEnabled;
    private final String commandBroadcastFormat;
    private final Set<String> commandBroadcastCommands;
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
        commandBroadcastCommands = parseBroadcastCommands(admin);

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

    public boolean shouldBroadcastCommand(@NotNull String message) {
        String trimmed = message.trim();
        if (trimmed.startsWith("//")) {
            return true;
        }
        if (trimmed.isEmpty() || trimmed.charAt(0) != '/') {
            return false;
        }
        String commandLine = trimmed.substring(1);
        String root = normalizeCommandRoot(commandLine.split("\\s+", 2)[0]);
        return commandBroadcastCommands.contains(root);
    }

    public @NotNull String commandBroadcastFormat() {
        return commandBroadcastFormat;
    }

    public @NotNull Set<String> commandBroadcastCommands() {
        return commandBroadcastCommands;
    }

    public boolean isLuckPermsEnabled() {
        return luckPermsEnabled;
    }

    public @NotNull String luckPermsVerifiedGroup() {
        return luckPermsVerifiedGroup;
    }

    private static @NotNull Set<String> parseBroadcastCommands(ConfigurationSection admin) {
        // Defaults apply only when the key is absent. An explicitly empty list
        // means the admin wants no command broadcasts.
        if (admin == null || !admin.contains("command-broadcast.commands")) {
            return defaultCheatCommands();
        }

        Set<String> commands = new HashSet<>();
        for (String command : admin.getStringList("command-broadcast.commands")) {
            commands.add(normalizeCommandRoot(command));
        }
        return commands;
    }

    private static @NotNull Set<String> defaultCheatCommands() {
        Set<String> commands = new HashSet<>();
        commands.add("gamemode");
        commands.add("gm");
        commands.add("give");
        commands.add("clear");
        commands.add("item");
        commands.add("tp");
        commands.add("teleport");
        commands.add("effect");
        commands.add("enchant");
        commands.add("experience");
        commands.add("xp");
        commands.add("fill");
        commands.add("setblock");
        commands.add("clone");
        commands.add("summon");
        commands.add("kill");
        commands.add("ride");
        commands.add("damage");
        commands.add("time");
        commands.add("weather");
        commands.add("difficulty");
        commands.add("gamerule");
        commands.add("defaultgamemode");
        commands.add("spawnpoint");
        commands.add("setworldspawn");
        commands.add("spreadplayers");
        commands.add("worldborder");
        commands.add("data");
        commands.add("attribute");
        commands.add("loot");
        commands.add("seed");
        commands.add("locate");

        commands.add("fly");
        commands.add("god");
        commands.add("heal");
        commands.add("feed");
        commands.add("speed");
        commands.add("invsee");
        commands.add("ecsee");
        commands.add("eecsee");
        commands.add("endersee");
        commands.add("openinv");
        commands.add("openender");
        commands.add("vanish");
        commands.add("v");
        commands.add("socialspy");
        commands.add("tphere");
        commands.add("gms");
        commands.add("gmc");
        commands.add("gma");
        commands.add("gmsp");
        commands.add("spectate");
        commands.add("spec");
        commands.add("worldedit");
        commands.add("we");
        commands.add("wand");
        commands.add("brush");
        commands.add("br");
        commands.add("tool");
        commands.add("schematic");
        commands.add("schem");
        commands.add("stack");
        commands.add("undo");
        commands.add("redo");
        return commands;
    }

    static @NotNull String normalizeCommandRoot(@NotNull String command) {
        String root = command.toLowerCase(Locale.ROOT).trim();
        int namespace = root.indexOf(':');
        if (namespace >= 0) {
            root = root.substring(namespace + 1);
        }
        return root;
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
