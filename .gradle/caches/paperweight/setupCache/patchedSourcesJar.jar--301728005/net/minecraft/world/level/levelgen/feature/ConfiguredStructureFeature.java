package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class ConfiguredStructureFeature<FC extends FeatureConfiguration, F extends StructureFeature<FC>> {
    public static final Codec<ConfiguredStructureFeature<?, ?>> DIRECT_CODEC = Registry.STRUCTURE_FEATURE.byNameCodec().dispatch((configuredStructureFeature) -> {
        return configuredStructureFeature.feature;
    }, StructureFeature::configuredStructureCodec);
    public static final Codec<Holder<ConfiguredStructureFeature<?, ?>>> CODEC = RegistryFileCodec.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, DIRECT_CODEC);
    public static final Codec<HolderSet<ConfiguredStructureFeature<?, ?>>> LIST_CODEC = RegistryCodecs.homogeneousList(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, DIRECT_CODEC);
    public final F feature;
    public final FC config;
    public final HolderSet<Biome> biomes;
    public final Map<MobCategory, StructureSpawnOverride> spawnOverrides;
    public final boolean adaptNoise;
    static { io.papermc.paper.world.structure.PaperConfiguredStructure.init(); } // Paper

    public ConfiguredStructureFeature(F feature, FC config, HolderSet<Biome> biomes, boolean bl, Map<MobCategory, StructureSpawnOverride> map) {
        this.feature = feature;
        this.config = config;
        this.biomes = biomes;
        this.adaptNoise = bl;
        this.spawnOverrides = map;
    }

    public StructureStart generate(RegistryAccess registryManager, ChunkGenerator chunkGenerator, BiomeSource biomeSource, StructureManager structureManager, long worldSeed, ChunkPos chunkPos, int structureReferences, LevelHeightAccessor world, Predicate<Holder<Biome>> biomePredicate) {
        Optional<PieceGenerator<FC>> optional = this.feature.pieceGeneratorSupplier().createGenerator(new PieceGeneratorSupplier.Context<>(chunkGenerator, biomeSource, worldSeed, chunkPos, this.config, world, biomePredicate, structureManager, registryManager));
        if (optional.isPresent()) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(0L));
            worldgenRandom.setLargeFeatureSeed(worldSeed, chunkPos.x, chunkPos.z);
            optional.get().generatePieces(structurePiecesBuilder, new PieceGenerator.Context<>(this.config, chunkGenerator, structureManager, chunkPos, world, worldgenRandom, worldSeed));
            StructureStart structureStart = new StructureStart(this, chunkPos, structureReferences, structurePiecesBuilder.build());
            if (structureStart.isValid()) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public HolderSet<Biome> biomes() {
        return this.biomes;
    }

    public BoundingBox adjustBoundingBox(BoundingBox boundingBox) {
        return this.adaptNoise ? boundingBox.inflatedBy(12) : boundingBox;
    }
}
