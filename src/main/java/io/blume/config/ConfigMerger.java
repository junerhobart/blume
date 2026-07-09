package io.blume.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public final class ConfigMerger {

    private static final Map<String, String> DEPRECATED_PATHS = Collections.emptyMap();

    private ConfigMerger() {
    }

    public static int mergeMissing(@NotNull FileConfiguration user, @NotNull FileConfiguration defaults) {
        return mergeSection(user, defaults, "");
    }

    private static int mergeSection(
        @NotNull ConfigurationSection user,
        @NotNull ConfigurationSection defaults,
        @NotNull String prefix
    ) {
        int added = 0;
        for (String key : defaults.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (defaults.isConfigurationSection(key)) {
                ConfigurationSection nestedDefaults = defaults.getConfigurationSection(key);
                if (nestedDefaults != null) {
                    added += mergeSection(user, nestedDefaults, path);
                }
            } else if (!user.contains(path, true)) {
                user.set(path, defaults.get(key));
                added++;
            }
        }
        return added;
    }

    public static void warnDeprecatedPaths(@NotNull FileConfiguration user, @NotNull java.util.logging.Logger log) {
        for (Map.Entry<String, String> entry : DEPRECATED_PATHS.entrySet()) {
            if (user.contains(entry.getKey(), true)) {
                log.warning("Config key '" + entry.getKey() + "' is deprecated; use '" + entry.getValue() + "'.");
            }
        }
    }

    static void selfCheck() {
        FileConfiguration user = new YamlConfiguration();
        user.set("a", 1);

        FileConfiguration defaults = new YamlConfiguration();
        defaults.set("a", 999);
        defaults.set("b", 2);

        int added = mergeMissing(user, defaults);
        if (added != 1) {
            throw new AssertionError("expected 1 added key, got " + added);
        }
        if (user.getInt("a") != 1) {
            throw new AssertionError("existing key must not be overwritten");
        }
        if (user.getInt("b") != 2) {
            throw new AssertionError("missing key must be copied from defaults");
        }

        FileConfiguration partial = new YamlConfiguration();
        partial.set("section.existing", "keep");

        FileConfiguration nestedDefaults = new YamlConfiguration();
        nestedDefaults.set("section.existing", "replace");
        nestedDefaults.set("section.new", "added");

        added = mergeMissing(partial, nestedDefaults);
        if (added != 1) {
            throw new AssertionError("expected 1 nested added key, got " + added);
        }
        if (!"keep".equals(partial.getString("section.existing"))) {
            throw new AssertionError("existing nested key must not be overwritten");
        }
        if (!"added".equals(partial.getString("section.new"))) {
            throw new AssertionError("missing nested key must be copied from defaults");
        }

        added = mergeMissing(partial, nestedDefaults);
        if (added != 0) {
            throw new AssertionError("second merge must be idempotent, got " + added);
        }
    }

    public static void main(String[] args) {
        selfCheck();
    }
}
