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
import io.blume.config.ConfigMerger;
import io.blume.geyser.GeyserAssetInstaller;
import io.blume.listener.PlayerJoinListener;
import io.blume.qol.QolConfig;
import io.blume.qol.QolModule;
import io.blume.resourcepack.JavaResourcePackSource;
import io.blume.resourcepack.ResourcePackService;
import io.blume.update.VersionCompare;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class BlumePlugin extends JavaPlugin {

    private BlumeConfig blumeConfig;
    private QolConfig qolConfig;
    private AdminConfig adminConfig;
    private ResourcePackService resourcePackService;
    private JavaResourcePackSource javaResourcePackSource;
    private QolModule qolModule;
    private AdminModule adminModule;
    private EnchantsConfig enchantsConfig;
    private EnchantsModule enchantsModule;
    private EcologyConfig ecologyConfig;
    private EcologyModule ecologyModule;

    @Override
    public void onLoad() {
        loadAndMergeConfig();
        GeyserAssetInstaller.install(this, loadBlumeConfig(), getLogger());
    }

    @Override
    public void onEnable() {
        FileConfiguration cfg = loadAndMergeConfig();
        blumeConfig = loadBlumeConfig(cfg);
        qolConfig = new QolConfig(cfg);
        adminConfig = new AdminConfig(cfg);
        enchantsConfig = new EnchantsConfig(cfg);
        ecologyConfig = new EcologyConfig(cfg);

        javaResourcePackSource = JavaResourcePackSource.start(this, blumeConfig, getLogger());
        resourcePackService = new ResourcePackService(
            this,
            javaResourcePackSource,
            blumeConfig.getResourcePackPrompt(),
            blumeConfig.isResourcePackRequired()
        );

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, resourcePackService), this);
        getServer().getScheduler().runTaskLater(this, this::sendResourcePackToOnlinePlayers, 1L);
        getLogger().warning(
            "[Blume] Minecraft 1.21.x is deprecated and will not receive further Blume updates. "
                + "Newer Blume releases require Paper 26.2 and Java 25."
        );
        if (VersionCompare.isBeta(getPluginMeta().getVersion())) {
            getLogger().warning(
                "[Blume] Experimental beta build (" + getPluginMeta().getVersion() + "). Expect bugs; not for production."
            );
        }

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
        if (javaResourcePackSource != null) {
            javaResourcePackSource.close();
            javaResourcePackSource = null;
        }
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
        FileConfiguration cfg = loadAndMergeConfig();
        blumeConfig = loadBlumeConfig(cfg);
        qolConfig = new QolConfig(cfg);
        adminConfig = new AdminConfig(cfg);
        enchantsConfig = new EnchantsConfig(cfg);
        ecologyConfig = new EcologyConfig(cfg);

        if (javaResourcePackSource != null) {
            javaResourcePackSource.close();
        }
        javaResourcePackSource = JavaResourcePackSource.start(this, blumeConfig, getLogger());
        resourcePackService.reload(
            javaResourcePackSource,
            blumeConfig.getResourcePackPrompt(),
            blumeConfig.isResourcePackRequired()
        );
        GeyserAssetInstaller.install(this, blumeConfig, getLogger());

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

    private BlumeConfig loadBlumeConfig() {
        return loadBlumeConfig(getConfig());
    }

    private FileConfiguration loadAndMergeConfig() {
        saveDefaultConfig();
        reloadConfig();
        FileConfiguration user = getConfig();
        FileConfiguration defaults = YamlConfiguration.loadConfiguration(
            new InputStreamReader(requireConfigResource(), StandardCharsets.UTF_8)
        );
        ConfigMerger.warnDeprecatedPaths(user, getLogger());
        int added = ConfigMerger.mergeMissing(user, defaults);
        if (added > 0) {
            saveConfig();
            getLogger().info("Added " + added + " new config key(s) from defaults.");
        }
        return user;
    }

    private @NotNull InputStream requireConfigResource() {
        InputStream stream = getResource("config.yml");
        if (stream == null) {
            throw new IllegalStateException("Missing bundled config.yml");
        }
        return stream;
    }

    private BlumeConfig loadBlumeConfig(FileConfiguration cfg) {
        BlumeConfig config = new BlumeConfig(cfg, getPluginMeta().getVersion(), getLogger());
        syncAutoManagedPackUrl(cfg, config);
        return config;
    }

    private void syncAutoManagedPackUrl(FileConfiguration cfg, BlumeConfig config) {
        String raw = cfg.getString("resource-pack.url");
        if (raw == null) {
            raw = "";
        }
        String resolved = config.getResourcePackUrl();
        if (!resolved.equals(raw) && config.isAutoManagedPackUrl(raw)) {
            cfg.set("resource-pack.url", resolved);
            saveConfig();
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
