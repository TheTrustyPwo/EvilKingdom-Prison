package net.minecraft.nbt.visitors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;

public class CollectFields extends CollectToTag {
    private int fieldsToGetCount;
    private final Set<TagType<?>> wantedTypes;
    private final Deque<FieldTree> stack = new ArrayDeque<>();

    public CollectFields(FieldSelector... queries) {
        this.fieldsToGetCount = queries.length;
        Builder<TagType<?>> builder = ImmutableSet.builder();
        FieldTree fieldTree = FieldTree.createRoot();

        for(FieldSelector fieldSelector : queries) {
            fieldTree.addEntry(fieldSelector);
            builder.add(fieldSelector.type());
        }

        this.stack.push(fieldTree);
        builder.add(CompoundTag.TYPE);
        this.wantedTypes = builder.build();
    }

    @Override
    public StreamTagVisitor.ValueResult visitRootEntry(TagType<?> rootType) {
        return rootType != CompoundTag.TYPE ? StreamTagVisitor.ValueResult.HALT : super.visitRootEntry(rootType);
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type) {
        FieldTree fieldTree = this.stack.element();
        if (this.depth() > fieldTree.depth()) {
            return super.visitEntry(type);
        } else if (this.fieldsToGetCount <= 0) {
            return StreamTagVisitor.EntryResult.HALT;
        } else {
            return !this.wantedTypes.contains(type) ? StreamTagVisitor.EntryResult.SKIP : super.visitEntry(type);
        }
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type, String key) {
        FieldTree fieldTree = this.stack.element();
        if (this.depth() > fieldTree.depth()) {
            return super.visitEntry(type, key);
        } else if (fieldTree.selectedFields().remove(key, type)) {
            --this.fieldsToGetCount;
            return super.visitEntry(type, key);
        } else {
            if (type == CompoundTag.TYPE) {
                FieldTree fieldTree2 = fieldTree.fieldsToRecurse().get(key);
                if (fieldTree2 != null) {
                    this.stack.push(fieldTree2);
                    return super.visitEntry(type, key);
                }
            }

            return StreamTagVisitor.EntryResult.SKIP;
        }
    }

    @Override
    public StreamTagVisitor.ValueResult visitContainerEnd() {
        if (this.depth() == this.stack.element().depth()) {
            this.stack.pop();
        }

        return super.visitContainerEnd();
    }

    public int getMissingFieldCount() {
        return this.fieldsToGetCount;
    }
}
