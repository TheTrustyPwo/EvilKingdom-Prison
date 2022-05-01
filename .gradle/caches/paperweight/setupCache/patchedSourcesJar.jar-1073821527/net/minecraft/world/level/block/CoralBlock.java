package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class CoralBlock extends Block {
    private final Block deadBlock;

    public CoralBlock(Block deadCoralBlock, BlockBehaviour.Properties settings) {
        super(settings);
        this.deadBlock = deadCoralBlock;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (!this.scanForWater(world, pos)) {
            world.setBlock(pos, this.deadBlock.defaultBlockState(), 2);
        }

    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (!this.scanForWater(world, pos)) {
            world.scheduleTick(pos, this, 60 + world.getRandom().nextInt(40));
        }

        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    protected boolean scanForWater(BlockGetter world, BlockPos pos) {
        for(Direction direction : Direction.values()) {
            FluidState fluidState = world.getFluidState(pos.relative(direction));
            if (fluidState.is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (!this.scanForWater(ctx.getLevel(), ctx.getClickedPos())) {
            ctx.getLevel().scheduleTick(ctx.getClickedPos(), this, 60 + ctx.getLevel().getRandom().nextInt(40));
        }

        return this.defaultBlockState();
    }
}
