package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;

public class CompoundTag implements Tag {
    public static final Codec<CompoundTag> CODEC = Codec.PASSTHROUGH.comapFlatMap((dynamic) -> {
        Tag tag = dynamic.convert(NbtOps.INSTANCE).getValue();
        return tag instanceof CompoundTag ? DataResult.success((CompoundTag)tag) : DataResult.error("Not a compound tag: " + tag);
    }, (nbt) -> {
        return new Dynamic<>(NbtOps.INSTANCE, nbt);
    });
    private static final int SELF_SIZE_IN_BITS = 384;
    private static final int MAP_ENTRY_SIZE_IN_BITS = 256;
    public static final TagType<CompoundTag> TYPE = new TagType.VariableSize<CompoundTag>() {
        @Override
        public CompoundTag load(DataInput dataInput, int i, NbtAccounter nbtAccounter) throws IOException {
            nbtAccounter.accountBits(384L);
            if (i > 512) {
                throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > 512");
            } else {
                Map<String, Tag> map = Maps.newHashMap();

                byte b;
                while((b = CompoundTag.readNamedTagType(dataInput, nbtAccounter)) != 0) {
                    String string = CompoundTag.readNamedTagName(dataInput, nbtAccounter);
                    nbtAccounter.accountBits((long)(224 + 16 * string.length()));
                    Tag tag = CompoundTag.readNamedTagData(TagTypes.getType(b), string, dataInput, i + 1, nbtAccounter);
                    if (map.put(string, tag) != null) {
                        nbtAccounter.accountBits(288L);
                    }
                }

                return new CompoundTag(map);
            }
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor visitor) throws IOException {
            while(true) {
                byte b;
                if ((b = input.readByte()) != 0) {
                    TagType<?> tagType = TagTypes.getType(b);
                    switch(visitor.visitEntry(tagType)) {
                    case HALT:
                        return StreamTagVisitor.ValueResult.HALT;
                    case BREAK:
                        StringTag.skipString(input);
                        tagType.skip(input);
                        break;
                    case SKIP:
                        StringTag.skipString(input);
                        tagType.skip(input);
                        continue;
                    default:
                        String string = input.readUTF();
                        switch(visitor.visitEntry(tagType, string)) {
                        case HALT:
                            return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                            tagType.skip(input);
                            break;
                        case SKIP:
                            tagType.skip(input);
                            continue;
                        default:
                            switch(tagType.parse(input, visitor)) {
                            case HALT:
                                return StreamTagVisitor.ValueResult.HALT;
                            case BREAK:
                            default:
                                continue;
                            }
                        }
                    }
                }

                if (b != 0) {
                    while((b = input.readByte()) != 0) {
                        StringTag.skipString(input);
                        TagTypes.getType(b).skip(input);
                    }
                }

                return visitor.visitContainerEnd();
            }
        }

        @Override
        public void skip(DataInput input) throws IOException {
            byte b;
            while((b = input.readByte()) != 0) {
                StringTag.skipString(input);
                TagTypes.getType(b).skip(input);
            }

        }

        @Override
        public String getName() {
            return "COMPOUND";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Compound";
        }
    };
    private final Map<String, Tag> tags;

    protected CompoundTag(Map<String, Tag> entries) {
        this.tags = entries;
    }

    public CompoundTag() {
        this(Maps.newHashMap());
    }

    @Override
    public void write(DataOutput output) throws IOException {
        for(String string : this.tags.keySet()) {
            Tag tag = this.tags.get(string);
            writeNamedTag(string, tag, output);
        }

        output.writeByte(0);
    }

    public Set<String> getAllKeys() {
        return this.tags.keySet();
    }

    @Override
    public byte getId() {
        return 10;
    }

    @Override
    public TagType<CompoundTag> getType() {
        return TYPE;
    }

    public int size() {
        return this.tags.size();
    }

    @Nullable
    public Tag put(String key, Tag element) {
        return this.tags.put(key, element);
    }

    public void putByte(String key, byte value) {
        this.tags.put(key, ByteTag.valueOf(value));
    }

    public void putShort(String key, short value) {
        this.tags.put(key, ShortTag.valueOf(value));
    }

    public void putInt(String key, int value) {
        this.tags.put(key, IntTag.valueOf(value));
    }

    public void putLong(String key, long value) {
        this.tags.put(key, LongTag.valueOf(value));
    }

    public void putUUID(String key, UUID value) {
        this.tags.put(key, NbtUtils.createUUID(value));
    }

    public UUID getUUID(String key) {
        return NbtUtils.loadUUID(this.get(key));
    }

