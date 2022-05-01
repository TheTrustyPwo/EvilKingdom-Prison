package net.minecraft.util;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

public class SortedArraySet<T> extends AbstractSet<T> {
    private static final int DEFAULT_INITIAL_CAPACITY = 10;
    private final Comparator<T> comparator;
    T[] contents;
    int size;

    private SortedArraySet(int initialCapacity, Comparator<T> comparator) {
        this.comparator = comparator;
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity (" + initialCapacity + ") is negative");
        } else {
            this.contents = (T[])castRawArray(new Object[initialCapacity]);
        }
    }

    public static <T extends Comparable<T>> SortedArraySet<T> create() {
        return create(10);
    }

    public static <T extends Comparable<T>> SortedArraySet<T> create(int initialCapacity) {
        return new SortedArraySet<>(initialCapacity, Comparator.naturalOrder());
    }

    public static <T> SortedArraySet<T> create(Comparator<T> comparator) {
        return create(comparator, 10);
    }

    public static <T> SortedArraySet<T> create(Comparator<T> comparator, int initialCapacity) {
        return new SortedArraySet<>(initialCapacity, comparator);
    }

    private static <T> T[] castRawArray(Object[] array) {
        return (T[])array;
    }

    private int findIndex(T object) {
        return Arrays.binarySearch(this.contents, 0, this.size, object, this.comparator);
    }

    private static int getInsertionPosition(int binarySearchResult) {
        return -binarySearchResult - 1;
    }

    @Override
    public boolean add(T object) {
        int i = this.findIndex(object);
        if (i >= 0) {
            return false;
        } else {
            int j = getInsertionPosition(i);
            this.addInternal(object, j);
            return true;
        }
    }

    private void grow(int minCapacity) {
        if (minCapacity > this.contents.length) {
            if (this.contents != ObjectArrays.DEFAULT_EMPTY_ARRAY) {
                minCapacity = (int)Math.max(Math.min((long)this.contents.length + (long)(this.contents.length >> 1), 2147483639L), (long)minCapacity);
            } else if (minCapacity < 10) {
                minCapacity = 10;
            }

            Object[] objects = new Object[minCapacity];
            System.arraycopy(this.contents, 0, objects, 0, this.size);
            this.contents = (T[])castRawArray(objects);
        }
    }

    private void addInternal(T object, int index) {
        this.grow(this.size + 1);
        if (index != this.size) {
            System.arraycopy(this.contents, index, this.contents, index + 1, this.size - index);
        }

        this.contents[index] = object;
        ++this.size;
    }

    void removeInternal(int index) {
        --this.size;
        if (index != this.size) {
            System.arraycopy(this.contents, index + 1, this.contents, index, this.size - index);
        }

        this.contents[this.size] = null;
    }

    private T getInternal(int index) {
        return this.contents[index];
    }

    public T addOrGet(T object) {
        int i = this.findIndex(object);
        if (i >= 0) {
            return this.getInternal(i);
        } else {
            this.addInternal(object, getInsertionPosition(i));
            return object;
        }
    }

    @Override
    public boolean remove(Object object) {
        int i = this.findIndex((T)object);
        if (i >= 0) {
            this.removeInternal(i);
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    public T get(T object) {
        int i = this.findIndex(object);
        return (T)(i >= 0 ? this.getInternal(i) : null);
    }

    public T first() {
        return this.getInternal(0);
    }

    public T last() {
        return this.getInternal(this.size - 1);
    }

    @Override
    public boolean contains(Object object) {
        int i = this.findIndex((T)object);
        return i >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new SortedArraySet.ArrayIterator();
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Object[] toArray() {
        return this.contents.clone();
    }

    @Override
    public <U> U[] toArray(U[] objects) {
        if (objects.length < this.size) {
            return (U[])Arrays.copyOf(this.contents, this.size, objects.getClass());
        } else {
            System.arraycopy(this.contents, 0, objects, 0, this.size);
            if (objects.length > this.size) {
                objects[this.size] = null;
            }

            return objects;
        }
    }

    @Override
    public void clear() {
        Arrays.fill(this.contents, 0, this.size, (Object)null);
        this.size = 0;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            if (object instanceof SortedArraySet) {
                SortedArraySet<?> sortedArraySet = (SortedArraySet)object;
                if (this.comparator.equals(sortedArraySet.comparator)) {
                    return this.size == sortedArraySet.size && Arrays.equals(this.contents, sortedArraySet.contents);
                }
            }

            return super.equals(object);
        }
    }

    class ArrayIterator implements Iterator<T> {
        private int index;
        private int last = -1;

        @Override
        public boolean hasNext() {
            return this.index < SortedArraySet.this.size;
        }

        @Override
        public T next() {
            if (this.index >= SortedArraySet.this.size) {
                throw new NoSuchElementException();
            } else {
                this.last = this.index++;
                return SortedArraySet.this.contents[this.last];
            }
        }

        @Override
        public void remove() {
            if (this.last == -1) {
                throw new IllegalStateException();
            } else {
                SortedArraySet.this.removeInternal(this.last);
                --this.index;
                this.last = -1;
            }
        }
    }
}
