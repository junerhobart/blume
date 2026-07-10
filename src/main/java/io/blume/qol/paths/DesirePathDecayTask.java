package io.blume.qol.paths;

import io.blume.BlumePlugin;
import io.blume.ecology.RandomTickPace;
import io.blume.qol.QolConfig;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

public final class DesirePathDecayTask {

    private static final int MAX_CHUNKS_PER_TICK = 8;
    private static final long START_DELAY_TICKS = 40L;

    private final BlumePlugin plugin;
    private final DesirePathService service;
    private final QolConfig config;
    private final Map<World, Integer> chunkCursor = new WeakHashMap<>();
    private BukkitTask task;

    public DesirePathDecayTask(
        @NotNull BlumePlugin plugin,
        @NotNull DesirePathService service,
        @NotNull QolConfig config
    ) {
        this.plugin = plugin;
        this.service = service;
        this.config = config;
    }

    public void start() {
        stop();
        if (!config.isDesirePathDeErosionEnabled()) {
            return;
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tickLoadedChunks();
            }
        }.runTaskTimer(plugin, START_DELAY_TICKS, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tickLoadedChunks() {
        if (!config.isDesirePathDeErosionEnabled()) {
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            if (!RandomTickPace.isActive(world)) {
                continue;
            }
            Chunk[] chunks = world.getLoadedChunks();
            if (chunks.length == 0) {
                continue;
            }

            int cursor = chunkCursor.getOrDefault(world, 0) % chunks.length;
            int toProcess = Math.min(MAX_CHUNKS_PER_TICK, chunks.length);
            for (int i = 0; i < toProcess; i++) {
                service.decayChunk(chunks[(cursor + i) % chunks.length]);
            }
            chunkCursor.put(world, (cursor + toProcess) % chunks.length);
        }
    }
}