    public boolean hasUUID(String key) {
        Tag tag = this.get(key);
        return tag != null && tag.getType() == IntArrayTag.TYPE && ((IntArrayTag)tag).getAsIntArray().length == 4;
    }

    public void putFloat(String key, float value) {
        this.tags.put(key, FloatTag.valueOf(value));
    }

    public void putDouble(String key, double value) {
        this.tags.put(key, DoubleTag.valueOf(value));
    }

    public void putString(String key, String value) {
        this.tags.put(key, StringTag.valueOf(value));
    }

    public void putByteArray(String key, byte[] value) {
        this.tags.put(key, new ByteArrayTag(value));
    }

    public void putByteArray(String key, List<Byte> value) {
        this.tags.put(key, new ByteArrayTag(value));
    }

    public void putIntArray(String key, int[] value) {
        this.tags.put(key, new IntArrayTag(value));
    }

    public void putIntArray(String key, List<Integer> value) {
        this.tags.put(key, new IntArrayTag(value));
    }

    public void putLongArray(String key, long[] value) {
        this.tags.put(key, new LongArrayTag(value));
    }

    public void putLongArray(String key, List<Long> value) {
        this.tags.put(key, new LongArrayTag(value));
    }

    public void putBoolean(String key, boolean value) {
        this.tags.put(key, ByteTag.valueOf(value));
    }

    @Nullable
    public Tag get(String key) {
        return this.tags.get(key);
    }

    public byte getTagType(String key) {
        Tag tag = this.tags.get(key);
        return tag == null ? 0 : tag.getId();
    }

    public boolean contains(String key) {
        return this.tags.containsKey(key);
    }

    public boolean contains(String key, int type) {
        int i = this.getTagType(key);
        if (i == type) {
            return true;
        } else if (type != 99) {
            return false;
        } else {
            return i == 1 || i == 2 || i == 3 || i == 4 || i == 5 || i == 6;
        }
    }

    public byte getByte(String key) {
        try {
            if (this.contains(key, 99)) {
                return ((NumericTag)this.tags.get(key)).getAsByte();
            }
        } catch (ClassCastException var3) {
        }

        return 0;
    }

    public short getShort(String key) {
        try {
            if (this.contains(key, 99)) {
                return ((NumericTag)this.tags.get(key)).getAsShort();
            }
        } catch (ClassCastException var3) {
        }

        return 0;
    }

    public int getInt(String key) {
        try {
            if (this.contains(key, 99)) {
                return ((NumericTag)this.tags.get(key)).getAsInt();
            }
        } catch (ClassCastException var3) {
        }

        return 0;
    }

    public long getLong(String key) {
        try {
            if (this.contains(key, 99)) {
                return ((NumericTag)this.tags.get(key)).getAsLong();
            }
        } catch (ClassCastException var3) {
        }

        return 0L;
    }

    public float getFloat(String key) {
        try {
            if (this.contains(key, 99)) {
                return ((NumericTag)this.tags.get(key)).getAsFloat();
            }
        } catch (ClassCastException var3) {
        }

        return 0.0F;
    }

    public double getDouble(String key) {
        try {
            if (this.contains(key, 99)) {
                return ((NumericTag)this.tags.get(key)).getAsDouble();
            }
        } catch (ClassCastException var3) {
        }

        return 0.0D;
    }

    public String getString(String key) {
        try {
            if (this.contains(key, 8)) {
                return this.tags.get(key).getAsString();
            }
        } catch (ClassCastException var3) {
        }

        return "";
    }

    public byte[] getByteArray(String key) {
        try {
            if (this.contains(key, 7)) {
                return ((ByteArrayTag)this.tags.get(key)).getAsByteArray();
            }
        } catch (ClassCastException var3) {
            throw new ReportedException(this.createReport(key, ByteArrayTag.TYPE, var3));
        }

        return new byte[0];
    }

    public int[] getIntArray(String key) {
        try {
            if (this.contains(key, 11)) {
                return ((IntArrayTag)this.tags.get(key)).getAsIntArray();
            }
        } catch (ClassCastException var3) {
            throw new ReportedException(this.createReport(key, IntArrayTag.TYPE, var3));
        }

        return new int[0];
    }

    public long[] getLongArray(String key) {
        try {
            if (this.contains(key, 12)) {
                return ((LongArrayTag)this.tags.get(key)).getAsLongArray();
            }
        } catch (ClassCastException var3) {
            throw new ReportedException(this.createReport(key, LongArrayTag.TYPE, var3));
        }

        return new long[0];
    }

