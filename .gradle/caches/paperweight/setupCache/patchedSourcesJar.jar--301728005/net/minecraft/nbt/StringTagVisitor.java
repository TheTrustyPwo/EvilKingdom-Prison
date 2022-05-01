package net.minecraft.nbt;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class StringTagVisitor implements TagVisitor {
    private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
    private final StringBuilder builder = new StringBuilder();

    public String visit(Tag element) {
        element.accept(this);
        return this.builder.toString();
    }

    @Override
    public void visitString(StringTag element) {
        this.builder.append(StringTag.quoteAndEscape(element.getAsString()));
    }

    @Override
    public void visitByte(ByteTag element) {
        this.builder.append((Object)element.getAsNumber()).append('b');
    }

    @Override
    public void visitShort(ShortTag element) {
        this.builder.append((Object)element.getAsNumber()).append('s');
    }

    @Override
    public void visitInt(IntTag element) {
        this.builder.append((Object)element.getAsNumber());
    }

    @Override
    public void visitLong(LongTag element) {
        this.builder.append((Object)element.getAsNumber()).append('L');
    }

    @Override
    public void visitFloat(FloatTag element) {
        this.builder.append(element.getAsFloat()).append('f');
    }

    @Override
    public void visitDouble(DoubleTag element) {
        this.builder.append(element.getAsDouble()).append('d');
    }

    @Override
    public void visitByteArray(ByteArrayTag element) {
        this.builder.append("[B;");
        byte[] bs = element.getAsByteArray();

        for(int i = 0; i < bs.length; ++i) {
            if (i != 0) {
                this.builder.append(',');
            }

            this.builder.append((int)bs[i]).append('B');
        }

        this.builder.append(']');
    }

    @Override
    public void visitIntArray(IntArrayTag element) {
        this.builder.append("[I;");
        int[] is = element.getAsIntArray();

        for(int i = 0; i < is.length; ++i) {
            if (i != 0) {
                this.builder.append(',');
            }

            this.builder.append(is[i]);
        }

        this.builder.append(']');
    }

    @Override
    public void visitLongArray(LongArrayTag element) {
        this.builder.append("[L;");
        long[] ls = element.getAsLongArray();

        for(int i = 0; i < ls.length; ++i) {
            if (i != 0) {
                this.builder.append(',');
            }

            this.builder.append(ls[i]).append('L');
        }

        this.builder.append(']');
    }

    @Override
    public void visitList(ListTag element) {
        this.builder.append('[');

        for(int i = 0; i < element.size(); ++i) {
            if (i != 0) {
                this.builder.append(',');
            }

            this.builder.append((new StringTagVisitor()).visit(element.get(i)));
        }

        this.builder.append(']');
    }

    @Override
    public void visitCompound(CompoundTag compound) {
        this.builder.append('{');
        List<String> list = Lists.newArrayList(compound.getAllKeys());
        Collections.sort(list);

        for(String string : list) {
            if (this.builder.length() != 1) {
                this.builder.append(',');
            }

            this.builder.append(handleEscape(string)).append(':').append((new StringTagVisitor()).visit(compound.get(string)));
        }

        this.builder.append('}');
    }

    protected static String handleEscape(String name) {
        return SIMPLE_VALUE.matcher(name).matches() ? name : StringTag.quoteAndEscape(name);
    }

    @Override
    public void visitEnd(EndTag element) {
        this.builder.append("END");
    }
}
