package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

public class TrunkVineDecorator extends TreeDecorator {
    public static final Codec<TrunkVineDecorator> CODEC = Codec.unit(() -> {
        return TrunkVineDecorator.INSTANCE;
    });
    public static final TrunkVineDecorator INSTANCE = new TrunkVineDecorator();

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.TRUNK_VINE;
    }

    @Override
    public void place(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, List<BlockPos> logPositions, List<BlockPos> leavesPositions) {
        logPositions.forEach((pos) -> {
            if (random.nextInt(3) > 0) {
                BlockPos blockPos = pos.west();
                if (Feature.isAir(world, blockPos)) {
                    placeVine(replacer, blockPos, VineBlock.EAST);
                }
            }

            if (random.nextInt(3) > 0) {
                BlockPos blockPos2 = pos.east();
                if (Feature.isAir(world, blockPos2)) {
                    placeVine(replacer, blockPos2, VineBlock.WEST);
                }
            }

            if (random.nextInt(3) > 0) {
                BlockPos blockPos3 = pos.north();
                if (Feature.isAir(world, blockPos3)) {
                    placeVine(replacer, blockPos3, VineBlock.SOUTH);
                }
            }

            if (random.nextInt(3) > 0) {
                BlockPos blockPos4 = pos.south();
                if (Feature.isAir(world, blockPos4)) {
                    placeVine(replacer, blockPos4, VineBlock.NORTH);
                }
            }

        });
    }
}
