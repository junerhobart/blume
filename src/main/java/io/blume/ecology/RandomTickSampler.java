package io.blume.ecology;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class RandomTickSampler {

    private static final int MAX_CHUNKS_PER_TICK = 8;

    private final Map<World, Integer> chunkCursor = new WeakHashMap<>();

    public void sampleBlocks(@NotNull World world, @NotNull Consumer<Block> handler) {
        if (!RandomTickPace.isActive(world)) {
            return;
        }
        Chunk[] chunks = world.getLoadedChunks();
        if (chunks.length == 0) {
            return;
        }

        int speed = RandomTickPace.speed(world);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int cursor = chunkCursor.getOrDefault(world, 0) % chunks.length;
        int toProcess = Math.min(MAX_CHUNKS_PER_TICK, chunks.length);
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int yRange = maxY - minY + 1;

        for (int i = 0; i < toProcess; i++) {
            Chunk chunk = chunks[(cursor + i) % chunks.length];
            int baseX = chunk.getX() << 4;
            int baseZ = chunk.getZ() << 4;
            for (int s = 0; s < speed; s++) {
                int x = baseX + random.nextInt(16);
                int y = minY + random.nextInt(yRange);
                int z = baseZ + random.nextInt(16);
                handler.accept(world.getBlockAt(x, y, z));
            }
        }
        chunkCursor.put(world, (cursor + toProcess) % chunks.length);
    }
}
