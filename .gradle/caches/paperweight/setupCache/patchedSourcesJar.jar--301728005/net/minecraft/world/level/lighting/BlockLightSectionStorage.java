package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class BlockLightSectionStorage extends LayerLightSectionStorage<BlockLightSectionStorage.BlockDataLayerStorageMap> {
    protected BlockLightSectionStorage(LightChunkGetter chunkProvider) {
        super(LightLayer.BLOCK, chunkProvider, new BlockLightSectionStorage.BlockDataLayerStorageMap(new com.destroystokyo.paper.util.map.QueuedChangesMapLong2Object<>(), false)); // Paper - avoid copying light data
    }

    @Override
    protected int getLightValue(long blockPos) {
        long l = SectionPos.blockToSection(blockPos);
        DataLayer dataLayer = this.getDataLayer(l, false);
        return dataLayer == null ? 0 : dataLayer.get(SectionPos.sectionRelative(BlockPos.getX(blockPos)), SectionPos.sectionRelative(BlockPos.getY(blockPos)), SectionPos.sectionRelative(BlockPos.getZ(blockPos)));
    }

    protected static final class BlockDataLayerStorageMap extends DataLayerStorageMap<BlockLightSectionStorage.BlockDataLayerStorageMap> {
        public BlockDataLayerStorageMap(com.destroystokyo.paper.util.map.QueuedChangesMapLong2Object<DataLayer> long2objectopenhashmap, boolean isVisible) { // Paper - avoid copying light data
            super(long2objectopenhashmap, isVisible); // Paper - avoid copying light data
        }

        @Override
        public BlockLightSectionStorage.BlockDataLayerStorageMap copy() {
            return new BlockDataLayerStorageMap(this.data, true); // Paper - avoid copying light data
        }
    }
}
