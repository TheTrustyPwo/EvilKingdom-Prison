package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PowderSnowCauldronBlock extends LayeredCauldronBlock {
    public PowderSnowCauldronBlock(BlockBehaviour.Properties settings, Predicate<Biome.Precipitation> precipitationPredicate, Map<Item, CauldronInteraction> behaviorMap) {
        super(settings, precipitationPredicate, behaviorMap);
    }

    @Override
    protected void handleEntityOnFireInside(BlockState state, Level world, BlockPos pos) {
        lowerFillLevel(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LEVEL, state.getValue(LEVEL)), world, pos);
    }
}
