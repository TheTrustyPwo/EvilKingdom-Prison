package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public interface PositionalRandomFactory {
    default RandomSource at(BlockPos pos) {
        return this.at(pos.getX(), pos.getY(), pos.getZ());
    }

    default RandomSource fromHashOf(ResourceLocation id) {
        return this.fromHashOf(id.toString());
    }

    RandomSource fromHashOf(String string);

    RandomSource at(int x, int y, int z);

    @VisibleForTesting
    void parityConfigString(StringBuilder info);
}
