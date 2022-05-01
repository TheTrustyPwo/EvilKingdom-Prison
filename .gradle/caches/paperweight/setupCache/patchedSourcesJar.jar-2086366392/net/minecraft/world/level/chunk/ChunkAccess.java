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
import java.util.Map.Entry;
import java.util.Set;
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
    protected final ChunkPos chunkPos; public final long coordinateKey; public final int locX; public final int locZ; // Paper - cache coordinate key
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

    // CraftBukkit start - SPIGOT-6814: move to IChunkAccess to account for 1.17 to 1.18 chunk upgrading.
    private static final org.bukkit.craftbukkit.v1_18_R2.persistence.CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new org.bukkit.craftbukkit.v1_18_R2.persistence.CraftPersistentDataTypeRegistry();
    public org.bukkit.craftbukkit.v1_18_R2.persistence.DirtyCraftPersistentDataContainer persistentDataContainer = new org.bukkit.craftbukkit.v1_18_R2.persistence.DirtyCraftPersistentDataContainer(ChunkAccess.DATA_TYPE_REGISTRY);
    // CraftBukkit end
    // Paper start - rewrite light engine
    private volatile ca.spottedleaf.starlight.common.light.SWMRNibbleArray[] blockNibbles;

    private volatile ca.spottedleaf.starlight.common.light.SWMRNibbleArray[] skyNibbles;

    private volatile boolean[] skyEmptinessMap;

    private volatile boolean[] blockEmptinessMap;

    public ca.spottedleaf.starlight.common.light.SWMRNibbleArray[] getBlockNibbles() {
        return this.blockNibbles;
    }

    public void setBlockNibbles(final ca.spottedleaf.starlight.common.light.SWMRNibbleArray[] nibbles) {
        this.blockNibbles = nibbles;
    }

    public ca.spottedleaf.starlight.common.light.SWMRNibbleArray[] getSkyNibbles() {
        return this.skyNibbles;
    }

    public void setSkyNibbles(final ca.spottedleaf.starlight.common.light.SWMRNibbleArray[] nibbles) {
        this.skyNibbles = nibbles;
    }

    public boolean[] getSkyEmptinessMap() {
        return this.skyEmptinessMap;
    }

    public void setSkyEmptinessMap(final boolean[] emptinessMap) {
        this.skyEmptinessMap = emptinessMap;
    }

    public boolean[] getBlockEmptinessMap() {
        return this.blockEmptinessMap;
    }

    public void setBlockEmptinessMap(final boolean[] emptinessMap) {
        this.blockEmptinessMap = emptinessMap;
    }
    // Paper end - rewrite light engine

    public ChunkAccess(ChunkPos pos, UpgradeData upgradeData, LevelHeightAccessor heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable LevelChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
        this.locX = pos.x; this.locZ = pos.z; // Paper - reduce need for field lookups
        this.chunkPos = pos; this.coordinateKey = ChunkPos.asLong(locX, locZ); // Paper - cache long key
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
                ChunkAccess.LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", sectionArrayInitializer.length, this.sections.length);
            }
        }

        ChunkAccess.replaceMissingSections(heightLimitView, biome, this.sections);
        // CraftBukkit start
        this.biomeRegistry = biome;
    }
    public final Registry<Biome> biomeRegistry;
    // CraftBukkit end

    private static void replaceMissingSections(LevelHeightAccessor world, Registry<Biome> biome, LevelChunkSection[] sectionArray) {
        for (int i = 0; i < sectionArray.length; ++i) {
            if (sectionArray[i] == null) {
                sectionArray[i] = new LevelChunkSection(world.getSectionYFromSectionIndex(i), biome, null, world instanceof net.minecraft.world.level.Level ? (net.minecraft.world.level.Level) world : null); // Paper - Anti-Xray - Add parameters
            }
        }

    }

    public GameEventDispatcher getEventDispatcher(int ySectionCoord) {
        return GameEventDispatcher.NOOP;
    }

    public abstract BlockState getBlockState(final int x, final int y, final int z); // Paper
    @Nullable
    public abstract BlockState setBlockState(BlockPos pos, BlockState state, boolean moved);

    public abstract void setBlockEntity(BlockEntity blockEntity);

    public abstract void addEntity(Entity entity);

    @Nullable
    public LevelChunkSection getHighestSection() {
        LevelChunkSection[] achunksection = this.getSections();

        for (int i = achunksection.length - 1; i >= 0; --i) {
            LevelChunkSection chunksection = achunksection[i];

            if (!chunksection.hasOnlyAir()) {
                return chunksection;
            }
        }

        return null;
    }

    public int getHighestSectionPosition() {
        LevelChunkSection chunksection = this.getHighestSection();

        return chunksection == null ? this.getMinBuildHeight() : chunksection.bottomBlockY();
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
        return (Heightmap) this.heightmaps.computeIfAbsent(type, (heightmap_type1) -> {
            return new Heightmap(this, heightmap_type1);
        });
    }

    public boolean hasPrimedHeightmap(Heightmap.Types type) {
        return this.heightmaps.get(type) != null;
    }

    public int getHeight(Heightmap.Types type, int x, int z) {
        Heightmap heightmap = (Heightmap) this.heightmaps.get(type);

        if (heightmap == null) {
            if (SharedConstants.IS_RUNNING_IN_IDE && this instanceof LevelChunk) {
                ChunkAccess.LOGGER.error("Unprimed heightmap: " + type + " " + x + " " + z);
            }

            Heightmap.primeHeightmaps(this, EnumSet.of(type));
            heightmap = (Heightmap) this.heightmaps.get(type);
        }

        return heightmap.getFirstAvailable(x & 15, z & 15) - 1;
    }

    public ChunkPos getPos() {
        return this.chunkPos;
    }

    @Nullable
    @Override
    public StructureStart getStartForFeature(ConfiguredStructureFeature<?, ?> structurefeature) {
        return (StructureStart) this.structureStarts.get(structurefeature);
    }

    @Override
    public void setStartForFeature(ConfiguredStructureFeature<?, ?> structurefeature, StructureStart start) {
        this.structureStarts.put(structurefeature, start);
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
    public LongSet getReferencesForFeature(ConfiguredStructureFeature<?, ?> structurefeature) {
        return (LongSet) this.structuresRefences.getOrDefault(structurefeature, ChunkAccess.EMPTY_REFERENCE_SET);
    }

    @Override
    public void addReferenceForFeature(ConfiguredStructureFeature<?, ?> structurefeature, long reference) {
        ((LongSet) this.structuresRefences.computeIfAbsent(structurefeature, (structurefeature1) -> {
            return new LongOpenHashSet();
        })).add(reference);
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

        for (int k = lowerHeight; k <= upperHeight; k += 16) {
            if (!this.getSection(this.getSectionIndex(k)).hasOnlyAir()) {
                return false;
            }
        }

        return true;
    }

    public void setUnsaved(boolean needsSaving) {
        this.unsaved = needsSaving;
        if (!needsSaving) this.persistentDataContainer.dirty(false); // CraftBukkit - SPIGOT-6814: chunk was saved, pdc is no longer dirty
    }

    public boolean isUnsaved() {
        return this.unsaved || this.persistentDataContainer.dirty(); // CraftBukkit - SPIGOT-6814: chunk is unsaved if pdc was mutated
    }

    public abstract ChunkStatus getStatus();

    public abstract void removeBlockEntity(BlockPos pos);

    public void markPosForPostprocessing(BlockPos pos) {
        ChunkAccess.LOGGER.warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", pos);
    }

    public ShortList[] getPostProcessing() {
        return this.postProcessing;
    }

    public void addPackedPostProcess(short packedPos, int index) {
        ChunkAccess.getOrCreateOffsetList(this.getPostProcessing(), index).add(packedPos);
    }

    public void setBlockEntityNbt(CompoundTag nbt) {
        this.pendingBlockEntities.put(BlockEntity.getPosFromTag(nbt), nbt);
    }

    @Nullable
    public CompoundTag getBlockEntityNbt(BlockPos pos) {
        return (CompoundTag) this.pendingBlockEntities.get(pos);
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
            this.carverBiome = (Holder) biomeSupplier.get();
        }

        return this.carverBiome;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        try {
            int l = QuartPos.fromBlock(this.getMinBuildHeight());
            int i1 = l + QuartPos.fromBlock(this.getHeight()) - 1;
            int j1 = Mth.clamp(biomeY, l, i1);
            int k1 = this.getSectionIndex(QuartPos.toBlock(j1));

            return this.sections[k1].getNoiseBiome(biomeX & 3, j1 & 3, biomeZ & 3);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting biome");
            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Biome being got");

            crashreportsystemdetails.setDetail("Location", () -> {
                return CrashReportCategory.formatLocation(this, biomeX, biomeY, biomeZ);
            });
            throw new ReportedException(crashreport);
        }
    }

    // CraftBukkit start
    public void setBiome(int i, int j, int k, Holder<Biome> biome) {
        try {
            int l = QuartPos.fromBlock(this.getMinBuildHeight());
            int i1 = l + QuartPos.fromBlock(this.getHeight()) - 1;
            int j1 = Mth.clamp(j, l, i1);
            int k1 = this.getSectionIndex(QuartPos.toBlock(j1));

            this.sections[k1].setBiome(i & 3, j1 & 3, k & 3, biome);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Setting biome");
            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Biome being set");

            crashreportsystemdetails.setDetail("Location", () -> {
                return CrashReportCategory.formatLocation(this, i, j, k);
            });
            throw new ReportedException(crashreport);
        }
    }
    // CraftBukkit end

    public void fillBiomesFromNoise(BiomeResolver biomeSupplier, Climate.Sampler sampler) {
        ChunkPos chunkcoordintpair = this.getPos();
        int i = QuartPos.fromBlock(chunkcoordintpair.getMinBlockX());
        int j = QuartPos.fromBlock(chunkcoordintpair.getMinBlockZ());
        LevelHeightAccessor levelheightaccessor = this.getHeightAccessorForGeneration();

        for (int k = levelheightaccessor.getMinSection(); k < levelheightaccessor.getMaxSection(); ++k) {
            LevelChunkSection chunksection = this.getSection(this.getSectionIndexFromSectionY(k));

            chunksection.fillBiomesFromNoise(biomeSupplier, sampler, i, j);
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
    public void setLastSaved(long ticks) {} // Paper

    // CraftBukkit start - decompile error
    public static record TicksToSave(SerializableTickContainer<Block> blocks, SerializableTickContainer<Fluid> fluids) {

        /*
        private final SerializableTickContainer<Block> blocks;
        private final SerializableTickContainer<FluidType> fluids;

        public a(SerializableTickContainer<Block> serializabletickcontainer, SerializableTickContainer<FluidType> serializabletickcontainer1) {
            this.blocks = serializabletickcontainer;
            this.fluids = serializabletickcontainer1;
        }

        public SerializableTickContainer<Block> blocks() {
            return this.blocks;
        }

        public SerializableTickContainer<FluidType> fluids() {
            return this.fluids;
        }
         */
        // CraftBukkit end
    }
}
