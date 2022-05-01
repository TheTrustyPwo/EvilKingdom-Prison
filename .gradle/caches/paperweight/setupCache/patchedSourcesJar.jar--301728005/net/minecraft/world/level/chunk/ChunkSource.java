package net.minecraft.world.level.chunk;

import java.io.IOException;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.LevelLightEngine;

public abstract class ChunkSource implements LightChunkGetter, AutoCloseable {
    @Nullable
    public LevelChunk getChunk(int chunkX, int chunkZ, boolean create) {
        return (LevelChunk)this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, create);
    }

    @Nullable
    public LevelChunk getChunkNow(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, false);
    }

    @Nullable
    @Override
    public BlockGetter getChunkForLighting(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
    }

    public boolean hasChunk(int x, int z) {
        return this.getChunk(x, z, ChunkStatus.FULL, false) != null;
    }

    @Nullable
    public abstract ChunkAccess getChunk(int x, int z, ChunkStatus leastStatus, boolean create);

    public abstract void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks);

    public abstract String gatherStats();

    public abstract int getLoadedChunksCount();

    @Override
    public void close() throws IOException {
    }

    public abstract LevelLightEngine getLightEngine();

    public void setSpawnSettings(boolean spawnMonsters, boolean spawnAnimals) {
    }

    public void updateChunkForced(ChunkPos pos, boolean forced) {
    }
}
