package net.minecraft.util.datafix;

import net.minecraft.util.Mth;
import org.apache.commons.lang3.Validate;

public class PackedBitStorage {
    private static final int BIT_TO_LONG_SHIFT = 6;
    private final long[] data;
    private final int bits;
    private final long mask;
    private final int size;

    public PackedBitStorage(int unitSize, int length) {
        this(unitSize, length, new long[Mth.roundToward(length * unitSize, 64) / 64]);
    }

    public PackedBitStorage(int unitSize, int length, long[] array) {
        Validate.inclusiveBetween(1L, 32L, (long)unitSize);
        this.size = length;
        this.bits = unitSize;
        this.data = array;
        this.mask = (1L << unitSize) - 1L;
        int i = Mth.roundToward(length * unitSize, 64) / 64;
        if (array.length != i) {
            throw new IllegalArgumentException("Invalid length given for storage, got: " + array.length + " but expected: " + i);
        }
    }

    public void set(int index, int value) {
        Validate.inclusiveBetween(0L, (long)(this.size - 1), (long)index);
        Validate.inclusiveBetween(0L, this.mask, (long)value);
        int i = index * this.bits;
        int j = i >> 6;
        int k = (index + 1) * this.bits - 1 >> 6;
        int l = i ^ j << 6;
        this.data[j] = this.data[j] & ~(this.mask << l) | ((long)value & this.mask) << l;
        if (j != k) {
            int m = 64 - l;
            int n = this.bits - m;
            this.data[k] = this.data[k] >>> n << n | ((long)value & this.mask) >> m;
        }

    }

    public int get(int index) {
        Validate.inclusiveBetween(0L, (long)(this.size - 1), (long)index);
        int i = index * this.bits;
        int j = i >> 6;
        int k = (index + 1) * this.bits - 1 >> 6;
        int l = i ^ j << 6;
        if (j == k) {
            return (int)(this.data[j] >>> l & this.mask);
        } else {
            int m = 64 - l;
            return (int)((this.data[j] >>> l | this.data[k] << m) & this.mask);
        }
    }

    public long[] getRaw() {
        return this.data;
    }

    public int getBits() {
        return this.bits;
    }
}
