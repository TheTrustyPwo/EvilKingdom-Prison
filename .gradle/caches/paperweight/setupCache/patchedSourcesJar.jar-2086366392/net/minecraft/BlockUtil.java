package net.minecraft;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntStack;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockUtil {
    public static BlockUtil.FoundRectangle getLargestRectangleAround(BlockPos center, Direction.Axis primaryAxis, int primaryMaxBlocks, Direction.Axis secondaryAxis, int secondaryMaxBlocks, Predicate<BlockPos> predicate) {
        BlockPos.MutableBlockPos mutableBlockPos = center.mutable();
        Direction direction = Direction.get(Direction.AxisDirection.NEGATIVE, primaryAxis);
        Direction direction2 = direction.getOpposite();
        Direction direction3 = Direction.get(Direction.AxisDirection.NEGATIVE, secondaryAxis);
        Direction direction4 = direction3.getOpposite();
        int i = getLimit(predicate, mutableBlockPos.set(center), direction, primaryMaxBlocks);
        int j = getLimit(predicate, mutableBlockPos.set(center), direction2, primaryMaxBlocks);
        int k = i;
        BlockUtil.IntBounds[] intBoundss = new BlockUtil.IntBounds[i + 1 + j];
        intBoundss[i] = new BlockUtil.IntBounds(getLimit(predicate, mutableBlockPos.set(center), direction3, secondaryMaxBlocks), getLimit(predicate, mutableBlockPos.set(center), direction4, secondaryMaxBlocks));
        int l = intBoundss[i].min;

        for(int m = 1; m <= i; ++m) {
            BlockUtil.IntBounds intBounds = intBoundss[k - (m - 1)];
            intBoundss[k - m] = new BlockUtil.IntBounds(getLimit(predicate, mutableBlockPos.set(center).move(direction, m), direction3, intBounds.min), getLimit(predicate, mutableBlockPos.set(center).move(direction, m), direction4, intBounds.max));
        }

        for(int n = 1; n <= j; ++n) {
            BlockUtil.IntBounds intBounds2 = intBoundss[k + n - 1];
            intBoundss[k + n] = new BlockUtil.IntBounds(getLimit(predicate, mutableBlockPos.set(center).move(direction2, n), direction3, intBounds2.min), getLimit(predicate, mutableBlockPos.set(center).move(direction2, n), direction4, intBounds2.max));
        }

        int o = 0;
        int p = 0;
        int q = 0;
        int r = 0;
        int[] is = new int[intBoundss.length];

        for(int s = l; s >= 0; --s) {
            for(int t = 0; t < intBoundss.length; ++t) {
                BlockUtil.IntBounds intBounds3 = intBoundss[t];
                int u = l - intBounds3.min;
                int v = l + intBounds3.max;
                is[t] = s >= u && s <= v ? v + 1 - s : 0;
            }

            Pair<BlockUtil.IntBounds, Integer> pair = getMaxRectangleLocation(is);
            BlockUtil.IntBounds intBounds4 = pair.getFirst();
            int w = 1 + intBounds4.max - intBounds4.min;
            int x = pair.getSecond();
            if (w * x > q * r) {
                o = intBounds4.min;
                p = s;
                q = w;
                r = x;
            }
        }

        return new BlockUtil.FoundRectangle(center.relative(primaryAxis, o - k).relative(secondaryAxis, p - l), q, r);
    }

    private static int getLimit(Predicate<BlockPos> predicate, BlockPos.MutableBlockPos pos, Direction direction, int max) {
        int i;
        for(i = 0; i < max && predicate.test(pos.move(direction)); ++i) {
        }

        return i;
    }

    @VisibleForTesting
    static Pair<BlockUtil.IntBounds, Integer> getMaxRectangleLocation(int[] heights) {
        int i = 0;
        int j = 0;
        int k = 0;
        IntStack intStack = new IntArrayList();
        intStack.push(0);

        for(int l = 1; l <= heights.length; ++l) {
            int m = l == heights.length ? 0 : heights[l];

            while(!intStack.isEmpty()) {
                int n = heights[intStack.topInt()];
                if (m >= n) {
                    intStack.push(l);
                    break;
                }

                intStack.popInt();
                int o = intStack.isEmpty() ? 0 : intStack.topInt() + 1;
                if (n * (l - o) > k * (j - i)) {
                    j = l;
                    i = o;
                    k = n;
                }
            }

            if (intStack.isEmpty()) {
                intStack.push(l);
            }
        }

        return new Pair<>(new BlockUtil.IntBounds(i, j - 1), k);
    }

    public static Optional<BlockPos> getTopConnectedBlock(BlockGetter world, BlockPos pos, Block intermediateBlock, Direction direction, Block endBlock) {
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

        BlockState blockState;
        do {
            mutableBlockPos.move(direction);
            blockState = world.getBlockState(mutableBlockPos);
        } while(blockState.is(intermediateBlock));

        return blockState.is(endBlock) ? Optional.of(mutableBlockPos) : Optional.empty();
    }

    public static class FoundRectangle {
        public final BlockPos minCorner;
        public final int axis1Size;
        public final int axis2Size;

        public FoundRectangle(BlockPos lowerLeft, int width, int height) {
            this.minCorner = lowerLeft;
            this.axis1Size = width;
            this.axis2Size = height;
        }
    }

    public static class IntBounds {
        public final int min;
        public final int max;

        public IntBounds(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public String toString() {
            return "IntBounds{min=" + this.min + ", max=" + this.max + "}";
        }
    }
}
