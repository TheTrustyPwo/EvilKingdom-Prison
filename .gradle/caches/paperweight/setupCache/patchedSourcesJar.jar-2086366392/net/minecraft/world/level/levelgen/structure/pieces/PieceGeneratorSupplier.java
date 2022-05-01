package net.minecraft.world.level.levelgen.structure.pieces;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

@FunctionalInterface
public interface PieceGeneratorSupplier<C extends FeatureConfiguration> {
    Optional<PieceGenerator<C>> createGenerator(PieceGeneratorSupplier.Context<C> context);

    static <C extends FeatureConfiguration> PieceGeneratorSupplier<C> simple(Predicate<PieceGeneratorSupplier.Context<C>> predicate, PieceGenerator<C> generator) {
        Optional<PieceGenerator<C>> optional = Optional.of(generator);
        return (context) -> {
            return predicate.test(context) ? optional : Optional.empty();
        };
    }

    static <C extends FeatureConfiguration> Predicate<PieceGeneratorSupplier.Context<C>> checkForBiomeOnTop(Heightmap.Types heightmapType) {
        return (context) -> {
            return context.validBiomeOnTop(heightmapType);
        };
    }

    public static record Context<C extends FeatureConfiguration>(ChunkGenerator chunkGenerator, BiomeSource biomeSource, long seed, ChunkPos chunkPos, C config, LevelHeightAccessor heightAccessor, Predicate<Holder<Biome>> validBiome, StructureManager structureManager, RegistryAccess registryAccess) {
        public boolean validBiomeOnTop(Heightmap.Types heightmapType) {
            int i = this.chunkPos.getMiddleBlockX();
            int j = this.chunkPos.getMiddleBlockZ();
            int k = this.chunkGenerator.getFirstOccupiedHeight(i, j, heightmapType, this.heightAccessor);
            Holder<Biome> holder = this.chunkGenerator.getNoiseBiome(QuartPos.fromBlock(i), QuartPos.fromBlock(k), QuartPos.fromBlock(j));
            return this.validBiome.test(holder);
        }

        public int[] getCornerHeights(int x, int width, int z, int height) {
            return new int[]{this.chunkGenerator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, this.heightAccessor), this.chunkGenerator.getFirstOccupiedHeight(x, z + height, Heightmap.Types.WORLD_SURFACE_WG, this.heightAccessor), this.chunkGenerator.getFirstOccupiedHeight(x + width, z, Heightmap.Types.WORLD_SURFACE_WG, this.heightAccessor), this.chunkGenerator.getFirstOccupiedHeight(x + width, z + height, Heightmap.Types.WORLD_SURFACE_WG, this.heightAccessor)};
        }

        public int getLowestY(int width, int height) {
            int i = this.chunkPos.getMinBlockX();
            int j = this.chunkPos.getMinBlockZ();
            int[] is = this.getCornerHeights(i, width, j, height);
            return Math.min(Math.min(is[0], is[1]), Math.min(is[2], is[3]));
        }
    }
}
