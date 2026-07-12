package io.blume.enchants;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class EnchantsConfig {

    private final boolean enabled;
    private final boolean autoSmeltEnabled;
    private final boolean veinminerEnabled;
    private final int veinminerMaxBlocks;
    private final boolean timberEnabled;
    private final int timberMaxBlocks;
    private final boolean timberReplantSaplings;
    private final boolean sickleEnabled;
    private final int[] sickleRadiusByLevel;
    private final boolean unbreakableEnabled;

    public EnchantsConfig(@NotNull FileConfiguration cfg) {
        ConfigurationSection enchants = cfg.getConfigurationSection("enchants");

        enabled = sectionBool(enchants, "enabled", true);
        autoSmeltEnabled = sectionBool(enchants, "auto-smelt.enabled", true);
        veinminerEnabled = sectionBool(enchants, "veinminer.enabled", true);
        veinminerMaxBlocks = sectionInt(enchants, "veinminer.max-blocks", 64);
        timberEnabled = sectionBool(enchants, "timber.enabled", true);
        timberMaxBlocks = sectionInt(enchants, "timber.max-blocks", 256);
        timberReplantSaplings = sectionBool(enchants, "timber.replant-saplings", true);
        sickleEnabled = sectionBool(enchants, "sickle.enabled", true);
        sickleRadiusByLevel = parseRadiusByLevel(enchants);
        unbreakableEnabled = sectionBool(enchants, "unbreakable.enabled", true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAutoSmeltEnabled() {
        return autoSmeltEnabled;
    }

    public boolean isVeinminerEnabled() {
        return veinminerEnabled;
    }

    public int veinminerMaxBlocks() {
        return veinminerMaxBlocks;
    }

    public boolean isTimberEnabled() {
        return timberEnabled;
    }

    public int timberMaxBlocks() {
        return timberMaxBlocks;
    }

    public boolean isTimberReplantSaplings() {
        return timberReplantSaplings;
    }

    public boolean isSickleEnabled() {
        return sickleEnabled;
    }

    public int sickleRadius(int level) {
        if (level <= 0) {
            return 0;
        }
        int index = Math.min(level, sickleRadiusByLevel.length) - 1;
        return sickleRadiusByLevel[index];
    }

    public boolean isUnbreakableEnabled() {
        return unbreakableEnabled;
    }

    private static int[] parseRadiusByLevel(ConfigurationSection enchants) {
        int[] defaults = {1, 2, 3};
        if (enchants == null) {
            return defaults;
        }
        List<Integer> values = enchants.getIntegerList("sickle.radius-by-level");
        if (values.size() < 3) {
            return defaults;
        }
        return new int[] {values.get(0), values.get(1), values.get(2)};
    }

    private static boolean sectionBool(ConfigurationSection section, @NotNull String path, boolean def) {
        return section == null ? def : section.getBoolean(path, def);
    }

    private static int sectionInt(ConfigurationSection section, @NotNull String path, int def) {
        return section != null ? section.getInt(path, def) : def;
    }
}
