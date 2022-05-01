package net.minecraft.server.level;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.util.DebugBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
// CraftBukkit start
import net.minecraft.server.MinecraftServer;
// CraftBukkit end

public class ChunkHolder {

    public static final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> UNLOADED_CHUNK = Either.right(ChunkHolder.ChunkLoadingFailure.UNLOADED);
    public static final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> UNLOADED_CHUNK_FUTURE = CompletableFuture.completedFuture(ChunkHolder.UNLOADED_CHUNK);
    public static final Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> UNLOADED_LEVEL_CHUNK = Either.right(ChunkHolder.ChunkLoadingFailure.UNLOADED);
    private static final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> NOT_DONE_YET = Either.right(ChunkHolder.ChunkLoadingFailure.UNLOADED);
    private static final CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> UNLOADED_LEVEL_CHUNK_FUTURE = CompletableFuture.completedFuture(ChunkHolder.UNLOADED_LEVEL_CHUNK);
    private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.getStatusList();
    private static final ChunkHolder.FullChunkStatus[] FULL_CHUNK_STATUSES = ChunkHolder.FullChunkStatus.values();
    private static final int BLOCKS_BEFORE_RESEND_FUDGE = 64;
    private final AtomicReferenceArray<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> futures;
    private final LevelHeightAccessor levelHeightAccessor;
    private volatile CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> fullChunkFuture; private int fullChunkCreateCount; private volatile boolean isFullChunkReady; // Paper - cache chunk ticking stage
    private volatile CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> tickingChunkFuture; private volatile boolean isTickingReady; // Paper - cache chunk ticking stage
    private volatile CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> entityTickingChunkFuture; private volatile boolean isEntityTickingReady; // Paper - cache chunk ticking stage
    public CompletableFuture<ChunkAccess> chunkToSave;  // Paper - public
    @Nullable
    private final DebugBuffer<ChunkHolder.ChunkSaveDebug> chunkToSaveHistory;
    public int oldTicketLevel;
    private int ticketLevel;
    public volatile int queueLevel; // Paper - private->public, make volatile since this is concurrently accessed
    public final ChunkPos pos;
    private boolean hasChangedSections;
    private final ShortSet[] changedBlocksPerSection;
    private final BitSet blockChangedLightSectionFilter;
    private final BitSet skyChangedLightSectionFilter;
    private final LevelLightEngine lightEngine;
    private final ChunkHolder.LevelChangeListener onLevelChange;
    public final ChunkHolder.PlayerProvider playerProvider;
    private boolean wasAccessibleSinceLastSave;
    private boolean resendLight;
    private CompletableFuture<Void> pendingFullStateConfirmation;

    public ServerLevel getWorld() { return chunkMap.level; } // Paper
    boolean isUpdateQueued = false; // Paper
    private final ChunkMap chunkMap; // Paper
    // Paper start - no-tick view distance
    public final LevelChunk getSendingChunk() {
        // it's important that we use getChunkAtIfLoadedImmediately to mirror the chunk sending logic used
        // in Chunk's neighbour callback
        LevelChunk ret = this.chunkMap.level.getChunkSource().getChunkAtIfLoadedImmediately(this.pos.x, this.pos.z);
        if (ret != null && ret.areNeighboursLoaded(1)) {
            return ret;
        }
        return null;
    }
    // Paper end - no-tick view distance

    // Paper start - optimise anyPlayerCloseEnoughForSpawning
    // cached here to avoid a map lookup
    com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> playersInMobSpawnRange;
    com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> playersInChunkTickRange;

    void onChunkAdd() {
        long key = net.minecraft.server.MCUtil.getCoordinateKey(this.pos);
        this.playersInMobSpawnRange = this.chunkMap.playerMobSpawnMap.getObjectsInRange(key);
        this.playersInChunkTickRange = this.chunkMap.playerChunkTickRangeMap.getObjectsInRange(key);
        // Paper start - optimise chunk tick iteration
        if (this.needsBroadcastChanges()) {
            this.chunkMap.needsChangeBroadcasting.add(this);
        }
        // Paper end - optimise chunk tick iteration
        // Paper start - optimise checkDespawn
        LevelChunk chunk = this.getFullChunkNowUnchecked();
        if (chunk != null) {
            chunk.updateGeneralAreaCache();
        }
        // Paper end - optimise checkDespawn
    }

    void onChunkRemove() {
        this.playersInMobSpawnRange = null;
        this.playersInChunkTickRange = null;
        // Paper start - optimise chunk tick iteration
        if (this.needsBroadcastChanges()) {
            this.chunkMap.needsChangeBroadcasting.remove(this);
        }
        // Paper end - optimise chunk tick iteration
        // Paper start - optimise checkDespawn
        LevelChunk chunk = this.getFullChunkNowUnchecked();
        if (chunk != null) {
            chunk.removeGeneralAreaCache();
        }
        // Paper end - optimise checkDespawn
    }
    // Paper end - optimise anyPlayerCloseEnoughForSpawning
    long lastAutoSaveTime; // Paper - incremental autosave
    long inactiveTimeStart; // Paper - incremental autosave
    // Paper start - optimize chunk status progression without jumping through thread pool
    public boolean canAdvanceStatus() {
        ChunkStatus status = getChunkHolderStatus();
        ChunkAccess chunk = getAvailableChunkNow();
        return chunk != null && (status == null || chunk.getStatus().isOrAfter(getNextStatus(status)));
    }
    // Paper end

