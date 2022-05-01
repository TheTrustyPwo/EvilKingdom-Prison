package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.slf4j.Logger;

public class StructureCheck {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NO_STRUCTURE = -1;
    private final ChunkScanAccess storageAccess;
    private final RegistryAccess registryAccess;
    private final Registry<Biome> biomes;
    private final Registry<ConfiguredStructureFeature<?, ?>> structureConfigs;
    private final StructureManager structureManager;
    private final ResourceKey<Level> dimension;
    private final ChunkGenerator chunkGenerator;
    private final LevelHeightAccessor heightAccessor;
    private final BiomeSource biomeSource;
    private final long seed;
    private final DataFixer fixerUpper;
    private final Long2ObjectMap<Object2IntMap<ConfiguredStructureFeature<?, ?>>> loadedChunks = new Long2ObjectOpenHashMap<>();
    private final Map<ConfiguredStructureFeature<?, ?>, Long2BooleanMap> featureChecks = new HashMap<>();

    public StructureCheck(ChunkScanAccess chunkIoWorker, RegistryAccess registryManager, StructureManager structureManager, ResourceKey<Level> worldKey, ChunkGenerator chunkGenerator, LevelHeightAccessor world, BiomeSource biomeSource, long seed, DataFixer dataFixer) {
        this.storageAccess = chunkIoWorker;
        this.registryAccess = registryManager;
        this.structureManager = structureManager;
        this.dimension = worldKey;
        this.chunkGenerator = chunkGenerator;
        this.heightAccessor = world;
        this.biomeSource = biomeSource;
        this.seed = seed;
        this.fixerUpper = dataFixer;
        this.biomes = registryManager.ownedRegistryOrThrow(Registry.BIOME_REGISTRY);
        this.structureConfigs = registryManager.ownedRegistryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
    }

    public StructureCheckResult checkStart(ChunkPos chunkPos, ConfiguredStructureFeature<?, ?> configuredStructureFeature, boolean skipExistingChunk) {
        long l = chunkPos.toLong();
        Object2IntMap<ConfiguredStructureFeature<?, ?>> object2IntMap = this.loadedChunks.get(l);
        if (object2IntMap != null) {
            return this.checkStructureInfo(object2IntMap, configuredStructureFeature, skipExistingChunk);
        } else {
            StructureCheckResult structureCheckResult = this.tryLoadFromStorage(chunkPos, configuredStructureFeature, skipExistingChunk, l);
            if (structureCheckResult != null) {
                return structureCheckResult;
            } else {
                boolean bl = this.featureChecks.computeIfAbsent(configuredStructureFeature, (configuredStructureFeaturex) -> {
                    return new Long2BooleanOpenHashMap();
                }).computeIfAbsent(l, (lx) -> {
                    return this.canCreateStructure(chunkPos, configuredStructureFeature);
                });
                return !bl ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.CHUNK_LOAD_NEEDED;
            }
        }
    }

    private <FC extends FeatureConfiguration, F extends StructureFeature<FC>> boolean canCreateStructure(ChunkPos pos, ConfiguredStructureFeature<FC, F> feature) {
        return feature.feature.canGenerate(this.registryAccess, this.chunkGenerator, this.biomeSource, this.structureManager, this.seed, pos, feature.config, this.heightAccessor, feature.biomes()::contains);
    }

    @Nullable
    private StructureCheckResult tryLoadFromStorage(ChunkPos pos, ConfiguredStructureFeature<?, ?> configuredStructureFeature, boolean skipExistingChunk, long posLong) {
        CollectFields collectFields = new CollectFields(new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector("Level", "Structures", CompoundTag.TYPE, "Starts"), new FieldSelector("structures", CompoundTag.TYPE, "starts"));

        try {
            this.storageAccess.scanChunk(pos, collectFields).join();
        } catch (Exception var13) {
            LOGGER.warn("Failed to read chunk {}", pos, var13);
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
        }

        Tag tag = collectFields.getResult();
        if (!(tag instanceof CompoundTag)) {
            return null;
        } else {
            CompoundTag compoundTag = (CompoundTag)tag;
            int i = ChunkStorage.getVersion(compoundTag);
            if (i <= 1493) {
                return StructureCheckResult.CHUNK_LOAD_NEEDED;
            } else {
                ChunkStorage.injectDatafixingContext(compoundTag, this.dimension, this.chunkGenerator.getTypeNameForDataFixer());

                CompoundTag compoundTag2;
                try {
                    compoundTag2 = NbtUtils.update(this.fixerUpper, DataFixTypes.CHUNK, compoundTag, i);
                } catch (Exception var12) {
                    LOGGER.warn("Failed to partially datafix chunk {}", pos, var12);
                    return StructureCheckResult.CHUNK_LOAD_NEEDED;
                }

                Object2IntMap<ConfiguredStructureFeature<?, ?>> object2IntMap = this.loadStructures(compoundTag2);
                if (object2IntMap == null) {
                    return null;
                } else {
                    this.storeFullResults(posLong, object2IntMap);
                    return this.checkStructureInfo(object2IntMap, configuredStructureFeature, skipExistingChunk);
                }
            }
        }
    }

