package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class LootItemRandomChanceWithLootingCondition implements LootItemCondition {

    final float percent;
    final float lootingMultiplier;

    LootItemRandomChanceWithLootingCondition(float chance, float lootingMultiplier) {
        this.percent = chance;
        this.lootingMultiplier = lootingMultiplier;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.RANDOM_CHANCE_WITH_LOOTING;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.KILLER_ENTITY);
    }

    public boolean test(LootContext loottableinfo) {
        Entity entity = (Entity) loottableinfo.getParamOrNull(LootContextParams.KILLER_ENTITY);
        int i = 0;

        if (entity instanceof LivingEntity) {
            i = EnchantmentHelper.getMobLooting((LivingEntity) entity);
        }
        // CraftBukkit start - only use lootingModifier if set by Bukkit
        if (loottableinfo.hasParam(LootContextParams.LOOTING_MOD)) {
            i = loottableinfo.getParamOrNull(LootContextParams.LOOTING_MOD);
        }
        // CraftBukkit end

        return loottableinfo.getRandom().nextFloat() < this.percent + (float) i * this.lootingMultiplier;
    }

    public static LootItemCondition.Builder randomChanceAndLootingBoost(float chance, float lootingMultiplier) {
        return () -> {
            return new LootItemRandomChanceWithLootingCondition(chance, lootingMultiplier);
        };
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<LootItemRandomChanceWithLootingCondition> {

        public Serializer() {}

        public void serialize(JsonObject json, LootItemRandomChanceWithLootingCondition object, JsonSerializationContext context) {
            json.addProperty("chance", object.percent);
            json.addProperty("looting_multiplier", object.lootingMultiplier);
        }

        @Override
        public LootItemRandomChanceWithLootingCondition deserialize(JsonObject json, JsonDeserializationContext context) {
            return new LootItemRandomChanceWithLootingCondition(GsonHelper.getAsFloat(json, "chance"), GsonHelper.getAsFloat(json, "looting_multiplier"));
        }
    }
}