    public ChunkHolder(ChunkPos pos, int level, LevelHeightAccessor world, LevelLightEngine lightingProvider, ChunkHolder.LevelChangeListener levelUpdateListener, ChunkHolder.PlayerProvider playersWatchingChunkProvider) {
        this.futures = new AtomicReferenceArray(ChunkHolder.CHUNK_STATUSES.size());
        this.fullChunkFuture = ChunkHolder.UNLOADED_LEVEL_CHUNK_FUTURE;
        this.tickingChunkFuture = ChunkHolder.UNLOADED_LEVEL_CHUNK_FUTURE;
        this.entityTickingChunkFuture = ChunkHolder.UNLOADED_LEVEL_CHUNK_FUTURE;
        this.chunkToSave = CompletableFuture.completedFuture(null); // CraftBukkit - decompile error
        this.chunkToSaveHistory = null;
        this.blockChangedLightSectionFilter = new BitSet();
        this.skyChangedLightSectionFilter = new BitSet();
        this.pendingFullStateConfirmation = CompletableFuture.completedFuture(null); // CraftBukkit - decompile error
        this.pos = pos;
        this.levelHeightAccessor = world;
        this.lightEngine = lightingProvider;
        this.onLevelChange = levelUpdateListener;
        this.playerProvider = playersWatchingChunkProvider;
        this.oldTicketLevel = ChunkMap.MAX_CHUNK_DISTANCE + 1;
        this.ticketLevel = this.oldTicketLevel;
        this.queueLevel = this.oldTicketLevel;
        this.setTicketLevel(level);
        this.changedBlocksPerSection = new ShortSet[world.getSectionsCount()];
        this.chunkMap = (ChunkMap)playersWatchingChunkProvider; // Paper
        this.onChunkAdd(); // Paper - optimise anyPlayerCloseEnoughForSpawning
    }

    // Paper start
    public @Nullable ChunkAccess getAvailableChunkNow() {
        // TODO can we just getStatusFuture(EMPTY)?
        for (ChunkStatus curr = ChunkStatus.FULL, next = curr.getParent(); curr != next; curr = next, next = next.getParent()) {
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = this.getFutureIfPresentUnchecked(curr);
            Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = future.getNow(null);
            if (either == null || either.left().isEmpty()) {
                continue;
            }
            return either.left().get();
        }
        return null;
    }
    // Paper end
    // CraftBukkit start
    public LevelChunk getFullChunkNow() {
        // Note: We use the oldTicketLevel for isLoaded checks.
        if (!ChunkHolder.getFullChunkStatus(this.oldTicketLevel).isOrAfter(ChunkHolder.FullChunkStatus.BORDER)) return null;
        return this.getFullChunkNowUnchecked();
    }

