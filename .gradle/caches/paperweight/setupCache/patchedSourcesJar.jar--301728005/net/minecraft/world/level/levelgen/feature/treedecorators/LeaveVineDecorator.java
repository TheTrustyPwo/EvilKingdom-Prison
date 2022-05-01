package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.Feature;

public class LeaveVineDecorator extends TreeDecorator {
    public static final Codec<LeaveVineDecorator> CODEC = Codec.unit(() -> {
        return LeaveVineDecorator.INSTANCE;
    });
    public static final LeaveVineDecorator INSTANCE = new LeaveVineDecorator();

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.LEAVE_VINE;
    }

    @Override
    public void place(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, List<BlockPos> logPositions, List<BlockPos> leavesPositions) {
        leavesPositions.forEach((pos) -> {
            if (random.nextInt(4) == 0) {
                BlockPos blockPos = pos.west();
                if (Feature.isAir(world, blockPos)) {
                    addHangingVine(world, blockPos, VineBlock.EAST, replacer);
                }
            }

            if (random.nextInt(4) == 0) {
                BlockPos blockPos2 = pos.east();
                if (Feature.isAir(world, blockPos2)) {
                    addHangingVine(world, blockPos2, VineBlock.WEST, replacer);
                }
            }

            if (random.nextInt(4) == 0) {
                BlockPos blockPos3 = pos.north();
                if (Feature.isAir(world, blockPos3)) {
                    addHangingVine(world, blockPos3, VineBlock.SOUTH, replacer);
                }
            }

            if (random.nextInt(4) == 0) {
                BlockPos blockPos4 = pos.south();
                if (Feature.isAir(world, blockPos4)) {
                    addHangingVine(world, blockPos4, VineBlock.NORTH, replacer);
                }
            }

        });
    }

    private static void addHangingVine(LevelSimulatedReader world, BlockPos pos, BooleanProperty facing, BiConsumer<BlockPos, BlockState> replacer) {
        placeVine(replacer, pos, facing);
        int i = 4;

        for(BlockPos var5 = pos.below(); Feature.isAir(world, var5) && i > 0; --i) {
            placeVine(replacer, var5, facing);
            var5 = var5.below();
        }

    }
}
