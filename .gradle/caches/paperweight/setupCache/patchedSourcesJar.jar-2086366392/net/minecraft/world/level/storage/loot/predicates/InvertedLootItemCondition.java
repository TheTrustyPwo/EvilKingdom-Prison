package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public class InvertedLootItemCondition implements LootItemCondition {
    final LootItemCondition term;

    InvertedLootItemCondition(LootItemCondition term) {
        this.term = term;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.INVERTED;
    }

    @Override
    public final boolean test(LootContext lootContext) {
        return !this.term.test(lootContext);
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.term.getReferencedContextParams();
    }

    @Override
    public void validate(ValidationContext reporter) {
        LootItemCondition.super.validate(reporter);
        this.term.validate(reporter);
    }

    public static LootItemCondition.Builder invert(LootItemCondition.Builder term) {
        InvertedLootItemCondition invertedLootItemCondition = new InvertedLootItemCondition(term.build());
        return () -> {
            return invertedLootItemCondition;
        };
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<InvertedLootItemCondition> {
        @Override
        public void serialize(JsonObject json, InvertedLootItemCondition object, JsonSerializationContext context) {
            json.add("term", context.serialize(object.term));
        }

        @Override
        public InvertedLootItemCondition deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LootItemCondition lootItemCondition = GsonHelper.getAsObject(jsonObject, "term", jsonDeserializationContext, LootItemCondition.class);
            return new InvertedLootItemCondition(lootItemCondition);
        }
    }
}
