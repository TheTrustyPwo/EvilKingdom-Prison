package net.minecraft.tags;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class Tag<T> {
    private static final Tag<?> EMPTY = new Tag(List.of());
    final List<T> elements;

    public Tag(Collection<T> values) {
        this.elements = List.copyOf(values);
    }

    public List<T> getValues() {
        return this.elements;
    }

    public static <T> Tag<T> empty() {
        return EMPTY;
    }

    public static class Builder {
        private final List<Tag.BuilderEntry> entries = new ArrayList<>();

        public static Tag.Builder tag() {
            return new Tag.Builder();
        }

        public Tag.Builder add(Tag.BuilderEntry trackedEntry) {
            this.entries.add(trackedEntry);
            return this;
        }

        public Tag.Builder add(Tag.Entry entry, String source) {
            return this.add(new Tag.BuilderEntry(entry, source));
        }

        public Tag.Builder addElement(ResourceLocation id, String source) {
            return this.add(new Tag.ElementEntry(id), source);
        }

        public Tag.Builder addOptionalElement(ResourceLocation id, String source) {
            return this.add(new Tag.OptionalElementEntry(id), source);
        }

        public Tag.Builder addTag(ResourceLocation id, String source) {
            return this.add(new Tag.TagEntry(id), source);
        }

        public Tag.Builder addOptionalTag(ResourceLocation id, String source) {
            return this.add(new Tag.OptionalTagEntry(id), source);
        }

        public <T> Either<Collection<Tag.BuilderEntry>, Tag<T>> build(Function<ResourceLocation, Tag<T>> tagGetter, Function<ResourceLocation, T> objectGetter) {
            ImmutableSet.Builder<T> builder = ImmutableSet.builder();
            List<Tag.BuilderEntry> list = new ArrayList<>();

            for(Tag.BuilderEntry builderEntry : this.entries) {
                if (!builderEntry.entry().build(tagGetter, objectGetter, builder::add)) {
                    list.add(builderEntry);
                }
            }

            return list.isEmpty() ? Either.right(new Tag<>(builder.build())) : Either.left(list);
        }

        public Stream<Tag.BuilderEntry> getEntries() {
            return this.entries.stream();
        }

        public void visitRequiredDependencies(Consumer<ResourceLocation> consumer) {
            this.entries.forEach((builderEntry) -> {
                builderEntry.entry.visitRequiredDependencies(consumer);
            });
        }

        public void visitOptionalDependencies(Consumer<ResourceLocation> consumer) {
            this.entries.forEach((builderEntry) -> {
                builderEntry.entry.visitOptionalDependencies(consumer);
            });
        }

        public Tag.Builder addFromJson(JsonObject json, String source) {
            JsonArray jsonArray = GsonHelper.getAsJsonArray(json, "values");
            List<Tag.Entry> list = new ArrayList<>();

            for(JsonElement jsonElement : jsonArray) {
                list.add(parseEntry(jsonElement));
            }

            if (GsonHelper.getAsBoolean(json, "replace", false)) {
                this.entries.clear();
            }

            list.forEach((entry) -> {
                this.entries.add(new Tag.BuilderEntry(entry, source));
            });
            return this;
        }

        private static Tag.Entry parseEntry(JsonElement json) {
            String string;
            boolean bl;
            if (json.isJsonObject()) {
                JsonObject jsonObject = json.getAsJsonObject();
                string = GsonHelper.getAsString(jsonObject, "id");
                bl = GsonHelper.getAsBoolean(jsonObject, "required", true);
            } else {
                string = GsonHelper.convertToString(json, "id");
                bl = true;
            }

            if (string.startsWith("#")) {
                ResourceLocation resourceLocation = new ResourceLocation(string.substring(1));
                return (Tag.Entry)(bl ? new Tag.TagEntry(resourceLocation) : new Tag.OptionalTagEntry(resourceLocation));
            } else {
                ResourceLocation resourceLocation2 = new ResourceLocation(string);
                return (Tag.Entry)(bl ? new Tag.ElementEntry(resourceLocation2) : new Tag.OptionalElementEntry(resourceLocation2));
            }
        }

        public JsonObject serializeToJson() {
            JsonObject jsonObject = new JsonObject();
            JsonArray jsonArray = new JsonArray();

            for(Tag.BuilderEntry builderEntry : this.entries) {
                builderEntry.entry().serializeTo(jsonArray);
            }

            jsonObject.addProperty("replace", false);
            jsonObject.add("values", jsonArray);
            return jsonObject;
        }
    }

    public static record BuilderEntry(Tag.Entry entry, String source) {
        @Override
        public String toString() {
            return this.entry + " (from " + this.source + ")";
        }
    }

    static class ElementEntry implements Tag.Entry {
        private final ResourceLocation id;

        public ElementEntry(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public <T> boolean build(Function<ResourceLocation, Tag<T>> tagGetter, Function<ResourceLocation, T> objectGetter, Consumer<T> collector) {
            T object = objectGetter.apply(this.id);
            if (object == null) {
                return false;
            } else {
                collector.accept(object);
                return true;
            }
        }

        @Override
        public void serializeTo(JsonArray json) {
            json.add(this.id.toString());
        }

        @Override
        public boolean verifyIfPresent(Predicate<ResourceLocation> objectExistsTest, Predicate<ResourceLocation> tagExistsTest) {
            return objectExistsTest.test(this.id);
        }

        @Override
        public String toString() {
            return this.id.toString();
        }
    }

    public interface Entry {
        <T> boolean build(Function<ResourceLocation, Tag<T>> tagGetter, Function<ResourceLocation, T> objectGetter, Consumer<T> collector);

        void serializeTo(JsonArray json);

        default void visitRequiredDependencies(Consumer<ResourceLocation> consumer) {
        }

        default void visitOptionalDependencies(Consumer<ResourceLocation> consumer) {
        }

        boolean verifyIfPresent(Predicate<ResourceLocation> objectExistsTest, Predicate<ResourceLocation> tagExistsTest);
    }

    static class OptionalElementEntry implements Tag.Entry {
        private final ResourceLocation id;

        public OptionalElementEntry(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public <T> boolean build(Function<ResourceLocation, Tag<T>> tagGetter, Function<ResourceLocation, T> objectGetter, Consumer<T> collector) {
            T object = objectGetter.apply(this.id);
            if (object != null) {
                collector.accept(object);
            }

            return true;
        }

        @Override
        public void serializeTo(JsonArray json) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", this.id.toString());
            jsonObject.addProperty("required", false);
            json.add(jsonObject);
        }

        @Override
        public boolean verifyIfPresent(Predicate<ResourceLocation> objectExistsTest, Predicate<ResourceLocation> tagExistsTest) {
            return true;
        }

        @Override
        public String toString() {
            return this.id + "?";
        }
    }

    static class OptionalTagEntry implements Tag.Entry {
        private final ResourceLocation id;

        public OptionalTagEntry(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public <T> boolean build(Function<ResourceLocation, Tag<T>> tagGetter, Function<ResourceLocation, T> objectGetter, Consumer<T> collector) {
            Tag<T> tag = tagGetter.apply(this.id);
            if (tag != null) {
                tag.elements.forEach(collector);
            }

            return true;
        }

        @Override
        public void serializeTo(JsonArray json) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", "#" + this.id);
            jsonObject.addProperty("required", false);
            json.add(jsonObject);
        }

        @Override
        public String toString() {
            return "#" + this.id + "?";
        }

        @Override
        public void visitOptionalDependencies(Consumer<ResourceLocation> consumer) {
            consumer.accept(this.id);
        }

        @Override
        public boolean verifyIfPresent(Predicate<ResourceLocation> objectExistsTest, Predicate<ResourceLocation> tagExistsTest) {
            return true;
        }
    }

    static class TagEntry implements Tag.Entry {
        private final ResourceLocation id;

        public TagEntry(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public <T> boolean build(Function<ResourceLocation, Tag<T>> tagGetter, Function<ResourceLocation, T> objectGetter, Consumer<T> collector) {
            Tag<T> tag = tagGetter.apply(this.id);
            if (tag == null) {
                return false;
            } else {
                tag.elements.forEach(collector);
                return true;
            }
        }

        @Override
        public void serializeTo(JsonArray json) {
            json.add("#" + this.id);
        }

        @Override
        public String toString() {
            return "#" + this.id;
        }

        @Override
        public boolean verifyIfPresent(Predicate<ResourceLocation> objectExistsTest, Predicate<ResourceLocation> tagExistsTest) {
            return tagExistsTest.test(this.id);
        }

        @Override
        public void visitRequiredDependencies(Consumer<ResourceLocation> consumer) {
            consumer.accept(this.id);
        }
    }
}
