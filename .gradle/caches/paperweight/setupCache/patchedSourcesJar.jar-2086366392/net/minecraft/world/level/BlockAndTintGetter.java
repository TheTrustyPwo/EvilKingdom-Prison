package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.lighting.LevelLightEngine;

public interface BlockAndTintGetter extends BlockGetter {
    float getShade(Direction direction, boolean shaded);

    LevelLightEngine getLightEngine();

    int getBlockTint(BlockPos pos, ColorResolver colorResolver);

    default int getBrightness(LightLayer type, BlockPos pos) {
        return this.getLightEngine().getLayerListener(type).getLightValue(pos);
    }

    default int getRawBrightness(BlockPos pos, int ambientDarkness) {
        return this.getLightEngine().getRawBrightness(pos, ambientDarkness);
    }

    default boolean canSeeSky(BlockPos pos) {
        return this.getBrightness(LightLayer.SKY, pos) >= this.getMaxLightLevel();
    }
}
