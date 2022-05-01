package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerHurtEntityTrigger extends SimpleCriterionTrigger<PlayerHurtEntityTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("player_hurt_entity");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public PlayerHurtEntityTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        DamagePredicate damagePredicate = DamagePredicate.fromJson(jsonObject.get("damage"));
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "entity", deserializationContext);
        return new PlayerHurtEntityTrigger.TriggerInstance(composite, damagePredicate, composite2);
    }

    public void trigger(ServerPlayer player, Entity entity, DamageSource damage, float dealt, float taken, boolean blocked) {
        LootContext lootContext = EntityPredicate.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, lootContext, damage, dealt, taken, blocked);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final DamagePredicate damage;
        private final EntityPredicate.Composite entity;

        public TriggerInstance(EntityPredicate.Composite player, DamagePredicate damage, EntityPredicate.Composite entity) {
            super(PlayerHurtEntityTrigger.ID, player);
            this.damage = damage;
            this.entity = entity;
        }

        public static PlayerHurtEntityTrigger.TriggerInstance playerHurtEntity() {
            return new PlayerHurtEntityTrigger.TriggerInstance(EntityPredicate.Composite.ANY, DamagePredicate.ANY, EntityPredicate.Composite.ANY);
        }

        public static PlayerHurtEntityTrigger.TriggerInstance playerHurtEntity(DamagePredicate damagePredicate) {
            return new PlayerHurtEntityTrigger.TriggerInstance(EntityPredicate.Composite.ANY, damagePredicate, EntityPredicate.Composite.ANY);
        }

        public static PlayerHurtEntityTrigger.TriggerInstance playerHurtEntity(DamagePredicate.Builder damagePredicateBuilder) {
            return new PlayerHurtEntityTrigger.TriggerInstance(EntityPredicate.Composite.ANY, damagePredicateBuilder.build(), EntityPredicate.Composite.ANY);
        }

        public static PlayerHurtEntityTrigger.TriggerInstance playerHurtEntity(EntityPredicate hurtEntityPredicate) {
            return new PlayerHurtEntityTrigger.TriggerInstance(EntityPredicate.Composite.ANY, DamagePredicate.ANY, EntityPredicate.Composite.wrap(hurtEntityPredicate));
        }

        public static PlayerHurtEntityTrigger.TriggerInstance playerHurtEntity(DamagePredicate damagePredicate, EntityPredicate hurtEntityPredicate) {
            return new PlayerHurtEntityTrigger.TriggerInstance(EntityPredicate.Composite.ANY, damagePredicate, EntityPredicate.Composite.wrap(hurtEntityPredicate));
        }

        public static PlayerHurtEntityTrigger.TriggerInstance playerHurtEntity(DamagePredicate.Builder damagePredicateBuilder, EntityPredicate hurtEntityPredicate) {
            return new PlayerHurtEntityTrigger.TriggerInstance(EntityPredicate.Composite.ANY, damagePredicateBuilder.build(), EntityPredicate.Composite.wrap(hurtEntityPredicate));
        }

        public boolean matches(ServerPlayer player, LootContext entityContext, DamageSource source, float dealt, float taken, boolean blocked) {
            if (!this.damage.matches(player, source, dealt, taken, blocked)) {
                return false;
            } else {
                return this.entity.matches(entityContext);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("damage", this.damage.serializeToJson());
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
