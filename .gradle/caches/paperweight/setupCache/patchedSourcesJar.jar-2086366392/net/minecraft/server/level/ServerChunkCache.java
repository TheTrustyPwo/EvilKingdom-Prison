package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet; // Paper

public class ServerChunkCache extends ChunkSource {

    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger(); // Paper
    public static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.getStatusList();
    private final DistanceManager distanceManager;
    final ServerLevel level;
    public final Thread mainThread;
    final ThreadedLevelLightEngine lightEngine;
    public final ServerChunkCache.MainThreadExecutor mainThreadProcessor;
    public final ChunkMap chunkMap;
    private final DimensionDataStorage dataStorage;
    private long lastInhabitedUpdate;
    public boolean spawnEnemies = true;
    public boolean spawnFriendlies = true;
    private static final int CACHE_SIZE = 4;
    private final long[] lastChunkPos = new long[4];
    private final ChunkStatus[] lastChunkStatus = new ChunkStatus[4];
    private final ChunkAccess[] lastChunk = new ChunkAccess[4];
    @Nullable
    @VisibleForDebug
    private NaturalSpawner.SpawnState lastSpawnState;
    // Paper start
    final com.destroystokyo.paper.util.concurrent.WeakSeqLock loadedChunkMapSeqLock = new com.destroystokyo.paper.util.concurrent.WeakSeqLock();
    final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<LevelChunk> loadedChunkMap = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>(8192, 0.5f);

    private final LevelChunk[] lastLoadedChunks = new LevelChunk[4 * 4];

    private static int getChunkCacheKey(int x, int z) {
        return x & 3 | ((z & 3) << 2);
    }

    public void addLoadedChunk(LevelChunk chunk) {
        this.loadedChunkMapSeqLock.acquireWrite();
        try {
            this.loadedChunkMap.put(chunk.coordinateKey, chunk);
        } finally {
            this.loadedChunkMapSeqLock.releaseWrite();
        }

        // rewrite cache if we have to
        // we do this since we also cache null chunks
        int cacheKey = getChunkCacheKey(chunk.locX, chunk.locZ);

        this.lastLoadedChunks[cacheKey] = chunk;
    }

    public void removeLoadedChunk(LevelChunk chunk) {
        this.loadedChunkMapSeqLock.acquireWrite();
        try {
            this.loadedChunkMap.remove(chunk.coordinateKey);
        } finally {
            this.loadedChunkMapSeqLock.releaseWrite();
        }

        // rewrite cache if we have to
        // we do this since we also cache null chunks
        int cacheKey = getChunkCacheKey(chunk.locX, chunk.locZ);

        LevelChunk cachedChunk = this.lastLoadedChunks[cacheKey];
        if (cachedChunk != null && cachedChunk.coordinateKey == chunk.coordinateKey) {
            this.lastLoadedChunks[cacheKey] = null;
        }
    }

    public final LevelChunk getChunkAtIfLoadedMainThread(int x, int z) {
        int cacheKey = getChunkCacheKey(x, z);

        LevelChunk cachedChunk = this.lastLoadedChunks[cacheKey];
        if (cachedChunk != null && cachedChunk.locX == x & cachedChunk.locZ == z) {
            return this.lastLoadedChunks[cacheKey];
        }

        long chunkKey = ChunkPos.asLong(x, z);

        cachedChunk = this.loadedChunkMap.get(chunkKey);
        // Skipping a null check to avoid extra instructions to improve inline capability
        this.lastLoadedChunks[cacheKey] = cachedChunk;
        return cachedChunk;
    }

    public final LevelChunk getChunkAtIfLoadedMainThreadNoCache(int x, int z) {
        return this.loadedChunkMap.get(ChunkPos.asLong(x, z));
    }

    public final LevelChunk getChunkAtMainThread(int x, int z) {
        LevelChunk ret = this.getChunkAtIfLoadedMainThread(x, z);
        if (ret != null) {
            return ret;
        }
        return (LevelChunk)this.getChunk(x, z, ChunkStatus.FULL, true);
    }

    long chunkFutureAwaitCounter; // Paper - private -> package private

    public void getEntityTickingChunkAsync(int x, int z, java.util.function.Consumer<LevelChunk> onLoad) {
        if (Thread.currentThread() != this.mainThread) {
            this.mainThreadProcessor.execute(() -> {
                ServerChunkCache.this.getEntityTickingChunkAsync(x, z, onLoad);
            });
            return;
        }
        this.getChunkFutureAsynchronously(x, z, 31, ChunkHolder::getEntityTickingChunkFuture, onLoad);
    }

    public void getTickingChunkAsync(int x, int z, java.util.function.Consumer<LevelChunk> onLoad) {
        if (Thread.currentThread() != this.mainThread) {
            this.mainThreadProcessor.execute(() -> {
                ServerChunkCache.this.getTickingChunkAsync(x, z, onLoad);
            });
            return;
        }
        this.getChunkFutureAsynchronously(x, z, 32, ChunkHolder::getTickingChunkFuture, onLoad);
    }

    public void getFullChunkAsync(int x, int z, java.util.function.Consumer<LevelChunk> onLoad) {
        if (Thread.currentThread() != this.mainThread) {
            this.mainThreadProcessor.execute(() -> {
                ServerChunkCache.this.getFullChunkAsync(x, z, onLoad);
            });
            return;
        }
        this.getChunkFutureAsynchronously(x, z, 33, ChunkHolder::getFullChunkFuture, onLoad);
    }

