package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class NonOverlappingMerger extends AbstractDoubleList implements IndexMerger {
    private final DoubleList lower;
    private final DoubleList upper;
    private final boolean swap;

    protected NonOverlappingMerger(DoubleList first, DoubleList second, boolean inverted) {
        this.lower = first;
        this.upper = second;
        this.swap = inverted;
    }

    @Override
    public int size() {
        return this.lower.size() + this.upper.size();
    }

    @Override
    public boolean forMergedIndexes(IndexMerger.IndexConsumer predicate) {
        return this.swap ? this.forNonSwappedIndexes((i, j, k) -> {
            return predicate.merge(j, i, k);
        }) : this.forNonSwappedIndexes(predicate);
    }

    private boolean forNonSwappedIndexes(IndexMerger.IndexConsumer indexConsumer) {
        int i = this.lower.size();

        for(int j = 0; j < i; ++j) {
            if (!indexConsumer.merge(j, -1, j)) {
                return false;
            }
        }

        int k = this.upper.size() - 1;

        for(int l = 0; l < k; ++l) {
            if (!indexConsumer.merge(i - 1, l, i + l)) {
                return false;
            }
        }

        return true;
    }

    public double getDouble(int i) {
        return i < this.lower.size() ? this.lower.getDouble(i) : this.upper.getDouble(i - this.lower.size());
    }

    @Override
    public DoubleList getList() {
        return this;
    }
}
