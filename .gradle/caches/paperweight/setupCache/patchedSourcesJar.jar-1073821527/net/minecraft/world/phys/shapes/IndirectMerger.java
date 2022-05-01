package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleLists;

public class IndirectMerger implements IndexMerger {
    private static final DoubleList EMPTY = DoubleLists.unmodifiable(DoubleArrayList.wrap(new double[]{0.0D}));
    private final double[] result;
    private final int[] firstIndices;
    private final int[] secondIndices;
    private final int resultLength;

    public IndirectMerger(DoubleList first, DoubleList second, boolean includeFirstOnly, boolean includeSecondOnly) {
        double d = Double.NaN;
        int i = first.size();
        int j = second.size();
        int k = i + j;
        this.result = new double[k];
        this.firstIndices = new int[k];
        this.secondIndices = new int[k];
        boolean bl = !includeFirstOnly;
        boolean bl2 = !includeSecondOnly;
        int l = 0;
        int m = 0;
        int n = 0;

        while(true) {
            boolean bl5;
            while(true) {
                boolean bl3 = m >= i;
                boolean bl4 = n >= j;
                if (bl3 && bl4) {
                    this.resultLength = Math.max(1, l);
                    return;
                }

                bl5 = !bl3 && (bl4 || first.getDouble(m) < second.getDouble(n) + 1.0E-7D);
                if (bl5) {
                    ++m;
                    if (!bl || n != 0 && !bl4) {
                        break;
                    }
                } else {
                    ++n;
                    if (!bl2 || m != 0 && !bl3) {
                        break;
                    }
                }
            }

            int o = m - 1;
            int p = n - 1;
            double e = bl5 ? first.getDouble(o) : second.getDouble(p);
            if (!(d >= e - 1.0E-7D)) {
                this.firstIndices[l] = o;
                this.secondIndices[l] = p;
                this.result[l] = e;
                ++l;
                d = e;
            } else {
                this.firstIndices[l - 1] = o;
                this.secondIndices[l - 1] = p;
            }
        }
    }

    @Override
    public boolean forMergedIndexes(IndexMerger.IndexConsumer predicate) {
        int i = this.resultLength - 1;

        for(int j = 0; j < i; ++j) {
            if (!predicate.merge(this.firstIndices[j], this.secondIndices[j], j)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int size() {
        return this.resultLength;
    }

    @Override
    public DoubleList getList() {
        return (DoubleList)(this.resultLength <= 1 ? EMPTY : DoubleArrayList.wrap(this.result, this.resultLength));
    }
}
