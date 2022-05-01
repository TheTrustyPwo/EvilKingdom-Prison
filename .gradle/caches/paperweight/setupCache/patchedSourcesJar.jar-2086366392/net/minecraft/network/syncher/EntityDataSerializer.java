package net.minecraft.network.syncher;

import net.minecraft.network.FriendlyByteBuf;

public interface EntityDataSerializer<T> {
    void write(FriendlyByteBuf buf, T value);

    T read(FriendlyByteBuf buf);

    default EntityDataAccessor<T> createAccessor(int i) {
        return new EntityDataAccessor<>(i, this);
    }

    T copy(T value);
}
