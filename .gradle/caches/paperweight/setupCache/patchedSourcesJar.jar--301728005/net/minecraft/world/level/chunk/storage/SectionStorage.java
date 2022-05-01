package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.slf4j.Logger;

public class SectionStorage<R> extends RegionFileStorage implements AutoCloseable { // Paper - nuke IOWorker
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SECTIONS_TAG = "Sections";
    // Paper - remove mojang I/O thread
    private final Long2ObjectMap<Optional<R>> storage = new Long2ObjectOpenHashMap<>();
    public final LongLinkedOpenHashSet dirty = new LongLinkedOpenHashSet();
    private final Function<Runnable, Codec<R>> codec;
    private final Function<Runnable, R> factory;
    private final DataFixer fixerUpper;
    private final DataFixTypes type;
    protected final LevelHeightAccessor levelHeightAccessor;

    public SectionStorage(Path path, Function<Runnable, Codec<R>> codecFactory, Function<Runnable, R> factory, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean dsync, LevelHeightAccessor world) {
        super(path, dsync);
        this.codec = codecFactory;
        this.factory = factory;
        this.fixerUpper = dataFixer;
        this.type = dataFixTypes;
        this.levelHeightAccessor = world;
        // Paper - remove mojang I/O thread
    }

    // Paper start - actually unload POI data
    public void unloadData(long coordinate) {
        ChunkPos chunkPos = new ChunkPos(coordinate);
        this.flush(chunkPos);

        Long2ObjectMap<Optional<R>> data = this.storage;
        int before = data.size();

        for (int section = this.levelHeightAccessor.getMinSection(); section < this.levelHeightAccessor.getMaxSection(); ++section) {
            data.remove(SectionPos.asLong(chunkPos.x, section, chunkPos.z));
        }

        if (before != data.size()) {
            this.onUnload(coordinate);
        }
    }

    protected void onUnload(long coordinate) {}

    public boolean isEmpty(long coordinate) {
        Long2ObjectMap<Optional<R>> data = this.storage;
        int x = net.minecraft.server.MCUtil.getCoordinateX(coordinate);
        int z = net.minecraft.server.MCUtil.getCoordinateZ(coordinate);
        for (int section = this.levelHeightAccessor.getMinSection(); section < this.levelHeightAccessor.getMaxSection(); ++section) {
            Optional<R> optional = data.get(SectionPos.asLong(x, section, z));
            if (optional != null && optional.orElse(null) != null) {
                return false;
            }
        }

        return true;
    }
    // Paper end - actually unload POI data

    protected void tick(BooleanSupplier shouldKeepTicking) {
        while(this.hasWork() && shouldKeepTicking.getAsBoolean()) {
            ChunkPos chunkPos = SectionPos.of(this.dirty.firstLong()).chunk();
            this.writeColumn(chunkPos);
        }

    }

    public boolean hasWork() {
        return !this.dirty.isEmpty();
    }

    @Nullable
    public Optional<R> get(long pos) { // Paper - public
        return this.storage.get(pos);
    }

    public Optional<R> getOrLoad(long pos) { // Paper - public
        if (this.outsideStoredRange(pos)) {
            return Optional.empty();
        } else {
            Optional<R> optional = this.get(pos);
            if (optional != null) {
                return optional;
            } else {
                this.readColumn(SectionPos.of(pos).chunk());
                optional = this.get(pos);
                if (optional == null) {
                    throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
                } else {
                    return optional;
                }
            }
        }
    }

    protected boolean outsideStoredRange(long pos) {
        int i = SectionPos.sectionToBlockCoord(SectionPos.y(pos));
        return this.levelHeightAccessor.isOutsideBuildHeight(i);
    }

