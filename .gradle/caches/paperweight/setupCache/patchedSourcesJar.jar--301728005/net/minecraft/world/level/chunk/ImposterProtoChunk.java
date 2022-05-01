package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.TickContainerAccess;

public class ImposterProtoChunk extends ProtoChunk {
    private final LevelChunk wrapped;
    private final boolean allowWrites;

    // Paper start - rewrite light engine
    @Override
    public ca.spottedleaf.starlight.common.light.SWMRNibbleArray[] getBlockNibbles() {
        return this.wrapped.getBlockNibbles();
    }

    @Override
    public void setBlockNibbles(final ca.spottedleaf.starlight.common.light.SWMRNibbleArray[] nibbles) {
        this.wrapped.setBlockNibbles(nibbles);
    }

    @Override
    public ca.spottedleaf.starlight.common.light.SWMRNibbleArray[] getSkyNibbles() {
        return this.wrapped.getSkyNibbles();
    }

    @Override
    public void setSkyNibbles(final ca.spottedleaf.starlight.common.light.SWMRNibbleArray[] nibbles) {
        this.wrapped.setSkyNibbles(nibbles);
    }

    @Override
    public boolean[] getSkyEmptinessMap() {
        return this.wrapped.getSkyEmptinessMap();
    }

    @Override
    public void setSkyEmptinessMap(final boolean[] emptinessMap) {
        this.wrapped.setSkyEmptinessMap(emptinessMap);
    }

    @Override
    public boolean[] getBlockEmptinessMap() {
        return this.wrapped.getBlockEmptinessMap();
    }

    @Override
    public void setBlockEmptinessMap(final boolean[] emptinessMap) {
        this.wrapped.setBlockEmptinessMap(emptinessMap);
    }
    // Paper end - rewrite light engine