    public LevelChunk getFullChunkNowUnchecked() {
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> statusFuture = this.getFutureIfPresentUnchecked(ChunkStatus.FULL);
        Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = (Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>) statusFuture.getNow(null);
        return (either == null) ? null : (LevelChunk) either.left().orElse(null);
    }
    // CraftBukkit end

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getFutureIfPresentUnchecked(ChunkStatus leastStatus) {
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = (CompletableFuture) this.futures.get(leastStatus.getIndex());

        return completablefuture == null ? ChunkHolder.UNLOADED_CHUNK_FUTURE : completablefuture;
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getFutureIfPresent(ChunkStatus leastStatus) {
        return ChunkHolder.getStatus(this.ticketLevel).isOrAfter(leastStatus) ? this.getFutureIfPresentUnchecked(leastStatus) : ChunkHolder.UNLOADED_CHUNK_FUTURE;
    }

    public final CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> getTickingChunkFuture() { // Paper - final for inline
        return this.tickingChunkFuture;
    }

    public final CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> getEntityTickingChunkFuture() { // Paper - final for inline
        return this.entityTickingChunkFuture;
    }

    public final CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> getFullChunkFuture() { // Paper - final for inline
        return this.fullChunkFuture;
    }

    @Nullable
    public final LevelChunk getTickingChunk() { // Paper - final for inline
        CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getTickingChunkFuture();
        Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> either = (Either) completablefuture.getNow(null); // CraftBukkit - decompile error

        return either == null ? null : (LevelChunk) either.left().orElse(null); // CraftBukkit - decompile error
    }

    @Nullable
    public final LevelChunk getFullChunk() { // Paper - final for inline
        CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getFullChunkFuture();
        Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> either = (Either) completablefuture.getNow(null); // CraftBukkit - decompile error

        return either == null ? null : (LevelChunk) either.left().orElse(null); // CraftBukkit - decompile error
    }

    @Nullable
    public ChunkStatus getLastAvailableStatus() {
        for (int i = ChunkHolder.CHUNK_STATUSES.size() - 1; i >= 0; --i) {
            ChunkStatus chunkstatus = (ChunkStatus) ChunkHolder.CHUNK_STATUSES.get(i);
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getFutureIfPresentUnchecked(chunkstatus);

            if (((Either) completablefuture.getNow(ChunkHolder.UNLOADED_CHUNK)).left().isPresent()) {
                return chunkstatus;
            }
        }

        return null;
    }

    // Paper start
    public ChunkStatus getChunkHolderStatus() {
        for (ChunkStatus curr = ChunkStatus.FULL, next = curr.getParent(); curr != next; curr = next, next = next.getParent()) {
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = this.getFutureIfPresentUnchecked(curr);
            Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = future.getNow(null);
            if (either == null || !either.left().isPresent()) {
                continue;
            }
            return curr;
        }

        return null;
    }
    // Paper end

    @Nullable
    public ChunkAccess getLastAvailable() {
        for (int i = ChunkHolder.CHUNK_STATUSES.size() - 1; i >= 0; --i) {
            ChunkStatus chunkstatus = (ChunkStatus) ChunkHolder.CHUNK_STATUSES.get(i);
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getFutureIfPresentUnchecked(chunkstatus);

            if (!completablefuture.isCompletedExceptionally()) {
                Optional<ChunkAccess> optional = ((Either) completablefuture.getNow(ChunkHolder.UNLOADED_CHUNK)).left();

                if (optional.isPresent()) {
                    return (ChunkAccess) optional.get();
                }
            }
        }

        return null;
    }

    public final CompletableFuture<ChunkAccess> getChunkToSave() { // Paper - final for inline
        return this.chunkToSave;
    }

    public void blockChanged(BlockPos pos) {
        if (!pos.isInsideBuildHeightAndWorldBoundsHorizontal(levelHeightAccessor)) return; // Paper - SPIGOT-6086 for all invalid locations; avoid acquiring locks
        LevelChunk chunk = this.getSendingChunk(); // Paper - no-tick view distance

        if (chunk != null) {
            int i = this.levelHeightAccessor.getSectionIndex(pos.getY());

            if (i < 0 || i >= this.changedBlocksPerSection.length) return; // CraftBukkit - SPIGOT-6086, SPIGOT-6296
            if (this.changedBlocksPerSection[i] == null) {
                this.hasChangedSections = true; this.addToBroadcastMap(); // Paper - optimise chunk tick iteration
                this.changedBlocksPerSection[i] = new ShortOpenHashSet();
            }

            this.changedBlocksPerSection[i].add(SectionPos.sectionRelativePos(pos));
        }
    }

    public void sectionLightChanged(LightLayer lightType, int y) {
        ChunkAccess chunk = this.getAvailableChunkNow(); // Paper - no-tick view distance

        if (chunk != null) {
            chunk.setUnsaved(true);
            LevelChunk chunk1 = this.getSendingChunk(); // Paper - no-tick view distance

            if (chunk1 != null) {
                int j = this.lightEngine.getMinLightSection();
                int k = this.lightEngine.getMaxLightSection();

                if (y >= j && y <= k) {
                    this.addToBroadcastMap(); // Paper - optimise chunk tick iteration
                    int l = y - j;

                    if (lightType == LightLayer.SKY) {
                        this.skyChangedLightSectionFilter.set(l);
                    } else {
                        this.blockChangedLightSectionFilter.set(l);
                    }

                }
            }
        }
    }

    // Paper start - optimise chunk tick iteration
    public final boolean needsBroadcastChanges() {
        return this.hasChangedSections || !this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty();
    }

    private void addToBroadcastMap() {
        org.spigotmc.AsyncCatcher.catchOp("ChunkHolder update");
        this.chunkMap.needsChangeBroadcasting.add(this);
    }
    // Paper end - optimise chunk tick iteration

    public void broadcastChanges(LevelChunk chunk) {
        if (this.needsBroadcastChanges()) { // Paper - moved into above, other logic needs to call
            Level world = chunk.getLevel();
            int i = 0;

            int j;

            for (j = 0; j < this.changedBlocksPerSection.length; ++j) {
                i += this.changedBlocksPerSection[j] != null ? this.changedBlocksPerSection[j].size() : 0;
            }

            this.resendLight |= i >= 64;
            if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
                this.broadcast(new ClientboundLightUpdatePacket(chunk.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter, true), !this.resendLight);
                this.skyChangedLightSectionFilter.clear();
                this.blockChangedLightSectionFilter.clear();
            }

            for (j = 0; j < this.changedBlocksPerSection.length; ++j) {
                ShortSet shortset = this.changedBlocksPerSection[j];

                if (shortset != null) {
                    int k = this.levelHeightAccessor.getSectionYFromSectionIndex(j);
                    SectionPos sectionposition = SectionPos.of(chunk.getPos(), k);

                    if (shortset.size() == 1) {
                        BlockPos blockposition = sectionposition.relativeToBlockPos(shortset.iterator().nextShort());
                        BlockState iblockdata = world.getBlockState(blockposition);

                        this.broadcast(new ClientboundBlockUpdatePacket(blockposition, iblockdata), false);
                        this.broadcastBlockEntityIfNeeded(world, blockposition, iblockdata);
                    } else {
                        LevelChunkSection chunksection = chunk.getSection(j);
                        ClientboundSectionBlocksUpdatePacket packetplayoutmultiblockchange = new ClientboundSectionBlocksUpdatePacket(sectionposition, shortset, chunksection, this.resendLight);

                        this.broadcast(packetplayoutmultiblockchange, false);
                        packetplayoutmultiblockchange.runUpdates((blockposition1, iblockdata1) -> {
                            this.broadcastBlockEntityIfNeeded(world, blockposition1, iblockdata1);
                        });
                    }

                    this.changedBlocksPerSection[j] = null;
                }
            }

            this.hasChangedSections = false;
        }
    }

    private void broadcastBlockEntityIfNeeded(Level world, BlockPos pos, BlockState state) {
        if (state.hasBlockEntity()) {
            this.broadcastBlockEntity(world, pos);
        }

    }

    private void broadcastBlockEntity(Level world, BlockPos pos) {
        BlockEntity tileentity = world.getBlockEntity(pos);

        if (tileentity != null) {
            Packet<?> packet = tileentity.getUpdatePacket();

            if (packet != null) {
                this.broadcast(packet, false);
            }
        }

    }

