package io.blume;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.blume.admin.AdminConfig;
import io.blume.admin.AdminModule;
import io.blume.enchants.EnchantsConfig;
import io.blume.enchants.EnchantsModule;
import io.blume.ecology.EcologyConfig;
import io.blume.ecology.EcologyModule;
import io.blume.ecology.command.EcologyGiveCommand;
import io.blume.command.BlumeCommand;
import io.blume.config.BlumeConfig;
import io.blume.listener.PlayerJoinListener;
import io.blume.qol.QolConfig;
import io.blume.qol.QolModule;
import io.blume.resourcepack.ResourcePackService;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class BlumePlugin extends JavaPlugin {

    private BlumeConfig blumeConfig;
    private QolConfig qolConfig;
    private AdminConfig adminConfig;
    private ResourcePackService resourcePackService;
    private QolModule qolModule;
    private AdminModule adminModule;
    private EnchantsConfig enchantsConfig;
    private EnchantsModule enchantsModule;
    private EcologyConfig ecologyConfig;
    private EcologyModule ecologyModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        blumeConfig = new BlumeConfig(getConfig(), getLogger());
        qolConfig = new QolConfig(getConfig());
        adminConfig = new AdminConfig(getConfig());
        enchantsConfig = new EnchantsConfig(getConfig());
        ecologyConfig = new EcologyConfig(getConfig());
        resourcePackService = new ResourcePackService(this, blumeConfig);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, resourcePackService), this);
        getServer().getScheduler().runTaskLater(this, this::sendResourcePackToOnlinePlayers, 1L);

        qolModule = new QolModule(this, qolConfig);
        qolModule.enable();

        adminModule = new AdminModule(this, adminConfig);
        adminModule.enable();

        enchantsModule = new EnchantsModule(this, enchantsConfig);
        enchantsModule.enable();

        ecologyModule = new EcologyModule(this, ecologyConfig);
        ecologyModule.enable();

        BlumeCommand cmd = new BlumeCommand(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                Commands.literal("blume")
                    .requires(src -> src.getSender().hasPermission("blume.admin"))
                    .executes(ctx -> { cmd.execute(ctx.getSource().getSender(), new String[0]); return 1; })
                    .then(Commands.literal("reload")
                        .executes(ctx -> { cmd.execute(ctx.getSource().getSender(), new String[]{"reload"}); return 1; }))
                    .then(EcologyGiveCommand.literal(this))
                    .build(),
                "Blume admin commands."
            );

            event.registrar().register(
                Commands.literal("vouch")
                    .requires(src -> src.getSender().hasPermission("blume.vouch"))
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                String name = online.getName();
                                if (remaining.isEmpty()
                                    || name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                                    builder.suggest(name);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> adminModule.vouchCommand().execute(
                            ctx,
                            StringArgumentType.getString(ctx, "player")
                        )))
                    .build(),
                "Verify a player and remove graylist restrictions."
            );
        });
    }

    @Override
    public void onDisable() {
        if (adminModule != null) {
            adminModule.disable();
            adminModule = null;
        }
        if (enchantsModule != null) {
            enchantsModule.disable();
            enchantsModule = null;
        }
        if (ecologyModule != null) {
            ecologyModule.disable();
            ecologyModule = null;
        }
        if (qolModule != null) {
            qolModule.disable();
            qolModule = null;
        }
    }

    public void reload() {
        reloadConfig();
        FileConfiguration cfg = getConfig();
        blumeConfig = new BlumeConfig(cfg, getLogger());
        qolConfig = new QolConfig(cfg);
        adminConfig = new AdminConfig(cfg);
        enchantsConfig = new EnchantsConfig(cfg);
        ecologyConfig = new EcologyConfig(cfg);
        resourcePackService.reload(blumeConfig);

        if (qolModule != null) {
            qolModule.reload(qolConfig);
        }
        if (adminModule != null) {
            adminModule.reload(adminConfig);
        }
        if (enchantsModule != null) {
            enchantsModule.reload(enchantsConfig);
        }
        if (ecologyModule != null) {
            ecologyModule.reload(ecologyConfig);
        }

        sendResourcePackToOnlinePlayers();
    }

    private void sendResourcePackToOnlinePlayers() {
        for (Player player : getServer().getOnlinePlayers()) {
            resourcePackService.sendTo(player);
        }
    }

    public BlumeConfig blumeConfig() {
        return blumeConfig;
    }

    public ResourcePackService resourcePackService() {
        return resourcePackService;
    }

    public EcologyModule ecologyModule() {
        return ecologyModule;
    }
}
