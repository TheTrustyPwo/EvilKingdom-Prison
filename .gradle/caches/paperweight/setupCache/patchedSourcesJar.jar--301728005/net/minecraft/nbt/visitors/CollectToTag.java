package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;

public class CollectToTag implements StreamTagVisitor {
    private String lastId = "";
    @Nullable
    private Tag rootTag;
    private final Deque<Consumer<Tag>> consumerStack = new ArrayDeque<>();

    @Nullable
    public Tag getResult() {
        return this.rootTag;
    }

    protected int depth() {
        return this.consumerStack.size();
    }

    private void appendEntry(Tag nbt) {
        this.consumerStack.getLast().accept(nbt);
    }

    @Override
    public StreamTagVisitor.ValueResult visitEnd() {
        this.appendEntry(EndTag.INSTANCE);
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(String value) {
        this.appendEntry(StringTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(byte value) {
        this.appendEntry(ByteTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(short value) {
        this.appendEntry(ShortTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(int value) {
        this.appendEntry(IntTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(long value) {
        this.appendEntry(LongTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(float value) {
        this.appendEntry(FloatTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(double value) {
        this.appendEntry(DoubleTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(byte[] value) {
        this.appendEntry(new ByteArrayTag(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(int[] value) {
        this.appendEntry(new IntArrayTag(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(long[] value) {
        this.appendEntry(new LongArrayTag(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visitList(TagType<?> entryType, int length) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.EntryResult visitElement(TagType<?> type, int index) {
        this.enterContainerIfNeeded(type);
        return StreamTagVisitor.EntryResult.ENTER;
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type) {
        return StreamTagVisitor.EntryResult.ENTER;
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type, String key) {
        this.lastId = key;
        this.enterContainerIfNeeded(type);
        return StreamTagVisitor.EntryResult.ENTER;
    }

    private void enterContainerIfNeeded(TagType<?> type) {
        if (type == ListTag.TYPE) {
            ListTag listTag = new ListTag();
            this.appendEntry(listTag);
            this.consumerStack.addLast(listTag::add);
        } else if (type == CompoundTag.TYPE) {
            CompoundTag compoundTag = new CompoundTag();
            this.appendEntry(compoundTag);
            this.consumerStack.addLast((nbt) -> {
                compoundTag.put(this.lastId, nbt);
            });
        }

    }

    @Override
    public StreamTagVisitor.ValueResult visitContainerEnd() {
        this.consumerStack.removeLast();
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visitRootEntry(TagType<?> rootType) {
        if (rootType == ListTag.TYPE) {
            ListTag listTag = new ListTag();
            this.rootTag = listTag;
            this.consumerStack.addLast(listTag::add);
        } else if (rootType == CompoundTag.TYPE) {
            CompoundTag compoundTag = new CompoundTag();
            this.rootTag = compoundTag;
            this.consumerStack.addLast((nbt) -> {
                compoundTag.put(this.lastId, nbt);
            });
        } else {
            this.consumerStack.addLast((nbt) -> {
                this.rootTag = nbt;
            });
        }

        return StreamTagVisitor.ValueResult.CONTINUE;
    }
}
