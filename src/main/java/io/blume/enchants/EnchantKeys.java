package io.blume.enchants;

import io.blume.BlumePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class EnchantKeys {

    private final NamespacedKey playerPlacedRoot;

    public EnchantKeys(@NotNull BlumePlugin plugin) {
        playerPlacedRoot = new NamespacedKey(plugin, "player_placed");
    }

    public void markPlayerPlaced(@NotNull Block block) {
        pdc(block).set(placedKey(block), PersistentDataType.BYTE, (byte) 1);
    }

    public boolean isPlayerPlaced(@NotNull Block block) {
        Byte flag = pdc(block).get(placedKey(block), PersistentDataType.BYTE);
        return flag != null && flag == 1;
    }

    private @NotNull PersistentDataContainer pdc(@NotNull Block block) {
        return block.getChunk().getPersistentDataContainer();
    }

    private @NotNull NamespacedKey placedKey(@NotNull Block block) {
        return new NamespacedKey(
            playerPlacedRoot.getNamespace(),
            "pl_" + block.getX() + "_" + block.getY() + "_" + block.getZ()
        );
    }
}
