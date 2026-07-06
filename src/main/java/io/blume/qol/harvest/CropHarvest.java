package io.blume.qol.harvest;

import io.blume.ecology.harvest.CropOriginHelper;
import io.blume.ecology.harvest.PoisonPotatoHandler;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class CropHarvest {

    public static final Set<Material> CROPS = EnumSet.of(
        Material.WHEAT,
        Material.BEETROOTS,
        Material.CARROTS,
        Material.POTATOES,
        Material.NETHER_WART
    );

    private static CropOriginHelper originHelper;
    private static PoisonPotatoHandler poisonHandler;

    private CropHarvest() {}

    public static void inject(@Nullable CropOriginHelper originHelper, @Nullable PoisonPotatoHandler poisonHandler) {
        CropHarvest.originHelper = originHelper;
        CropHarvest.poisonHandler = poisonHandler;
    }

    public static boolean isMatureCrop(@NotNull Block block) {
        if (!CROPS.contains(block.getType())) {
            return false;
        }
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    public static void harvestAndReplant(@NotNull Player player, @NotNull Block block, @NotNull ItemStack tool) {
        if (!isMatureCrop(block)) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>(block.getDrops(tool));
        if (poisonHandler != null) {
            poisonHandler.modifyDrops(block, drops);
        }

        ReplantResult replant = determineReplant(block);
        if (replant == null) {
            dropAll(player, block, drops);
            breakBlock(player, block, tool);
            return;
        }

        if (replant.consumeFromDrops() && !consumeOne(drops, replant.material())) {
            if (canReplantInPlace(block, drops, replant.material())) {
                dropAll(player, block, drops);
                resetAge(block);
                return;
            }
            dropAll(player, block, drops);
            breakBlock(player, block, tool);
            return;
        }

        dropAll(player, block, drops);
        resetAge(block);
    }

    private static void breakBlock(@NotNull Player player, @NotNull Block block, @NotNull ItemStack tool) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            block.setType(Material.AIR, false);
            return;
        }
        block.breakNaturally(tool, true);
    }

    private static @Nullable ReplantResult determineReplant(@NotNull Block block) {
        if (originHelper != null) {
            CropOriginHelper.ReplantChoice choice = originHelper.replantChoice(block);
            if (choice != null) {
                if (choice.replant() == null) {
                    return null;
                }
                return new ReplantResult(choice.replant().getType(), choice.consumeFromDrops());
            }
            if (originHelper.usesInPlaceReplant(block)) {
                return new ReplantResult(Material.WHEAT_SEEDS, false);
            }
        }

        ReplantInfo info = replantInfo(block.getType());
        return info == null ? null : new ReplantResult(info.material(), true);
    }

    private static void resetAge(@NotNull Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable, false);
        }
    }

    private static void dropAll(@NotNull Player player, @NotNull Block block, @NotNull List<ItemStack> drops) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) {
                continue;
            }
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
        }
    }

    private static boolean canReplantInPlace(
        @NotNull Block block,
        @NotNull List<ItemStack> drops,
        @NotNull Material replantMaterial
    ) {
        if (originHelper != null && originHelper.usesInPlaceReplant(block)) {
            return true;
        }
        return replantMaterial == Material.POTATO
            && !containsMaterial(drops, Material.POTATO)
            && containsMaterial(drops, Material.POISONOUS_POTATO);
    }

    private static boolean containsMaterial(@NotNull List<ItemStack> drops, @NotNull Material material) {
        for (ItemStack drop : drops) {
            if (drop != null && drop.getType() == material && drop.getAmount() > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeOne(@NotNull List<ItemStack> drops, @NotNull Material material) {
        for (ItemStack drop : drops) {
            if (drop != null && drop.getType() == material && drop.getAmount() > 0) {
                drop.setAmount(drop.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static ReplantInfo replantInfo(@NotNull Material crop) {
        return switch (crop) {
            case WHEAT -> new ReplantInfo(Material.WHEAT_SEEDS);
            case BEETROOTS -> new ReplantInfo(Material.BEETROOT_SEEDS);
            case CARROTS -> new ReplantInfo(Material.CARROT);
            case POTATOES -> new ReplantInfo(Material.POTATO);
            case NETHER_WART -> new ReplantInfo(Material.NETHER_WART);
            default -> null;
        };
    }

    public record ReplantInfo(@NotNull Material material) {}

    private record ReplantResult(@NotNull Material material, boolean consumeFromDrops) {}
}
