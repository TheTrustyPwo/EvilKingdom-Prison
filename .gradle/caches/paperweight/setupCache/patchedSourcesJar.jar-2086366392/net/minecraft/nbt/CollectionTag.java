package net.minecraft.nbt;

import java.util.AbstractList;

public abstract class CollectionTag<T extends Tag> extends AbstractList<T> implements Tag {
    @Override
    public abstract T set(int i, T tag);

    @Override
    public abstract void add(int i, T tag);

    @Override
    public abstract T remove(int i);

    public abstract boolean setTag(int index, Tag element);

    public abstract boolean addTag(int index, Tag element);

    public abstract byte getElementType();
}
