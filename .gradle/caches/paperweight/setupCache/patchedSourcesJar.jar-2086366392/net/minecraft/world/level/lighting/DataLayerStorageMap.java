package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.level.chunk.DataLayer;

public abstract class DataLayerStorageMap<M extends DataLayerStorageMap<M>> {
    private static final int CACHE_SIZE = 2;
    private final long[] lastSectionKeys = new long[2];
    private final DataLayer[] lastSections = new DataLayer[2];
    private boolean cacheEnabled;
    protected final com.destroystokyo.paper.util.map.QueuedChangesMapLong2Object<DataLayer> data; // Paper - avoid copying light data
    protected final boolean isVisible; // Paper - avoid copying light data
    java.util.function.Function<Long, DataLayer> lookup; // Paper - faster branchless lookup

    // Paper start - avoid copying light data
    protected DataLayerStorageMap(com.destroystokyo.paper.util.map.QueuedChangesMapLong2Object<DataLayer> data, boolean isVisible) {
        if (isVisible) {
            data.performUpdatesLockMap();
        }
        this.data = data;
        this.isVisible = isVisible;
        if (isVisible) {
            lookup = data::getVisibleAsync;
        } else {
            lookup = data::getUpdating;
        }
        // Paper end - avoid copying light data
        this.clearCache();
        this.cacheEnabled = true;
    }

    public abstract M copy();

    public void copyDataLayer(long pos) {
        if (this.isVisible) { throw new IllegalStateException("writing to visible data"); } // Paper - avoid copying light data
        this.data.queueUpdate(pos, ((DataLayer) this.data.getUpdating(pos)).copy()); // Paper - avoid copying light data
        this.clearCache();
    }

    public boolean hasLayer(long chunkPos) {
        return lookup.apply(chunkPos) != null; // Paper - avoid copying light data
    }

    @Nullable
    public final DataLayer getLayer(long chunkPos) { // Paper - final
        if (this.cacheEnabled) {
            for(int i = 0; i < 2; ++i) {
                if (chunkPos == this.lastSectionKeys[i]) {
                    return this.lastSections[i];
                }
            }
        }

        DataLayer dataLayer = lookup.apply(chunkPos); // Paper - avoid copying light data
        if (dataLayer == null) {
            return null;
        } else {
            if (this.cacheEnabled) {
                for(int j = 1; j > 0; --j) {
                    this.lastSectionKeys[j] = this.lastSectionKeys[j - 1];
                    this.lastSections[j] = this.lastSections[j - 1];
                }

                this.lastSectionKeys[0] = chunkPos;
                this.lastSections[0] = dataLayer;
            }

            return dataLayer;
        }
    }

    @Nullable
    public DataLayer removeLayer(long chunkPos) {
        if (this.isVisible) { throw new IllegalStateException("writing to visible data"); } // Paper - avoid copying light data
        return (DataLayer) this.data.queueRemove(chunkPos); // Paper - avoid copying light data
    }

    public void setLayer(long pos, DataLayer data) {
        if (this.isVisible) { throw new IllegalStateException("writing to visible data"); } // Paper - avoid copying light data
        this.data.queueUpdate(pos, data); // Paper - avoid copying light data
    }

    public void clearCache() {
        for(int i = 0; i < 2; ++i) {
            this.lastSectionKeys[i] = Long.MAX_VALUE;
            this.lastSections[i] = null;
        }

    }

    public void disableCache() {
        this.cacheEnabled = false;
    }
}
