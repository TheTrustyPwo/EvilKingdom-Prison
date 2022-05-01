package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class SetStewEffectFunction extends LootItemConditionalFunction {
    final Map<MobEffect, NumberProvider> effectDurationMap;

    SetStewEffectFunction(LootItemCondition[] conditions, Map<MobEffect, NumberProvider> effects) {
        super(conditions);
        this.effectDurationMap = ImmutableMap.copyOf(effects);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_STEW_EFFECT;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.effectDurationMap.values().stream().flatMap((numberProvider) -> {
            return numberProvider.getReferencedContextParams().stream();
        }).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        if (stack.is(Items.SUSPICIOUS_STEW) && !this.effectDurationMap.isEmpty()) {
            Random random = context.getRandom();
            int i = random.nextInt(this.effectDurationMap.size());
            Entry<MobEffect, NumberProvider> entry = Iterables.get(this.effectDurationMap.entrySet(), i);
            MobEffect mobEffect = entry.getKey();
            int j = entry.getValue().getInt(context);
            if (!mobEffect.isInstantenous()) {
                j *= 20;
            }

            SuspiciousStewItem.saveMobEffect(stack, mobEffect, j);
            return stack;
        } else {
            return stack;
        }
    }

    public static SetStewEffectFunction.Builder stewEffect() {
        return new SetStewEffectFunction.Builder();
    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetStewEffectFunction.Builder> {
        private final Map<MobEffect, NumberProvider> effectDurationMap = Maps.newHashMap();

        @Override
        protected SetStewEffectFunction.Builder getThis() {
            return this;
        }

        public SetStewEffectFunction.Builder withEffect(MobEffect effect, NumberProvider durationRange) {
            this.effectDurationMap.put(effect, durationRange);
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetStewEffectFunction(this.getConditions(), this.effectDurationMap);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetStewEffectFunction> {
        @Override
        public void serialize(JsonObject json, SetStewEffectFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            if (!object.effectDurationMap.isEmpty()) {
                JsonArray jsonArray = new JsonArray();

                for(MobEffect mobEffect : object.effectDurationMap.keySet()) {
                    JsonObject jsonObject = new JsonObject();
                    ResourceLocation resourceLocation = Registry.MOB_EFFECT.getKey(mobEffect);
                    if (resourceLocation == null) {
                        throw new IllegalArgumentException("Don't know how to serialize mob effect " + mobEffect);
                    }

                    jsonObject.add("type", new JsonPrimitive(resourceLocation.toString()));
                    jsonObject.add("duration", context.serialize(object.effectDurationMap.get(mobEffect)));
                    jsonArray.add(jsonObject);
                }

                json.add("effects", jsonArray);
            }

        }

        @Override
        public SetStewEffectFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            Map<MobEffect, NumberProvider> map = Maps.newHashMap();
            if (jsonObject.has("effects")) {
                for(JsonElement jsonElement : GsonHelper.getAsJsonArray(jsonObject, "effects")) {
                    String string = GsonHelper.getAsString(jsonElement.getAsJsonObject(), "type");
                    MobEffect mobEffect = Registry.MOB_EFFECT.getOptional(new ResourceLocation(string)).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown mob effect '" + string + "'");
                    });
                    NumberProvider numberProvider = GsonHelper.getAsObject(jsonElement.getAsJsonObject(), "duration", jsonDeserializationContext, NumberProvider.class);
                    map.put(mobEffect, numberProvider);
                }
            }

            return new SetStewEffectFunction(lootItemConditions, map);
        }
    }
}
