package io.blume.qol.harvest;

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

    private CropHarvest() {}

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
        ReplantInfo replant = replantInfo(block.getType());
        if (replant == null) {
            dropAll(player, block, drops);
            block.breakNaturally(tool, true);
            return;
        }

        if (!consumeOne(drops, replant.material())) {
            dropAll(player, block, drops);
            block.breakNaturally(tool, true);
            return;
        }

        dropAll(player, block, drops);
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable, false);
        }
    }

    private static void dropAll(@NotNull Player player, @NotNull Block block, @NotNull List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) {
                continue;
            }
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
        }
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
}
