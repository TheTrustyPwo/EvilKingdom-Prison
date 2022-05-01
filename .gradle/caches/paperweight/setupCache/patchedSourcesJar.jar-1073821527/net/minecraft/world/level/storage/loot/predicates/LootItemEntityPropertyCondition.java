package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class LootItemEntityPropertyCondition implements LootItemCondition {
    final EntityPredicate predicate;
    final LootContext.EntityTarget entityTarget;

    LootItemEntityPropertyCondition(EntityPredicate predicate, LootContext.EntityTarget entity) {
        this.predicate = predicate;
        this.entityTarget = entity;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ENTITY_PROPERTIES;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.ORIGIN, this.entityTarget.getParam());
    }

    @Override
    public boolean test(LootContext lootContext) {
        Entity entity = lootContext.getParamOrNull(this.entityTarget.getParam());
        Vec3 vec3 = lootContext.getParamOrNull(LootContextParams.ORIGIN);
        return this.predicate.matches(lootContext.getLevel(), vec3, entity);
    }

    public static LootItemCondition.Builder entityPresent(LootContext.EntityTarget entity) {
        return hasProperties(entity, EntityPredicate.Builder.entity());
    }

    public static LootItemCondition.Builder hasProperties(LootContext.EntityTarget entity, EntityPredicate.Builder predicateBuilder) {
        return () -> {
            return new LootItemEntityPropertyCondition(predicateBuilder.build(), entity);
        };
    }

    public static LootItemCondition.Builder hasProperties(LootContext.EntityTarget entity, EntityPredicate predicate) {
        return () -> {
            return new LootItemEntityPropertyCondition(predicate, entity);
        };
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<LootItemEntityPropertyCondition> {
        @Override
        public void serialize(JsonObject json, LootItemEntityPropertyCondition object, JsonSerializationContext context) {
            json.add("predicate", object.predicate.serializeToJson());
            json.add("entity", context.serialize(object.entityTarget));
        }

        @Override
        public LootItemEntityPropertyCondition deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            EntityPredicate entityPredicate = EntityPredicate.fromJson(jsonObject.get("predicate"));
            return new LootItemEntityPropertyCondition(entityPredicate, GsonHelper.getAsObject(jsonObject, "entity", jsonDeserializationContext, LootContext.EntityTarget.class));
        }
    }
}
