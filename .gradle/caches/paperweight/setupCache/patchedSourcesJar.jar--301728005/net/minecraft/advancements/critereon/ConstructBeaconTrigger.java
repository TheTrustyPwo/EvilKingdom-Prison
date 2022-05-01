package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ConstructBeaconTrigger extends SimpleCriterionTrigger<ConstructBeaconTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("construct_beacon");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public ConstructBeaconTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("level"));
        return new ConstructBeaconTrigger.TriggerInstance(composite, ints);
    }

    public void trigger(ServerPlayer player, int level) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(level);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final MinMaxBounds.Ints level;

        public TriggerInstance(EntityPredicate.Composite player, MinMaxBounds.Ints level) {
            super(ConstructBeaconTrigger.ID, player);
            this.level = level;
        }

        public static ConstructBeaconTrigger.TriggerInstance constructedBeacon() {
            return new ConstructBeaconTrigger.TriggerInstance(EntityPredicate.Composite.ANY, MinMaxBounds.Ints.ANY);
        }

        public static ConstructBeaconTrigger.TriggerInstance constructedBeacon(MinMaxBounds.Ints level) {
            return new ConstructBeaconTrigger.TriggerInstance(EntityPredicate.Composite.ANY, level);
        }

        public boolean matches(int level) {
            return this.level.matches(level);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("level", this.level.serializeToJson());
            return jsonObject;
        }
    }
}
