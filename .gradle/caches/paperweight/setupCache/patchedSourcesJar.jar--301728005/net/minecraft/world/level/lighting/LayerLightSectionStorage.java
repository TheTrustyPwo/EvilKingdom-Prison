package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.SectionTracker;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public abstract class LayerLightSectionStorage<M extends DataLayerStorageMap<M>> extends SectionTracker {
    protected static final int LIGHT_AND_DATA = 0;
    protected static final int LIGHT_ONLY = 1;
    protected static final int EMPTY = 2;
    protected static final DataLayer EMPTY_DATA = new DataLayer();
    private static final Direction[] DIRECTIONS = Direction.values();
    private final LightLayer layer;
    private final LightChunkGetter chunkSource;
    protected final LongSet dataSectionSet = new LongOpenHashSet();
    protected final LongSet toMarkNoData = new LongOpenHashSet();
    protected final LongSet toMarkData = new LongOpenHashSet();
    protected volatile M e_visible; protected final Object visibleUpdateLock = new Object(); // Paper - diff on change, should be "visible" - force compile fail on usage change
    protected final M updatingSectionData;
    protected final LongSet changedSections = new LongOpenHashSet();
    protected final LongSet sectionsAffectedByLightUpdates = new LongOpenHashSet();
    protected final Long2ObjectMap<DataLayer> queuedSections = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
    private final LongSet untrustedSections = new LongOpenHashSet();
    private final LongSet columnsToRetainQueuedDataFor = new LongOpenHashSet();
    private final LongSet toRemove = new LongOpenHashSet();
    protected volatile boolean hasToRemove;

    protected LayerLightSectionStorage(LightLayer lightType, LightChunkGetter chunkProvider, M lightData) {
        super(3, 16, 256);
        this.layer = lightType;
        this.chunkSource = chunkProvider;
        this.updatingSectionData = lightData;
        this.e_visible = lightData.copy(); // Paper - avoid copying light dat
        this.e_visible.disableCache(); // Paper - avoid copying light dat
    }

    protected boolean storingLightForSection(long sectionPos) {
        return this.getDataLayer(sectionPos, true) != null;
    }

    @Nullable
    protected DataLayer getDataLayer(long sectionPos, boolean cached) {
        // Paper start - avoid copying light data
        if (cached) {
            return this.getDataLayer(this.updatingSectionData, sectionPos);
        } else {
            synchronized (this.visibleUpdateLock) {
                return this.getDataLayer(this.e_visible, sectionPos);
            }
        }
        // Paper end - avoid copying light data
    }

    @Nullable
    protected DataLayer getDataLayer(M storage, long sectionPos) {
        return storage.getLayer(sectionPos);
    }

    @Nullable
    public DataLayer getDataLayerData(long sectionPos) {
        DataLayer dataLayer = this.queuedSections.get(sectionPos);
        return dataLayer != null ? dataLayer : this.getDataLayer(sectionPos, false);
    }

    protected abstract int getLightValue(long blockPos);

    protected int getStoredLevel(long blockPos) {
        long l = SectionPos.blockToSection(blockPos);
        DataLayer dataLayer = this.getDataLayer(l, true);
        return dataLayer.get(SectionPos.sectionRelative(BlockPos.getX(blockPos)), SectionPos.sectionRelative(BlockPos.getY(blockPos)), SectionPos.sectionRelative(BlockPos.getZ(blockPos)));
    }

    protected void setStoredLevel(long blockPos, int value) {
        long l = SectionPos.blockToSection(blockPos);
        if (this.changedSections.add(l)) {
            this.updatingSectionData.copyDataLayer(l);
        }

        DataLayer dataLayer = this.getDataLayer(l, true);
        dataLayer.set(SectionPos.sectionRelative(BlockPos.getX(blockPos)), SectionPos.sectionRelative(BlockPos.getY(blockPos)), SectionPos.sectionRelative(BlockPos.getZ(blockPos)), value);
        SectionPos.aroundAndAtBlockPos(blockPos, this.sectionsAffectedByLightUpdates::add);
    }

    @Override
    protected int getLevel(long id) {
        if (id == Long.MAX_VALUE) {
            return 2;
        } else if (this.dataSectionSet.contains(id)) {
            return 0;
        } else {
            return !this.toRemove.contains(id) && this.updatingSectionData.hasLayer(id) ? 1 : 2;
        }
    }

    @Override
    protected int getLevelFromSource(long id) {
        if (this.toMarkNoData.contains(id)) {
            return 2;
        } else {
            return !this.dataSectionSet.contains(id) && !this.toMarkData.contains(id) ? 2 : 0;
        }
    }

    @Override
    protected void setLevel(long id, int level) {
        int i = this.getLevel(id);
        if (i != 0 && level == 0) {
            this.dataSectionSet.add(id);
            this.toMarkData.remove(id);
        }

        if (i == 0 && level != 0) {
            this.dataSectionSet.remove(id);
            this.toMarkNoData.remove(id);
        }

        if (i >= 2 && level != 2) {
            if (this.toRemove.contains(id)) {
                this.toRemove.remove(id);
            } else {
                this.updatingSectionData.setLayer(id, this.createDataLayer(id));
                this.changedSections.add(id);
                this.onNodeAdded(id);
                int j = SectionPos.x(id);
                int k = SectionPos.y(id);
                int l = SectionPos.z(id);

                for(int m = -1; m <= 1; ++m) {
                    for(int n = -1; n <= 1; ++n) {
                        for(int o = -1; o <= 1; ++o) {
                            this.sectionsAffectedByLightUpdates.add(SectionPos.asLong(j + n, k + o, l + m));
                        }
                    }
                }
            }
        }

        if (i != 2 && level >= 2) {
            this.toRemove.add(id);
        }

        this.hasToRemove = !this.toRemove.isEmpty();
    }

    protected DataLayer createDataLayer(long sectionPos) {
        DataLayer dataLayer = this.queuedSections.get(sectionPos);
        return dataLayer != null ? dataLayer : new DataLayer();
    }

    protected void clearQueuedSectionBlocks(LayerLightEngine<?, ?> storage, long sectionPos) {
        if (storage.getQueueSize() != 0) {
            if (storage.getQueueSize() < 8192) {
                storage.removeIf((mx) -> {
                    return SectionPos.blockToSection(mx) == sectionPos;
                });
            } else {
                int i = SectionPos.sectionToBlockCoord(SectionPos.x(sectionPos));
                int j = SectionPos.sectionToBlockCoord(SectionPos.y(sectionPos));
                int k = SectionPos.sectionToBlockCoord(SectionPos.z(sectionPos));

                for(int l = 0; l < 16; ++l) {
                    for(int m = 0; m < 16; ++m) {
                        for(int n = 0; n < 16; ++n) {
                            long o = BlockPos.asLong(i + l, j + m, k + n);
                            storage.removeFromQueue(o);
                        }
                    }
                }

            }
        }
    }

    protected boolean hasInconsistencies() {
        return this.hasToRemove;
    }

    protected void markNewInconsistencies(LayerLightEngine<M, ?> lightProvider, boolean doSkylight, boolean skipEdgeLightPropagation) {
        if (this.hasInconsistencies() || !this.queuedSections.isEmpty()) {
            for(long l : this.toRemove) {
                this.clearQueuedSectionBlocks(lightProvider, l);
                DataLayer dataLayer = this.queuedSections.remove(l);
                DataLayer dataLayer2 = this.updatingSectionData.removeLayer(l);
                if (this.columnsToRetainQueuedDataFor.contains(SectionPos.getZeroNode(l))) {
                    if (dataLayer != null) {
                        this.queuedSections.put(l, dataLayer);
                    } else if (dataLayer2 != null) {
                        this.queuedSections.put(l, dataLayer2);
                    }
                }
            }

            this.updatingSectionData.clearCache();

            for(long m : this.toRemove) {
                this.onNodeRemoved(m);
            }

            this.toRemove.clear();
            this.hasToRemove = false;

            for(Entry<DataLayer> entry : this.queuedSections.long2ObjectEntrySet()) {
                long n = entry.getLongKey();
                if (this.storingLightForSection(n)) {
                    DataLayer dataLayer3 = entry.getValue();
                    if (this.updatingSectionData.getLayer(n) != dataLayer3) {
                        this.clearQueuedSectionBlocks(lightProvider, n);
                        this.updatingSectionData.setLayer(n, dataLayer3);
                        this.changedSections.add(n);
                    }
                }
            }

            this.updatingSectionData.clearCache();
            if (!skipEdgeLightPropagation) {
                for(long o : this.queuedSections.keySet()) {
                    this.checkEdgesForSection(lightProvider, o);
                }
            } else {
                for(long p : this.untrustedSections) {
                    this.checkEdgesForSection(lightProvider, p);
                }
            }

            this.untrustedSections.clear();
            ObjectIterator<Entry<DataLayer>> objectIterator = this.queuedSections.long2ObjectEntrySet().iterator();

            while(objectIterator.hasNext()) {
                Entry<DataLayer> entry2 = objectIterator.next();
                long q = entry2.getLongKey();
                if (this.storingLightForSection(q)) {
                    objectIterator.remove();
                }
            }

        }
    }

    private void checkEdgesForSection(LayerLightEngine<M, ?> lightProvider, long sectionPos) {
        if (this.storingLightForSection(sectionPos)) {
            int i = SectionPos.sectionToBlockCoord(SectionPos.x(sectionPos));
            int j = SectionPos.sectionToBlockCoord(SectionPos.y(sectionPos));
            int k = SectionPos.sectionToBlockCoord(SectionPos.z(sectionPos));

            for(Direction direction : DIRECTIONS) {
                long l = SectionPos.offset(sectionPos, direction);
                if (!this.queuedSections.containsKey(l) && this.storingLightForSection(l)) {
                    for(int m = 0; m < 16; ++m) {
                        for(int n = 0; n < 16; ++n) {
                            long o;
                            long p;
                            switch(direction) {
                            case DOWN:
                                o = BlockPos.asLong(i + n, j, k + m);
                                p = BlockPos.asLong(i + n, j - 1, k + m);
                                break;
                            case UP:
                                o = BlockPos.asLong(i + n, j + 16 - 1, k + m);
                                p = BlockPos.asLong(i + n, j + 16, k + m);
                                break;
                            case NORTH:
                                o = BlockPos.asLong(i + m, j + n, k);
                                p = BlockPos.asLong(i + m, j + n, k - 1);
                                break;
                            case SOUTH:
                                o = BlockPos.asLong(i + m, j + n, k + 16 - 1);
                                p = BlockPos.asLong(i + m, j + n, k + 16);
                                break;
                            case WEST:
                                o = BlockPos.asLong(i, j + m, k + n);
                                p = BlockPos.asLong(i - 1, j + m, k + n);
                                break;
                            default:
                                o = BlockPos.asLong(i + 16 - 1, j + m, k + n);
                                p = BlockPos.asLong(i + 16, j + m, k + n);
                            }

                            lightProvider.checkEdge(o, p, lightProvider.computeLevelFromNeighbor(o, p, lightProvider.getLevel(o)), false);
                            lightProvider.checkEdge(p, o, lightProvider.computeLevelFromNeighbor(p, o, lightProvider.getLevel(p)), false);
                        }
                    }
                }
            }

        }
    }

    protected void onNodeAdded(long sectionPos) {
    }

    protected void onNodeRemoved(long sectionPos) {
    }

    protected void enableLightSources(long columnPos, boolean enabled) {
    }

    public void retainData(long sectionPos, boolean retain) {
        if (retain) {
            this.columnsToRetainQueuedDataFor.add(sectionPos);
        } else {
            this.columnsToRetainQueuedDataFor.remove(sectionPos);
        }

    }

    protected void queueSectionData(long sectionPos, @Nullable DataLayer array, boolean nonEdge) {
        if (array != null) {
            this.queuedSections.put(sectionPos, array);
            if (!nonEdge) {
                this.untrustedSections.add(sectionPos);
            }
        } else {
            this.queuedSections.remove(sectionPos);
        }

    }

    protected void updateSectionStatus(long sectionPos, boolean notReady) {
        boolean bl = this.dataSectionSet.contains(sectionPos);
        if (!bl && !notReady) {
            this.toMarkData.add(sectionPos);
            this.checkEdge(Long.MAX_VALUE, sectionPos, 0, true);
        }

        if (bl && notReady) {
            this.toMarkNoData.add(sectionPos);
            this.checkEdge(Long.MAX_VALUE, sectionPos, 2, false);
        }

    }

    protected void runAllUpdates() {
        if (this.hasWork()) {
            this.runUpdates(Integer.MAX_VALUE);
        }

    }

    protected void swapSectionMap() {
        if (!this.changedSections.isEmpty()) {
            synchronized (this.visibleUpdateLock) { // Paper - avoid copying light data
            M dataLayerStorageMap = this.updatingSectionData.copy();
            dataLayerStorageMap.disableCache();
            this.e_visible = dataLayerStorageMap; // Paper - avoid copying light data
            } // Paper - avoid copying light data
            this.changedSections.clear();
        }

        if (!this.sectionsAffectedByLightUpdates.isEmpty()) {
            LongIterator longIterator = this.sectionsAffectedByLightUpdates.iterator();

            while(longIterator.hasNext()) {
                long l = longIterator.nextLong();
                this.chunkSource.onLightUpdate(this.layer, SectionPos.of(l));
            }

            this.sectionsAffectedByLightUpdates.clear();
        }

    }
}
