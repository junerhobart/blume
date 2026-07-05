package io.blume.enchants;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class BlumeBootstrap implements PluginBootstrap {

    private static final int COMMON_WEIGHT = 10;
    private static final int TREASURE_WEIGHT = 1;
    private static final EnchantmentRegistryEntry.EnchantmentCost COMMON_MIN =
        EnchantmentRegistryEntry.EnchantmentCost.of(5, 15);
    private static final EnchantmentRegistryEntry.EnchantmentCost COMMON_MAX =
        EnchantmentRegistryEntry.EnchantmentCost.of(15, 65);
    private static final EnchantmentRegistryEntry.EnchantmentCost TREASURE_MIN =
        EnchantmentRegistryEntry.EnchantmentCost.of(25, 25);
    private static final EnchantmentRegistryEntry.EnchantmentCost TREASURE_MAX =
        EnchantmentRegistryEntry.EnchantmentCost.of(75, 75);

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(
            RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {
                event.registry().register(BlumeEnchantments.AUTO_SMELT, b ->
                    commonPickaxe(event, b, "Auto Smelt", 1));
                event.registry().register(BlumeEnchantments.VEINMINER, b ->
                    commonPickaxe(event, b, "Veinminer", 1));
                event.registry().register(BlumeEnchantments.TIMBER, b ->
                    commonAxe(event, b, "Timber", 1));
                event.registry().register(BlumeEnchantments.SICKLE, b ->
                    commonHoe(event, b, "Sickle", 3));
                event.registry().register(BlumeEnchantments.UNBREAKABLE, b ->
                    unbreakable(event, b));
            })
        );

        context.getLifecycleManager().registerEventHandler(
            LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT).newHandler(event -> {
                PostFlattenTagRegistrar<Enchantment> registrar = event.registrar();
                Set<TypedKey<Enchantment>> common = Set.of(
                    BlumeEnchantments.AUTO_SMELT,
                    BlumeEnchantments.VEINMINER,
                    BlumeEnchantments.TIMBER,
                    BlumeEnchantments.SICKLE
                );
                registrar.addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, common);
                registrar.addToTag(EnchantmentTagKeys.TRADEABLE, common);
                registrar.addToTag(EnchantmentTagKeys.NON_TREASURE, common);
                for (TagKey<Enchantment> tradeTag : tradeCommonTags()) {
                    registrar.addToTag(tradeTag, common);
                }

                Set<TypedKey<Enchantment>> treasure = Set.of(BlumeEnchantments.UNBREAKABLE);
                registrar.addToTag(EnchantmentTagKeys.TREASURE, treasure);
                registrar.addToTag(EnchantmentTagKeys.ON_RANDOM_LOOT, treasure);
            })
        );
    }

    private void commonPickaxe(
        @NotNull RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> event,
        @NotNull EnchantmentRegistryEntry.Builder builder,
        @NotNull String displayName,
        int maxLevel
    ) {
        builder
            .description(enchantName(displayName))
            .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.PICKAXES))
            .primaryItems(event.getOrCreateTag(ItemTypeTagKeys.PICKAXES))
            .weight(COMMON_WEIGHT)
            .maxLevel(maxLevel)
            .minimumCost(COMMON_MIN)
            .maximumCost(COMMON_MAX)
            .anvilCost(2)
            .activeSlots(EquipmentSlotGroup.HAND);
    }

    private void commonAxe(
        @NotNull RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> event,
        @NotNull EnchantmentRegistryEntry.Builder builder,
        @NotNull String displayName,
        int maxLevel
    ) {
        builder
            .description(enchantName(displayName))
            .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.AXES))
            .primaryItems(event.getOrCreateTag(ItemTypeTagKeys.AXES))
            .weight(COMMON_WEIGHT)
            .maxLevel(maxLevel)
            .minimumCost(COMMON_MIN)
            .maximumCost(COMMON_MAX)
            .anvilCost(2)
            .activeSlots(EquipmentSlotGroup.HAND);
    }

    private void commonHoe(
        @NotNull RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> event,
        @NotNull EnchantmentRegistryEntry.Builder builder,
        @NotNull String displayName,
        int maxLevel
    ) {
        builder
            .description(enchantName(displayName))
            .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.HOES))
            .primaryItems(event.getOrCreateTag(ItemTypeTagKeys.HOES))
            .weight(COMMON_WEIGHT)
            .maxLevel(maxLevel)
            .minimumCost(COMMON_MIN)
            .maximumCost(COMMON_MAX)
            .anvilCost(2)
            .activeSlots(EquipmentSlotGroup.HAND);
    }

    private void unbreakable(
        @NotNull RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> event,
        @NotNull EnchantmentRegistryEntry.Builder builder
    ) {
        builder
            .description(enchantName("Unbreakable"))
            .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_DURABILITY))
            .primaryItems(event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_DURABILITY))
            .weight(TREASURE_WEIGHT)
            .maxLevel(1)
            .minimumCost(TREASURE_MIN)
            .maximumCost(TREASURE_MAX)
            .anvilCost(8)
            .activeSlots(EquipmentSlotGroup.ANY);
    }

    private static @NotNull Component enchantName(@NotNull String displayName) {
        return Component.text(displayName).decoration(TextDecoration.ITALIC, false);
    }

    private static @NotNull TagKey<Enchantment>[] tradeCommonTags() {
        return new TagKey[] {
            EnchantmentTagKeys.TRADES_DESERT_COMMON,
            EnchantmentTagKeys.TRADES_JUNGLE_COMMON,
            EnchantmentTagKeys.TRADES_PLAINS_COMMON,
            EnchantmentTagKeys.TRADES_SAVANNA_COMMON,
            EnchantmentTagKeys.TRADES_SNOW_COMMON,
            EnchantmentTagKeys.TRADES_SWAMP_COMMON,
            EnchantmentTagKeys.TRADES_TAIGA_COMMON
        };
    }
}
