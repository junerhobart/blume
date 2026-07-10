package io.blume.qol;

import io.blume.BlumePlugin;
import io.blume.qol.anvil.AnvilListener;
import io.blume.qol.creeper.CreeperRebuildListener;
import io.blume.qol.creeper.CreeperRebuildService;
import io.blume.qol.death.DeathDropListener;
import io.blume.qol.doubledoors.DoubleDoorsListener;
import io.blume.qol.harvest.HarvestListener;
import io.blume.qol.paths.DesirePathDecayTask;
import io.blume.qol.paths.DesirePathListener;
import io.blume.qol.paths.DesirePathService;
import io.blume.qol.paths.PathSpeedListener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class QolModule {

    private final BlumePlugin plugin;
    private QolConfig config;
    private final QolKeys keys;
    private final List<Listener> listeners = new ArrayList<>();
    private CreeperRebuildService creeperRebuildService;
    private DeathDropListener deathDropListener;
    private DesirePathService desirePathService;
    private DesirePathDecayTask desirePathDecayTask;

    public QolModule(@NotNull BlumePlugin plugin, @NotNull QolConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.keys = new QolKeys(plugin);
    }

    public void enable() {
        PluginManager pm = plugin.getServer().getPluginManager();

        if (config.isDoubleDoorsEnabled()) {
            register(new DoubleDoorsListener(plugin));
        }
        if (config.isAnvilEnabled()) {
            register(new AnvilListener(config));
        }
        if (config.isDeathDropsEnabled()) {
            deathDropListener = new DeathDropListener(plugin, config, keys);
            register(deathDropListener);
            deathDropListener.startCleanupTask();
        }
        if (config.isPathSpeedEnabled()) {
            register(new PathSpeedListener(plugin, config));
        }
        if (config.isHarvestReplantEnabled()) {
            register(new HarvestListener(config.isHarvestRequireEmptyHand()));
        }
        if (config.isDesirePathsEnabled()) {
            desirePathService = new DesirePathService(plugin, config, keys);
            register(new DesirePathListener(desirePathService));
            desirePathDecayTask = new DesirePathDecayTask(plugin, desirePathService, config);
            desirePathDecayTask.start();
        }
        if (config.isCreeperRebuildEnabled()) {
            creeperRebuildService = new CreeperRebuildService(plugin, config);
            register(new CreeperRebuildListener(creeperRebuildService, config.isCreeperRebuildWither()));
        }

        for (Listener listener : listeners) {
            pm.registerEvents(listener, plugin);
        }
    }

    public void disable() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
        if (deathDropListener != null) {
            deathDropListener.stopCleanupTask();
            deathDropListener = null;
        }
        if (creeperRebuildService != null) {
            creeperRebuildService.shutdown();
            creeperRebuildService = null;
        }
        if (desirePathDecayTask != null) {
            desirePathDecayTask.stop();
            desirePathDecayTask = null;
        }
        desirePathService = null;
    }

    public void reload(@NotNull QolConfig newConfig) {
        disable();
        this.config = newConfig;
        enable();
    }

    private void register(Listener listener) {
        listeners.add(listener);
    }
}
