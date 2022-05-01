package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public interface LightEventListener {
    void checkBlock(BlockPos pos);

    void onBlockEmissionIncrease(BlockPos pos, int level);

    boolean hasLightWork();

    int runUpdates(int i, boolean doSkylight, boolean skipEdgeLightPropagation);

    default void updateSectionStatus(BlockPos pos, boolean notReady) {
        this.updateSectionStatus(SectionPos.of(pos), notReady);
    }

    void updateSectionStatus(SectionPos pos, boolean notReady);

    void enableLightSources(ChunkPos pos, boolean retainData);
}
