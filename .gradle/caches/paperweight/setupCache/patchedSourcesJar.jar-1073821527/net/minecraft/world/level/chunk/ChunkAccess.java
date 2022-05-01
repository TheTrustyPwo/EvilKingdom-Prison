package net.minecraft.world.level.chunk;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.SerializableTickContainer;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public abstract class ChunkAccess implements BlockGetter, BiomeManager.NoiseBiomeSource, FeatureAccess {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LongSet EMPTY_REFERENCE_SET = new LongOpenHashSet();
    protected final ShortList[] postProcessing;
    protected volatile boolean unsaved;
    private volatile boolean isLightCorrect;
    protected final ChunkPos chunkPos;
    private long inhabitedTime;
    /** @deprecated */
    @Nullable
    @Deprecated
    private Holder<Biome> carverBiome;
    @Nullable
    protected NoiseChunk noiseChunk;
    protected final UpgradeData upgradeData;
    @Nullable
    protected BlendingData blendingData;
    public final Map<Heightmap.Types, Heightmap> heightmaps = Maps.newEnumMap(Heightmap.Types.class);
    private final Map<ConfiguredStructureFeature<?, ?>, StructureStart> structureStarts = Maps.newHashMap();
    private final Map<ConfiguredStructureFeature<?, ?>, LongSet> structuresRefences = Maps.newHashMap();
    protected final Map<BlockPos, CompoundTag> pendingBlockEntities = Maps.newHashMap();
    public final Map<BlockPos, BlockEntity> blockEntities = Maps.newHashMap();
    protected final LevelHeightAccessor levelHeightAccessor;
    protected final LevelChunkSection[] sections;

    public ChunkAccess(ChunkPos pos, UpgradeData upgradeData, LevelHeightAccessor heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable LevelChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
        this.chunkPos = pos;
        this.upgradeData = upgradeData;
        this.levelHeightAccessor = heightLimitView;
        this.sections = new LevelChunkSection[heightLimitView.getSectionsCount()];
        this.inhabitedTime = inhabitedTime;
        this.postProcessing = new ShortList[heightLimitView.getSectionsCount()];
        this.blendingData = blendingData;
        if (sectionArrayInitializer != null) {
            if (this.sections.length == sectionArrayInitializer.length) {
                System.arraycopy(sectionArrayInitializer, 0, this.sections, 0, this.sections.length);
            } else {
                LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", sectionArrayInitializer.length, this.sections.length);
            }
        }

        replaceMissingSections(heightLimitView, biome, this.sections);
    }

    private static void replaceMissingSections(LevelHeightAccessor world, Registry<Biome> biome, LevelChunkSection[] sectionArray) {
        for(int i = 0; i < sectionArray.length; ++i) {
            if (sectionArray[i] == null) {
                sectionArray[i] = new LevelChunkSection(world.getSectionYFromSectionIndex(i), biome);
            }
        }

    }

    public GameEventDispatcher getEventDispatcher(int ySectionCoord) {
        return GameEventDispatcher.NOOP;
    }

    @Nullable
    public abstract BlockState setBlockState(BlockPos pos, BlockState state, boolean moved);

    public abstract void setBlockEntity(BlockEntity blockEntity);

    public abstract void addEntity(Entity entity);

    @Nullable
    public LevelChunkSection getHighestSection() {
        LevelChunkSection[] levelChunkSections = this.getSections();

        for(int i = levelChunkSections.length - 1; i >= 0; --i) {
            LevelChunkSection levelChunkSection = levelChunkSections[i];
            if (!levelChunkSection.hasOnlyAir()) {
                return levelChunkSection;
            }
        }

        return null;
    }

    public int getHighestSectionPosition() {
        LevelChunkSection levelChunkSection = this.getHighestSection();
        return levelChunkSection == null ? this.getMinBuildHeight() : levelChunkSection.bottomBlockY();
    }

    public Set<BlockPos> getBlockEntitiesPos() {
        Set<BlockPos> set = Sets.newHashSet(this.pendingBlockEntities.keySet());
        set.addAll(this.blockEntities.keySet());
        return set;
    }

    public LevelChunkSection[] getSections() {
        return this.sections;
    }

    public LevelChunkSection getSection(int yIndex) {
        return this.getSections()[yIndex];
    }

    public Collection<Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return Collections.unmodifiableSet(this.heightmaps.entrySet());
    }

    public void setHeightmap(Heightmap.Types type, long[] heightmap) {
        this.getOrCreateHeightmapUnprimed(type).setRawData(this, type, heightmap);
    }

    public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return this.heightmaps.computeIfAbsent(type, (types) -> {
            return new Heightmap(this, types);
        });
    }

    public boolean hasPrimedHeightmap(Heightmap.Types type) {
        return this.heightmaps.get(type) != null;
    }

    public int getHeight(Heightmap.Types type, int x, int z) {
        Heightmap heightmap = this.heightmaps.get(type);
        if (heightmap == null) {
            if (SharedConstants.IS_RUNNING_IN_IDE && this instanceof LevelChunk) {
                LOGGER.error("Unprimed heightmap: " + type + " " + x + " " + z);
            }

            Heightmap.primeHeightmaps(this, EnumSet.of(type));
            heightmap = this.heightmaps.get(type);
        }

        return heightmap.getFirstAvailable(x & 15, z & 15) - 1;
    }

    public ChunkPos getPos() {
        return this.chunkPos;
    }

    @Nullable
    @Override
    public StructureStart getStartForFeature(ConfiguredStructureFeature<?, ?> configuredStructureFeature) {
        return this.structureStarts.get(configuredStructureFeature);
    }

    @Override
    public void setStartForFeature(ConfiguredStructureFeature<?, ?> configuredStructureFeature, StructureStart start) {
        this.structureStarts.put(configuredStructureFeature, start);
        this.unsaved = true;
    }

    public Map<ConfiguredStructureFeature<?, ?>, StructureStart> getAllStarts() {
        return Collections.unmodifiableMap(this.structureStarts);
    }

    public void setAllStarts(Map<ConfiguredStructureFeature<?, ?>, StructureStart> structureStarts) {
        this.structureStarts.clear();
        this.structureStarts.putAll(structureStarts);
        this.unsaved = true;
    }

    @Override
    public LongSet getReferencesForFeature(ConfiguredStructureFeature<?, ?> configuredStructureFeature) {
        return this.structuresRefences.getOrDefault(configuredStructureFeature, EMPTY_REFERENCE_SET);
    }

    @Override
    public void addReferenceForFeature(ConfiguredStructureFeature<?, ?> configuredStructureFeature, long reference) {
        this.structuresRefences.computeIfAbsent(configuredStructureFeature, (configuredStructureFeaturex) -> {
            return new LongOpenHashSet();
        }).add(reference);
        this.unsaved = true;
    }

    @Override
    public Map<ConfiguredStructureFeature<?, ?>, LongSet> getAllReferences() {
        return Collections.unmodifiableMap(this.structuresRefences);
    }

    @Override
    public void setAllReferences(Map<ConfiguredStructureFeature<?, ?>, LongSet> structureReferences) {
        this.structuresRefences.clear();
        this.structuresRefences.putAll(structureReferences);
        this.unsaved = true;
    }

    public boolean isYSpaceEmpty(int lowerHeight, int upperHeight) {
        if (lowerHeight < this.getMinBuildHeight()) {
            lowerHeight = this.getMinBuildHeight();
        }

        if (upperHeight >= this.getMaxBuildHeight()) {
            upperHeight = this.getMaxBuildHeight() - 1;
        }

        for(int i = lowerHeight; i <= upperHeight; i += 16) {
            if (!this.getSection(this.getSectionIndex(i)).hasOnlyAir()) {
                return false;
            }
        }

        return true;
    }

    public void setUnsaved(boolean needsSaving) {
        this.unsaved = needsSaving;
    }

    public boolean isUnsaved() {
        return this.unsaved;
    }

    public abstract ChunkStatus getStatus();

    public abstract void removeBlockEntity(BlockPos pos);

    public void markPosForPostprocessing(BlockPos pos) {
        LOGGER.warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", (Object)pos);
    }

    public ShortList[] getPostProcessing() {
        return this.postProcessing;
    }

    public void addPackedPostProcess(short packedPos, int index) {
        getOrCreateOffsetList(this.getPostProcessing(), index).add(packedPos);
    }

    public void setBlockEntityNbt(CompoundTag nbt) {
        this.pendingBlockEntities.put(BlockEntity.getPosFromTag(nbt), nbt);
    }

    @Nullable
    public CompoundTag getBlockEntityNbt(BlockPos pos) {
        return this.pendingBlockEntities.get(pos);
    }

    @Nullable
    public abstract CompoundTag getBlockEntityNbtForSaving(BlockPos pos);

    public abstract Stream<BlockPos> getLights();

    public abstract TickContainerAccess<Block> getBlockTicks();

    public abstract TickContainerAccess<Fluid> getFluidTicks();

    public abstract ChunkAccess.TicksToSave getTicksForSerialization();

    public UpgradeData getUpgradeData() {
        return this.upgradeData;
    }

    public boolean isOldNoiseGeneration() {
        return this.blendingData != null && this.blendingData.oldNoise();
    }

    @Nullable
    public BlendingData getBlendingData() {
        return this.blendingData;
    }

    public void setBlendingData(BlendingData blendingData) {
        this.blendingData = blendingData;
    }

    public long getInhabitedTime() {
        return this.inhabitedTime;
    }

    public void incrementInhabitedTime(long delta) {
        this.inhabitedTime += delta;
    }

    public void setInhabitedTime(long inhabitedTime) {
        this.inhabitedTime = inhabitedTime;
    }

    public static ShortList getOrCreateOffsetList(ShortList[] lists, int index) {
        if (lists[index] == null) {
            lists[index] = new ShortArrayList();
        }

        return lists[index];
    }

    public boolean isLightCorrect() {
        return this.isLightCorrect;
    }

    public void setLightCorrect(boolean lightOn) {
        this.isLightCorrect = lightOn;
        this.setUnsaved(true);
    }

    @Override
    public int getMinBuildHeight() {
        return this.levelHeightAccessor.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return this.levelHeightAccessor.getHeight();
    }

    public NoiseChunk getOrCreateNoiseChunk(NoiseRouter noiseColumnSampler, Supplier<DensityFunctions.BeardifierOrMarker> columnSampler, NoiseGeneratorSettings chunkGeneratorSettings, Aquifer.FluidPicker fluidLevelSampler, Blender blender) {
        if (this.noiseChunk == null) {
            this.noiseChunk = NoiseChunk.forChunk(this, noiseColumnSampler, columnSampler, chunkGeneratorSettings, fluidLevelSampler, blender);
        }

        return this.noiseChunk;
    }

    /** @deprecated */
    @Deprecated
    public Holder<Biome> carverBiome(Supplier<Holder<Biome>> biomeSupplier) {
        if (this.carverBiome == null) {
            this.carverBiome = biomeSupplier.get();
        }

        return this.carverBiome;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        try {
            int i = QuartPos.fromBlock(this.getMinBuildHeight());
            int j = i + QuartPos.fromBlock(this.getHeight()) - 1;
            int k = Mth.clamp(biomeY, i, j);
            int l = this.getSectionIndex(QuartPos.toBlock(k));
            return this.sections[l].getNoiseBiome(biomeX & 3, k & 3, biomeZ & 3);
        } catch (Throwable var8) {
            CrashReport crashReport = CrashReport.forThrowable(var8, "Getting biome");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Biome being got");
            crashReportCategory.setDetail("Location", () -> {
                return CrashReportCategory.formatLocation(this, biomeX, biomeY, biomeZ);
            });
            throw new ReportedException(crashReport);
        }
    }

    public void fillBiomesFromNoise(BiomeResolver biomeSupplier, Climate.Sampler sampler) {
        ChunkPos chunkPos = this.getPos();
        int i = QuartPos.fromBlock(chunkPos.getMinBlockX());
        int j = QuartPos.fromBlock(chunkPos.getMinBlockZ());
        LevelHeightAccessor levelHeightAccessor = this.getHeightAccessorForGeneration();

        for(int k = levelHeightAccessor.getMinSection(); k < levelHeightAccessor.getMaxSection(); ++k) {
            LevelChunkSection levelChunkSection = this.getSection(this.getSectionIndexFromSectionY(k));
            levelChunkSection.fillBiomesFromNoise(biomeSupplier, sampler, i, j);
        }

    }

    public boolean hasAnyStructureReferences() {
        return !this.getAllReferences().isEmpty();
    }

    @Nullable
    public BelowZeroRetrogen getBelowZeroRetrogen() {
        return null;
    }

    public boolean isUpgrading() {
        return this.getBelowZeroRetrogen() != null;
    }

    public LevelHeightAccessor getHeightAccessorForGeneration() {
        return this;
    }

    public static record TicksToSave(SerializableTickContainer<Block> blocks, SerializableTickContainer<Fluid> fluids) {
    }
}
