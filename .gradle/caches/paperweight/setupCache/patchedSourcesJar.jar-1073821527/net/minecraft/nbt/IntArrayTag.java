package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class IntArrayTag extends CollectionTag<IntTag> {
    private static final int SELF_SIZE_IN_BITS = 192;
    public static final TagType<IntArrayTag> TYPE = new TagType.VariableSize<IntArrayTag>() {
        @Override
        public IntArrayTag load(DataInput dataInput, int i, NbtAccounter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(192L);
            int j = dataInput.readInt();
            nbtAccounter.accountBits(32L * (long)j);
            int[] is = new int[j];

            for(int k = 0; k < j; ++k) {
                is[k] = dataInput.readInt();
            }

            return new IntArrayTag(is);
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException {
            int i = input.readInt();
            int[] is = new int[i];

            for(int j = 0; j < i; ++j) {
                is[j] = input.readInt();
            }

            return visitor.visit(is);
        }

        @Override
        public void skip(DataInput input) throws IOException {
            input.skipBytes(input.readInt() * 4);
        }

        @Override
        public String getName() {
            return "INT[]";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Int_Array";
        }
    };
    private int[] data;

    public IntArrayTag(int[] value) {
        this.data = value;
    }

    public IntArrayTag(List<Integer> value) {
        this(toArray(value));
    }

    private static int[] toArray(List<Integer> list) {
        int[] is = new int[list.size()];

        for(int i = 0; i < list.size(); ++i) {
            Integer integer = list.get(i);
            is[i] = integer == null ? 0 : integer;
        }

        return is;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.data.length);

        for(int i : this.data) {
            output.writeInt(i);
        }

    }

    @Override
    public byte getId() {
        return 11;
    }

    @Override
    public TagType<IntArrayTag> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.getAsString();
    }

    @Override
    public IntArrayTag copy() {
        int[] is = new int[this.data.length];
        System.arraycopy(this.data, 0, is, 0, this.data.length);
        return new IntArrayTag(is);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof IntArrayTag && Arrays.equals(this.data, ((IntArrayTag)object).data);
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    public int[] getAsIntArray() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitIntArray(this);
    }

    @Override
    public int size() {
        return this.data.length;
    }

    @Override
    public IntTag get(int i) {
        return IntTag.valueOf(this.data[i]);
    }

    @Override
    public IntTag set(int i, IntTag intTag) {
        int j = this.data[i];
        this.data[i] = intTag.getAsInt();
        return IntTag.valueOf(j);
    }

    @Override
    public void add(int i, IntTag intTag) {
        this.data = ArrayUtils.add(this.data, i, intTag.getAsInt());
    }

    @Override
    public boolean setTag(int index, Tag element) {
        if (element instanceof NumericTag) {
            this.data[index] = ((NumericTag)element).getAsInt();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addTag(int index, Tag element) {
        if (element instanceof NumericTag) {
            this.data = ArrayUtils.add(this.data, index, ((NumericTag)element).getAsInt());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public IntTag remove(int i) {
        int j = this.data[i];
        this.data = ArrayUtils.remove(this.data, i);
        return IntTag.valueOf(j);
    }

    @Override
    public byte getElementType() {
        return 3;
    }

    @Override
    public void clear() {
        this.data = new int[0];
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        return visitor.visit(this.data);
    }
}