    private void getChunkFutureAsynchronously(int x, int z, int ticketLevel, java.util.function.Function<ChunkHolder, CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>>> futureGet, java.util.function.Consumer<LevelChunk> onLoad) {
        if (Thread.currentThread() != this.mainThread) {
            throw new IllegalStateException();
        }
        ChunkPos chunkPos = new ChunkPos(x, z);
        Long identifier = this.chunkFutureAwaitCounter++;
        this.distanceManager.addTicket(TicketType.FUTURE_AWAIT, chunkPos, ticketLevel, identifier);
        this.runDistanceManagerUpdates();

        ChunkHolder chunk = this.chunkMap.getUpdatingChunkIfPresent(chunkPos.toLong());

        if (chunk == null) {
            throw new IllegalStateException("Expected playerchunk " + chunkPos + " in world '" + this.level.getWorld().getName() + "'");
        }

        CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> future = futureGet.apply(chunk);

        future.whenCompleteAsync((either, throwable) -> {
            try {
                if (throwable != null) {
                    if (throwable instanceof ThreadDeath) {
                        throw (ThreadDeath)throwable;
                    }
                    net.minecraft.server.MinecraftServer.LOGGER.error("Failed to complete future await for chunk " + chunkPos.toString() + " in world '" + ServerChunkCache.this.level.getWorld().getName() + "'", throwable);
                } else if (either.right().isPresent()) {
                    net.minecraft.server.MinecraftServer.LOGGER.error("Failed to complete future await for chunk " + chunkPos.toString() + " in world '" + ServerChunkCache.this.level.getWorld().getName() + "': " + either.right().get().toString());
                }

                try {
                    if (onLoad != null) {
                        onLoad.accept(either == null ? null : either.left().orElse(null)); // indicate failure to the callback.
                    }
                } catch (Throwable thr) {
                    if (thr instanceof ThreadDeath) {
                        throw (ThreadDeath)thr;
                    }
                    net.minecraft.server.MinecraftServer.LOGGER.error("Load callback for future await failed " + chunkPos.toString() + " in world '" + ServerChunkCache.this.level.getWorld().getName() + "'", thr);
                    return;
                }
            } finally {
                // due to odd behaviour with CB unload implementation we need to have these AFTER the load callback.
                ServerChunkCache.this.distanceManager.addTicket(TicketType.UNKNOWN, chunkPos, ticketLevel, chunkPos);
                ServerChunkCache.this.distanceManager.removeTicket(TicketType.FUTURE_AWAIT, chunkPos, ticketLevel, identifier);
            }
        }, this.mainThreadProcessor);
    }
    // Paper end

    // Paper start
    @Nullable
    public ChunkAccess getChunkAtImmediately(int x, int z) {
        ChunkHolder holder = this.chunkMap.getVisibleChunkIfPresent(ChunkPos.asLong(x, z));
        if (holder == null) {
            return null;
        }

        return holder.getLastAvailable();
    }

