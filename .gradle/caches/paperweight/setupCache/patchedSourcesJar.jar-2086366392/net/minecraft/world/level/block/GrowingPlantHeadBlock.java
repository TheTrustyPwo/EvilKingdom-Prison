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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GrowingPlantHeadBlock extends GrowingPlantBlock implements BonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_25;
    public static final int MAX_AGE = 25;
    private final double growPerTickProbability;

    protected GrowingPlantHeadBlock(BlockBehaviour.Properties settings, Direction growthDirection, VoxelShape outlineShape, boolean tickWater, double growthChance) {
        super(settings, growthDirection, outlineShape, tickWater);
        this.growPerTickProbability = growthChance;
        this.registerDefaultState((BlockState) ((BlockState) this.stateDefinition.any()).setValue(GrowingPlantHeadBlock.AGE, 0));
    }

    @Override
    public BlockState getStateForPlacement(LevelAccessor world) {
        return (BlockState) this.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, world.getRandom().nextInt(25));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return (Integer) state.getValue(GrowingPlantHeadBlock.AGE) < 25;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        // Paper start
        final int modifier;
        if (state.is(Blocks.TWISTING_VINES) || state.is(Blocks.TWISTING_VINES_PLANT)) {
            modifier = world.spigotConfig.twistingVinesModifier;
        } else if (state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT)) {
            modifier = world.spigotConfig.weepingVinesModifier;
        } else if (state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT)) {
            modifier = world.spigotConfig.caveVinesModifier;
        } else if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)) {
            modifier = world.spigotConfig.kelpModifier;
        } else {
            modifier = 100; // Above cases are exhaustive as of 1.18
        }
        if ((Integer) state.getValue(GrowingPlantHeadBlock.AGE) < 25 && random.nextDouble() < (modifier / 100.0D) * this.growPerTickProbability) { // Spigot // Paper - fix growth modifier having the reverse effect
            // Paper end
            BlockPos blockposition1 = pos.relative(this.growthDirection);

            if (this.canGrowInto(world.getBlockState(blockposition1))) {
                org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.handleBlockSpreadEvent(world, pos, blockposition1, this.getGrowIntoState(state, world.random, world)); // CraftBukkit // Paper
            }
        }

    }

    // Paper start
    protected BlockState getGrowIntoState(BlockState state, Random random, Level level) {
        return this.getGrowIntoState(state, random);
    }
    // Paper end

    protected BlockState getGrowIntoState(BlockState state, Random random) {
        return (BlockState) state.cycle(GrowingPlantHeadBlock.AGE);
    }

    public BlockState getMaxAgeState(BlockState state) {
        return (BlockState) state.setValue(GrowingPlantHeadBlock.AGE, 25);
    }

    public boolean isMaxAge(BlockState state) {
        return (Integer) state.getValue(GrowingPlantHeadBlock.AGE) == 25;
    }

    protected BlockState updateBodyAfterConvertedFromHead(BlockState from, BlockState to) {
        return to;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == this.growthDirection.getOpposite() && !state.canSurvive(world, pos)) {
            world.scheduleTick(pos, (Block) this, 1);
        }

        if (direction == this.growthDirection && (neighborState.is((Block) this) || neighborState.is(this.getBodyBlock()))) {
            return this.updateBodyAfterConvertedFromHead(state, this.getBodyBlock().defaultBlockState());
        } else {
            if (this.scheduleFluidTicks) {
                world.scheduleTick(pos, (Fluid) Fluids.WATER, Fluids.WATER.getTickDelay(world));
            }

            return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GrowingPlantHeadBlock.AGE);
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
        BlockPos blockposition1 = pos.relative(this.growthDirection);
        int i = Math.min((Integer) state.getValue(GrowingPlantHeadBlock.AGE) + 1, 25);
        int j = this.getBlocksToGrowWhenBonemealed(random);

        for (int k = 0; k < j && this.canGrowInto(world.getBlockState(blockposition1)); ++k) {
            world.setBlockAndUpdate(blockposition1, (BlockState) state.setValue(GrowingPlantHeadBlock.AGE, i));
            blockposition1 = blockposition1.relative(this.growthDirection);
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
