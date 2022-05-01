package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;

public class SkipFields extends CollectToTag {
    private final Deque<FieldTree> stack = new ArrayDeque<>();

    public SkipFields(FieldSelector... excludedQueries) {
        FieldTree fieldTree = FieldTree.createRoot();

        for(FieldSelector fieldSelector : excludedQueries) {
            fieldTree.addEntry(fieldSelector);
        }

        this.stack.push(fieldTree);
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type, String key) {
        FieldTree fieldTree = this.stack.element();
        if (fieldTree.isSelected(type, key)) {
            return StreamTagVisitor.EntryResult.SKIP;
        } else {
            if (type == CompoundTag.TYPE) {
                FieldTree fieldTree2 = fieldTree.fieldsToRecurse().get(key);
                if (fieldTree2 != null) {
                    this.stack.push(fieldTree2);
                }
            }

            return super.visitEntry(type, key);
        }
    }

    @Override
    public StreamTagVisitor.ValueResult visitContainerEnd() {
        if (this.depth() == this.stack.element().depth()) {
            this.stack.pop();
        }

        return super.visitContainerEnd();
    }
}
