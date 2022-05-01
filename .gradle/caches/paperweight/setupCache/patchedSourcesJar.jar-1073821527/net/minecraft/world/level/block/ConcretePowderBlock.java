package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ConcretePowderBlock extends FallingBlock {
    private final BlockState concrete;

    public ConcretePowderBlock(Block hardened, BlockBehaviour.Properties settings) {
        super(settings);
        this.concrete = hardened.defaultBlockState();
    }

    @Override
    public void onLand(Level world, BlockPos pos, BlockState fallingBlockState, BlockState currentStateInPos, FallingBlockEntity fallingBlockEntity) {
        if (shouldSolidify(world, pos, currentStateInPos)) {
            world.setBlock(pos, this.concrete, 3);
        }

    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockGetter blockGetter = ctx.getLevel();
        BlockPos blockPos = ctx.getClickedPos();
        BlockState blockState = blockGetter.getBlockState(blockPos);
        return shouldSolidify(blockGetter, blockPos, blockState) ? this.concrete : super.getStateForPlacement(ctx);
    }

    private static boolean shouldSolidify(BlockGetter world, BlockPos pos, BlockState state) {
        return canSolidify(state) || touchesLiquid(world, pos);
    }

    private static boolean touchesLiquid(BlockGetter world, BlockPos pos) {
        boolean bl = false;
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

        for(Direction direction : Direction.values()) {
            BlockState blockState = world.getBlockState(mutableBlockPos);
            if (direction != Direction.DOWN || canSolidify(blockState)) {
                mutableBlockPos.setWithOffset(pos, direction);
                blockState = world.getBlockState(mutableBlockPos);
                if (canSolidify(blockState) && !blockState.isFaceSturdy(world, pos, direction.getOpposite())) {
                    bl = true;
                    break;
                }
            }
        }

        return bl;
    }

    private static boolean canSolidify(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return touchesLiquid(world, pos) ? this.concrete : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter world, BlockPos pos) {
        return state.getMapColor(world, pos).col;
    }
}
