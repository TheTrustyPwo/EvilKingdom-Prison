package net.minecraft.world.level.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;

public class PalettedContainer<T> implements PaletteResize<T> {
    private static final int MIN_PALETTE_BITS = 0;
    private final PaletteResize<T> dummyPaletteResize = (newSize, added) -> {
        return 0;
    };
    public final IdMap<T> registry;
    private volatile PalettedContainer.Data<T> data;
    private final PalettedContainer.Strategy strategy;
    private final ThreadingDetector threadingDetector = new ThreadingDetector("PalettedContainer");

    public void acquire() {
        this.threadingDetector.checkAndLock();
    }

    public void release() {
        this.threadingDetector.checkAndUnlock();
    }

    public static <T> Codec<PalettedContainer<T>> codec(IdMap<T> idList, Codec<T> entryCodec, PalettedContainer.Strategy provider, T object) {
        return RecordCodecBuilder.create((instance) -> {
            return instance.group(entryCodec.mapResult(ExtraCodecs.orElsePartial(object)).listOf().fieldOf("palette").forGetter(PalettedContainer.DiscData::paletteEntries), Codec.LONG_STREAM.optionalFieldOf("data").forGetter(PalettedContainer.DiscData::storage)).apply(instance, PalettedContainer.DiscData::new);
        }).comapFlatMap((serialized) -> {
            return read(idList, provider, serialized);
        }, (container) -> {
            return container.write(idList, provider);
        });
    }

    public PalettedContainer(IdMap<T> idList, PalettedContainer.Strategy paletteProvider, PalettedContainer.Configuration<T> dataProvider, BitStorage storage, List<T> paletteEntries) {
        this.registry = idList;
        this.strategy = paletteProvider;
        this.data = new PalettedContainer.Data<>(dataProvider, storage, dataProvider.factory().create(dataProvider.bits(), idList, this, paletteEntries));
    }

    private PalettedContainer(IdMap<T> idList, PalettedContainer.Strategy paletteProvider, PalettedContainer.Data<T> data) {
        this.registry = idList;
        this.strategy = paletteProvider;
        this.data = data;
    }

    public PalettedContainer(IdMap<T> idList, T object, PalettedContainer.Strategy paletteProvider) {
        this.strategy = paletteProvider;
        this.registry = idList;
        this.data = this.createOrReuseData((PalettedContainer.Data<T>)null, 0);
        this.data.palette.idFor(object);
    }

    private PalettedContainer.Data<T> createOrReuseData(@Nullable PalettedContainer.Data<T> previousData, int bits) {
        PalettedContainer.Configuration<T> configuration = this.strategy.getConfiguration(this.registry, bits);
        return previousData != null && configuration.equals(previousData.configuration()) ? previousData : configuration.createData(this.registry, this, this.strategy.size());
    }

    @Override
    public int onResize(int newBits, T object) {
        PalettedContainer.Data<T> data = this.data;
        PalettedContainer.Data<T> data2 = this.createOrReuseData(data, newBits);
        data2.copyFrom(data.palette, data.storage);
        this.data = data2;
        return data2.palette.idFor(object);
    }

    public T getAndSet(int x, int y, int z, T value) {
        this.acquire();

        Object var5;
        try {
            var5 = this.getAndSet(this.strategy.getIndex(x, y, z), value);
        } finally {
            this.release();
        }

        return (T)var5;
    }

    public T getAndSetUnchecked(int x, int y, int z, T value) {
        return this.getAndSet(this.strategy.getIndex(x, y, z), value);
    }

    private T getAndSet(int index, T value) {
        int i = this.data.palette.idFor(value);
        int j = this.data.storage.getAndSet(index, i);
        return this.data.palette.valueFor(j);
    }

    public void set(int x, int y, int z, T value) {
        this.acquire();

        try {
            this.set(this.strategy.getIndex(x, y, z), value);
        } finally {
            this.release();
        }

    }

    private void set(int index, T value) {
        int i = this.data.palette.idFor(value);
        this.data.storage.set(index, i);
    }

    public T get(int x, int y, int z) {
        return this.get(this.strategy.getIndex(x, y, z));
    }

    protected T get(int index) {
        PalettedContainer.Data<T> data = this.data;
        return data.palette.valueFor(data.storage.get(index));
    }

