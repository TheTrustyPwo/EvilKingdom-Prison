package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public abstract class AbstractTreeGrower {
    @Nullable
    protected abstract Holder<? extends ConfiguredFeature<?, ?>> getConfiguredFeature(Random random, boolean bees);

    public boolean growTree(ServerLevel world, ChunkGenerator chunkGenerator, BlockPos pos, BlockState state, Random random) {
        Holder<? extends ConfiguredFeature<?, ?>> holder = this.getConfiguredFeature(random, this.hasFlowers(world, pos));
        if (holder == null) {
            return false;
        } else {
            ConfiguredFeature<?, ?> configuredFeature = holder.value();
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 4);
            if (configuredFeature.place(world, chunkGenerator, random, pos)) {
                return true;
            } else {
                world.setBlock(pos, state, 4);
                return false;
            }
        }
    }

    private boolean hasFlowers(LevelAccessor world, BlockPos pos) {
        for(BlockPos blockPos : BlockPos.MutableBlockPos.betweenClosed(pos.below().north(2).west(2), pos.above().south(2).east(2))) {
            if (world.getBlockState(blockPos).is(BlockTags.FLOWERS)) {
                return true;
            }
        }

        return false;
    }
}
