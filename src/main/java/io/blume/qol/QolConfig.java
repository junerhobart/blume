package io.blume.qol;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QolConfig {

    private final boolean doubleDoorsEnabled;
    private final boolean creeperRebuildEnabled;
    private final long creeperRebuildDelayMs;
    private final double creeperRebuildDelayFalloff;
    private final long creeperRebuildMinDelayMs;
    private final boolean creeperRebuildWither;
    private final boolean desirePathsEnabled;
    private final int walksToTrample;
    private final int walksToWorn;
    private final int walksToPath;
    private final boolean desirePathDeErosionEnabled;
    private final int desirePathDeErosionTrampledMcDays;
    private final int desirePathDeErosionWornMcDays;
    private final Map<Material, Integer> wornWeights;
    private final boolean deathDropsEnabled;
    private final long deathDropLifetimeMinutes;
    private final boolean anvilEnabled;
    private final int maxRepairCost;
    private final boolean harvestReplantEnabled;
    private final boolean harvestRequireEmptyHand;
    private final boolean pathSpeedEnabled;
    private final Set<Material> pathSpeed1Blocks;
    private final Set<Material> pathSpeed2Blocks;

    public QolConfig(@NotNull FileConfiguration cfg) {
        ConfigurationSection qol = cfg.getConfigurationSection("qol");

        doubleDoorsEnabled = sectionBool(qol, "double-doors.enabled", true);

        creeperRebuildEnabled = sectionBool(qol, "creeper-rebuild.enabled", true);
        creeperRebuildDelayMs = sectionLong(qol, "creeper-rebuild.rebuild-delay-ms", 2000L);
        creeperRebuildDelayFalloff = sectionDouble(qol, "creeper-rebuild.rebuild-delay-falloff", 0.175);
        creeperRebuildMinDelayMs = sectionLong(qol, "creeper-rebuild.rebuild-min-delay-ms", 50L);
        creeperRebuildWither = sectionBool(qol, "creeper-rebuild.rebuild-wither", true);

        desirePathsEnabled = sectionBool(qol, "desire-paths.enabled", true);
        walksToTrample = sectionInt(qol, "desire-paths.walks-to-trample", 4);
        walksToWorn = sectionInt(qol, "desire-paths.walks-to-worn", 8);
        walksToPath = sectionInt(qol, "desire-paths.walks-to-path", 12);
        desirePathDeErosionEnabled = sectionBool(qol, "desire-paths.erosion.enabled", true);
        desirePathDeErosionTrampledMcDays = sectionInt(qol, "desire-paths.erosion.timeout-mc-days.trampled", 5);
        desirePathDeErosionWornMcDays = sectionInt(qol, "desire-paths.erosion.timeout-mc-days.worn", 8);
        wornWeights = parseWornWeights(qol);

        deathDropsEnabled = sectionBool(qol, "death-drops.enabled", true);
        deathDropLifetimeMinutes = sectionLong(qol, "death-drops.lifetime-minutes", 240L);

        anvilEnabled = sectionBool(qol, "anvil.enabled", true);
        maxRepairCost = sectionInt(qol, "anvil.max-repair-cost", Integer.MAX_VALUE);

        harvestReplantEnabled = sectionBool(qol, "harvest-replant.enabled", true);
        harvestRequireEmptyHand = sectionBool(qol, "harvest-replant.require-empty-hand", false);

        pathSpeedEnabled = sectionBool(qol, "path-speed.enabled", true);
        pathSpeed1Blocks = parseMaterials(qol, "path-speed.speed-1-blocks",
            Material.DIRT, Material.COARSE_DIRT, Material.GRAVEL);
        pathSpeed2Blocks = parseMaterials(qol, "path-speed.speed-2-blocks", Material.DIRT_PATH);
    }

    public boolean isDoubleDoorsEnabled() {
        return doubleDoorsEnabled;
    }

    public boolean isCreeperRebuildEnabled() {
        return creeperRebuildEnabled;
    }

    public long creeperRebuildDelayMs() {
        return creeperRebuildDelayMs;
    }

    public double creeperRebuildDelayFalloff() {
        return creeperRebuildDelayFalloff;
    }

    public long creeperRebuildMinDelayMs() {
        return creeperRebuildMinDelayMs;
    }

    public boolean isCreeperRebuildWither() {
        return creeperRebuildWither;
    }

    public boolean isDesirePathsEnabled() {
        return desirePathsEnabled;
    }

    public int walksToTrample() {
        return walksToTrample;
    }

    public int walksToWorn() {
        return walksToWorn;
    }

    public int walksToPath() {
        return walksToPath;
    }

    public boolean isDesirePathDeErosionEnabled() {
        return desirePathDeErosionEnabled;
    }

    public int desirePathDeErosionTrampledMcDays() {
        return desirePathDeErosionTrampledMcDays;
    }

    public int desirePathDeErosionWornMcDays() {
        return desirePathDeErosionWornMcDays;
    }

    public @NotNull Map<Material, Integer> wornWeights() {
        return wornWeights;
    }

    public boolean isDeathDropsEnabled() {
        return deathDropsEnabled;
    }

    public long deathDropLifetimeMinutes() {
        return deathDropLifetimeMinutes;
    }

    public boolean isAnvilEnabled() {
        return anvilEnabled;
    }

    public int maxRepairCost() {
        return maxRepairCost;
    }

    public boolean isHarvestReplantEnabled() {
        return harvestReplantEnabled;
    }

    public boolean isHarvestRequireEmptyHand() {
        return harvestRequireEmptyHand;
    }

    public boolean isPathSpeedEnabled() {
        return pathSpeedEnabled;
    }

    public @NotNull Set<Material> pathSpeed1Blocks() {
        return pathSpeed1Blocks;
    }

    public @NotNull Set<Material> pathSpeed2Blocks() {
        return pathSpeed2Blocks;
    }

    private static @NotNull Map<Material, Integer> parseWornWeights(ConfigurationSection qol) {
        Map<Material, Integer> weights = new HashMap<>();
        weights.put(Material.DIRT, 75);
        weights.put(Material.COARSE_DIRT, 20);
        weights.put(Material.GRAVEL, 5);

        if (qol == null) {
            return weights;
        }
        ConfigurationSection section = qol.getConfigurationSection("desire-paths.worn-weights");
        if (section == null) {
            return weights;
        }

        weights.clear();
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key.toUpperCase());
            if (material != null) {
                weights.put(material, section.getInt(key));
            }
        }
        if (weights.isEmpty()) {
            weights.put(Material.DIRT, 75);
            weights.put(Material.COARSE_DIRT, 20);
            weights.put(Material.GRAVEL, 5);
        }
        return weights;
    }

    private static @NotNull Set<Material> parseMaterials(
        ConfigurationSection qol,
        @NotNull String path,
        Material... defaults
    ) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        if (qol != null) {
            List<String> names = qol.getStringList(path);
            for (String name : names) {
                Material material = Material.matchMaterial(name.toUpperCase());
                if (material != null) {
                    materials.add(material);
                }
            }
        }
        if (materials.isEmpty()) {
            materials.addAll(Set.of(defaults));
        }
        return materials;
    }

    private static boolean sectionBool(ConfigurationSection qol, @NotNull String path, boolean def) {
        return qol != null ? qol.getBoolean(path, def) : def;
    }

    private static int sectionInt(ConfigurationSection qol, @NotNull String path, int def) {
        return qol != null ? qol.getInt(path, def) : def;
    }

    private static long sectionLong(ConfigurationSection qol, @NotNull String path, long def) {
        return qol != null ? qol.getLong(path, def) : def;
    }

    private static double sectionDouble(ConfigurationSection qol, @NotNull String path, double def) {
        return qol != null ? qol.getDouble(path, def) : def;
    }
}
