package net.minecraft.server.level;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class SectionTracker extends DynamicGraphMinFixedPoint {
    protected SectionTracker(int levelCount, int expectedLevelSize, int expectedTotalSize) {
        super(levelCount, expectedLevelSize, expectedTotalSize);
    }

    @Override
    protected boolean isSource(long id) {
        return id == Long.MAX_VALUE;
    }

    @Override
    protected void checkNeighborsAfterUpdate(long id, int level, boolean decrease) {
        for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
                for(int k = -1; k <= 1; ++k) {
                    long l = SectionPos.offset(id, i, j, k);
                    if (l != id) {
                        this.checkNeighbor(id, l, level, decrease);
                    }
                }
            }
        }

    }

    @Override
    protected int getComputedLevel(long id, long excludedId, int maxLevel) {
        int i = maxLevel;

        for(int j = -1; j <= 1; ++j) {
            for(int k = -1; k <= 1; ++k) {
                for(int l = -1; l <= 1; ++l) {
                    long m = SectionPos.offset(id, j, k, l);
                    if (m == id) {
                        m = Long.MAX_VALUE;
                    }

                    if (m != excludedId) {
                        int n = this.computeLevelFromNeighbor(m, id, this.getLevel(m));
                        if (i > n) {
                            i = n;
                        }

                        if (i == 0) {
                            return i;
                        }
                    }
                }
            }
        }

        return i;
    }

    @Override
    protected int computeLevelFromNeighbor(long sourceId, long targetId, int level) {
        return sourceId == Long.MAX_VALUE ? this.getLevelFromSource(targetId) : level + 1;
    }

    protected abstract int getLevelFromSource(long id);

    public void update(long id, int level, boolean decrease) {
        this.checkEdge(Long.MAX_VALUE, id, level, decrease);
    }
}
