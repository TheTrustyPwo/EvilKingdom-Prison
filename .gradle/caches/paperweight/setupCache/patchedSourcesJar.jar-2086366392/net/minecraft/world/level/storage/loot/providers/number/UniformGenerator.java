package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public class UniformGenerator implements NumberProvider {
    final NumberProvider min;
    final NumberProvider max;

    UniformGenerator(NumberProvider min, NumberProvider max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.UNIFORM;
    }

    public static UniformGenerator between(float min, float max) {
        return new UniformGenerator(ConstantValue.exactly(min), ConstantValue.exactly(max));
    }

    @Override
    public int getInt(LootContext context) {
        return Mth.nextInt(context.getRandom(), this.min.getInt(context), this.max.getInt(context));
    }

    @Override
    public float getFloat(LootContext context) {
        return Mth.nextFloat(context.getRandom(), this.min.getFloat(context), this.max.getFloat(context));
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Sets.union(this.min.getReferencedContextParams(), this.max.getReferencedContextParams());
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<UniformGenerator> {
        @Override
        public UniformGenerator deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            NumberProvider numberProvider = GsonHelper.getAsObject(jsonObject, "min", jsonDeserializationContext, NumberProvider.class);
            NumberProvider numberProvider2 = GsonHelper.getAsObject(jsonObject, "max", jsonDeserializationContext, NumberProvider.class);
            return new UniformGenerator(numberProvider, numberProvider2);
        }

        @Override
        public void serialize(JsonObject json, UniformGenerator object, JsonSerializationContext context) {
            json.add("min", context.serialize(object.min));
            json.add("max", context.serialize(object.max));
        }
    }
}
