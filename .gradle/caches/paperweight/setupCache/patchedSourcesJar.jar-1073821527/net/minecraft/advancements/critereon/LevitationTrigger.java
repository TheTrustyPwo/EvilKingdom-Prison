package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LevitationTrigger extends SimpleCriterionTrigger<LevitationTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("levitation");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public LevitationTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        DistancePredicate distancePredicate = DistancePredicate.fromJson(jsonObject.get("distance"));
        MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("duration"));
        return new LevitationTrigger.TriggerInstance(composite, distancePredicate, ints);
    }

    public void trigger(ServerPlayer player, Vec3 startPos, int duration) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, startPos, duration);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final DistancePredicate distance;
        private final MinMaxBounds.Ints duration;

        public TriggerInstance(EntityPredicate.Composite player, DistancePredicate distance, MinMaxBounds.Ints duration) {
            super(LevitationTrigger.ID, player);
            this.distance = distance;
            this.duration = duration;
        }

        public static LevitationTrigger.TriggerInstance levitated(DistancePredicate distance) {
            return new LevitationTrigger.TriggerInstance(EntityPredicate.Composite.ANY, distance, MinMaxBounds.Ints.ANY);
        }

        public boolean matches(ServerPlayer player, Vec3 startPos, int duration) {
            if (!this.distance.matches(startPos.x, startPos.y, startPos.z, player.getX(), player.getY(), player.getZ())) {
                return false;
            } else {
                return this.duration.matches(duration);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("distance", this.distance.serializeToJson());
            jsonObject.add("duration", this.duration.serializeToJson());
            return jsonObject;
        }
    }
}
