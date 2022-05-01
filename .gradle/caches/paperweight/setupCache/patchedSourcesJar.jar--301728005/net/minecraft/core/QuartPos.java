package net.minecraft.core;

public final class QuartPos {
    public static final int BITS = 2;
    public static final int SIZE = 4;
    public static final int MASK = 3;
    private static final int SECTION_TO_QUARTS_BITS = 2;

    private QuartPos() {
    }

    public static int fromBlock(int blockCoord) {
        return blockCoord >> 2;
    }

    public static int quartLocal(int i) {
        return i & 3;
    }

    public static int toBlock(int biomeCoord) {
        return biomeCoord << 2;
    }

    public static int fromSection(int chunkCoord) {
        return chunkCoord << 2;
    }

    public static int toSection(int biomeCoord) {
        return biomeCoord >> 2;
    }
}
