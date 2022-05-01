package net.minecraft.data.models.blockstates;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Variant implements Supplier<JsonElement> {
    private final Map<VariantProperty<?>, VariantProperty<?>.Value> values = Maps.newLinkedHashMap();

    public <T> Variant with(VariantProperty<T> key, T value) {
        VariantProperty<?>.Value value2 = this.values.put(key, key.withValue(value));
        if (value2 != null) {
            throw new IllegalStateException("Replacing value of " + value2 + " with " + value);
        } else {
            return this;
        }
    }

    public static Variant variant() {
        return new Variant();
    }

    public static Variant merge(Variant first, Variant second) {
        Variant variant = new Variant();
        variant.values.putAll(first.values);
        variant.values.putAll(second.values);
        return variant;
    }

    @Override
    public JsonElement get() {
        JsonObject jsonObject = new JsonObject();
        this.values.values().forEach((value) -> {
            value.addToVariant(jsonObject);
        });
        return jsonObject;
    }

    public static JsonElement convertList(List<Variant> variants) {
        if (variants.size() == 1) {
            return variants.get(0).get();
        } else {
            JsonArray jsonArray = new JsonArray();
            variants.forEach((variant) -> {
                jsonArray.add(variant.get());
            });
            return jsonArray;
        }
    }
}
