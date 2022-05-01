package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;

public class ProtoChunk extends ChunkAccess {
    @Nullable
    private volatile LevelLightEngine lightEngine;
    private volatile ChunkStatus status = ChunkStatus.EMPTY;
    private final List<CompoundTag> entities = Lists.newArrayList();
    private final List<BlockPos> lights = Lists.newArrayList();
    private final Map<GenerationStep.Carving, CarvingMask> carvingMasks = new Object2ObjectArrayMap<>();
    @Nullable
    private BelowZeroRetrogen belowZeroRetrogen;
    private final ProtoChunkTicks<Block> blockTicks;
    private final ProtoChunkTicks<Fluid> fluidTicks;

    public ProtoChunk(ChunkPos pos, UpgradeData upgradeData, LevelHeightAccessor world, Registry<Biome> biomeRegistry, @Nullable BlendingData blendingData) {
        this(pos, upgradeData, (LevelChunkSection[])null, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(), world, biomeRegistry, blendingData);
    }

    public ProtoChunk(ChunkPos pos, UpgradeData upgradeData, @Nullable LevelChunkSection[] sections, ProtoChunkTicks<Block> blockTickScheduler, ProtoChunkTicks<Fluid> fluidTickScheduler, LevelHeightAccessor world, Registry<Biome> biomeRegistry, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, world, biomeRegistry, 0L, sections, blendingData);
        // Paper start - rewrite light engine
        if (!(this instanceof ImposterProtoChunk)) {
            this.setBlockNibbles(ca.spottedleaf.starlight.common.light.StarLightEngine.getFilledEmptyLight(world));
            this.setSkyNibbles(ca.spottedleaf.starlight.common.light.StarLightEngine.getFilledEmptyLight(world));
        }
        // Paper end - rewrite light engine
        this.blockTicks = blockTickScheduler;
        this.fluidTicks = fluidTickScheduler;
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

    // Paper start - If loaded util
    @Override
    public final FluidState getFluidIfLoaded(BlockPos blockposition) {
        return this.getFluidState(blockposition);
    }

    @Override
    public final BlockState getBlockStateIfLoaded(BlockPos blockposition) {
        return this.getBlockState(blockposition);
    }
    // Paper end

    @Override
    public BlockState getBlockState(BlockPos pos) {
        // Paper start
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }
    public BlockState getBlockState(final int x, final int y, final int z) {
        if (this.isOutsideBuildHeight(y)) {
            return Blocks.VOID_AIR.defaultBlockState();
        } else {
            LevelChunkSection levelChunkSection = this.getSections()[this.getSectionIndex(y)];
            return levelChunkSection.hasOnlyAir() ? Blocks.AIR.defaultBlockState() : levelChunkSection.getBlockState(x & 15, y & 15, z & 15);
        }
    }
    // Paper end

    @Override
    public FluidState getFluidState(BlockPos pos) {
        int i = pos.getY();
        if (this.isOutsideBuildHeight(i)) {
            return Fluids.EMPTY.defaultFluidState();
        } else {
            LevelChunkSection levelChunkSection = this.getSection(this.getSectionIndex(i));
            return levelChunkSection.hasOnlyAir() ? Fluids.EMPTY.defaultFluidState() : levelChunkSection.getFluidState(pos.getX() & 15, i & 15, pos.getZ() & 15);
        }
    }

    @Override
    public Stream<BlockPos> getLights() {
        return this.lights.stream();
    }

    public ShortList[] getPackedLights() {
        ShortList[] shortLists = new ShortList[this.getSectionsCount()];

        for(BlockPos blockPos : this.lights) {
            ChunkAccess.getOrCreateOffsetList(shortLists, this.getSectionIndex(blockPos.getY())).add(packOffsetCoordinates(blockPos));
        }

        return shortLists;
    }

    public void addLight(short chunkSliceRel, int sectionY) {
        this.addLight(unpackOffsetCoordinates(chunkSliceRel, this.getSectionYFromSectionIndex(sectionY), this.chunkPos));
    }

    public void addLight(BlockPos pos) {
        this.lights.add(pos.immutable());
    }

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        if (j >= this.getMinBuildHeight() && j < this.getMaxBuildHeight()) {
            int l = this.getSectionIndex(j);
            if (this.sections[l].hasOnlyAir() && state.is(Blocks.AIR)) {
                return state;
            } else {
                if (state.getLightEmission() > 0) {
                    this.lights.add(new BlockPos((i & 15) + this.getPos().getMinBlockX(), j, (k & 15) + this.getPos().getMinBlockZ()));
                }

                LevelChunkSection levelChunkSection = this.getSection(l);
                BlockState blockState = levelChunkSection.setBlockState(i & 15, j & 15, k & 15, state);
                if (this.status.isOrAfter(ChunkStatus.FEATURES) && state != blockState && (state.getLightBlock(this, pos) != blockState.getLightBlock(this, pos) || state.getLightEmission() != blockState.getLightEmission() || state.useShapeForLightOcclusion() || blockState.useShapeForLightOcclusion())) {
                    this.lightEngine.checkBlock(pos);
                }

                EnumSet<Heightmap.Types> enumSet = this.getStatus().heightmapsAfter();
                EnumSet<Heightmap.Types> enumSet2 = null;

                for(Heightmap.Types types : enumSet) {
                    Heightmap heightmap = this.heightmaps.get(types);
                    if (heightmap == null) {
                        if (enumSet2 == null) {
                            enumSet2 = EnumSet.noneOf(Heightmap.Types.class);
                        }

                        enumSet2.add(types);
                    }
                }

                if (enumSet2 != null) {
                    Heightmap.primeHeightmaps(this, enumSet2);
                }

                for(Heightmap.Types types2 : enumSet) {
                    this.heightmaps.get(types2).update(i & 15, j, k & 15, state);
                }

                return blockState;
            }
        } else {
            return Blocks.VOID_AIR.defaultBlockState();
        }
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        this.blockEntities.put(blockEntity.getBlockPos(), blockEntity);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.blockEntities.get(pos);
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void addEntity(CompoundTag entityNbt) {
        this.entities.add(entityNbt);
    }

