package io.blume.enchants;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class Logs {

    private Logs() {}

    public static boolean isTimber(@NotNull Material material) {
        if (material.isAir()) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_LOG")
            || name.endsWith("_STEM")
            || name.endsWith("_WOOD")
            || name.endsWith("_HYPHAE");
    }

    /** @deprecated use {@link #isTimber} */
    public static boolean isLog(@NotNull Material material) {
        return isTimber(material);
    }

    public static boolean sameFamily(@NotNull Material a, @NotNull Material b) {
        Material saplingA = saplingFor(a);
        Material saplingB = saplingFor(b);
        return saplingA != null && saplingA == saplingB;
    }

    public static @Nullable Material saplingFor(@NotNull Material block) {
        return switch (block) {
            case OAK_LOG, STRIPPED_OAK_LOG, OAK_WOOD, STRIPPED_OAK_WOOD -> Material.OAK_SAPLING;
            case SPRUCE_LOG, STRIPPED_SPRUCE_LOG, SPRUCE_WOOD, STRIPPED_SPRUCE_WOOD -> Material.SPRUCE_SAPLING;
            case BIRCH_LOG, STRIPPED_BIRCH_LOG, BIRCH_WOOD, STRIPPED_BIRCH_WOOD -> Material.BIRCH_SAPLING;
            case JUNGLE_LOG, STRIPPED_JUNGLE_LOG, JUNGLE_WOOD, STRIPPED_JUNGLE_WOOD -> Material.JUNGLE_SAPLING;
            case ACACIA_LOG, STRIPPED_ACACIA_LOG, ACACIA_WOOD, STRIPPED_ACACIA_WOOD -> Material.ACACIA_SAPLING;
            case DARK_OAK_LOG, STRIPPED_DARK_OAK_LOG, DARK_OAK_WOOD, STRIPPED_DARK_OAK_WOOD ->
                Material.DARK_OAK_SAPLING;
            case CHERRY_LOG, STRIPPED_CHERRY_LOG, CHERRY_WOOD, STRIPPED_CHERRY_WOOD -> Material.CHERRY_SAPLING;
            case MANGROVE_LOG, STRIPPED_MANGROVE_LOG, MANGROVE_WOOD, STRIPPED_MANGROVE_WOOD ->
                Material.MANGROVE_PROPAGULE;
            case CRIMSON_STEM, STRIPPED_CRIMSON_STEM, CRIMSON_HYPHAE, STRIPPED_CRIMSON_HYPHAE ->
                Material.CRIMSON_FUNGUS;
            case WARPED_STEM, STRIPPED_WARPED_STEM, WARPED_HYPHAE, STRIPPED_WARPED_HYPHAE ->
                Material.WARPED_FUNGUS;
            case PALE_OAK_LOG, STRIPPED_PALE_OAK_LOG, PALE_OAK_WOOD, STRIPPED_PALE_OAK_WOOD ->
                Material.PALE_OAK_SAPLING;
            default -> saplingFromName(block.name());
        };
    }

    private static @Nullable Material saplingFromName(@NotNull String materialName) {
        String species = speciesFromMaterialName(materialName);
        if (species == null) {
            return null;
        }
        return switch (species) {
            case "OAK" -> Material.OAK_SAPLING;
            case "SPRUCE" -> Material.SPRUCE_SAPLING;
            case "BIRCH" -> Material.BIRCH_SAPLING;
            case "JUNGLE" -> Material.JUNGLE_SAPLING;
            case "ACACIA" -> Material.ACACIA_SAPLING;
            case "DARK_OAK" -> Material.DARK_OAK_SAPLING;
            case "CHERRY" -> Material.CHERRY_SAPLING;
            case "MANGROVE" -> Material.MANGROVE_PROPAGULE;
            case "CRIMSON" -> Material.CRIMSON_FUNGUS;
            case "WARPED" -> Material.WARPED_FUNGUS;
            case "PALE_OAK" -> Material.PALE_OAK_SAPLING;
            default -> null;
        };
    }

    private static @Nullable String speciesFromMaterialName(@NotNull String materialName) {
        String name = materialName.toUpperCase(Locale.ROOT);
        if (name.startsWith("STRIPPED_")) {
            name = name.substring("STRIPPED_".length());
        }
        if (name.endsWith("_LOG")) {
            return name.substring(0, name.length() - "_LOG".length());
        }
        if (name.endsWith("_WOOD")) {
            return name.substring(0, name.length() - "_WOOD".length());
        }
        if (name.endsWith("_STEM")) {
            return name.substring(0, name.length() - "_STEM".length());
        }
        if (name.endsWith("_HYPHAE")) {
            return name.substring(0, name.length() - "_HYPHAE".length());
        }
        return null;
    }
}
