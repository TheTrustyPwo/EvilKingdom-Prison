package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.Random;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GrowingPlantBodyBlock extends GrowingPlantBlock implements BonemealableBlock {
    protected GrowingPlantBodyBlock(BlockBehaviour.Properties settings, Direction growthDirection, VoxelShape outlineShape, boolean tickWater) {
        super(settings, growthDirection, outlineShape, tickWater);
    }

    protected BlockState updateHeadAfterConvertedFromBody(BlockState from, BlockState to) {
        return to;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == this.growthDirection.getOpposite() && !state.canSurvive(world, pos)) {
            world.scheduleTick(pos, this, 1);
        }

        GrowingPlantHeadBlock growingPlantHeadBlock = this.getHeadBlock();
        if (direction == this.growthDirection && !neighborState.is(this) && !neighborState.is(growingPlantHeadBlock)) {
            return this.updateHeadAfterConvertedFromBody(state, growingPlantHeadBlock.getStateForPlacement(world));
        } else {
            if (this.scheduleFluidTicks) {
                world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
            }

            return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        return new ItemStack(this.getHeadBlock());
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter world, BlockPos pos, BlockState state, boolean isClient) {
        Optional<BlockPos> optional = this.getHeadPos(world, pos, state.getBlock());
        return optional.isPresent() && this.getHeadBlock().canGrowInto(world.getBlockState(optional.get().relative(this.growthDirection)));
    }

    @Override
    public boolean isBonemealSuccess(Level world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, Random random, BlockPos pos, BlockState state) {
        Optional<BlockPos> optional = this.getHeadPos(world, pos, state.getBlock());
        if (optional.isPresent()) {
            BlockState blockState = world.getBlockState(optional.get());
            ((GrowingPlantHeadBlock)blockState.getBlock()).performBonemeal(world, random, optional.get(), blockState);
        }

    }

    private Optional<BlockPos> getHeadPos(BlockGetter world, BlockPos pos, Block block) {
        return BlockUtil.getTopConnectedBlock(world, pos, block, this.growthDirection, this.getHeadBlock());
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        boolean bl = super.canBeReplaced(state, context);
        return bl && context.getItemInHand().is(this.getHeadBlock().asItem()) ? false : bl;
    }

    @Override
    protected Block getBodyBlock() {
        return this;
    }
}
