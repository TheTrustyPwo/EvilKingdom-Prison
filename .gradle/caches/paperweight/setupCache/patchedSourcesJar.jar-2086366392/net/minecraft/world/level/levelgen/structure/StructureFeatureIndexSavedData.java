package net.minecraft.world.level.levelgen.structure;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class StructureFeatureIndexSavedData extends SavedData {
    private static final String TAG_REMAINING_INDEXES = "Remaining";
    private static final String TAG_All_INDEXES = "All";
    private final LongSet all;
    private final LongSet remaining;

    private StructureFeatureIndexSavedData(LongSet all, LongSet remaining) {
        this.all = all;
        this.remaining = remaining;
    }

    public StructureFeatureIndexSavedData() {
        this(new LongOpenHashSet(), new LongOpenHashSet());
    }

    public static StructureFeatureIndexSavedData load(CompoundTag nbt) {
        return new StructureFeatureIndexSavedData(new LongOpenHashSet(nbt.getLongArray("All")), new LongOpenHashSet(nbt.getLongArray("Remaining")));
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putLongArray("All", this.all.toLongArray());
        nbt.putLongArray("Remaining", this.remaining.toLongArray());
        return nbt;
    }

    public void addIndex(long l) {
        this.all.add(l);
        this.remaining.add(l);
    }

    public boolean hasStartIndex(long l) {
        return this.all.contains(l);
    }

    public boolean hasUnhandledIndex(long l) {
        return this.remaining.contains(l);
    }

    public void removeIndex(long l) {
        this.remaining.remove(l);
    }

    public LongSet getAll() {
        return this.all;
    }
}
