package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.Message;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LowerCaseEnumTypeAdapterFactory;

public interface Component extends Message, FormattedText {
    Style getStyle();

    String getContents();

    @Override
    default String getString() {
        return FormattedText.super.getString();
    }

    default String getString(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        this.visit((string) -> {
            int j = length - stringBuilder.length();
            if (j <= 0) {
                return STOP_ITERATION;
            } else {
                stringBuilder.append(string.length() <= j ? string : string.substring(0, j));
                return Optional.empty();
            }
        });
        return stringBuilder.toString();
    }

    List<Component> getSiblings();

    MutableComponent plainCopy();

    MutableComponent copy();

    FormattedCharSequence getVisualOrderText();

    @Override
    default <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> styledVisitor, Style style) {
        Style style2 = this.getStyle().applyTo(style);
        Optional<T> optional = this.visitSelf(styledVisitor, style2);
        if (optional.isPresent()) {
            return optional;
        } else {
            for(Component component : this.getSiblings()) {
                Optional<T> optional2 = component.visit(styledVisitor, style2);
                if (optional2.isPresent()) {
                    return optional2;
                }
            }

            return Optional.empty();
        }
    }

    @Override
    default <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
        Optional<T> optional = this.visitSelf(visitor);
        if (optional.isPresent()) {
            return optional;
        } else {
            for(Component component : this.getSiblings()) {
                Optional<T> optional2 = component.visit(visitor);
                if (optional2.isPresent()) {
                    return optional2;
                }
            }

            return Optional.empty();
        }
    }

    default <T> Optional<T> visitSelf(FormattedText.StyledContentConsumer<T> visitor, Style style) {
        return visitor.accept(style, this.getContents());
    }

    default <T> Optional<T> visitSelf(FormattedText.ContentConsumer<T> visitor) {
        return visitor.accept(this.getContents());
    }

    default List<Component> toFlatList(Style style) {
        List<Component> list = Lists.newArrayList();
        this.visit((styleOverride, text) -> {
            if (!text.isEmpty()) {
                list.add((new TextComponent(text)).withStyle(styleOverride));
            }

            return Optional.empty();
        }, style);
        return list;
    }

    static Component nullToEmpty(@Nullable String string) {
        return (Component)(string != null ? new TextComponent(string) : TextComponent.EMPTY);
    }

    public static class Serializer implements JsonDeserializer<MutableComponent>, JsonSerializer<Component> {
        private static final Gson GSON = Util.make(() -> {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.disableHtmlEscaping();
            gsonBuilder.registerTypeHierarchyAdapter(Component.class, new Component.Serializer());
            gsonBuilder.registerTypeHierarchyAdapter(Style.class, new Style.Serializer());
            gsonBuilder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
            return gsonBuilder.create();
        });
        private static final Field JSON_READER_POS = Util.make(() -> {
            try {
                new JsonReader(new StringReader(""));
                Field field = JsonReader.class.getDeclaredField("pos");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException var1) {
                throw new IllegalStateException("Couldn't get field 'pos' for JsonReader", var1);
            }
        });
        private static final Field JSON_READER_LINESTART = Util.make(() -> {
            try {
                new JsonReader(new StringReader(""));
                Field field = JsonReader.class.getDeclaredField("lineStart");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException var1) {
                throw new IllegalStateException("Couldn't get field 'lineStart' for JsonReader", var1);
            }
        });

        @Override
        public MutableComponent deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonPrimitive()) {
                return new TextComponent(jsonElement.getAsString());
            } else if (!jsonElement.isJsonObject()) {
                if (jsonElement.isJsonArray()) {
                    JsonArray jsonArray3 = jsonElement.getAsJsonArray();
                    MutableComponent mutableComponent13 = null;

                    for(JsonElement jsonElement2 : jsonArray3) {
                        MutableComponent mutableComponent14 = this.deserialize(jsonElement2, jsonElement2.getClass(), jsonDeserializationContext);
                        if (mutableComponent13 == null) {
                            mutableComponent13 = mutableComponent14;
                        } else {
                            mutableComponent13.append(mutableComponent14);
                        }
                    }

                    return mutableComponent13;
                } else {
                    throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
                }
            } else {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                MutableComponent mutableComponent;
                if (jsonObject.has("text")) {
                    mutableComponent = new TextComponent(GsonHelper.getAsString(jsonObject, "text"));
                } else if (jsonObject.has("translate")) {
                    String string = GsonHelper.getAsString(jsonObject, "translate");
                    if (jsonObject.has("with")) {
                        JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "with");
                        Object[] objects = new Object[jsonArray.size()];

                        for(int i = 0; i < objects.length; ++i) {
                            objects[i] = this.deserialize(jsonArray.get(i), type, jsonDeserializationContext);
                            if (objects[i] instanceof TextComponent) {
                                TextComponent textComponent = (TextComponent)objects[i];
                                if (textComponent.getStyle().isEmpty() && textComponent.getSiblings().isEmpty()) {
                                    objects[i] = textComponent.getText();
                                }
                            }
                        }

                        mutableComponent = new TranslatableComponent(string, objects);
                    } else {
                        mutableComponent = new TranslatableComponent(string);
                    }
                } else if (jsonObject.has("score")) {
                    JsonObject jsonObject2 = GsonHelper.getAsJsonObject(jsonObject, "score");
                    if (!jsonObject2.has("name") || !jsonObject2.has("objective")) {
                        throw new JsonParseException("A score component needs a least a name and an objective");
                    }

                    mutableComponent = new ScoreComponent(GsonHelper.getAsString(jsonObject2, "name"), GsonHelper.getAsString(jsonObject2, "objective"));
                } else if (jsonObject.has("selector")) {
                    Optional<Component> optional = this.parseSeparator(type, jsonDeserializationContext, jsonObject);
                    mutableComponent = new SelectorComponent(GsonHelper.getAsString(jsonObject, "selector"), optional);
                } else if (jsonObject.has("keybind")) {
                    mutableComponent = new KeybindComponent(GsonHelper.getAsString(jsonObject, "keybind"));
                } else {
                    if (!jsonObject.has("nbt")) {
                        throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
                    }

                    String string2 = GsonHelper.getAsString(jsonObject, "nbt");
                    Optional<Component> optional2 = this.parseSeparator(type, jsonDeserializationContext, jsonObject);
                    boolean bl = GsonHelper.getAsBoolean(jsonObject, "interpret", false);
                    if (jsonObject.has("block")) {
                        mutableComponent = new NbtComponent.BlockNbtComponent(string2, bl, GsonHelper.getAsString(jsonObject, "block"), optional2);
                    } else if (jsonObject.has("entity")) {
                        mutableComponent = new NbtComponent.EntityNbtComponent(string2, bl, GsonHelper.getAsString(jsonObject, "entity"), optional2);
                    } else {
                        if (!jsonObject.has("storage")) {
                            throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
                        }

                        mutableComponent = new NbtComponent.StorageNbtComponent(string2, bl, new ResourceLocation(GsonHelper.getAsString(jsonObject, "storage")), optional2);
                    }
                }

                if (jsonObject.has("extra")) {
                    JsonArray jsonArray2 = GsonHelper.getAsJsonArray(jsonObject, "extra");
                    if (jsonArray2.size() <= 0) {
                        throw new JsonParseException("Unexpected empty array of components");
                    }

                    for(int j = 0; j < jsonArray2.size(); ++j) {
                        mutableComponent.append(this.deserialize(jsonArray2.get(j), type, jsonDeserializationContext));
                    }
                }

                mutableComponent.setStyle(jsonDeserializationContext.deserialize(jsonElement, Style.class));
                return mutableComponent;
            }
        }

        private Optional<Component> parseSeparator(Type type, JsonDeserializationContext context, JsonObject json) {
            return json.has("separator") ? Optional.of(this.deserialize(json.get("separator"), type, context)) : Optional.empty();
        }

        private void serializeStyle(Style style, JsonObject json, JsonSerializationContext context) {
            JsonElement jsonElement = context.serialize(style);
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = (JsonObject)jsonElement;

                for(Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    json.add(entry.getKey(), entry.getValue());
                }
            }

        }

        @Override
        public JsonElement serialize(Component component, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            if (!component.getStyle().isEmpty()) {
                this.serializeStyle(component.getStyle(), jsonObject, jsonSerializationContext);
            }

            if (!component.getSiblings().isEmpty()) {
                JsonArray jsonArray = new JsonArray();

                for(Component component2 : component.getSiblings()) {
                    jsonArray.add(this.serialize(component2, component2.getClass(), jsonSerializationContext));
                }

                jsonObject.add("extra", jsonArray);
            }

            if (component instanceof TextComponent) {
                jsonObject.addProperty("text", ((TextComponent)component).getText());
            } else if (component instanceof TranslatableComponent) {
                TranslatableComponent translatableComponent = (TranslatableComponent)component;
                jsonObject.addProperty("translate", translatableComponent.getKey());
                if (translatableComponent.getArgs() != null && translatableComponent.getArgs().length > 0) {
                    JsonArray jsonArray2 = new JsonArray();

                    for(Object object : translatableComponent.getArgs()) {
                        if (object instanceof Component) {
                            jsonArray2.add(this.serialize((Component)object, object.getClass(), jsonSerializationContext));
                        } else {
                            jsonArray2.add(new JsonPrimitive(String.valueOf(object)));
                        }
                    }

                    jsonObject.add("with", jsonArray2);
                }
            } else if (component instanceof ScoreComponent) {
                ScoreComponent scoreComponent = (ScoreComponent)component;
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.addProperty("name", scoreComponent.getName());
                jsonObject2.addProperty("objective", scoreComponent.getObjective());
                jsonObject.add("score", jsonObject2);
            } else if (component instanceof SelectorComponent) {
                SelectorComponent selectorComponent = (SelectorComponent)component;
                jsonObject.addProperty("selector", selectorComponent.getPattern());
                this.serializeSeparator(jsonSerializationContext, jsonObject, selectorComponent.getSeparator());
            } else if (component instanceof KeybindComponent) {
                KeybindComponent keybindComponent = (KeybindComponent)component;
                jsonObject.addProperty("keybind", keybindComponent.getName());
            } else {
                if (!(component instanceof NbtComponent)) {
                    throw new IllegalArgumentException("Don't know how to serialize " + component + " as a Component");
                }

                NbtComponent nbtComponent = (NbtComponent)component;
                jsonObject.addProperty("nbt", nbtComponent.getNbtPath());
                jsonObject.addProperty("interpret", nbtComponent.isInterpreting());
                this.serializeSeparator(jsonSerializationContext, jsonObject, nbtComponent.separator);
                if (component instanceof NbtComponent.BlockNbtComponent) {
                    NbtComponent.BlockNbtComponent blockNbtComponent = (NbtComponent.BlockNbtComponent)component;
                    jsonObject.addProperty("block", blockNbtComponent.getPos());
                } else if (component instanceof NbtComponent.EntityNbtComponent) {
                    NbtComponent.EntityNbtComponent entityNbtComponent = (NbtComponent.EntityNbtComponent)component;
                    jsonObject.addProperty("entity", entityNbtComponent.getSelector());
                } else {
                    if (!(component instanceof NbtComponent.StorageNbtComponent)) {
                        throw new IllegalArgumentException("Don't know how to serialize " + component + " as a Component");
                    }

                    NbtComponent.StorageNbtComponent storageNbtComponent = (NbtComponent.StorageNbtComponent)component;
                    jsonObject.addProperty("storage", storageNbtComponent.getId().toString());
                }
            }

            return jsonObject;
        }

        private void serializeSeparator(JsonSerializationContext context, JsonObject json, Optional<Component> separator) {
            separator.ifPresent((separatorx) -> {
                json.add("separator", this.serialize(separatorx, separatorx.getClass(), context));
            });
        }

        public static String toJson(Component text) {
            return GSON.toJson(text);
        }

        public static JsonElement toJsonTree(Component text) {
            return GSON.toJsonTree(text);
        }

        @Nullable
        public static MutableComponent fromJson(String json) {
            return GsonHelper.fromJson(GSON, json, MutableComponent.class, false);
        }

        @Nullable
        public static MutableComponent fromJson(JsonElement json) {
            return GSON.fromJson(json, MutableComponent.class);
        }

        @Nullable
        public static MutableComponent fromJsonLenient(String json) {
            return GsonHelper.fromJson(GSON, json, MutableComponent.class, true);
        }

        public static MutableComponent fromJson(com.mojang.brigadier.StringReader reader) {
            try {
                JsonReader jsonReader = new JsonReader(new StringReader(reader.getRemaining()));
                jsonReader.setLenient(false);
                MutableComponent mutableComponent = GSON.getAdapter(MutableComponent.class).read(jsonReader);
                reader.setCursor(reader.getCursor() + getPos(jsonReader));
                return mutableComponent;
            } catch (StackOverflowError | IOException var3) {
                throw new JsonParseException(var3);
            }
        }

        private static int getPos(JsonReader reader) {
            try {
                return JSON_READER_POS.getInt(reader) - JSON_READER_LINESTART.getInt(reader) + 1;
            } catch (IllegalAccessException var2) {
                throw new IllegalStateException("Couldn't read position of JsonReader", var2);
            }
        }
    }
}
