package net.minecraft.world.level.chunk;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.VisibleForDebug;

public final class DataLayer {
    public static final int LAYER_COUNT = 16;
    public static final int LAYER_SIZE = 128;
    public static final int SIZE = 2048;
    private static final int NIBBLE_SIZE = 4;
    @Nullable
    protected byte[] data;

    public DataLayer() {
    }

    public DataLayer(byte[] bytes) {
        this.data = bytes;
        if (bytes.length != 2048) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("DataLayer should be 2048 bytes not: " + bytes.length));
        }
    }

    protected DataLayer(int size) {
        this.data = new byte[size];
    }

    public int get(int x, int y, int z) {
        return this.get(getIndex(x, y, z));
    }

    public void set(int x, int y, int z, int value) {
        this.set(getIndex(x, y, z), value);
    }

    private static int getIndex(int i, int x, int y) {
        return x << 8 | y << 4 | i;
    }

    private int get(int index) {
        if (this.data == null) {
            return 0;
        } else {
            int i = getByteIndex(index);
            int j = getNibbleIndex(index);
            return this.data[i] >> 4 * j & 15;
        }
    }

    private void set(int index, int value) {
        if (this.data == null) {
            this.data = new byte[2048];
        }

        int i = getByteIndex(index);
        int j = getNibbleIndex(index);
        int k = ~(15 << 4 * j);
        int l = (value & 15) << 4 * j;
        this.data[i] = (byte)(this.data[i] & k | l);
    }

    private static int getNibbleIndex(int i) {
        return i & 1;
    }

    private static int getByteIndex(int i) {
        return i >> 1;
    }

    public byte[] getData() {
        if (this.data == null) {
            this.data = new byte[2048];
        }

        return this.data;
    }

    public DataLayer copy() {
        return this.data == null ? new DataLayer() : new DataLayer((byte[])this.data.clone());
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        for(int i = 0; i < 4096; ++i) {
            stringBuilder.append(Integer.toHexString(this.get(i)));
            if ((i & 15) == 15) {
                stringBuilder.append("\n");
            }

            if ((i & 255) == 255) {
                stringBuilder.append("\n");
            }
        }

        return stringBuilder.toString();
    }

    @VisibleForDebug
    public String layerToString(int unused) {
        StringBuilder stringBuilder = new StringBuilder();

        for(int i = 0; i < 256; ++i) {
            stringBuilder.append(Integer.toHexString(this.get(i)));
            if ((i & 15) == 15) {
                stringBuilder.append("\n");
            }
        }

        return stringBuilder.toString();
    }

    public boolean isEmpty() {
        return this.data == null;
    }
}
