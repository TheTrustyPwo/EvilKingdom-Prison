package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class EnchantWithLevelsFunction extends LootItemConditionalFunction {
    final NumberProvider levels;
    final boolean treasure;

    EnchantWithLevelsFunction(LootItemCondition[] conditions, NumberProvider range, boolean treasureEnchantmentsAllowed) {
        super(conditions);
        this.levels = range;
        this.treasure = treasureEnchantmentsAllowed;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.ENCHANT_WITH_LEVELS;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.levels.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        Random random = context.getRandom();
        return EnchantmentHelper.enchantItem(random, stack, this.levels.getInt(context), this.treasure);
    }

    public static EnchantWithLevelsFunction.Builder enchantWithLevels(NumberProvider range) {
        return new EnchantWithLevelsFunction.Builder(range);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<EnchantWithLevelsFunction.Builder> {
        private final NumberProvider levels;
        private boolean treasure;

        public Builder(NumberProvider range) {
            this.levels = range;
        }

        @Override
        protected EnchantWithLevelsFunction.Builder getThis() {
            return this;
        }

        public EnchantWithLevelsFunction.Builder allowTreasure() {
            this.treasure = true;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new EnchantWithLevelsFunction(this.getConditions(), this.levels, this.treasure);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<EnchantWithLevelsFunction> {
        @Override
        public void serialize(JsonObject json, EnchantWithLevelsFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("levels", context.serialize(object.levels));
            json.addProperty("treasure", object.treasure);
        }

        @Override
        public EnchantWithLevelsFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            NumberProvider numberProvider = GsonHelper.getAsObject(jsonObject, "levels", jsonDeserializationContext, NumberProvider.class);
            boolean bl = GsonHelper.getAsBoolean(jsonObject, "treasure", false);
            return new EnchantWithLevelsFunction(lootItemConditions, numberProvider, bl);
        }
    }
}
