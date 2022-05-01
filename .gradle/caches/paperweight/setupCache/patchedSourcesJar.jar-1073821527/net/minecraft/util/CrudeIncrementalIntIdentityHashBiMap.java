package net.minecraft.util;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;

public class CrudeIncrementalIntIdentityHashBiMap<K> implements IdMap<K> {
    private static final int NOT_FOUND = -1;
    private static final Object EMPTY_SLOT = null;
    private static final float LOADFACTOR = 0.8F;
    private K[] keys;
    private int[] values;
    private K[] byId;
    private int nextId;
    private int size;

    private CrudeIncrementalIntIdentityHashBiMap(int size) {
        this.keys = (K[])(new Object[size]);
        this.values = new int[size];
        this.byId = (K[])(new Object[size]);
    }

    private CrudeIncrementalIntIdentityHashBiMap(K[] objects, int[] is, K[] objects2, int i, int j) {
        this.keys = objects;
        this.values = is;
        this.byId = objects2;
        this.nextId = i;
        this.size = j;
    }

    public static <A> CrudeIncrementalIntIdentityHashBiMap<A> create(int expectedSize) {
        return new CrudeIncrementalIntIdentityHashBiMap<>((int)((float)expectedSize / 0.8F));
    }

    @Override
    public int getId(@Nullable K value) {
        return this.getValue(this.indexOf(value, this.hash(value)));
    }

    @Nullable
    @Override
    public K byId(int index) {
        return (K)(index >= 0 && index < this.byId.length ? this.byId[index] : null);
    }

    private int getValue(int index) {
        return index == -1 ? -1 : this.values[index];
    }

    public boolean contains(K value) {
        return this.getId(value) != -1;
    }

    public boolean contains(int index) {
        return this.byId(index) != null;
    }

    public int add(K value) {
        int i = this.nextId();
        this.addMapping(value, i);
        return i;
    }

    private int nextId() {
        while(this.nextId < this.byId.length && this.byId[this.nextId] != null) {
            ++this.nextId;
        }

        return this.nextId;
    }

    private void grow(int newSize) {
        K[] objects = this.keys;
        int[] is = this.values;
        CrudeIncrementalIntIdentityHashBiMap<K> crudeIncrementalIntIdentityHashBiMap = new CrudeIncrementalIntIdentityHashBiMap<>(newSize);

        for(int i = 0; i < objects.length; ++i) {
            if (objects[i] != null) {
                crudeIncrementalIntIdentityHashBiMap.addMapping(objects[i], is[i]);
            }
        }

        this.keys = crudeIncrementalIntIdentityHashBiMap.keys;
        this.values = crudeIncrementalIntIdentityHashBiMap.values;
        this.byId = crudeIncrementalIntIdentityHashBiMap.byId;
        this.nextId = crudeIncrementalIntIdentityHashBiMap.nextId;
        this.size = crudeIncrementalIntIdentityHashBiMap.size;
    }

    public void addMapping(K value, int id) {
        int i = Math.max(id, this.size + 1);
        if ((float)i >= (float)this.keys.length * 0.8F) {
            int j;
            for(j = this.keys.length << 1; j < id; j <<= 1) {
            }

            this.grow(j);
        }

        int k = this.findEmpty(this.hash(value));
        this.keys[k] = value;
        this.values[k] = id;
        this.byId[id] = value;
        ++this.size;
        if (id == this.nextId) {
            ++this.nextId;
        }

    }

    private int hash(@Nullable K value) {
        return (Mth.murmurHash3Mixer(System.identityHashCode(value)) & Integer.MAX_VALUE) % this.keys.length;
    }

    private int indexOf(@Nullable K value, int id) {
        for(int i = id; i < this.keys.length; ++i) {
            if (this.keys[i] == value) {
                return i;
            }

            if (this.keys[i] == EMPTY_SLOT) {
                return -1;
            }
        }

        for(int j = 0; j < id; ++j) {
            if (this.keys[j] == value) {
                return j;
            }

            if (this.keys[j] == EMPTY_SLOT) {
                return -1;
            }
        }

        return -1;
    }

    private int findEmpty(int size) {
        for(int i = size; i < this.keys.length; ++i) {
            if (this.keys[i] == EMPTY_SLOT) {
                return i;
            }
        }

        for(int j = 0; j < size; ++j) {
            if (this.keys[j] == EMPTY_SLOT) {
                return j;
            }
        }

        throw new RuntimeException("Overflowed :(");
    }

    @Override
    public Iterator<K> iterator() {
        return Iterators.filter(Iterators.forArray(this.byId), Predicates.notNull());
    }

    public void clear() {
        Arrays.fill(this.keys, (Object)null);
        Arrays.fill(this.byId, (Object)null);
        this.nextId = 0;
        this.size = 0;
    }

    @Override
    public int size() {
        return this.size;
    }

    public CrudeIncrementalIntIdentityHashBiMap<K> copy() {
        return new CrudeIncrementalIntIdentityHashBiMap<>((K[])((Object[])this.keys.clone()), (int[])this.values.clone(), (K[])((Object[])this.byId.clone()), this.nextId, this.size);
    }
}