    public void getAll(Consumer<T> consumer) {
        Palette<T> palette = this.data.palette();
        IntSet intSet = new IntArraySet();
        this.data.storage.getAll(intSet::add);
        intSet.forEach((id) -> {
            consumer.accept(palette.valueFor(id));
        });
    }

    public void read(FriendlyByteBuf buf) {
        this.acquire();

        try {
            int i = buf.readByte();
            PalettedContainer.Data<T> data = this.createOrReuseData(this.data, i);
            data.palette.read(buf);
            buf.readLongArray(data.storage.getRaw());
            this.data = data;
        } finally {
            this.release();
        }

    }

    public void write(FriendlyByteBuf buf) {
        this.acquire();

        try {
            this.data.write(buf);
        } finally {
            this.release();
        }

    }

    private static <T> DataResult<PalettedContainer<T>> read(IdMap<T> idList, PalettedContainer.Strategy provider, PalettedContainer.DiscData<T> serialized) {
        List<T> list = serialized.paletteEntries();
        int i = provider.size();
        int j = provider.calculateBitsForSerialization(idList, list.size());
        PalettedContainer.Configuration<T> configuration = provider.getConfiguration(idList, j);
        BitStorage bitStorage;
        if (j == 0) {
            bitStorage = new ZeroBitStorage(i);
        } else {
            Optional<LongStream> optional = serialized.storage();
            if (optional.isEmpty()) {
                return DataResult.error("Missing values for non-zero storage");
            }

            long[] ls = optional.get().toArray();

            try {
                if (configuration.factory() == PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY) {
                    Palette<T> palette = new HashMapPalette<>(idList, j, (ix, object) -> {
                        return 0;
                    }, list);
                    SimpleBitStorage simpleBitStorage = new SimpleBitStorage(j, i, ls);
                    int[] is = new int[i];
                    simpleBitStorage.unpack(is);
                    swapPalette(is, (ix) -> {
                        return idList.getId(palette.valueFor(ix));
                    });
                    bitStorage = new SimpleBitStorage(configuration.bits(), i, is);
                } else {
                    bitStorage = new SimpleBitStorage(configuration.bits(), i, ls);
                }
            } catch (SimpleBitStorage.InitializationException var13) {
                return DataResult.error("Failed to read PalettedContainer: " + var13.getMessage());
            }
        }

        return DataResult.success(new PalettedContainer<>(idList, provider, configuration, bitStorage, list));
    }

    private PalettedContainer.DiscData<T> write(IdMap<T> idList, PalettedContainer.Strategy provider) {
        this.acquire();

        PalettedContainer.DiscData var12;
        try {
            HashMapPalette<T> hashMapPalette = new HashMapPalette<>(idList, this.data.storage.getBits(), this.dummyPaletteResize);
            int i = provider.size();
            int[] is = new int[i];
            this.data.storage.unpack(is);
            swapPalette(is, (id) -> {
                return hashMapPalette.idFor(this.data.palette.valueFor(id));
            });
            int j = provider.calculateBitsForSerialization(idList, hashMapPalette.getSize());
            Optional<LongStream> optional;
            if (j != 0) {
                SimpleBitStorage simpleBitStorage = new SimpleBitStorage(j, i, is);
                optional = Optional.of(Arrays.stream(simpleBitStorage.getRaw()));
            } else {
                optional = Optional.empty();
            }

            var12 = new PalettedContainer.DiscData<>(hashMapPalette.getEntries(), optional);
        } finally {
            this.release();
        }

        return var12;
    }

    private static <T> void swapPalette(int[] is, IntUnaryOperator intUnaryOperator) {
        int i = -1;
        int j = -1;

        for(int k = 0; k < is.length; ++k) {
            int l = is[k];
            if (l != i) {
                i = l;
                j = intUnaryOperator.applyAsInt(l);
            }

            is[k] = j;
        }

    }

    public int getSerializedSize() {
        return this.data.getSerializedSize();
    }

    public boolean maybeHas(Predicate<T> predicate) {
        return this.data.palette.maybeHas(predicate);
    }

    public PalettedContainer<T> copy() {
        return new PalettedContainer<>(this.registry, this.strategy, new PalettedContainer.Data<>(this.data.configuration(), this.data.storage().copy(), this.data.palette().copy()));
    }

