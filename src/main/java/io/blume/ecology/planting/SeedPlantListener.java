package io.blume.ecology.planting;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.blume.BlumePlugin;
import io.blume.ecology.EcologyConfig;
import io.blume.ecology.harvest.CropOriginHelper;
import io.blume.ecology.items.EcologyItems;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class SeedPlantListener implements Listener {

    private static final List<Material> MIXED_CROPS = List.of(
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS
    );

    private final BlumePlugin plugin;
    private final EcologyConfig config;
    private final EcologyItems items;
    private final CropOriginHelper originHelper;

    public SeedPlantListener(
        @NotNull BlumePlugin plugin,
        @NotNull EcologyConfig config,
        @NotNull EcologyItems items,
        @NotNull CropOriginHelper originHelper
    ) {
        this.plugin = plugin;
        this.config = config;
        this.items = items;
        this.originHelper = originHelper;
    }

    // ignoreCancelled: protection plugins (claims, WorldGuard) cancel the
    // interact event; planting must respect that.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.FARMLAND) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.ADVENTURE) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }

        // If the space above isn't plantable, don't cancel vanilla — otherwise
        // the click becomes a silent no-op with the item stuck in hand.
        Material above = clicked.getRelative(BlockFace.UP).getType();
        if (!above.isAir() && above != Material.SHORT_GRASS) {
            return;
        }

        if (items.isAnyEcologySeed(item)) {
            cancelVanilla(event);
            if (items.isCarrotSeeds(item)) {
                plantCrop(player, clicked, Material.CARROTS, CropOriginHelper.CARROT, event.getHand());
                fixAccidentalWheat(clicked, Material.CARROTS, CropOriginHelper.CARROT);
            } else if (items.isPotatoSeeds(item)) {
                plantCrop(player, clicked, Material.POTATOES, CropOriginHelper.POTATO_SEED, event.getHand());
                fixAccidentalWheat(clicked, Material.POTATOES, CropOriginHelper.POTATO_SEED);
            } else if (items.isMixedSeeds(item)) {
                plantMixedSeed(player, clicked, event.getHand());
            }
            return;
        }

        Material type = item.getType();
        if (type == Material.CARROT) {
            cancelVanilla(event);
            plantCrop(player, clicked, Material.CARROTS, CropOriginHelper.CARROT, event.getHand());
            return;
        }
        if (type == Material.POTATO) {
            cancelVanilla(event);
            plantCrop(player, clicked, Material.POTATOES, CropOriginHelper.POTATO, event.getHand());
        }
    }

    // Origin PDC entries are written per planted crop; without cleanup on
    // destruction they accumulate on the chunk forever.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        clearOriginIfPresent(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDestroy(BlockDestroyEvent event) {
        clearOriginIfPresent(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            clearOriginIfPresent(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            clearOriginIfPresent(block);
        }
    }

    private void clearOriginIfPresent(@NotNull Block block) {
        if (originHelper.hasOrigin(block)) {
            originHelper.clearOrigin(block);
        }
    }

    private void fixAccidentalWheat(
        @NotNull Block farmland,
        @NotNull Material crop,
        @NotNull String origin
    ) {
        Block target = farmland.getRelative(BlockFace.UP);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (target.getType() != Material.WHEAT) {
                return;
            }
            target.setType(crop, false);
            if (target.getBlockData() instanceof Ageable ageable) {
                ageable.setAge(0);
                target.setBlockData(ageable, false);
            }
            originHelper.writeOrigin(target, origin);
        });
    }

    private void cancelVanilla(@NotNull PlayerInteractEvent event) {
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    private void plantCrop(
        @NotNull Player player,
        @NotNull Block farmland,
        @NotNull Material crop,
        @NotNull String origin,
        @NotNull EquipmentSlot hand
    ) {
        Block target = farmland.getRelative(0, 1, 0);
        if (!target.isEmpty()) {
            return;
        }

        target.setType(crop, false);
        if (target.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            target.setBlockData(ageable, false);
        }
        originHelper.writeOrigin(target, origin);
        consumeOne(player, hand);
    }

    private void plantMixedSeed(@NotNull Player player, @NotNull Block farmland, @NotNull EquipmentSlot hand) {
        Block above = farmland.getRelative(0, 1, 0);
        if (!above.isEmpty() && above.getType() != Material.SHORT_GRASS) {
            return;
        }

        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < config.mixedGrassChance()) {
            above.setType(Material.SHORT_GRASS, false);
            consumeOne(player, hand);
            return;
        }

        Material crop = MIXED_CROPS.get(ThreadLocalRandom.current().nextInt(MIXED_CROPS.size()));
        plantCrop(player, farmland, crop, CropOriginHelper.MIXED, hand);
    }

    private void consumeOne(@NotNull Player player, @NotNull EquipmentSlot hand) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack stack = handItem(player, hand);
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
            return;
        }
        setHandItem(player, hand, ItemStack.empty());
    }

    private static @Nullable ItemStack handItem(@NotNull Player player, @NotNull EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND
            ? player.getInventory().getItemInOffHand()
            : player.getInventory().getItemInMainHand();
    }

    private static void setHandItem(@NotNull Player player, @NotNull EquipmentSlot hand, @NotNull ItemStack stack) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(stack);
        } else {
            player.getInventory().setItemInMainHand(stack);
        }
    }
}
