package net.minecraft.util.datafix.fixes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.lang.reflect.Type;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.GsonHelper;
import org.apache.commons.lang3.StringUtils;

public class BlockEntitySignTextStrictJsonFix extends NamedEntityFix {
    public static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(Component.class, new JsonDeserializer<Component>() {
        @Override
        public MutableComponent deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonPrimitive()) {
                return new TextComponent(jsonElement.getAsString());
            } else if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                MutableComponent mutableComponent = null;

                for(JsonElement jsonElement2 : jsonArray) {
                    MutableComponent mutableComponent2 = this.deserialize(jsonElement2, jsonElement2.getClass(), jsonDeserializationContext);
                    if (mutableComponent == null) {
                        mutableComponent = mutableComponent2;
                    } else {
                        mutableComponent.append(mutableComponent2);
                    }
                }

                return mutableComponent;
            } else {
                throw new JsonParseException("Don't know how to turn " + jsonElement + " into a Component");
            }
        }
    }).create();

    public BlockEntitySignTextStrictJsonFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "BlockEntitySignTextStrictJsonFix", References.BLOCK_ENTITY, "Sign");
    }

    private Dynamic<?> updateLine(Dynamic<?> dynamic, String lineName) {
        String string = dynamic.get(lineName).asString("");
        Component component = null;
        if (!"null".equals(string) && !StringUtils.isEmpty(string)) {
            if (string.charAt(0) == '"' && string.charAt(string.length() - 1) == '"' || string.charAt(0) == '{' && string.charAt(string.length() - 1) == '}') {
                try {
                    component = GsonHelper.fromJson(GSON, string, Component.class, true);
                    if (component == null) {
                        component = TextComponent.EMPTY;
                    }
                } catch (Exception var8) {
                }

                if (component == null) {
                    try {
                        component = Component.Serializer.fromJson(string);
                    } catch (Exception var7) {
                    }
                }

                if (component == null) {
                    try {
                        component = Component.Serializer.fromJsonLenient(string);
                    } catch (Exception var6) {
                    }
                }

                if (component == null) {
                    component = new TextComponent(string);
                }
            } else {
                component = new TextComponent(string);
            }
        } else {
            component = TextComponent.EMPTY;
        }

        return dynamic.set(lineName, dynamic.createString(Component.Serializer.toJson(component)));
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), (dynamic) -> {
            dynamic = this.updateLine(dynamic, "Text1");
            dynamic = this.updateLine(dynamic, "Text2");
            dynamic = this.updateLine(dynamic, "Text3");
            return this.updateLine(dynamic, "Text4");
        });
    }
}
