package io.blume.ecology;

import io.blume.BlumePlugin;
import io.blume.ecology.grass.MixedSeedDropListener;
import io.blume.ecology.grass.NaturalGrassTask;
import io.blume.ecology.harvest.CropOriginHelper;
import io.blume.ecology.harvest.PoisonPotatoHandler;
import io.blume.ecology.husbandry.PastureScoreService;
import io.blume.ecology.husbandry.WelfareYieldListener;
import io.blume.ecology.items.ChickenSeedFeedListener;
import io.blume.ecology.items.EcologyItems;
import io.blume.ecology.planting.SeedPlantListener;
import io.blume.ecology.pollination.BeePollinationListener;
import io.blume.ecology.wild.WildCropChunkListener;
import io.blume.ecology.wild.WildCropGuardListener;
import io.blume.ecology.wild.WildCropPopulator;
import io.blume.qol.harvest.CropHarvest;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class EcologyModule {

    private final BlumePlugin plugin;
    private EcologyConfig config;
    private final EcologyKeys keys;
    private final EcologyItems items;
    private final CropOriginHelper originHelper;
    private PastureScoreService pastureScoreService;
    private final List<Listener> listeners = new ArrayList<>();
    private final List<BukkitRunnable> tasks = new ArrayList<>();

    public EcologyModule(@NotNull BlumePlugin plugin, @NotNull EcologyConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.keys = new EcologyKeys(plugin);
        this.items = new EcologyItems();
        this.originHelper = new CropOriginHelper(keys, items);
    }

    public void enable() {
        if (!config.isEnabled()) {
            return;
        }

        this.pastureScoreService = new PastureScoreService(plugin, config);
        register(new WelfareYieldListener(pastureScoreService, config, keys));

        PoisonPotatoHandler poisonHandler = new PoisonPotatoHandler(originHelper, config);
        CropHarvest.inject(originHelper, poisonHandler);

        register(new SeedPlantListener(plugin, config, items, originHelper));
        register(new ChickenSeedFeedListener(items));
        register(new MixedSeedDropListener(config, items));
        register(poisonHandler);

        if (config.isWildCropsEnabled()) {
            WildCropPopulator populator = new WildCropPopulator(config, keys);
            WildCropChunkListener chunkListener = new WildCropChunkListener(plugin, populator);
            register(new WildCropGuardListener(keys));
            register(chunkListener);
            chunkListener.populateLoadedChunks();
        }
        if (config.isNaturalGrassEnabled()) {
            NaturalGrassTask naturalGrassTask = new NaturalGrassTask(plugin, config);
            naturalGrassTask.start();
            tasks.add(naturalGrassTask);
        }
        if (config.isPollinationEnabled()) {
            BeePollinationListener beePollination = new BeePollinationListener(plugin, config, keys);
            beePollination.start();
            tasks.add(beePollination);
        }

        PluginManager pm = plugin.getServer().getPluginManager();
        for (Listener listener : listeners) {
            pm.registerEvents(listener, plugin);
        }
    }

    public void disable() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
        for (BukkitRunnable task : tasks) {
            task.cancel();
        }
        tasks.clear();
        if (pastureScoreService != null) {
            pastureScoreService.shutdown();
            pastureScoreService = null;
        }
    }

    public void reload(@NotNull EcologyConfig newConfig) {
        disable();
        this.config = newConfig;
        enable();
    }

    public @NotNull EcologyConfig config() {
        return config;
    }

    public @NotNull EcologyKeys keys() {
        return keys;
    }

    public @NotNull EcologyItems items() {
        return items;
    }

    public @NotNull CropOriginHelper originHelper() {
        return originHelper;
    }

    private void register(@NotNull Listener listener) {
        listeners.add(listener);
    }
}
