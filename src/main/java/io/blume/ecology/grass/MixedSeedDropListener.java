package io.blume.ecology.grass;

import io.blume.ecology.EcologyConfig;
import io.blume.ecology.items.EcologyItems;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class MixedSeedDropListener implements Listener {

    private static final Set<Material> GRASS_PLANTS = Set.of(
        Material.SHORT_GRASS,
        Material.TALL_GRASS,
        Material.FERN,
        Material.LARGE_FERN
    );

    private final EcologyConfig config;
    private final EcologyItems items;

    public MixedSeedDropListener(@NotNull EcologyConfig config, @NotNull EcologyItems items) {
        this.config = config;
        this.items = items;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!config.isEnabled()) {
            return;
        }
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Block block = event.getBlock();
        if (!GRASS_PLANTS.contains(block.getType())) {
            return;
        }

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        List<ItemStack> drops = new ArrayList<>(block.getDrops(tool));
        drops.removeIf(drop -> drop != null && drop.getType() == Material.WHEAT_SEEDS);

        int amount = ThreadLocalRandom.current().nextInt(config.mixedDropMin(), config.mixedDropMax() + 1);
        if (amount > 0) {
            drops.add(items.createMixedSeeds(amount));
        }

        event.setDropItems(false);
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack drop : drops) {
            if (drop != null && !drop.getType().isAir() && drop.getAmount() > 0) {
                block.getWorld().dropItemNaturally(center, drop);
            }
        }
    }
}
