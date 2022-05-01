package net.minecraft.world.level.storage.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import java.lang.reflect.Type;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class GsonAdapterFactory {
    public static <E, T extends SerializerType<E>> GsonAdapterFactory.Builder<E, T> builder(Registry<T> registry, String rootFieldName, String idFieldName, Function<E, T> typeGetter) {
        return new GsonAdapterFactory.Builder<>(registry, rootFieldName, idFieldName, typeGetter);
    }

    public static class Builder<E, T extends SerializerType<E>> {
        private final Registry<T> registry;
        private final String elementName;
        private final String typeKey;
        private final Function<E, T> typeGetter;
        @Nullable
        private Pair<T, GsonAdapterFactory.InlineSerializer<? extends E>> inlineType;
        @Nullable
        private T defaultType;

        Builder(Registry<T> registry, String rootFieldName, String idFieldName, Function<E, T> typeIdentification) {
            this.registry = registry;
            this.elementName = rootFieldName;
            this.typeKey = idFieldName;
            this.typeGetter = typeIdentification;
        }

        public GsonAdapterFactory.Builder<E, T> withInlineSerializer(T type, GsonAdapterFactory.InlineSerializer<? extends E> serializer) {
            this.inlineType = Pair.of(type, serializer);
            return this;
        }

        public GsonAdapterFactory.Builder<E, T> withDefaultType(T defaultType) {
            this.defaultType = defaultType;
            return this;
        }

        public Object build() {
            return new GsonAdapterFactory.JsonAdapter<>(this.registry, this.elementName, this.typeKey, this.typeGetter, this.defaultType, this.inlineType);
        }
    }

    public interface InlineSerializer<T> {
        JsonElement serialize(T object, JsonSerializationContext context);

        T deserialize(JsonElement json, JsonDeserializationContext context);
    }

    static class JsonAdapter<E, T extends SerializerType<E>> implements JsonDeserializer<E>, JsonSerializer<E> {
        private final Registry<T> registry;
        private final String elementName;
        private final String typeKey;
        private final Function<E, T> typeGetter;
        @Nullable
        private final T defaultType;
        @Nullable
        private final Pair<T, GsonAdapterFactory.InlineSerializer<? extends E>> inlineType;

        JsonAdapter(Registry<T> registry, String rootFieldName, String idFieldName, Function<E, T> typeGetter, @Nullable T defaultType, @Nullable Pair<T, GsonAdapterFactory.InlineSerializer<? extends E>> elementSerializer) {
            this.registry = registry;
            this.elementName = rootFieldName;
            this.typeKey = idFieldName;
            this.typeGetter = typeGetter;
            this.defaultType = defaultType;
            this.inlineType = elementSerializer;
        }

        @Override
        public E deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, this.elementName);
                String string = GsonHelper.getAsString(jsonObject, this.typeKey, "");
                T serializerType;
                if (string.isEmpty()) {
                    serializerType = this.defaultType;
                } else {
                    ResourceLocation resourceLocation = new ResourceLocation(string);
                    serializerType = this.registry.get(resourceLocation);
                }

                if (serializerType == null) {
                    throw new JsonSyntaxException("Unknown type '" + string + "'");
                } else {
                    return serializerType.getSerializer().deserialize(jsonObject, jsonDeserializationContext);
                }
            } else if (this.inlineType == null) {
                throw new UnsupportedOperationException("Object " + jsonElement + " can't be deserialized");
            } else {
                return this.inlineType.getSecond().deserialize(jsonElement, jsonDeserializationContext);
            }
        }

        @Override
        public JsonElement serialize(E object, Type type, JsonSerializationContext jsonSerializationContext) {
            T serializerType = this.typeGetter.apply(object);
            if (this.inlineType != null && this.inlineType.getFirst() == serializerType) {
                return this.inlineType.getSecond().serialize(object, jsonSerializationContext);
            } else if (serializerType == null) {
                throw new JsonSyntaxException("Unknown type: " + object);
            } else {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty(this.typeKey, this.registry.getKey(serializerType).toString());
                serializerType.getSerializer().serialize(jsonObject, object, jsonSerializationContext);
                return jsonObject;
            }
        }
    }
}
