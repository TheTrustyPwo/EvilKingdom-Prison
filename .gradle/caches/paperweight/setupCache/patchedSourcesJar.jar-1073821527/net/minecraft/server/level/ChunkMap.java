package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class ChunkMap extends ChunkStorage implements ChunkHolder.PlayerProvider {
    private static final byte CHUNK_TYPE_REPLACEABLE = -1;
    private static final byte CHUNK_TYPE_UNKNOWN = 0;
    private static final byte CHUNK_TYPE_FULL = 1;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHUNK_SAVED_PER_TICK = 200;
    private static final int CHUNK_SAVED_EAGERLY_PER_TICK = 20;
    private static final int EAGER_CHUNK_SAVE_COOLDOWN_IN_MILLIS = 10000;
    private static final int MIN_VIEW_DISTANCE = 3;
    public static final int MAX_VIEW_DISTANCE = 33;
    public static final int MAX_CHUNK_DISTANCE = 33 + ChunkStatus.maxDistance();
    public static final int FORCED_TICKET_LEVEL = 31;
    public final Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap = new Long2ObjectLinkedOpenHashMap<>();
    public volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap = this.updatingChunkMap.clone();
    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads = new Long2ObjectLinkedOpenHashMap<>();
    public final LongSet entitiesInLevel = new LongOpenHashSet();
    public final ServerLevel level;
    private final ThreadedLevelLightEngine lightEngine;
    private final BlockableEventLoop<Runnable> mainThreadExecutor;
    public ChunkGenerator generator;
    public final Supplier<DimensionDataStorage> overworldDataStorage;
    private final PoiManager poiManager;
    public final LongSet toDrop = new LongOpenHashSet();
    private boolean modified;
    private final ChunkTaskPriorityQueueSorter queueSorter;
    private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> worldgenMailbox;
    public final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> mainThreadMailbox;
    public final ChunkProgressListener progressListener;
    private final ChunkStatusUpdateListener chunkStatusListener;
    public final ChunkMap.DistanceManager distanceManager;
    private final AtomicInteger tickingGenerated = new AtomicInteger();
    public final StructureManager structureManager;
    private final String storageName;
    private final PlayerMap playerMap = new PlayerMap();
    public final Int2ObjectMap<ChunkMap.TrackedEntity> entityMap = new Int2ObjectOpenHashMap<>();
    private final Long2ByteMap chunkTypeCache = new Long2ByteOpenHashMap();
    private final Long2LongMap chunkSaveCooldowns = new Long2LongOpenHashMap();
    private final Queue<Runnable> unloadQueue = Queues.newConcurrentLinkedQueue();
    int viewDistance;

    public ChunkMap(ServerLevel world, LevelStorageSource.LevelStorageAccess session, DataFixer dataFixer, StructureManager structureManager, Executor executor, BlockableEventLoop<Runnable> mainThreadExecutor, LightChunkGetter chunkProvider, ChunkGenerator chunkGenerator, ChunkProgressListener worldGenerationProgressListener, ChunkStatusUpdateListener chunkStatusChangeListener, Supplier<DimensionDataStorage> persistentStateManagerFactory, int viewDistance, boolean dsync) {
        super(session.getDimensionPath(world.dimension()).resolve("region"), dataFixer, dsync);
        this.structureManager = structureManager;
        Path path = session.getDimensionPath(world.dimension());
        this.storageName = path.getFileName().toString();
        this.level = world;
        this.generator = chunkGenerator;
        this.mainThreadExecutor = mainThreadExecutor;
        ProcessorMailbox<Runnable> processorMailbox = ProcessorMailbox.create(executor, "worldgen");
        ProcessorHandle<Runnable> processorHandle = ProcessorHandle.of("main", mainThreadExecutor::tell);
        this.progressListener = worldGenerationProgressListener;
        this.chunkStatusListener = chunkStatusChangeListener;
        ProcessorMailbox<Runnable> processorMailbox2 = ProcessorMailbox.create(executor, "light");
        this.queueSorter = new ChunkTaskPriorityQueueSorter(ImmutableList.of(processorMailbox, processorHandle, processorMailbox2), executor, Integer.MAX_VALUE);
        this.worldgenMailbox = this.queueSorter.getProcessor(processorMailbox, false);
        this.mainThreadMailbox = this.queueSorter.getProcessor(processorHandle, false);
        this.lightEngine = new ThreadedLevelLightEngine(chunkProvider, this, this.level.dimensionType().hasSkyLight(), processorMailbox2, this.queueSorter.getProcessor(processorMailbox2, false));
        this.distanceManager = new ChunkMap.DistanceManager(executor, mainThreadExecutor);
        this.overworldDataStorage = persistentStateManagerFactory;
        this.poiManager = new PoiManager(path.resolve("poi"), dataFixer, dsync, world);
        this.setViewDistance(viewDistance);
    }

    protected ChunkGenerator generator() {
        return this.generator;
    }

    public void debugReloadGenerator() {
        DataResult<JsonElement> dataResult = ChunkGenerator.CODEC.encodeStart(JsonOps.INSTANCE, this.generator);
        DataResult<ChunkGenerator> dataResult2 = dataResult.flatMap((json) -> {
            return ChunkGenerator.CODEC.parse(JsonOps.INSTANCE, json);
        });
        dataResult2.result().ifPresent((chunkGenerator) -> {
            this.generator = chunkGenerator;
        });
    }

    private static double euclideanDistanceSquared(ChunkPos pos, Entity entity) {
        double d = (double)SectionPos.sectionToBlockCoord(pos.x, 8);
        double e = (double)SectionPos.sectionToBlockCoord(pos.z, 8);
        double f = d - entity.getX();
        double g = e - entity.getZ();
        return f * f + g * g;
    }

    public static boolean isChunkInRange(int x1, int z1, int x2, int z2, int distance) {
        int i = Math.max(0, Math.abs(x1 - x2) - 1);
        int j = Math.max(0, Math.abs(z1 - z2) - 1);
        long l = (long)Math.max(0, Math.max(i, j) - 1);
        long m = (long)Math.min(i, j);
        long n = m * m + l * l;
        int k = distance - 1;
        int o = k * k;
        return n <= (long)o;
    }

    private static boolean isChunkOnRangeBorder(int x1, int z1, int x2, int z2, int distance) {
        if (!isChunkInRange(x1, z1, x2, z2, distance)) {
            return false;
        } else if (!isChunkInRange(x1 + 1, z1, x2, z2, distance)) {
            return true;
        } else if (!isChunkInRange(x1, z1 + 1, x2, z2, distance)) {
            return true;
        } else if (!isChunkInRange(x1 - 1, z1, x2, z2, distance)) {
            return true;
        } else {
            return !isChunkInRange(x1, z1 - 1, x2, z2, distance);
        }
    }

    protected ThreadedLevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    @Nullable
    public ChunkHolder getUpdatingChunkIfPresent(long pos) {
        return this.updatingChunkMap.get(pos);
    }

    @Nullable
    public ChunkHolder getVisibleChunkIfPresent(long pos) {
        return this.visibleChunkMap.get(pos);
    }

    protected IntSupplier getChunkQueueLevel(long pos) {
        return () -> {
            ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos);
            return chunkHolder == null ? ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1 : Math.min(chunkHolder.getQueueLevel(), ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
        };
    }

    public String getChunkDebugData(ChunkPos chunkPos) {
        ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPos.toLong());
        if (chunkHolder == null) {
            return "null";
        } else {
            String string = chunkHolder.getTicketLevel() + "\n";
            ChunkStatus chunkStatus = chunkHolder.getLastAvailableStatus();
            ChunkAccess chunkAccess = chunkHolder.getLastAvailable();
            if (chunkStatus != null) {
                string = string + "St: \u00a7" + chunkStatus.getIndex() + chunkStatus + "\u00a7r\n";
            }

            if (chunkAccess != null) {
                string = string + "Ch: \u00a7" + chunkAccess.getStatus().getIndex() + chunkAccess.getStatus() + "\u00a7r\n";
            }

            ChunkHolder.FullChunkStatus fullChunkStatus = chunkHolder.getFullStatus();
            string = string + "\u00a7" + fullChunkStatus.ordinal() + fullChunkStatus;
            return string + "\u00a7r";
        }
    }

    private CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> getChunkRangeFuture(ChunkPos centerChunk, int margin, IntFunction<ChunkStatus> distanceToStatus) {
        List<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> list = new ArrayList<>();
        List<ChunkHolder> list2 = new ArrayList<>();
        int i = centerChunk.x;
        int j = centerChunk.z;

        for(int k = -margin; k <= margin; ++k) {
            for(int l = -margin; l <= margin; ++l) {
                int m = Math.max(Math.abs(l), Math.abs(k));
                final ChunkPos chunkPos = new ChunkPos(i + l, j + k);
                long n = chunkPos.toLong();
                ChunkHolder chunkHolder = this.getUpdatingChunkIfPresent(n);
                if (chunkHolder == null) {
                    return CompletableFuture.completedFuture(Either.right(new ChunkHolder.ChunkLoadingFailure() {
                        @Override
                        public String toString() {
                            return "Unloaded " + chunkPos;
                        }
                    }));
                }

                ChunkStatus chunkStatus = distanceToStatus.apply(m);
                CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = chunkHolder.getOrScheduleFuture(chunkStatus, this);
                list2.add(chunkHolder);
                list.add(completableFuture);
            }
        }

        CompletableFuture<List<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> completableFuture2 = Util.sequence(list);
        CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> completableFuture3 = completableFuture2.thenApply((listx) -> {
            List<ChunkAccess> list2 = Lists.newArrayList();
            int l = 0;

            for(final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either : listx) {
                if (either == null) {
                    throw this.debugFuturesAndCreateReportedException(new IllegalStateException("At least one of the chunk futures were null"), "n/a");
                }

                Optional<ChunkAccess> optional = either.left();
                if (!optional.isPresent()) {
                    final int m = l;
                    return Either.right(new ChunkHolder.ChunkLoadingFailure() {
                        @Override
                        public String toString() {
                            return "Unloaded " + new ChunkPos(i + m % (margin * 2 + 1), j + m / (margin * 2 + 1)) + " " + either.right().get();
                        }
                    });
                }

                list2.add(optional.get());
                ++l;
            }

            return Either.left(list2);
        });

        for(ChunkHolder chunkHolder2 : list2) {
            chunkHolder2.addSaveDependency("getChunkRangeFuture " + centerChunk + " " + margin, completableFuture3);
        }

        return completableFuture3;
    }

    public ReportedException debugFuturesAndCreateReportedException(IllegalStateException exception, String string) {
        StringBuilder stringBuilder = new StringBuilder();
        Consumer<ChunkHolder> consumer = (chunkHolder) -> {
            chunkHolder.getAllFutures().forEach((pair) -> {
                ChunkStatus chunkStatus = pair.getFirst();
                CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = pair.getSecond();
                if (completableFuture != null && completableFuture.isDone() && completableFuture.join() == null) {
                    stringBuilder.append((Object)chunkHolder.getPos()).append(" - status: ").append((Object)chunkStatus).append(" future: ").append((Object)completableFuture).append(System.lineSeparator());
                }

            });
        };
        stringBuilder.append("Updating:").append(System.lineSeparator());
        this.updatingChunkMap.values().forEach(consumer);
        stringBuilder.append("Visible:").append(System.lineSeparator());
        this.visibleChunkMap.values().forEach(consumer);
        CrashReport crashReport = CrashReport.forThrowable(exception, "Chunk loading");
        CrashReportCategory crashReportCategory = crashReport.addCategory("Chunk loading");
        crashReportCategory.setDetail("Details", string);
        crashReportCategory.setDetail("Futures", stringBuilder);
        return new ReportedException(crashReport);
    }

    public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareEntityTickingChunk(ChunkPos pos) {
        return this.getChunkRangeFuture(pos, 2, (distance) -> {
            return ChunkStatus.FULL;
        }).thenApplyAsync((either) -> {
            return either.mapLeft((chunks) -> {
                return (LevelChunk)chunks.get(chunks.size() / 2);
            });
        }, this.mainThreadExecutor);
    }

    @Nullable
    ChunkHolder updateChunkScheduling(long pos, int level, @Nullable ChunkHolder holder, int i) {
        if (i > MAX_CHUNK_DISTANCE && level > MAX_CHUNK_DISTANCE) {
            return holder;
        } else {
            if (holder != null) {
                holder.setTicketLevel(level);
            }

            if (holder != null) {
                if (level > MAX_CHUNK_DISTANCE) {
                    this.toDrop.add(pos);
                } else {
                    this.toDrop.remove(pos);
                }
            }

            if (level <= MAX_CHUNK_DISTANCE && holder == null) {
                holder = this.pendingUnloads.remove(pos);
                if (holder != null) {
                    holder.setTicketLevel(level);
                } else {
                    holder = new ChunkHolder(new ChunkPos(pos), level, this.level, this.lightEngine, this.queueSorter, this);
                }

                this.updatingChunkMap.put(pos, holder);
                this.modified = true;
            }

            return holder;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.queueSorter.close();
            this.poiManager.close();
        } finally {
            super.close();
        }

    }

    protected void saveAllChunks(boolean flush) {
        if (flush) {
            List<ChunkHolder> list = this.visibleChunkMap.values().stream().filter(ChunkHolder::wasAccessibleSinceLastSave).peek(ChunkHolder::refreshAccessibility).collect(Collectors.toList());
            MutableBoolean mutableBoolean = new MutableBoolean();

            do {
                mutableBoolean.setFalse();
                list.stream().map((chunkHolder) -> {
                    CompletableFuture<ChunkAccess> completableFuture;
                    do {
                        completableFuture = chunkHolder.getChunkToSave();
                        this.mainThreadExecutor.managedBlock(completableFuture::isDone);
                    } while(completableFuture != chunkHolder.getChunkToSave());

                    return completableFuture.join();
                }).filter((chunk) -> {
                    return chunk instanceof ImposterProtoChunk || chunk instanceof LevelChunk;
                }).filter(this::save).forEach((chunkAccess) -> {
                    mutableBoolean.setTrue();
                });
            } while(mutableBoolean.isTrue());

            this.processUnloads(() -> {
                return true;
            });
            this.flushWorker();
        } else {
            this.visibleChunkMap.values().forEach(this::saveChunkIfNeeded);
        }

    }

    protected void tick(BooleanSupplier shouldKeepTicking) {
        ProfilerFiller profilerFiller = this.level.getProfiler();
        profilerFiller.push("poi");
        this.poiManager.tick(shouldKeepTicking);
        profilerFiller.popPush("chunk_unload");
        if (!this.level.noSave()) {
            this.processUnloads(shouldKeepTicking);
        }

        profilerFiller.pop();
    }

    public boolean hasWork() {
        return this.lightEngine.hasLightWork() || !this.pendingUnloads.isEmpty() || !this.updatingChunkMap.isEmpty() || this.poiManager.hasWork() || !this.toDrop.isEmpty() || !this.unloadQueue.isEmpty() || this.queueSorter.hasWork() || this.distanceManager.hasTickets();
    }

    private void processUnloads(BooleanSupplier shouldKeepTicking) {
        LongIterator longIterator = this.toDrop.iterator();

        for(int i = 0; longIterator.hasNext() && (shouldKeepTicking.getAsBoolean() || i < 200 || this.toDrop.size() > 2000); longIterator.remove()) {
            long l = longIterator.nextLong();
            ChunkHolder chunkHolder = this.updatingChunkMap.remove(l);
            if (chunkHolder != null) {
                this.pendingUnloads.put(l, chunkHolder);
                this.modified = true;
                ++i;
                this.scheduleUnload(l, chunkHolder);
            }
        }

        int j = Math.max(0, this.unloadQueue.size() - 2000);

        Runnable runnable;
        while((shouldKeepTicking.getAsBoolean() || j > 0) && (runnable = this.unloadQueue.poll()) != null) {
            --j;
            runnable.run();
        }

        int k = 0;
        ObjectIterator<ChunkHolder> objectIterator = this.visibleChunkMap.values().iterator();

        while(k < 20 && shouldKeepTicking.getAsBoolean() && objectIterator.hasNext()) {
            if (this.saveChunkIfNeeded(objectIterator.next())) {
                ++k;
            }
        }

    }

    private void scheduleUnload(long pos, ChunkHolder holder) {
        CompletableFuture<ChunkAccess> completableFuture = holder.getChunkToSave();
        completableFuture.thenAcceptAsync((chunk) -> {
            CompletableFuture<ChunkAccess> completableFuture2 = holder.getChunkToSave();
            if (completableFuture2 != completableFuture) {
                this.scheduleUnload(pos, holder);
            } else {
                if (this.pendingUnloads.remove(pos, holder) && chunk != null) {
                    if (chunk instanceof LevelChunk) {
                        ((LevelChunk)chunk).setLoaded(false);
                    }

                    this.save(chunk);
                    if (this.entitiesInLevel.remove(pos) && chunk instanceof LevelChunk) {
                        LevelChunk levelChunk = (LevelChunk)chunk;
                        this.level.unload(levelChunk);
                    }

                    this.lightEngine.updateChunkStatus(chunk.getPos());
                    this.lightEngine.tryScheduleUpdate();
                    this.progressListener.onStatusChange(chunk.getPos(), (ChunkStatus)null);
                    this.chunkSaveCooldowns.remove(chunk.getPos().toLong());
                }

            }
        }, this.unloadQueue::add).whenComplete((void_, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Failed to save chunk {}", holder.getPos(), throwable);
            }

        });
    }

    protected boolean promoteChunkMap() {
        if (!this.modified) {
            return false;
        } else {
            this.visibleChunkMap = this.updatingChunkMap.clone();
            this.modified = false;
            return true;
        }
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> schedule(ChunkHolder holder, ChunkStatus requiredStatus) {
        ChunkPos chunkPos = holder.getPos();
        if (requiredStatus == ChunkStatus.EMPTY) {
            return this.scheduleChunkLoad(chunkPos);
        } else {
            if (requiredStatus == ChunkStatus.LIGHT) {
                this.distanceManager.addTicket(TicketType.LIGHT, chunkPos, 33 + ChunkStatus.getDistance(ChunkStatus.LIGHT), chunkPos);
            }

            Optional<ChunkAccess> optional = holder.getOrScheduleFuture(requiredStatus.getParent(), this).getNow(ChunkHolder.UNLOADED_CHUNK).left();
            if (optional.isPresent() && optional.get().getStatus().isOrAfter(requiredStatus)) {
                CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = requiredStatus.load(this.level, this.structureManager, this.lightEngine, (chunkAccess) -> {
                    return this.protoChunkToFullChunk(holder);
                }, optional.get());
                this.progressListener.onStatusChange(chunkPos, requiredStatus);
                return completableFuture;
            } else {
                return this.scheduleChunkGeneration(holder, requiredStatus);
            }
        }
    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkLoad(ChunkPos pos) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.level.getProfiler().incrementCounter("chunkLoad");
                CompoundTag compoundTag = this.readChunk(pos);
                if (compoundTag != null) {
                    boolean bl = compoundTag.contains("Status", 8);
                    if (bl) {
                        ChunkAccess chunkAccess = ChunkSerializer.read(this.level, this.poiManager, pos, compoundTag);
                        this.markPosition(pos, chunkAccess.getStatus().getChunkType());
                        return Either.left(chunkAccess);
                    }

                    LOGGER.error("Chunk file at {} is missing level data, skipping", (Object)pos);
                }
            } catch (ReportedException var5) {
                Throwable throwable = var5.getCause();
                if (!(throwable instanceof IOException)) {
                    this.markPositionReplaceable(pos);
                    throw var5;
                }

                LOGGER.error("Couldn't load chunk {}", pos, throwable);
            } catch (Exception var6) {
                LOGGER.error("Couldn't load chunk {}", pos, var6);
            }

            this.markPositionReplaceable(pos);
            return Either.left(new ProtoChunk(pos, UpgradeData.EMPTY, this.level, this.level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), (BlendingData)null));
        }, this.mainThreadExecutor);
    }

    private void markPositionReplaceable(ChunkPos pos) {
        this.chunkTypeCache.put(pos.toLong(), (byte)-1);
    }

    private byte markPosition(ChunkPos pos, ChunkStatus.ChunkType type) {
        return this.chunkTypeCache.put(pos.toLong(), (byte)(type == ChunkStatus.ChunkType.PROTOCHUNK ? -1 : 1));
    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkGeneration(ChunkHolder holder, ChunkStatus requiredStatus) {
        ChunkPos chunkPos = holder.getPos();
        CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.getChunkRangeFuture(chunkPos, requiredStatus.getRange(), (i) -> {
            return this.getDependencyStatus(requiredStatus, i);
        });
        this.level.getProfiler().incrementCounter(() -> {
            return "chunkGenerate " + requiredStatus.getName();
        });
        Executor executor = (task) -> {
            this.worldgenMailbox.tell(ChunkTaskPriorityQueueSorter.message(holder, task));
        };
        return completableFuture.thenComposeAsync((either) -> {
            return either.map((chunks) -> {
                try {
                    CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = requiredStatus.generate(executor, this.level, this.generator, this.structureManager, this.lightEngine, (chunkAccess) -> {
                        return this.protoChunkToFullChunk(holder);
                    }, chunks, false);
                    this.progressListener.onStatusChange(chunkPos, requiredStatus);
                    return completableFuture;
                } catch (Exception var9) {
                    var9.getStackTrace();
                    CrashReport crashReport = CrashReport.forThrowable(var9, "Exception generating new chunk");
                    CrashReportCategory crashReportCategory = crashReport.addCategory("Chunk to be generated");
                    crashReportCategory.setDetail("Location", String.format("%d,%d", chunkPos.x, chunkPos.z));
                    crashReportCategory.setDetail("Position hash", ChunkPos.asLong(chunkPos.x, chunkPos.z));
                    crashReportCategory.setDetail("Generator", this.generator);
                    this.mainThreadExecutor.execute(() -> {
                        throw new ReportedException(crashReport);
                    });
                    throw new ReportedException(crashReport);
                }
            }, (unloaded) -> {
                this.releaseLightTicket(chunkPos);
                return CompletableFuture.completedFuture(Either.right(unloaded));
            });
        }, executor);
    }

    protected void releaseLightTicket(ChunkPos pos) {
        this.mainThreadExecutor.tell(Util.name(() -> {
            this.distanceManager.removeTicket(TicketType.LIGHT, pos, 33 + ChunkStatus.getDistance(ChunkStatus.LIGHT), pos);
        }, () -> {
            return "release light ticket " + pos;
        }));
    }

    private ChunkStatus getDependencyStatus(ChunkStatus centerChunkTargetStatus, int distance) {
        ChunkStatus chunkStatus;
        if (distance == 0) {
            chunkStatus = centerChunkTargetStatus.getParent();
        } else {
            chunkStatus = ChunkStatus.getStatusAroundFullChunk(ChunkStatus.getDistance(centerChunkTargetStatus) + distance);
        }

        return chunkStatus;
    }

    private static void postLoadProtoChunk(ServerLevel world, List<CompoundTag> nbt) {
        if (!nbt.isEmpty()) {
            world.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(nbt, world));
        }

    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> protoChunkToFullChunk(ChunkHolder chunkHolder) {
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = chunkHolder.getFutureIfPresentUnchecked(ChunkStatus.FULL.getParent());
        return completableFuture.thenApplyAsync((either) -> {
            ChunkStatus chunkStatus = ChunkHolder.getStatus(chunkHolder.getTicketLevel());
            return !chunkStatus.isOrAfter(ChunkStatus.FULL) ? ChunkHolder.UNLOADED_CHUNK : either.mapLeft((protoChunk) -> {
                ChunkPos chunkPos = chunkHolder.getPos();
                ProtoChunk protoChunk2 = (ProtoChunk)protoChunk;
                LevelChunk levelChunk;
                if (protoChunk2 instanceof ImposterProtoChunk) {
                    levelChunk = ((ImposterProtoChunk)protoChunk2).getWrapped();
                } else {
                    levelChunk = new LevelChunk(this.level, protoChunk2, (chunk) -> {
                        postLoadProtoChunk(this.level, protoChunk2.getEntities());
                    });
                    chunkHolder.replaceProtoChunk(new ImposterProtoChunk(levelChunk, false));
                }

                levelChunk.setFullStatus(() -> {
                    return ChunkHolder.getFullChunkStatus(chunkHolder.getTicketLevel());
                });
                levelChunk.runPostLoad();
                if (this.entitiesInLevel.add(chunkPos.toLong())) {
                    levelChunk.setLoaded(true);
                    levelChunk.registerAllBlockEntitiesAfterLevelLoad();
                    levelChunk.registerTickContainerInLevel(this.level);
                }

                return levelChunk;
            });
        }, (task) -> {
            this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(task, chunkHolder.getPos().toLong(), chunkHolder::getTicketLevel));
        });
    }

    public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareTickingChunk(ChunkHolder holder) {
        ChunkPos chunkPos = holder.getPos();
        CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.getChunkRangeFuture(chunkPos, 1, (i) -> {
            return ChunkStatus.FULL;
        });
        CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> completableFuture2 = completableFuture.thenApplyAsync((either) -> {
            return either.mapLeft((list) -> {
                return (LevelChunk)list.get(list.size() / 2);
            });
        }, (task) -> {
            this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(holder, task));
        }).thenApplyAsync((either) -> {
            return either.ifLeft((chunk) -> {
                chunk.postProcessGeneration();
                this.level.startTickingChunk(chunk);
            });
        }, this.mainThreadExecutor);
        completableFuture2.thenAcceptAsync((either) -> {
            either.ifLeft((chunk) -> {
                this.tickingGenerated.getAndIncrement();
                MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject = new MutableObject<>();
                this.getPlayers(chunkPos, false).forEach((player) -> {
                    this.playerLoadedChunk(player, mutableObject, chunk);
                });
            });
        }, (task) -> {
            this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(holder, task));
        });
        return completableFuture2;
    }

    public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareAccessibleChunk(ChunkHolder holder) {
        return this.getChunkRangeFuture(holder.getPos(), 1, ChunkStatus::getStatusAroundFullChunk).thenApplyAsync((either) -> {
            return either.mapLeft((chunks) -> {
                return (LevelChunk)chunks.get(chunks.size() / 2);
            });
        }, (task) -> {
            this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(holder, task));
        });
    }

    public int getTickingGenerated() {
        return this.tickingGenerated.get();
    }

    private boolean saveChunkIfNeeded(ChunkHolder chunkHolder) {
        if (!chunkHolder.wasAccessibleSinceLastSave()) {
            return false;
        } else {
            ChunkAccess chunkAccess = chunkHolder.getChunkToSave().getNow((ChunkAccess)null);
            if (!(chunkAccess instanceof ImposterProtoChunk) && !(chunkAccess instanceof LevelChunk)) {
                return false;
            } else {
                long l = chunkAccess.getPos().toLong();
                long m = this.chunkSaveCooldowns.getOrDefault(l, -1L);
                long n = System.currentTimeMillis();
                if (n < m) {
                    return false;
                } else {
                    boolean bl = this.save(chunkAccess);
                    chunkHolder.refreshAccessibility();
                    if (bl) {
                        this.chunkSaveCooldowns.put(l, n + 10000L);
                    }

                    return bl;
                }
            }
        }
    }

    public boolean save(ChunkAccess chunk) {
        this.poiManager.flush(chunk.getPos());
        if (!chunk.isUnsaved()) {
            return false;
        } else {
            chunk.setUnsaved(false);
            ChunkPos chunkPos = chunk.getPos();

            try {
                ChunkStatus chunkStatus = chunk.getStatus();
                if (chunkStatus.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
                    if (this.isExistingChunkFull(chunkPos)) {
                        return false;
                    }

                    if (chunkStatus == ChunkStatus.EMPTY && chunk.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                        return false;
                    }
                }

                this.level.getProfiler().incrementCounter("chunkSave");
                CompoundTag compoundTag = ChunkSerializer.write(this.level, chunk);
                this.write(chunkPos, compoundTag);
                this.markPosition(chunkPos, chunkStatus.getChunkType());
                return true;
            } catch (Exception var5) {
                LOGGER.error("Failed to save chunk {},{}", chunkPos.x, chunkPos.z, var5);
                return false;
            }
        }
    }

    private boolean isExistingChunkFull(ChunkPos pos) {
        byte b = this.chunkTypeCache.get(pos.toLong());
        if (b != 0) {
            return b == 1;
        } else {
            CompoundTag compoundTag;
            try {
                compoundTag = this.readChunk(pos);
                if (compoundTag == null) {
                    this.markPositionReplaceable(pos);
                    return false;
                }
            } catch (Exception var5) {
                LOGGER.error("Failed to read chunk {}", pos, var5);
                this.markPositionReplaceable(pos);
                return false;
            }

            ChunkStatus.ChunkType chunkType = ChunkSerializer.getChunkTypeFromTag(compoundTag);
            return this.markPosition(pos, chunkType) == 1;
        }
    }

    public void setViewDistance(int watchDistance) {
        int i = Mth.clamp(watchDistance + 1, 3, 33);
        if (i != this.viewDistance) {
            int j = this.viewDistance;
            this.viewDistance = i;
            this.distanceManager.updatePlayerTickets(this.viewDistance + 1);

            for(ChunkHolder chunkHolder : this.updatingChunkMap.values()) {
                ChunkPos chunkPos = chunkHolder.getPos();
                MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject = new MutableObject<>();
                this.getPlayers(chunkPos, false).forEach((player) -> {
                    SectionPos sectionPos = player.getLastSectionPos();
                    boolean bl = isChunkInRange(chunkPos.x, chunkPos.z, sectionPos.x(), sectionPos.z(), j);
                    boolean bl2 = isChunkInRange(chunkPos.x, chunkPos.z, sectionPos.x(), sectionPos.z(), this.viewDistance);
                    this.updateChunkTracking(player, chunkPos, mutableObject, bl, bl2);
                });
            }
        }

    }

    protected void updateChunkTracking(ServerPlayer player, ChunkPos pos, MutableObject<ClientboundLevelChunkWithLightPacket> packet, boolean oldWithinViewDistance, boolean newWithinViewDistance) {
        if (player.level == this.level) {
            if (newWithinViewDistance && !oldWithinViewDistance) {
                ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos.toLong());
                if (chunkHolder != null) {
                    LevelChunk levelChunk = chunkHolder.getTickingChunk();
                    if (levelChunk != null) {
                        this.playerLoadedChunk(player, packet, levelChunk);
                    }

                    DebugPackets.sendPoiPacketsForChunk(this.level, pos);
                }
            }

            if (!newWithinViewDistance && oldWithinViewDistance) {
                player.untrackChunk(pos);
            }

        }
    }

    public int size() {
        return this.visibleChunkMap.size();
    }

    public net.minecraft.server.level.DistanceManager getDistanceManager() {
        return this.distanceManager;
    }

    protected Iterable<ChunkHolder> getChunks() {
        return Iterables.unmodifiableIterable(this.visibleChunkMap.values());
    }

    void dumpChunks(Writer writer) throws IOException {
        CsvOutput csvOutput = CsvOutput.builder().addColumn("x").addColumn("z").addColumn("level").addColumn("in_memory").addColumn("status").addColumn("full_status").addColumn("accessible_ready").addColumn("ticking_ready").addColumn("entity_ticking_ready").addColumn("ticket").addColumn("spawning").addColumn("block_entity_count").addColumn("ticking_ticket").addColumn("ticking_level").addColumn("block_ticks").addColumn("fluid_ticks").build(writer);
        TickingTracker tickingTracker = this.distanceManager.tickingTracker();

        for(Entry<ChunkHolder> entry : this.visibleChunkMap.long2ObjectEntrySet()) {
            long l = entry.getLongKey();
            ChunkPos chunkPos = new ChunkPos(l);
            ChunkHolder chunkHolder = entry.getValue();
            Optional<ChunkAccess> optional = Optional.ofNullable(chunkHolder.getLastAvailable());
            Optional<LevelChunk> optional2 = optional.flatMap((chunk) -> {
                return chunk instanceof LevelChunk ? Optional.of((LevelChunk)chunk) : Optional.empty();
            });
            csvOutput.writeRow(chunkPos.x, chunkPos.z, chunkHolder.getTicketLevel(), optional.isPresent(), optional.map(ChunkAccess::getStatus).orElse((ChunkStatus)null), optional2.map(LevelChunk::getFullStatus).orElse((ChunkHolder.FullChunkStatus)null), printFuture(chunkHolder.getFullChunkFuture()), printFuture(chunkHolder.getTickingChunkFuture()), printFuture(chunkHolder.getEntityTickingChunkFuture()), this.distanceManager.getTicketDebugString(l), this.anyPlayerCloseEnoughForSpawning(chunkPos), optional2.map((levelChunk) -> {
                return levelChunk.getBlockEntities().size();
            }).orElse(0), tickingTracker.getTicketDebugString(l), tickingTracker.getLevel(l), optional2.map((levelChunk) -> {
                return levelChunk.getBlockTicks().count();
            }).orElse(0), optional2.map((levelChunk) -> {
                return levelChunk.getFluidTicks().count();
            }).orElse(0));
        }

    }

    private static String printFuture(CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> future) {
        try {
            Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> either = future.getNow((Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>)null);
            return either != null ? either.map((chunk) -> {
                return "done";
            }, (unloaded) -> {
                return "unloaded";
            }) : "not completed";
        } catch (CompletionException var2) {
            return "failed " + var2.getCause().getMessage();
        } catch (CancellationException var3) {
            return "cancelled";
        }
    }

    @Nullable
    public CompoundTag readChunk(ChunkPos pos) throws IOException {
        CompoundTag compoundTag = this.read(pos);
        return compoundTag == null ? null : this.upgradeChunkTag(this.level.dimension(), this.overworldDataStorage, compoundTag, this.generator.getTypeNameForDataFixer());
    }

    boolean anyPlayerCloseEnoughForSpawning(ChunkPos pos) {
        long l = pos.toLong();
        if (!this.distanceManager.hasPlayersNearby(l)) {
            return false;
        } else {
            for(ServerPlayer serverPlayer : this.playerMap.getPlayers(l)) {
                if (this.playerIsCloseEnoughForSpawning(serverPlayer, pos)) {
                    return true;
                }
            }

            return false;
        }
    }

    public List<ServerPlayer> getPlayersCloseForSpawning(ChunkPos pos) {
        long l = pos.toLong();
        if (!this.distanceManager.hasPlayersNearby(l)) {
            return List.of();
        } else {
            Builder<ServerPlayer> builder = ImmutableList.builder();

            for(ServerPlayer serverPlayer : this.playerMap.getPlayers(l)) {
                if (this.playerIsCloseEnoughForSpawning(serverPlayer, pos)) {
                    builder.add(serverPlayer);
                }
            }

            return builder.build();
        }
    }

    private boolean playerIsCloseEnoughForSpawning(ServerPlayer player, ChunkPos pos) {
        if (player.isSpectator()) {
            return false;
        } else {
            double d = euclideanDistanceSquared(pos, player);
            return d < 16384.0D;
        }
    }

    private boolean skipPlayer(ServerPlayer player) {
        return player.isSpectator() && !this.level.getGameRules().getBoolean(GameRules.RULE_SPECTATORSGENERATECHUNKS);
    }

    void updatePlayerStatus(ServerPlayer player, boolean added) {
        boolean bl = this.skipPlayer(player);
        boolean bl2 = this.playerMap.ignoredOrUnknown(player);
        int i = SectionPos.blockToSectionCoord(player.getBlockX());
        int j = SectionPos.blockToSectionCoord(player.getBlockZ());
        if (added) {
            this.playerMap.addPlayer(ChunkPos.asLong(i, j), player, bl);
            this.updatePlayerPos(player);
            if (!bl) {
                this.distanceManager.addPlayer(SectionPos.of(player), player);
            }
        } else {
            SectionPos sectionPos = player.getLastSectionPos();
            this.playerMap.removePlayer(sectionPos.chunk().toLong(), player);
            if (!bl2) {
                this.distanceManager.removePlayer(sectionPos, player);
            }
        }

        for(int k = i - this.viewDistance - 1; k <= i + this.viewDistance + 1; ++k) {
            for(int l = j - this.viewDistance - 1; l <= j + this.viewDistance + 1; ++l) {
                if (isChunkInRange(k, l, i, j, this.viewDistance)) {
                    ChunkPos chunkPos = new ChunkPos(k, l);
                    this.updateChunkTracking(player, chunkPos, new MutableObject<>(), !added, added);
                }
            }
        }

    }

    private SectionPos updatePlayerPos(ServerPlayer player) {
        SectionPos sectionPos = SectionPos.of(player);
        player.setLastSectionPos(sectionPos);
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(sectionPos.x(), sectionPos.z()));
        return sectionPos;
    }

    public void move(ServerPlayer player) {
        for(ChunkMap.TrackedEntity trackedEntity : this.entityMap.values()) {
            if (trackedEntity.entity == player) {
                trackedEntity.updatePlayers(this.level.players());
            } else {
                trackedEntity.updatePlayer(player);
            }
        }

        int i = SectionPos.blockToSectionCoord(player.getBlockX());
        int j = SectionPos.blockToSectionCoord(player.getBlockZ());
        SectionPos sectionPos = player.getLastSectionPos();
        SectionPos sectionPos2 = SectionPos.of(player);
        long l = sectionPos.chunk().toLong();
        long m = sectionPos2.chunk().toLong();
        boolean bl = this.playerMap.ignored(player);
        boolean bl2 = this.skipPlayer(player);
        boolean bl3 = sectionPos.asLong() != sectionPos2.asLong();
        if (bl3 || bl != bl2) {
            this.updatePlayerPos(player);
            if (!bl) {
                this.distanceManager.removePlayer(sectionPos, player);
            }

            if (!bl2) {
                this.distanceManager.addPlayer(sectionPos2, player);
            }

            if (!bl && bl2) {
                this.playerMap.ignorePlayer(player);
            }

            if (bl && !bl2) {
                this.playerMap.unIgnorePlayer(player);
            }

            if (l != m) {
                this.playerMap.updatePlayer(l, m, player);
            }
        }

        int k = sectionPos.x();
        int n = sectionPos.z();
        if (Math.abs(k - i) <= this.viewDistance * 2 && Math.abs(n - j) <= this.viewDistance * 2) {
            int o = Math.min(i, k) - this.viewDistance - 1;
            int p = Math.min(j, n) - this.viewDistance - 1;
            int q = Math.max(i, k) + this.viewDistance + 1;
            int r = Math.max(j, n) + this.viewDistance + 1;

            for(int s = o; s <= q; ++s) {
                for(int t = p; t <= r; ++t) {
                    boolean bl4 = isChunkInRange(s, t, k, n, this.viewDistance);
                    boolean bl5 = isChunkInRange(s, t, i, j, this.viewDistance);
                    this.updateChunkTracking(player, new ChunkPos(s, t), new MutableObject<>(), bl4, bl5);
                }
            }
        } else {
            for(int u = k - this.viewDistance - 1; u <= k + this.viewDistance + 1; ++u) {
                for(int v = n - this.viewDistance - 1; v <= n + this.viewDistance + 1; ++v) {
                    if (isChunkInRange(u, v, k, n, this.viewDistance)) {
                        boolean bl6 = true;
                        boolean bl7 = false;
                        this.updateChunkTracking(player, new ChunkPos(u, v), new MutableObject<>(), true, false);
                    }
                }
            }

            for(int w = i - this.viewDistance - 1; w <= i + this.viewDistance + 1; ++w) {
                for(int x = j - this.viewDistance - 1; x <= j + this.viewDistance + 1; ++x) {
                    if (isChunkInRange(w, x, i, j, this.viewDistance)) {
                        boolean bl8 = false;
                        boolean bl9 = true;
                        this.updateChunkTracking(player, new ChunkPos(w, x), new MutableObject<>(), false, true);
                    }
                }
            }
        }

    }

    @Override
    public List<ServerPlayer> getPlayers(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
        Set<ServerPlayer> set = this.playerMap.getPlayers(chunkPos.toLong());
        Builder<ServerPlayer> builder = ImmutableList.builder();

        for(ServerPlayer serverPlayer : set) {
            SectionPos sectionPos = serverPlayer.getLastSectionPos();
            if (onlyOnWatchDistanceEdge && isChunkOnRangeBorder(chunkPos.x, chunkPos.z, sectionPos.x(), sectionPos.z(), this.viewDistance) || !onlyOnWatchDistanceEdge && isChunkInRange(chunkPos.x, chunkPos.z, sectionPos.x(), sectionPos.z(), this.viewDistance)) {
                builder.add(serverPlayer);
            }
        }

        return builder.build();
    }

    public void addEntity(Entity entity) {
        if (!(entity instanceof EnderDragonPart)) {
            EntityType<?> entityType = entity.getType();
            int i = entityType.clientTrackingRange() * 16;
            if (i != 0) {
                int j = entityType.updateInterval();
                if (this.entityMap.containsKey(entity.getId())) {
                    throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Entity is already tracked!"));
                } else {
                    ChunkMap.TrackedEntity trackedEntity = new ChunkMap.TrackedEntity(entity, i, j, entityType.trackDeltas());
                    this.entityMap.put(entity.getId(), trackedEntity);
                    trackedEntity.updatePlayers(this.level.players());
                    if (entity instanceof ServerPlayer) {
                        ServerPlayer serverPlayer = (ServerPlayer)entity;
                        this.updatePlayerStatus(serverPlayer, true);

                        for(ChunkMap.TrackedEntity trackedEntity2 : this.entityMap.values()) {
                            if (trackedEntity2.entity != serverPlayer) {
                                trackedEntity2.updatePlayer(serverPlayer);
                            }
                        }
                    }

                }
            }
        }
    }

    protected void removeEntity(Entity entity) {
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            this.updatePlayerStatus(serverPlayer, false);

            for(ChunkMap.TrackedEntity trackedEntity : this.entityMap.values()) {
                trackedEntity.removePlayer(serverPlayer);
            }
        }

        ChunkMap.TrackedEntity trackedEntity2 = this.entityMap.remove(entity.getId());
        if (trackedEntity2 != null) {
            trackedEntity2.broadcastRemoved();
        }

    }

    protected void tick() {
        List<ServerPlayer> list = Lists.newArrayList();
        List<ServerPlayer> list2 = this.level.players();

        for(ChunkMap.TrackedEntity trackedEntity : this.entityMap.values()) {
            SectionPos sectionPos = trackedEntity.lastSectionPos;
            SectionPos sectionPos2 = SectionPos.of(trackedEntity.entity);
            boolean bl = !Objects.equals(sectionPos, sectionPos2);
            if (bl) {
                trackedEntity.updatePlayers(list2);
                Entity entity = trackedEntity.entity;
                if (entity instanceof ServerPlayer) {
                    list.add((ServerPlayer)entity);
                }

                trackedEntity.lastSectionPos = sectionPos2;
            }

            if (bl || this.distanceManager.inEntityTickingRange(sectionPos2.chunk().toLong())) {
                trackedEntity.serverEntity.sendChanges();
            }
        }

        if (!list.isEmpty()) {
            for(ChunkMap.TrackedEntity trackedEntity2 : this.entityMap.values()) {
                trackedEntity2.updatePlayers(list);
            }
        }

    }

    public void broadcast(Entity entity, Packet<?> packet) {
        ChunkMap.TrackedEntity trackedEntity = this.entityMap.get(entity.getId());
        if (trackedEntity != null) {
            trackedEntity.broadcast(packet);
        }

    }

    protected void broadcastAndSend(Entity entity, Packet<?> packet) {
        ChunkMap.TrackedEntity trackedEntity = this.entityMap.get(entity.getId());
        if (trackedEntity != null) {
            trackedEntity.broadcastAndSend(packet);
        }

    }

    private void playerLoadedChunk(ServerPlayer player, MutableObject<ClientboundLevelChunkWithLightPacket> cachedDataPacket, LevelChunk chunk) {
        if (cachedDataPacket.getValue() == null) {
            cachedDataPacket.setValue(new ClientboundLevelChunkWithLightPacket(chunk, this.lightEngine, (BitSet)null, (BitSet)null, true));
        }

        player.trackChunk(chunk.getPos(), cachedDataPacket.getValue());
        DebugPackets.sendPoiPacketsForChunk(this.level, chunk.getPos());
        List<Entity> list = Lists.newArrayList();
        List<Entity> list2 = Lists.newArrayList();

        for(ChunkMap.TrackedEntity trackedEntity : this.entityMap.values()) {
            Entity entity = trackedEntity.entity;
            if (entity != player && entity.chunkPosition().equals(chunk.getPos())) {
                trackedEntity.updatePlayer(player);
                if (entity instanceof Mob && ((Mob)entity).getLeashHolder() != null) {
                    list.add(entity);
                }

                if (!entity.getPassengers().isEmpty()) {
                    list2.add(entity);
                }
            }
        }

        if (!list.isEmpty()) {
            for(Entity entity2 : list) {
                player.connection.send(new ClientboundSetEntityLinkPacket(entity2, ((Mob)entity2).getLeashHolder()));
            }
        }

        if (!list2.isEmpty()) {
            for(Entity entity3 : list2) {
                player.connection.send(new ClientboundSetPassengersPacket(entity3));
            }
        }

    }

    public PoiManager getPoiManager() {
        return this.poiManager;
    }

    public String getStorageName() {
        return this.storageName;
    }

    void onFullChunkStatusChange(ChunkPos chunkPos, ChunkHolder.FullChunkStatus levelType) {
        this.chunkStatusListener.onChunkStatusChange(chunkPos, levelType);
    }

    class DistanceManager extends net.minecraft.server.level.DistanceManager {
        protected DistanceManager(Executor workerExecutor, Executor mainThreadExecutor) {
            super(workerExecutor, mainThreadExecutor);
        }

        @Override
        protected boolean isChunkToRemove(long pos) {
            return ChunkMap.this.toDrop.contains(pos);
        }

        @Nullable
        @Override
        protected ChunkHolder getChunk(long pos) {
            return ChunkMap.this.getUpdatingChunkIfPresent(pos);
        }

        @Nullable
        @Override
        protected ChunkHolder updateChunkScheduling(long pos, int level, @Nullable ChunkHolder holder, int i) {
            return ChunkMap.this.updateChunkScheduling(pos, level, holder, i);
        }
    }

    public class TrackedEntity {
        final ServerEntity serverEntity;
        final Entity entity;
        private final int range;
        SectionPos lastSectionPos;
        public final Set<ServerPlayerConnection> seenBy = Sets.newIdentityHashSet();

        public TrackedEntity(Entity entity, int maxDistance, int tickInterval, boolean alwaysUpdateVelocity) {
            this.serverEntity = new ServerEntity(ChunkMap.this.level, entity, tickInterval, alwaysUpdateVelocity, this::broadcast);
            this.entity = entity;
            this.range = maxDistance;
            this.lastSectionPos = SectionPos.of(entity);
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof ChunkMap.TrackedEntity) {
                return ((ChunkMap.TrackedEntity)object).entity.getId() == this.entity.getId();
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.entity.getId();
        }

        public void broadcast(Packet<?> packet) {
            for(ServerPlayerConnection serverPlayerConnection : this.seenBy) {
                serverPlayerConnection.send(packet);
            }

        }

        public void broadcastAndSend(Packet<?> packet) {
            this.broadcast(packet);
            if (this.entity instanceof ServerPlayer) {
                ((ServerPlayer)this.entity).connection.send(packet);
            }

        }

        public void broadcastRemoved() {
            for(ServerPlayerConnection serverPlayerConnection : this.seenBy) {
                this.serverEntity.removePairing(serverPlayerConnection.getPlayer());
            }

        }

        public void removePlayer(ServerPlayer player) {
            if (this.seenBy.remove(player.connection)) {
                this.serverEntity.removePairing(player);
            }

        }

        public void updatePlayer(ServerPlayer player) {
            if (player != this.entity) {
                Vec3 vec3 = player.position().subtract(this.serverEntity.sentPos());
                double d = (double)Math.min(this.getEffectiveRange(), (ChunkMap.this.viewDistance - 1) * 16);
                double e = vec3.x * vec3.x + vec3.z * vec3.z;
                double f = d * d;
                boolean bl = e <= f && this.entity.broadcastToPlayer(player);
                if (bl) {
                    if (this.seenBy.add(player.connection)) {
                        this.serverEntity.addPairing(player);
                    }
                } else if (this.seenBy.remove(player.connection)) {
                    this.serverEntity.removePairing(player);
                }

            }
        }

        private int scaledRange(int initialDistance) {
            return ChunkMap.this.level.getServer().getScaledTrackingDistance(initialDistance);
        }

        private int getEffectiveRange() {
            int i = this.range;

            for(Entity entity : this.entity.getIndirectPassengers()) {
                int j = entity.getType().clientTrackingRange() * 16;
                if (j > i) {
                    i = j;
                }
            }

            return this.scaledRange(i);
        }

        public void updatePlayers(List<ServerPlayer> players) {
            for(ServerPlayer serverPlayer : players) {
                this.updatePlayer(serverPlayer);
            }

        }
    }
}
