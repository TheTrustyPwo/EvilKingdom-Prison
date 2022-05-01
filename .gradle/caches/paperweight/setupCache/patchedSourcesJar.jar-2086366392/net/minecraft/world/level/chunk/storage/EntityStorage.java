package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import org.slf4j.Logger;

public class EntityStorage implements EntityPersistentStorage<Entity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ENTITIES_TAG = "Entities";
    private static final String POSITION_TAG = "Position";
    public final ServerLevel level;
    private final IOWorker worker;
    private final LongSet emptyChunks = new LongOpenHashSet();
    public final ProcessorMailbox<Runnable> entityDeserializerQueue;
    protected final DataFixer fixerUpper;

    public EntityStorage(ServerLevel world, Path path, DataFixer dataFixer, boolean dsync, Executor executor) {
        this.level = world;
        this.fixerUpper = dataFixer;
        this.entityDeserializerQueue = ProcessorMailbox.create(executor, "entity-deserializer");
        this.worker = new IOWorker(path, dsync, "entities");
    }

    @Override
    public CompletableFuture<ChunkEntities<Entity>> loadEntities(ChunkPos pos) {
        return this.emptyChunks.contains(pos.toLong()) ? CompletableFuture.completedFuture(emptyChunk(pos)) : this.worker.loadAsync(pos).thenApplyAsync((compound) -> {
            if (compound == null) {
                this.emptyChunks.add(pos.toLong());
                return emptyChunk(pos);
            } else {
                try {
                    ChunkPos chunkPos2 = readChunkPos(compound);
                    if (!Objects.equals(pos, chunkPos2)) {
                        LOGGER.error("Chunk file at {} is in the wrong location. (Expected {}, got {})", pos, pos, chunkPos2);
                    }
                } catch (Exception var6) {
                    LOGGER.warn("Failed to parse chunk {} position info", pos, var6);
                }

                CompoundTag compoundTag = this.upgradeChunkTag(compound);
                ListTag listTag = compoundTag.getList("Entities", 10);
                List<Entity> list = EntityType.loadEntitiesRecursive(listTag, this.level).collect(ImmutableList.toImmutableList());
                return new ChunkEntities<>(pos, list);
            }
        }, this.entityDeserializerQueue::tell);
    }

    private static ChunkPos readChunkPos(CompoundTag chunkNbt) {
        int[] is = chunkNbt.getIntArray("Position");
        return new ChunkPos(is[0], is[1]);
    }

    private static void writeChunkPos(CompoundTag chunkNbt, ChunkPos pos) {
        chunkNbt.put("Position", new IntArrayTag(new int[]{pos.x, pos.z}));
    }

    private static ChunkEntities<Entity> emptyChunk(ChunkPos pos) {
        return new ChunkEntities<>(pos, ImmutableList.of());
    }

    @Override
    public void storeEntities(ChunkEntities<Entity> dataList) {
        ChunkPos chunkPos = dataList.getPos();
        if (dataList.isEmpty()) {
            if (this.emptyChunks.add(chunkPos.toLong())) {
                this.worker.store(chunkPos, (CompoundTag)null);
            }

        } else {
            ListTag listTag = new ListTag();
            final java.util.Map<net.minecraft.world.entity.EntityType<?>, Integer> savedEntityCounts = new java.util.HashMap<>(); // Paper
            dataList.getEntities().forEach((entity) -> {
                // Paper start
                final EntityType<?> entityType = entity.getType();
                final int saveLimit = this.level.paperConfig.entityPerChunkSaveLimits.getOrDefault(entityType, -1);
                if (saveLimit > -1) {
                    if (savedEntityCounts.getOrDefault(entityType, 0) >= saveLimit) {
                        return;
                    }
                    savedEntityCounts.merge(entityType, 1, Integer::sum);
                }
                // Paper end
                CompoundTag compoundTag = new CompoundTag();
                if (entity.save(compoundTag)) {
                    listTag.add(compoundTag);
                }

            });
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
            compoundTag.put("Entities", listTag);
            writeChunkPos(compoundTag, chunkPos);
            this.worker.store(chunkPos, compoundTag).exceptionally((ex) -> {
                LOGGER.error("Failed to store chunk {}", chunkPos, ex);
                return null;
            });
            this.emptyChunks.remove(chunkPos.toLong());
        }
    }

    @Override
    public void flush(boolean sync) {
        this.worker.synchronize(sync).join();
        this.entityDeserializerQueue.runAll();
    }

    private CompoundTag upgradeChunkTag(CompoundTag chunkNbt) {
        int i = getVersion(chunkNbt);
        return ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.ENTITY_CHUNK, chunkNbt, i, SharedConstants.getCurrentVersion().getWorldVersion()); // Paper - route to new converter system
    }

    public static int getVersion(CompoundTag chunkNbt) {
        return chunkNbt.contains("DataVersion", 99) ? chunkNbt.getInt("DataVersion") : -1;
    }

    @Override
    public void close() throws IOException {
        this.worker.close();
    }
}