    public CompoundTag getCompound(String key) {
        try {
            if (this.contains(key, 10)) {
                return (CompoundTag)this.tags.get(key);
            }
        } catch (ClassCastException var3) {
            throw new ReportedException(this.createReport(key, TYPE, var3));
        }

        return new CompoundTag();
    }

    public ListTag getList(String key, int type) {
        try {
            if (this.getTagType(key) == 9) {
                ListTag listTag = (ListTag)this.tags.get(key);
                if (!listTag.isEmpty() && listTag.getElementType() != type) {
                    return new ListTag();
                }

                return listTag;
            }
        } catch (ClassCastException var4) {
            throw new ReportedException(this.createReport(key, ListTag.TYPE, var4));
        }

        return new ListTag();
    }

    public boolean getBoolean(String key) {
        return this.getByte(key) != 0;
    }

    public void remove(String key) {
        this.tags.remove(key);
    }

    @Override
    public String toString() {
        return this.getAsString();
    }

    public boolean isEmpty() {
        return this.tags.isEmpty();
    }

    private CrashReport createReport(String key, TagType<?> reader, ClassCastException exception) {
        CrashReport crashReport = CrashReport.forThrowable(exception, "Reading NBT data");
        CrashReportCategory crashReportCategory = crashReport.addCategory("Corrupt NBT tag", 1);
        crashReportCategory.setDetail("Tag type found", () -> {
            return this.tags.get(key).getType().getName();
        });
        crashReportCategory.setDetail("Tag type expected", reader::getName);
        crashReportCategory.setDetail("Tag name", key);
        return crashReport;
    }

    @Override
    public CompoundTag copy() {
        Map<String, Tag> map = Maps.newHashMap(Maps.transformValues(this.tags, Tag::copy));
        return new CompoundTag(map);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof CompoundTag && Objects.equals(this.tags, ((CompoundTag)object).tags);
        }
    }

    @Override
    public int hashCode() {
        return this.tags.hashCode();
    }

    private static void writeNamedTag(String key, Tag element, DataOutput output) throws IOException {
        output.writeByte(element.getId());
        if (element.getId() != 0) {
            output.writeUTF(key);
            element.write(output);
        }
    }

    static byte readNamedTagType(DataInput input, NbtAccounter tracker) throws IOException {
        return input.readByte();
    }

    static String readNamedTagName(DataInput input, NbtAccounter tracker) throws IOException {
        return input.readUTF();
    }

    static Tag readNamedTagData(TagType<?> reader, String key, DataInput input, int depth, NbtAccounter tracker) {
        try {
            return reader.load(input, depth, tracker);
        } catch (IOException var8) {
            CrashReport crashReport = CrashReport.forThrowable(var8, "Loading NBT data");
            CrashReportCategory crashReportCategory = crashReport.addCategory("NBT Tag");
            crashReportCategory.setDetail("Tag name", key);
            crashReportCategory.setDetail("Tag type", reader.getName());
            throw new ReportedException(crashReport);
        }
    }

    public CompoundTag merge(CompoundTag source) {
        for(String string : source.tags.keySet()) {
            Tag tag = source.tags.get(string);
            if (tag.getId() == 10) {
                if (this.contains(string, 10)) {
                    CompoundTag compoundTag = this.getCompound(string);
                    compoundTag.merge((CompoundTag)tag);
                } else {
                    this.put(string, tag.copy());
                }
            } else {
                this.put(string, tag.copy());
            }
        }

        return this;
    }

    @Override
    public void accept(TagVisitor visitor) {
        visitor.visitCompound(this);
    }

    protected Map<String, Tag> entries() {
        return Collections.unmodifiableMap(this.tags);
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor) {
        for(Entry<String, Tag> entry : this.tags.entrySet()) {
            Tag tag = entry.getValue();
            TagType<?> tagType = tag.getType();
            StreamTagVisitor.EntryResult entryResult = visitor.visitEntry(tagType);
            switch(entryResult) {
            case HALT:
                return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
                return visitor.visitContainerEnd();
            case SKIP:
                break;
            default:
                entryResult = visitor.visitEntry(tagType, entry.getKey());
                switch(entryResult) {
                case HALT:
                    return StreamTagVisitor.ValueResult.HALT;
                case BREAK:
                    return visitor.visitContainerEnd();
                case SKIP:
                    break;
                default:
                    StreamTagVisitor.ValueResult valueResult = tag.accept(visitor);
                    switch(valueResult) {
                    case HALT:
                        return StreamTagVisitor.ValueResult.HALT;
                    case BREAK:
                        return visitor.visitContainerEnd();
                    }
                }
            }
        }

        return visitor.visitContainerEnd();
    }
}
