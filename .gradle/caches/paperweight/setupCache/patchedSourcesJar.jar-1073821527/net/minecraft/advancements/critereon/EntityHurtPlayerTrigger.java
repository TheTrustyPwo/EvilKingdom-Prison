package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public class EntityHurtPlayerTrigger extends SimpleCriterionTrigger<EntityHurtPlayerTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("entity_hurt_player");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public EntityHurtPlayerTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        DamagePredicate damagePredicate = DamagePredicate.fromJson(jsonObject.get("damage"));
        return new EntityHurtPlayerTrigger.TriggerInstance(composite, damagePredicate);
    }

    public void trigger(ServerPlayer player, DamageSource source, float dealt, float taken, boolean blocked) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, source, dealt, taken, blocked);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final DamagePredicate damage;

        public TriggerInstance(EntityPredicate.Composite player, DamagePredicate damage) {
            super(EntityHurtPlayerTrigger.ID, player);
            this.damage = damage;
        }

        public static EntityHurtPlayerTrigger.TriggerInstance entityHurtPlayer() {
            return new EntityHurtPlayerTrigger.TriggerInstance(EntityPredicate.Composite.ANY, DamagePredicate.ANY);
        }

        public static EntityHurtPlayerTrigger.TriggerInstance entityHurtPlayer(DamagePredicate predicate) {
            return new EntityHurtPlayerTrigger.TriggerInstance(EntityPredicate.Composite.ANY, predicate);
        }

        public static EntityHurtPlayerTrigger.TriggerInstance entityHurtPlayer(DamagePredicate.Builder damageBuilder) {
            return new EntityHurtPlayerTrigger.TriggerInstance(EntityPredicate.Composite.ANY, damageBuilder.build());
        }

        public boolean matches(ServerPlayer player, DamageSource source, float dealt, float taken, boolean blocked) {
            return this.damage.matches(player, source, dealt, taken, blocked);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("damage", this.damage.serializeToJson());
            return jsonObject;
        }
    }
}
