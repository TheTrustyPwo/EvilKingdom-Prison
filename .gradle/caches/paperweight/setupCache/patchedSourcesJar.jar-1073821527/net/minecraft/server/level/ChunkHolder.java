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

public class ChunkHolder {
    public static final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> UNLOADED_CHUNK = Either.right(ChunkHolder.ChunkLoadingFailure.UNLOADED);
    public static final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> UNLOADED_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_CHUNK);
    public static final Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> UNLOADED_LEVEL_CHUNK = Either.right(ChunkHolder.ChunkLoadingFailure.UNLOADED);
    private static final Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> NOT_DONE_YET = Either.right(ChunkHolder.ChunkLoadingFailure.UNLOADED);
    private static final CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> UNLOADED_LEVEL_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_LEVEL_CHUNK);
    private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.getStatusList();
    private static final ChunkHolder.FullChunkStatus[] FULL_CHUNK_STATUSES = ChunkHolder.FullChunkStatus.values();
    private static final int BLOCKS_BEFORE_RESEND_FUDGE = 64;
    private final AtomicReferenceArray<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> futures = new AtomicReferenceArray<>(CHUNK_STATUSES.size());
    private final LevelHeightAccessor levelHeightAccessor;
    private volatile CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private volatile CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private volatile CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private CompletableFuture<ChunkAccess> chunkToSave = CompletableFuture.completedFuture((ChunkAccess)null);
    @Nullable
    private final DebugBuffer<ChunkHolder.ChunkSaveDebug> chunkToSaveHistory = null;
    public int oldTicketLevel;
    private int ticketLevel;
    private int queueLevel;
    public final ChunkPos pos;
    private boolean hasChangedSections;
    private final ShortSet[] changedBlocksPerSection;
    private final BitSet blockChangedLightSectionFilter = new BitSet();
    private final BitSet skyChangedLightSectionFilter = new BitSet();
    private final LevelLightEngine lightEngine;
    private final ChunkHolder.LevelChangeListener onLevelChange;
    public final ChunkHolder.PlayerProvider playerProvider;
    private boolean wasAccessibleSinceLastSave;
    private boolean resendLight;
    private CompletableFuture<Void> pendingFullStateConfirmation = CompletableFuture.completedFuture((Void)null);

    public ChunkHolder(ChunkPos pos, int level, LevelHeightAccessor world, LevelLightEngine lightingProvider, ChunkHolder.LevelChangeListener levelUpdateListener, ChunkHolder.PlayerProvider playersWatchingChunkProvider) {
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
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getFutureIfPresentUnchecked(ChunkStatus leastStatus) {
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.futures.get(leastStatus.getIndex());
        return completableFuture == null ? UNLOADED_CHUNK_FUTURE : completableFuture;
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getFutureIfPresent(ChunkStatus leastStatus) {
        return getStatus(this.ticketLevel).isOrAfter(leastStatus) ? this.getFutureIfPresentUnchecked(leastStatus) : UNLOADED_CHUNK_FUTURE;
    }

    public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> getTickingChunkFuture() {
        return this.tickingChunkFuture;
    }

    public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> getEntityTickingChunkFuture() {
        return this.entityTickingChunkFuture;
    }

    public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> getFullChunkFuture() {
        return this.fullChunkFuture;
    }

    @Nullable
    public LevelChunk getTickingChunk() {
        CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.getTickingChunkFuture();
        Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> either = completableFuture.getNow((Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>)null);
        return either == null ? null : either.left().orElse((LevelChunk)null);
    }

    @Nullable
    public LevelChunk getFullChunk() {
        CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.getFullChunkFuture();
        Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> either = completableFuture.getNow((Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>)null);
        return either == null ? null : either.left().orElse((LevelChunk)null);
    }

    @Nullable
    public ChunkStatus getLastAvailableStatus() {
        for(int i = CHUNK_STATUSES.size() - 1; i >= 0; --i) {
            ChunkStatus chunkStatus = CHUNK_STATUSES.get(i);
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.getFutureIfPresentUnchecked(chunkStatus);
            if (completableFuture.getNow(UNLOADED_CHUNK).left().isPresent()) {
                return chunkStatus;
            }
        }

        return null;
    }

    @Nullable
    public ChunkAccess getLastAvailable() {
        for(int i = CHUNK_STATUSES.size() - 1; i >= 0; --i) {
            ChunkStatus chunkStatus = CHUNK_STATUSES.get(i);
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.getFutureIfPresentUnchecked(chunkStatus);
            if (!completableFuture.isCompletedExceptionally()) {
                Optional<ChunkAccess> optional = completableFuture.getNow(UNLOADED_CHUNK).left();
                if (optional.isPresent()) {
                    return optional.get();
                }
            }
        }

        return null;
    }

    public CompletableFuture<ChunkAccess> getChunkToSave() {
        return this.chunkToSave;
    }

    public void blockChanged(BlockPos pos) {
        LevelChunk levelChunk = this.getTickingChunk();
        if (levelChunk != null) {
            int i = this.levelHeightAccessor.getSectionIndex(pos.getY());
            if (this.changedBlocksPerSection[i] == null) {
                this.hasChangedSections = true;
                this.changedBlocksPerSection[i] = new ShortOpenHashSet();
            }

            this.changedBlocksPerSection[i].add(SectionPos.sectionRelativePos(pos));
        }
    }

    public void sectionLightChanged(LightLayer lightType, int y) {
        LevelChunk levelChunk = this.getFullChunk();
        if (levelChunk != null) {
            levelChunk.setUnsaved(true);
            LevelChunk levelChunk2 = this.getTickingChunk();
            if (levelChunk2 != null) {
                int i = this.lightEngine.getMinLightSection();
                int j = this.lightEngine.getMaxLightSection();
                if (y >= i && y <= j) {
                    int k = y - i;
                    if (lightType == LightLayer.SKY) {
                        this.skyChangedLightSectionFilter.set(k);
                    } else {
                        this.blockChangedLightSectionFilter.set(k);
                    }

                }
            }
        }
    }

    public void broadcastChanges(LevelChunk chunk) {
        if (this.hasChangedSections || !this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
            Level level = chunk.getLevel();
            int i = 0;

            for(int j = 0; j < this.changedBlocksPerSection.length; ++j) {
                i += this.changedBlocksPerSection[j] != null ? this.changedBlocksPerSection[j].size() : 0;
            }

            this.resendLight |= i >= 64;
            if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
                this.broadcast(new ClientboundLightUpdatePacket(chunk.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter, true), !this.resendLight);
                this.skyChangedLightSectionFilter.clear();
                this.blockChangedLightSectionFilter.clear();
            }

            for(int k = 0; k < this.changedBlocksPerSection.length; ++k) {
                ShortSet shortSet = this.changedBlocksPerSection[k];
                if (shortSet != null) {
                    int l = this.levelHeightAccessor.getSectionYFromSectionIndex(k);
                    SectionPos sectionPos = SectionPos.of(chunk.getPos(), l);
                    if (shortSet.size() == 1) {
                        BlockPos blockPos = sectionPos.relativeToBlockPos(shortSet.iterator().nextShort());
                        BlockState blockState = level.getBlockState(blockPos);
                        this.broadcast(new ClientboundBlockUpdatePacket(blockPos, blockState), false);
                        this.broadcastBlockEntityIfNeeded(level, blockPos, blockState);
                    } else {
                        LevelChunkSection levelChunkSection = chunk.getSection(k);
                        ClientboundSectionBlocksUpdatePacket clientboundSectionBlocksUpdatePacket = new ClientboundSectionBlocksUpdatePacket(sectionPos, shortSet, levelChunkSection, this.resendLight);
                        this.broadcast(clientboundSectionBlocksUpdatePacket, false);
                        clientboundSectionBlocksUpdatePacket.runUpdates((pos, state) -> {
                            this.broadcastBlockEntityIfNeeded(level, pos, state);
                        });
                    }

                    this.changedBlocksPerSection[k] = null;
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
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            Packet<?> packet = blockEntity.getUpdatePacket();
            if (packet != null) {
                this.broadcast(packet, false);
            }
        }

    }

    public void broadcast(Packet<?> packet, boolean onlyOnWatchDistanceEdge) {
        this.playerProvider.getPlayers(this.pos, onlyOnWatchDistanceEdge).forEach((serverPlayer) -> {
            serverPlayer.connection.send(packet);
        });
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getOrScheduleFuture(ChunkStatus targetStatus, ChunkMap chunkStorage) {
        int i = targetStatus.getIndex();
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.futures.get(i);
        if (completableFuture != null) {
            Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = completableFuture.getNow(NOT_DONE_YET);
            if (either == null) {
                String string = "value in future for status: " + targetStatus + " was incorrectly set to null at chunk: " + this.pos;
                throw chunkStorage.debugFuturesAndCreateReportedException(new IllegalStateException("null value previously set for chunk status"), string);
            }

            if (either == NOT_DONE_YET || either.right().isEmpty()) {
                return completableFuture;
            }
        }

        if (getStatus(this.ticketLevel).isOrAfter(targetStatus)) {
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture2 = chunkStorage.schedule(this, targetStatus);
            this.updateChunkToSave(completableFuture2, "schedule " + targetStatus);
            this.futures.set(i, completableFuture2);
            return completableFuture2;
        } else {
            return completableFuture == null ? UNLOADED_CHUNK_FUTURE : completableFuture;
        }
    }

    protected void addSaveDependency(String thenDesc, CompletableFuture<?> then) {
        if (this.chunkToSaveHistory != null) {
            this.chunkToSaveHistory.push(new ChunkHolder.ChunkSaveDebug(Thread.currentThread(), then, thenDesc));
        }

        this.chunkToSave = this.chunkToSave.thenCombine(then, (chunkAccess, object) -> {
            return chunkAccess;
        });
    }

    private void updateChunkToSave(CompletableFuture<? extends Either<? extends ChunkAccess, ChunkHolder.ChunkLoadingFailure>> then, String thenDesc) {
        if (this.chunkToSaveHistory != null) {
            this.chunkToSaveHistory.push(new ChunkHolder.ChunkSaveDebug(Thread.currentThread(), then, thenDesc));
        }

        this.chunkToSave = this.chunkToSave.thenCombine(then, (chunkAccess, either) -> {
            return either.map((chunkAccessx) -> {
                return chunkAccessx;
            }, (chunkLoadingFailure) -> {
                return chunkAccess;
            });
        });
    }

    public ChunkHolder.FullChunkStatus getFullStatus() {
        return getFullChunkStatus(this.ticketLevel);
    }

    public ChunkPos getPos() {
        return this.pos;
    }

    public int getTicketLevel() {
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

    private void scheduleFullChunkPromotion(ChunkMap chunkMap, CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> completableFuture, Executor executor, ChunkHolder.FullChunkStatus fullChunkStatus) {
        this.pendingFullStateConfirmation.cancel(false);
        CompletableFuture<Void> completableFuture2 = new CompletableFuture<>();
        completableFuture2.thenRunAsync(() -> {
            chunkMap.onFullChunkStatusChange(this.pos, fullChunkStatus);
        }, executor);
        this.pendingFullStateConfirmation = completableFuture2;
        completableFuture.thenAccept((either) -> {
            either.ifLeft((levelChunk) -> {
                completableFuture2.complete((Void)null);
            });
        });
    }

    private void demoteFullChunk(ChunkMap chunkMap, ChunkHolder.FullChunkStatus fullChunkStatus) {
        this.pendingFullStateConfirmation.cancel(false);
        chunkMap.onFullChunkStatusChange(this.pos, fullChunkStatus);
    }

    protected void updateFutures(ChunkMap chunkStorage, Executor executor) {
        ChunkStatus chunkStatus = getStatus(this.oldTicketLevel);
        ChunkStatus chunkStatus2 = getStatus(this.ticketLevel);
        boolean bl = this.oldTicketLevel <= ChunkMap.MAX_CHUNK_DISTANCE;
        boolean bl2 = this.ticketLevel <= ChunkMap.MAX_CHUNK_DISTANCE;
        ChunkHolder.FullChunkStatus fullChunkStatus = getFullChunkStatus(this.oldTicketLevel);
        ChunkHolder.FullChunkStatus fullChunkStatus2 = getFullChunkStatus(this.ticketLevel);
        if (bl) {
            Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = Either.right(new ChunkHolder.ChunkLoadingFailure() {
                @Override
                public String toString() {
                    return "Unloaded ticket level " + ChunkHolder.this.pos;
                }
            });

            for(int i = bl2 ? chunkStatus2.getIndex() + 1 : 0; i <= chunkStatus.getIndex(); ++i) {
                CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.futures.get(i);
                if (completableFuture == null) {
                    this.futures.set(i, CompletableFuture.completedFuture(either));
                }
            }
        }

        boolean bl3 = fullChunkStatus.isOrAfter(ChunkHolder.FullChunkStatus.BORDER);
        boolean bl4 = fullChunkStatus2.isOrAfter(ChunkHolder.FullChunkStatus.BORDER);
        this.wasAccessibleSinceLastSave |= bl4;
        if (!bl3 && bl4) {
            this.fullChunkFuture = chunkStorage.prepareAccessibleChunk(this);
            this.scheduleFullChunkPromotion(chunkStorage, this.fullChunkFuture, executor, ChunkHolder.FullChunkStatus.BORDER);
            this.updateChunkToSave(this.fullChunkFuture, "full");
        }

        if (bl3 && !bl4) {
            this.fullChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        boolean bl5 = fullChunkStatus.isOrAfter(ChunkHolder.FullChunkStatus.TICKING);
        boolean bl6 = fullChunkStatus2.isOrAfter(ChunkHolder.FullChunkStatus.TICKING);
        if (!bl5 && bl6) {
            this.tickingChunkFuture = chunkStorage.prepareTickingChunk(this);
            this.scheduleFullChunkPromotion(chunkStorage, this.tickingChunkFuture, executor, ChunkHolder.FullChunkStatus.TICKING);
            this.updateChunkToSave(this.tickingChunkFuture, "ticking");
        }

        if (bl5 && !bl6) {
            this.tickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        boolean bl7 = fullChunkStatus.isOrAfter(ChunkHolder.FullChunkStatus.ENTITY_TICKING);
        boolean bl8 = fullChunkStatus2.isOrAfter(ChunkHolder.FullChunkStatus.ENTITY_TICKING);
        if (!bl7 && bl8) {
            if (this.entityTickingChunkFuture != UNLOADED_LEVEL_CHUNK_FUTURE) {
                throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
            }

            this.entityTickingChunkFuture = chunkStorage.prepareEntityTickingChunk(this.pos);
            this.scheduleFullChunkPromotion(chunkStorage, this.entityTickingChunkFuture, executor, ChunkHolder.FullChunkStatus.ENTITY_TICKING);
            this.updateChunkToSave(this.entityTickingChunkFuture, "entity ticking");
        }

        if (bl7 && !bl8) {
            this.entityTickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        if (!fullChunkStatus2.isOrAfter(fullChunkStatus)) {
            this.demoteFullChunk(chunkStorage, fullChunkStatus2);
        }

        this.onLevelChange.onLevelChange(this.pos, this::getQueueLevel, this.ticketLevel, this::setQueueLevel);
        this.oldTicketLevel = this.ticketLevel;
    }

    public static ChunkStatus getStatus(int level) {
        return level < 33 ? ChunkStatus.FULL : ChunkStatus.getStatusAroundFullChunk(level - 33);
    }

    public static ChunkHolder.FullChunkStatus getFullChunkStatus(int distance) {
        return FULL_CHUNK_STATUSES[Mth.clamp(33 - distance + 1, 0, FULL_CHUNK_STATUSES.length - 1)];
    }

    public boolean wasAccessibleSinceLastSave() {
        return this.wasAccessibleSinceLastSave;
    }

    public void refreshAccessibility() {
        this.wasAccessibleSinceLastSave = getFullChunkStatus(this.ticketLevel).isOrAfter(ChunkHolder.FullChunkStatus.BORDER);
    }

    public void replaceProtoChunk(ImposterProtoChunk chunk) {
        for(int i = 0; i < this.futures.length(); ++i) {
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.futures.get(i);
            if (completableFuture != null) {
                Optional<ChunkAccess> optional = completableFuture.getNow(UNLOADED_CHUNK).left();
                if (!optional.isEmpty() && optional.get() instanceof ProtoChunk) {
                    this.futures.set(i, CompletableFuture.completedFuture(Either.left(chunk)));
                }
            }
        }

        this.updateChunkToSave(CompletableFuture.completedFuture(Either.left(chunk.getWrapped())), "replaceProto");
    }

    public List<Pair<ChunkStatus, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>>> getAllFutures() {
        List<Pair<ChunkStatus, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>>> list = new ArrayList<>();

        for(int i = 0; i < CHUNK_STATUSES.size(); ++i) {
            list.add(Pair.of(CHUNK_STATUSES.get(i), this.futures.get(i)));
        }

        return list;
    }

    public interface ChunkLoadingFailure {
        ChunkHolder.ChunkLoadingFailure UNLOADED = new ChunkHolder.ChunkLoadingFailure() {
            @Override
            public String toString() {
                return "UNLOADED";
            }
        };
    }

    static final class ChunkSaveDebug {
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
        INACCESSIBLE,
        BORDER,
        TICKING,
        ENTITY_TICKING;

        public boolean isOrAfter(ChunkHolder.FullChunkStatus levelType) {
            return this.ordinal() >= levelType.ordinal();
        }
    }

    @FunctionalInterface
    public interface LevelChangeListener {
        void onLevelChange(ChunkPos pos, IntSupplier levelGetter, int targetLevel, IntConsumer levelSetter);
    }

    public interface PlayerProvider {
        List<ServerPlayer> getPlayers(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge);
    }
}
