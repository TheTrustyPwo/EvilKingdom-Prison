package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public final class BinomialDistributionGenerator implements NumberProvider {
    final NumberProvider n;
    final NumberProvider p;

    BinomialDistributionGenerator(NumberProvider n, NumberProvider p) {
        this.n = n;
        this.p = p;
    }

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.BINOMIAL;
    }

    @Override
    public int getInt(LootContext context) {
        int i = this.n.getInt(context);
        float f = this.p.getFloat(context);
        Random random = context.getRandom();
        int j = 0;

        for(int k = 0; k < i; ++k) {
            if (random.nextFloat() < f) {
                ++j;
            }
        }

        return j;
    }

    @Override
    public float getFloat(LootContext context) {
        return (float)this.getInt(context);
    }

    public static BinomialDistributionGenerator binomial(int n, float p) {
        return new BinomialDistributionGenerator(ConstantValue.exactly((float)n), ConstantValue.exactly(p));
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Sets.union(this.n.getReferencedContextParams(), this.p.getReferencedContextParams());
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<BinomialDistributionGenerator> {
        @Override
        public BinomialDistributionGenerator deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            NumberProvider numberProvider = GsonHelper.getAsObject(jsonObject, "n", jsonDeserializationContext, NumberProvider.class);
            NumberProvider numberProvider2 = GsonHelper.getAsObject(jsonObject, "p", jsonDeserializationContext, NumberProvider.class);
            return new BinomialDistributionGenerator(numberProvider, numberProvider2);
        }

        @Override
        public void serialize(JsonObject json, BinomialDistributionGenerator object, JsonSerializationContext context) {
            json.add("n", context.serialize(object.n));
            json.add("p", context.serialize(object.p));
        }
    }
}
