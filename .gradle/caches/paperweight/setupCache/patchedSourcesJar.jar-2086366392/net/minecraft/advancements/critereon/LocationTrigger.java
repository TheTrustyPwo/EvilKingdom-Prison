package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class LocationTrigger extends SimpleCriterionTrigger<LocationTrigger.TriggerInstance> {
    final ResourceLocation id;

    public LocationTrigger(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public LocationTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        JsonObject jsonObject2 = GsonHelper.getAsJsonObject(jsonObject, "location", jsonObject);
        LocationPredicate locationPredicate = LocationPredicate.fromJson(jsonObject2);
        return new LocationTrigger.TriggerInstance(this.id, composite, locationPredicate);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(player.getLevel(), player.getX(), player.getY(), player.getZ());
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final LocationPredicate location;

        public TriggerInstance(ResourceLocation id, EntityPredicate.Composite player, LocationPredicate location) {
            super(id, player);
            this.location = location;
        }

        public static LocationTrigger.TriggerInstance located(LocationPredicate location) {
            return new LocationTrigger.TriggerInstance(CriteriaTriggers.LOCATION.id, EntityPredicate.Composite.ANY, location);
        }

        public static LocationTrigger.TriggerInstance located(EntityPredicate entity) {
            return new LocationTrigger.TriggerInstance(CriteriaTriggers.LOCATION.id, EntityPredicate.Composite.wrap(entity), LocationPredicate.ANY);
        }

        public static LocationTrigger.TriggerInstance sleptInBed() {
            return new LocationTrigger.TriggerInstance(CriteriaTriggers.SLEPT_IN_BED.id, EntityPredicate.Composite.ANY, LocationPredicate.ANY);
        }

        public static LocationTrigger.TriggerInstance raidWon() {
            return new LocationTrigger.TriggerInstance(CriteriaTriggers.RAID_WIN.id, EntityPredicate.Composite.ANY, LocationPredicate.ANY);
        }

        public static LocationTrigger.TriggerInstance walkOnBlockWithEquipment(Block block, Item boots) {
            return located(EntityPredicate.Builder.entity().equipment(EntityEquipmentPredicate.Builder.equipment().feet(ItemPredicate.Builder.item().of(boots).build()).build()).steppingOn(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(block).build()).build()).build());
        }

        public boolean matches(ServerLevel world, double x, double y, double z) {
            return this.location.matches(world, x, y, z);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("location", this.location.serializeToJson());
            return jsonObject;
        }
    }
}
