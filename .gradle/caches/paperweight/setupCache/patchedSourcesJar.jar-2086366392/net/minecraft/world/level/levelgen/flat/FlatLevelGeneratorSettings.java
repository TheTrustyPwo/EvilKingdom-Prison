package net.minecraft.world.level.levelgen.flat;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.LayerConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.slf4j.Logger;

public class FlatLevelGeneratorSettings {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<FlatLevelGeneratorSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter((flatLevelGeneratorSettings) -> {
            return flatLevelGeneratorSettings.biomes;
        }), RegistryCodecs.homogeneousList(Registry.STRUCTURE_SET_REGISTRY).optionalFieldOf("structure_overrides").forGetter((flatLevelGeneratorSettings) -> {
            return flatLevelGeneratorSettings.structureOverrides;
        }), FlatLayerInfo.CODEC.listOf().fieldOf("layers").forGetter(FlatLevelGeneratorSettings::getLayersInfo), Codec.BOOL.fieldOf("lakes").orElse(false).forGetter((flatLevelGeneratorSettings) -> {
            return flatLevelGeneratorSettings.addLakes;
        }), Codec.BOOL.fieldOf("features").orElse(false).forGetter((flatLevelGeneratorSettings) -> {
            return flatLevelGeneratorSettings.decoration;
        }), Biome.CODEC.optionalFieldOf("biome").orElseGet(Optional::empty).forGetter((flatLevelGeneratorSettings) -> {
            return Optional.of(flatLevelGeneratorSettings.biome);
        })).apply(instance, FlatLevelGeneratorSettings::new);
    }).comapFlatMap(FlatLevelGeneratorSettings::validateHeight, Function.identity()).stable();
    private final Registry<Biome> biomes;
    private final Optional<HolderSet<StructureSet>> structureOverrides;
    private final List<FlatLayerInfo> layersInfo = Lists.newArrayList();
    private Holder<Biome> biome;
    private final List<BlockState> layers;
    private boolean voidGen;
    private boolean decoration;
    private boolean addLakes;

    private static DataResult<FlatLevelGeneratorSettings> validateHeight(FlatLevelGeneratorSettings config) {
        int i = config.layersInfo.stream().mapToInt(FlatLayerInfo::getHeight).sum();
        return i > DimensionType.Y_SIZE ? DataResult.error("Sum of layer heights is > " + DimensionType.Y_SIZE, config) : DataResult.success(config);
    }

    private FlatLevelGeneratorSettings(Registry<Biome> biomeRegistry, Optional<HolderSet<StructureSet>> optional, List<FlatLayerInfo> layers, boolean hasLakes, boolean hasFeatures, Optional<Holder<Biome>> biome) {
        this(optional, biomeRegistry);
        if (hasLakes) {
            this.setAddLakes();
        }

        if (hasFeatures) {
            this.setDecoration();
        }

        this.layersInfo.addAll(layers);
        this.updateLayers();
        if (biome.isEmpty()) {
            LOGGER.error("Unknown biome, defaulting to plains");
            this.biome = biomeRegistry.getOrCreateHolder(Biomes.PLAINS);
        } else {
            this.biome = biome.get();
        }

    }

    public FlatLevelGeneratorSettings(Optional<HolderSet<StructureSet>> optional, Registry<Biome> biomeRegistry) {
        this.biomes = biomeRegistry;
        this.structureOverrides = optional;
        this.biome = biomeRegistry.getOrCreateHolder(Biomes.PLAINS);
        this.layers = Lists.newArrayList();
    }

    public FlatLevelGeneratorSettings withLayers(List<FlatLayerInfo> layers, Optional<HolderSet<StructureSet>> optional) {
        FlatLevelGeneratorSettings flatLevelGeneratorSettings = new FlatLevelGeneratorSettings(optional, this.biomes);

        for(FlatLayerInfo flatLayerInfo : layers) {
            flatLevelGeneratorSettings.layersInfo.add(new FlatLayerInfo(flatLayerInfo.getHeight(), flatLayerInfo.getBlockState().getBlock()));
            flatLevelGeneratorSettings.updateLayers();
        }

        flatLevelGeneratorSettings.setBiome(this.biome);
        if (this.decoration) {
            flatLevelGeneratorSettings.setDecoration();
        }

        if (this.addLakes) {
            flatLevelGeneratorSettings.setAddLakes();
        }

        return flatLevelGeneratorSettings;
    }

    public void setDecoration() {
        this.decoration = true;
    }

    public void setAddLakes() {
        this.addLakes = true;
    }

    public Holder<Biome> getBiomeFromSettings() {
        Biome biome = this.getBiome().value();
        BiomeGenerationSettings biomeGenerationSettings = biome.getGenerationSettings();
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder();
        if (this.addLakes) {
            builder.addFeature(GenerationStep.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND);
            builder.addFeature(GenerationStep.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_SURFACE);
        }

        boolean bl = (!this.voidGen || this.biome.is(Biomes.THE_VOID)) && this.decoration;
        if (bl) {
            List<HolderSet<PlacedFeature>> list = biomeGenerationSettings.features();

            for(int i = 0; i < list.size(); ++i) {
                if (i != GenerationStep.Decoration.UNDERGROUND_STRUCTURES.ordinal() && i != GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal()) {
                    for(Holder<PlacedFeature> holder : list.get(i)) {
                        builder.addFeature(i, holder);
                    }
                }
            }
        }

        List<BlockState> list2 = this.getLayers();

        for(int j = 0; j < list2.size(); ++j) {
            BlockState blockState = list2.get(j);
            if (!Heightmap.Types.MOTION_BLOCKING.isOpaque().test(blockState)) {
                list2.set(j, (BlockState)null);
                builder.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, PlacementUtils.inlinePlaced(Feature.FILL_LAYER, new LayerConfiguration(j, blockState)));
            }
        }

        return Holder.direct(Biome.BiomeBuilder.from(biome).generationSettings(builder.build()).build());
    }

    public Optional<HolderSet<StructureSet>> structureOverrides() {
        return this.structureOverrides;
    }

    public Holder<Biome> getBiome() {
        return this.biome;
    }

    public void setBiome(Holder<Biome> holder) {
        this.biome = holder;
    }

    public List<FlatLayerInfo> getLayersInfo() {
        return this.layersInfo;
    }

    public List<BlockState> getLayers() {
        return this.layers;
    }

    public void updateLayers() {
        this.layers.clear();

        for(FlatLayerInfo flatLayerInfo : this.layersInfo) {
            for(int i = 0; i < flatLayerInfo.getHeight(); ++i) {
                this.layers.add(flatLayerInfo.getBlockState());
            }
        }

        this.voidGen = this.layers.stream().allMatch((state) -> {
            return state.is(Blocks.AIR);
        });
    }

    public static FlatLevelGeneratorSettings getDefault(Registry<Biome> biomeRegistry, Registry<StructureSet> registry) {
        HolderSet<StructureSet> holderSet = HolderSet.direct(registry.getHolderOrThrow(BuiltinStructureSets.STRONGHOLDS), registry.getHolderOrThrow(BuiltinStructureSets.VILLAGES));
        FlatLevelGeneratorSettings flatLevelGeneratorSettings = new FlatLevelGeneratorSettings(Optional.of(holderSet), biomeRegistry);
        flatLevelGeneratorSettings.biome = biomeRegistry.getOrCreateHolder(Biomes.PLAINS);
        flatLevelGeneratorSettings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.BEDROCK));
        flatLevelGeneratorSettings.getLayersInfo().add(new FlatLayerInfo(2, Blocks.DIRT));
        flatLevelGeneratorSettings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.GRASS_BLOCK));
        flatLevelGeneratorSettings.updateLayers();
        return flatLevelGeneratorSettings;
    }
}