    public ImposterProtoChunk(LevelChunk wrapped, boolean bl) {
        super(wrapped.getPos(), UpgradeData.EMPTY, wrapped.levelHeightAccessor, wrapped.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), wrapped.getBlendingData());
        this.wrapped = wrapped;
        this.allowWrites = bl;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.wrapped.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.wrapped.getBlockState(pos);
    }
    // Paper start
    @Override
    public final BlockState getBlockState(final int x, final int y, final int z) {
        return this.wrapped.getBlockStateFinal(x, y, z);
    }
    // Paper end

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.wrapped.getFluidState(pos);
    }

    @Override
    public int getMaxLightLevel() {
        return this.wrapped.getMaxLightLevel();
    }

    @Override
    public LevelChunkSection getSection(int yIndex) {
        return this.allowWrites ? this.wrapped.getSection(yIndex) : super.getSection(yIndex);
    }

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
        return this.allowWrites ? this.wrapped.setBlockState(pos, state, moved) : null;
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        if (this.allowWrites) {
            this.wrapped.setBlockEntity(blockEntity);
        }

    }

    @Override
    public void addEntity(Entity entity) {
        if (this.allowWrites) {
            this.wrapped.addEntity(entity);
        }

    }

    @Override
    public void setStatus(ChunkStatus status) {
        if (this.allowWrites) {
            super.setStatus(status);
        }

    }

    @Override
    public LevelChunkSection[] getSections() {
        return this.wrapped.getSections();
    }

    @Override
    public void setHeightmap(Heightmap.Types type, long[] heightmap) {
    }

    private Heightmap.Types fixType(Heightmap.Types type) {
        if (type == Heightmap.Types.WORLD_SURFACE_WG) {
            return Heightmap.Types.WORLD_SURFACE;
        } else {
            return type == Heightmap.Types.OCEAN_FLOOR_WG ? Heightmap.Types.OCEAN_FLOOR : type;
        }
    }

    @Override
    public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return this.wrapped.getOrCreateHeightmapUnprimed(type);
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        return this.wrapped.getHeight(this.fixType(type), x, z);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return this.wrapped.getNoiseBiome(biomeX, biomeY, biomeZ);
    }

    @Override
    public ChunkPos getPos() {
        return this.wrapped.getPos();
    }

    @Nullable
    @Override
    public StructureStart getStartForFeature(ConfiguredStructureFeature<?, ?> configuredStructureFeature) {
        return this.wrapped.getStartForFeature(configuredStructureFeature);
    }

    @Override
    public void setStartForFeature(ConfiguredStructureFeature<?, ?> configuredStructureFeature, StructureStart start) {
    }

    @Override
    public Map<ConfiguredStructureFeature<?, ?>, StructureStart> getAllStarts() {
        return this.wrapped.getAllStarts();
    }

    @Override
    public void setAllStarts(Map<ConfiguredStructureFeature<?, ?>, StructureStart> structureStarts) {
    }

    @Override
    public LongSet getReferencesForFeature(ConfiguredStructureFeature<?, ?> configuredStructureFeature) {
        return this.wrapped.getReferencesForFeature(configuredStructureFeature);
    }

    @Override
    public void addReferenceForFeature(ConfiguredStructureFeature<?, ?> configuredStructureFeature, long reference) {
    }

    @Override
    public Map<ConfiguredStructureFeature<?, ?>, LongSet> getAllReferences() {
        return this.wrapped.getAllReferences();
    }

    @Override
    public void setAllReferences(Map<ConfiguredStructureFeature<?, ?>, LongSet> structureReferences) {
    }

    @Override
    public void setUnsaved(boolean needsSaving) {
    }

    @Override
    public boolean isUnsaved() {
        return false;
    }

    @Override
    public ChunkStatus getStatus() {
        return this.wrapped.getStatus();
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
    }

    @Override
    public void markPosForPostprocessing(BlockPos pos) {
    }

    @Override
    public void setBlockEntityNbt(CompoundTag nbt) {
    }

    @Nullable
    @Override
    public CompoundTag getBlockEntityNbt(BlockPos pos) {
        return this.wrapped.getBlockEntityNbt(pos);
    }

    @Nullable
    @Override
    public CompoundTag getBlockEntityNbtForSaving(BlockPos pos) {
        return this.wrapped.getBlockEntityNbtForSaving(pos);
    }

    @Override
    public Stream<BlockPos> getLights() {
        return this.wrapped.getLights();
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.allowWrites ? this.wrapped.getBlockTicks() : BlackholeTickAccess.emptyContainer();
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.allowWrites ? this.wrapped.getFluidTicks() : BlackholeTickAccess.emptyContainer();
    }

    @Override
    public ChunkAccess.TicksToSave getTicksForSerialization() {
        return this.wrapped.getTicksForSerialization();
    }

    @Nullable
    @Override
    public BlendingData getBlendingData() {
        return this.wrapped.getBlendingData();
    }

    @Override
    public void setBlendingData(BlendingData blendingData) {
        this.wrapped.setBlendingData(blendingData);
    }

    @Override
    public CarvingMask getCarvingMask(GenerationStep.Carving carver) {
        if (this.allowWrites) {
            return super.getCarvingMask(carver);
        } else {
            throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
        }
    }

    @Override
    public CarvingMask getOrCreateCarvingMask(GenerationStep.Carving carver) {
        if (this.allowWrites) {
            return super.getOrCreateCarvingMask(carver);
        } else {
            throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
        }
    }

    public LevelChunk getWrapped() {
        return this.wrapped;
    }

    @Override
    public boolean isLightCorrect() {
        return this.wrapped.isLightCorrect();
    }

    @Override
    public void setLightCorrect(boolean lightOn) {
        this.wrapped.setLightCorrect(lightOn);
    }

    @Override
    public void fillBiomesFromNoise(BiomeResolver biomeSupplier, Climate.Sampler sampler) {
        if (this.allowWrites) {
            this.wrapped.fillBiomesFromNoise(biomeSupplier, sampler);
        }

    }
}
