package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public class EndCityFeature extends StructureFeature<NoneFeatureConfiguration> {
    private static final int RANDOM_SALT = 10387313;

    public EndCityFeature(Codec<NoneFeatureConfiguration> configCodec) {
        super(configCodec, EndCityFeature::pieceGeneratorSupplier);
    }

    private static int getYPositionForFeature(ChunkPos pos, ChunkGenerator chunkGenerator, LevelHeightAccessor world) {
        Random random = new Random((long)(pos.x + pos.z * 10387313));
        Rotation rotation = Rotation.getRandom(random);
        int i = 5;
        int j = 5;
        if (rotation == Rotation.CLOCKWISE_90) {
            i = -5;
        } else if (rotation == Rotation.CLOCKWISE_180) {
            i = -5;
            j = -5;
        } else if (rotation == Rotation.COUNTERCLOCKWISE_90) {
            j = -5;
        }

        int k = pos.getBlockX(7);
        int l = pos.getBlockZ(7);
        int m = chunkGenerator.getFirstOccupiedHeight(k, l, Heightmap.Types.WORLD_SURFACE_WG, world);
        int n = chunkGenerator.getFirstOccupiedHeight(k, l + j, Heightmap.Types.WORLD_SURFACE_WG, world);
        int o = chunkGenerator.getFirstOccupiedHeight(k + i, l, Heightmap.Types.WORLD_SURFACE_WG, world);
        int p = chunkGenerator.getFirstOccupiedHeight(k + i, l + j, Heightmap.Types.WORLD_SURFACE_WG, world);
        return Math.min(Math.min(m, n), Math.min(o, p));
    }

    private static Optional<PieceGenerator<NoneFeatureConfiguration>> pieceGeneratorSupplier(PieceGeneratorSupplier.Context<NoneFeatureConfiguration> context) {
        int i = getYPositionForFeature(context.chunkPos(), context.chunkGenerator(), context.heightAccessor());
        if (i < 60) {
            return Optional.empty();
        } else {
            BlockPos blockPos = context.chunkPos().getMiddleBlockPosition(i);
            return !context.validBiome().test(context.chunkGenerator().getNoiseBiome(QuartPos.fromBlock(blockPos.getX()), QuartPos.fromBlock(blockPos.getY()), QuartPos.fromBlock(blockPos.getZ()))) ? Optional.empty() : Optional.of((collector, contextx) -> {
                Rotation rotation = Rotation.getRandom(contextx.random());
                List<StructurePiece> list = Lists.newArrayList();
                EndCityPieces.startHouseTower(contextx.structureManager(), blockPos, rotation, list, contextx.random());
                list.forEach(collector::addPiece);
            });
        }
    }
}
