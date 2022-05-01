package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class DistanceTrigger extends SimpleCriterionTrigger<DistanceTrigger.TriggerInstance> {
    final ResourceLocation id;

    public DistanceTrigger(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public DistanceTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        LocationPredicate locationPredicate = LocationPredicate.fromJson(jsonObject.get("start_position"));
        DistancePredicate distancePredicate = DistancePredicate.fromJson(jsonObject.get("distance"));
        return new DistanceTrigger.TriggerInstance(this.id, composite, locationPredicate, distancePredicate);
    }

    public void trigger(ServerPlayer player, Vec3 startPos) {
        Vec3 vec3 = player.position();
        this.trigger(player, (conditions) -> {
            return conditions.matches(player.getLevel(), startPos, vec3);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final LocationPredicate startPosition;
        private final DistancePredicate distance;

        public TriggerInstance(ResourceLocation id, EntityPredicate.Composite entity, LocationPredicate startPos, DistancePredicate distance) {
            super(id, entity);
            this.startPosition = startPos;
            this.distance = distance;
        }

        public static DistanceTrigger.TriggerInstance fallFromHeight(EntityPredicate.Builder entity, DistancePredicate distance, LocationPredicate startPos) {
            return new DistanceTrigger.TriggerInstance(CriteriaTriggers.FALL_FROM_HEIGHT.id, EntityPredicate.Composite.wrap(entity.build()), startPos, distance);
        }

        public static DistanceTrigger.TriggerInstance rideEntityInLava(EntityPredicate.Builder entity, DistancePredicate distance) {
            return new DistanceTrigger.TriggerInstance(CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.id, EntityPredicate.Composite.wrap(entity.build()), LocationPredicate.ANY, distance);
        }

        public static DistanceTrigger.TriggerInstance travelledThroughNether(DistancePredicate distance) {
            return new DistanceTrigger.TriggerInstance(CriteriaTriggers.NETHER_TRAVEL.id, EntityPredicate.Composite.ANY, LocationPredicate.ANY, distance);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("start_position", this.startPosition.serializeToJson());
            jsonObject.add("distance", this.distance.serializeToJson());
            return jsonObject;
        }

        public boolean matches(ServerLevel world, Vec3 startPos, Vec3 endPos) {
            if (!this.startPosition.matches(world, startPos.x, startPos.y, startPos.z)) {
                return false;
            } else {
                return this.distance.matches(startPos.x, startPos.y, startPos.z, endPos.x, endPos.y, endPos.z);
            }
        }
    }
}
