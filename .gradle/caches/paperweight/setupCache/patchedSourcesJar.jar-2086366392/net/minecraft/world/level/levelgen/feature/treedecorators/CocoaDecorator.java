package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

public class CocoaDecorator extends TreeDecorator {
    public static final Codec<CocoaDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(CocoaDecorator::new, (decorator) -> {
        return decorator.probability;
    }).codec();
    private final float probability;

    public CocoaDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.COCOA;
    }

    @Override
    public void place(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, List<BlockPos> logPositions, List<BlockPos> leavesPositions) {
        if (logPositions.isEmpty()) return; // Paper
        if (!(random.nextFloat() >= this.probability)) {
            int i = logPositions.get(0).getY();
            logPositions.stream().filter((pos) -> {
                return pos.getY() - i <= 2;
            }).forEach((pos) -> {
                for(Direction direction : Direction.Plane.HORIZONTAL) {
                    if (random.nextFloat() <= 0.25F) {
                        Direction direction2 = direction.getOpposite();
                        BlockPos blockPos = pos.offset(direction2.getStepX(), 0, direction2.getStepZ());
                        if (Feature.isAir(world, blockPos)) {
                            replacer.accept(blockPos, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE, Integer.valueOf(random.nextInt(3))).setValue(CocoaBlock.FACING, direction));
                        }
                    }
                }

            });
        }
    }
}