    public void count(PalettedContainer.CountConsumer<T> counter) {
        if (this.data.palette.getSize() == 1) {
            counter.accept(this.data.palette.valueFor(0), this.data.storage.getSize());
        } else {
            Int2IntOpenHashMap int2IntOpenHashMap = new Int2IntOpenHashMap();
            this.data.storage.getAll((key) -> {
                int2IntOpenHashMap.addTo(key, 1);
            });
            int2IntOpenHashMap.int2IntEntrySet().forEach((entry) -> {
                counter.accept(this.data.palette.valueFor(entry.getIntKey()), entry.getIntValue());
            });
        }
    }

    static record Configuration<T>(Palette.Factory factory, int bits) {
        public PalettedContainer.Data<T> createData(IdMap<T> idList, PaletteResize<T> listener, int size) {
            BitStorage bitStorage = (BitStorage)(this.bits == 0 ? new ZeroBitStorage(size) : new SimpleBitStorage(this.bits, size));
            Palette<T> palette = this.factory.create(this.bits, idList, listener, List.of());
            return new PalettedContainer.Data<>(this, bitStorage, palette);
        }
    }

    @FunctionalInterface
    public interface CountConsumer<T> {
        void accept(T object, int count);
    }

    static record Data<T>(PalettedContainer.Configuration<T> configuration, BitStorage storage, Palette<T> palette) {
        public void copyFrom(Palette<T> palette, BitStorage storage) {
            for(int i = 0; i < storage.getSize(); ++i) {
                T object = palette.valueFor(storage.get(i));
                this.storage.set(i, this.palette.idFor(object));
            }

        }

        public int getSerializedSize() {
            return 1 + this.palette.getSerializedSize() + FriendlyByteBuf.getVarIntSize(this.storage.getSize()) + this.storage.getRaw().length * 8;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeByte(this.storage.getBits());
            this.palette.write(buf);
            buf.writeLongArray(this.storage.getRaw());
        }
    }

    static record DiscData<T>(List<T> paletteEntries, Optional<LongStream> storage) {
    }

    public abstract static class Strategy {
        public static final Palette.Factory SINGLE_VALUE_PALETTE_FACTORY = SingleValuePalette::create;
        public static final Palette.Factory LINEAR_PALETTE_FACTORY = LinearPalette::create;
        public static final Palette.Factory HASHMAP_PALETTE_FACTORY = HashMapPalette::create;
        static final Palette.Factory GLOBAL_PALETTE_FACTORY = GlobalPalette::create;
        public static final PalettedContainer.Strategy SECTION_STATES = new PalettedContainer.Strategy(4) {
            @Override
            public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> idList, int bits) {
                PalettedContainer.Configuration var10000;
                switch(bits) {
                case 0:
                    var10000 = new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, bits);
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                    var10000 = new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, 4);
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                    var10000 = new PalettedContainer.Configuration(HASHMAP_PALETTE_FACTORY, bits);
                    break;
                default:
                    var10000 = new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(idList.size()));
                }

                return var10000;
            }
        };
        public static final PalettedContainer.Strategy SECTION_BIOMES = new PalettedContainer.Strategy(2) {
            @Override
            public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> idList, int bits) {
                PalettedContainer.Configuration var10000;
                switch(bits) {
                case 0:
                    var10000 = new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, bits);
                    break;
                case 1:
                case 2:
                case 3:
                    var10000 = new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, bits);
                    break;
                default:
                    var10000 = new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(idList.size()));
                }

                return var10000;
            }
        };
        private final int sizeBits;

        Strategy(int edgeBits) {
            this.sizeBits = edgeBits;
        }

        public int size() {
            return 1 << this.sizeBits * 3;
        }

        public int getIndex(int x, int y, int z) {
            return (y << this.sizeBits | z) << this.sizeBits | x;
        }

        public abstract <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> idList, int bits);

        <A> int calculateBitsForSerialization(IdMap<A> idList, int size) {
            int i = Mth.ceillog2(size);
            PalettedContainer.Configuration<A> configuration = this.getConfiguration(idList, i);
            return configuration.factory() == GLOBAL_PALETTE_FACTORY ? i : configuration.bits();
        }
    }
}
