package io.blume.ecology;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EcologyConfig {

    private final boolean enabled;

    private final double potatoPoisonWholeCropChance;

    private final double mixedGrassChance;
    private final double mixedCropChance;
    private final int mixedDropMin;
    private final int mixedDropMax;

    private final boolean wildCropsEnabled;
    private final int wildMaxPerChunk;
    private final double wildSpawnChance;
    private final int wildPatchesPerChunk;
    private final boolean wildBiomeFilter;
    private final Set<String> wildBiomeAllowlist;
    private final boolean wildCropsSpreadEnabled;
    private final double wildCropsSpreadChance;
    private final double wildCropsSpreadIntoGrassChance;

    private final boolean naturalGrassEnabled;
    private final double naturalGrassShortGrassChance;
    private final double naturalGrassTallGrassChance;
    private final boolean naturalGrassInvadeFarmland;
    private final double naturalGrassInvadeFarmlandChance;

    private final boolean pollinationEnabled;
    private final double pollinationFlowerSpreadChance;

    private final boolean husbandryEnabled;
    private final int husbandryMaxPastureRadius;
    private final Map<String, Integer> husbandryMinBlocksPerAnimal;
    private final Map<String, Double> husbandryTierMultipliers;
    private final boolean husbandryWaterBoost;
    private final int husbandryShelterRequiredMaxRadius;
    private final Set<Material> husbandryGoodFlooring;
    private final Set<Material> husbandryBadFlooring;
    private final double husbandryGoodBoostThreshold;
    private final double husbandryBadPenaltyThreshold;
    private final boolean husbandryStandingBadForcesPoor;
    private final boolean husbandrySunlightBoostEnabled;
    private final double husbandrySunlightBoostThreshold;
    private final boolean husbandryStandingNoSkyPenalty;
    private final double husbandrySunlightYieldBonus;
    private final Map<String, Map<String, Object>> husbandryBonuses;

    public EcologyConfig(@NotNull FileConfiguration cfg) {
        ConfigurationSection ecology = cfg.getConfigurationSection("ecology");

        enabled = sectionBool(ecology, "enabled", true);

        potatoPoisonWholeCropChance = sectionDouble(ecology, "potato-seeds.poison-whole-crop-chance", 0.60);

        mixedGrassChance = sectionDouble(ecology, "mixed-seeds.grass-chance", 0.65);
        mixedCropChance = sectionDouble(ecology, "mixed-seeds.crop-chance", 0.35);
        mixedDropMin = sectionInt(ecology, "mixed-seeds.drop-min", 0);
        mixedDropMax = sectionInt(ecology, "mixed-seeds.drop-max", 2);

        wildCropsEnabled = sectionBool(ecology, "wild-crops.enabled", true);
        wildMaxPerChunk = sectionInt(ecology, "wild-crops.max-per-chunk", 16);
        wildSpawnChance = sectionDouble(ecology, "wild-crops.spawn-chance", 0.35);
        wildPatchesPerChunk = sectionInt(ecology, "wild-crops.patches-per-chunk", 3);
        wildBiomeFilter = sectionBool(ecology, "wild-crops.biome-filter", false);
        wildBiomeAllowlist = parseStringSet(ecology, "wild-crops.biome-allowlist",
            "plains", "forest", "meadow", "savanna", "taiga", "jungle", "swamp", "river");
        wildCropsSpreadEnabled = sectionBool(ecology, "wild-crops.spread-enabled", true);
        wildCropsSpreadChance = sectionDouble(ecology, "wild-crops.spread-chance", 0.05);
        wildCropsSpreadIntoGrassChance = sectionDouble(ecology, "wild-crops.spread-into-grass-chance", 0.02);

        naturalGrassEnabled = sectionBool(ecology, "natural-grass.enabled", true);
        naturalGrassShortGrassChance = sectionDouble(ecology, "natural-grass.short-grass-chance", 0.05);
        naturalGrassTallGrassChance = sectionDouble(ecology, "natural-grass.tall-grass-chance", 0.03);
        naturalGrassInvadeFarmland = sectionBool(ecology, "natural-grass.invade-farmland", false);
        naturalGrassInvadeFarmlandChance = sectionDouble(ecology, "natural-grass.invade-farmland-chance", 0.08);

        pollinationEnabled = sectionBool(ecology, "pollination.enabled", true);
        pollinationFlowerSpreadChance = sectionDouble(ecology, "pollination.flower-spread-chance", 0.05);

        husbandryEnabled = sectionBool(ecology, "husbandry.enabled", true);
        husbandryMaxPastureRadius = sectionInt(ecology, "husbandry.max-pasture-radius", 48);
        husbandryMinBlocksPerAnimal = parseMinBlocksPerAnimal(ecology);
        husbandryTierMultipliers = parseTierMultipliers(ecology);
        husbandryWaterBoost = sectionBool(ecology, "husbandry.water-boost", true);
        husbandryShelterRequiredMaxRadius = sectionInt(ecology, "husbandry.shelter-required-max-radius", 24);
        husbandryGoodFlooring = parseMaterials(ecology, "husbandry.flooring.good-blocks",
            Material.GRASS_BLOCK, Material.MOSS_BLOCK, Material.MOSS_CARPET, Material.PODZOL,
            Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT, Material.GRAVEL,
            Material.CLAY, Material.MUD, Material.SAND);
        husbandryBadFlooring = parseMaterials(ecology, "husbandry.flooring.bad-blocks",
            Material.HOPPER, Material.DROPPER, Material.DISPENSER, Material.MAGMA_BLOCK,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CACTUS, Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE, Material.LAVA);
        husbandryGoodBoostThreshold = sectionDouble(ecology, "husbandry.flooring.good-boost-threshold", 0.60);
        husbandryBadPenaltyThreshold = sectionDouble(ecology, "husbandry.flooring.bad-penalty-threshold", 0.15);
        husbandryStandingBadForcesPoor = sectionBool(ecology, "husbandry.flooring.standing-bad-forces-poor", true);
        husbandrySunlightBoostEnabled = sectionBool(ecology, "husbandry.sunlight-boost-enabled", true);
        husbandrySunlightBoostThreshold = sectionDouble(ecology, "husbandry.sunlight-boost-threshold", 0.60);
        husbandryStandingNoSkyPenalty = sectionBool(ecology, "husbandry.standing-no-sky-penalty", true);
        husbandrySunlightYieldBonus = sectionDouble(ecology, "husbandry.sunlight-yield-bonus", 0.10);
        husbandryBonuses = parseBonusTables(ecology);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double potatoPoisonWholeCropChance() {
        return potatoPoisonWholeCropChance;
    }

    public double mixedGrassChance() {
        return mixedGrassChance;
    }

    public double mixedCropChance() {
        return mixedCropChance;
    }

    public int mixedDropMin() {
        return mixedDropMin;
    }

    public int mixedDropMax() {
        return mixedDropMax;
    }

    public boolean isWildCropsEnabled() {
        return wildCropsEnabled;
    }

    public int wildMaxPerChunk() {
        return wildMaxPerChunk;
    }

    public double wildSpawnChance() {
        return wildSpawnChance;
    }

    public int wildPatchesPerChunk() {
        return wildPatchesPerChunk;
    }

    public boolean isWildBiomeFilter() {
        return wildBiomeFilter;
    }

    public @NotNull Set<String> wildBiomeAllowlist() {
        return wildBiomeAllowlist;
    }

    public boolean isWildCropsSpreadEnabled() {
        return wildCropsSpreadEnabled;
    }

    public double wildCropsSpreadChance() {
        return wildCropsSpreadChance;
    }

    public double wildCropsSpreadIntoGrassChance() {
        return wildCropsSpreadIntoGrassChance;
    }

    public boolean isNaturalGrassEnabled() {
        return naturalGrassEnabled;
    }

    public double naturalGrassShortGrassChance() {
        return naturalGrassShortGrassChance;
    }

    public double naturalGrassTallGrassChance() {
        return naturalGrassTallGrassChance;
    }

    public boolean isNaturalGrassInvadeFarmland() {
        return naturalGrassInvadeFarmland;
    }

    public double naturalGrassInvadeFarmlandChance() {
        return naturalGrassInvadeFarmlandChance;
    }

    public boolean isPollinationEnabled() {
        return pollinationEnabled;
    }

    public double pollinationFlowerSpreadChance() {
        return pollinationFlowerSpreadChance;
    }

    public boolean isHusbandryEnabled() {
        return husbandryEnabled;
    }

    public int husbandryMaxPastureRadius() {
        return husbandryMaxPastureRadius;
    }

    public @NotNull Map<String, Integer> husbandryMinBlocksPerAnimal() {
        return husbandryMinBlocksPerAnimal;
    }

    public @NotNull Map<String, Double> husbandryTierMultipliers() {
        return husbandryTierMultipliers;
    }

    public boolean isHusbandryWaterBoost() {
        return husbandryWaterBoost;
    }

    public int husbandryShelterRequiredMaxRadius() {
        return husbandryShelterRequiredMaxRadius;
    }

    public @NotNull Set<Material> husbandryGoodFlooring() {
        return husbandryGoodFlooring;
    }

    public @NotNull Set<Material> husbandryBadFlooring() {
        return husbandryBadFlooring;
    }

    public double husbandryGoodBoostThreshold() {
        return husbandryGoodBoostThreshold;
    }

    public double husbandryBadPenaltyThreshold() {
        return husbandryBadPenaltyThreshold;
    }

    public boolean isHusbandryStandingBadForcesPoor() {
        return husbandryStandingBadForcesPoor;
    }

    public boolean isHusbandrySunlightBoostEnabled() {
        return husbandrySunlightBoostEnabled;
    }

    public double husbandrySunlightBoostThreshold() {
        return husbandrySunlightBoostThreshold;
    }

    public boolean isHusbandryStandingNoSkyPenalty() {
        return husbandryStandingNoSkyPenalty;
    }

    public double husbandrySunlightYieldBonus() {
        return husbandrySunlightYieldBonus;
    }

    public @NotNull Map<String, Map<String, Object>> husbandryBonuses() {
        return husbandryBonuses;
    }

    private static @NotNull Map<String, Integer> parseMinBlocksPerAnimal(ConfigurationSection ecology) {
        Map<String, Integer> defaults = new HashMap<>();
        defaults.put("cow", 64);
        defaults.put("sheep", 48);
        defaults.put("pig", 40);
        defaults.put("chicken", 12);

        if (ecology == null) {
            return defaults;
        }
        ConfigurationSection section = ecology.getConfigurationSection("husbandry.min-blocks-per-animal");
        if (section == null) {
            return defaults;
        }

        for (String key : section.getKeys(false)) {
            defaults.put(key, section.getInt(key, defaults.getOrDefault(key, 12)));
        }
        return defaults;
    }

    private static @NotNull Map<String, Double> parseTierMultipliers(ConfigurationSection ecology) {
        Map<String, Double> defaults = new HashMap<>();
        defaults.put("good", 1.5);
        defaults.put("fair", 2.5);

        if (ecology == null) {
            return defaults;
        }
        ConfigurationSection section = ecology.getConfigurationSection("husbandry.tier-multipliers");
        if (section == null) {
            return defaults;
        }

        for (String key : section.getKeys(false)) {
            defaults.put(key, section.getDouble(key, defaults.getOrDefault(key, 2.5)));
        }
        return defaults;
    }

    private static @NotNull Map<String, Map<String, Object>> parseBonusTables(ConfigurationSection ecology) {
        Map<String, Map<String, Object>> tables = new HashMap<>();
        if (ecology == null) {
            return tables;
        }
        ConfigurationSection bonuses = ecology.getConfigurationSection("husbandry.bonuses");
        if (bonuses == null) {
            return tables;
        }
        for (String tier : bonuses.getKeys(false)) {
            ConfigurationSection tierSection = bonuses.getConfigurationSection(tier);
            if (tierSection != null) {
                tables.put(tier, new HashMap<>(tierSection.getValues(false)));
            }
        }
        return tables;
    }

    private static @NotNull Set<Material> parseMaterials(
        ConfigurationSection ecology,
        @NotNull String path,
        Material... defaults
    ) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        if (ecology != null) {
            List<String> names = ecology.getStringList(path);
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

    private static @NotNull Set<String> parseStringSet(
        ConfigurationSection ecology,
        @NotNull String path,
        String... defaults
    ) {
        Set<String> set = new HashSet<>();
        if (ecology != null) {
            List<String> values = ecology.getStringList(path);
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    set.add(value.toLowerCase());
                }
            }
        }
        if (set.isEmpty()) {
            for (String value : defaults) {
                set.add(value.toLowerCase());
            }
        }
        return set;
    }

    private static boolean sectionBool(ConfigurationSection ecology, @NotNull String path, boolean def) {
        return ecology != null ? ecology.getBoolean(path, def) : def;
    }

    private static int sectionInt(ConfigurationSection ecology, @NotNull String path, int def) {
        return ecology != null ? ecology.getInt(path, def) : def;
    }

    private static double sectionDouble(ConfigurationSection ecology, @NotNull String path, double def) {
        return ecology != null ? ecology.getDouble(path, def) : def;
    }
}
