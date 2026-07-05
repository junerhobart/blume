package io.blume.admin;

import io.blume.BlumePlugin;
import io.blume.admin.command.CommandBroadcastListener;
import io.blume.admin.command.VouchCommand;
import io.blume.admin.graylist.GraylistListener;
import io.blume.admin.graylist.GraylistService;
import io.blume.admin.luckperms.LuckPermsHook;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AdminModule {

    private final BlumePlugin plugin;
    private AdminConfig config;
    private final GraylistService graylistService;
    private final LuckPermsHook luckPermsHook;
    private VouchCommand vouchCommand;
    private final List<Listener> listeners = new ArrayList<>();

    public AdminModule(@NotNull BlumePlugin plugin, @NotNull AdminConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.graylistService = new GraylistService(plugin, config);
        this.luckPermsHook = new LuckPermsHook(plugin, config);
    }

    public void enable() {
        PluginManager pm = plugin.getServer().getPluginManager();

        graylistService.load();
        luckPermsHook.reload(config);

        vouchCommand = new VouchCommand(
            graylistService,
            luckPermsHook,
            plugin.getLogger(),
            plugin.getDataFolder().toPath()
        );

        if (config.isGraylistEnabled()) {
            register(new GraylistListener(graylistService));
            register(new JoinListener());
        }

        if (config.isCommandBroadcastEnabled()) {
            register(new CommandBroadcastListener(config));
        }

        for (Listener listener : listeners) {
            pm.registerEvents(listener, plugin);
        }

        luckPermsHook.syncVerifiedPlayers(graylistService.verifiedPlayers());
        graylistService.syncOnlineVerified(luckPermsHook.isActive());
    }

    public void disable() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
        graylistService.clearAttachments();
        vouchCommand = null;
    }

    public void reload(@NotNull AdminConfig newConfig) {
        disable();
        this.config = newConfig;
        graylistService.reload(newConfig);
        enable();
    }

    public @NotNull VouchCommand vouchCommand() {
        if (vouchCommand == null) {
            throw new IllegalStateException("AdminModule is not enabled");
        }
        return vouchCommand;
    }

    public @NotNull GraylistService graylistService() {
        return graylistService;
    }

    private void register(Listener listener) {
        listeners.add(listener);
    }

    private final class JoinListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            if (graylistService.isVerified(uuid)) {
                luckPermsHook.promoteToVerified(uuid);
                graylistService.applyVerifiedPermission(player, luckPermsHook.isActive());
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!graylistService.isRestricted(player)) {
                    return;
                }
                if (!config.isGraylistJoinMessageEnabled()) {
                    return;
                }
                MiniMessage miniMessage = MiniMessage.miniMessage();
                String escapedName = miniMessage.escapeTags(player.getName());
                String text = config.graylistJoinMessage()
                    .replace("{username}", escapedName)
                    .replace("<username>", escapedName);
                player.sendMessage(miniMessage.deserialize(text));
            });
        }
    }
}
