package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.logging.LogUtils;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.slf4j.Logger;

public class SetItemDamageFunction extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    final NumberProvider damage;
    final boolean add;

    SetItemDamageFunction(LootItemCondition[] conditions, NumberProvider durabilityRange, boolean add) {
        super(conditions);
        this.damage = durabilityRange;
        this.add = add;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_DAMAGE;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.damage.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        if (stack.isDamageableItem()) {
            int i = stack.getMaxDamage();
            float f = this.add ? 1.0F - (float)stack.getDamageValue() / (float)i : 0.0F;
            float g = 1.0F - Mth.clamp(this.damage.getFloat(context) + f, 0.0F, 1.0F);
            stack.setDamageValue(Mth.floor(g * (float)i));
        } else {
            LOGGER.warn("Couldn't set damage of loot item {}", (Object)stack);
        }

        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> setDamage(NumberProvider durabilityRange) {
        return simpleBuilder((conditions) -> {
            return new SetItemDamageFunction(conditions, durabilityRange, false);
        });
    }

    public static LootItemConditionalFunction.Builder<?> setDamage(NumberProvider durabilityRange, boolean add) {
        return simpleBuilder((conditions) -> {
            return new SetItemDamageFunction(conditions, durabilityRange, add);
        });
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetItemDamageFunction> {
        @Override
        public void serialize(JsonObject json, SetItemDamageFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("damage", context.serialize(object.damage));
            json.addProperty("add", object.add);
        }

        @Override
        public SetItemDamageFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            NumberProvider numberProvider = GsonHelper.getAsObject(jsonObject, "damage", jsonDeserializationContext, NumberProvider.class);
            boolean bl = GsonHelper.getAsBoolean(jsonObject, "add", false);
            return new SetItemDamageFunction(lootItemConditions, numberProvider, bl);
        }
    }
}
