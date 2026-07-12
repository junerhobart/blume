package io.blume.enchants;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.Nullable;

public final class BlumeEnchantments {

    public static final TypedKey<Enchantment> AUTO_SMELT =
        EnchantmentKeys.create(Key.key("blume", "auto_smelt"));
    public static final TypedKey<Enchantment> VEINMINER =
        EnchantmentKeys.create(Key.key("blume", "veinminer"));
    public static final TypedKey<Enchantment> TIMBER =
        EnchantmentKeys.create(Key.key("blume", "timber"));
    public static final TypedKey<Enchantment> SICKLE =
        EnchantmentKeys.create(Key.key("blume", "sickle"));
    public static final TypedKey<Enchantment> UNBREAKABLE =
        EnchantmentKeys.create(Key.key("blume", "unbreakable"));

    private static @Nullable Enchantment autoSmelt;
    private static @Nullable Enchantment veinminer;
    private static @Nullable Enchantment timber;
    private static @Nullable Enchantment sickle;
    private static @Nullable Enchantment unbreakable;

    private BlumeEnchantments() {}

    public static void resolve() {
        var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        autoSmelt = registry.get(AUTO_SMELT);
        veinminer = registry.get(VEINMINER);
        timber = registry.get(TIMBER);
        sickle = registry.get(SICKLE);
        unbreakable = registry.get(UNBREAKABLE);
    }

    public static boolean isResolved() {
        return veinminer != null;
    }

    public static @Nullable Enchantment autoSmelt() {
        return autoSmelt;
    }

    public static @Nullable Enchantment veinminer() {
        return veinminer;
    }

    public static @Nullable Enchantment timber() {
        return timber;
    }

    public static @Nullable Enchantment sickle() {
        return sickle;
    }

    public static @Nullable Enchantment unbreakable() {
        return unbreakable;
    }
}
