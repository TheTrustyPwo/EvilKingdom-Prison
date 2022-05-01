package net.minecraft.world.level.storage.loot.providers.number;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.GsonAdapterFactory;
import net.minecraft.world.level.storage.loot.LootContext;

public final class ConstantValue implements NumberProvider {
    final float value;

    ConstantValue(float value) {
        this.value = value;
    }

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.CONSTANT;
    }

    @Override
    public float getFloat(LootContext context) {
        return this.value;
    }

    public static ConstantValue exactly(float value) {
        return new ConstantValue(value);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            return Float.compare(((ConstantValue)object).value, this.value) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.value != 0.0F ? Float.floatToIntBits(this.value) : 0;
    }

    public static class InlineSerializer implements GsonAdapterFactory.InlineSerializer<ConstantValue> {
        @Override
        public JsonElement serialize(ConstantValue object, JsonSerializationContext context) {
            return new JsonPrimitive(object.value);
        }

        @Override
        public ConstantValue deserialize(JsonElement jsonElement, JsonDeserializationContext jsonDeserializationContext) {
            return new ConstantValue(GsonHelper.convertToFloat(jsonElement, "value"));
        }
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ConstantValue> {
        @Override
        public void serialize(JsonObject json, ConstantValue object, JsonSerializationContext context) {
            json.addProperty("value", object.value);
        }

        @Override
        public ConstantValue deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            float f = GsonHelper.getAsFloat(jsonObject, "value");
            return new ConstantValue(f);
        }
    }
}