    public void broadcast(Packet<?> packet, boolean onlyOnWatchDistanceEdge) {
        // Paper start - per player view distance
        // there can be potential desync with player's last mapped section and the view distance map, so use the
        // view distance map here.
        com.destroystokyo.paper.util.misc.PlayerAreaMap viewDistanceMap = this.chunkMap.playerChunkManager.broadcastMap; // Paper - replace old player chunk manager
        com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> players = viewDistanceMap.getObjectsInRange(this.pos);
        if (players == null) {
            return;
        }

        Object[] backingSet = players.getBackingSet();
        for (int i = 0, len = backingSet.length; i < len; ++i) {
            Object temp = backingSet[i];
            if (!(temp instanceof ServerPlayer)) {
                continue;
            }
            ServerPlayer player = (ServerPlayer)temp;
            if (!this.chunkMap.playerChunkManager.isChunkSent(player, this.pos.x, this.pos.z, onlyOnWatchDistanceEdge)) {
                continue;
            }
            player.connection.send(packet);
        }
        // Paper end - per player view distance
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getOrScheduleFuture(ChunkStatus targetStatus, ChunkMap chunkStorage) {
        int i = targetStatus.getIndex();
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = (CompletableFuture) this.futures.get(i);

        if (completablefuture != null) {
            Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = (Either) completablefuture.getNow(ChunkHolder.NOT_DONE_YET);

            if (either == null) {
                String s = "value in future for status: " + targetStatus + " was incorrectly set to null at chunk: " + this.pos;

                throw chunkStorage.debugFuturesAndCreateReportedException(new IllegalStateException("null value previously set for chunk status"), s);
            }

            if (either == ChunkHolder.NOT_DONE_YET || either.right().isEmpty()) {
                return completablefuture;
            }
        }

        if (ChunkHolder.getStatus(this.ticketLevel).isOrAfter(targetStatus)) {
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture1 = chunkStorage.schedule(this, targetStatus);

            this.updateChunkToSave(completablefuture1, "schedule " + targetStatus);
            this.futures.set(i, completablefuture1);
            return completablefuture1;
        } else {
            return completablefuture == null ? ChunkHolder.UNLOADED_CHUNK_FUTURE : completablefuture;
        }
    }

    protected void addSaveDependency(String thenDesc, CompletableFuture<?> then) {
        if (this.chunkToSaveHistory != null) {
            this.chunkToSaveHistory.push(new ChunkHolder.ChunkSaveDebug(Thread.currentThread(), then, thenDesc));
        }

        this.chunkToSave = this.chunkToSave.thenCombine(then, (ichunkaccess, object) -> {
            return ichunkaccess;
        });
    }

    private void updateChunkToSave(CompletableFuture<? extends Either<? extends ChunkAccess, ChunkHolder.ChunkLoadingFailure>> then, String thenDesc) {
        if (this.chunkToSaveHistory != null) {
            this.chunkToSaveHistory.push(new ChunkHolder.ChunkSaveDebug(Thread.currentThread(), then, thenDesc));
        }

        this.chunkToSave = this.chunkToSave.thenCombine(then, (ichunkaccess, either) -> {
            return (ChunkAccess) either.map((ichunkaccess1) -> {
                return ichunkaccess1;
            }, (playerchunk_failure) -> {
                return ichunkaccess;
            });
        });
    }

    public ChunkHolder.FullChunkStatus getFullStatus() {
        return ChunkHolder.getFullChunkStatus(this.ticketLevel);
    }

    public final ChunkPos getPos() { // Paper - final for inline
        return this.pos;
    }

    public final int getTicketLevel() { // Paper - final for inline
        return this.ticketLevel;
    }

    public int getQueueLevel() {
        return this.queueLevel;
    }

    private void setQueueLevel(int level) {
        this.queueLevel = level;
    }

    public void setTicketLevel(int level) {
        this.ticketLevel = level;
    }

    private void scheduleFullChunkPromotion(ChunkMap playerchunkmap, CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> completablefuture, Executor executor, ChunkHolder.FullChunkStatus playerchunk_state) {
        this.pendingFullStateConfirmation.cancel(false);
        CompletableFuture<Void> completablefuture1 = new CompletableFuture();

        completablefuture1.thenRunAsync(() -> {
            // Paper start - do not allow ticket level changes
            boolean unloadingBefore = this.chunkMap.unloadingPlayerChunk;
            this.chunkMap.unloadingPlayerChunk = true;
            try {
            // Paper end  - do not allow ticket level changes
            playerchunkmap.onFullChunkStatusChange(this.pos, playerchunk_state);
            } finally { this.chunkMap.unloadingPlayerChunk = unloadingBefore; } // Paper - do not allow ticket level changes
        }, executor);
        this.pendingFullStateConfirmation = completablefuture1;
        completablefuture.thenAccept((either) -> {
            either.ifLeft((chunk) -> {
                completablefuture1.complete(null); // CraftBukkit - decompile error
            });
        });
    }

    // Paper start
    private boolean loadCallbackScheduled = false;
    private boolean unloadCallbackScheduled = false;
    // Paper end

    private void demoteFullChunk(ChunkMap playerchunkmap, ChunkHolder.FullChunkStatus playerchunk_state) {
        this.pendingFullStateConfirmation.cancel(false);
        // Paper start - do not allow ticket level changes
        boolean unloadingBefore = this.chunkMap.unloadingPlayerChunk;
        this.chunkMap.unloadingPlayerChunk = true;
        try { // Paper end  - do not allow ticket level changes
        playerchunkmap.onFullChunkStatusChange(this.pos, playerchunk_state);
        } finally { this.chunkMap.unloadingPlayerChunk = unloadingBefore; } // Paper - do not allow ticket level changes
    }

    protected long updateCount; // Paper - correctly handle recursion
    protected void updateFutures(ChunkMap chunkStorage, Executor executor) {
        io.papermc.paper.util.TickThread.ensureTickThread("Async ticket level update"); // Paper
        long updateCount = ++this.updateCount; // Paper - correctly handle recursion
        ChunkStatus chunkstatus = ChunkHolder.getStatus(this.oldTicketLevel);
        ChunkStatus chunkstatus1 = ChunkHolder.getStatus(this.ticketLevel);
        boolean flag = this.oldTicketLevel <= ChunkMap.MAX_CHUNK_DISTANCE;
        boolean flag1 = this.ticketLevel <= ChunkMap.MAX_CHUNK_DISTANCE; // Paper - diff on change: (flag1 = new ticket level is in loadable range)
        ChunkHolder.FullChunkStatus playerchunk_state = ChunkHolder.getFullChunkStatus(this.oldTicketLevel);
        ChunkHolder.FullChunkStatus playerchunk_state1 = ChunkHolder.getFullChunkStatus(this.ticketLevel);
        // CraftBukkit start
        // ChunkUnloadEvent: Called before the chunk is unloaded: isChunkLoaded is still true and chunk can still be modified by plugins.
        if (playerchunk_state.isOrAfter(ChunkHolder.FullChunkStatus.BORDER) && !playerchunk_state1.isOrAfter(ChunkHolder.FullChunkStatus.BORDER)) {
            this.getFutureIfPresentUnchecked(ChunkStatus.FULL).thenAccept((either) -> {
                io.papermc.paper.util.TickThread.ensureTickThread("Async full status chunk future completion"); // Paper
                LevelChunk chunk = (LevelChunk)either.left().orElse(null);
                if (chunk != null && chunk.wasLoadCallbackInvoked() && ChunkHolder.this.ticketLevel > 33) { // Paper - only invoke unload if load was called
                    // Paper  start - only schedule once, now the future is no longer completed as RIGHT if unloaded...
                    if (ChunkHolder.this.unloadCallbackScheduled) {
                        return;
                    }
                    ChunkHolder.this.unloadCallbackScheduled = true;
                    // Paper  end - only schedule once, now the future is no longer completed as RIGHT if unloaded...
                    chunkStorage.callbackExecutor.execute(() -> {
                        // Paper  start - only schedule once, now the future is no longer completed as RIGHT if unloaded...
                        ChunkHolder.this.unloadCallbackScheduled = false;
                        if (ChunkHolder.this.ticketLevel <= 33) {
                            return;
                        }
                        // Paper  end - only schedule once, now the future is no longer completed as RIGHT if unloaded...
                        // Minecraft will apply the chunks tick lists to the world once the chunk got loaded, and then store the tick
                        // lists again inside the chunk once the chunk becomes inaccessible and set the chunk's needsSaving flag.
                        // These actions may however happen deferred, so we manually set the needsSaving flag already here.
                        chunk.setUnsaved(true);
                        chunk.unloadCallback();
                    });
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                MinecraftServer.LOGGER.error("Failed to schedule unload callback for chunk " + ChunkHolder.this.pos, throwable);
                return null;
            });

            // Run callback right away if the future was already done
            chunkStorage.callbackExecutor.run();
            // Paper start - correctly handle recursion
            if (this.updateCount != updateCount) {
                // something else updated ticket level for us.
                return;
            }
            // Paper end - correctly handle recursion
        }
        // CraftBukkit end

        if (flag) {
            Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = Either.right(new ChunkHolder.ChunkLoadingFailure() {
                public String toString() {
                    return "Unloaded ticket level " + ChunkHolder.this.pos;
                }
            });

            for (int i = flag1 ? chunkstatus1.getIndex() + 1 : 0; i <= chunkstatus.getIndex(); ++i) {
                CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = (CompletableFuture) this.futures.get(i);

                if (completablefuture == null) {
                    this.futures.set(i, CompletableFuture.completedFuture(either));
                }
            }
        }

        boolean flag2 = playerchunk_state.isOrAfter(ChunkHolder.FullChunkStatus.BORDER);
        boolean flag3 = playerchunk_state1.isOrAfter(ChunkHolder.FullChunkStatus.BORDER);

        boolean prevHasBeenLoaded = this.wasAccessibleSinceLastSave; // Paper
        this.wasAccessibleSinceLastSave |= flag3;
        // Paper start - incremental autosave
        if (this.wasAccessibleSinceLastSave & !prevHasBeenLoaded) {
            long timeSinceAutoSave = this.inactiveTimeStart - this.lastAutoSaveTime;
            if (timeSinceAutoSave < 0) {
                // safest bet is to assume autosave is needed here
                timeSinceAutoSave = this.chunkMap.level.paperConfig.autoSavePeriod;
            }
            this.lastAutoSaveTime = this.chunkMap.level.getGameTime() - timeSinceAutoSave;
            this.chunkMap.autoSaveQueue.add(this);
        }
        // Paper end
        if (!flag2 && flag3) {
            int expectCreateCount = ++this.fullChunkCreateCount; // Paper
            this.fullChunkFuture = chunkStorage.prepareAccessibleChunk(this);
            this.scheduleFullChunkPromotion(chunkStorage, this.fullChunkFuture, executor, ChunkHolder.FullChunkStatus.BORDER);
            // Paper start - cache ticking ready status
            this.fullChunkFuture.thenAccept(either -> {
                io.papermc.paper.util.TickThread.ensureTickThread("Async full chunk future completion"); // Paper
                final Optional<LevelChunk> left = either.left();
                if (left.isPresent() && ChunkHolder.this.fullChunkCreateCount == expectCreateCount) {
                    // note: Here is a very good place to add callbacks to logic waiting on this.
                    LevelChunk fullChunk = either.left().get();
                    ChunkHolder.this.isFullChunkReady = true;
                    fullChunk.playerChunk = ChunkHolder.this;
                    this.chunkMap.distanceManager.clearPriorityTickets(pos);
                }
            });
            this.updateChunkToSave(this.fullChunkFuture, "full");
        }

        if (flag2 && !flag3) {
            this.fullChunkFuture.complete(ChunkHolder.UNLOADED_LEVEL_CHUNK);
            this.fullChunkFuture = ChunkHolder.UNLOADED_LEVEL_CHUNK_FUTURE;
            ++this.fullChunkCreateCount; // Paper - cache ticking ready status
            this.isFullChunkReady = false; // Paper - cache ticking ready status
        }

        boolean flag4 = playerchunk_state.isOrAfter(ChunkHolder.FullChunkStatus.TICKING);
        boolean flag5 = playerchunk_state1.isOrAfter(ChunkHolder.FullChunkStatus.TICKING);

        if (!flag4 && flag5) {
            this.tickingChunkFuture = chunkStorage.prepareTickingChunk(this);
            this.scheduleFullChunkPromotion(chunkStorage, this.tickingChunkFuture, executor, ChunkHolder.FullChunkStatus.TICKING);
            // Paper start - cache ticking ready status
            this.tickingChunkFuture.thenAccept(either -> {
                io.papermc.paper.util.TickThread.ensureTickThread("Async full chunk future completion"); // Paper
                either.ifLeft(chunk -> {
                    // note: Here is a very good place to add callbacks to logic waiting on this.
                    ChunkHolder.this.isTickingReady = true;
                    // Paper start - ticking chunk set
                    ChunkHolder.this.chunkMap.level.getChunkSource().tickingChunks.add(chunk);
                    // Paper end - ticking chunk set
                });
            });
            // Paper end
            this.updateChunkToSave(this.tickingChunkFuture, "ticking");
        }

        if (flag4 && !flag5) {
            this.tickingChunkFuture.complete(ChunkHolder.UNLOADED_LEVEL_CHUNK); this.isTickingReady = false; // Paper - cache chunk ticking stage
            this.tickingChunkFuture = ChunkHolder.UNLOADED_LEVEL_CHUNK_FUTURE;
            // Paper start - ticking chunk set
            LevelChunk chunkIfCached = this.getFullChunkNowUnchecked();
            if (chunkIfCached != null) {
                this.chunkMap.level.getChunkSource().tickingChunks.remove(chunkIfCached);
            }
            // Paper end - ticking chunk set
        }

        boolean flag6 = playerchunk_state.isOrAfter(ChunkHolder.FullChunkStatus.ENTITY_TICKING);
        boolean flag7 = playerchunk_state1.isOrAfter(ChunkHolder.FullChunkStatus.ENTITY_TICKING);

        if (!flag6 && flag7) {
            if (this.entityTickingChunkFuture != ChunkHolder.UNLOADED_LEVEL_CHUNK_FUTURE) {
                throw (IllegalStateException) Util.pauseInIde(new IllegalStateException());
            }

            this.entityTickingChunkFuture = chunkStorage.prepareEntityTickingChunk(this.pos);
            this.scheduleFullChunkPromotion(chunkStorage, this.entityTickingChunkFuture, executor, ChunkHolder.FullChunkStatus.ENTITY_TICKING);
            // Paper start - cache ticking ready status
            this.entityTickingChunkFuture.thenAccept(either -> {
                io.papermc.paper.util.TickThread.ensureTickThread("Async full chunk future completion"); // Paper
                either.ifLeft(chunk -> {
                    ChunkHolder.this.isEntityTickingReady = true;
                    // Paper start - entity ticking chunk set
                    ChunkHolder.this.chunkMap.level.getChunkSource().entityTickingChunks.add(chunk);
                    // Paper end - entity ticking chunk set
                });
            });
            // Paper end
            this.updateChunkToSave(this.entityTickingChunkFuture, "entity ticking");
        }

        if (flag6 && !flag7) {
            this.entityTickingChunkFuture.complete(ChunkHolder.UNLOADED_LEVEL_CHUNK); this.isEntityTickingReady = false; // Paper - cache chunk ticking stage
            this.entityTickingChunkFuture = ChunkHolder.UNLOADED_LEVEL_CHUNK_FUTURE;
            // Paper start - entity ticking chunk set
            LevelChunk chunkIfCached = this.getFullChunkNowUnchecked();
            if (chunkIfCached != null) {
                this.chunkMap.level.getChunkSource().entityTickingChunks.remove(chunkIfCached);
            }
            // Paper end - entity ticking chunk set
        }

        if (!playerchunk_state1.isOrAfter(playerchunk_state)) {
            this.demoteFullChunk(chunkStorage, playerchunk_state1);
        }

        //this.onLevelChange.onLevelChange(this.pos, this::getQueueLevel, this.ticketLevel, this::setQueueLevel);
        // Paper start - raise IO/load priority if priority changes, use our preferred priority
        priorityBoost = chunkMap.distanceManager.getChunkPriority(pos);
        int currRequestedPriority = this.requestedPriority;
        int priority = getDemandedPriority();
        int newRequestedPriority = this.requestedPriority = priority;
        if (this.queueLevel > priority) {
            int ioPriority = com.destroystokyo.paper.io.PrioritizedTaskQueue.NORMAL_PRIORITY;
            if (priority <= 10) {
                ioPriority = com.destroystokyo.paper.io.PrioritizedTaskQueue.HIGHEST_PRIORITY;
            } else if (priority <= 20) {
                ioPriority = com.destroystokyo.paper.io.PrioritizedTaskQueue.HIGH_PRIORITY;
            }
            chunkMap.level.asyncChunkTaskManager.raisePriority(pos.x, pos.z, ioPriority);
            chunkMap.level.getChunkSource().getLightEngine().queue.changePriority(pos.toLong(), this.queueLevel, priority); // Paper // Restore this in chunk priority later?
        }
        if (currRequestedPriority != newRequestedPriority) {
            this.onLevelChange.onLevelChange(this.pos, () -> this.queueLevel, priority, p -> this.queueLevel = p); // use preferred priority
            int neighborsPriority = getNeighborsPriority();
            this.neighbors.forEach((neighbor, neighborDesired) -> neighbor.setNeighborPriority(this, neighborsPriority));
        }
        // Paper end
        this.oldTicketLevel = this.ticketLevel;
        // CraftBukkit start
        // ChunkLoadEvent: Called after the chunk is loaded: isChunkLoaded returns true and chunk is ready to be modified by plugins.
        if (!playerchunk_state.isOrAfter(ChunkHolder.FullChunkStatus.BORDER) && playerchunk_state1.isOrAfter(ChunkHolder.FullChunkStatus.BORDER)) {
            this.getFutureIfPresentUnchecked(ChunkStatus.FULL).thenAccept((either) -> {
                io.papermc.paper.util.TickThread.ensureTickThread("Async full status chunk future completion"); // Paper
                LevelChunk chunk = (LevelChunk)either.left().orElse(null);
                if (chunk != null && ChunkHolder.this.oldTicketLevel <= 33 && !chunk.wasLoadCallbackInvoked()) { // Paper - ensure ticket level is set to loaded before calling, as now this can complete with ticket level > 33
                    // Paper  start - only schedule once, now the future is no longer completed as RIGHT if unloaded...
                    if (ChunkHolder.this.loadCallbackScheduled) {
                        return;
                    }
                    ChunkHolder.this.loadCallbackScheduled = true;
                    // Paper  end - only schedule once, now the future is no longer completed as RIGHT if unloaded...
                    chunkStorage.callbackExecutor.execute(() -> {
                        ChunkHolder.this.loadCallbackScheduled = false; // Paper  - only schedule once, now the future is no longer completed as RIGHT if unloaded...
                        if (ChunkHolder.this.oldTicketLevel <= 33) chunk.loadCallback(); // Paper "
                    });
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                MinecraftServer.LOGGER.error("Failed to schedule load callback for chunk " + ChunkHolder.this.pos, throwable);
                return null;
            });

            // Run callback right away if the future was already done
            chunkStorage.callbackExecutor.run();
        }
        // CraftBukkit end
    }

    public static ChunkStatus getStatus(int level) {
        return level < 33 ? ChunkStatus.FULL : ChunkStatus.getStatusAroundFullChunk(level - 33);
    }

    public static ChunkHolder.FullChunkStatus getFullChunkStatus(int distance) {
        return ChunkHolder.FULL_CHUNK_STATUSES[Mth.clamp(33 - distance + 1, (int) 0, ChunkHolder.FULL_CHUNK_STATUSES.length - 1)];
    }

    public boolean wasAccessibleSinceLastSave() {
        return this.wasAccessibleSinceLastSave;
    }

    public void refreshAccessibility() {
        boolean prev = this.wasAccessibleSinceLastSave; // Paper
        this.wasAccessibleSinceLastSave = ChunkHolder.getFullChunkStatus(this.ticketLevel).isOrAfter(ChunkHolder.FullChunkStatus.BORDER);
        // Paper start - incremental autosave
        if (prev != this.wasAccessibleSinceLastSave) {
            if (this.wasAccessibleSinceLastSave) {
                long timeSinceAutoSave = this.inactiveTimeStart - this.lastAutoSaveTime;
                if (timeSinceAutoSave < 0) {
                    // safest bet is to assume autosave is needed here
                    timeSinceAutoSave = this.chunkMap.level.paperConfig.autoSavePeriod;
                }
                this.lastAutoSaveTime = this.chunkMap.level.getGameTime() - timeSinceAutoSave;
                this.chunkMap.autoSaveQueue.add(this);
            } else {
                this.inactiveTimeStart = this.chunkMap.level.getGameTime();
                this.chunkMap.autoSaveQueue.remove(this);
            }
        }
        // Paper end
    }

    // Paper start - incremental autosave
    public boolean setHasBeenLoaded() {
        this.wasAccessibleSinceLastSave = getFullChunkStatus(this.ticketLevel).isOrAfter(ChunkHolder.FullChunkStatus.BORDER);
        return this.wasAccessibleSinceLastSave;
    }
    // Paper end

    public void replaceProtoChunk(ImposterProtoChunk chunk) {
        for (int i = 0; i < this.futures.length(); ++i) {
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = (CompletableFuture) this.futures.get(i);

            if (completablefuture != null) {
                Optional<ChunkAccess> optional = ((Either) completablefuture.getNow(ChunkHolder.UNLOADED_CHUNK)).left();

                if (!optional.isEmpty() && optional.get() instanceof ProtoChunk) {
                    this.futures.set(i, CompletableFuture.completedFuture(Either.left(chunk)));
                }
            }
        }

        this.updateChunkToSave(CompletableFuture.completedFuture(Either.left(chunk.getWrapped())), "replaceProto");
    }

    public List<Pair<ChunkStatus, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>>> getAllFutures() {
        List<Pair<ChunkStatus, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>>> list = new ArrayList();

        for (int i = 0; i < ChunkHolder.CHUNK_STATUSES.size(); ++i) {
            list.add(Pair.of((ChunkStatus) ChunkHolder.CHUNK_STATUSES.get(i), (CompletableFuture) this.futures.get(i)));
        }

        return list;
    }

    @FunctionalInterface
    public interface LevelChangeListener {

        void onLevelChange(ChunkPos pos, IntSupplier levelGetter, int targetLevel, IntConsumer levelSetter);
    }

    public interface PlayerProvider {

        List<ServerPlayer> getPlayers(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge);
    }

    private static final class ChunkSaveDebug {

        private final Thread thread;
        private final CompletableFuture<?> future;
        private final String source;

        ChunkSaveDebug(Thread thread, CompletableFuture<?> action, String actionDesc) {
            this.thread = thread;
            this.future = action;
            this.source = actionDesc;
        }
    }

    public static enum FullChunkStatus {

        INACCESSIBLE, BORDER, TICKING, ENTITY_TICKING;

        private FullChunkStatus() {}

        public boolean isOrAfter(ChunkHolder.FullChunkStatus levelType) {
            return this.ordinal() >= levelType.ordinal();
        }
    }

    public interface ChunkLoadingFailure {

        ChunkHolder.ChunkLoadingFailure UNLOADED = new ChunkHolder.ChunkLoadingFailure() {
            public String toString() {
                return "UNLOADED";
            }
        };
    }

    // Paper start - Chunk gen/load priority system
    volatile int neighborPriority = -1;
    volatile int priorityBoost = 0;
    public final java.util.concurrent.ConcurrentHashMap<ChunkHolder, ChunkStatus> neighbors = new java.util.concurrent.ConcurrentHashMap<>();
    public final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Integer> neighborPriorities = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    int requestedPriority = ChunkMap.MAX_CHUNK_DISTANCE + 1; // this priority is possible pending, but is used to ensure needless updates are not queued

    private int getDemandedPriority() {
        int priority = neighborPriority; // if we have a neighbor priority, use it
        int myPriority = getMyPriority();

        if (priority == -1 || (ticketLevel <= 33 && priority > myPriority)) {
            priority = myPriority;
        }

        return Math.max(1, Math.min(Math.max(ticketLevel, ChunkMap.MAX_CHUNK_DISTANCE), priority));
    }

    private int getMyPriority() {
        if (priorityBoost == DistanceManager.URGENT_PRIORITY) {
            return 2; // Urgent - ticket level isn't always 31 so 33-30 = 3, but allow 1 more tasks to go below this for dependents
        }
        return ticketLevel - priorityBoost;
    }

    private int getNeighborsPriority() {
        return (neighborPriorities.isEmpty() ? getMyPriority() : getDemandedPriority()) + 1;
    }

    public void onNeighborRequest(ChunkHolder neighbor, ChunkStatus status) {
        neighbor.setNeighborPriority(this, getNeighborsPriority());
        this.neighbors.compute(neighbor, (playerChunk, currentWantedStatus) -> {
            if (currentWantedStatus == null || !currentWantedStatus.isOrAfter(status)) {
                //System.out.println(this + " request " + neighbor + " at " + status + " currently " + currentWantedStatus);
                return status;
            } else {
                //System.out.println(this + " requested " + neighbor + " at " + status + " but thats lower than other wanted status " + currentWantedStatus);
                return currentWantedStatus;
            }
        });

    }

    public void onNeighborDone(ChunkHolder neighbor, ChunkStatus chunkstatus, ChunkAccess chunk) {
        this.neighbors.compute(neighbor, (playerChunk, wantedStatus) -> {
            if (wantedStatus != null && chunkstatus.isOrAfter(wantedStatus)) {
                //System.out.println(this + " neighbor done at " + neighbor + " for status " + chunkstatus + " wanted " + wantedStatus);
                neighbor.removeNeighborPriority(this);
                return null;
            } else {
                //System.out.println(this + " neighbor finished our previous request at " + neighbor + " for status " + chunkstatus + " but we now want instead " + wantedStatus);
                return wantedStatus;
            }
        });
    }

    private void removeNeighborPriority(ChunkHolder requester) {
        synchronized (neighborPriorities) {
            neighborPriorities.remove(requester.pos.toLong());
            recalcNeighborPriority();
        }
        checkPriority();
    }


    private void setNeighborPriority(ChunkHolder requester, int priority) {
        synchronized (neighborPriorities) {
            if (!Integer.valueOf(priority).equals(neighborPriorities.put(requester.pos.toLong(), Integer.valueOf(priority)))) {
                recalcNeighborPriority();
            }
        }
        checkPriority();
    }

    private void recalcNeighborPriority() {
        neighborPriority = -1;
        if (!neighborPriorities.isEmpty()) {
            synchronized (neighborPriorities) {
                for (Integer neighbor : neighborPriorities.values()) {
                    if (neighbor < neighborPriority || neighborPriority == -1) {
                        neighborPriority = neighbor;
                    }
                }
            }
        }
    }
    private void checkPriority() {
        if (this.requestedPriority != getDemandedPriority()) this.chunkMap.queueHolderUpdate(this);
    }

    public final double getDistance(ServerPlayer player) {
        return getDistance(player.getX(), player.getZ());
    }
    public final double getDistance(double blockX, double blockZ) {
        int cx = net.minecraft.server.MCUtil.fastFloor(blockX) >> 4;
        int cz = net.minecraft.server.MCUtil.fastFloor(blockZ) >> 4;
        final double x = pos.x - cx;
        final double z = pos.z - cz;
        return (x * x) + (z * z);
    }

    public final double getDistanceFrom(BlockPos pos) {
        return getDistance(pos.getX(), pos.getZ());
    }

    public static ChunkStatus getNextStatus(ChunkStatus status) {
        if (status == ChunkStatus.FULL) {
            return status;
        }
        return CHUNK_STATUSES.get(status.getIndex() + 1);
    }
    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getStatusFutureUncheckedMain(ChunkStatus chunkstatus) {
        return ensureMain(getFutureIfPresentUnchecked(chunkstatus));
    }
    public <T> CompletableFuture<T> ensureMain(CompletableFuture<T> future) {
        return future.thenApplyAsync(r -> r, chunkMap.mainInvokingExecutor);
    }

    @Override
    public String toString() {
        return "PlayerChunk{" +
            "location=" + pos +
            ", ticketLevel=" + ticketLevel + "/" + getStatus(this.ticketLevel) +
            ", chunkHolderStatus=" + getChunkHolderStatus() +
            ", neighborPriority=" + getNeighborsPriority() +
            ", priority=(" + ticketLevel + " - " + priorityBoost +" vs N " + neighborPriority + ") = " + getDemandedPriority() + " A " + queueLevel +
            '}';
    }
    public final boolean isEntityTickingReady() {
        return this.isEntityTickingReady;
    }

    public final boolean isTickingReady() {
        return this.isTickingReady;
    }

    public final boolean isFullChunkReady() {
        return this.isFullChunkReady;
    }
    // Paper end
}
