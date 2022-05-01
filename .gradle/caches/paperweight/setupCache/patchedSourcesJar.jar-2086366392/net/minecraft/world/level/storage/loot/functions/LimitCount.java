package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LimitCount extends LootItemConditionalFunction {
    final IntRange limiter;

    LimitCount(LootItemCondition[] conditions, IntRange limit) {
        super(conditions);
        this.limiter = limit;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.LIMIT_COUNT;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.limiter.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        int i = this.limiter.clamp(context, stack.getCount());
        stack.setCount(i);
        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> limitCount(IntRange limit) {
        return simpleBuilder((conditions) -> {
            return new LimitCount(conditions, limit);
        });
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<LimitCount> {
        @Override
        public void serialize(JsonObject json, LimitCount object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("limit", context.serialize(object.limiter));
        }

        @Override
        public LimitCount deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            IntRange intRange = GsonHelper.getAsObject(jsonObject, "limit", jsonDeserializationContext, IntRange.class);
            return new LimitCount(lootItemConditions, intRange);
        }
    }
}