    protected R getOrCreate(long pos) {
        if (this.outsideStoredRange(pos)) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("sectionPos out of bounds"));
        } else {
            Optional<R> optional = this.getOrLoad(pos);
            if (optional.isPresent()) {
                return optional.get();
            } else {
                R object = this.factory.apply(() -> {
                    this.setDirty(pos);
                });
                this.storage.put(pos, Optional.of(object));
                return object;
            }
        }
    }

    private void readColumn(ChunkPos chunkPos) {
        // Paper start - expose function to load in data
       this.loadInData(chunkPos, this.tryRead(chunkPos));
    }
    public void loadInData(ChunkPos chunkPos, CompoundTag compound) {
        this.readColumn(chunkPos, NbtOps.INSTANCE, compound);
        // Paper end - expose function to load in data
    }

    @Nullable
    private CompoundTag tryRead(ChunkPos pos) {
        try {
            return this.read(pos); // Paper - nuke IOWorker
        } catch (IOException var3) {
            LOGGER.error("Error reading chunk {} data from disk", pos, var3);
            return null;
        }
    }

    private <T> void readColumn(ChunkPos pos, DynamicOps<T> ops, @Nullable T data) {
        if (data == null) {
            for(int i = this.levelHeightAccessor.getMinSection(); i < this.levelHeightAccessor.getMaxSection(); ++i) {
                this.storage.put(getKey(pos, i), Optional.empty());
            }
        } else {
            Dynamic<T> dynamic = new Dynamic<>(ops, data);
            int j = getVersion(dynamic);
            int k = SharedConstants.getCurrentVersion().getWorldVersion();
            boolean bl = j != k;
            // Paper start - route to new converter system
            Dynamic<T> dynamic2;
            if (this.type.getType() == net.minecraft.util.datafix.fixes.References.POI_CHUNK) {
                dynamic2 = new Dynamic<>(dynamic.getOps(), (T)ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.POI_CHUNK, (CompoundTag)dynamic.getValue(), j, k));
            } else {
                dynamic2 = this.fixerUpper.update(this.type.getType(), dynamic, j, k);
            }
            // Paper end - route to new converter system
            OptionalDynamic<T> optionalDynamic = dynamic2.get("Sections");

            for(int l = this.levelHeightAccessor.getMinSection(); l < this.levelHeightAccessor.getMaxSection(); ++l) {
                long m = getKey(pos, l);
                Optional<R> optional = optionalDynamic.get(Integer.toString(l)).result().flatMap((dynamicx) -> {
                    return this.codec.apply(() -> {
                        this.setDirty(m);
                    }).parse(dynamicx).resultOrPartial(LOGGER::error);
                });
                this.storage.put(m, optional);
                optional.ifPresent((sections) -> {
                    this.onSectionLoad(m);
                    if (bl) {
                        this.setDirty(m);
                    }

                });
            }
        }
        if (this instanceof net.minecraft.world.entity.ai.village.poi.PoiManager) { ((net.minecraft.world.entity.ai.village.poi.PoiManager)this).queueUnload(pos.longKey, net.minecraft.server.MinecraftServer.currentTickLong + 1); } // Paper - unload POI data

    }

    private void writeColumn(ChunkPos chunkPos) {
        Dynamic<Tag> dynamic = this.writeColumn(chunkPos, NbtOps.INSTANCE);
        Tag tag = dynamic.getValue();
        if (tag instanceof CompoundTag) {
            try { this.write(chunkPos, (CompoundTag)tag); } catch (IOException ioexception) { SectionStorage.LOGGER.error("Error writing data to disk", ioexception); } // Paper - nuke IOWorker
        } else {
            LOGGER.error("Expected compound tag, got {}", (Object)tag);
        }

    }

    // Paper start - internal get data function, copied from above
    private CompoundTag getDataInternal(ChunkPos chunkcoordintpair) {
        Dynamic<Tag> dynamic = this.writeColumn(chunkcoordintpair, NbtOps.INSTANCE);
        Tag nbtbase = (Tag) dynamic.getValue();

        if (nbtbase instanceof CompoundTag) {
            return (CompoundTag)nbtbase;
        } else {
            SectionStorage.LOGGER.error("Expected compound tag, got {}", nbtbase);
        }
        return null;
    }
    // Paper end
    private <T> Dynamic<T> writeColumn(ChunkPos chunkPos, DynamicOps<T> dynamicOps) {
        Map<T, T> map = Maps.newHashMap();

        for(int i = this.levelHeightAccessor.getMinSection(); i < this.levelHeightAccessor.getMaxSection(); ++i) {
            long l = getKey(chunkPos, i);
            this.dirty.remove(l);
            Optional<R> optional = this.storage.get(l);
            if (optional != null && optional.isPresent()) {
                DataResult<T> dataResult = this.codec.apply(() -> {
                    this.setDirty(l);
                }).encodeStart(dynamicOps, optional.get());
                String string = Integer.toString(i);
                dataResult.resultOrPartial(LOGGER::error).ifPresent((object) -> {
                    map.put(dynamicOps.createString(string), object);
                });
            }
        }

        return new Dynamic<>(dynamicOps, dynamicOps.createMap(ImmutableMap.of(dynamicOps.createString("Sections"), dynamicOps.createMap(map), dynamicOps.createString("DataVersion"), dynamicOps.createInt(SharedConstants.getCurrentVersion().getWorldVersion()))));
    }

    private static long getKey(ChunkPos chunkPos, int y) {
        return SectionPos.asLong(chunkPos.x, y, chunkPos.z);
    }

    protected void onSectionLoad(long pos) {
    }

    protected void setDirty(long pos) {
        Optional<R> optional = this.storage.get(pos);
        if (optional != null && optional.isPresent()) {
            this.dirty.add(pos);
        } else {
            LOGGER.warn("No data for position: {}", (Object)SectionPos.of(pos));
        }
    }

    private static int getVersion(Dynamic<?> dynamic) {
        return dynamic.get("DataVersion").asInt(1945);
    }

    public void flush(ChunkPos pos) {
        if (this.hasWork()) {
            for(int i = this.levelHeightAccessor.getMinSection(); i < this.levelHeightAccessor.getMaxSection(); ++i) {
                long l = getKey(pos, i);
                if (this.dirty.contains(l)) {
                    this.writeColumn(pos);
                    return;
                }
            }
        }

    }

    @Override
    public void close() throws IOException {
        //this.worker.close(); // Paper - nuke I/O worker
    }

    // Paper start - get data function
    public CompoundTag getData(ChunkPos chunkcoordintpair) {
        // Note: Copied from above
        // This is checking if the data needs to be written, then it builds it later in getDataInternal(ChunkCoordIntPair)
        if (!this.dirty.isEmpty()) {
            for (int i = this.levelHeightAccessor.getMinSection(); i < this.levelHeightAccessor.getMaxSection(); ++i) {
                long j = SectionPos.of(chunkcoordintpair, i).asLong();

                if (this.dirty.contains(j)) {
                    return this.getDataInternal(chunkcoordintpair);
                }
            }
        }
        return null;
    }
    // Paper end
}
