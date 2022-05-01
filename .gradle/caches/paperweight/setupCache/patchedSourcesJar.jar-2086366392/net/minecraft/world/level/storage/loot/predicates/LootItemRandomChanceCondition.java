package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;

public class LootItemRandomChanceCondition implements LootItemCondition {
    final float probability;

    LootItemRandomChanceCondition(float chance) {
        this.probability = chance;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.RANDOM_CHANCE;
    }

    @Override
    public boolean test(LootContext lootContext) {
        return lootContext.getRandom().nextFloat() < this.probability;
    }

    public static LootItemCondition.Builder randomChance(float chance) {
        return () -> {
            return new LootItemRandomChanceCondition(chance);
        };
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<LootItemRandomChanceCondition> {
        @Override
        public void serialize(JsonObject json, LootItemRandomChanceCondition object, JsonSerializationContext context) {
            json.addProperty("chance", object.probability);
        }

        @Override
        public LootItemRandomChanceCondition deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            return new LootItemRandomChanceCondition(GsonHelper.getAsFloat(jsonObject, "chance"));
        }
    }
}
