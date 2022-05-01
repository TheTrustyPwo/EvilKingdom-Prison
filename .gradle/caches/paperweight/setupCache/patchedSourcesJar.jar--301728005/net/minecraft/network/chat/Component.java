package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import io.papermc.paper.adventure.AdventureComponent; // Paper
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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LowerCaseEnumTypeAdapterFactory;
// CraftBukkit start
import com.google.common.collect.Streams;
import java.util.stream.Stream;
// CraftBukkit end

public interface Component extends Message, FormattedText, Iterable<Component> { // CraftBukkit

    // CraftBukkit start
    default Stream<Component> stream() {
        return Streams.concat(new Stream[]{Stream.of(this), this.getSiblings().stream().flatMap(Component::stream)});
    }

    @Override
    default Iterator<Component> iterator() {
        return this.stream().iterator();
    }
    // CraftBukkit end

    Style getStyle();

    String getContents();

    @Override
    default String getString() {
        return FormattedText.super.getString();
    }

    default String getString(int length) {
        StringBuilder stringbuilder = new StringBuilder();

        this.visit((s) -> {
            int j = length - stringbuilder.length();

            if (j <= 0) {
                return Component.STOP_ITERATION;
            } else {
                stringbuilder.append(s.length() <= j ? s : s.substring(0, j));
                return Optional.empty();
            }
        });
        return stringbuilder.toString();
    }

    List<Component> getSiblings();

    MutableComponent plainCopy();

    MutableComponent copy();

    FormattedCharSequence getVisualOrderText();

