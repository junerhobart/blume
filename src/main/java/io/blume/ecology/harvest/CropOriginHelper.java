package io.blume.ecology.harvest;

import io.blume.ecology.EcologyKeys;
import io.blume.ecology.items.EcologyItems;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CropOriginHelper {

    public static final String CARROT = "carrot";
    public static final String POTATO = "potato";
    public static final String POTATO_SEED = "potato_seed";
    public static final String MIXED = "mixed";
    public static final String WILD = "wild";

    private final EcologyKeys keys;
    private final EcologyItems items;

    public CropOriginHelper(@NotNull EcologyKeys keys, @NotNull EcologyItems items) {
        this.keys = keys;
        this.items = items;
    }

    public @Nullable String readOrigin(@NotNull Block block) {
        return pdc(block).get(keys.cropOrigin(block), PersistentDataType.STRING);
    }

    public void writeOrigin(@NotNull Block block, @NotNull String origin) {
        pdc(block).set(keys.cropOrigin(block), PersistentDataType.STRING, origin);
    }

    public void clearOrigin(@NotNull Block block) {
        pdc(block).remove(keys.cropOrigin(block));
    }

    private static @NotNull PersistentDataContainer pdc(@NotNull Block block) {
        return block.getChunk().getPersistentDataContainer();
    }

    public boolean originMatches(@NotNull Block block, @NotNull String origin) {
        return origin.equals(readOrigin(block));
    }

    public boolean hasOrigin(@NotNull Block block) {
        return readOrigin(block) != null;
    }

    public boolean isWild(@NotNull Block block) {
        return WILD.equals(readOrigin(block));
    }

    public boolean usesInPlaceReplant(@NotNull Block block) {
        String origin = readOrigin(block);
        return CARROT.equals(origin) || POTATO_SEED.equals(origin) || MIXED.equals(origin);
    }

    public @Nullable ReplantChoice replantChoice(@NotNull Block block) {
        String origin = readOrigin(block);
        if (origin == null) {
            return null;
        }
        return switch (origin) {
            case POTATO -> new ReplantChoice(new ItemStack(Material.POTATO), true);
            case CARROT -> new ReplantChoice(items.createCarrotSeeds(1), false);
            case POTATO_SEED -> new ReplantChoice(items.createPotatoSeeds(1), false);
            case MIXED -> new ReplantChoice(items.createMixedSeeds(1), false);
            case WILD -> new ReplantChoice(null, false);
            default -> null;
        };
    }

    public record ReplantChoice(@Nullable ItemStack replant, boolean consumeFromDrops) {}
}
