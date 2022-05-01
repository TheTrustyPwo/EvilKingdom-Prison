package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class IntRange {
    @Nullable
    final NumberProvider min;
    @Nullable
    final NumberProvider max;
    private final IntRange.IntLimiter limiter;
    private final IntRange.IntChecker predicate;

    public Set<LootContextParam<?>> getReferencedContextParams() {
        Builder<LootContextParam<?>> builder = ImmutableSet.builder();
        if (this.min != null) {
            builder.addAll(this.min.getReferencedContextParams());
        }

        if (this.max != null) {
            builder.addAll(this.max.getReferencedContextParams());
        }

        return builder.build();
    }

    IntRange(@Nullable NumberProvider min, @Nullable NumberProvider max) {
        this.min = min;
        this.max = max;
        if (min == null) {
            if (max == null) {
                this.limiter = (context, value) -> {
                    return value;
                };
                this.predicate = (context, value) -> {
                    return true;
                };
            } else {
                this.limiter = (context, value) -> {
                    return Math.min(max.getInt(context), value);
                };
                this.predicate = (context, value) -> {
                    return value <= max.getInt(context);
                };
            }
        } else if (max == null) {
            this.limiter = (context, value) -> {
                return Math.max(min.getInt(context), value);
            };
            this.predicate = (context, value) -> {
                return value >= min.getInt(context);
            };
        } else {
            this.limiter = (context, value) -> {
                return Mth.clamp(value, min.getInt(context), max.getInt(context));
            };
            this.predicate = (context, value) -> {
                return value >= min.getInt(context) && value <= max.getInt(context);
            };
        }

    }

    public static IntRange exact(int value) {
        ConstantValue constantValue = ConstantValue.exactly((float)value);
        return new IntRange(constantValue, constantValue);
    }

    public static IntRange range(int min, int max) {
        return new IntRange(ConstantValue.exactly((float)min), ConstantValue.exactly((float)max));
    }

    public static IntRange lowerBound(int min) {
        return new IntRange(ConstantValue.exactly((float)min), (NumberProvider)null);
    }

    public static IntRange upperBound(int max) {
        return new IntRange((NumberProvider)null, ConstantValue.exactly((float)max));
    }

    public int clamp(LootContext context, int value) {
        return this.limiter.apply(context, value);
    }

    public boolean test(LootContext context, int value) {
        return this.predicate.test(context, value);
    }

    @FunctionalInterface
    interface IntChecker {
        boolean test(LootContext context, int value);
    }

    @FunctionalInterface
    interface IntLimiter {
        int apply(LootContext context, int value);
    }

    public static class Serializer implements JsonDeserializer<IntRange>, JsonSerializer<IntRange> {
        @Override
        public IntRange deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            if (jsonElement.isJsonPrimitive()) {
                return IntRange.exact(jsonElement.getAsInt());
            } else {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "value");
                NumberProvider numberProvider = jsonObject.has("min") ? GsonHelper.getAsObject(jsonObject, "min", jsonDeserializationContext, NumberProvider.class) : null;
                NumberProvider numberProvider2 = jsonObject.has("max") ? GsonHelper.getAsObject(jsonObject, "max", jsonDeserializationContext, NumberProvider.class) : null;
                return new IntRange(numberProvider, numberProvider2);
            }
        }

        @Override
        public JsonElement serialize(IntRange intRange, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            if (Objects.equals(intRange.max, intRange.min)) {
                return jsonSerializationContext.serialize(intRange.min);
            } else {
                if (intRange.max != null) {
                    jsonObject.add("max", jsonSerializationContext.serialize(intRange.max));
                }

                if (intRange.min != null) {
                    jsonObject.add("min", jsonSerializationContext.serialize(intRange.min));
                }

                return jsonObject;
            }
        }
    }
}
