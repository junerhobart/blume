package io.blume.ecology;

import io.blume.BlumePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public final class EcologyKeys {

    private final NamespacedKey seedType;
    private final NamespacedKey cropOriginRoot;
    private final NamespacedKey wildCropRoot;
    private final NamespacedKey wildCropCount;
    private final NamespacedKey wildCropsPopulated;
    private final NamespacedKey penId;
    private final NamespacedKey shearedAt;

    public EcologyKeys(@NotNull BlumePlugin plugin) {
        seedType = new NamespacedKey(plugin, "seed_type");
        cropOriginRoot = new NamespacedKey(plugin, "crop_origin");
        wildCropRoot = new NamespacedKey(plugin, "wild_crop");
        wildCropCount = new NamespacedKey(plugin, "wild_crop_count");
        wildCropsPopulated = new NamespacedKey(plugin, "wild_crops_populated");
        penId = new NamespacedKey(plugin, "pen_id");
        shearedAt = new NamespacedKey(plugin, "sheared_at");
    }

    public @NotNull NamespacedKey seedType() {
        return seedType;
    }

    public @NotNull NamespacedKey cropOrigin(@NotNull Block block) {
        return blockScopedKey(cropOriginRoot, block);
    }

    public @NotNull NamespacedKey wildCrop(@NotNull Block block) {
        return blockScopedKey(wildCropRoot, block);
    }

    public @NotNull NamespacedKey penId() {
        return penId;
    }

    public @NotNull NamespacedKey shearedAt() {
        return shearedAt;
    }

    public @NotNull NamespacedKey wildCropCount() {
        return wildCropCount;
    }

    public @NotNull NamespacedKey wildCropsPopulated() {
        return wildCropsPopulated;
    }

    private @NotNull NamespacedKey blockScopedKey(@NotNull NamespacedKey root, @NotNull Block block) {
        return new NamespacedKey(
            root.getNamespace(),
            root.getKey() + "_" + block.getX() + "_" + block.getY() + "_" + block.getZ()
        );
    }
}
