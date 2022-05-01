package net.minecraft.server.level;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class ChunkTracker extends DynamicGraphMinFixedPoint {
    protected ChunkTracker(int levelCount, int expectedLevelSize, int expectedTotalSize) {
        super(levelCount, expectedLevelSize, expectedTotalSize);
    }

    @Override
    protected boolean isSource(long id) {
        return id == ChunkPos.INVALID_CHUNK_POS;
    }

    @Override
    protected void checkNeighborsAfterUpdate(long id, int level, boolean decrease) {
        ChunkPos chunkPos = new ChunkPos(id);
        int i = chunkPos.x;
        int j = chunkPos.z;

        for(int k = -1; k <= 1; ++k) {
            for(int l = -1; l <= 1; ++l) {
                long m = ChunkPos.asLong(i + k, j + l);
                if (m != id) {
                    this.checkNeighbor(id, m, level, decrease);
                }
            }
        }

    }

    @Override
    protected int getComputedLevel(long id, long excludedId, int maxLevel) {
        int i = maxLevel;
        ChunkPos chunkPos = new ChunkPos(id);
        int j = chunkPos.x;
        int k = chunkPos.z;

        for(int l = -1; l <= 1; ++l) {
            for(int m = -1; m <= 1; ++m) {
                long n = ChunkPos.asLong(j + l, k + m);
                if (n == id) {
                    n = ChunkPos.INVALID_CHUNK_POS;
                }

                if (n != excludedId) {
                    int o = this.computeLevelFromNeighbor(n, id, this.getLevel(n));
                    if (i > o) {
                        i = o;
                    }

                    if (i == 0) {
                        return i;
                    }
                }
            }
        }

        return i;
    }

    @Override
    protected int computeLevelFromNeighbor(long sourceId, long targetId, int level) {
        return sourceId == ChunkPos.INVALID_CHUNK_POS ? this.getLevelFromSource(targetId) : level + 1;
    }

    protected abstract int getLevelFromSource(long id);

    public void update(long chunkPos, int distance, boolean decrease) {
        this.checkEdge(ChunkPos.INVALID_CHUNK_POS, chunkPos, distance, decrease);
    }
}