    @Nullable
    private Object2IntMap<ConfiguredStructureFeature<?, ?>> loadStructures(CompoundTag nbt) {
        if (!nbt.contains("structures", 10)) {
            return null;
        } else {
            CompoundTag compoundTag = nbt.getCompound("structures");
            if (!compoundTag.contains("starts", 10)) {
                return null;
            } else {
                CompoundTag compoundTag2 = compoundTag.getCompound("starts");
                if (compoundTag2.isEmpty()) {
                    return Object2IntMaps.emptyMap();
                } else {
                    Object2IntMap<ConfiguredStructureFeature<?, ?>> object2IntMap = new Object2IntOpenHashMap<>();
                    Registry<ConfiguredStructureFeature<?, ?>> registry = this.registryAccess.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);

                    for(String string : compoundTag2.getAllKeys()) {
                        ResourceLocation resourceLocation = ResourceLocation.tryParse(string);
                        if (resourceLocation != null) {
                            ConfiguredStructureFeature<?, ?> configuredStructureFeature = registry.get(resourceLocation);
                            if (configuredStructureFeature != null) {
                                CompoundTag compoundTag3 = compoundTag2.getCompound(string);
                                if (!compoundTag3.isEmpty()) {
                                    String string2 = compoundTag3.getString("id");
                                    if (!"INVALID".equals(string2)) {
                                        int i = compoundTag3.getInt("references");
                                        object2IntMap.put(configuredStructureFeature, i);
                                    }
                                }
                            }
                        }
                    }

                    return object2IntMap;
                }
            }
        }
    }

    private static Object2IntMap<ConfiguredStructureFeature<?, ?>> deduplicateEmptyMap(Object2IntMap<ConfiguredStructureFeature<?, ?>> map) {
        return map.isEmpty() ? Object2IntMaps.emptyMap() : map;
    }

    private StructureCheckResult checkStructureInfo(Object2IntMap<ConfiguredStructureFeature<?, ?>> referencesByStructure, ConfiguredStructureFeature<?, ?> configuredStructureFeature, boolean skipExistingChunk) {
        int i = referencesByStructure.getOrDefault(configuredStructureFeature, -1);
        return i == -1 || skipExistingChunk && i != 0 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.START_PRESENT;
    }

    public void onStructureLoad(ChunkPos pos, Map<ConfiguredStructureFeature<?, ?>, StructureStart> structureStarts) {
        long l = pos.toLong();
        Object2IntMap<ConfiguredStructureFeature<?, ?>> object2IntMap = new Object2IntOpenHashMap<>();
        structureStarts.forEach((configuredStructureFeature, structureStart) -> {
            if (structureStart.isValid()) {
                object2IntMap.put(configuredStructureFeature, structureStart.getReferences());
            }

        });
        this.storeFullResults(l, object2IntMap);
    }

    private void storeFullResults(long pos, Object2IntMap<ConfiguredStructureFeature<?, ?>> referencesByStructure) {
        this.loadedChunks.put(pos, deduplicateEmptyMap(referencesByStructure));
        this.featureChecks.values().forEach((generationPossibilityByChunkPos) -> {
            generationPossibilityByChunkPos.remove(pos);
        });
    }

    public void incrementReference(ChunkPos pos, ConfiguredStructureFeature<?, ?> configuredStructureFeature) {
        this.loadedChunks.compute(pos.toLong(), (posx, referencesByStructure) -> {
            if (referencesByStructure == null || referencesByStructure.isEmpty()) {
                referencesByStructure = new Object2IntOpenHashMap<>();
            }

            referencesByStructure.computeInt(configuredStructureFeature, (configuredStructureFeaturex, references) -> {
                return references == null ? 1 : references + 1;
            });
            return referencesByStructure;
        });
    }
}
