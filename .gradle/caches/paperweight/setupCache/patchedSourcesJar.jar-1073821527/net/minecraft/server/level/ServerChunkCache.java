package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

public class ServerChunkCache extends ChunkSource {
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
        for(int i = 3; i > 0; --i) {
            this.lastChunkPos[i] = this.lastChunkPos[i - 1];
            this.lastChunkStatus[i] = this.lastChunkStatus[i - 1];
            this.lastChunk[i] = this.lastChunk[i - 1];
        }

        this.lastChunkPos[0] = pos;
        this.lastChunkStatus[0] = status;
        this.lastChunk[0] = chunk;
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus leastStatus, boolean create) {
        if (Thread.currentThread() != this.mainThread) {
            return CompletableFuture.supplyAsync(() -> {
                return this.getChunk(x, z, leastStatus, create);
            }, this.mainThreadProcessor).join();
        } else {
            ProfilerFiller profilerFiller = this.level.getProfiler();
            profilerFiller.incrementCounter("getChunk");
            long l = ChunkPos.asLong(x, z);

            for(int i = 0; i < 4; ++i) {
                if (l == this.lastChunkPos[i] && leastStatus == this.lastChunkStatus[i]) {
                    ChunkAccess chunkAccess = this.lastChunk[i];
                    if (chunkAccess != null || !create) {
                        return chunkAccess;
                    }
                }
            }

            profilerFiller.incrementCounter("getChunkCacheMiss");
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.getChunkFutureMainThread(x, z, leastStatus, create);
            this.mainThreadProcessor.managedBlock(completableFuture::isDone);
            ChunkAccess chunkAccess2 = completableFuture.join().map((chunk) -> {
                return chunk;
            }, (unloaded) -> {
                if (create) {
                    throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + unloaded));
                } else {
                    return null;
                }
            });
            this.storeInCache(l, chunkAccess2, leastStatus);
            return chunkAccess2;
        }
    }

    @Nullable
    @Override
    public LevelChunk getChunkNow(int chunkX, int chunkZ) {
        if (Thread.currentThread() != this.mainThread) {
            return null;
        } else {
            this.level.getProfiler().incrementCounter("getChunkNow");
            long l = ChunkPos.asLong(chunkX, chunkZ);

            for(int i = 0; i < 4; ++i) {
                if (l == this.lastChunkPos[i] && this.lastChunkStatus[i] == ChunkStatus.FULL) {
                    ChunkAccess chunkAccess = this.lastChunk[i];
                    return chunkAccess instanceof LevelChunk ? (LevelChunk)chunkAccess : null;
                }
            }

            ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(l);
            if (chunkHolder == null) {
                return null;
            } else {
                Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = chunkHolder.getFutureIfPresent(ChunkStatus.FULL).getNow((Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>)null);
                if (either == null) {
                    return null;
                } else {
                    ChunkAccess chunkAccess2 = either.left().orElse((ChunkAccess)null);
                    if (chunkAccess2 != null) {
                        this.storeInCache(l, chunkAccess2, ChunkStatus.FULL);
                        if (chunkAccess2 instanceof LevelChunk) {
                            return (LevelChunk)chunkAccess2;
                        }
                    }

                    return null;
                }
            }
        }
    }

    private void clearCache() {
        Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
        Arrays.fill(this.lastChunkStatus, (Object)null);
        Arrays.fill(this.lastChunk, (Object)null);
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getChunkFuture(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        boolean bl = Thread.currentThread() == this.mainThread;
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture;
        if (bl) {
            completableFuture = this.getChunkFutureMainThread(chunkX, chunkZ, leastStatus, create);
            this.mainThreadProcessor.managedBlock(completableFuture::isDone);
        } else {
            completableFuture = CompletableFuture.supplyAsync(() -> {
                return this.getChunkFutureMainThread(chunkX, chunkZ, leastStatus, create);
            }, this.mainThreadProcessor).thenCompose((completableFuture) -> {
                return completableFuture;
            });
        }

        return completableFuture;
    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getChunkFutureMainThread(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        long l = chunkPos.toLong();
        int i = 33 + ChunkStatus.getDistance(leastStatus);
        ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(l);
        if (create) {
            this.distanceManager.addTicket(TicketType.UNKNOWN, chunkPos, i, chunkPos);
            if (this.chunkAbsent(chunkHolder, i)) {
                ProfilerFiller profilerFiller = this.level.getProfiler();
                profilerFiller.push("chunkLoad");
                this.runDistanceManagerUpdates();
                chunkHolder = this.getVisibleChunkIfPresent(l);
                profilerFiller.pop();
                if (this.chunkAbsent(chunkHolder, i)) {
                    throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
                }
            }
        }

        return this.chunkAbsent(chunkHolder, i) ? ChunkHolder.UNLOADED_CHUNK_FUTURE : chunkHolder.getOrScheduleFuture(leastStatus, this.chunkMap);
    }

    private boolean chunkAbsent(@Nullable ChunkHolder holder, int maxLevel) {
        return holder == null || holder.getTicketLevel() > maxLevel;
    }

    @Override
    public boolean hasChunk(int x, int z) {
        ChunkHolder chunkHolder = this.getVisibleChunkIfPresent((new ChunkPos(x, z)).toLong());
        int i = 33 + ChunkStatus.getDistance(ChunkStatus.FULL);
        return !this.chunkAbsent(chunkHolder, i);
    }

    @Override
    public BlockGetter getChunkForLighting(int chunkX, int chunkZ) {
        long l = ChunkPos.asLong(chunkX, chunkZ);
        ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(l);
        if (chunkHolder == null) {
            return null;
        } else {
            int i = CHUNK_STATUSES.size() - 1;

            while(true) {
                ChunkStatus chunkStatus = CHUNK_STATUSES.get(i);
                Optional<ChunkAccess> optional = chunkHolder.getFutureIfPresentUnchecked(chunkStatus).getNow(ChunkHolder.UNLOADED_CHUNK).left();
                if (optional.isPresent()) {
                    return optional.get();
                }

                if (chunkStatus == ChunkStatus.LIGHT.getParent()) {
                    return null;
                }

                --i;
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
        boolean bl = this.distanceManager.runAllUpdates(this.chunkMap);
        boolean bl2 = this.chunkMap.promoteChunkMap();
        if (!bl && !bl2) {
            return false;
        } else {
            this.clearCache();
            return true;
        }
    }

    public boolean isPositionTicking(long pos) {
        ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos);
        if (chunkHolder == null) {
            return false;
        } else if (!this.level.shouldTickBlocksAt(pos)) {
            return false;
        } else {
            Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> either = chunkHolder.getTickingChunkFuture().getNow((Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>)null);
            return either != null && either.left().isPresent();
        }
    }

    public void save(boolean flush) {
        this.runDistanceManagerUpdates();
        this.chunkMap.saveAllChunks(flush);
    }

    @Override
    public void close() throws IOException {
        this.save(true);
        this.lightEngine.close();
        this.chunkMap.close();
    }

    @Override
    public void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks) {
        this.level.getProfiler().push("purge");
        this.distanceManager.purgeStaleTickets();
        this.runDistanceManagerUpdates();
        this.level.getProfiler().popPush("chunks");
        if (tickChunks) {
            this.tickChunks();
        }

        this.level.getProfiler().popPush("unload");
        this.chunkMap.tick(shouldKeepTicking);
        this.level.getProfiler().pop();
        this.clearCache();
    }

    private void tickChunks() {
        long l = this.level.getGameTime();
        long m = l - this.lastInhabitedUpdate;
        this.lastInhabitedUpdate = l;
        boolean bl = this.level.isDebug();
        if (bl) {
            this.chunkMap.tick();
        } else {
            LevelData levelData = this.level.getLevelData();
            ProfilerFiller profilerFiller = this.level.getProfiler();
            profilerFiller.push("pollingChunks");
            int i = this.level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            boolean bl2 = levelData.getGameTime() % 400L == 0L;
            profilerFiller.push("naturalSpawnCount");
            int j = this.distanceManager.getNaturalSpawnChunkCount();
            NaturalSpawner.SpawnState spawnState = NaturalSpawner.createState(j, this.level.getAllEntities(), this::getFullChunk, new LocalMobCapCalculator(this.chunkMap));
            this.lastSpawnState = spawnState;
            profilerFiller.popPush("filteringLoadedChunks");
            List<ServerChunkCache.ChunkAndHolder> list = Lists.newArrayListWithCapacity(j);

            for(ChunkHolder chunkHolder : this.chunkMap.getChunks()) {
                LevelChunk levelChunk = chunkHolder.getTickingChunk();
                if (levelChunk != null) {
                    list.add(new ServerChunkCache.ChunkAndHolder(levelChunk, chunkHolder));
                }
            }

            profilerFiller.popPush("spawnAndTick");
            boolean bl3 = this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
            Collections.shuffle(list);

            for(ServerChunkCache.ChunkAndHolder chunkAndHolder : list) {
                LevelChunk levelChunk2 = chunkAndHolder.chunk;
                ChunkPos chunkPos = levelChunk2.getPos();
                if (this.level.isNaturalSpawningAllowed(chunkPos) && this.chunkMap.anyPlayerCloseEnoughForSpawning(chunkPos)) {
                    levelChunk2.incrementInhabitedTime(m);
                    if (bl3 && (this.spawnEnemies || this.spawnFriendlies) && this.level.getWorldBorder().isWithinBounds(chunkPos)) {
                        NaturalSpawner.spawnForChunk(this.level, levelChunk2, spawnState, this.spawnFriendlies, this.spawnEnemies, bl2);
                    }

                    if (this.level.shouldTickBlocksAt(chunkPos.toLong())) {
                        this.level.tickChunk(levelChunk2, i);
                    }
                }
            }

            profilerFiller.popPush("customSpawners");
            if (bl3) {
                this.level.tickCustomSpawners(this.spawnEnemies, this.spawnFriendlies);
            }

            profilerFiller.popPush("broadcast");
            list.forEach((chunk) -> {
                chunk.holder.broadcastChanges(chunk.chunk);
            });
            profilerFiller.pop();
            profilerFiller.pop();
            this.chunkMap.tick();
        }
    }

    private void getFullChunk(long pos, Consumer<LevelChunk> chunkConsumer) {
        ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos);
        if (chunkHolder != null) {
            chunkHolder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left().ifPresent(chunkConsumer);
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
        ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(ChunkPos.asLong(i, j));
        if (chunkHolder != null) {
            chunkHolder.blockChanged(pos);
        }

    }

    @Override
    public void onLightUpdate(LightLayer type, SectionPos pos) {
        this.mainThreadProcessor.execute(() -> {
            ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos.chunk().toLong());
            if (chunkHolder != null) {
                chunkHolder.sectionLightChanged(type, pos.y());
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

    static record ChunkAndHolder(LevelChunk chunk, ChunkHolder holder) {
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
        protected boolean pollTask() {
            if (ServerChunkCache.this.runDistanceManagerUpdates()) {
                return true;
            } else {
                ServerChunkCache.this.lightEngine.tryScheduleUpdate();
                return super.pollTask();
            }
        }
    }
}
