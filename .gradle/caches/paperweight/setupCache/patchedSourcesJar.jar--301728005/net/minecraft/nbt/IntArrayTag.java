// mc-dev import
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
        public IntArrayTag load(DataInput input, int depth, NbtAccounter tracker) throws IOException {
            tracker.accountBits(192L);
            int j = input.readInt();
            com.google.common.base.Preconditions.checkArgument( j < 1 << 24); // Spigot

            tracker.accountBits(32L * (long) j);
            int[] aint = new int[j];

            for (int k = 0; k < j; ++k) {
                aint[k] = input.readInt();
            }

            return new IntArrayTag(aint);
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException {
            int i = input.readInt();
            int[] aint = new int[i];

            for (int j = 0; j < i; ++j) {
                aint[j] = input.readInt();
            }

            return visitor.visit(aint);
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
        this(IntArrayTag.toArray(value));
    }

    private static int[] toArray(List<Integer> list) {
        int[] aint = new int[list.size()];

        for (int i = 0; i < list.size(); ++i) {
            Integer integer = (Integer) list.get(i);

            aint[i] = integer == null ? 0 : integer;
        }

        return aint;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(this.data.length);
        int[] aint = this.data;
        int i = aint.length;

        for (int j = 0; j < i; ++j) {
            int k = aint[j];

            output.writeInt(k);
        }

    }

    @Override
    public byte getId() {
        return 11;
    }

    @Override
    public TagType<IntArrayTag> getType() {
        return IntArrayTag.TYPE;
    }

    @Override
    public String toString() {
        return this.getAsString();
    }

    @Override
    public IntArrayTag copy() {
        int[] aint = new int[this.data.length];

        System.arraycopy(this.data, 0, aint, 0, this.data.length);
        return new IntArrayTag(aint);
    }

    public boolean equals(Object object) {
        return this == object ? true : object instanceof IntArrayTag && Arrays.equals(this.data, ((IntArrayTag) object).data);
    }

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

    public int size() {
        return this.data.length;
    }

    public IntTag get(int i) {
        return IntTag.valueOf(this.data[i]);
    }

    public IntTag set(int i, IntTag nbttagint) {
        int j = this.data[i];

        this.data[i] = nbttagint.getAsInt();
        return IntTag.valueOf(j);
    }

    public void add(int i, IntTag nbttagint) {
        this.data = ArrayUtils.add(this.data, i, nbttagint.getAsInt());
    }

    @Override
    public boolean setTag(int index, Tag element) {
        if (element instanceof NumericTag) {
            this.data[index] = ((NumericTag) element).getAsInt();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addTag(int index, Tag element) {
        if (element instanceof NumericTag) {
            this.data = ArrayUtils.add(this.data, index, ((NumericTag) element).getAsInt());
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

    public void clear() {
        this.data = new int[0];
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        return visitor.visit(this.data);
    }
}
