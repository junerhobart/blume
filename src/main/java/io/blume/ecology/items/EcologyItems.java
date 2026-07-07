package io.blume.ecology.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class EcologyItems {

    public static final String CARROT = "carrot";
    public static final String POTATO = "potato";
    public static final String MIXED = "mixed";

    public static final List<String> GIVE_IDS = List.of(
        "carrot_seeds",
        "potato_seeds",
        "mixed_seeds"
    );

    private static final Key CARROT_MODEL = Key.key("blume", "carrot_seeds");
    private static final Key POTATO_MODEL = Key.key("blume", "potato_seeds");
    private static final Key MIXED_MODEL = Key.key("blume", "mixed_seeds");

    private static final String CARROT_NAME = "Carrot Seeds";
    private static final String POTATO_NAME = "Potato Seeds";
    private static final String MIXED_NAME = "Mixed Seeds";

    public EcologyItems() {
    }

    public boolean isSeedType(@Nullable ItemStack stack, @NotNull String type) {
        return type.equals(seedType(stack));
    }

    public boolean isCarrotSeeds(@Nullable ItemStack stack) {
        return isSeedType(stack, CARROT);
    }

    public boolean isPotatoSeeds(@Nullable ItemStack stack) {
        return isSeedType(stack, POTATO);
    }

    public boolean isMixedSeeds(@Nullable ItemStack stack) {
        return isSeedType(stack, MIXED);
    }

    public boolean isAnyEcologySeed(@Nullable ItemStack stack) {
        return seedType(stack) != null;
    }

    public @Nullable ItemStack createByGiveId(@NotNull String giveId, int amount) {
        return switch (giveId.toLowerCase(Locale.ROOT)) {
            case "carrot_seeds" -> createCarrotSeeds(amount);
            case "potato_seeds" -> createPotatoSeeds(amount);
            case "mixed_seeds" -> createMixedSeeds(amount);
            default -> null;
        };
    }

    public @NotNull ItemStack createCarrotSeeds(int amount) {
        return createSeed(CARROT_NAME, CARROT_MODEL, amount);
    }

    public @NotNull ItemStack createPotatoSeeds(int amount) {
        return createSeed(POTATO_NAME, POTATO_MODEL, amount);
    }

    public @NotNull ItemStack createMixedSeeds(int amount) {
        return createSeed(MIXED_NAME, MIXED_MODEL, amount);
    }

    private @NotNull ItemStack createSeed(
        @NotNull String displayName,
        @NotNull Key model,
        int amount
    ) {
        ItemStack stack = ItemStack.of(Material.WHEAT_SEEDS, Math.max(1, amount));
        Component name = Component.text(displayName)
            .decoration(TextDecoration.ITALIC, false);
        stack.setData(DataComponentTypes.ITEM_NAME, name);
        stack.setData(DataComponentTypes.ITEM_MODEL, model);
        stack.unsetData(DataComponentTypes.CUSTOM_NAME);
        return stack;
    }

    private @Nullable String seedType(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }

        String fromModel = typeFromModel(stack.getData(DataComponentTypes.ITEM_MODEL));
        if (fromModel != null) {
            return fromModel;
        }

        return null;
    }

    private static @Nullable String typeFromModel(@Nullable Key model) {
        if (model == null) {
            return null;
        }
        if (CARROT_MODEL.equals(model)) {
            return CARROT;
        }
        if (POTATO_MODEL.equals(model)) {
            return POTATO;
        }
        if (MIXED_MODEL.equals(model)) {
            return MIXED;
        }
        return null;
    }
}
