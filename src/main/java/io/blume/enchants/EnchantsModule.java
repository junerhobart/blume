package io.blume.enchants;

import io.blume.BlumePlugin;
import io.blume.enchants.autosmelt.AutoSmeltListener;
import io.blume.enchants.sickle.SickleListener;
import io.blume.enchants.timber.TimberListener;
import io.blume.enchants.unbreakable.UnbreakableListener;
import io.blume.enchants.veinminer.VeinminerListener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class EnchantsModule {

    private final BlumePlugin plugin;
    private EnchantsConfig config;
    private final EnchantKeys keys;
    private final List<Listener> listeners = new ArrayList<>();

    public EnchantsModule(@NotNull BlumePlugin plugin, @NotNull EnchantsConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.keys = new EnchantKeys(plugin);
    }

    public void enable() {
        BlumeEnchantments.resolve();
        if (!BlumeEnchantments.isResolved()) {
            plugin.getLogger().warning(
                "Custom enchantments missing from registry - restart the server after updating Blume."
            );
        }
        if (!config.isEnabled()) {
            return;
        }

        if (config.isVeinminerEnabled()) {
            register(new VeinminerListener(config));
        }
        if (config.isAutoSmeltEnabled()) {
            register(new AutoSmeltListener());
        }
        if (config.isTimberEnabled()) {
            register(new TimberListener(config, keys));
        }
        if (config.isSickleEnabled()) {
            register(new SickleListener(config));
        }
        if (config.isUnbreakableEnabled()) {
            register(new UnbreakableListener());
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
    }

    public void reload(@NotNull EnchantsConfig newConfig) {
        disable();
        this.config = newConfig;
        enable();
    }

    private void register(Listener listener) {
        listeners.add(listener);
    }
}
