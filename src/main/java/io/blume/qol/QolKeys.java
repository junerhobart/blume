package io.blume.qol;

import io.blume.BlumePlugin;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public final class QolKeys {

    private final NamespacedKey deathDropExpiry;
    private final NamespacedKey desirePathRoot;

    public QolKeys(@NotNull BlumePlugin plugin) {
        deathDropExpiry = new NamespacedKey(plugin, "death_drop_expiry");
        desirePathRoot = new NamespacedKey(plugin, "desire_path");
    }

    public @NotNull NamespacedKey deathDropExpiry() {
        return deathDropExpiry;
    }

    public @NotNull NamespacedKey desirePathBlock(int x, int y, int z) {
        return new NamespacedKey(
            desirePathRoot.getNamespace(),
            "dp_" + x + "_" + y + "_" + z
        );
    }
}
