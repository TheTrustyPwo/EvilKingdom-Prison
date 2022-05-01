package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DeadBushBlock extends BushBlock {
    protected static final float AABB_OFFSET = 6.0F;
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    protected DeadBushBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(Blocks.SAND) || floor.is(Blocks.RED_SAND) || floor.is(Blocks.TERRACOTTA) || floor.is(Blocks.WHITE_TERRACOTTA) || floor.is(Blocks.ORANGE_TERRACOTTA) || floor.is(Blocks.MAGENTA_TERRACOTTA) || floor.is(Blocks.LIGHT_BLUE_TERRACOTTA) || floor.is(Blocks.YELLOW_TERRACOTTA) || floor.is(Blocks.LIME_TERRACOTTA) || floor.is(Blocks.PINK_TERRACOTTA) || floor.is(Blocks.GRAY_TERRACOTTA) || floor.is(Blocks.LIGHT_GRAY_TERRACOTTA) || floor.is(Blocks.CYAN_TERRACOTTA) || floor.is(Blocks.PURPLE_TERRACOTTA) || floor.is(Blocks.BLUE_TERRACOTTA) || floor.is(Blocks.BROWN_TERRACOTTA) || floor.is(Blocks.GREEN_TERRACOTTA) || floor.is(Blocks.RED_TERRACOTTA) || floor.is(Blocks.BLACK_TERRACOTTA) || floor.is(BlockTags.DIRT);
    }
}
