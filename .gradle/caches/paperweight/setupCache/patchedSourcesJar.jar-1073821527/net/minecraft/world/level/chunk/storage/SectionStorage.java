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

public class SectionStorage<R> implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SECTIONS_TAG = "Sections";
    private final IOWorker worker;
    private final Long2ObjectMap<Optional<R>> storage = new Long2ObjectOpenHashMap<>();
    public final LongLinkedOpenHashSet dirty = new LongLinkedOpenHashSet();
    private final Function<Runnable, Codec<R>> codec;
    private final Function<Runnable, R> factory;
    private final DataFixer fixerUpper;
    private final DataFixTypes type;
    protected final LevelHeightAccessor levelHeightAccessor;

    public SectionStorage(Path path, Function<Runnable, Codec<R>> codecFactory, Function<Runnable, R> factory, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean dsync, LevelHeightAccessor world) {
        this.codec = codecFactory;
        this.factory = factory;
        this.fixerUpper = dataFixer;
        this.type = dataFixTypes;
        this.levelHeightAccessor = world;
        this.worker = new IOWorker(path, dsync, path.getFileName().toString());
    }

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
    protected Optional<R> get(long pos) {
        return this.storage.get(pos);
    }

    protected Optional<R> getOrLoad(long pos) {
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
        this.readColumn(chunkPos, NbtOps.INSTANCE, this.tryRead(chunkPos));
    }

    @Nullable
    private CompoundTag tryRead(ChunkPos pos) {
        try {
            return this.worker.load(pos);
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
            Dynamic<T> dynamic2 = this.fixerUpper.update(this.type.getType(), dynamic, j, k);
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

    }

    private void writeColumn(ChunkPos chunkPos) {
        Dynamic<Tag> dynamic = this.writeColumn(chunkPos, NbtOps.INSTANCE);
        Tag tag = dynamic.getValue();
        if (tag instanceof CompoundTag) {
            this.worker.store(chunkPos, (CompoundTag)tag);
        } else {
            LOGGER.error("Expected compound tag, got {}", (Object)tag);
        }

    }

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
        this.worker.close();
    }
}
