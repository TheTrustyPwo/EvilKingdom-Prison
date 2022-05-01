package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.EuclideanGameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public class LevelChunk extends ChunkAccess {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity() {
        @Override
        public void tick() {
        }

        @Override
        public boolean isRemoved() {
            return true;
        }

        @Override
        public BlockPos getPos() {
            return BlockPos.ZERO;
        }

        @Override
        public String getType() {
            return "<null>";
        }
    };
    private final Map<BlockPos, LevelChunk.RebindableTickingBlockEntityWrapper> tickersInLevel = Maps.newHashMap();
    public boolean loaded;
    private boolean clientLightReady = false;
    public final Level level;
    @Nullable
    private Supplier<ChunkHolder.FullChunkStatus> fullStatus;
    @Nullable
    private LevelChunk.PostLoadProcessor postLoad;
    private final Int2ObjectMap<GameEventDispatcher> gameEventDispatcherSections;
    private final LevelChunkTicks<Block> blockTicks;
    private final LevelChunkTicks<Fluid> fluidTicks;

    public LevelChunk(Level world, ChunkPos pos) {
        this(world, pos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, (LevelChunkSection[])null, (LevelChunk.PostLoadProcessor)null, (BlendingData)null);
    }

    public LevelChunk(Level world, ChunkPos pos, UpgradeData upgradeData, LevelChunkTicks<Block> blockTickScheduler, LevelChunkTicks<Fluid> fluidTickScheduler, long inhabitedTime, @Nullable LevelChunkSection[] sectionArrayInitializer, @Nullable LevelChunk.PostLoadProcessor entityLoader, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, world, world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), inhabitedTime, sectionArrayInitializer, blendingData);
        this.level = world;
        this.gameEventDispatcherSections = new Int2ObjectOpenHashMap<>();

        for(Heightmap.Types types : Heightmap.Types.values()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(types)) {
                this.heightmaps.put(types, new Heightmap(this, types));
            }
        }

        this.postLoad = entityLoader;
        this.blockTicks = blockTickScheduler;
        this.fluidTicks = fluidTickScheduler;
    }

    public LevelChunk(ServerLevel world, ProtoChunk protoChunk, @Nullable LevelChunk.PostLoadProcessor entityLoader) {
        this(world, protoChunk.getPos(), protoChunk.getUpgradeData(), protoChunk.unpackBlockTicks(), protoChunk.unpackFluidTicks(), protoChunk.getInhabitedTime(), protoChunk.getSections(), entityLoader, protoChunk.getBlendingData());

        for(BlockEntity blockEntity : protoChunk.getBlockEntities().values()) {
            this.setBlockEntity(blockEntity);
        }

        this.pendingBlockEntities.putAll(protoChunk.getBlockEntityNbts());

        for(int i = 0; i < protoChunk.getPostProcessing().length; ++i) {
            this.postProcessing[i] = protoChunk.getPostProcessing()[i];
        }

        this.setAllStarts(protoChunk.getAllStarts());
        this.setAllReferences(protoChunk.getAllReferences());

        for(Entry<Heightmap.Types, Heightmap> entry : protoChunk.getHeightmaps()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(entry.getKey())) {
                this.setHeightmap(entry.getKey(), entry.getValue().getRawData());
            }
        }

        this.setLightCorrect(protoChunk.isLightCorrect());
        this.unsaved = true;
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public ChunkAccess.TicksToSave getTicksForSerialization() {
        return new ChunkAccess.TicksToSave(this.blockTicks, this.fluidTicks);
    }

    @Override
    public GameEventDispatcher getEventDispatcher(int ySectionCoord) {
        return this.gameEventDispatcherSections.computeIfAbsent(ySectionCoord, (sectionCoord) -> {
            return new EuclideanGameEventDispatcher(this.level);
        });
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        if (this.level.isDebug()) {
            BlockState blockState = null;
            if (j == 60) {
                blockState = Blocks.BARRIER.defaultBlockState();
            }

            if (j == 70) {
                blockState = DebugLevelSource.getBlockStateFor(i, k);
            }

            return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
        } else {
            try {
                int l = this.getSectionIndex(j);
                if (l >= 0 && l < this.sections.length) {
                    LevelChunkSection levelChunkSection = this.sections[l];
                    if (!levelChunkSection.hasOnlyAir()) {
                        return levelChunkSection.getBlockState(i & 15, j & 15, k & 15);
                    }
                }

                return Blocks.AIR.defaultBlockState();
            } catch (Throwable var8) {
                CrashReport crashReport = CrashReport.forThrowable(var8, "Getting block state");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
                crashReportCategory.setDetail("Location", () -> {
                    return CrashReportCategory.formatLocation(this, i, j, k);
                });
                throw new ReportedException(crashReport);
            }
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
    }

    public FluidState getFluidState(int x, int y, int z) {
        try {
            int i = this.getSectionIndex(y);
            if (i >= 0 && i < this.sections.length) {
                LevelChunkSection levelChunkSection = this.sections[i];
                if (!levelChunkSection.hasOnlyAir()) {
                    return levelChunkSection.getFluidState(x & 15, y & 15, z & 15);
                }
            }

            return Fluids.EMPTY.defaultFluidState();
        } catch (Throwable var7) {
            CrashReport crashReport = CrashReport.forThrowable(var7, "Getting fluid state");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
            crashReportCategory.setDetail("Location", () -> {
                return CrashReportCategory.formatLocation(this, x, y, z);
            });
            throw new ReportedException(crashReport);
        }
    }

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
        int i = pos.getY();
        LevelChunkSection levelChunkSection = this.getSection(this.getSectionIndex(i));
        boolean bl = levelChunkSection.hasOnlyAir();
        if (bl && state.isAir()) {
            return null;
        } else {
            int j = pos.getX() & 15;
            int k = i & 15;
            int l = pos.getZ() & 15;
            BlockState blockState = levelChunkSection.setBlockState(j, k, l, state);
            if (blockState == state) {
                return null;
            } else {
                Block block = state.getBlock();
                this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(j, i, l, state);
                this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(j, i, l, state);
                this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(j, i, l, state);
                this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(j, i, l, state);
                boolean bl2 = levelChunkSection.hasOnlyAir();
                if (bl != bl2) {
                    this.level.getChunkSource().getLightEngine().updateSectionStatus(pos, bl2);
                }

                boolean bl3 = blockState.hasBlockEntity();
                if (!this.level.isClientSide) {
                    blockState.onRemove(this.level, pos, state, moved);
                } else if (!blockState.is(block) && bl3) {
                    this.removeBlockEntity(pos);
                }

                if (!levelChunkSection.getBlockState(j, k, l).is(block)) {
                    return null;
                } else {
                    if (!this.level.isClientSide) {
                        state.onPlace(this.level, pos, blockState, moved);
                    }

                    if (state.hasBlockEntity()) {
                        BlockEntity blockEntity = this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
                        if (blockEntity == null) {
                            blockEntity = ((EntityBlock)block).newBlockEntity(pos, state);
                            if (blockEntity != null) {
                                this.addAndRegisterBlockEntity(blockEntity);
                            }
                        } else {
                            blockEntity.setBlockState(state);
                            this.updateBlockEntityTicker(blockEntity);
                        }
                    }

                    this.unsaved = true;
                    return blockState;
                }
            }
        }
    }

    /** @deprecated */
    @Deprecated
    @Override
    public void addEntity(Entity entity) {
    }

    @Nullable
    private BlockEntity createBlockEntity(BlockPos pos) {
        BlockState blockState = this.getBlockState(pos);
        return !blockState.hasBlockEntity() ? null : ((EntityBlock)blockState.getBlock()).newBlockEntity(pos, blockState);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos, LevelChunk.EntityCreationType creationType) {
        BlockEntity blockEntity = this.blockEntities.get(pos);
        if (blockEntity == null) {
            CompoundTag compoundTag = this.pendingBlockEntities.remove(pos);
            if (compoundTag != null) {
                BlockEntity blockEntity2 = this.promotePendingBlockEntity(pos, compoundTag);
                if (blockEntity2 != null) {
                    return blockEntity2;
                }
            }
        }

        if (blockEntity == null) {
            if (creationType == LevelChunk.EntityCreationType.IMMEDIATE) {
                blockEntity = this.createBlockEntity(pos);
                if (blockEntity != null) {
                    this.addAndRegisterBlockEntity(blockEntity);
                }
            }
        } else if (blockEntity.isRemoved()) {
            this.blockEntities.remove(pos);
            return null;
        }

        return blockEntity;
    }

    public void addAndRegisterBlockEntity(BlockEntity blockEntity) {
        this.setBlockEntity(blockEntity);
        if (this.isInLevel()) {
            this.addGameEventListener(blockEntity);
            this.updateBlockEntityTicker(blockEntity);
        }

    }

    private boolean isInLevel() {
        return this.loaded || this.level.isClientSide();
    }

    boolean isTicking(BlockPos pos) {
        if (!this.level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        } else {
            Level var3 = this.level;
            if (!(var3 instanceof ServerLevel)) {
                return true;
            } else {
                ServerLevel serverLevel = (ServerLevel)var3;
                return this.getFullStatus().isOrAfter(ChunkHolder.FullChunkStatus.TICKING) && serverLevel.areEntitiesLoaded(ChunkPos.asLong(pos));
            }
        }
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        BlockPos blockPos = blockEntity.getBlockPos();
        if (this.getBlockState(blockPos).hasBlockEntity()) {
            blockEntity.setLevel(this.level);
            blockEntity.clearRemoved();
            BlockEntity blockEntity2 = this.blockEntities.put(blockPos.immutable(), blockEntity);
            if (blockEntity2 != null && blockEntity2 != blockEntity) {
                blockEntity2.setRemoved();
            }

        }
    }

    @Nullable
    @Override
    public CompoundTag getBlockEntityNbtForSaving(BlockPos pos) {
        BlockEntity blockEntity = this.getBlockEntity(pos);
        if (blockEntity != null && !blockEntity.isRemoved()) {
            CompoundTag compoundTag = blockEntity.saveWithFullMetadata();
            compoundTag.putBoolean("keepPacked", false);
            return compoundTag;
        } else {
            CompoundTag compoundTag2 = this.pendingBlockEntities.get(pos);
            if (compoundTag2 != null) {
                compoundTag2 = compoundTag2.copy();
                compoundTag2.putBoolean("keepPacked", true);
            }

            return compoundTag2;
        }
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        if (this.isInLevel()) {
            BlockEntity blockEntity = this.blockEntities.remove(pos);
            if (blockEntity != null) {
                this.removeGameEventListener(blockEntity);
                blockEntity.setRemoved();
            }
        }

        this.removeBlockEntityTicker(pos);
    }

    private <T extends BlockEntity> void removeGameEventListener(T blockEntity) {
        if (!this.level.isClientSide) {
            Block block = blockEntity.getBlockState().getBlock();
            if (block instanceof EntityBlock) {
                GameEventListener gameEventListener = ((EntityBlock)block).getListener(this.level, blockEntity);
                if (gameEventListener != null) {
                    int i = SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY());
                    GameEventDispatcher gameEventDispatcher = this.getEventDispatcher(i);
                    gameEventDispatcher.unregister(gameEventListener);
                    if (gameEventDispatcher.isEmpty()) {
                        this.gameEventDispatcherSections.remove(i);
                    }
                }
            }

        }
    }

    private void removeBlockEntityTicker(BlockPos pos) {
        LevelChunk.RebindableTickingBlockEntityWrapper rebindableTickingBlockEntityWrapper = this.tickersInLevel.remove(pos);
        if (rebindableTickingBlockEntityWrapper != null) {
            rebindableTickingBlockEntityWrapper.rebind(NULL_TICKER);
        }

    }

    public void runPostLoad() {
        if (this.postLoad != null) {
            this.postLoad.run(this);
            this.postLoad = null;
        }

    }

    public boolean isEmpty() {
        return false;
    }

    public void replaceWithPacketData(FriendlyByteBuf buf, CompoundTag nbt, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer) {
        this.clearAllBlockEntities();

        for(LevelChunkSection levelChunkSection : this.sections) {
            levelChunkSection.read(buf);
        }

        for(Heightmap.Types types : Heightmap.Types.values()) {
            String string = types.getSerializationKey();
            if (nbt.contains(string, 12)) {
                this.setHeightmap(types, nbt.getLongArray(string));
            }
        }

        consumer.accept((pos, blockEntityType, nbtx) -> {
            BlockEntity blockEntity = this.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
            if (blockEntity != null && nbtx != null && blockEntity.getType() == blockEntityType) {
                blockEntity.load(nbtx);
            }

        });
    }

    public void setLoaded(boolean loadedToWorld) {
        this.loaded = loadedToWorld;
    }

    public Level getLevel() {
        return this.level;
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    @Override
    public Stream<BlockPos> getLights() {
        return StreamSupport.stream(BlockPos.betweenClosed(this.chunkPos.getMinBlockX(), this.getMinBuildHeight(), this.chunkPos.getMinBlockZ(), this.chunkPos.getMaxBlockX(), this.getMaxBuildHeight() - 1, this.chunkPos.getMaxBlockZ()).spliterator(), false).filter((blockPos) -> {
            return this.getBlockState(blockPos).getLightEmission() != 0;
        });
    }

    public void postProcessGeneration() {
        ChunkPos chunkPos = this.getPos();

        for(int i = 0; i < this.postProcessing.length; ++i) {
            if (this.postProcessing[i] != null) {
                for(Short short_ : this.postProcessing[i]) {
                    BlockPos blockPos = ProtoChunk.unpackOffsetCoordinates(short_, this.getSectionYFromSectionIndex(i), chunkPos);
                    BlockState blockState = this.getBlockState(blockPos);
                    FluidState fluidState = blockState.getFluidState();
                    if (!fluidState.isEmpty()) {
                        fluidState.tick(this.level, blockPos);
                    }

                    if (!(blockState.getBlock() instanceof LiquidBlock)) {
                        BlockState blockState2 = Block.updateFromNeighbourShapes(blockState, this.level, blockPos);
                        this.level.setBlock(blockPos, blockState2, 20);
                    }
                }

                this.postProcessing[i].clear();
            }
        }

        for(BlockPos blockPos2 : ImmutableList.copyOf(this.pendingBlockEntities.keySet())) {
            this.getBlockEntity(blockPos2);
        }

        this.pendingBlockEntities.clear();
        this.upgradeData.upgrade(this);
    }

    @Nullable
    private BlockEntity promotePendingBlockEntity(BlockPos pos, CompoundTag nbt) {
        BlockState blockState = this.getBlockState(pos);
        BlockEntity blockEntity;
        if ("DUMMY".equals(nbt.getString("id"))) {
            if (blockState.hasBlockEntity()) {
                blockEntity = ((EntityBlock)blockState.getBlock()).newBlockEntity(pos, blockState);
            } else {
                blockEntity = null;
                LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pos, blockState);
            }
        } else {
            blockEntity = BlockEntity.loadStatic(pos, blockState, nbt);
        }

        if (blockEntity != null) {
            blockEntity.setLevel(this.level);
            this.addAndRegisterBlockEntity(blockEntity);
        } else {
            LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", blockState, pos);
        }

        return blockEntity;
    }

    public void unpackTicks(long time) {
        this.blockTicks.unpack(time);
        this.fluidTicks.unpack(time);
    }

    public void registerTickContainerInLevel(ServerLevel world) {
        world.getBlockTicks().addContainer(this.chunkPos, this.blockTicks);
        world.getFluidTicks().addContainer(this.chunkPos, this.fluidTicks);
    }

    public void unregisterTickContainerFromLevel(ServerLevel world) {
        world.getBlockTicks().removeContainer(this.chunkPos);
        world.getFluidTicks().removeContainer(this.chunkPos);
    }

    @Override
    public ChunkStatus getStatus() {
        return ChunkStatus.FULL;
    }

    public ChunkHolder.FullChunkStatus getFullStatus() {
        return this.fullStatus == null ? ChunkHolder.FullChunkStatus.BORDER : this.fullStatus.get();
    }

    public void setFullStatus(Supplier<ChunkHolder.FullChunkStatus> levelTypeProvider) {
        this.fullStatus = levelTypeProvider;
    }

    public void clearAllBlockEntities() {
        this.blockEntities.values().forEach(BlockEntity::setRemoved);
        this.blockEntities.clear();
        this.tickersInLevel.values().forEach((ticker) -> {
            ticker.rebind(NULL_TICKER);
        });
        this.tickersInLevel.clear();
    }

    public void registerAllBlockEntitiesAfterLevelLoad() {
        this.blockEntities.values().forEach((blockEntity) -> {
            this.addGameEventListener(blockEntity);
            this.updateBlockEntityTicker(blockEntity);
        });
    }

    private <T extends BlockEntity> void addGameEventListener(T blockEntity) {
        if (!this.level.isClientSide) {
            Block block = blockEntity.getBlockState().getBlock();
            if (block instanceof EntityBlock) {
                GameEventListener gameEventListener = ((EntityBlock)block).getListener(this.level, blockEntity);
                if (gameEventListener != null) {
                    GameEventDispatcher gameEventDispatcher = this.getEventDispatcher(SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY()));
                    gameEventDispatcher.register(gameEventListener);
                }
            }

        }
    }

    private <T extends BlockEntity> void updateBlockEntityTicker(T blockEntity) {
        BlockState blockState = blockEntity.getBlockState();
        BlockEntityTicker<T> blockEntityTicker = blockState.getTicker(this.level, blockEntity.getType());
        if (blockEntityTicker == null) {
            this.removeBlockEntityTicker(blockEntity.getBlockPos());
        } else {
            this.tickersInLevel.compute(blockEntity.getBlockPos(), (pos, rebindableTickingBlockEntityWrapper) -> {
                TickingBlockEntity tickingBlockEntity = this.createTicker(blockEntity, blockEntityTicker);
                if (rebindableTickingBlockEntityWrapper != null) {
                    rebindableTickingBlockEntityWrapper.rebind(tickingBlockEntity);
                    return rebindableTickingBlockEntityWrapper;
                } else if (this.isInLevel()) {
                    LevelChunk.RebindableTickingBlockEntityWrapper rebindableTickingBlockEntityWrapper2 = new LevelChunk.RebindableTickingBlockEntityWrapper(tickingBlockEntity);
                    this.level.addBlockEntityTicker(rebindableTickingBlockEntityWrapper2);
                    return rebindableTickingBlockEntityWrapper2;
                } else {
                    return null;
                }
            });
        }

    }

    private <T extends BlockEntity> TickingBlockEntity createTicker(T blockEntity, BlockEntityTicker<T> blockEntityTicker) {
        return new LevelChunk.BoundTickingBlockEntity<>(blockEntity, blockEntityTicker);
    }

    public boolean isClientLightReady() {
        return this.clientLightReady;
    }

    public void setClientLightReady(boolean shouldRenderOnUpdate) {
        this.clientLightReady = shouldRenderOnUpdate;
    }

    class BoundTickingBlockEntity<T extends BlockEntity> implements TickingBlockEntity {
        private final T blockEntity;
        private final BlockEntityTicker<T> ticker;
        private boolean loggedInvalidBlockState;

        BoundTickingBlockEntity(T blockEntity, BlockEntityTicker<T> ticker) {
            this.blockEntity = blockEntity;
            this.ticker = ticker;
        }

        @Override
        public void tick() {
            if (!this.blockEntity.isRemoved() && this.blockEntity.hasLevel()) {
                BlockPos blockPos = this.blockEntity.getBlockPos();
                if (LevelChunk.this.isTicking(blockPos)) {
                    try {
                        ProfilerFiller profilerFiller = LevelChunk.this.level.getProfiler();
                        profilerFiller.push(this::getType);
                        BlockState blockState = LevelChunk.this.getBlockState(blockPos);
                        if (this.blockEntity.getType().isValid(blockState)) {
                            this.ticker.tick(LevelChunk.this.level, this.blockEntity.getBlockPos(), blockState, this.blockEntity);
                            this.loggedInvalidBlockState = false;
                        } else if (!this.loggedInvalidBlockState) {
                            this.loggedInvalidBlockState = true;
                            LevelChunk.LOGGER.warn("Block entity {} @ {} state {} invalid for ticking:", LogUtils.defer(this::getType), LogUtils.defer(this::getPos), blockState);
                        }

                        profilerFiller.pop();
                    } catch (Throwable var5) {
                        CrashReport crashReport = CrashReport.forThrowable(var5, "Ticking block entity");
                        CrashReportCategory crashReportCategory = crashReport.addCategory("Block entity being ticked");
                        this.blockEntity.fillCrashReportCategory(crashReportCategory);
                        throw new ReportedException(crashReport);
                    }
                }
            }

        }

        @Override
        public boolean isRemoved() {
            return this.blockEntity.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.blockEntity.getBlockPos();
        }

        @Override
        public String getType() {
            return BlockEntityType.getKey(this.blockEntity.getType()).toString();
        }

        @Override
        public String toString() {
            return "Level ticker for " + this.getType() + "@" + this.getPos();
        }
    }

    public static enum EntityCreationType {
        IMMEDIATE,
        QUEUED,
        CHECK;
    }

    @FunctionalInterface
    public interface PostLoadProcessor {
        void run(LevelChunk chunk);
    }

    class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {
        private TickingBlockEntity ticker;

        RebindableTickingBlockEntityWrapper(TickingBlockEntity wrapped) {
            this.ticker = wrapped;
        }

        void rebind(TickingBlockEntity wrapped) {
            this.ticker = wrapped;
        }

        @Override
        public void tick() {
            this.ticker.tick();
        }

        @Override
        public boolean isRemoved() {
            return this.ticker.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.ticker.getPos();
        }

        @Override
        public String getType() {
            return this.ticker.getType();
        }

        @Override
        public String toString() {
            return this.ticker.toString() + " <wrapped>";
        }
    }
}
