package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.slf4j.Logger;

public class ThreadedLevelLightEngine extends LevelLightEngine implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ProcessorMailbox<Runnable> taskMailbox;
    private final ObjectList<Pair<ThreadedLevelLightEngine.TaskType, Runnable>> lightTasks = new ObjectArrayList<>();
    private final ChunkMap chunkMap;
    private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> sorterMailbox;
    private volatile int taskPerBatch = 5;
    private final AtomicBoolean scheduled = new AtomicBoolean();

    public ThreadedLevelLightEngine(LightChunkGetter chunkProvider, ChunkMap chunkStorage, boolean hasBlockLight, ProcessorMailbox<Runnable> processor, ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> executor) {
        super(chunkProvider, true, hasBlockLight);
        this.chunkMap = chunkStorage;
        this.sorterMailbox = executor;
        this.taskMailbox = processor;
    }

    @Override
    public void close() {
    }

    @Override
    public int runUpdates(int i, boolean doSkylight, boolean skipEdgeLightPropagation) {
        throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Ran automatically on a different thread!"));
    }

    @Override
    public void onBlockEmissionIncrease(BlockPos pos, int level) {
        throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Ran automatically on a different thread!"));
    }

    @Override
    public void checkBlock(BlockPos pos) {
        BlockPos blockPos = pos.immutable();
        this.addTask(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ThreadedLevelLightEngine.TaskType.POST_UPDATE, Util.name(() -> {
            super.checkBlock(blockPos);
        }, () -> {
            return "checkBlock " + blockPos;
        }));
    }

    protected void updateChunkStatus(ChunkPos pos) {
        this.addTask(pos.x, pos.z, () -> {
            return 0;
        }, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            super.retainData(pos, false);
            super.enableLightSources(pos, false);

            for(int i = this.getMinLightSection(); i < this.getMaxLightSection(); ++i) {
                super.queueSectionData(LightLayer.BLOCK, SectionPos.of(pos, i), (DataLayer)null, true);
                super.queueSectionData(LightLayer.SKY, SectionPos.of(pos, i), (DataLayer)null, true);
            }

            for(int j = this.levelHeightAccessor.getMinSection(); j < this.levelHeightAccessor.getMaxSection(); ++j) {
                super.updateSectionStatus(SectionPos.of(pos, j), true);
            }

        }, () -> {
            return "updateChunkStatus " + pos + " true";
        }));
    }

    @Override
    public void updateSectionStatus(SectionPos pos, boolean notReady) {
        this.addTask(pos.x(), pos.z(), () -> {
            return 0;
        }, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            super.updateSectionStatus(pos, notReady);
        }, () -> {
            return "updateSectionStatus " + pos + " " + notReady;
        }));
    }

    @Override
    public void enableLightSources(ChunkPos pos, boolean retainData) {
        this.addTask(pos.x, pos.z, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            super.enableLightSources(pos, retainData);
        }, () -> {
            return "enableLight " + pos + " " + retainData;
        }));
    }

    @Override
    public void queueSectionData(LightLayer lightType, SectionPos pos, @Nullable DataLayer nibbles, boolean nonEdge) {
        this.addTask(pos.x(), pos.z(), () -> {
            return 0;
        }, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            super.queueSectionData(lightType, pos, nibbles, nonEdge);
        }, () -> {
            return "queueData " + pos;
        }));
    }

    private void addTask(int x, int z, ThreadedLevelLightEngine.TaskType stage, Runnable task) {
        this.addTask(x, z, this.chunkMap.getChunkQueueLevel(ChunkPos.asLong(x, z)), stage, task);
    }

    private void addTask(int x, int z, IntSupplier completedLevelSupplier, ThreadedLevelLightEngine.TaskType stage, Runnable task) {
        this.sorterMailbox.tell(ChunkTaskPriorityQueueSorter.message(() -> {
            this.lightTasks.add(Pair.of(stage, task));
            if (this.lightTasks.size() >= this.taskPerBatch) {
                this.runUpdate();
            }

        }, ChunkPos.asLong(x, z), completedLevelSupplier));
    }

    @Override
    public void retainData(ChunkPos pos, boolean retainData) {
        this.addTask(pos.x, pos.z, () -> {
            return 0;
        }, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            super.retainData(pos, retainData);
        }, () -> {
            return "retainData " + pos;
        }));
    }

    public CompletableFuture<ChunkAccess> lightChunk(ChunkAccess chunk, boolean excludeBlocks) {
        ChunkPos chunkPos = chunk.getPos();
        chunk.setLightCorrect(false);
        this.addTask(chunkPos.x, chunkPos.z, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            LevelChunkSection[] levelChunkSections = chunk.getSections();

            for(int i = 0; i < chunk.getSectionsCount(); ++i) {
                LevelChunkSection levelChunkSection = levelChunkSections[i];
                if (!levelChunkSection.hasOnlyAir()) {
                    int j = this.levelHeightAccessor.getSectionYFromSectionIndex(i);
                    super.updateSectionStatus(SectionPos.of(chunkPos, j), false);
                }
            }

            super.enableLightSources(chunkPos, true);
            if (!excludeBlocks) {
                chunk.getLights().forEach((pos) -> {
                    super.onBlockEmissionIncrease(pos, chunk.getLightEmission(pos));
                });
            }

        }, () -> {
            return "lightChunk " + chunkPos + " " + excludeBlocks;
        }));
        return CompletableFuture.supplyAsync(() -> {
            chunk.setLightCorrect(true);
            super.retainData(chunkPos, false);
            this.chunkMap.releaseLightTicket(chunkPos);
            return chunk;
        }, (runnable) -> {
            this.addTask(chunkPos.x, chunkPos.z, ThreadedLevelLightEngine.TaskType.POST_UPDATE, runnable);
        });
    }

    public void tryScheduleUpdate() {
        if ((!this.lightTasks.isEmpty() || super.hasLightWork()) && this.scheduled.compareAndSet(false, true)) {
            this.taskMailbox.tell(() -> {
                this.runUpdate();
                this.scheduled.set(false);
            });
        }

    }

    private void runUpdate() {
        int i = Math.min(this.lightTasks.size(), this.taskPerBatch);
        ObjectListIterator<Pair<ThreadedLevelLightEngine.TaskType, Runnable>> objectListIterator = this.lightTasks.iterator();

        int j;
        for(j = 0; objectListIterator.hasNext() && j < i; ++j) {
            Pair<ThreadedLevelLightEngine.TaskType, Runnable> pair = objectListIterator.next();
            if (pair.getFirst() == ThreadedLevelLightEngine.TaskType.PRE_UPDATE) {
                pair.getSecond().run();
            }
        }

        objectListIterator.back(j);
        super.runUpdates(Integer.MAX_VALUE, true, true);

        for(int var5 = 0; objectListIterator.hasNext() && var5 < i; ++var5) {
            Pair<ThreadedLevelLightEngine.TaskType, Runnable> pair2 = objectListIterator.next();
            if (pair2.getFirst() == ThreadedLevelLightEngine.TaskType.POST_UPDATE) {
                pair2.getSecond().run();
            }

            objectListIterator.remove();
        }

    }

    public void setTaskPerBatch(int taskBatchSize) {
        this.taskPerBatch = taskBatchSize;
    }

    static enum TaskType {
        PRE_UPDATE,
        POST_UPDATE;
    }
}
