package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class CauldronBlock extends AbstractCauldronBlock {
    private static final float RAIN_FILL_CHANCE = 0.05F;
    private static final float POWDER_SNOW_FILL_CHANCE = 0.1F;

    public CauldronBlock(BlockBehaviour.Properties settings) {
        super(settings, CauldronInteraction.EMPTY);
    }

    @Override
    public boolean isFull(BlockState state) {
        return false;
    }

    protected static boolean shouldHandlePrecipitation(Level world, Biome.Precipitation precipitation) {
        if (precipitation == Biome.Precipitation.RAIN) {
            return world.getRandom().nextFloat() < 0.05F;
        } else if (precipitation == Biome.Precipitation.SNOW) {
            return world.getRandom().nextFloat() < 0.1F;
        } else {
            return false;
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level world, BlockPos pos, Biome.Precipitation precipitation) {
        if (shouldHandlePrecipitation(world, precipitation)) {
            if (precipitation == Biome.Precipitation.RAIN) {
                world.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState());
                world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
            } else if (precipitation == Biome.Precipitation.SNOW) {
                world.setBlockAndUpdate(pos, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState());
                world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
            }

        }
    }

    @Override
    protected boolean canReceiveStalactiteDrip(Fluid fluid) {
        return true;
    }

    @Override
    protected void receiveStalactiteDrip(BlockState state, Level world, BlockPos pos, Fluid fluid) {
        if (fluid == Fluids.WATER) {
            world.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState());
            world.levelEvent(1047, pos, 0);
            world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
        } else if (fluid == Fluids.LAVA) {
            world.setBlockAndUpdate(pos, Blocks.LAVA_CAULDRON.defaultBlockState());
            world.levelEvent(1046, pos, 0);
            world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, pos);
        }

    }
}
