package io.blume.ecology.wild;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.blume.ecology.EcologyKeys;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class WildCropGuardListener implements Listener {

    private final EcologyKeys keys;

    public WildCropGuardListener(@NotNull EcologyKeys keys) {
        this.keys = keys;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (isWildCrop(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDestroy(BlockDestroyEvent event) {
        if (isWildCrop(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDestroyCleanup(BlockDestroyEvent event) {
        if (event.isCancelled() || !isWildCrop(event.getBlock())) {
            return;
        }
        clearWildCrop(event.getBlock());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isWildCrop(event.getBlock())) {
            return;
        }
        clearWildCrop(event.getBlock());
    }

    // Explosions bypass BlockBreakEvent/BlockDestroyEvent; without these the
    // wild-crop PDC entries would be orphaned on the chunk forever.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        clearWildCrops(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        clearWildCrops(event.blockList());
    }

    private void clearWildCrops(@NotNull List<Block> blocks) {
        for (Block block : blocks) {
            if (isWildCrop(block)) {
                clearWildCrop(block);
            }
        }
    }

    private boolean isWildCrop(@NotNull Block block) {
        return block.getChunk().getPersistentDataContainer().has(keys.wildCrop(block), PersistentDataType.STRING);
    }

    private void clearWildCrop(@NotNull Block block) {
        decrementChunkCount(block);
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        pdc.remove(keys.wildCrop(block));
        pdc.remove(keys.cropOrigin(block));
    }

    private void decrementChunkCount(@NotNull Block block) {
        Chunk chunk = block.getChunk();
        Integer count = chunk.getPersistentDataContainer().get(keys.wildCropCount(), PersistentDataType.INTEGER);
        if (count != null && count > 0) {
            chunk.getPersistentDataContainer().set(keys.wildCropCount(), PersistentDataType.INTEGER, count - 1);
        }
    }
}
