package net.minecraft.world.level.lighting;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.DataLayer;

public interface LayerLightEventListener extends LightEventListener {
    @Nullable
    DataLayer getDataLayerData(SectionPos pos);

    int getLightValue(BlockPos pos);

    public static enum DummyLightLayerEventListener implements LayerLightEventListener {
        INSTANCE;

        @Nullable
        @Override
        public DataLayer getDataLayerData(SectionPos pos) {
            return null;
        }

        @Override
        public int getLightValue(BlockPos pos) {
            return 0;
        }

        @Override
        public void checkBlock(BlockPos pos) {
        }

        @Override
        public void onBlockEmissionIncrease(BlockPos pos, int level) {
        }

        @Override
        public boolean hasLightWork() {
            return false;
        }

        @Override
        public int runUpdates(int i, boolean doSkylight, boolean skipEdgeLightPropagation) {
            return i;
        }

        @Override
        public void updateSectionStatus(SectionPos pos, boolean notReady) {
        }

        @Override
        public void enableLightSources(ChunkPos pos, boolean retainData) {
        }
    }
}
