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

    public boolean isDesirePathBlockKey(@NotNull NamespacedKey key) {
        return desirePathRoot.getNamespace().equals(key.getNamespace())
            && key.getKey().startsWith("dp_");
    }

    public @org.jetbrains.annotations.Nullable int[] parseDesirePathCoords(@NotNull NamespacedKey key) {
        if (!isDesirePathBlockKey(key)) {
            return null;
        }
        String[] parts = key.getKey().substring(3).split("_");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new int[] {
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            };
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
