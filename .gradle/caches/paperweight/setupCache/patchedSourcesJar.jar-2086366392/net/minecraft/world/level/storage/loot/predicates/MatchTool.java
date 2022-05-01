package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class MatchTool implements LootItemCondition {
    final ItemPredicate predicate;

    public MatchTool(ItemPredicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.MATCH_TOOL;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.TOOL);
    }

    @Override
    public boolean test(LootContext lootContext) {
        ItemStack itemStack = lootContext.getParamOrNull(LootContextParams.TOOL);
        return itemStack != null && this.predicate.matches(itemStack);
    }

    public static LootItemCondition.Builder toolMatches(ItemPredicate.Builder predicate) {
        return () -> {
            return new MatchTool(predicate.build());
        };
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<MatchTool> {
        @Override
        public void serialize(JsonObject json, MatchTool object, JsonSerializationContext context) {
            json.add("predicate", object.predicate.serializeToJson());
        }

        @Override
        public MatchTool deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("predicate"));
            return new MatchTool(itemPredicate);
        }
    }
}
