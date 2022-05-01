package net.minecraft.world.level.lighting;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class LevelLightEngine implements LightEventListener {
    public static final int MAX_SOURCE_LEVEL = 15;
    public static final int LIGHT_SECTION_PADDING = 1;
    protected final LevelHeightAccessor levelHeightAccessor;
    @Nullable
    private final LayerLightEngine<?, ?> blockEngine;
    @Nullable
    private final LayerLightEngine<?, ?> skyEngine;

    public LevelLightEngine(LightChunkGetter chunkProvider, boolean hasBlockLight, boolean hasSkyLight) {
        this.levelHeightAccessor = chunkProvider.getLevel();
        this.blockEngine = hasBlockLight ? new BlockLightEngine(chunkProvider) : null;
        this.skyEngine = hasSkyLight ? new SkyLightEngine(chunkProvider) : null;
    }

    @Override
    public void checkBlock(BlockPos pos) {
        if (this.blockEngine != null) {
            this.blockEngine.checkBlock(pos);
        }

        if (this.skyEngine != null) {
            this.skyEngine.checkBlock(pos);
        }

    }

    @Override
    public void onBlockEmissionIncrease(BlockPos pos, int level) {
        if (this.blockEngine != null) {
            this.blockEngine.onBlockEmissionIncrease(pos, level);
        }

    }

    @Override
    public boolean hasLightWork() {
        if (this.skyEngine != null && this.skyEngine.hasLightWork()) {
            return true;
        } else {
            return this.blockEngine != null && this.blockEngine.hasLightWork();
        }
    }

    @Override
    public int runUpdates(int i, boolean doSkylight, boolean skipEdgeLightPropagation) {
        if (this.blockEngine != null && this.skyEngine != null) {
            int j = i / 2;
            int k = this.blockEngine.runUpdates(j, doSkylight, skipEdgeLightPropagation);
            int l = i - j + k;
            int m = this.skyEngine.runUpdates(l, doSkylight, skipEdgeLightPropagation);
            return k == 0 && m > 0 ? this.blockEngine.runUpdates(m, doSkylight, skipEdgeLightPropagation) : m;
        } else if (this.blockEngine != null) {
            return this.blockEngine.runUpdates(i, doSkylight, skipEdgeLightPropagation);
        } else {
            return this.skyEngine != null ? this.skyEngine.runUpdates(i, doSkylight, skipEdgeLightPropagation) : i;
        }
    }

    @Override
    public void updateSectionStatus(SectionPos pos, boolean notReady) {
        if (this.blockEngine != null) {
            this.blockEngine.updateSectionStatus(pos, notReady);
        }

        if (this.skyEngine != null) {
            this.skyEngine.updateSectionStatus(pos, notReady);
        }

    }

    @Override
    public void enableLightSources(ChunkPos pos, boolean retainData) {
        if (this.blockEngine != null) {
            this.blockEngine.enableLightSources(pos, retainData);
        }

        if (this.skyEngine != null) {
            this.skyEngine.enableLightSources(pos, retainData);
        }

    }

    public LayerLightEventListener getLayerListener(LightLayer lightType) {
        if (lightType == LightLayer.BLOCK) {
            return (LayerLightEventListener)(this.blockEngine == null ? LayerLightEventListener.DummyLightLayerEventListener.INSTANCE : this.blockEngine);
        } else {
            return (LayerLightEventListener)(this.skyEngine == null ? LayerLightEventListener.DummyLightLayerEventListener.INSTANCE : this.skyEngine);
        }
    }

    public String getDebugData(LightLayer lightType, SectionPos pos) {
        if (lightType == LightLayer.BLOCK) {
            if (this.blockEngine != null) {
                return this.blockEngine.getDebugData(pos.asLong());
            }
        } else if (this.skyEngine != null) {
            return this.skyEngine.getDebugData(pos.asLong());
        }

        return "n/a";
    }

    public void queueSectionData(LightLayer lightType, SectionPos pos, @Nullable DataLayer nibbles, boolean nonEdge) {
        if (lightType == LightLayer.BLOCK) {
            if (this.blockEngine != null) {
                this.blockEngine.queueSectionData(pos.asLong(), nibbles, nonEdge);
            }
        } else if (this.skyEngine != null) {
            this.skyEngine.queueSectionData(pos.asLong(), nibbles, nonEdge);
        }

    }

    public void retainData(ChunkPos pos, boolean retainData) {
        if (this.blockEngine != null) {
            this.blockEngine.retainData(pos, retainData);
        }

        if (this.skyEngine != null) {
            this.skyEngine.retainData(pos, retainData);
        }

    }

    public int getRawBrightness(BlockPos pos, int ambientDarkness) {
        int i = this.skyEngine == null ? 0 : this.skyEngine.getLightValue(pos) - ambientDarkness;
        int j = this.blockEngine == null ? 0 : this.blockEngine.getLightValue(pos);
        return Math.max(j, i);
    }

    public int getLightSectionCount() {
        return this.levelHeightAccessor.getSectionsCount() + 2;
    }

    public int getMinLightSection() {
        return this.levelHeightAccessor.getMinSection() - 1;
    }

    public int getMaxLightSection() {
        return this.getMinLightSection() + this.getLightSectionCount();
    }
}
