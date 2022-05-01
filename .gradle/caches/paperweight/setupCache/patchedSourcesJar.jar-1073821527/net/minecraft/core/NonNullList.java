package net.minecraft.core;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;

public class NonNullList<E> extends AbstractList<E> {
    private final List<E> list;
    @Nullable
    private final E defaultValue;

    public static <E> NonNullList<E> create() {
        return new NonNullList<>(Lists.newArrayList(), (E)null);
    }

    public static <E> NonNullList<E> createWithCapacity(int size) {
        return new NonNullList<>(Lists.newArrayListWithCapacity(size), (E)null);
    }

    public static <E> NonNullList<E> withSize(int size, E defaultValue) {
        Validate.notNull(defaultValue);
        Object[] objects = new Object[size];
        Arrays.fill(objects, defaultValue);
        return new NonNullList<>(Arrays.asList((E[])objects), defaultValue);
    }

    @SafeVarargs
    public static <E> NonNullList<E> of(E defaultValue, E... values) {
        return new NonNullList<>(Arrays.asList(values), defaultValue);
    }

    protected NonNullList(List<E> delegate, @Nullable E initialElement) {
        this.list = delegate;
        this.defaultValue = initialElement;
    }

    @Nonnull
    @Override
    public E get(int i) {
        return this.list.get(i);
    }

    @Override
    public E set(int i, E object) {
        Validate.notNull(object);
        return this.list.set(i, object);
    }

    @Override
    public void add(int i, E object) {
        Validate.notNull(object);
        this.list.add(i, object);
    }

    @Override
    public E remove(int i) {
        return this.list.remove(i);
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public void clear() {
        if (this.defaultValue == null) {
            super.clear();
        } else {
            for(int i = 0; i < this.size(); ++i) {
                this.set(i, this.defaultValue);
            }
        }

    }
}
