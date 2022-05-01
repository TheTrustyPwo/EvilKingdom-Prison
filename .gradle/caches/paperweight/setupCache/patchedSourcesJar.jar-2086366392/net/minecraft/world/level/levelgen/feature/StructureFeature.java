package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.MineshaftConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OceanRuinConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RangeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RuinedPortalConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ShipwreckConfiguration;
import net.minecraft.world.level.levelgen.structure.NetherFossilFeature;
import net.minecraft.world.level.levelgen.structure.OceanRuinFeature;
import net.minecraft.world.level.levelgen.structure.PostPlacementProcessor;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.slf4j.Logger;

public class StructureFeature<C extends FeatureConfiguration> {
    private static final Map<StructureFeature<?>, GenerationStep.Decoration> STEP = Maps.newHashMap();
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final StructureFeature<JigsawConfiguration> PILLAGER_OUTPOST = register("pillager_outpost", new PillagerOutpostFeature(JigsawConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<MineshaftConfiguration> MINESHAFT = register("mineshaft", new MineshaftFeature(MineshaftConfiguration.CODEC), GenerationStep.Decoration.UNDERGROUND_STRUCTURES);
    public static final StructureFeature<NoneFeatureConfiguration> WOODLAND_MANSION = register("mansion", new WoodlandMansionFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<NoneFeatureConfiguration> JUNGLE_TEMPLE = register("jungle_pyramid", new JunglePyramidFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<NoneFeatureConfiguration> DESERT_PYRAMID = register("desert_pyramid", new DesertPyramidFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<NoneFeatureConfiguration> IGLOO = register("igloo", new IglooFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<RuinedPortalConfiguration> RUINED_PORTAL = register("ruined_portal", new RuinedPortalFeature(RuinedPortalConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<ShipwreckConfiguration> SHIPWRECK = register("shipwreck", new ShipwreckFeature(ShipwreckConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<NoneFeatureConfiguration> SWAMP_HUT = register("swamp_hut", new SwamplandHutFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<NoneFeatureConfiguration> STRONGHOLD = register("stronghold", new StrongholdFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.STRONGHOLDS);
    public static final StructureFeature<NoneFeatureConfiguration> OCEAN_MONUMENT = register("monument", new OceanMonumentFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<OceanRuinConfiguration> OCEAN_RUIN = register("ocean_ruin", new OceanRuinFeature(OceanRuinConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<NoneFeatureConfiguration> FORTRESS = register("fortress", new NetherFortressFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.UNDERGROUND_DECORATION);
    public static final StructureFeature<NoneFeatureConfiguration> END_CITY = register("endcity", new EndCityFeature(NoneFeatureConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<ProbabilityFeatureConfiguration> BURIED_TREASURE = register("buried_treasure", new BuriedTreasureFeature(ProbabilityFeatureConfiguration.CODEC), GenerationStep.Decoration.UNDERGROUND_STRUCTURES);
    public static final StructureFeature<JigsawConfiguration> VILLAGE = register("village", new VillageFeature(JigsawConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final StructureFeature<RangeConfiguration> NETHER_FOSSIL = register("nether_fossil", new NetherFossilFeature(RangeConfiguration.CODEC), GenerationStep.Decoration.UNDERGROUND_DECORATION);
    public static final StructureFeature<JigsawConfiguration> BASTION_REMNANT = register("bastion_remnant", new BastionFeature(JigsawConfiguration.CODEC), GenerationStep.Decoration.SURFACE_STRUCTURES);
    public static final int MAX_STRUCTURE_RANGE = 8;
    private final Codec<ConfiguredStructureFeature<C, StructureFeature<C>>> configuredStructureCodec;
    private final PieceGeneratorSupplier<C> pieceGenerator;
    private final PostPlacementProcessor postPlacementProcessor;

    private static <F extends StructureFeature<?>> F register(String name, F structureFeature, GenerationStep.Decoration step) {
        STEP.put(structureFeature, step);
        return Registry.register(Registry.STRUCTURE_FEATURE, name, structureFeature);
    }

    public StructureFeature(Codec<C> configCodec, PieceGeneratorSupplier<C> piecesGenerator) {
        this(configCodec, piecesGenerator, PostPlacementProcessor.NONE);
    }

    public StructureFeature(Codec<C> configCodec, PieceGeneratorSupplier<C> piecesGenerator, PostPlacementProcessor postPlacementProcessor) {
        this.configuredStructureCodec = RecordCodecBuilder.create((instance) -> {
            return instance.group(configCodec.fieldOf("config").forGetter((configuredStructureFeature) -> {
                return (C)configuredStructureFeature.config;
            }), RegistryCodecs.homogeneousList(Registry.BIOME_REGISTRY).fieldOf("biomes").forGetter(ConfiguredStructureFeature::biomes), Codec.BOOL.optionalFieldOf("adapt_noise", Boolean.valueOf(false)).forGetter((configuredStructureFeature) -> {
                return configuredStructureFeature.adaptNoise;
            }), Codec.simpleMap(MobCategory.CODEC, StructureSpawnOverride.CODEC, StringRepresentable.keys(MobCategory.values())).fieldOf("spawn_overrides").forGetter((configuredStructureFeature) -> {
                return configuredStructureFeature.spawnOverrides;
            })).apply(instance, (featureConfiguration, holderSet, boolean_, map) -> {
                return new ConfiguredStructureFeature<>(this, featureConfiguration, holderSet, boolean_, map);
            });
        });
        this.pieceGenerator = piecesGenerator;
        this.postPlacementProcessor = postPlacementProcessor;
    }

    public GenerationStep.Decoration step() {
        return STEP.get(this);
    }

    public static void bootstrap() {
    }

    @Nullable
    public static StructureStart loadStaticStart(StructurePieceSerializationContext context, CompoundTag nbt, long worldSeed) {
        String string = nbt.getString("id");
        if ("INVALID".equals(string)) {
            return StructureStart.INVALID_START;
        } else {
            Registry<ConfiguredStructureFeature<?, ?>> registry = context.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
            ConfiguredStructureFeature<?, ?> configuredStructureFeature = registry.get(new ResourceLocation(string));
            if (configuredStructureFeature == null) {
                LOGGER.error("Unknown feature id: {}", (Object)string);
                return null;
            } else {
                ChunkPos chunkPos = new ChunkPos(nbt.getInt("ChunkX"), nbt.getInt("ChunkZ"));
                int i = nbt.getInt("references");
                ListTag listTag = nbt.getList("Children", 10);

                try {
                    PiecesContainer piecesContainer = PiecesContainer.load(listTag, context);
                    if (configuredStructureFeature.feature == OCEAN_MONUMENT) {
                        piecesContainer = OceanMonumentFeature.regeneratePiecesAfterLoad(chunkPos, worldSeed, piecesContainer);
                    }

                    return new StructureStart(configuredStructureFeature, chunkPos, i, piecesContainer);
                } catch (Exception var11) {
                    LOGGER.error("Failed Start with id {}", string, var11);
                    return null;
                }
            }
        }
    }

    public Codec<ConfiguredStructureFeature<C, StructureFeature<C>>> configuredStructureCodec() {
        return this.configuredStructureCodec;
    }

    public ConfiguredStructureFeature<C, ? extends StructureFeature<C>> configured(C config, TagKey<Biome> biomeTag) {
        return this.configured(config, biomeTag, false);
    }

    public ConfiguredStructureFeature<C, ? extends StructureFeature<C>> configured(C config, TagKey<Biome> biomeTag, boolean bl) {
        return new ConfiguredStructureFeature<>(this, config, BuiltinRegistries.BIOME.getOrCreateTag(biomeTag), bl, Map.of());
    }

    public ConfiguredStructureFeature<C, ? extends StructureFeature<C>> configured(C config, TagKey<Biome> biomeTag, Map<MobCategory, StructureSpawnOverride> map) {
        return new ConfiguredStructureFeature<>(this, config, BuiltinRegistries.BIOME.getOrCreateTag(biomeTag), false, map);
    }

    public ConfiguredStructureFeature<C, ? extends StructureFeature<C>> configured(C config, TagKey<Biome> biomeTag, boolean bl, Map<MobCategory, StructureSpawnOverride> map) {
        return new ConfiguredStructureFeature<>(this, config, BuiltinRegistries.BIOME.getOrCreateTag(biomeTag), bl, map);
    }

    public static BlockPos getLocatePos(RandomSpreadStructurePlacement placement, ChunkPos chunkPos) {
        return (new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ())).offset(placement.locateOffset());
    }

    public boolean canGenerate(RegistryAccess registryManager, ChunkGenerator chunkGenerator, BiomeSource biomeSource, StructureManager structureManager, long worldSeed, ChunkPos pos, C config, LevelHeightAccessor world, Predicate<Holder<Biome>> biomePredicate) {
        return this.pieceGenerator.createGenerator(new PieceGeneratorSupplier.Context<>(chunkGenerator, biomeSource, worldSeed, pos, config, world, biomePredicate, structureManager, registryManager)).isPresent();
    }

    public PieceGeneratorSupplier<C> pieceGeneratorSupplier() {
        return this.pieceGenerator;
    }

    public PostPlacementProcessor getPostPlacementProcessor() {
        return this.postPlacementProcessor;
    }
}
