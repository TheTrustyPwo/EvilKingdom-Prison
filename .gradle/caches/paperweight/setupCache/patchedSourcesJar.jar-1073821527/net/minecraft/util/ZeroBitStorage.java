package net.minecraft.util;

import java.util.Arrays;
import java.util.function.IntConsumer;
import org.apache.commons.lang3.Validate;

public class ZeroBitStorage implements BitStorage {
    public static final long[] RAW = new long[0];
    private final int size;

    public ZeroBitStorage(int size) {
        this.size = size;
    }

    @Override
    public int getAndSet(int index, int value) {
        Validate.inclusiveBetween(0L, (long)(this.size - 1), (long)index);
        Validate.inclusiveBetween(0L, 0L, (long)value);
        return 0;
    }

    @Override
    public void set(int index, int value) {
        Validate.inclusiveBetween(0L, (long)(this.size - 1), (long)index);
        Validate.inclusiveBetween(0L, 0L, (long)value);
    }

    @Override
    public int get(int index) {
        Validate.inclusiveBetween(0L, (long)(this.size - 1), (long)index);
        return 0;
    }

    @Override
    public long[] getRaw() {
        return RAW;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public int getBits() {
        return 0;
    }

    @Override
    public void getAll(IntConsumer action) {
        for(int i = 0; i < this.size; ++i) {
            action.accept(0);
        }

    }

    @Override
    public void unpack(int[] is) {
        Arrays.fill(is, 0, this.size, 0);
    }

    @Override
    public BitStorage copy() {
        return this;
    }
}
