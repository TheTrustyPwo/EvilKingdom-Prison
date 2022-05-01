package net.minecraft.nbt;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.bytes.ByteOpenHashSet;
import it.unimi.dsi.fastutil.bytes.ByteSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ListTag extends CollectionTag<Tag> {
    private static final int SELF_SIZE_IN_BITS = 296;
    public static final TagType<ListTag> TYPE = new TagType.VariableSize<ListTag>() {
        @Override
        public ListTag load(DataInput dataInput, int i, NbtAccounter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(296L);
            if (i > 512) {
                throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > 512");
            } else {
                byte b = dataInput.readByte();
                int j = dataInput.readInt();
                if (b == 0 && j > 0) {
                    throw new RuntimeException("Missing type on ListTag");
                } else {
                    nbtAccounter.accountBits(32L * (long)j);
                    TagType<?> tagType = TagTypes.getType(b);
                    List<Tag> list = Lists.newArrayListWithCapacity(j);

                    for(int k = 0; k < j; ++k) {
                        list.add(tagType.load(dataInput, i + 1, nbtAccounter));
                    }

                    return new ListTag(list, b);
                }
            }
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException {
            TagType<?> tagType = TagTypes.getType(input.readByte());
            int i = input.readInt();
            switch(visitor.visitList(tagType, i)) {
            case HALT:
                return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
                tagType.skip(input, i);
                return visitor.visitContainerEnd();
            default:
                int j = 0;

                while(true) {
                    label45: {
                        if (j < i) {
                            switch(visitor.visitElement(tagType, j)) {
                            case HALT:
                                return StreamTagVisitor.ValueResult.HALT;
                            case BREAK:
                                tagType.skip(input);
                                break;
                            case SKIP:
                                tagType.skip(input);
                                break label45;
                            default:
                                switch(tagType.parse(input, visitor)) {
                                case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                case BREAK:
                                    break;
                                default:
                                    break label45;
                                }
                            }
                        }

                        int k = i - 1 - j;
                        if (k > 0) {
                            tagType.skip(input, k);
                        }

                        return visitor.visitContainerEnd();
                    }

                    ++j;
                }
            }
        }

        @Override
        public void skip(DataInput input) throws IOException {
            TagType<?> tagType = TagTypes.getType(input.readByte());
            int i = input.readInt();
            tagType.skip(input, i);
        }

        @Override
        public String getName() {
            return "LIST";
        }

        @Override
        public String getPrettyName() {
            return "TAG_List";
        }
    };
    private static final ByteSet INLINE_ELEMENT_TYPES = new ByteOpenHashSet(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6)); // Paper - decompiler fix // todo: what is this
    private final List<Tag> list;
    private byte type;

    ListTag(List<Tag> list, byte type) {
        this.list = list;
        this.type = type;
    }

    public ListTag() {
        this(Lists.newArrayList(), (byte)0);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        if (this.list.isEmpty()) {
            this.type = 0;
        } else {
            this.type = this.list.get(0).getId();
        }

        output.writeByte(this.type);
        output.writeInt(this.list.size());

        for(Tag tag : this.list) {
            tag.write(output);
        }

    }

    @Override
    public byte getId() {
        return 9;
    }

    @Override
    public TagType<ListTag> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return this.getAsString();
    }

    private void updateTypeAfterRemove() {
        if (this.list.isEmpty()) {
            this.type = 0;
        }

    }

    @Override
    public Tag remove(int i) {
        Tag tag = this.list.remove(i);
        this.updateTypeAfterRemove();
        return tag;
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    public CompoundTag getCompound(int index) {
        if (index >= 0 && index < this.list.size()) {
            Tag tag = this.list.get(index);
            if (tag.getId() == 10) {
                return (CompoundTag)tag;
            }
        }

        return new CompoundTag();
    }

    public ListTag getList(int index) {
        if (index >= 0 && index < this.list.size()) {
            Tag tag = this.list.get(index);
            if (tag.getId() == 9) {
                return (ListTag)tag;
            }
        }

        return new ListTag();
    }

    public short getShort(int index) {
        if (index >= 0 && index < this.list.size()) {
            Tag tag = this.list.get(index);
            if (tag.getId() == 2) {
                return ((ShortTag)tag).getAsShort();
            }
        }

        return 0;
    }

    public int getInt(int index) {
        if (index >= 0 && index < this.list.size()) {
            Tag tag = this.list.get(index);
            if (tag.getId() == 3) {
                return ((IntTag)tag).getAsInt();
            }
        }

        return 0;
    }

    public int[] getIntArray(int index) {
        if (index >= 0 && index < this.list.size()) {
            Tag tag = this.list.get(index);
            if (tag.getId() == 11) {
                return ((IntArrayTag)tag).getAsIntArray();
            }
        }

        return new int[0];
    }

    public long[] getLongArray(int index) {
        if (index >= 0 && index < this.list.size()) {
            Tag tag = this.list.get(index);
            if (tag.getId() == 11) {
                return ((LongArrayTag)tag).getAsLongArray();
            }
        }

        return new long[0];
    }

    public double getDouble(int index) {
        if (index >= 0 && index < this.list.size()) {
            Tag tag = this.list.get(index);
            if (tag.getId() == 6) {
                return ((DoubleTag)tag).getAsDouble();
            }
        }

        return 0.0D;
    }

    public float getFloat(int index) {
        if (index >= 0 && index < this.list.size()) {
            Tag tag = this.list.get(index);
            if (tag.getId() == 5) {
                return ((FloatTag)tag).getAsFloat();
            }
        }

        return 0.0F;
    }

    public String getString(int index) {
        if (index >= 0 && index < this.list.size()) {
            Tag tag = this.list.get(index);
            return tag.getId() == 8 ? tag.getAsString() : tag.toString();
        } else {
            return "";
        }
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public Tag get(int i) {
        return this.list.get(i);
    }

    @Override
    public Tag set(int i, Tag tag) {
        Tag tag2 = this.get(i);
        if (!this.setTag(i, tag)) {
            throw new UnsupportedOperationException(String.format("Trying to add tag of type %d to list of %d", tag.getId(), this.type));
        } else {
            return tag2;
        }
    }

    @Override
    public void add(int i, Tag tag) {
        if (!this.addTag(i, tag)) {
            throw new UnsupportedOperationException(String.format("Trying to add tag of type %d to list of %d", tag.getId(), this.type));
        }
    }

    @Override
    public boolean setTag(int index, Tag element) {
        if (this.updateType(element)) {
            this.list.set(index, element);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addTag(int index, Tag element) {
        if (this.updateType(element)) {
            this.list.add(index, element);
            return true;
        } else {
            return false;
        }
    }

    private boolean updateType(Tag element) {
        if (element.getId() == 0) {
            return false;
        } else if (this.type == 0) {
            this.type = element.getId();
            return true;
        } else {
            return this.type == element.getId();
        }
    }

    @Override
    public ListTag copy() {
        Iterable<Tag> iterable = (Iterable<Tag>)(TagTypes.getType(this.type).isValue() ? this.list : Iterables.transform(this.list, Tag::copy));
        List<Tag> list = Lists.newArrayList(iterable);
        return new ListTag(list, this.type);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof ListTag && Objects.equals(this.list, ((ListTag)object).list);
        }
    }

    @Override
    public int hashCode() {
        return this.list.hashCode();
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitList(this);
    }

    @Override
    public byte getElementType() {
        return this.type;
    }

    @Override
    public void clear() {
        this.list.clear();
        this.type = 0;
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        switch(visitor.visitList(TagTypes.getType(this.type), this.list.size())) {
        case HALT:
            return StreamTagVisitor.ValueResult.HALT;
        case BREAK:
            return visitor.visitContainerEnd();
        default:
            int i = 0;

            while(i < this.list.size()) {
                Tag tag = this.list.get(i);
                switch(visitor.visitElement(tag.getType(), i)) {
                case HALT:
                    return StreamTagVisitor.ValueResult.HALT;
                case BREAK:
                    return visitor.visitContainerEnd();
                default:
                    switch(tag.accept(visitor)) {
                    case HALT:
                        return StreamTagVisitor.ValueResult.HALT;
                    case BREAK:
                        return visitor.visitContainerEnd();
                    }
                case SKIP:
                    ++i;
                }
            }

            return visitor.visitContainerEnd();
        }
    }
}
