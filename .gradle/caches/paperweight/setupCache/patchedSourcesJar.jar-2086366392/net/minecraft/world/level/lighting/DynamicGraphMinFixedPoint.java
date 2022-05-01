package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.function.LongPredicate;
import net.minecraft.util.Mth;

public abstract class DynamicGraphMinFixedPoint {
    private static final int NO_COMPUTED_LEVEL = 255;
    private final int levelCount;
    private final LongLinkedOpenHashSet[] queues;
    private final Long2ByteMap computedLevels;
    private int firstQueuedLevel;
    private volatile boolean hasWork;

    protected DynamicGraphMinFixedPoint(int levelCount, int expectedLevelSize, int expectedTotalSize) {
        if (levelCount >= 254) {
            throw new IllegalArgumentException("Level count must be < 254.");
        } else {
            this.levelCount = levelCount;
            this.queues = new LongLinkedOpenHashSet[levelCount];

            for(int i = 0; i < levelCount; ++i) {
                this.queues[i] = new LongLinkedOpenHashSet(expectedLevelSize, 0.5F) {
                    protected void rehash(int i) {
                        if (i > expectedLevelSize) {
                            super.rehash(i);
                        }

                    }
                };
            }

            this.computedLevels = new Long2ByteOpenHashMap(expectedTotalSize, 0.5F) {
                protected void rehash(int i) {
                    if (i > expectedTotalSize) {
                        super.rehash(i);
                    }

                }
            };
            this.computedLevels.defaultReturnValue((byte)-1);
            this.firstQueuedLevel = levelCount;
        }
    }

    private int getKey(int a, int b) {
        int i = a;
        if (a > b) {
            i = b;
        }

        if (i > this.levelCount - 1) {
            i = this.levelCount - 1;
        }

        return i;
    }

    private void checkFirstQueuedLevel(int maxLevel) {
        int i = this.firstQueuedLevel;
        this.firstQueuedLevel = maxLevel;

        for(int j = i + 1; j < maxLevel; ++j) {
            if (!this.queues[j].isEmpty()) {
                this.firstQueuedLevel = j;
                break;
            }
        }

    }

    protected void removeFromQueue(long id) {
        int i = this.computedLevels.get(id) & 255;
        if (i != 255) {
            int j = this.getLevel(id);
            int k = this.getKey(j, i);
            this.dequeue(id, k, this.levelCount, true);
            this.hasWork = this.firstQueuedLevel < this.levelCount;
        }
    }

    public void removeIf(LongPredicate predicate) {
        LongList longList = new LongArrayList();
        this.computedLevels.keySet().forEach((l) -> {
            if (predicate.test(l)) {
                longList.add(l);
            }

        });
        longList.forEach(this::removeFromQueue);
    }

    private void dequeue(long id, int level, int levelCount, boolean removeFully) {
        if (removeFully) {
            this.computedLevels.remove(id);
        }

        this.queues[level].remove(id);
        if (this.queues[level].isEmpty() && this.firstQueuedLevel == level) {
            this.checkFirstQueuedLevel(levelCount);
        }

    }

    private void enqueue(long id, int level, int targetLevel) {
        this.computedLevels.put(id, (byte)level);
        this.queues[targetLevel].add(id);
        if (this.firstQueuedLevel > targetLevel) {
            this.firstQueuedLevel = targetLevel;
        }

    }

    protected void checkNode(long id) {
        this.checkEdge(id, id, this.levelCount - 1, false);
    }

    protected void checkEdge(long sourceId, long id, int level, boolean decrease) {
        this.checkEdge(sourceId, id, level, this.getLevel(id), this.computedLevels.get(id) & 255, decrease);
        this.hasWork = this.firstQueuedLevel < this.levelCount;
    }

    private void checkEdge(long sourceId, long id, int level, int currentLevel, int pendingLevel, boolean decrease) {
        if (!this.isSource(id)) {
            level = Mth.clamp(level, 0, this.levelCount - 1);
            currentLevel = Mth.clamp(currentLevel, 0, this.levelCount - 1);
            boolean bl;
            if (pendingLevel == 255) {
                bl = true;
                pendingLevel = currentLevel;
            } else {
                bl = false;
            }

            int i;
            if (decrease) {
                i = Math.min(pendingLevel, level);
            } else {
                i = Mth.clamp(this.getComputedLevel(id, sourceId, level), 0, this.levelCount - 1);
            }

            int k = this.getKey(currentLevel, pendingLevel);
            if (currentLevel != i) {
                int l = this.getKey(currentLevel, i);
                if (k != l && !bl) {
                    this.dequeue(id, k, l, false);
                }

                this.enqueue(id, i, l);
            } else if (!bl) {
                this.dequeue(id, k, this.levelCount, true);
            }

        }
    }

    protected final void checkNeighbor(long sourceId, long targetId, int level, boolean decrease) {
        int i = this.computedLevels.get(targetId) & 255;
        int j = Mth.clamp(this.computeLevelFromNeighbor(sourceId, targetId, level), 0, this.levelCount - 1);
        if (decrease) {
            this.checkEdge(sourceId, targetId, j, this.getLevel(targetId), i, true);
        } else {
            int k;
            boolean bl;
            if (i == 255) {
                bl = true;
                k = Mth.clamp(this.getLevel(targetId), 0, this.levelCount - 1);
            } else {
                k = i;
                bl = false;
            }

            if (j == k) {
                this.checkEdge(sourceId, targetId, this.levelCount - 1, bl ? k : this.getLevel(targetId), i, false);
            }
        }

    }

    protected final boolean hasWork() {
        return this.hasWork;
    }

    protected final int runUpdates(int maxSteps) {
        if (this.firstQueuedLevel >= this.levelCount) {
            return maxSteps;
        } else {
            while(this.firstQueuedLevel < this.levelCount && maxSteps > 0) {
                --maxSteps;
                LongLinkedOpenHashSet longLinkedOpenHashSet = this.queues[this.firstQueuedLevel];
                long l = longLinkedOpenHashSet.removeFirstLong();
                int i = Mth.clamp(this.getLevel(l), 0, this.levelCount - 1);
                if (longLinkedOpenHashSet.isEmpty()) {
                    this.checkFirstQueuedLevel(this.levelCount);
                }

                int j = this.computedLevels.remove(l) & 255;
                if (j < i) {
                    this.setLevel(l, j);
                    this.checkNeighborsAfterUpdate(l, j, true);
                } else if (j > i) {
                    this.enqueue(l, j, this.getKey(this.levelCount - 1, j));
                    this.setLevel(l, this.levelCount - 1);
                    this.checkNeighborsAfterUpdate(l, i, false);
                }
            }

            this.hasWork = this.firstQueuedLevel < this.levelCount;
            return maxSteps;
        }
    }

    public int getQueueSize() {
        return this.computedLevels.size();
    }

    protected abstract boolean isSource(long id);

    protected abstract int getComputedLevel(long id, long excludedId, int maxLevel);

    protected abstract void checkNeighborsAfterUpdate(long id, int level, boolean decrease);

    protected abstract int getLevel(long id);

    protected abstract void setLevel(long id, int level);

    protected abstract int computeLevelFromNeighbor(long sourceId, long targetId, int level);
}
