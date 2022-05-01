package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class LayeredCauldronBlock extends AbstractCauldronBlock {
    public static final int MIN_FILL_LEVEL = 1;
    public static final int MAX_FILL_LEVEL = 3;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_CAULDRON;
    private static final int BASE_CONTENT_HEIGHT = 6;
    private static final double HEIGHT_PER_LEVEL = 3.0D;
    public static final Predicate<Biome.Precipitation> RAIN = (precipitation) -> {
        return precipitation == Biome.Precipitation.RAIN;
    };
    public static final Predicate<Biome.Precipitation> SNOW = (precipitation) -> {
        return precipitation == Biome.Precipitation.SNOW;
    };
    private final Predicate<Biome.Precipitation> fillPredicate;

    public LayeredCauldronBlock(BlockBehaviour.Properties settings, Predicate<Biome.Precipitation> precipitationPredicate, Map<Item, CauldronInteraction> behaviorMap) {
        super(settings, behaviorMap);
        this.fillPredicate = precipitationPredicate;
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, Integer.valueOf(1)));
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(LEVEL) == 3;
    }

    @Override
    protected boolean canReceiveStalactiteDrip(Fluid fluid) {
        return fluid == Fluids.WATER && this.fillPredicate == RAIN;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return (6.0D + (double)state.getValue(LEVEL).intValue() * 3.0D) / 16.0D;
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!world.isClientSide && entity.isOnFire() && this.isEntityInsideContent(state, pos, entity)) {
            entity.clearFire();
            if (entity.mayInteract(world, pos)) {
                this.handleEntityOnFireInside(state, world, pos);
            }
        }

    }

    protected void handleEntityOnFireInside(BlockState state, Level world, BlockPos pos) {
        lowerFillLevel(state, world, pos);
    }

    public static void lowerFillLevel(BlockState state, Level world, BlockPos pos) {
        int i = state.getValue(LEVEL) - 1;
        world.setBlockAndUpdate(pos, i == 0 ? Blocks.CAULDRON.defaultBlockState() : state.setValue(LEVEL, Integer.valueOf(i)));
    }

    @Override
    public void handlePrecipitation(BlockState state, Level world, BlockPos pos, Biome.Precipitation precipitation) {
        if (CauldronBlock.shouldHandlePrecipitation(world, precipitation) && state.getValue(LEVEL) != 3 && this.fillPredicate.test(precipitation)) {
            world.setBlockAndUpdate(pos, state.cycle(LEVEL));
        }
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected void receiveStalactiteDrip(BlockState state, Level world, BlockPos pos, Fluid fluid) {
        if (!this.isFull(state)) {
            world.setBlockAndUpdate(pos, state.setValue(LEVEL, Integer.valueOf(state.getValue(LEVEL) + 1)));
            world.levelEvent(1047, pos, 0);
        }
    }
}
