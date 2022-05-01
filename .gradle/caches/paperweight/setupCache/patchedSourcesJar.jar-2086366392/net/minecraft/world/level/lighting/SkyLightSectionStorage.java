package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class SkyLightSectionStorage extends LayerLightSectionStorage<SkyLightSectionStorage.SkyDataLayerStorageMap> {
    private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private final LongSet sectionsWithSources = new LongOpenHashSet();
    private final LongSet sectionsToAddSourcesTo = new LongOpenHashSet();
    private final LongSet sectionsToRemoveSourcesFrom = new LongOpenHashSet();
    private final LongSet columnsWithSkySources = new LongOpenHashSet();
    private volatile boolean hasSourceInconsistencies;

    protected SkyLightSectionStorage(LightChunkGetter chunkProvider) {
        super(LightLayer.SKY, chunkProvider, new SkyLightSectionStorage.SkyDataLayerStorageMap(new com.destroystokyo.paper.util.map.QueuedChangesMapLong2Object<>(), new com.destroystokyo.paper.util.map.QueuedChangesMapLong2Int(), Integer.MAX_VALUE, false)); // Paper - avoid copying light data
    }

    @Override
    protected int getLightValue(long blockPos) {
        return this.getLightValue(blockPos, false);
    }

    protected int getLightValue(long blockPos, boolean cached) {
        long l = SectionPos.blockToSection(blockPos);
        int i = SectionPos.y(l);
        synchronized (this.visibleUpdateLock) { // Paper - avoid copying light data
        SkyLightSectionStorage.SkyDataLayerStorageMap skyDataLayerStorageMap = (SkyLightSectionStorage.SkyDataLayerStorageMap) this.e_visible; // Paper - avoid copying light data - must be after lock acquire
        int j = skyDataLayerStorageMap.otherData.getVisibleAsync(SectionPos.getZeroNode(l)); // Paper - avoid copying light data
        if (j != skyDataLayerStorageMap.currentLowestY && i < j) {
            DataLayer dataLayer = this.getDataLayer(skyDataLayerStorageMap, l);
            if (dataLayer == null) {
                for(blockPos = BlockPos.getFlatIndex(blockPos); dataLayer == null; dataLayer = this.getDataLayer(skyDataLayerStorageMap, l)) {
                    ++i;
                    if (i >= j) {
                        return 15;
                    }

                    blockPos = BlockPos.offset(blockPos, 0, 16, 0);
                    l = SectionPos.offset(l, Direction.UP);
                }
            }

            return dataLayer.get(SectionPos.sectionRelative(BlockPos.getX(blockPos)), SectionPos.sectionRelative(BlockPos.getY(blockPos)), SectionPos.sectionRelative(BlockPos.getZ(blockPos)));
        } else {
            return cached && !this.lightOnInSection(l) ? 0 : 15;
        }
        } // Paper - avoid copying light data
    }

    @Override
    protected void onNodeAdded(long sectionPos) {
        int i = SectionPos.y(sectionPos);
        if ((this.updatingSectionData).currentLowestY > i) {
            (this.updatingSectionData).currentLowestY = i;
            (this.updatingSectionData).otherData.queueDefaultReturnValue((this.updatingSectionData).currentLowestY); // Paper - avoid copying light data
        }

        long l = SectionPos.getZeroNode(sectionPos);
        int j = (this.updatingSectionData).otherData.getUpdating(l); // Paper - avoid copying light data
        if (j < i + 1) {
            (this.updatingSectionData).otherData.queueUpdate(l, i + 1); // Paper - avoid copying light data
            if (this.columnsWithSkySources.contains(l)) {
                this.queueAddSource(sectionPos);
                if (j > (this.updatingSectionData).currentLowestY) {
                    long m = SectionPos.asLong(SectionPos.x(sectionPos), j - 1, SectionPos.z(sectionPos));
                    this.queueRemoveSource(m);
                }

                this.recheckInconsistencyFlag();
            }
        }

    }

    private void queueRemoveSource(long sectionPos) {
        this.sectionsToRemoveSourcesFrom.add(sectionPos);
        this.sectionsToAddSourcesTo.remove(sectionPos);
    }

    private void queueAddSource(long sectionPos) {
        this.sectionsToAddSourcesTo.add(sectionPos);
        this.sectionsToRemoveSourcesFrom.remove(sectionPos);
    }

    private void recheckInconsistencyFlag() {
        this.hasSourceInconsistencies = !this.sectionsToAddSourcesTo.isEmpty() || !this.sectionsToRemoveSourcesFrom.isEmpty();
    }

    @Override
    protected void onNodeRemoved(long sectionPos) {
        long l = SectionPos.getZeroNode(sectionPos);
        boolean bl = this.columnsWithSkySources.contains(l);
        if (bl) {
            this.queueRemoveSource(sectionPos);
        }

        int i = SectionPos.y(sectionPos);
        if ((this.updatingSectionData).otherData.getUpdating(l) == i + 1) { // Paper - avoid copying light data
            long m;
            for(m = sectionPos; !this.storingLightForSection(m) && this.hasSectionsBelow(i); m = SectionPos.offset(m, Direction.DOWN)) {
                --i;
            }

            if (this.storingLightForSection(m)) {
                (this.updatingSectionData).otherData.queueUpdate(l, i + 1); // Paper - avoid copying light data
                if (bl) {
                    this.queueAddSource(m);
                }
            } else {
                (this.updatingSectionData).otherData.queueRemove(l); // Paper - avoid copying light data
            }
        }

        if (bl) {
            this.recheckInconsistencyFlag();
        }

    }

    @Override
    protected void enableLightSources(long columnPos, boolean enabled) {
        this.runAllUpdates();
        if (enabled && this.columnsWithSkySources.add(columnPos)) {
            int i = (this.updatingSectionData).otherData.getUpdating(columnPos); // Paper - avoid copying light data
            if (i != (this.updatingSectionData).currentLowestY) {
                long l = SectionPos.asLong(SectionPos.x(columnPos), i - 1, SectionPos.z(columnPos));
                this.queueAddSource(l);
                this.recheckInconsistencyFlag();
            }
        } else if (!enabled) {
            this.columnsWithSkySources.remove(columnPos);
        }

    }

    @Override
    protected boolean hasInconsistencies() {
        return super.hasInconsistencies() || this.hasSourceInconsistencies;
    }

    @Override
    protected DataLayer createDataLayer(long sectionPos) {
        DataLayer dataLayer = this.queuedSections.get(sectionPos);
        if (dataLayer != null) {
            return dataLayer;
        } else {
            long l = SectionPos.offset(sectionPos, Direction.UP);
            int i = (this.updatingSectionData).otherData.getUpdating(SectionPos.getZeroNode(sectionPos)); // Paper - avoid copying light data
            if (i != (this.updatingSectionData).currentLowestY && SectionPos.y(l) < i) {
                DataLayer dataLayer2;
                while((dataLayer2 = this.getDataLayer(l, true)) == null) {
                    l = SectionPos.offset(l, Direction.UP);
                }

                return repeatFirstLayer(dataLayer2);
            } else {
                return new DataLayer();
            }
        }
    }

    private static DataLayer repeatFirstLayer(DataLayer source) {
        if (source.isEmpty()) {
            return new DataLayer();
        } else {
            byte[] bs = source.getData();
            byte[] cs = new byte[2048];

            for(int i = 0; i < 16; ++i) {
                System.arraycopy(bs, 0, cs, i * 128, 128);
            }

            return new DataLayer(cs);
        }
    }

    @Override
    protected void markNewInconsistencies(LayerLightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, ?> lightProvider, boolean doSkylight, boolean skipEdgeLightPropagation) {
        super.markNewInconsistencies(lightProvider, doSkylight, skipEdgeLightPropagation);
        if (doSkylight) {
            if (!this.sectionsToAddSourcesTo.isEmpty()) {
                for(long l : this.sectionsToAddSourcesTo) {
                    int i = this.getLevel(l);
                    if (i != 2 && !this.sectionsToRemoveSourcesFrom.contains(l) && this.sectionsWithSources.add(l)) {
                        if (i == 1) {
                            this.clearQueuedSectionBlocks(lightProvider, l);
                            if (this.changedSections.add(l)) {
                                this.updatingSectionData.copyDataLayer(l);
                            }

                            Arrays.fill(this.getDataLayer(l, true).getData(), (byte)-1);
                            int j = SectionPos.sectionToBlockCoord(SectionPos.x(l));
                            int k = SectionPos.sectionToBlockCoord(SectionPos.y(l));
                            int m = SectionPos.sectionToBlockCoord(SectionPos.z(l));

                            for(Direction direction : HORIZONTALS) {
                                long n = SectionPos.offset(l, direction);
                                if ((this.sectionsToRemoveSourcesFrom.contains(n) || !this.sectionsWithSources.contains(n) && !this.sectionsToAddSourcesTo.contains(n)) && this.storingLightForSection(n)) {
                                    for(int o = 0; o < 16; ++o) {
                                        for(int p = 0; p < 16; ++p) {
                                            long q;
                                            long r;
                                            switch(direction) {
                                            case NORTH:
                                                q = BlockPos.asLong(j + o, k + p, m);
                                                r = BlockPos.asLong(j + o, k + p, m - 1);
                                                break;
                                            case SOUTH:
                                                q = BlockPos.asLong(j + o, k + p, m + 16 - 1);
                                                r = BlockPos.asLong(j + o, k + p, m + 16);
                                                break;
                                            case WEST:
                                                q = BlockPos.asLong(j, k + o, m + p);
                                                r = BlockPos.asLong(j - 1, k + o, m + p);
                                                break;
                                            default:
                                                q = BlockPos.asLong(j + 16 - 1, k + o, m + p);
                                                r = BlockPos.asLong(j + 16, k + o, m + p);
                                            }

                                            lightProvider.checkEdge(q, r, lightProvider.computeLevelFromNeighbor(q, r, 0), true);
                                        }
                                    }
                                }
                            }

                            for(int y = 0; y < 16; ++y) {
                                for(int z = 0; z < 16; ++z) {
                                    long aa = BlockPos.asLong(SectionPos.sectionToBlockCoord(SectionPos.x(l), y), SectionPos.sectionToBlockCoord(SectionPos.y(l)), SectionPos.sectionToBlockCoord(SectionPos.z(l), z));
                                    long ab = BlockPos.asLong(SectionPos.sectionToBlockCoord(SectionPos.x(l), y), SectionPos.sectionToBlockCoord(SectionPos.y(l)) - 1, SectionPos.sectionToBlockCoord(SectionPos.z(l), z));
                                    lightProvider.checkEdge(aa, ab, lightProvider.computeLevelFromNeighbor(aa, ab, 0), true);
                                }
                            }
                        } else {
                            for(int ac = 0; ac < 16; ++ac) {
                                for(int ad = 0; ad < 16; ++ad) {
                                    long ae = BlockPos.asLong(SectionPos.sectionToBlockCoord(SectionPos.x(l), ac), SectionPos.sectionToBlockCoord(SectionPos.y(l), 15), SectionPos.sectionToBlockCoord(SectionPos.z(l), ad));
                                    lightProvider.checkEdge(Long.MAX_VALUE, ae, 0, true);
                                }
                            }
                        }
                    }
                }
            }

            this.sectionsToAddSourcesTo.clear();
            if (!this.sectionsToRemoveSourcesFrom.isEmpty()) {
                for(long af : this.sectionsToRemoveSourcesFrom) {
                    if (this.sectionsWithSources.remove(af) && this.storingLightForSection(af)) {
                        for(int ag = 0; ag < 16; ++ag) {
                            for(int ah = 0; ah < 16; ++ah) {
                                long ai = BlockPos.asLong(SectionPos.sectionToBlockCoord(SectionPos.x(af), ag), SectionPos.sectionToBlockCoord(SectionPos.y(af), 15), SectionPos.sectionToBlockCoord(SectionPos.z(af), ah));
                                lightProvider.checkEdge(Long.MAX_VALUE, ai, 15, false);
                            }
                        }
                    }
                }
            }

            this.sectionsToRemoveSourcesFrom.clear();
            this.hasSourceInconsistencies = false;
        }
    }

    protected boolean hasSectionsBelow(int sectionY) {
        return sectionY >= (this.updatingSectionData).currentLowestY;
    }

    protected boolean isAboveData(long sectionPos) {
        long l = SectionPos.getZeroNode(sectionPos);
        int i = (this.updatingSectionData).otherData.getUpdating(l); // Paper - avoid copying light data
        return i == (this.updatingSectionData).currentLowestY || SectionPos.y(sectionPos) >= i;
    }

    protected boolean lightOnInSection(long sectionPos) {
        long l = SectionPos.getZeroNode(sectionPos);
        return this.columnsWithSkySources.contains(l);
    }

    protected static final class SkyDataLayerStorageMap extends DataLayerStorageMap<SkyLightSectionStorage.SkyDataLayerStorageMap> {
        int currentLowestY;
        private final com.destroystokyo.paper.util.map.QueuedChangesMapLong2Int otherData; // Paper - avoid copying light data

        // Paper start - avoid copying light data
        public SkyDataLayerStorageMap(com.destroystokyo.paper.util.map.QueuedChangesMapLong2Object<DataLayer> arrays, com.destroystokyo.paper.util.map.QueuedChangesMapLong2Int columnToTopSection, int minSectionY, boolean isVisible) {
            super(arrays, isVisible);
            this.otherData = columnToTopSection;
            otherData.queueDefaultReturnValue(minSectionY);
            // Paper end
            this.currentLowestY = minSectionY;
        }

        @Override
        public SkyLightSectionStorage.SkyDataLayerStorageMap copy() {
            this.otherData.performUpdatesLockMap(); // Paper - avoid copying light data
            return new SkyLightSectionStorage.SkyDataLayerStorageMap(this.data, this.otherData, this.currentLowestY, true); // Paper - avoid copying light data
        }
    }
}