    @Override
    public void addEntity(Entity entity) {
        if (!entity.isPassenger()) {
            CompoundTag compoundTag = new CompoundTag();
            entity.save(compoundTag);
            this.addEntity(compoundTag);
        }
    }

    @Override
    public void setStartForFeature(ConfiguredStructureFeature<?, ?> configuredStructureFeature, StructureStart start) {
        BelowZeroRetrogen belowZeroRetrogen = this.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null && start.isValid()) {
            BoundingBox boundingBox = start.getBoundingBox();
            LevelHeightAccessor levelHeightAccessor = this.getHeightAccessorForGeneration();
            if (boundingBox.minY() < levelHeightAccessor.getMinBuildHeight() || boundingBox.maxY() >= levelHeightAccessor.getMaxBuildHeight()) {
                return;
            }
        }

        super.setStartForFeature(configuredStructureFeature, start);
    }

    public List<CompoundTag> getEntities() {
        return this.entities;
    }

    @Override
    public ChunkStatus getStatus() {
        return this.status;
    }

    public void setStatus(ChunkStatus status) {
        this.status = status;
        if (this.belowZeroRetrogen != null && status.isOrAfter(this.belowZeroRetrogen.targetStatus())) {
            this.setBelowZeroRetrogen((BelowZeroRetrogen)null);
        }

        this.setUnsaved(true);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        if (!this.getStatus().isOrAfter(ChunkStatus.BIOMES) && (this.belowZeroRetrogen == null || !this.belowZeroRetrogen.targetStatus().isOrAfter(ChunkStatus.BIOMES))) {
            throw new IllegalStateException("Asking for biomes before we have biomes");
        } else {
            return super.getNoiseBiome(biomeX, biomeY, biomeZ);
        }
    }

    public static short packOffsetCoordinates(BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        int l = i & 15;
        int m = j & 15;
        int n = k & 15;
        return (short)(l | m << 4 | n << 8);
    }

    public static BlockPos unpackOffsetCoordinates(short sectionRel, int sectionY, ChunkPos chunkPos) {
        int i = SectionPos.sectionToBlockCoord(chunkPos.x, sectionRel & 15);
        int j = SectionPos.sectionToBlockCoord(sectionY, sectionRel >>> 4 & 15);
        int k = SectionPos.sectionToBlockCoord(chunkPos.z, sectionRel >>> 8 & 15);
        return new BlockPos(i, j, k);
    }

    @Override
    public void markPosForPostprocessing(BlockPos pos) {
        if (!this.isOutsideBuildHeight(pos)) {
            ChunkAccess.getOrCreateOffsetList(this.postProcessing, this.getSectionIndex(pos.getY())).add(packOffsetCoordinates(pos));
        }

    }

    @Override
    public void addPackedPostProcess(short packedPos, int index) {
        ChunkAccess.getOrCreateOffsetList(this.postProcessing, index).add(packedPos);
    }

    public Map<BlockPos, CompoundTag> getBlockEntityNbts() {
        return Collections.unmodifiableMap(this.pendingBlockEntities);
    }

    @Nullable
    @Override
    public CompoundTag getBlockEntityNbtForSaving(BlockPos pos) {
        BlockEntity blockEntity = this.getBlockEntity(pos);
        return blockEntity != null ? blockEntity.saveWithFullMetadata() : this.pendingBlockEntities.get(pos);
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        this.blockEntities.remove(pos);
        this.pendingBlockEntities.remove(pos);
    }

    @Nullable
    public CarvingMask getCarvingMask(GenerationStep.Carving carver) {
        return this.carvingMasks.get(carver);
    }

    public CarvingMask getOrCreateCarvingMask(GenerationStep.Carving carver) {
        return this.carvingMasks.computeIfAbsent(carver, (carving) -> {
            return new CarvingMask(this.getHeight(), this.getMinBuildHeight());
        });
    }

    public void setCarvingMask(GenerationStep.Carving carver, CarvingMask carvingMask) {
        this.carvingMasks.put(carver, carvingMask);
    }

    public void setLightEngine(LevelLightEngine lightingProvider) {
        this.lightEngine = lightingProvider;
    }

    public void setBelowZeroRetrogen(@Nullable BelowZeroRetrogen belowZeroRetrogen) {
        this.belowZeroRetrogen = belowZeroRetrogen;
    }

    @Nullable
    @Override
    public BelowZeroRetrogen getBelowZeroRetrogen() {
        return this.belowZeroRetrogen;
    }

    private static <T> LevelChunkTicks<T> unpackTicks(ProtoChunkTicks<T> tickScheduler) {
        return new LevelChunkTicks<>(tickScheduler.scheduledTicks());
    }

    public LevelChunkTicks<Block> unpackBlockTicks() {
        return unpackTicks(this.blockTicks);
    }

    public LevelChunkTicks<Fluid> unpackFluidTicks() {
        return unpackTicks(this.fluidTicks);
    }

    @Override
    public LevelHeightAccessor getHeightAccessorForGeneration() {
        return (LevelHeightAccessor)(this.isUpgrading() ? BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR : this);
    }
}
