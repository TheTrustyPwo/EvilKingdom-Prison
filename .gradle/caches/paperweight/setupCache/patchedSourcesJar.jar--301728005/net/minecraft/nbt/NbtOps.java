package net.minecraft.nbt;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class NbtOps implements DynamicOps<Tag> {
    public static final NbtOps INSTANCE = new NbtOps();

    protected NbtOps() {
    }

    public Tag empty() {
        return EndTag.INSTANCE;
    }

    public <U> U convertTo(DynamicOps<U> dynamicOps, Tag tag) {
        switch(tag.getId()) {
        case 0:
            return dynamicOps.empty();
        case 1:
            return dynamicOps.createByte(((NumericTag)tag).getAsByte());
        case 2:
            return dynamicOps.createShort(((NumericTag)tag).getAsShort());
        case 3:
            return dynamicOps.createInt(((NumericTag)tag).getAsInt());
        case 4:
            return dynamicOps.createLong(((NumericTag)tag).getAsLong());
        case 5:
            return dynamicOps.createFloat(((NumericTag)tag).getAsFloat());
        case 6:
            return dynamicOps.createDouble(((NumericTag)tag).getAsDouble());
        case 7:
            return dynamicOps.createByteList(ByteBuffer.wrap(((ByteArrayTag)tag).getAsByteArray()));
        case 8:
            return dynamicOps.createString(tag.getAsString());
        case 9:
            return this.convertList(dynamicOps, tag);
        case 10:
            return this.convertMap(dynamicOps, tag);
        case 11:
            return dynamicOps.createIntList(Arrays.stream(((IntArrayTag)tag).getAsIntArray()));
        case 12:
            return dynamicOps.createLongList(Arrays.stream(((LongArrayTag)tag).getAsLongArray()));
        default:
            throw new IllegalStateException("Unknown tag type: " + tag);
        }
    }

    public DataResult<Number> getNumberValue(Tag tag) {
        return tag instanceof NumericTag ? DataResult.success(((NumericTag)tag).getAsNumber()) : DataResult.error("Not a number");
    }

    public Tag createNumeric(Number number) {
        return DoubleTag.valueOf(number.doubleValue());
    }

    public Tag createByte(byte b) {
        return ByteTag.valueOf(b);
    }

    public Tag createShort(short s) {
        return ShortTag.valueOf(s);
    }

    public Tag createInt(int i) {
        return IntTag.valueOf(i);
    }

    public Tag createLong(long l) {
        return LongTag.valueOf(l);
    }

    public Tag createFloat(float f) {
        return FloatTag.valueOf(f);
    }

    public Tag createDouble(double d) {
        return DoubleTag.valueOf(d);
    }

    public Tag createBoolean(boolean bl) {
        return ByteTag.valueOf(bl);
    }

    public DataResult<String> getStringValue(Tag tag) {
        return tag instanceof StringTag ? DataResult.success(tag.getAsString()) : DataResult.error("Not a string");
    }

    public Tag createString(String string) {
        return StringTag.valueOf(string);
    }

    private static CollectionTag<?> createGenericList(byte knownType, byte valueType) {
        if (typesMatch(knownType, valueType, (byte)4)) {
            return new LongArrayTag(new long[0]);
        } else if (typesMatch(knownType, valueType, (byte)1)) {
            return new ByteArrayTag(new byte[0]);
        } else {
            return (CollectionTag<?>)(typesMatch(knownType, valueType, (byte)3) ? new IntArrayTag(new int[0]) : new ListTag());
        }
    }

    private static boolean typesMatch(byte knownType, byte valueType, byte expectedType) {
        return knownType == expectedType && (valueType == expectedType || valueType == 0);
    }

    private static <T extends Tag> void fillOne(CollectionTag<T> destination, Tag source, Tag additionalValue) {
        if (source instanceof CollectionTag) {
            CollectionTag<?> collectionTag = (CollectionTag)source;
            collectionTag.forEach((nbt) -> {
                destination.add(nbt);
            });
        }

        destination.add(additionalValue);
    }

    private static <T extends Tag> void fillMany(CollectionTag<T> destination, Tag source, List<Tag> additionalValues) {
        if (source instanceof CollectionTag) {
            CollectionTag<?> collectionTag = (CollectionTag)source;
            collectionTag.forEach((nbt) -> {
                destination.add(nbt);
            });
        }

        additionalValues.forEach((nbt) -> {
            destination.add(nbt);
        });
    }

    public DataResult<Tag> mergeToList(Tag tag, Tag tag2) {
        if (!(tag instanceof CollectionTag) && !(tag instanceof EndTag)) {
            return DataResult.error("mergeToList called with not a list: " + tag, tag);
        } else {
            CollectionTag<?> collectionTag = createGenericList(tag instanceof CollectionTag ? ((CollectionTag)tag).getElementType() : 0, tag2.getId());
            fillOne(collectionTag, tag, tag2);
            return DataResult.success(collectionTag);
        }
    }

    public DataResult<Tag> mergeToList(Tag tag, List<Tag> list) {
        if (!(tag instanceof CollectionTag) && !(tag instanceof EndTag)) {
            return DataResult.error("mergeToList called with not a list: " + tag, tag);
        } else {
            CollectionTag<?> collectionTag = createGenericList(tag instanceof CollectionTag ? ((CollectionTag)tag).getElementType() : 0, list.stream().findFirst().map(Tag::getId).orElse((byte)0));
            fillMany(collectionTag, tag, list);
            return DataResult.success(collectionTag);
        }
    }

    public DataResult<Tag> mergeToMap(Tag tag, Tag tag2, Tag tag3) {
        if (!(tag instanceof CompoundTag) && !(tag instanceof EndTag)) {
            return DataResult.error("mergeToMap called with not a map: " + tag, tag);
        } else if (!(tag2 instanceof StringTag)) {
            return DataResult.error("key is not a string: " + tag2, tag);
        } else {
            CompoundTag compoundTag = new CompoundTag();
            if (tag instanceof CompoundTag) {
                CompoundTag compoundTag2 = (CompoundTag)tag;
                compoundTag2.getAllKeys().forEach((key) -> {
                    compoundTag.put(key, compoundTag2.get(key));
                });
            }

            compoundTag.put(tag2.getAsString(), tag3);
            return DataResult.success(compoundTag);
        }
    }

    public DataResult<Tag> mergeToMap(Tag tag, MapLike<Tag> mapLike) {
        if (!(tag instanceof CompoundTag) && !(tag instanceof EndTag)) {
            return DataResult.error("mergeToMap called with not a map: " + tag, tag);
        } else {
            CompoundTag compoundTag = new CompoundTag();
            if (tag instanceof CompoundTag) {
                CompoundTag compoundTag2 = (CompoundTag)tag;
                compoundTag2.getAllKeys().forEach((string) -> {
                    compoundTag.put(string, compoundTag2.get(string));
                });
            }

            List<Tag> list = Lists.newArrayList();
            mapLike.entries().forEach((pair) -> {
                Tag tag = pair.getFirst();
                if (!(tag instanceof StringTag)) {
                    list.add(tag);
                } else {
                    compoundTag.put(tag.getAsString(), pair.getSecond());
                }
            });
            return !list.isEmpty() ? DataResult.error("some keys are not strings: " + list, compoundTag) : DataResult.success(compoundTag);
        }
    }

    public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag tag) {
        if (!(tag instanceof CompoundTag)) {
            return DataResult.error("Not a map: " + tag);
        } else {
            CompoundTag compoundTag = (CompoundTag)tag;
            return DataResult.success(compoundTag.getAllKeys().stream().map((key) -> {
                return Pair.of(this.createString(key), compoundTag.get(key));
            }));
        }
    }

    public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag tag) {
        if (!(tag instanceof CompoundTag)) {
            return DataResult.error("Not a map: " + tag);
        } else {
            CompoundTag compoundTag = (CompoundTag)tag;
            return DataResult.success((entryConsumer) -> {
                compoundTag.getAllKeys().forEach((key) -> {
                    entryConsumer.accept(this.createString(key), compoundTag.get(key));
                });
            });
        }
    }

    public DataResult<MapLike<Tag>> getMap(Tag tag) {
        if (!(tag instanceof CompoundTag)) {
            return DataResult.error("Not a map: " + tag);
        } else {
            final CompoundTag compoundTag = (CompoundTag)tag;
            return DataResult.success(new MapLike<Tag>() {
                @Nullable
                public Tag get(Tag tag) {
                    return compoundTag.get(tag.getAsString());
                }

                @Nullable
                public Tag get(String string) {
                    return compoundTag.get(string);
                }

                public Stream<Pair<Tag, Tag>> entries() {
                    return compoundTag.getAllKeys().stream().map((key) -> {
                        return Pair.of(NbtOps.this.createString(key), compoundTag.get(key));
                    });
                }

                @Override
                public String toString() {
                    return "MapLike[" + compoundTag + "]";
                }
            });
        }
    }

    public Tag createMap(Stream<Pair<Tag, Tag>> stream) {
        CompoundTag compoundTag = new CompoundTag();
        stream.forEach((entry) -> {
            compoundTag.put(entry.getFirst().getAsString(), entry.getSecond());
        });
        return compoundTag;
    }

    public DataResult<Stream<Tag>> getStream(Tag tag) {
        return tag instanceof CollectionTag ? DataResult.success(((CollectionTag)tag).stream().map((nbt) -> {
            return nbt;
        })) : DataResult.error("Not a list");
    }

    public DataResult<Consumer<Consumer<Tag>>> getList(Tag tag) {
        if (tag instanceof CollectionTag) {
            CollectionTag<?> collectionTag = (CollectionTag)tag;
            return DataResult.success(collectionTag::forEach);
        } else {
            return DataResult.error("Not a list: " + tag);
        }
    }

    public DataResult<ByteBuffer> getByteBuffer(Tag tag) {
        return tag instanceof ByteArrayTag ? DataResult.success(ByteBuffer.wrap(((ByteArrayTag)tag).getAsByteArray())) : DynamicOps.super.getByteBuffer(tag);
    }

    public Tag createByteList(ByteBuffer byteBuffer) {
        return new ByteArrayTag(DataFixUtils.toArray(byteBuffer));
    }

    public DataResult<IntStream> getIntStream(Tag tag) {
        return tag instanceof IntArrayTag ? DataResult.success(Arrays.stream(((IntArrayTag)tag).getAsIntArray())) : DynamicOps.super.getIntStream(tag);
    }

    public Tag createIntList(IntStream intStream) {
        return new IntArrayTag(intStream.toArray());
    }

    public DataResult<LongStream> getLongStream(Tag tag) {
        return tag instanceof LongArrayTag ? DataResult.success(Arrays.stream(((LongArrayTag)tag).getAsLongArray())) : DynamicOps.super.getLongStream(tag);
    }

    public Tag createLongList(LongStream longStream) {
        return new LongArrayTag(longStream.toArray());
    }

    public Tag createList(Stream<Tag> stream) {
        PeekingIterator<Tag> peekingIterator = Iterators.peekingIterator(stream.iterator());
        if (!peekingIterator.hasNext()) {
            return new ListTag();
        } else {
            Tag tag = peekingIterator.peek();
            if (tag instanceof ByteTag) {
                List<Byte> list = Lists.newArrayList(Iterators.transform(peekingIterator, (nbt) -> {
                    return ((ByteTag)nbt).getAsByte();
                }));
                return new ByteArrayTag(list);
            } else if (tag instanceof IntTag) {
                List<Integer> list2 = Lists.newArrayList(Iterators.transform(peekingIterator, (nbt) -> {
                    return ((IntTag)nbt).getAsInt();
                }));
                return new IntArrayTag(list2);
            } else if (tag instanceof LongTag) {
                List<Long> list3 = Lists.newArrayList(Iterators.transform(peekingIterator, (nbt) -> {
                    return ((LongTag)nbt).getAsLong();
                }));
                return new LongArrayTag(list3);
            } else {
                ListTag listTag = new ListTag();

                while(peekingIterator.hasNext()) {
                    Tag tag2 = peekingIterator.next();
                    if (!(tag2 instanceof EndTag)) {
                        listTag.add(tag2);
                    }
                }

                return listTag;
            }
        }
    }

    public Tag remove(Tag tag, String string) {
        if (tag instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag)tag;
            CompoundTag compoundTag2 = new CompoundTag();
            compoundTag.getAllKeys().stream().filter((k) -> {
                return !Objects.equals(k, string);
            }).forEach((k) -> {
                compoundTag2.put(k, compoundTag.get(k));
            });
            return compoundTag2;
        } else {
            return tag;
        }
    }

    @Override
    public String toString() {
        return "NBT";
    }

    public RecordBuilder<Tag> mapBuilder() {
        return new NbtOps.NbtRecordBuilder();
    }

    class NbtRecordBuilder extends AbstractStringBuilder<Tag, CompoundTag> {
        protected NbtRecordBuilder() {
            super(NbtOps.this);
        }

        protected CompoundTag initBuilder() {
            return new CompoundTag();
        }

        protected CompoundTag append(String string, Tag tag, CompoundTag compoundTag) {
            compoundTag.put(string, tag);
            return compoundTag;
        }

        protected DataResult<Tag> build(CompoundTag compoundTag, Tag tag) {
            if (tag != null && tag != EndTag.INSTANCE) {
                if (!(tag instanceof CompoundTag)) {
                    return DataResult.error("mergeToMap called with not a map: " + tag, tag);
                } else {
                    CompoundTag compoundTag2 = new CompoundTag(Maps.newHashMap(((CompoundTag)tag).entries()));

                    for(Entry<String, Tag> entry : compoundTag.entries().entrySet()) {
                        compoundTag2.put(entry.getKey(), entry.getValue());
                    }

                    return DataResult.success(compoundTag2);
                }
            } else {
                return DataResult.success(compoundTag);
            }
        }
    }
}
