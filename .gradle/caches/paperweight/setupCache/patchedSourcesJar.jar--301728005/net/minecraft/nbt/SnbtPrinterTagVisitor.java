package net.minecraft.nbt;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.Util;

public class SnbtPrinterTagVisitor implements TagVisitor {
    private static final Map<String, List<String>> KEY_ORDER = Util.make(Maps.newHashMap(), (map) -> {
        map.put("{}", Lists.newArrayList("DataVersion", "author", "size", "data", "entities", "palette", "palettes"));
        map.put("{}.data.[].{}", Lists.newArrayList("pos", "state", "nbt"));
        map.put("{}.entities.[].{}", Lists.newArrayList("blockPos", "pos"));
    });
    private static final Set<String> NO_INDENTATION = Sets.newHashSet("{}.size.[]", "{}.data.[].{}", "{}.palette.[].{}", "{}.entities.[].{}");
    private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
    private static final String NAME_VALUE_SEPARATOR = String.valueOf(':');
    private static final String ELEMENT_SEPARATOR = String.valueOf(',');
    private static final String LIST_OPEN = "[";
    private static final String LIST_CLOSE = "]";
    private static final String LIST_TYPE_SEPARATOR = ";";
    private static final String ELEMENT_SPACING = " ";
    private static final String STRUCT_OPEN = "{";
    private static final String STRUCT_CLOSE = "}";
    private static final String NEWLINE = "\n";
    private final String indentation;
    private final int depth;
    private final List<String> path;
    private String result = "";

    public SnbtPrinterTagVisitor() {
        this("    ", 0, Lists.newArrayList());
    }

    public SnbtPrinterTagVisitor(String prefix, int indentationLevel, List<String> pathParts) {
        this.indentation = prefix;
        this.depth = indentationLevel;
        this.path = pathParts;
    }

    public String visit(Tag element) {
        element.accept(this);
        return this.result;
    }

    @Override
    public void visitString(StringTag element) {
        this.result = StringTag.quoteAndEscape(element.getAsString());
    }

    @Override
    public void visitByte(ByteTag element) {
        this.result = element.getAsNumber() + "b";
    }

    @Override
    public void visitShort(ShortTag element) {
        this.result = element.getAsNumber() + "s";
    }

    @Override
    public void visitInt(IntTag element) {
        this.result = String.valueOf((Object)element.getAsNumber());
    }

    @Override
    public void visitLong(LongTag element) {
        this.result = element.getAsNumber() + "L";
    }

    @Override
    public void visitFloat(FloatTag element) {
        this.result = element.getAsFloat() + "f";
    }

    @Override
    public void visitDouble(DoubleTag element) {
        this.result = element.getAsDouble() + "d";
    }

    @Override
    public void visitByteArray(ByteArrayTag element) {
        StringBuilder stringBuilder = (new StringBuilder("[")).append("B").append(";");
        byte[] bs = element.getAsByteArray();

        for(int i = 0; i < bs.length; ++i) {
            stringBuilder.append(" ").append((int)bs[i]).append("B");
            if (i != bs.length - 1) {
                stringBuilder.append(ELEMENT_SEPARATOR);
            }
        }

        stringBuilder.append("]");
        this.result = stringBuilder.toString();
    }

    @Override
    public void visitIntArray(IntArrayTag element) {
        StringBuilder stringBuilder = (new StringBuilder("[")).append("I").append(";");
        int[] is = element.getAsIntArray();

        for(int i = 0; i < is.length; ++i) {
            stringBuilder.append(" ").append(is[i]);
            if (i != is.length - 1) {
                stringBuilder.append(ELEMENT_SEPARATOR);
            }
        }

        stringBuilder.append("]");
        this.result = stringBuilder.toString();
    }

    @Override
    public void visitLongArray(LongArrayTag element) {
        String string = "L";
        StringBuilder stringBuilder = (new StringBuilder("[")).append("L").append(";");
        long[] ls = element.getAsLongArray();

        for(int i = 0; i < ls.length; ++i) {
            stringBuilder.append(" ").append(ls[i]).append("L");
            if (i != ls.length - 1) {
                stringBuilder.append(ELEMENT_SEPARATOR);
            }
        }

        stringBuilder.append("]");
        this.result = stringBuilder.toString();
    }

    @Override
    public void visitList(ListTag element) {
        if (element.isEmpty()) {
            this.result = "[]";
        } else {
            StringBuilder stringBuilder = new StringBuilder("[");
            this.pushPath("[]");
            String string = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
            if (!string.isEmpty()) {
                stringBuilder.append("\n");
            }

            for(int i = 0; i < element.size(); ++i) {
                stringBuilder.append(Strings.repeat(string, this.depth + 1));
                stringBuilder.append((new SnbtPrinterTagVisitor(string, this.depth + 1, this.path)).visit(element.get(i)));
                if (i != element.size() - 1) {
                    stringBuilder.append(ELEMENT_SEPARATOR).append(string.isEmpty() ? " " : "\n");
                }
            }

            if (!string.isEmpty()) {
                stringBuilder.append("\n").append(Strings.repeat(string, this.depth));
            }

            stringBuilder.append("]");
            this.result = stringBuilder.toString();
            this.popPath();
        }
    }

    @Override
    public void visitCompound(CompoundTag compound) {
        if (compound.isEmpty()) {
            this.result = "{}";
        } else {
            StringBuilder stringBuilder = new StringBuilder("{");
            this.pushPath("{}");
            String string = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
            if (!string.isEmpty()) {
                stringBuilder.append("\n");
            }

            Collection<String> collection = this.getKeys(compound);
            Iterator<String> iterator = collection.iterator();

            while(iterator.hasNext()) {
                String string2 = iterator.next();
                Tag tag = compound.get(string2);
                this.pushPath(string2);
                stringBuilder.append(Strings.repeat(string, this.depth + 1)).append(handleEscapePretty(string2)).append(NAME_VALUE_SEPARATOR).append(" ").append((new SnbtPrinterTagVisitor(string, this.depth + 1, this.path)).visit(tag));
                this.popPath();
                if (iterator.hasNext()) {
                    stringBuilder.append(ELEMENT_SEPARATOR).append(string.isEmpty() ? " " : "\n");
                }
            }

            if (!string.isEmpty()) {
                stringBuilder.append("\n").append(Strings.repeat(string, this.depth));
            }

            stringBuilder.append("}");
            this.result = stringBuilder.toString();
            this.popPath();
        }
    }

    private void popPath() {
        this.path.remove(this.path.size() - 1);
    }

    private void pushPath(String part) {
        this.path.add(part);
    }

    protected List<String> getKeys(CompoundTag compound) {
        Set<String> set = Sets.newHashSet(compound.getAllKeys());
        List<String> list = Lists.newArrayList();
        List<String> list2 = KEY_ORDER.get(this.pathString());
        if (list2 != null) {
            for(String string : list2) {
                if (set.remove(string)) {
                    list.add(string);
                }
            }

            if (!set.isEmpty()) {
                set.stream().sorted().forEach(list::add);
            }
        } else {
            list.addAll(set);
            Collections.sort(list);
        }

        return list;
    }

    public String pathString() {
        return String.join(".", this.path);
    }

    protected static String handleEscapePretty(String name) {
        return SIMPLE_VALUE.matcher(name).matches() ? name : StringTag.quoteAndEscape(name);
    }

    @Override
    public void visitEnd(EndTag element) {
    }
}
