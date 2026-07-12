package io.blume.enchants.timber;

import io.blume.enchants.BlumeEnchantments;
import io.blume.enchants.EnchantChecks;
import io.blume.enchants.EnchantContext;
import io.blume.enchants.EnchantKeys;
import io.blume.enchants.EnchantsConfig;
import io.blume.enchants.Logs;
import io.blume.enchants.ManualBreak;
import io.blume.enchants.VeinFill;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TimberListener implements Listener {

    private static final int MULTI_TRUNK_BASE_LOGS = 2;

    private final EnchantsConfig config;
    private final EnchantKeys keys;

    public TimberListener(EnchantsConfig config, EnchantKeys keys) {
        this.config = config;
        this.keys = keys;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!Logs.isTimber(event.getBlock().getType())) {
            return;
        }
        keys.markPlayerPlaced(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (EnchantContext.isProcessing()) {
            return;
        }
        Block origin = event.getBlock();
        if (!Logs.isTimber(origin.getType())) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.ADVENTURE) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!EnchantChecks.hasKey(tool, BlumeEnchantments.TIMBER)) {
            return;
        }

        List<Block> tree = VeinFill.collectFamily(
            origin,
            config.timberMaxBlocks(),
            this::isNaturalTimber,
            Logs::isTimber,
            Logs::sameFamily
        );
        if (tree.size() <= 1) {
            return;
        }

        event.setCancelled(true);
        Block stump = tree.stream()
            .min(Comparator.comparingInt(block -> block.getY()))
            .orElse(origin);
        Material stumpType = stump.getType();
        Material sapling = Logs.saplingFor(stumpType);
        int stumpY = stump.getY();
        List<Location> sites = computeReplantSites(stump, stumpY, sapling, tree);

        EnchantContext.runWhileProcessing(() -> {
            for (Block block : tree) {
                if (block.getType().isAir()) {
                    continue;
                }
                // Re-fetch each iteration: the axe may break mid-fell.
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType().isAir() || !ManualBreak.canBreakOneMore(player, held)) {
                    break;
                }
                ManualBreak.breakAsPlayer(player, block, held, false);
            }
            if (config.isTimberReplantSaplings() && sapling != null) {
                for (Location site : sites) {
                    tryPlantSapling(site.getBlock(), sapling);
                }
            }
        });
    }

    private boolean isNaturalTimber(@NotNull Block block) {
        if (!Logs.isTimber(block.getType())) {
            return false;
        }
        return !keys.isPlayerPlaced(block);
    }

    private static @NotNull List<Location> computeReplantSites(
        @NotNull Block stump,
        int stumpY,
        @Nullable Material sapling,
        @NotNull List<Block> tree
    ) {
        if (sapling == null) {
            return List.of();
        }

        int baseLogs = 0;
        int minX = stump.getX();
        int minZ = stump.getZ();
        for (Block block : tree) {
            if (block.getY() != stumpY || !Logs.sameFamily(block.getType(), stump.getType())) {
                continue;
            }
            baseLogs++;
            minX = Math.min(minX, block.getX());
            minZ = Math.min(minZ, block.getZ());
        }

        boolean multiTrunk = baseLogs >= MULTI_TRUNK_BASE_LOGS;
        if (!multiTrunk) {
            return List.of(stump.getLocation());
        }

        World world = stump.getWorld();
        List<Location> sites = new ArrayList<>(4);
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                sites.add(new Location(world, minX + dx, stumpY, minZ + dz));
            }
        }
        return sites;
    }

    private void tryPlantSapling(@NotNull Block stump, @NotNull Material sapling) {
        Block ground = stump.getRelative(BlockFace.DOWN);
        if (!canPlantOn(ground.getType(), sapling)) {
            return;
        }
        Block above = stump.getRelative(BlockFace.UP);
        if (!above.getType().isAir() && !(above.getBlockData() instanceof Leaves)) {
            return;
        }
        stump.setType(sapling, false);
    }

    private static boolean canPlantOn(@NotNull Material ground, @NotNull Material sapling) {
        if (sapling == Material.MANGROVE_PROPAGULE) {
            return ground == Material.GRASS_BLOCK
                || ground == Material.DIRT
                || ground == Material.COARSE_DIRT
                || ground == Material.ROOTED_DIRT
                || ground == Material.PODZOL
                || ground == Material.MYCELIUM
                || ground == Material.MUD
                || ground == Material.MUDDY_MANGROVE_ROOTS
                || ground == Material.MOSS_BLOCK
                || ground == Material.CLAY;
        }
        if (sapling == Material.CRIMSON_FUNGUS || sapling == Material.WARPED_FUNGUS) {
            return ground == Material.CRIMSON_NYLIUM || ground == Material.WARPED_NYLIUM;
        }
        return ground == Material.GRASS_BLOCK
            || ground == Material.DIRT
            || ground == Material.COARSE_DIRT
            || ground == Material.ROOTED_DIRT
            || ground == Material.PODZOL
            || ground == Material.MYCELIUM;
    }
}