    @Override
    default <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> styledVisitor, Style style) {
        Style chatmodifier1 = this.getStyle().applyTo(style);
        Optional<T> optional = this.visitSelf(styledVisitor, chatmodifier1);

        if (optional.isPresent()) {
            return optional;
        } else {
            Iterator iterator = this.getSiblings().iterator();

            Optional optional1;

            do {
                if (!iterator.hasNext()) {
                    return Optional.empty();
                }

                Component ichatbasecomponent = (Component) iterator.next();

                optional1 = ichatbasecomponent.visit(styledVisitor, chatmodifier1);
            } while (!optional1.isPresent());

            return optional1;
        }
    }

    @Override
    default <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
        Optional<T> optional = this.visitSelf(visitor);

        if (optional.isPresent()) {
            return optional;
        } else {
            Iterator iterator = this.getSiblings().iterator();

            Optional optional1;

            do {
                if (!iterator.hasNext()) {
                    return Optional.empty();
                }

                Component ichatbasecomponent = (Component) iterator.next();

                optional1 = ichatbasecomponent.visit(visitor);
            } while (!optional1.isPresent());

            return optional1;
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

        this.visit((chatmodifier1, s) -> {
            if (!s.isEmpty()) {
                list.add((new TextComponent(s)).withStyle(chatmodifier1));
            }

            return Optional.empty();
        }, style);
        return list;
    }

    static Component nullToEmpty(@Nullable String string) {
        return (Component) (string != null ? new TextComponent(string) : TextComponent.EMPTY);
    }

    public static class Serializer implements JsonDeserializer<MutableComponent>, JsonSerializer<Component> {

        private static final Gson GSON = (Gson) Util.make(() -> {
            GsonBuilder gsonbuilder = new GsonBuilder();

            gsonbuilder.disableHtmlEscaping();
            gsonbuilder.registerTypeAdapter(AdventureComponent.class, new AdventureComponent.Serializer()); // Paper
            gsonbuilder.registerTypeHierarchyAdapter(Component.class, new Component.Serializer());
            gsonbuilder.registerTypeHierarchyAdapter(Style.class, new Style.Serializer());
            gsonbuilder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
            return gsonbuilder.create();
        });
        private static final Field JSON_READER_POS = (Field) Util.make(() -> {
            try {
                new JsonReader(new StringReader(""));
                Field field = JsonReader.class.getDeclaredField("pos");

                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException nosuchfieldexception) {
                throw new IllegalStateException("Couldn't get field 'pos' for JsonReader", nosuchfieldexception);
            }
        });
        private static final Field JSON_READER_LINESTART = (Field) Util.make(() -> {
            try {
                new JsonReader(new StringReader(""));
                Field field = JsonReader.class.getDeclaredField("lineStart");

                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException nosuchfieldexception) {
                throw new IllegalStateException("Couldn't get field 'lineStart' for JsonReader", nosuchfieldexception);
            }
        });

        public Serializer() {}

        public MutableComponent deserialize(JsonElement jsonelement, Type type, JsonDeserializationContext jsondeserializationcontext) throws JsonParseException {
            if (jsonelement.isJsonPrimitive()) {
                return new TextComponent(jsonelement.getAsString());
            } else if (!jsonelement.isJsonObject()) {
                if (jsonelement.isJsonArray()) {
                    JsonArray jsonarray = jsonelement.getAsJsonArray();
                    MutableComponent ichatmutablecomponent = null;
                    Iterator iterator = jsonarray.iterator();

                    while (iterator.hasNext()) {
                        JsonElement jsonelement1 = (JsonElement) iterator.next();
                        MutableComponent ichatmutablecomponent1 = this.deserialize(jsonelement1, jsonelement1.getClass(), jsondeserializationcontext);

                        if (ichatmutablecomponent == null) {
                            ichatmutablecomponent = ichatmutablecomponent1;
                        } else {
                            ichatmutablecomponent.append((Component) ichatmutablecomponent1);
                        }
                    }

                    return ichatmutablecomponent;
                } else {
                    throw new JsonParseException("Don't know how to turn " + jsonelement + " into a Component");
                }
            } else {
                JsonObject jsonobject = jsonelement.getAsJsonObject();
                Object object;

                if (jsonobject.has("text")) {
                    object = new TextComponent(GsonHelper.getAsString(jsonobject, "text"));
                } else {
                    String s;

                    if (jsonobject.has("translate")) {
                        s = GsonHelper.getAsString(jsonobject, "translate");
                        if (jsonobject.has("with")) {
                            JsonArray jsonarray1 = GsonHelper.getAsJsonArray(jsonobject, "with");
                            Object[] aobject = new Object[jsonarray1.size()];

                            for (int i = 0; i < aobject.length; ++i) {
                                aobject[i] = this.deserialize(jsonarray1.get(i), type, jsondeserializationcontext);
                                if (aobject[i] instanceof TextComponent) {
                                    TextComponent chatcomponenttext = (TextComponent) aobject[i];

                                    if (chatcomponenttext.getStyle().isEmpty() && chatcomponenttext.getSiblings().isEmpty()) {
                                        aobject[i] = chatcomponenttext.getText();
                                    }
                                }
                            }

                            object = new TranslatableComponent(s, aobject);
                        } else {
                            object = new TranslatableComponent(s);
                        }
                    } else if (jsonobject.has("score")) {
                        JsonObject jsonobject1 = GsonHelper.getAsJsonObject(jsonobject, "score");

                        if (!jsonobject1.has("name") || !jsonobject1.has("objective")) {
                            throw new JsonParseException("A score component needs a least a name and an objective");
                        }

                        object = new ScoreComponent(GsonHelper.getAsString(jsonobject1, "name"), GsonHelper.getAsString(jsonobject1, "objective"));
                    } else if (jsonobject.has("selector")) {
                        Optional<Component> optional = this.parseSeparator(type, jsondeserializationcontext, jsonobject);

                        object = new SelectorComponent(GsonHelper.getAsString(jsonobject, "selector"), optional);
                    } else if (jsonobject.has("keybind")) {
                        object = new KeybindComponent(GsonHelper.getAsString(jsonobject, "keybind"));
                    } else {
                        if (!jsonobject.has("nbt")) {
                            throw new JsonParseException("Don't know how to turn " + jsonelement + " into a Component");
                        }

                        s = GsonHelper.getAsString(jsonobject, "nbt");
                        Optional<Component> optional1 = this.parseSeparator(type, jsondeserializationcontext, jsonobject);
                        boolean flag = GsonHelper.getAsBoolean(jsonobject, "interpret", false);

                        if (jsonobject.has("block")) {
                            object = new NbtComponent.BlockNbtComponent(s, flag, GsonHelper.getAsString(jsonobject, "block"), optional1);
                        } else if (jsonobject.has("entity")) {
                            object = new NbtComponent.EntityNbtComponent(s, flag, GsonHelper.getAsString(jsonobject, "entity"), optional1);
                        } else {
                            if (!jsonobject.has("storage")) {
                                throw new JsonParseException("Don't know how to turn " + jsonelement + " into a Component");
                            }

                            object = new NbtComponent.StorageNbtComponent(s, flag, new ResourceLocation(GsonHelper.getAsString(jsonobject, "storage")), optional1);
                        }
                    }
                }

                if (jsonobject.has("extra")) {
                    JsonArray jsonarray2 = GsonHelper.getAsJsonArray(jsonobject, "extra");

                    if (jsonarray2.size() <= 0) {
                        throw new JsonParseException("Unexpected empty array of components");
                    }

                    for (int j = 0; j < jsonarray2.size(); ++j) {
                        ((MutableComponent) object).append((Component) this.deserialize(jsonarray2.get(j), type, jsondeserializationcontext));
                    }
                }

                ((MutableComponent) object).setStyle((Style) jsondeserializationcontext.deserialize(jsonelement, Style.class));
                return (MutableComponent) object;
            }
        }

        private Optional<Component> parseSeparator(Type type, JsonDeserializationContext context, JsonObject json) {
            return json.has("separator") ? Optional.of(this.deserialize(json.get("separator"), type, context)) : Optional.empty();
        }

        private void serializeStyle(Style style, JsonObject json, JsonSerializationContext context) {
            JsonElement jsonelement = context.serialize(style);

            if (jsonelement.isJsonObject()) {
                JsonObject jsonobject1 = (JsonObject) jsonelement;
                Iterator iterator = jsonobject1.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry<String, JsonElement> entry = (Entry) iterator.next();

                    json.add((String) entry.getKey(), (JsonElement) entry.getValue());
                }
            }

        }

        public JsonElement serialize(Component ichatbasecomponent, Type type, JsonSerializationContext jsonserializationcontext) {
            if (ichatbasecomponent instanceof AdventureComponent) return jsonserializationcontext.serialize(ichatbasecomponent); // Paper
            JsonObject jsonobject = new JsonObject();

            if (!ichatbasecomponent.getStyle().isEmpty()) {
                this.serializeStyle(ichatbasecomponent.getStyle(), jsonobject, jsonserializationcontext);
            }

            if (!ichatbasecomponent.getSiblings().isEmpty()) {
                JsonArray jsonarray = new JsonArray();
                Iterator iterator = ichatbasecomponent.getSiblings().iterator();

                while (iterator.hasNext()) {
                    Component ichatbasecomponent1 = (Component) iterator.next();

                    jsonarray.add(this.serialize(ichatbasecomponent1, ichatbasecomponent1.getClass(), jsonserializationcontext));
                }

                jsonobject.add("extra", jsonarray);
            }

            if (ichatbasecomponent instanceof TextComponent) {
                jsonobject.addProperty("text", ((TextComponent) ichatbasecomponent).getText());
            } else if (ichatbasecomponent instanceof TranslatableComponent) {
                TranslatableComponent chatmessage = (TranslatableComponent) ichatbasecomponent;

                jsonobject.addProperty("translate", chatmessage.getKey());
                if (chatmessage.getArgs() != null && chatmessage.getArgs().length > 0) {
                    JsonArray jsonarray1 = new JsonArray();
                    Object[] aobject = chatmessage.getArgs();
                    int i = aobject.length;

                    for (int j = 0; j < i; ++j) {
                        Object object = aobject[j];

                        if (object instanceof Component) {
                            jsonarray1.add(this.serialize((Component) object, object.getClass(), jsonserializationcontext));
                        } else {
                            jsonarray1.add(new JsonPrimitive(String.valueOf(object)));
                        }
                    }

                    jsonobject.add("with", jsonarray1);
                }
            } else if (ichatbasecomponent instanceof ScoreComponent) {
                ScoreComponent chatcomponentscore = (ScoreComponent) ichatbasecomponent;
                JsonObject jsonobject1 = new JsonObject();

                jsonobject1.addProperty("name", chatcomponentscore.getName());
                jsonobject1.addProperty("objective", chatcomponentscore.getObjective());
                jsonobject.add("score", jsonobject1);
            } else if (ichatbasecomponent instanceof SelectorComponent) {
                SelectorComponent chatcomponentselector = (SelectorComponent) ichatbasecomponent;

                jsonobject.addProperty("selector", chatcomponentselector.getPattern());
                this.serializeSeparator(jsonserializationcontext, jsonobject, chatcomponentselector.getSeparator());
            } else if (ichatbasecomponent instanceof KeybindComponent) {
                KeybindComponent chatcomponentkeybind = (KeybindComponent) ichatbasecomponent;

                jsonobject.addProperty("keybind", chatcomponentkeybind.getName());
            } else {
                if (!(ichatbasecomponent instanceof NbtComponent)) {
                    throw new IllegalArgumentException("Don't know how to serialize " + ichatbasecomponent + " as a Component");
                }

                NbtComponent chatcomponentnbt = (NbtComponent) ichatbasecomponent;

                jsonobject.addProperty("nbt", chatcomponentnbt.getNbtPath());
                jsonobject.addProperty("interpret", chatcomponentnbt.isInterpreting());
                this.serializeSeparator(jsonserializationcontext, jsonobject, chatcomponentnbt.separator);
                if (ichatbasecomponent instanceof NbtComponent.BlockNbtComponent) {
                    NbtComponent.BlockNbtComponent chatcomponentnbt_a = (NbtComponent.BlockNbtComponent) ichatbasecomponent;

                    jsonobject.addProperty("block", chatcomponentnbt_a.getPos());
                } else if (ichatbasecomponent instanceof NbtComponent.EntityNbtComponent) {
                    NbtComponent.EntityNbtComponent chatcomponentnbt_b = (NbtComponent.EntityNbtComponent) ichatbasecomponent;

                    jsonobject.addProperty("entity", chatcomponentnbt_b.getSelector());
                } else {
                    if (!(ichatbasecomponent instanceof NbtComponent.StorageNbtComponent)) {
                        throw new IllegalArgumentException("Don't know how to serialize " + ichatbasecomponent + " as a Component");
                    }

                    NbtComponent.StorageNbtComponent chatcomponentnbt_c = (NbtComponent.StorageNbtComponent) ichatbasecomponent;

                    jsonobject.addProperty("storage", chatcomponentnbt_c.getId().toString());
                }
            }

            return jsonobject;
        }

        private void serializeSeparator(JsonSerializationContext context, JsonObject json, Optional<Component> separator) {
            separator.ifPresent((ichatbasecomponent) -> {
                json.add("separator", this.serialize(ichatbasecomponent, ichatbasecomponent.getClass(), context));
            });
        }

        public static String toJson(Component text) {
            return Component.Serializer.GSON.toJson(text);
        }

        public static JsonElement toJsonTree(Component text) {
            return Component.Serializer.GSON.toJsonTree(text);
        }

        @Nullable
        public static MutableComponent fromJson(String json) {
            return (MutableComponent) GsonHelper.fromJson(Component.Serializer.GSON, json, MutableComponent.class, false);
        }

        @Nullable
        public static MutableComponent fromJson(JsonElement json) {
            return (MutableComponent) Component.Serializer.GSON.fromJson(json, MutableComponent.class);
        }

        @Nullable
        public static MutableComponent fromJsonLenient(String json) {
            return (MutableComponent) GsonHelper.fromJson(Component.Serializer.GSON, json, MutableComponent.class, true);
        }

        public static MutableComponent fromJson(com.mojang.brigadier.StringReader reader) {
            try {
                JsonReader jsonreader = new JsonReader(new StringReader(reader.getRemaining()));

                jsonreader.setLenient(false);
                MutableComponent ichatmutablecomponent = (MutableComponent) Component.Serializer.GSON.getAdapter(MutableComponent.class).read(jsonreader);

                reader.setCursor(reader.getCursor() + Serializer.getPos(jsonreader));
                return ichatmutablecomponent;
            } catch (StackOverflowError | IOException ioexception) {
                throw new JsonParseException(ioexception);
            }
        }

        private static int getPos(JsonReader reader) {
            try {
                return Component.Serializer.JSON_READER_POS.getInt(reader) - Component.Serializer.JSON_READER_LINESTART.getInt(reader) + 1;
            } catch (IllegalAccessException illegalaccessexception) {
                throw new IllegalStateException("Couldn't read position of JsonReader", illegalaccessexception);
            }
        }
    }
}
