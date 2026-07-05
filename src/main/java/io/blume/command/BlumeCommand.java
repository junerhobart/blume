package io.blume.command;

import io.blume.BlumePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class BlumeCommand {

    private final BlumePlugin plugin;

    public BlumeCommand(@NotNull BlumePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            sender.sendMessage(Component.text("[Blume] Config reloaded.", NamedTextColor.GREEN));
            return;
        }

        sendHelp(sender);
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("[Blume] Commands:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  /blume reload  - Reload config and re-send resource pack", NamedTextColor.YELLOW));
    }
}