    // this will try to avoid chunk neighbours for lighting
    public final ChunkAccess getFullStatusChunkAt(int chunkX, int chunkZ) {
        LevelChunk ifLoaded = this.getChunkAtIfLoadedImmediately(chunkX, chunkZ);
        if (ifLoaded != null) {
            return ifLoaded;
        }

        ChunkAccess empty = this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, true);
        if (empty != null && empty.getStatus().isOrAfter(ChunkStatus.FULL)) {
            return empty;
        }
        return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
    }

    public final ChunkAccess getFullStatusChunkAtIfLoaded(int chunkX, int chunkZ) {
        LevelChunk ifLoaded = this.getChunkAtIfLoadedImmediately(chunkX, chunkZ);
        if (ifLoaded != null) {
            return ifLoaded;
        }

        ChunkAccess ret = this.getChunkAtImmediately(chunkX, chunkZ);
        if (ret != null && ret.getStatus().isOrAfter(ChunkStatus.FULL)) {
            return ret;
        } else {
            return null;
        }
    }

    void getChunkAtAsynchronously(int chunkX, int chunkZ, int ticketLevel,
                                  java.util.function.Consumer<ChunkAccess> consumer) {
        this.getChunkAtAsynchronously(chunkX, chunkZ, ticketLevel, (ChunkHolder chunkHolder) -> {
            if (ticketLevel <= 33) {
                return (CompletableFuture)chunkHolder.getFullChunkFuture();
            } else {
                return chunkHolder.getOrScheduleFuture(ChunkHolder.getStatus(ticketLevel), ServerChunkCache.this.chunkMap);
            }
        }, consumer);
    }

    void getChunkAtAsynchronously(int chunkX, int chunkZ, int ticketLevel,
                                  java.util.function.Function<ChunkHolder, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> function,
                                  java.util.function.Consumer<ChunkAccess> consumer) {
        if (Thread.currentThread() != this.mainThread) {
            throw new IllegalStateException();
        }

        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        Long identifier = Long.valueOf(this.chunkFutureAwaitCounter++);
        this.addTicketAtLevel(TicketType.FUTURE_AWAIT, chunkPos, ticketLevel, identifier);
        this.runDistanceManagerUpdates();

        ChunkHolder chunk = this.chunkMap.getUpdatingChunkIfPresent(chunkPos.toLong());

        if (chunk == null) {
            throw new IllegalStateException("Expected playerchunk " + chunkPos + " in world '" + this.level.getWorld().getName() + "'");
        }

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = function.apply(chunk);

        future.whenCompleteAsync((either, throwable) -> {
            try {
                if (throwable != null) {
                    if (throwable instanceof ThreadDeath) {
                        throw (ThreadDeath)throwable;
                    }
                    LOGGER.error("Failed to complete future await for chunk " + chunkPos.toString() + " in world '" + ServerChunkCache.this.level.getWorld().getName() + "'", throwable);
                } else if (either.right().isPresent()) {
                    LOGGER.error("Failed to complete future await for chunk " + chunkPos.toString() + " in world '" + ServerChunkCache.this.level.getWorld().getName() + "': " + either.right().get().toString());
                }

                try {
                    if (consumer != null) {
                        consumer.accept(either == null ? null : either.left().orElse(null)); // indicate failure to the callback.
                    }
                } catch (Throwable thr) {
                    if (thr instanceof ThreadDeath) {
                        throw (ThreadDeath)thr;
                    }
                    LOGGER.error("Load callback for future await failed " + chunkPos.toString() + " in world '" + ServerChunkCache.this.level.getWorld().getName() + "'", thr);
                    return;
                }
            } finally {
                // due to odd behaviour with CB unload implementation we need to have these AFTER the load callback.
                ServerChunkCache.this.addTicketAtLevel(TicketType.UNKNOWN, chunkPos, ticketLevel, chunkPos);
                ServerChunkCache.this.removeTicketAtLevel(TicketType.FUTURE_AWAIT, chunkPos, ticketLevel, identifier);
            }
        }, this.mainThreadProcessor);
    }

    public <T> void addTicketAtLevel(TicketType<T> ticketType, ChunkPos chunkPos, int ticketLevel, T identifier) {
        this.distanceManager.addTicket(ticketType, chunkPos, ticketLevel, identifier);
    }

    public <T> void removeTicketAtLevel(TicketType<T> ticketType, ChunkPos chunkPos, int ticketLevel, T identifier) {
        this.distanceManager.removeTicket(ticketType, chunkPos, ticketLevel, identifier);
    }

    void chunkLoadAccept(int chunkX, int chunkZ, ChunkAccess chunk, java.util.function.Consumer<ChunkAccess> consumer) {
        try {
            consumer.accept(chunk);
        } catch (Throwable throwable) {
            if (throwable instanceof ThreadDeath) {
                throw (ThreadDeath)throwable;
            }
            LOGGER.error("Load callback for chunk " + chunkX + "," + chunkZ + " in world '" + this.level.getWorld().getName() + "' threw an exception", throwable);
        }
    }

    public final void getChunkAtAsynchronously(int chunkX, int chunkZ, ChunkStatus status, boolean gen, boolean allowSubTicketLevel, java.util.function.Consumer<ChunkAccess> onLoad) {
        // try to fire sync
        int chunkStatusTicketLevel = 33 + ChunkStatus.getDistance(status);
        ChunkHolder playerChunk = this.chunkMap.getUpdatingChunkIfPresent(io.papermc.paper.util.CoordinateUtils.getChunkKey(chunkX, chunkZ));
        if (playerChunk != null) {
            ChunkStatus holderStatus = playerChunk.getChunkHolderStatus();
            ChunkAccess immediate = playerChunk.getAvailableChunkNow();
            if (immediate != null) {
                if (allowSubTicketLevel ? immediate.getStatus().isOrAfter(status) : (playerChunk.getTicketLevel() <= chunkStatusTicketLevel && holderStatus != null && holderStatus.isOrAfter(status))) {
                    this.chunkLoadAccept(chunkX, chunkZ, immediate, onLoad);
                    return;
                } else {
                    if (gen || (!allowSubTicketLevel && immediate.getStatus().isOrAfter(status))) {
                        this.getChunkAtAsynchronously(chunkX, chunkZ, chunkStatusTicketLevel, onLoad);
                        return;
                    } else {
                        this.chunkLoadAccept(chunkX, chunkZ, null, onLoad);
                        return;
                    }
                }
            }
        }

        // need to fire async

        if (gen && !allowSubTicketLevel) {
            this.getChunkAtAsynchronously(chunkX, chunkZ, chunkStatusTicketLevel, onLoad);
            return;
        }

        this.getChunkAtAsynchronously(chunkX, chunkZ, net.minecraft.server.MCUtil.getTicketLevelFor(ChunkStatus.EMPTY), (ChunkAccess chunk) -> {
            if (chunk == null) {
                throw new IllegalStateException("Chunk cannot be null");
            }

            if (!chunk.getStatus().isOrAfter(status)) {
                if (gen) {
                    this.getChunkAtAsynchronously(chunkX, chunkZ, chunkStatusTicketLevel, onLoad);
                    return;
                } else {
                    ServerChunkCache.this.chunkLoadAccept(chunkX, chunkZ, null, onLoad);
                    return;
                }
            } else {
                if (allowSubTicketLevel) {
                    ServerChunkCache.this.chunkLoadAccept(chunkX, chunkZ, chunk, onLoad);
                    return;
                } else {
                    this.getChunkAtAsynchronously(chunkX, chunkZ, chunkStatusTicketLevel, onLoad);
                    return;
                }
            }
        });
    }

    final io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet<LevelChunk> tickingChunks = new io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet<>(4096, 0.75f, 4096, 0.15, true);
    final io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet<LevelChunk> entityTickingChunks = new io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet<>(4096, 0.75f, 4096, 0.15, true);
    // Paper end

    public ServerChunkCache(ServerLevel world, LevelStorageSource.LevelStorageAccess session, DataFixer dataFixer, StructureManager structureManager, Executor workerExecutor, ChunkGenerator chunkGenerator, int viewDistance, int simulationDistance, boolean dsync, ChunkProgressListener worldGenerationProgressListener, ChunkStatusUpdateListener chunkStatusChangeListener, Supplier<DimensionDataStorage> persistentStateManagerFactory) {
        this.level = world;
        this.mainThreadProcessor = new ServerChunkCache.MainThreadExecutor(world);
        this.mainThread = Thread.currentThread();
        File file = session.getDimensionPath(world.dimension()).resolve("data").toFile();

        file.mkdirs();
        this.dataStorage = new DimensionDataStorage(file, dataFixer);
        this.chunkMap = new ChunkMap(world, session, dataFixer, structureManager, workerExecutor, this.mainThreadProcessor, this, chunkGenerator, worldGenerationProgressListener, chunkStatusChangeListener, persistentStateManagerFactory, viewDistance, dsync);
        this.lightEngine = this.chunkMap.getLightEngine();
        this.distanceManager = this.chunkMap.getDistanceManager();
        this.distanceManager.updateSimulationDistance(simulationDistance);
        this.clearCache();
    }

    // CraftBukkit start - properly implement isChunkLoaded
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        ChunkHolder chunk = this.chunkMap.getUpdatingChunkIfPresent(ChunkPos.asLong(chunkX, chunkZ));
        if (chunk == null) {
            return false;
        }
        return chunk.getFullChunkNow() != null;
    }
    // CraftBukkit end

    @Override
    public ThreadedLevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    @Nullable
    private ChunkHolder getVisibleChunkIfPresent(long pos) {
        return this.chunkMap.getVisibleChunkIfPresent(pos);
    }

    public int getTickingGenerated() {
        return this.chunkMap.getTickingGenerated();
    }

    private void storeInCache(long pos, ChunkAccess chunk, ChunkStatus status) {
        for (int j = 3; j > 0; --j) {
            this.lastChunkPos[j] = this.lastChunkPos[j - 1];
            this.lastChunkStatus[j] = this.lastChunkStatus[j - 1];
            this.lastChunk[j] = this.lastChunk[j - 1];
        }

        this.lastChunkPos[0] = pos;
        this.lastChunkStatus[0] = status;
        this.lastChunk[0] = chunk;
    }

    // Paper start - "real" get chunk if loaded
    // Note: Partially copied from the getChunkAt method below
    @Nullable
    public LevelChunk getChunkAtIfCachedImmediately(int x, int z) {
        long k = ChunkPos.asLong(x, z);

        // Note: Bypass cache since we need to check ticket level, and to make this MT-Safe

        ChunkHolder playerChunk = this.getVisibleChunkIfPresent(k);
        if (playerChunk == null) {
            return null;
        }

        return playerChunk.getFullChunkNowUnchecked();
    }

    @Nullable
    public LevelChunk getChunkAtIfLoadedImmediately(int x, int z) {
        long k = ChunkPos.asLong(x, z);

        if (Thread.currentThread() == this.mainThread) {
            return this.getChunkAtIfLoadedMainThread(x, z);
        }

        LevelChunk ret = null;
        long readlock;
        do {
            readlock = this.loadedChunkMapSeqLock.acquireRead();
            try {
                ret = this.loadedChunkMap.get(k);
            } catch (Throwable thr) {
                if (thr instanceof ThreadDeath) {
                    throw (ThreadDeath)thr;
                }
                // re-try, this means a CME occurred...
                continue;
            }
        } while (!this.loadedChunkMapSeqLock.tryReleaseRead(readlock));

        return ret;
    }
    // Paper end
    // Paper start - async chunk io
    private long asyncLoadSeqCounter;

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getChunkAtAsynchronously(int x, int z, boolean gen, boolean isUrgent) {
        if (Thread.currentThread() != this.mainThread) {
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = new CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>();
            this.mainThreadProcessor.execute(() -> {
                this.getChunkAtAsynchronously(x, z, gen, isUrgent).whenComplete((chunk, ex) -> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                    } else {
                        future.complete(chunk);
                    }
                });
            });
            return future;
        }

        long k = ChunkPos.asLong(x, z);
        ChunkPos chunkPos = new ChunkPos(x, z);

        ChunkAccess ichunkaccess;

        // try cache
        for (int l = 0; l < 4; ++l) {
            if (k == this.lastChunkPos[l] && ChunkStatus.FULL == this.lastChunkStatus[l]) {
                ichunkaccess = this.lastChunk[l];
                if (ichunkaccess != null) { // CraftBukkit - the chunk can become accessible in the meantime TODO for non-null chunks it might also make sense to check that the chunk's state hasn't changed in the meantime

                    // move to first in cache

                    for (int i1 = 3; i1 > 0; --i1) {
                        this.lastChunkPos[i1] = this.lastChunkPos[i1 - 1];
                        this.lastChunkStatus[i1] = this.lastChunkStatus[i1 - 1];
                        this.lastChunk[i1] = this.lastChunk[i1 - 1];
                    }

                    this.lastChunkPos[0] = k;
                    this.lastChunkStatus[0] = ChunkStatus.FULL;
                    this.lastChunk[0] = ichunkaccess;

                    return CompletableFuture.completedFuture(Either.left(ichunkaccess));
                }
            }
        }

        if (gen) {
            return this.bringToFullStatusAsync(x, z, chunkPos, isUrgent);
        }

        ChunkAccess current = this.getChunkAtImmediately(x, z); // we want to bypass ticket restrictions
        if (current != null) {
            if (!(current instanceof net.minecraft.world.level.chunk.ImposterProtoChunk) && !(current instanceof LevelChunk)) {
                return CompletableFuture.completedFuture(ChunkHolder.UNLOADED_CHUNK);
            }
            // we know the chunk is at full status here (either in read-only mode or the real thing)
            return this.bringToFullStatusAsync(x, z, chunkPos, isUrgent);
        }

        // here we don't know what status it is and we're not supposed to generate
        // so we asynchronously load empty status
        return this.bringToStatusAsync(x, z, chunkPos, ChunkStatus.EMPTY, isUrgent).thenCompose((either) -> {
            ChunkAccess chunk = either.left().orElse(null);
            if (!(chunk instanceof net.minecraft.world.level.chunk.ImposterProtoChunk) && !(chunk instanceof LevelChunk)) {
                // the chunk on disk was not a full status chunk
                return CompletableFuture.completedFuture(ChunkHolder.UNLOADED_CHUNK);
            }
            // bring to full status if required
            return this.bringToFullStatusAsync(x, z, chunkPos, isUrgent);
        });
    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> bringToFullStatusAsync(int x, int z, ChunkPos chunkPos, boolean isUrgent) {
        return this.bringToStatusAsync(x, z, chunkPos, ChunkStatus.FULL, isUrgent);
    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> bringToStatusAsync(int x, int z, ChunkPos chunkPos, ChunkStatus status, boolean isUrgent) {
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = this.getChunkFutureMainThread(x, z, status, true, isUrgent);
        Long identifier = Long.valueOf(this.asyncLoadSeqCounter++);
        int ticketLevel = net.minecraft.server.MCUtil.getTicketLevelFor(status);
        this.addTicketAtLevel(TicketType.ASYNC_LOAD, chunkPos, ticketLevel, identifier);

        return future.thenComposeAsync((Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either) -> {
            // either left -> success
            // either right -> failure

            this.removeTicketAtLevel(TicketType.ASYNC_LOAD, chunkPos, ticketLevel, identifier);
            this.addTicketAtLevel(TicketType.UNKNOWN, chunkPos, ticketLevel, chunkPos); // allow unloading

            Optional<ChunkHolder.ChunkLoadingFailure> failure = either.right();

            if (failure.isPresent()) {
                // failure
                throw new IllegalStateException("Chunk failed to load: " + failure.get().toString());
            }

            return CompletableFuture.completedFuture(either);
        }, this.mainThreadProcessor);
    }

    public boolean markUrgent(ChunkPos coords) {
        return this.distanceManager.markUrgent(coords);
    }

    public boolean markHighPriority(ChunkPos coords, int priority) {
        return this.distanceManager.markHighPriority(coords, priority);
    }

    public void markAreaHighPriority(ChunkPos center, int priority, int radius) {
        this.distanceManager.markAreaHighPriority(center, priority, radius);
    }

    public void clearAreaPriorityTickets(ChunkPos center, int radius) {
        this.distanceManager.clearAreaPriorityTickets(center, radius);
    }

    public void clearPriorityTickets(ChunkPos coords) {
        this.distanceManager.clearPriorityTickets(coords);
    }
    // Paper end - async chunk io

    @Nullable
    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus leastStatus, boolean create) {
        final int x1 = x; final int z1 = z; // Paper - conflict on variable change
        if (Thread.currentThread() != this.mainThread) {
            return (ChunkAccess) CompletableFuture.supplyAsync(() -> {
                return this.getChunk(x, z, leastStatus, create);
            }, this.mainThreadProcessor).join();
        } else {
            // Paper start - optimise for loaded chunks
            LevelChunk ifLoaded = this.getChunkAtIfLoadedMainThread(x, z);
            if (ifLoaded != null) {
                return ifLoaded;
            }
            // Paper end
            ProfilerFiller gameprofilerfiller = this.level.getProfiler();

            gameprofilerfiller.incrementCounter("getChunk");
            long k = ChunkPos.asLong(x, z);

            ChunkAccess ichunkaccess;

            for (int l = 0; l < 4; ++l) {
                if (k == this.lastChunkPos[l] && leastStatus == this.lastChunkStatus[l]) {
                    ichunkaccess = this.lastChunk[l];
                    if (ichunkaccess != null) { // CraftBukkit - the chunk can become accessible in the meantime TODO for non-null chunks it might also make sense to check that the chunk's state hasn't changed in the meantime
                        return ichunkaccess;
                    }
                }
            }

            gameprofilerfiller.incrementCounter("getChunkCacheMiss");
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getChunkFutureMainThread(x, z, leastStatus, create, true); // Paper
            ServerChunkCache.MainThreadExecutor chunkproviderserver_b = this.mainThreadProcessor;

            Objects.requireNonNull(completablefuture);
            if (!completablefuture.isDone()) { // Paper
                // Paper start - async chunk io/loading
                ChunkPos pair = new ChunkPos(x1, z1); // Paper - Chunk priority
                this.distanceManager.markUrgent(pair); // Paper - Chunk priority
                this.level.asyncChunkTaskManager.raisePriority(x1, z1, com.destroystokyo.paper.io.PrioritizedTaskQueue.HIGHEST_PRIORITY);
                com.destroystokyo.paper.io.chunk.ChunkTaskManager.pushChunkWait(this.level, x1, z1);
                // Paper end
                com.destroystokyo.paper.io.SyncLoadFinder.logSyncLoad(this.level, x1, z1); // Paper - sync load info
                this.level.timings.syncChunkLoad.startTiming(); // Paper
            chunkproviderserver_b.managedBlock(completablefuture::isDone);
                com.destroystokyo.paper.io.chunk.ChunkTaskManager.popChunkWait(); // Paper - async chunk debug
                this.level.timings.syncChunkLoad.stopTiming(); // Paper
                this.distanceManager.clearPriorityTickets(pair); // Paper - Chunk priority
                this.distanceManager.clearUrgent(pair); // Paper - Chunk priority
            } // Paper
            ichunkaccess = (ChunkAccess) ((Either) completablefuture.join()).map((ichunkaccess1) -> {
                return ichunkaccess1;
            }, (playerchunk_failure) -> {
                if (create) {
                    throw (IllegalStateException) Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + playerchunk_failure));
                } else {
                    return null;
                }
            });
            this.storeInCache(k, ichunkaccess, leastStatus);
            return ichunkaccess;
        }
    }

    @Nullable
    @Override
    public LevelChunk getChunkNow(int chunkX, int chunkZ) {
        if (Thread.currentThread() != this.mainThread) {
            return null;
        } else {
            return this.getChunkAtIfLoadedMainThread(chunkX, chunkZ); // Paper - optimise for loaded chunks
        }
    }

    private void clearCache() {
        Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
        Arrays.fill(this.lastChunkStatus, (Object) null);
        Arrays.fill(this.lastChunk, (Object) null);
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getChunkFuture(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        boolean flag1 = Thread.currentThread() == this.mainThread;
        CompletableFuture completablefuture;

        if (flag1) {
            completablefuture = this.getChunkFutureMainThread(chunkX, chunkZ, leastStatus, create);
            ServerChunkCache.MainThreadExecutor chunkproviderserver_b = this.mainThreadProcessor;

            Objects.requireNonNull(completablefuture);
            chunkproviderserver_b.managedBlock(completablefuture::isDone);
        } else {
            completablefuture = CompletableFuture.supplyAsync(() -> {
                return this.getChunkFutureMainThread(chunkX, chunkZ, leastStatus, create);
            }, this.mainThreadProcessor).thenCompose((completablefuture1) -> {
                return completablefuture1;
            });
        }

        return completablefuture;
    }

    private long syncLoadCounter; // Paper - prevent plugin unloads from removing our ticket

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getChunkFutureMainThread(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        // Paper start - add isUrgent - old sig left in place for dirty nms plugins
        return getChunkFutureMainThread(chunkX, chunkZ, leastStatus, create, false);
    }
    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getChunkFutureMainThread(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create, boolean isUrgent) {
        // Paper end
        ChunkPos chunkcoordintpair = new ChunkPos(chunkX, chunkZ);
        long k = chunkcoordintpair.toLong();
        int l = 33 + ChunkStatus.getDistance(leastStatus);
        ChunkHolder playerchunk = this.getVisibleChunkIfPresent(k);

        // CraftBukkit start - don't add new ticket for currently unloading chunk
        boolean currentlyUnloading = false;
        if (playerchunk != null) {
            ChunkHolder.FullChunkStatus oldChunkState = ChunkHolder.getFullChunkStatus(playerchunk.oldTicketLevel);
            ChunkHolder.FullChunkStatus currentChunkState = ChunkHolder.getFullChunkStatus(playerchunk.getTicketLevel());
            currentlyUnloading = (oldChunkState.isOrAfter(ChunkHolder.FullChunkStatus.BORDER) && !currentChunkState.isOrAfter(ChunkHolder.FullChunkStatus.BORDER));
        }
        final Long identifier; // Paper - prevent plugin unloads from removing our ticket
        if (create && !currentlyUnloading) {
            // CraftBukkit end
            this.distanceManager.addTicket(TicketType.UNKNOWN, chunkcoordintpair, l, chunkcoordintpair);
            identifier = Long.valueOf(this.syncLoadCounter++); // Paper - prevent plugin unloads from removing our ticket
            this.distanceManager.addTicket(TicketType.REQUIRED_LOAD, chunkcoordintpair, l, identifier); // Paper - prevent plugin unloads from removing our ticket
            if (isUrgent) this.distanceManager.markUrgent(chunkcoordintpair); // Paper - Chunk priority
            if (this.chunkAbsent(playerchunk, l)) {
                ProfilerFiller gameprofilerfiller = this.level.getProfiler();

                gameprofilerfiller.push("chunkLoad");
                distanceManager.delayDistanceManagerTick = false; // Paper - Chunk priority - ensure this is never false
                this.runDistanceManagerUpdates();
                playerchunk = this.getVisibleChunkIfPresent(k);
                gameprofilerfiller.pop();
                if (this.chunkAbsent(playerchunk, l)) {
                    this.distanceManager.removeTicket(TicketType.REQUIRED_LOAD, chunkcoordintpair, l, identifier); // Paper
                    throw (IllegalStateException) Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
                }
            }

        } else { identifier = null; } // Paper - prevent plugin unloads from removing our ticket
        // Paper start - Chunk priority
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = this.chunkAbsent(playerchunk, l) ? ChunkHolder.UNLOADED_CHUNK_FUTURE : playerchunk.getOrScheduleFuture(leastStatus, this.chunkMap);
        // Paper start - prevent plugin unloads from removing our ticket
        if (create && !currentlyUnloading) {
            future.thenAcceptAsync((either) -> {
                ServerChunkCache.this.distanceManager.removeTicket(TicketType.REQUIRED_LOAD, chunkcoordintpair, l, identifier);
            }, ServerChunkCache.this.mainThreadProcessor);
        }
        // Paper end - prevent plugin unloads from removing our ticket
        if (isUrgent) {
            future.thenAccept(either -> this.distanceManager.clearUrgent(chunkcoordintpair));
        }
        return future;
        // Paper end
    }

    private boolean chunkAbsent(@Nullable ChunkHolder holder, int maxLevel) {
        return holder == null || holder.oldTicketLevel > maxLevel; // CraftBukkit using oldTicketLevel for isLoaded checks
    }

    @Override
    public boolean hasChunk(int x, int z) {
        ChunkHolder playerchunk = this.getVisibleChunkIfPresent((new ChunkPos(x, z)).toLong());
        int k = 33 + ChunkStatus.getDistance(ChunkStatus.FULL);

        return !this.chunkAbsent(playerchunk, k);
    }

    @Override
    public BlockGetter getChunkForLighting(int chunkX, int chunkZ) {
        long k = ChunkPos.asLong(chunkX, chunkZ);
        ChunkHolder playerchunk = this.getVisibleChunkIfPresent(k);

        if (playerchunk == null) {
            return null;
        } else {
            int l = ServerChunkCache.CHUNK_STATUSES.size() - 1;

            while (true) {
                ChunkStatus chunkstatus = (ChunkStatus) ServerChunkCache.CHUNK_STATUSES.get(l);
                Optional<ChunkAccess> optional = ((Either) playerchunk.getFutureIfPresentUnchecked(chunkstatus).getNow(ChunkHolder.UNLOADED_CHUNK)).left();

                if (optional.isPresent()) {
                    return (BlockGetter) optional.get();
                }

                if (chunkstatus == ChunkStatus.LIGHT.getParent()) {
                    return null;
                }

                --l;
            }
        }
    }

    @Override
    public Level getLevel() {
        return this.level;
    }

    public boolean pollTask() {
        return this.mainThreadProcessor.pollTask();
    }

    public boolean runDistanceManagerUpdates() {
        if (distanceManager.delayDistanceManagerTick) return false; // Paper - Chunk priority
        if (this.chunkMap.unloadingPlayerChunk) { LOGGER.error("Cannot tick distance manager while unloading playerchunks", new Throwable()); throw new IllegalStateException("Cannot tick distance manager while unloading playerchunks"); } // Paper
        co.aikar.timings.MinecraftTimings.distanceManagerTick.startTiming(); try { // Paper - add timings for distance manager
        boolean flag = this.distanceManager.runAllUpdates(this.chunkMap);
        boolean flag1 = this.chunkMap.promoteChunkMap();

        if (!flag && !flag1) {
            return false;
        } else {
            this.clearCache();
            return true;
        }
        } finally { co.aikar.timings.MinecraftTimings.distanceManagerTick.stopTiming(); } // Paper - add timings for distance manager
    }

    // Paper start - helper
    public boolean isPositionTicking(Entity entity) {
        return this.isPositionTicking(ChunkPos.asLong(net.minecraft.util.Mth.floor(entity.getX()) >> 4, net.minecraft.util.Mth.floor(entity.getZ()) >> 4));
    }
    // Paper end

    public boolean isPositionTicking(long pos) {
        // Paper start - replace player chunk loader system
        ChunkHolder holder = this.chunkMap.getVisibleChunkIfPresent(pos);
        return holder != null && holder.isTickingReady();
        // Paper end - replace player chunk loader system
    }

    public void save(boolean flush) {
        this.runDistanceManagerUpdates();
        try (co.aikar.timings.Timing timed = level.timings.chunkSaveData.startTiming()) { // Paper - Timings
        this.chunkMap.saveAllChunks(flush);
        } // Paper - Timings
    }

    // Paper start - duplicate save, but call incremental
    public void saveIncrementally() {
        this.runDistanceManagerUpdates();
        try (co.aikar.timings.Timing timed = level.timings.chunkSaveData.startTiming()) { // Paper - Timings
            this.chunkMap.saveIncrementally();
        } // Paper - Timings
    }
    // Paper end

    @Override
    public void close() throws IOException {
        // CraftBukkit start
        this.close(true);
    }

    public void close(boolean save) throws IOException {
        if (save) {
            this.save(true);
        }
        // CraftBukkit end
        this.lightEngine.close();
        this.chunkMap.close();
    }

    // CraftBukkit start - modelled on below
    public void purgeUnload() {
        if (true) return; // Paper - tickets will be removed later, this behavior isn't really well accounted for by the chunk system
        this.level.getProfiler().push("purge");
        this.distanceManager.purgeStaleTickets();
        this.runDistanceManagerUpdates();
        this.level.getProfiler().popPush("unload");
        this.chunkMap.tick(() -> true);
        this.level.getProfiler().pop();
        this.clearCache();
    }
    // CraftBukkit end

    @Override
    public void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks) {
        this.level.getProfiler().push("purge");
        this.level.timings.doChunkMap.startTiming(); // Spigot
        this.distanceManager.purgeStaleTickets();
        this.runDistanceManagerUpdates();
        this.level.timings.doChunkMap.stopTiming(); // Spigot
        this.level.getProfiler().popPush("chunks");
        if (tickChunks) {
            this.level.timings.chunks.startTiming(); // Paper - timings
            this.chunkMap.playerChunkManager.tick(); // Paper - this is mostly is to account for view distance changes
            this.tickChunks();
            this.level.timings.chunks.stopTiming(); // Paper - timings
        }

        this.level.timings.doChunkUnload.startTiming(); // Spigot
        this.level.getProfiler().popPush("unload");
        this.chunkMap.tick(shouldKeepTicking);
        this.level.timings.doChunkUnload.stopTiming(); // Spigot
        this.level.getProfiler().pop();
        this.clearCache();
    }

    private void tickChunks() {
        long i = this.level.getGameTime();
        long j = i - this.lastInhabitedUpdate;

        this.lastInhabitedUpdate = i;
        boolean flag = this.level.isDebug();

        if (flag) {
            this.chunkMap.tick();
        } else {
            // Paper start - optimize isOutisdeRange
            ChunkMap playerChunkMap = this.chunkMap;
            for (ServerPlayer player : this.level.players) {
                if (!player.affectsSpawning || player.isSpectator()) {
                    playerChunkMap.playerMobSpawnMap.remove(player);
                    continue;
                }

                int viewDistance = this.chunkMap.getEffectiveViewDistance();

                // copied and modified from isOutisdeRange
                int chunkRange = level.spigotConfig.mobSpawnRange;
                chunkRange = (chunkRange > viewDistance) ? (byte)viewDistance : chunkRange;
                chunkRange = (chunkRange > DistanceManager.MOB_SPAWN_RANGE) ? DistanceManager.MOB_SPAWN_RANGE : chunkRange;

                com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent event = new com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent(player.getBukkitEntity(), (byte)chunkRange);
                event.callEvent();
                if (event.isCancelled() || event.getSpawnRadius() < 0 || playerChunkMap.playerChunkTickRangeMap.getLastViewDistance(player) == -1) {
                    playerChunkMap.playerMobSpawnMap.remove(player);
                    continue;
                }

                int range = Math.min(event.getSpawnRadius(), 32); // limit to max view distance
                int chunkX = net.minecraft.server.MCUtil.getChunkCoordinate(player.getX());
                int chunkZ = net.minecraft.server.MCUtil.getChunkCoordinate(player.getZ());

                playerChunkMap.playerMobSpawnMap.addOrUpdate(player, chunkX, chunkZ, range);
                player.lastEntitySpawnRadiusSquared = (double)((range << 4) * (range << 4)); // used in anyPlayerCloseEnoughForSpawning
                player.playerNaturallySpawnedEvent = event;
            }
            // Paper end - optimize isOutisdeRange
            LevelData worlddata = this.level.getLevelData();
            ProfilerFiller gameprofilerfiller = this.level.getProfiler();

            gameprofilerfiller.push("pollingChunks");
            int k = this.level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            boolean flag1 = level.ticksPerSpawnCategory.getLong(org.bukkit.entity.SpawnCategory.ANIMAL) != 0L && worlddata.getGameTime() % level.ticksPerSpawnCategory.getLong(org.bukkit.entity.SpawnCategory.ANIMAL) == 0L; // CraftBukkit

            gameprofilerfiller.push("naturalSpawnCount");
            this.level.timings.countNaturalMobs.startTiming(); // Paper - timings
            int l = this.distanceManager.getNaturalSpawnChunkCount();
            // Paper start - per player mob spawning
            NaturalSpawner.SpawnState spawnercreature_d; // moved down
            if ((this.spawnFriendlies || this.spawnEnemies) && this.chunkMap.playerMobDistanceMap != null) { // don't count mobs when animals and monsters are disabled
                // re-set mob counts
                for (ServerPlayer player : this.level.players) {
                    Arrays.fill(player.mobCounts, 0);
                }
                spawnercreature_d = NaturalSpawner.createState(l, this.level.getAllEntities(), this::getFullChunk, null, true);
            } else {
                spawnercreature_d = NaturalSpawner.createState(l, this.level.getAllEntities(), this::getFullChunk, this.chunkMap.playerMobDistanceMap == null ? new LocalMobCapCalculator(this.chunkMap) : null, false);
            }
            // Paper end
            this.level.timings.countNaturalMobs.stopTiming(); // Paper - timings

            this.lastSpawnState = spawnercreature_d;
            gameprofilerfiller.popPush("filteringLoadedChunks");
            // Paper - moved down
            this.level.timings.chunkTicks.startTiming(); // Paper

            // Paper - moved down

            gameprofilerfiller.popPush("spawnAndTick");
            boolean flag2 = this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && !this.level.players().isEmpty(); // CraftBukkit

            // Paper - only shuffle if per-player mob spawning is disabled
            // Paper - moved natural spawn event up
            // Paper start - optimise chunk tick iteration
            Iterator<LevelChunk> iterator1;
            if (this.level.paperConfig.perPlayerMobSpawns) {
                iterator1 = this.entityTickingChunks.iterator();
            } else {
                iterator1 = this.entityTickingChunks.unsafeIterator();
                List<LevelChunk> shuffled = Lists.newArrayListWithCapacity(this.entityTickingChunks.size());
                while (iterator1.hasNext()) {
                    shuffled.add(iterator1.next());
                }
                Collections.shuffle(shuffled);
                iterator1 = shuffled.iterator();
            }

            int chunksTicked = 0; // Paper
            try {
            while (iterator1.hasNext()) {
                LevelChunk chunk1 = iterator1.next();
                ChunkHolder holder = chunk1.playerChunk;
                if (holder != null) {
                    // Paper - move down
                // Paper end - optimise chunk tick iteration
                ChunkPos chunkcoordintpair = chunk1.getPos();

                if ((true || this.level.isNaturalSpawningAllowed(chunkcoordintpair)) && this.chunkMap.anyPlayerCloseEnoughForSpawning(holder, chunkcoordintpair, false)) { // Paper - optimise anyPlayerCloseEnoughForSpawning // Paper - replace player chunk loader system
                    chunk1.incrementInhabitedTime(j);
                    if (flag2 && (this.spawnEnemies || this.spawnFriendlies) && this.level.getWorldBorder().isWithinBounds(chunkcoordintpair) && this.chunkMap.anyPlayerCloseEnoughForSpawning(holder, chunkcoordintpair, true)) { // Spigot // Paper - optimise anyPlayerCloseEnoughForSpawning & optimise chunk tick iteration
                        NaturalSpawner.spawnForChunk(this.level, chunk1, spawnercreature_d, this.spawnFriendlies, this.spawnEnemies, flag1);
                    }

                    if (true || this.level.shouldTickBlocksAt(chunkcoordintpair.toLong())) { // Paper - replace player chunk loader system
                        this.level.tickChunk(chunk1, k);
                        if ((chunksTicked++ & 1) == 0) net.minecraft.server.MinecraftServer.getServer().executeMidTickTasks(); // Paper
                    }
                }
                // Paper start - optimise chunk tick iteration
                }
            }

            } finally {
                if (iterator1 instanceof io.papermc.paper.util.maplist.IteratorSafeOrderedReferenceSet.Iterator safeIterator) {
                    safeIterator.finishedIterating();
                }
            }
            // Paper end - optimise chunk tick iteration
            this.level.timings.chunkTicks.stopTiming(); // Paper
            gameprofilerfiller.popPush("customSpawners");
            if (flag2) {
                try (co.aikar.timings.Timing ignored = this.level.timings.miscMobSpawning.startTiming()) { // Paper - timings
                this.level.tickCustomSpawners(this.spawnEnemies, this.spawnFriendlies);
                } // Paper - timings
            }
            gameprofilerfiller.pop();
            // Paper start - use set of chunks requiring updates, rather than iterating every single one loaded
            gameprofilerfiller.popPush("broadcast");
            this.level.timings.broadcastChunkUpdates.startTiming(); // Paper - timing
            if (!this.chunkMap.needsChangeBroadcasting.isEmpty()) {
                ReferenceOpenHashSet<ChunkHolder> copy = this.chunkMap.needsChangeBroadcasting.clone();
                this.chunkMap.needsChangeBroadcasting.clear();
                for (ChunkHolder holder : copy) {
                    holder.broadcastChanges(holder.getFullChunkNowUnchecked()); // LevelChunks are NEVER unloaded
                    if (holder.needsBroadcastChanges()) {
                        // I DON'T want to KNOW what DUMB plugins might be doing.
                        this.chunkMap.needsChangeBroadcasting.add(holder);
                    }
                }
            }
            this.level.timings.broadcastChunkUpdates.stopTiming(); // Paper - timing
            gameprofilerfiller.pop();
            // Paper end - use set of chunks requiring updates, rather than iterating every single one loaded
            // Paper start - controlled flush for entity tracker packets
            List<net.minecraft.network.Connection> disabledFlushes = new java.util.ArrayList<>(this.level.players.size());
            for (ServerPlayer player : this.level.players) {
                net.minecraft.server.network.ServerGamePacketListenerImpl connection = player.connection;
                if (connection != null) {
                    connection.connection.disableAutomaticFlush();
                    disabledFlushes.add(connection.connection);
                }
            }
            try { // Paper end - controlled flush for entity tracker packets
            this.chunkMap.tick();
            // Paper start - controlled flush for entity tracker packets
            } finally {
                for (net.minecraft.network.Connection networkManager : disabledFlushes) {
                    networkManager.enableAutomaticFlush();
                }
            }
            // Paper end - controlled flush for entity tracker packets
        }
    }

    private void getFullChunk(long pos, Consumer<LevelChunk> chunkConsumer) {
        ChunkHolder playerchunk = this.getVisibleChunkIfPresent(pos);

        if (playerchunk != null) {
            ((Either) playerchunk.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK)).left().ifPresent(chunkConsumer);
        }

    }

    @Override
    public String gatherStats() {
        return Integer.toString(this.getLoadedChunksCount());
    }

    @VisibleForTesting
    public int getPendingTasksCount() {
        return this.mainThreadProcessor.getPendingTasksCount();
    }

    public ChunkGenerator getGenerator() {
        return this.chunkMap.generator();
    }

    @Override
    public int getLoadedChunksCount() {
        return this.chunkMap.size();
    }

    public void blockChanged(BlockPos pos) {
        int i = SectionPos.blockToSectionCoord(pos.getX());
        int j = SectionPos.blockToSectionCoord(pos.getZ());
        ChunkHolder playerchunk = this.getVisibleChunkIfPresent(ChunkPos.asLong(i, j));

        if (playerchunk != null) {
            playerchunk.blockChanged(pos);
        }

    }

    @Override
    public void onLightUpdate(LightLayer type, SectionPos pos) {
        this.mainThreadProcessor.execute(() -> {
            ChunkHolder playerchunk = this.getVisibleChunkIfPresent(pos.chunk().toLong());

            if (playerchunk != null) {
                playerchunk.sectionLightChanged(type, pos.y());
            }

        });
    }

    public <T> void addRegionTicket(TicketType<T> ticketType, ChunkPos pos, int radius, T argument) {
        this.distanceManager.addRegionTicket(ticketType, pos, radius, argument);
    }

    public <T> void removeRegionTicket(TicketType<T> ticketType, ChunkPos pos, int radius, T argument) {
        this.distanceManager.removeRegionTicket(ticketType, pos, radius, argument);
    }

    @Override
    public void updateChunkForced(ChunkPos pos, boolean forced) {
        this.distanceManager.updateChunkForced(pos, forced);
    }

    public void move(ServerPlayer player) {
        if (!player.isRemoved()) {
            this.chunkMap.move(player);
        }

    }

    public void removeEntity(Entity entity) {
        this.chunkMap.removeEntity(entity);
    }

    public void addEntity(Entity entity) {
        this.chunkMap.addEntity(entity);
    }

    public void broadcastAndSend(Entity entity, Packet<?> packet) {
        this.chunkMap.broadcastAndSend(entity, packet);
    }

    public void broadcast(Entity entity, Packet<?> packet) {
        this.chunkMap.broadcast(entity, packet);
    }

    public void setViewDistance(int watchDistance) {
        this.chunkMap.setViewDistance(watchDistance);
    }

    public void setSimulationDistance(int simulationDistance) {
        this.distanceManager.updateSimulationDistance(simulationDistance);
    }

    @Override
    public void setSpawnSettings(boolean spawnMonsters, boolean spawnAnimals) {
        this.spawnEnemies = spawnMonsters;
        this.spawnFriendlies = spawnAnimals;
    }

    public String getChunkDebugData(ChunkPos pos) {
        return this.chunkMap.getChunkDebugData(pos);
    }

    public DimensionDataStorage getDataStorage() {
        return this.dataStorage;
    }

    public PoiManager getPoiManager() {
        return this.chunkMap.getPoiManager();
    }

    public ChunkScanAccess chunkScanner() {
        return this.chunkMap.chunkScanner();
    }

    @Nullable
    @VisibleForDebug
    public NaturalSpawner.SpawnState getLastSpawnState() {
        return this.lastSpawnState;
    }

    public void removeTicketsOnClosing() {
        this.distanceManager.removeTicketsOnClosing();
    }

    public final class MainThreadExecutor extends BlockableEventLoop<Runnable> {

        MainThreadExecutor(Level world) {
            super("Chunk source main thread executor for " + world.dimension().location());
        }

        @Override
        protected Runnable wrapRunnable(Runnable runnable) {
            return runnable;
        }

        @Override
        protected boolean shouldRun(Runnable task) {
            return true;
        }

        @Override
        protected boolean scheduleExecutables() {
            return true;
        }

        @Override
        protected Thread getRunningThread() {
            return ServerChunkCache.this.mainThread;
        }

        @Override
        protected void doRunTask(Runnable task) {
            ServerChunkCache.this.level.getProfiler().incrementCounter("runTask");
            super.doRunTask(task);
        }

        @Override
        // CraftBukkit start - process pending Chunk loadCallback() and unloadCallback() after each run task
        public boolean pollTask() {
        try {
            boolean execChunkTask = com.destroystokyo.paper.io.chunk.ChunkTaskManager.pollChunkWaitQueue() || ServerChunkCache.this.level.asyncChunkTaskManager.pollNextChunkTask(); // Paper
            ServerChunkCache.this.chunkMap.playerChunkManager.tickMidTick(); // Paper
            if (ServerChunkCache.this.runDistanceManagerUpdates()) {
                return true;
            } else {
                ServerChunkCache.this.lightEngine.tryScheduleUpdate();
                return super.pollTask() || execChunkTask; // Paper
            }
        } finally {
            chunkMap.chunkLoadConversionCallbackExecutor.run(); // Paper - Add chunk load conversion callback executor to prevent deadlock due to recursion in the chunk task queue sorter
            chunkMap.callbackExecutor.run();
        }
        // CraftBukkit end
        }
    }

    // CraftBukkit start - decompile error
    private static record ChunkAndHolder(LevelChunk chunk, ChunkHolder holder) {

        /*
        final Chunk chunk;
        final PlayerChunk holder;

        a(Chunk chunk, PlayerChunk playerchunk) {
            this.chunk = chunk;
            this.holder = playerchunk;
        }

        public Chunk chunk() {
            return this.chunk;
        }

        public PlayerChunk holder() {
            return this.holder;
        }
         */
        // CraftBukkit end
    }
}
