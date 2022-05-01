package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class SetItemCountFunction extends LootItemConditionalFunction {
    final NumberProvider value;
    final boolean add;

    SetItemCountFunction(LootItemCondition[] conditions, NumberProvider countRange, boolean add) {
        super(conditions);
        this.value = countRange;
        this.add = add;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_COUNT;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.value.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        int i = this.add ? stack.getCount() : 0;
        stack.setCount(Mth.clamp(i + this.value.getInt(context), 0, stack.getMaxStackSize()));
        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> setCount(NumberProvider countRange) {
        return simpleBuilder((conditions) -> {
            return new SetItemCountFunction(conditions, countRange, false);
        });
    }

    public static LootItemConditionalFunction.Builder<?> setCount(NumberProvider countRange, boolean add) {
        return simpleBuilder((conditions) -> {
            return new SetItemCountFunction(conditions, countRange, add);
        });
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetItemCountFunction> {
        @Override
        public void serialize(JsonObject json, SetItemCountFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("count", context.serialize(object.value));
            json.addProperty("add", object.add);
        }

        @Override
        public SetItemCountFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            NumberProvider numberProvider = GsonHelper.getAsObject(jsonObject, "count", jsonDeserializationContext, NumberProvider.class);
            boolean bl = GsonHelper.getAsBoolean(jsonObject, "add", false);
            return new SetItemCountFunction(lootItemConditions, numberProvider, bl);
        }
    }
}
