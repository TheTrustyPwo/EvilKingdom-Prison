package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.worldgen.features.NetherFeatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.lighting.LayerLightEngine;

public class NyliumBlock extends Block implements BonemealableBlock {

    protected NyliumBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    private static boolean canBeNylium(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos blockposition1 = pos.above();
        BlockState iblockdata1 = world.getBlockState(blockposition1);
        int i = LayerLightEngine.getLightBlockInto(world, state, pos, iblockdata1, blockposition1, Direction.UP, iblockdata1.getLightBlock(world, blockposition1));

        return i < world.getMaxLightLevel();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (!NyliumBlock.canBeNylium(state, world, pos)) {
            // CraftBukkit start
            if (org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callBlockFadeEvent(world, pos, Blocks.NETHERRACK.defaultBlockState()).isCancelled()) {
                return;
            }
            // CraftBukkit end
            world.setBlockAndUpdate(pos, Blocks.NETHERRACK.defaultBlockState());
        }

    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter world, BlockPos pos, BlockState state, boolean isClient) {
        return world.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, Random random, BlockPos pos, BlockState state) {
        BlockState iblockdata1 = world.getBlockState(pos);
        BlockPos blockposition1 = pos.above();
        ChunkGenerator chunkgenerator = world.getChunkSource().getGenerator();

        if (iblockdata1.is(Blocks.CRIMSON_NYLIUM)) {
            ((ConfiguredFeature) NetherFeatures.CRIMSON_FOREST_VEGETATION_BONEMEAL.value()).place(world, chunkgenerator, random, blockposition1);
        } else if (iblockdata1.is(Blocks.WARPED_NYLIUM)) {
            ((ConfiguredFeature) NetherFeatures.WARPED_FOREST_VEGETATION_BONEMEAL.value()).place(world, chunkgenerator, random, blockposition1);
            ((ConfiguredFeature) NetherFeatures.NETHER_SPROUTS_BONEMEAL.value()).place(world, chunkgenerator, random, blockposition1);
            if (random.nextInt(8) == 0) {
                ((ConfiguredFeature) NetherFeatures.TWISTING_VINES_BONEMEAL.value()).place(world, chunkgenerator, random, blockposition1);
            }
        }

    }
}
