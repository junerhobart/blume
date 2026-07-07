package io.blume.admin.command;

import com.mojang.brigadier.context.CommandContext;
import io.blume.admin.graylist.GraylistService;
import io.blume.admin.luckperms.LuckPermsHook;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class VouchCommand {

    private final GraylistService graylist;
    private final LuckPermsHook luckPerms;
    private final Logger logger;
    private final Path logFile;

    public VouchCommand(
        @NotNull GraylistService graylist,
        @NotNull LuckPermsHook luckPerms,
        @NotNull Logger logger,
        @NotNull Path dataFolder
    ) {
        this.graylist = graylist;
        this.luckPerms = luckPerms;
        this.logger = logger;
        this.logFile = dataFolder.resolve("vouch-log.txt");
    }

    public int execute(@NotNull CommandContext<CommandSourceStack> ctx, @NotNull String targetName) {
        CommandSender sender = ctx.getSource().getSender();

        OfflinePlayer target = resolveTarget(targetName);
        UUID targetId = target.getUniqueId();
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Component.text("Unknown player: " + targetName, NamedTextColor.RED));
            return 0;
        }
        String resolvedName = target.getName() != null ? target.getName() : targetName;

        if (graylist.isVerified(targetId)) {
            sender.sendMessage(Component.text(resolvedName + " is already verified.", NamedTextColor.YELLOW));
            return 0;
        }

        if (!graylist.vouch(targetId)) {
            sender.sendMessage(Component.text("Could not verify " + resolvedName + ".", NamedTextColor.RED));
            return 0;
        }

        luckPerms.promoteToVerified(targetId);
        appendLog(sender, targetId, resolvedName);

        Player onlineTarget = Bukkit.getPlayer(targetId);
        if (onlineTarget != null) {
            if (luckPerms.isActive()) {
                graylist.removeAttachment(onlineTarget);
            } else {
                graylist.applyVerifiedPermission(onlineTarget, false);
            }
            onlineTarget.sendMessage(Component.text(
                "You have been verified by " + sender.getName() + ".",
                NamedTextColor.GREEN
            ));
        }

        sender.sendMessage(Component.text("Verified " + resolvedName + ".", NamedTextColor.GREEN));

        if (luckPerms.isConfigured() && !luckPerms.isActive()) {
            sender.sendMessage(Component.text(
                "LuckPerms group promotion is not active - check server log or admin.luckperms in config.",
                NamedTextColor.YELLOW
            ));
        }

        Bukkit.broadcast(Component.text(
            resolvedName + " was verified by " + sender.getName() + ".",
            NamedTextColor.GOLD
        ));

        return 1;
    }

    private @NotNull OfflinePlayer resolveTarget(@NotNull String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayer(name);
    }

    private void appendLog(@NotNull CommandSender voucher, @NotNull UUID targetId, @NotNull String targetName) {
        String voucherId = voucher instanceof Player player
            ? player.getUniqueId().toString()
            : "console";

        String line = Instant.now()
            + " | "
            + voucher.getName()
            + " ("
            + voucherId
            + ") vouched "
            + targetName
            + " ("
            + targetId
            + ")"
            + System.lineSeparator();

        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(
                logFile,
                line,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to write vouch log", e);
        }
    }
}
