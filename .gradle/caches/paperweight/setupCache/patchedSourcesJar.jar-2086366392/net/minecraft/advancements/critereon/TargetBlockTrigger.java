package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

public class TargetBlockTrigger extends SimpleCriterionTrigger<TargetBlockTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("target_hit");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public TargetBlockTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("signal_strength"));
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "projectile", deserializationContext);
        return new TargetBlockTrigger.TriggerInstance(composite, ints, composite2);
    }

    public void trigger(ServerPlayer player, Entity projectile, Vec3 hitPos, int signalStrength) {
        LootContext lootContext = EntityPredicate.createContext(player, projectile);
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext, hitPos, signalStrength);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final MinMaxBounds.Ints signalStrength;
        private final EntityPredicate.Composite projectile;

        public TriggerInstance(EntityPredicate.Composite player, MinMaxBounds.Ints signalStrength, EntityPredicate.Composite projectile) {
            super(TargetBlockTrigger.ID, player);
            this.signalStrength = signalStrength;
            this.projectile = projectile;
        }

        public static TargetBlockTrigger.TriggerInstance targetHit(MinMaxBounds.Ints signalStrength, EntityPredicate.Composite projectile) {
            return new TargetBlockTrigger.TriggerInstance(EntityPredicate.Composite.ANY, signalStrength, projectile);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("signal_strength", this.signalStrength.serializeToJson());
            jsonObject.add("projectile", this.projectile.toJson(predicateSerializer));
            return jsonObject;
        }

        public boolean matches(LootContext projectileContext, Vec3 hitPos, int signalStrength) {
            if (!this.signalStrength.matches(signalStrength)) {
                return false;
            } else {
                return this.projectile.matches(projectileContext);
            }
        }
    }
}
