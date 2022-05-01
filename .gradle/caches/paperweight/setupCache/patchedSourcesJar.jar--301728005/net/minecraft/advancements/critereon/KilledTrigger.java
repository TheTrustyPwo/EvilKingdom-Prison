package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class KilledTrigger extends SimpleCriterionTrigger<KilledTrigger.TriggerInstance> {
    final ResourceLocation id;

    public KilledTrigger(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public KilledTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        return new KilledTrigger.TriggerInstance(this.id, composite, EntityPredicate.Composite.fromJson(jsonObject, "entity", deserializationContext), DamageSourcePredicate.fromJson(jsonObject.get("killing_blow")));
    }

    public void trigger(ServerPlayer player, Entity entity, DamageSource killingDamage) {
        LootContext lootContext = EntityPredicate.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, lootContext, killingDamage);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final EntityPredicate.Composite entityPredicate;
        private final DamageSourcePredicate killingBlow;

        public TriggerInstance(ResourceLocation id, EntityPredicate.Composite player, EntityPredicate.Composite entity, DamageSourcePredicate killingBlow) {
            super(id, player);
            this.entityPredicate = entity;
            this.killingBlow = killingBlow;
        }

        public static KilledTrigger.TriggerInstance playerKilledEntity(EntityPredicate killedEntityPredicate) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.PLAYER_KILLED_ENTITY.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killedEntityPredicate), DamageSourcePredicate.ANY);
        }

        public static KilledTrigger.TriggerInstance playerKilledEntity(EntityPredicate.Builder killedEntityPredicateBuilder) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.PLAYER_KILLED_ENTITY.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killedEntityPredicateBuilder.build()), DamageSourcePredicate.ANY);
        }

        public static KilledTrigger.TriggerInstance playerKilledEntity() {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.PLAYER_KILLED_ENTITY.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, DamageSourcePredicate.ANY);
        }

        public static KilledTrigger.TriggerInstance playerKilledEntity(EntityPredicate killedEntityPredicate, DamageSourcePredicate damageSourcePredicate) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.PLAYER_KILLED_ENTITY.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killedEntityPredicate), damageSourcePredicate);
        }

        public static KilledTrigger.TriggerInstance playerKilledEntity(EntityPredicate.Builder killedEntityPredicateBuilder, DamageSourcePredicate damageSourcePredicate) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.PLAYER_KILLED_ENTITY.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killedEntityPredicateBuilder.build()), damageSourcePredicate);
        }

        public static KilledTrigger.TriggerInstance playerKilledEntity(EntityPredicate killedEntityPredicate, DamageSourcePredicate.Builder damageSourcePredicateBuilder) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.PLAYER_KILLED_ENTITY.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killedEntityPredicate), damageSourcePredicateBuilder.build());
        }

        public static KilledTrigger.TriggerInstance playerKilledEntity(EntityPredicate.Builder killedEntityPredicateBuilder, DamageSourcePredicate.Builder killingBlowBuilder) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.PLAYER_KILLED_ENTITY.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killedEntityPredicateBuilder.build()), killingBlowBuilder.build());
        }

        public static KilledTrigger.TriggerInstance entityKilledPlayer(EntityPredicate killerEntityPredicate) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.ENTITY_KILLED_PLAYER.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killerEntityPredicate), DamageSourcePredicate.ANY);
        }

        public static KilledTrigger.TriggerInstance entityKilledPlayer(EntityPredicate.Builder killerEntityPredicateBuilder) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.ENTITY_KILLED_PLAYER.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killerEntityPredicateBuilder.build()), DamageSourcePredicate.ANY);
        }

        public static KilledTrigger.TriggerInstance entityKilledPlayer() {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.ENTITY_KILLED_PLAYER.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, DamageSourcePredicate.ANY);
        }

        public static KilledTrigger.TriggerInstance entityKilledPlayer(EntityPredicate killerEntityPredicate, DamageSourcePredicate damageSourcePredicate) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.ENTITY_KILLED_PLAYER.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killerEntityPredicate), damageSourcePredicate);
        }

        public static KilledTrigger.TriggerInstance entityKilledPlayer(EntityPredicate.Builder killerEntityPredicateBuilder, DamageSourcePredicate damageSourcePredicate) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.ENTITY_KILLED_PLAYER.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killerEntityPredicateBuilder.build()), damageSourcePredicate);
        }

        public static KilledTrigger.TriggerInstance entityKilledPlayer(EntityPredicate killerEntityPredicate, DamageSourcePredicate.Builder damageSourcePredicateBuilder) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.ENTITY_KILLED_PLAYER.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killerEntityPredicate), damageSourcePredicateBuilder.build());
        }

        public static KilledTrigger.TriggerInstance entityKilledPlayer(EntityPredicate.Builder killerEntityPredicateBuilder, DamageSourcePredicate.Builder damageSourcePredicateBuilder) {
            return new KilledTrigger.TriggerInstance(CriteriaTriggers.ENTITY_KILLED_PLAYER.id, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(killerEntityPredicateBuilder.build()), damageSourcePredicateBuilder.build());
        }

        public boolean matches(ServerPlayer player, LootContext killedEntityContext, DamageSource killingBlow) {
            return !this.killingBlow.matches(player, killingBlow) ? false : this.entityPredicate.matches(killedEntityContext);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("entity", this.entityPredicate.toJson(predicateSerializer));
            jsonObject.add("killing_blow", this.killingBlow.serializeToJson());
            return jsonObject;
        }
    }
}
