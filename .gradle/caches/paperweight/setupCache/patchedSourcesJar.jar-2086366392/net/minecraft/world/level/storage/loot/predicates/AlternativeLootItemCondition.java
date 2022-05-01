package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public class AlternativeLootItemCondition implements LootItemCondition {
    final LootItemCondition[] terms;
    private final Predicate<LootContext> composedPredicate;

    AlternativeLootItemCondition(LootItemCondition[] terms) {
        this.terms = terms;
        this.composedPredicate = LootItemConditions.orConditions(terms);
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ALTERNATIVE;
    }

    @Override
    public final boolean test(LootContext lootContext) {
        return this.composedPredicate.test(lootContext);
    }

    @Override
    public void validate(ValidationContext reporter) {
        LootItemCondition.super.validate(reporter);

        for(int i = 0; i < this.terms.length; ++i) {
            this.terms[i].validate(reporter.forChild(".term[" + i + "]"));
        }

    }

    public static AlternativeLootItemCondition.Builder alternative(LootItemCondition.Builder... terms) {
        return new AlternativeLootItemCondition.Builder(terms);
    }

    public static class Builder implements LootItemCondition.Builder {
        private final List<LootItemCondition> terms = Lists.newArrayList();

        public Builder(LootItemCondition.Builder... terms) {
            for(LootItemCondition.Builder builder : terms) {
                this.terms.add(builder.build());
            }

        }

        @Override
        public AlternativeLootItemCondition.Builder or(LootItemCondition.Builder condition) {
            this.terms.add(condition.build());
            return this;
        }

        @Override
        public LootItemCondition build() {
            return new AlternativeLootItemCondition(this.terms.toArray(new LootItemCondition[0]));
        }
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<AlternativeLootItemCondition> {
        @Override
        public void serialize(JsonObject json, AlternativeLootItemCondition object, JsonSerializationContext context) {
            json.add("terms", context.serialize(object.terms));
        }

        @Override
        public AlternativeLootItemCondition deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LootItemCondition[] lootItemConditions = GsonHelper.getAsObject(jsonObject, "terms", jsonDeserializationContext, LootItemCondition[].class);
            return new AlternativeLootItemCondition(lootItemConditions);
        }
    }
}
