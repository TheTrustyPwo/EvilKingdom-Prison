package net.minecraft.world.level.chunk;

import java.util.BitSet;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public class CarvingMask {
    private final int minY;
    private final BitSet mask;
    private CarvingMask.Mask additionalMask = (offsetX, y, offsetZ) -> {
        return false;
    };

    public CarvingMask(int height, int bottomY) {
        this.minY = bottomY;
        this.mask = new BitSet(256 * height);
    }

    public void setAdditionalMask(CarvingMask.Mask maskPredicate) {
        this.additionalMask = maskPredicate;
    }

    public CarvingMask(long[] mask, int bottomY) {
        this.minY = bottomY;
        this.mask = BitSet.valueOf(mask);
    }

    private int getIndex(int offsetX, int y, int offsetZ) {
        return offsetX & 15 | (offsetZ & 15) << 4 | y - this.minY << 8;
    }

    public void set(int offsetX, int y, int offsetZ) {
        this.mask.set(this.getIndex(offsetX, y, offsetZ));
    }

    public boolean get(int offsetX, int y, int offsetZ) {
        return this.additionalMask.test(offsetX, y, offsetZ) || this.mask.get(this.getIndex(offsetX, y, offsetZ));
    }

    public Stream<BlockPos> stream(ChunkPos chunkPos) {
        return this.mask.stream().mapToObj((mask) -> {
            int i = mask & 15;
            int j = mask >> 4 & 15;
            int k = mask >> 8;
            return chunkPos.getBlockAt(i, k + this.minY, j);
        });
    }

    public long[] toArray() {
        return this.mask.toLongArray();
    }

    public interface Mask {
        boolean test(int offsetX, int y, int offsetZ);
    }
}
