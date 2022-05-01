package net.minecraft.world.level.chunk;

import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;

public interface LightChunkGetter {
    @Nullable
    BlockGetter getChunkForLighting(int chunkX, int chunkZ);

    default void onLightUpdate(LightLayer type, SectionPos pos) {
    }

    BlockGetter getLevel();
}
