package io.blume.ecology.harvest;

import io.blume.ecology.EcologyConfig;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PoisonPotatoHandler implements Listener {

    private final CropOriginHelper originHelper;
    private final EcologyConfig config;

    public PoisonPotatoHandler(@NotNull CropOriginHelper originHelper, @NotNull EcologyConfig config) {
        this.originHelper = originHelper;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (shouldApply(block) && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            Player player = event.getPlayer();
            ItemStack tool = player.getInventory().getItemInMainHand();
            List<ItemStack> drops = new ArrayList<>(block.getDrops(tool));
            if (modifyDrops(block, drops)) {
                event.setDropItems(false);
                Location center = block.getLocation().add(0.5, 0.5, 0.5);
                for (ItemStack drop : drops) {
                    if (drop != null && !drop.getType().isAir() && drop.getAmount() > 0) {
                        block.getWorld().dropItemNaturally(center, drop);
                    }
                }
            }
        }

        if (originHelper.hasOrigin(block)) {
            originHelper.clearOrigin(block);
        }
    }

    public boolean modifyDrops(@NotNull Block block, @NotNull List<ItemStack> drops) {
        if (!config.isEnabled()) {
            return false;
        }
        if (!shouldApply(block)) {
            return false;
        }

        double chance = config.potatoPoisonWholeCropChance();
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return false;
        }

        int potatoCount = 0;
        for (ItemStack drop : drops) {
            if (drop != null && drop.getType() == Material.POTATO) {
                potatoCount += drop.getAmount();
            }
        }
        if (potatoCount <= 0) {
            return false;
        }

        drops.removeIf(drop -> drop != null && drop.getType() == Material.POTATO);
        drops.add(new ItemStack(Material.POISONOUS_POTATO, potatoCount));
        return true;
    }

    private boolean shouldApply(@NotNull Block block) {
        if (block.getType() != Material.POTATOES) {
            return false;
        }
        if (!(block.getBlockData() instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
            return false;
        }
        return originHelper.originMatches(block, CropOriginHelper.POTATO_SEED);
    }
}
