package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetPotionFunction extends LootItemConditionalFunction {
    final Potion potion;

    SetPotionFunction(LootItemCondition[] conditions, Potion potion) {
        super(conditions);
        this.potion = potion;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_POTION;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        PotionUtils.setPotion(stack, this.potion);
        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> setPotion(Potion potion) {
        return simpleBuilder((conditions) -> {
            return new SetPotionFunction(conditions, potion);
        });
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetPotionFunction> {
        @Override
        public void serialize(JsonObject json, SetPotionFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("id", Registry.POTION.getKey(object.potion).toString());
        }

        @Override
        public SetPotionFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            String string = GsonHelper.getAsString(jsonObject, "id");
            Potion potion = Registry.POTION.getOptional(ResourceLocation.tryParse(string)).orElseThrow(() -> {
                return new JsonSyntaxException("Unknown potion '" + string + "'");
            });
            return new SetPotionFunction(lootItemConditions, potion);
        }
    }
}
