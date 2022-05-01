package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.Validate;

public class SingleValuePalette<T> implements Palette<T> {
    private final IdMap<T> registry;
    @Nullable
    private T value;
    private final PaletteResize<T> resizeHandler;

    public SingleValuePalette(IdMap<T> idList, PaletteResize<T> listener, List<T> entries) {
        this.registry = idList;
        this.resizeHandler = listener;
        if (entries.size() > 0) {
            Validate.isTrue(entries.size() <= 1, "Can't initialize SingleValuePalette with %d values.", (long)entries.size());
            this.value = entries.get(0);
        }

    }

    public static <A> Palette<A> create(int bitSize, IdMap<A> idList, PaletteResize<A> listener, List<A> entries) {
        return new SingleValuePalette<>(idList, listener, entries);
    }

    @Override
    public int idFor(T object) {
        if (this.value != null && this.value != object) {
            return this.resizeHandler.onResize(1, object);
        } else {
            this.value = object;
            return 0;
        }
    }

    @Override
    public boolean maybeHas(Predicate<T> predicate) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return predicate.test(this.value);
        }
    }

    @Override
    public T valueFor(int id) {
        if (this.value != null && id == 0) {
            return this.value;
        } else {
            throw new IllegalStateException("Missing Palette entry for id " + id + ".");
        }
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.value = this.registry.byIdOrThrow(buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            buf.writeVarInt(this.registry.getId(this.value));
        }
    }

    @Override
    public int getSerializedSize() {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return FriendlyByteBuf.getVarIntSize(this.registry.getId(this.value));
        }
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public Palette<T> copy() {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return this;
        }
    }
}
