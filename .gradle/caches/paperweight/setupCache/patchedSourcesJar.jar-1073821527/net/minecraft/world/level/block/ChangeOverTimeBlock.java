package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface ChangeOverTimeBlock<T extends Enum<T>> {
    int SCAN_DISTANCE = 4;

    Optional<BlockState> getNext(BlockState state);

    float getChanceModifier();

    default void onRandomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        float f = 0.05688889F;
        if (random.nextFloat() < 0.05688889F) {
            this.applyChangeOverTime(state, world, pos, random);
        }

    }

    T getAge();

    default void applyChangeOverTime(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        int i = this.getAge().ordinal();
        int j = 0;
        int k = 0;

        for(BlockPos blockPos : BlockPos.withinManhattan(pos, 4, 4, 4)) {
            int l = blockPos.distManhattan(pos);
            if (l > 4) {
                break;
            }

            if (!blockPos.equals(pos)) {
                BlockState blockState = world.getBlockState(blockPos);
                Block block = blockState.getBlock();
                if (block instanceof ChangeOverTimeBlock) {
                    Enum<?> enum_ = ((ChangeOverTimeBlock)block).getAge();
                    if (this.getAge().getClass() == enum_.getClass()) {
                        int m = enum_.ordinal();
                        if (m < i) {
                            return;
                        }

                        if (m > i) {
                            ++k;
                        } else {
                            ++j;
                        }
                    }
                }
            }
        }

        float f = (float)(k + 1) / (float)(k + j + 1);
        float g = f * f * this.getChanceModifier();
        if (random.nextFloat() < g) {
            this.getNext(state).ifPresent((statex) -> {
                world.setBlockAndUpdate(pos, statex);
            });
        }

    }
}
