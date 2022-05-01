package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ShortTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import org.slf4j.Logger;

public class ChunkSerializer {
    public static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codec(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState());
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_UPGRADE_DATA = "UpgradeData";
    private static final String BLOCK_TICKS_TAG = "block_ticks";
    private static final String FLUID_TICKS_TAG = "fluid_ticks";

    public static ProtoChunk read(ServerLevel world, PoiManager poiStorage, ChunkPos chunkPos, CompoundTag nbt) {
        ChunkPos chunkPos2 = new ChunkPos(nbt.getInt("xPos"), nbt.getInt("zPos"));
        if (!Objects.equals(chunkPos, chunkPos2)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", chunkPos, chunkPos, chunkPos2);
        }

        UpgradeData upgradeData = nbt.contains("UpgradeData", 10) ? new UpgradeData(nbt.getCompound("UpgradeData"), world) : UpgradeData.EMPTY;
        boolean bl = nbt.getBoolean("isLightOn");
        ListTag listTag = nbt.getList("sections", 10);
        int i = world.getSectionsCount();
        LevelChunkSection[] levelChunkSections = new LevelChunkSection[i];
        boolean bl2 = world.dimensionType().hasSkyLight();
        ChunkSource chunkSource = world.getChunkSource();
        LevelLightEngine levelLightEngine = chunkSource.getLightEngine();
        if (bl) {
            levelLightEngine.retainData(chunkPos, true);
        }

        Registry<Biome> registry = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        Codec<PalettedContainer<Holder<Biome>>> codec = makeBiomeCodec(registry);

        for(int j = 0; j < listTag.size(); ++j) {
            CompoundTag compoundTag = listTag.getCompound(j);
            int k = compoundTag.getByte("Y");
            int l = world.getSectionIndexFromSectionY(k);
            if (l >= 0 && l < levelChunkSections.length) {
                PalettedContainer<BlockState> palettedContainer;
                if (compoundTag.contains("block_states", 10)) {
                    palettedContainer = BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, compoundTag.getCompound("block_states")).promotePartial((errorMessage) -> {
                        logErrors(chunkPos, k, errorMessage);
                    }).getOrThrow(false, LOGGER::error);
                } else {
                    palettedContainer = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
                }

                PalettedContainer<Holder<Biome>> palettedContainer3;
                if (compoundTag.contains("biomes", 10)) {
                    palettedContainer3 = codec.parse(NbtOps.INSTANCE, compoundTag.getCompound("biomes")).promotePartial((errorMessage) -> {
                        logErrors(chunkPos, k, errorMessage);
                    }).getOrThrow(false, LOGGER::error);
                } else {
                    palettedContainer3 = new PalettedContainer<>(registry.asHolderIdMap(), registry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
                }

                LevelChunkSection levelChunkSection = new LevelChunkSection(k, palettedContainer, palettedContainer3);
                levelChunkSections[l] = levelChunkSection;
                poiStorage.checkConsistencyWithBlocks(chunkPos, levelChunkSection);
            }

            if (bl) {
                if (compoundTag.contains("BlockLight", 7)) {
                    levelLightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, k), new DataLayer(compoundTag.getByteArray("BlockLight")), true);
                }

                if (bl2 && compoundTag.contains("SkyLight", 7)) {
                    levelLightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, k), new DataLayer(compoundTag.getByteArray("SkyLight")), true);
                }
            }
        }

        long m = nbt.getLong("InhabitedTime");
        ChunkStatus.ChunkType chunkType = getChunkTypeFromTag(nbt);
        BlendingData blendingData;
        if (nbt.contains("blending_data", 10)) {
            blendingData = BlendingData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("blending_data"))).resultOrPartial(LOGGER::error).orElse((BlendingData)null);
        } else {
            blendingData = null;
        }

        ChunkAccess chunkAccess;
        if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
            LevelChunkTicks<Block> levelChunkTicks = LevelChunkTicks.load(nbt.getList("block_ticks", 10), (id) -> {
                return Registry.BLOCK.getOptional(ResourceLocation.tryParse(id));
            }, chunkPos);
            LevelChunkTicks<Fluid> levelChunkTicks2 = LevelChunkTicks.load(nbt.getList("fluid_ticks", 10), (id) -> {
                return Registry.FLUID.getOptional(ResourceLocation.tryParse(id));
            }, chunkPos);
            chunkAccess = new LevelChunk(world.getLevel(), chunkPos, upgradeData, levelChunkTicks, levelChunkTicks2, m, levelChunkSections, postLoadChunk(world, nbt), blendingData);
        } else {
            ProtoChunkTicks<Block> protoChunkTicks = ProtoChunkTicks.load(nbt.getList("block_ticks", 10), (id) -> {
                return Registry.BLOCK.getOptional(ResourceLocation.tryParse(id));
            }, chunkPos);
            ProtoChunkTicks<Fluid> protoChunkTicks2 = ProtoChunkTicks.load(nbt.getList("fluid_ticks", 10), (id) -> {
                return Registry.FLUID.getOptional(ResourceLocation.tryParse(id));
            }, chunkPos);
            ProtoChunk protoChunk = new ProtoChunk(chunkPos, upgradeData, levelChunkSections, protoChunkTicks, protoChunkTicks2, world, registry, blendingData);
            chunkAccess = protoChunk;
            protoChunk.setInhabitedTime(m);
            if (nbt.contains("below_zero_retrogen", 10)) {
                BelowZeroRetrogen.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("below_zero_retrogen"))).resultOrPartial(LOGGER::error).ifPresent(protoChunk::setBelowZeroRetrogen);
            }

            ChunkStatus chunkStatus = ChunkStatus.byName(nbt.getString("Status"));
            protoChunk.setStatus(chunkStatus);
            if (chunkStatus.isOrAfter(ChunkStatus.FEATURES)) {
                protoChunk.setLightEngine(levelLightEngine);
            }

            BelowZeroRetrogen belowZeroRetrogen = protoChunk.getBelowZeroRetrogen();
            boolean bl3 = chunkStatus.isOrAfter(ChunkStatus.LIGHT) || belowZeroRetrogen != null && belowZeroRetrogen.targetStatus().isOrAfter(ChunkStatus.LIGHT);
            if (!bl && bl3) {
                for(BlockPos blockPos : BlockPos.betweenClosed(chunkPos.getMinBlockX(), world.getMinBuildHeight(), chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(), world.getMaxBuildHeight() - 1, chunkPos.getMaxBlockZ())) {
                    if (chunkAccess.getBlockState(blockPos).getLightEmission() != 0) {
                        protoChunk.addLight(blockPos);
                    }
                }
            }
        }

        chunkAccess.setLightCorrect(bl);
        CompoundTag compoundTag2 = nbt.getCompound("Heightmaps");
        EnumSet<Heightmap.Types> enumSet = EnumSet.noneOf(Heightmap.Types.class);

        for(Heightmap.Types types : chunkAccess.getStatus().heightmapsAfter()) {
            String string = types.getSerializationKey();
            if (compoundTag2.contains(string, 12)) {
                chunkAccess.setHeightmap(types, compoundTag2.getLongArray(string));
            } else {
                enumSet.add(types);
            }
        }

        Heightmap.primeHeightmaps(chunkAccess, enumSet);
        CompoundTag compoundTag3 = nbt.getCompound("structures");
        chunkAccess.setAllStarts(unpackStructureStart(StructurePieceSerializationContext.fromLevel(world), compoundTag3, world.getSeed()));
        chunkAccess.setAllReferences(unpackStructureReferences(world.registryAccess(), chunkPos, compoundTag3));
        if (nbt.getBoolean("shouldSave")) {
            chunkAccess.setUnsaved(true);
        }

        ListTag listTag2 = nbt.getList("PostProcessing", 9);

        for(int n = 0; n < listTag2.size(); ++n) {
            ListTag listTag3 = listTag2.getList(n);

            for(int o = 0; o < listTag3.size(); ++o) {
                chunkAccess.addPackedPostProcess(listTag3.getShort(o), n);
            }
        }

        if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
            return new ImposterProtoChunk((LevelChunk)chunkAccess, false);
        } else {
            ProtoChunk protoChunk2 = (ProtoChunk)chunkAccess;
            ListTag listTag4 = nbt.getList("entities", 10);

            for(int p = 0; p < listTag4.size(); ++p) {
                protoChunk2.addEntity(listTag4.getCompound(p));
            }

            ListTag listTag5 = nbt.getList("block_entities", 10);

            for(int q = 0; q < listTag5.size(); ++q) {
                CompoundTag compoundTag4 = listTag5.getCompound(q);
                chunkAccess.setBlockEntityNbt(compoundTag4);
            }

            ListTag listTag6 = nbt.getList("Lights", 9);

            for(int r = 0; r < listTag6.size(); ++r) {
                ListTag listTag7 = listTag6.getList(r);

                for(int s = 0; s < listTag7.size(); ++s) {
                    protoChunk2.addLight(listTag7.getShort(s), r);
                }
            }

            CompoundTag compoundTag5 = nbt.getCompound("CarvingMasks");

            for(String string2 : compoundTag5.getAllKeys()) {
                GenerationStep.Carving carving = GenerationStep.Carving.valueOf(string2);
                protoChunk2.setCarvingMask(carving, new CarvingMask(compoundTag5.getLongArray(string2), chunkAccess.getMinBuildHeight()));
            }

            return protoChunk2;
        }
    }

    private static void logErrors(ChunkPos chunkPos, int y, String message) {
        LOGGER.error("Recoverable errors when loading section [" + chunkPos.x + ", " + y + ", " + chunkPos.z + "]: " + message);
    }

    private static Codec<PalettedContainer<Holder<Biome>>> makeBiomeCodec(Registry<Biome> biomeRegistry) {
        return PalettedContainer.codec(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.getHolderOrThrow(Biomes.PLAINS));
    }

    public static CompoundTag write(ServerLevel world, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        compoundTag.putInt("xPos", chunkPos.x);
        compoundTag.putInt("yPos", chunk.getMinSection());
        compoundTag.putInt("zPos", chunkPos.z);
        compoundTag.putLong("LastUpdate", world.getGameTime());
        compoundTag.putLong("InhabitedTime", chunk.getInhabitedTime());
        compoundTag.putString("Status", chunk.getStatus().getName());
        BlendingData blendingData = chunk.getBlendingData();
        if (blendingData != null) {
            BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, blendingData).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
                compoundTag.put("blending_data", tag);
            });
        }

        BelowZeroRetrogen belowZeroRetrogen = chunk.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null) {
            BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowZeroRetrogen).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
                compoundTag.put("below_zero_retrogen", tag);
            });
        }

        UpgradeData upgradeData = chunk.getUpgradeData();
        if (!upgradeData.isEmpty()) {
            compoundTag.put("UpgradeData", upgradeData.write());
        }

        LevelChunkSection[] levelChunkSections = chunk.getSections();
        ListTag listTag = new ListTag();
        LevelLightEngine levelLightEngine = world.getChunkSource().getLightEngine();
        Registry<Biome> registry = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        Codec<PalettedContainer<Holder<Biome>>> codec = makeBiomeCodec(registry);
        boolean bl = chunk.isLightCorrect();

        for(int i = levelLightEngine.getMinLightSection(); i < levelLightEngine.getMaxLightSection(); ++i) {
            int j = chunk.getSectionIndexFromSectionY(i);
            boolean bl2 = j >= 0 && j < levelChunkSections.length;
            DataLayer dataLayer = levelLightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkPos, i));
            DataLayer dataLayer2 = levelLightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkPos, i));
            if (bl2 || dataLayer != null || dataLayer2 != null) {
                CompoundTag compoundTag2 = new CompoundTag();
                if (bl2) {
                    LevelChunkSection levelChunkSection = levelChunkSections[j];
                    compoundTag2.put("block_states", BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, levelChunkSection.getStates()).getOrThrow(false, LOGGER::error));
                    compoundTag2.put("biomes", codec.encodeStart(NbtOps.INSTANCE, levelChunkSection.getBiomes()).getOrThrow(false, LOGGER::error));
                }

                if (dataLayer != null && !dataLayer.isEmpty()) {
                    compoundTag2.putByteArray("BlockLight", dataLayer.getData());
                }

                if (dataLayer2 != null && !dataLayer2.isEmpty()) {
                    compoundTag2.putByteArray("SkyLight", dataLayer2.getData());
                }

                if (!compoundTag2.isEmpty()) {
                    compoundTag2.putByte("Y", (byte)i);
                    listTag.add(compoundTag2);
                }
            }
        }

        compoundTag.put("sections", listTag);
        if (bl) {
            compoundTag.putBoolean("isLightOn", true);
        }

        ListTag listTag2 = new ListTag();

        for(BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            CompoundTag compoundTag3 = chunk.getBlockEntityNbtForSaving(blockPos);
            if (compoundTag3 != null) {
                listTag2.add(compoundTag3);
            }
        }

        compoundTag.put("block_entities", listTag2);
        if (chunk.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            ListTag listTag3 = new ListTag();
            listTag3.addAll(protoChunk.getEntities());
            compoundTag.put("entities", listTag3);
            compoundTag.put("Lights", packOffsets(protoChunk.getPackedLights()));
            CompoundTag compoundTag4 = new CompoundTag();

            for(GenerationStep.Carving carving : GenerationStep.Carving.values()) {
                CarvingMask carvingMask = protoChunk.getCarvingMask(carving);
                if (carvingMask != null) {
                    compoundTag4.putLongArray(carving.toString(), carvingMask.toArray());
                }
            }

            compoundTag.put("CarvingMasks", compoundTag4);
        }

        saveTicks(world, compoundTag, chunk.getTicksForSerialization());
        compoundTag.put("PostProcessing", packOffsets(chunk.getPostProcessing()));
        CompoundTag compoundTag5 = new CompoundTag();

        for(Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getStatus().heightmapsAfter().contains(entry.getKey())) {
                compoundTag5.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
            }
        }

        compoundTag.put("Heightmaps", compoundTag5);
        compoundTag.put("structures", packStructureData(StructurePieceSerializationContext.fromLevel(world), chunkPos, chunk.getAllStarts(), chunk.getAllReferences()));
        return compoundTag;
    }

    private static void saveTicks(ServerLevel world, CompoundTag nbt, ChunkAccess.TicksToSave tickSchedulers) {
        long l = world.getLevelData().getGameTime();
        nbt.put("block_ticks", tickSchedulers.blocks().save(l, (block) -> {
            return Registry.BLOCK.getKey(block).toString();
        }));
        nbt.put("fluid_ticks", tickSchedulers.fluids().save(l, (fluid) -> {
            return Registry.FLUID.getKey(fluid).toString();
        }));
    }

    public static ChunkStatus.ChunkType getChunkTypeFromTag(@Nullable CompoundTag nbt) {
        return nbt != null ? ChunkStatus.byName(nbt.getString("Status")).getChunkType() : ChunkStatus.ChunkType.PROTOCHUNK;
    }

    @Nullable
    private static LevelChunk.PostLoadProcessor postLoadChunk(ServerLevel world, CompoundTag nbt) {
        ListTag listTag = getListOfCompoundsOrNull(nbt, "entities");
        ListTag listTag2 = getListOfCompoundsOrNull(nbt, "block_entities");
        return listTag == null && listTag2 == null ? null : (chunk) -> {
            if (listTag != null) {
                world.addLegacyChunkEntities(EntityType.loadEntitiesRecursive(listTag, world));
            }

            if (listTag2 != null) {
                for(int i = 0; i < listTag2.size(); ++i) {
                    CompoundTag compoundTag = listTag2.getCompound(i);
                    boolean bl = compoundTag.getBoolean("keepPacked");
                    if (bl) {
                        chunk.setBlockEntityNbt(compoundTag);
                    } else {
                        BlockPos blockPos = BlockEntity.getPosFromTag(compoundTag);
                        BlockEntity blockEntity = BlockEntity.loadStatic(blockPos, chunk.getBlockState(blockPos), compoundTag);
                        if (blockEntity != null) {
                            chunk.setBlockEntity(blockEntity);
                        }
                    }
                }
            }

        };
    }

    @Nullable
    private static ListTag getListOfCompoundsOrNull(CompoundTag nbt, String key) {
        ListTag listTag = nbt.getList(key, 10);
        return listTag.isEmpty() ? null : listTag;
    }

    private static CompoundTag packStructureData(StructurePieceSerializationContext context, ChunkPos pos, Map<ConfiguredStructureFeature<?, ?>, StructureStart> starts, Map<ConfiguredStructureFeature<?, ?>, LongSet> references) {
        CompoundTag compoundTag = new CompoundTag();
        CompoundTag compoundTag2 = new CompoundTag();
        Registry<ConfiguredStructureFeature<?, ?>> registry = context.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);

        for(Entry<ConfiguredStructureFeature<?, ?>, StructureStart> entry : starts.entrySet()) {
            ResourceLocation resourceLocation = registry.getKey(entry.getKey());
            compoundTag2.put(resourceLocation.toString(), entry.getValue().createTag(context, pos));
        }

        compoundTag.put("starts", compoundTag2);
        CompoundTag compoundTag3 = new CompoundTag();

        for(Entry<ConfiguredStructureFeature<?, ?>, LongSet> entry2 : references.entrySet()) {
            if (!entry2.getValue().isEmpty()) {
                ResourceLocation resourceLocation2 = registry.getKey(entry2.getKey());
                compoundTag3.put(resourceLocation2.toString(), new LongArrayTag(entry2.getValue()));
            }
        }

        compoundTag.put("References", compoundTag3);
        return compoundTag;
    }

    private static Map<ConfiguredStructureFeature<?, ?>, StructureStart> unpackStructureStart(StructurePieceSerializationContext context, CompoundTag nbt, long worldSeed) {
        Map<ConfiguredStructureFeature<?, ?>, StructureStart> map = Maps.newHashMap();
        Registry<ConfiguredStructureFeature<?, ?>> registry = context.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
        CompoundTag compoundTag = nbt.getCompound("starts");

        for(String string : compoundTag.getAllKeys()) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(string);
            ConfiguredStructureFeature<?, ?> configuredStructureFeature = registry.get(resourceLocation);
            if (configuredStructureFeature == null) {
                LOGGER.error("Unknown structure start: {}", (Object)resourceLocation);
            } else {
                StructureStart structureStart = StructureFeature.loadStaticStart(context, compoundTag.getCompound(string), worldSeed);
                if (structureStart != null) {
                    map.put(configuredStructureFeature, structureStart);
                }
            }
        }

        return map;
    }

    private static Map<ConfiguredStructureFeature<?, ?>, LongSet> unpackStructureReferences(RegistryAccess registryAccess, ChunkPos chunkPos, CompoundTag compoundTag) {
        Map<ConfiguredStructureFeature<?, ?>, LongSet> map = Maps.newHashMap();
        Registry<ConfiguredStructureFeature<?, ?>> registry = registryAccess.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
        CompoundTag compoundTag2 = compoundTag.getCompound("References");

        for(String string : compoundTag2.getAllKeys()) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(string);
            ConfiguredStructureFeature<?, ?> configuredStructureFeature = registry.get(resourceLocation);
            if (configuredStructureFeature == null) {
                LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", resourceLocation, chunkPos);
            } else {
                long[] ls = compoundTag2.getLongArray(string);
                if (ls.length != 0) {
                    map.put(configuredStructureFeature, new LongOpenHashSet(Arrays.stream(ls).filter((packedPos) -> {
                        ChunkPos chunkPos2 = new ChunkPos(packedPos);
                        if (chunkPos2.getChessboardDistance(chunkPos) > 8) {
                            LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", resourceLocation, chunkPos2, chunkPos);
                            return false;
                        } else {
                            return true;
                        }
                    }).toArray()));
                }
            }
        }

        return map;
    }

    public static ListTag packOffsets(ShortList[] lists) {
        ListTag listTag = new ListTag();

        for(ShortList shortList : lists) {
            ListTag listTag2 = new ListTag();
            if (shortList != null) {
                for(Short short_ : shortList) {
                    listTag2.add(ShortTag.valueOf(short_));
                }
            }

            listTag.add(listTag2);
        }

        return listTag;
    }
}
