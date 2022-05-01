package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BonemealableBlock {
    boolean isValidBonemealTarget(BlockGetter world, BlockPos pos, BlockState state, boolean isClient);

    boolean isBonemealSuccess(Level world, Random random, BlockPos pos, BlockState state);

    void performBonemeal(ServerLevel world, Random random, BlockPos pos, BlockState state);
}
