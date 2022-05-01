package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;

public class JigsawFeature extends StructureFeature<JigsawConfiguration> {
    public JigsawFeature(Codec<JigsawConfiguration> codec, int structureStartY, boolean modifyBoundingBox, boolean surface, Predicate<PieceGeneratorSupplier.Context<JigsawConfiguration>> contextPredicate) {
        super(codec, (context) -> {
            if (!contextPredicate.test(context)) {
                return Optional.empty();
            } else {
                BlockPos blockPos = new BlockPos(context.chunkPos().getMinBlockX(), structureStartY, context.chunkPos().getMinBlockZ());
                Pools.bootstrap();
                return JigsawPlacement.addPieces(context, PoolElementStructurePiece::new, blockPos, modifyBoundingBox, surface);
            }
        });
    }
}
