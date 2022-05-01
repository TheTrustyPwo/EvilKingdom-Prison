package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class LootingEnchantFunction extends LootItemConditionalFunction {

    public static final int NO_LIMIT = 0;
    final NumberProvider value;
    final int limit;

    LootingEnchantFunction(LootItemCondition[] conditions, NumberProvider countRange, int limit) {
        super(conditions);
        this.value = countRange;
        this.limit = limit;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.LOOTING_ENCHANT;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Sets.union(ImmutableSet.of(LootContextParams.KILLER_ENTITY), this.value.getReferencedContextParams());
    }

    boolean hasLimit() {
        return this.limit > 0;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        Entity entity = (Entity) context.getParamOrNull(LootContextParams.KILLER_ENTITY);

        if (entity instanceof LivingEntity) {
            int i = EnchantmentHelper.getMobLooting((LivingEntity) entity);
            // CraftBukkit start - use lootingModifier if set by plugin
            if (context.hasParam(LootContextParams.LOOTING_MOD)) {
                i = context.getParamOrNull(LootContextParams.LOOTING_MOD);
            }
            // CraftBukkit end

            if (i <= 0) { // CraftBukkit - account for possible negative looting values from Bukkit
                return stack;
            }

            float f = (float) i * this.value.getFloat(context);

            stack.grow(Math.round(f));
            if (this.hasLimit() && stack.getCount() > this.limit) {
                stack.setCount(this.limit);
            }
        }

        return stack;
    }

    public static LootingEnchantFunction.Builder lootingMultiplier(NumberProvider countRange) {
        return new LootingEnchantFunction.Builder(countRange);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<LootingEnchantFunction.Builder> {

        private final NumberProvider count;
        private int limit = 0;

        public Builder(NumberProvider countRange) {
            this.count = countRange;
        }

        @Override
        protected LootingEnchantFunction.Builder getThis() {
            return this;
        }

        public LootingEnchantFunction.Builder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootingEnchantFunction(this.getConditions(), this.count, this.limit);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<LootingEnchantFunction> {

        public Serializer() {}

        public void serialize(JsonObject json, LootingEnchantFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context); // CraftBukkit - decompile error
            json.add("count", context.serialize(object.value));
            if (object.hasLimit()) {
                json.add("limit", context.serialize(object.limit));
            }

        }

        @Override
        public LootingEnchantFunction deserialize(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions) {
            int i = GsonHelper.getAsInt(json, "limit", 0);

            return new LootingEnchantFunction(conditions, (NumberProvider) GsonHelper.getAsObject(json, "count", context, NumberProvider.class), i);
        }
    }
}
