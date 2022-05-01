package net.minecraft.nbt;

public interface TagVisitor {
    void visitString(StringTag element);

    void visitByte(ByteTag element);

    void visitShort(ShortTag element);

    void visitInt(IntTag element);

    void visitLong(LongTag element);

    void visitFloat(FloatTag element);

    void visitDouble(DoubleTag element);

    void visitByteArray(ByteArrayTag element);

    void visitIntArray(IntArrayTag element);

    void visitLongArray(LongArrayTag element);

    void visitList(ListTag element);

    void visitCompound(CompoundTag compound);

    void visitEnd(EndTag element);
}
