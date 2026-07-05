package io.blume;

import io.blume.command.BlumeCommand;
import io.blume.config.BlumeConfig;
import io.blume.listener.PlayerJoinListener;
import io.blume.qol.QolConfig;
import io.blume.qol.QolModule;
import io.blume.resourcepack.ResourcePackService;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlumePlugin extends JavaPlugin {

    private BlumeConfig blumeConfig;
    private QolConfig qolConfig;
    private ResourcePackService resourcePackService;
    private QolModule qolModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        blumeConfig = new BlumeConfig(getConfig(), getLogger());
        qolConfig = new QolConfig(getConfig());
        resourcePackService = new ResourcePackService(this, blumeConfig);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, resourcePackService), this);

        qolModule = new QolModule(this, qolConfig);
        qolModule.enable();

        BlumeCommand cmd = new BlumeCommand(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                Commands.literal("blume")
                    .requires(src -> src.getSender().hasPermission("blume.admin"))
                    .executes(ctx -> { cmd.execute(ctx.getSource().getSender(), new String[0]); return 1; })
                    .then(Commands.literal("reload")
                        .executes(ctx -> { cmd.execute(ctx.getSource().getSender(), new String[]{"reload"}); return 1; }))
                    .build(),
                "Blume admin commands."
            );
        });
    }

    @Override
    public void onDisable() {
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
        resourcePackService.reload(blumeConfig);

        if (qolModule != null) {
            qolModule.reload(qolConfig);
        }

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
}
