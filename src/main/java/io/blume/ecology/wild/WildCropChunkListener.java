package io.blume.ecology.wild;

import io.blume.BlumePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class WildCropChunkListener implements Listener {

    private final BlumePlugin plugin;
    private final WildCropPopulator populator;

    public WildCropChunkListener(@NotNull BlumePlugin plugin, @NotNull WildCropPopulator populator) {
        this.plugin = plugin;
        this.populator = populator;
    }

    public void populateLoadedChunks() {
        List<Chunk> chunks = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                chunks.add(chunk);
            }
        }
        if (chunks.isEmpty()) {
            return;
        }

        new BukkitRunnable() {
            private int index;

            @Override
            public void run() {
                if (index >= chunks.size()) {
                    cancel();
                    return;
                }
                populator.populateIfNeeded(chunks.get(index++));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        populator.populateIfNeeded(event.getChunk());
    }
}
