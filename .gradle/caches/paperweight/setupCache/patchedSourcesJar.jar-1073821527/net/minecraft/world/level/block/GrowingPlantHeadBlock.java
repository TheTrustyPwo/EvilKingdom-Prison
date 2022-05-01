package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GrowingPlantHeadBlock extends GrowingPlantBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_25;
    public static final int MAX_AGE = 25;
    private final double growPerTickProbability;

    protected GrowingPlantHeadBlock(BlockBehaviour.Properties settings, Direction growthDirection, VoxelShape outlineShape, boolean tickWater, double growthChance) {
        super(settings, growthDirection, outlineShape, tickWater);
        this.growPerTickProbability = growthChance;
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, Integer.valueOf(0)));
    }

    @Override
    public BlockState getStateForPlacement(LevelAccessor world) {
        return this.defaultBlockState().setValue(AGE, Integer.valueOf(world.getRandom().nextInt(25)));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 25;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (state.getValue(AGE) < 25 && random.nextDouble() < this.growPerTickProbability) {
            BlockPos blockPos = pos.relative(this.growthDirection);
            if (this.canGrowInto(world.getBlockState(blockPos))) {
                world.setBlockAndUpdate(blockPos, this.getGrowIntoState(state, world.random));
            }
        }

    }

    protected BlockState getGrowIntoState(BlockState state, Random random) {
        return state.cycle(AGE);
    }

    public BlockState getMaxAgeState(BlockState state) {
        return state.setValue(AGE, Integer.valueOf(25));
    }

    public boolean isMaxAge(BlockState state) {
        return state.getValue(AGE) == 25;
    }

    protected BlockState updateBodyAfterConvertedFromHead(BlockState from, BlockState to) {
        return to;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == this.growthDirection.getOpposite() && !state.canSurvive(world, pos)) {
            world.scheduleTick(pos, this, 1);
        }

        if (direction != this.growthDirection || !neighborState.is(this) && !neighborState.is(this.getBodyBlock())) {
            if (this.scheduleFluidTicks) {
                world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
            }

            return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
        } else {
            return this.updateBodyAfterConvertedFromHead(state, this.getBodyBlock().defaultBlockState());
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter world, BlockPos pos, BlockState state, boolean isClient) {
        return this.canGrowInto(world.getBlockState(pos.relative(this.growthDirection)));
    }

    @Override
    public boolean isBonemealSuccess(Level world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, Random random, BlockPos pos, BlockState state) {
        BlockPos blockPos = pos.relative(this.growthDirection);
        int i = Math.min(state.getValue(AGE) + 1, 25);
        int j = this.getBlocksToGrowWhenBonemealed(random);

        for(int k = 0; k < j && this.canGrowInto(world.getBlockState(blockPos)); ++k) {
            world.setBlockAndUpdate(blockPos, state.setValue(AGE, Integer.valueOf(i)));
            blockPos = blockPos.relative(this.growthDirection);
            i = Math.min(i + 1, 25);
        }

    }

    protected abstract int getBlocksToGrowWhenBonemealed(Random random);

    protected abstract boolean canGrowInto(BlockState state);

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return this;
    }
}
