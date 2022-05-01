package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class LocationCheck implements LootItemCondition {
    final LocationPredicate predicate;
    final BlockPos offset;

    LocationCheck(LocationPredicate predicate, BlockPos offset) {
        this.predicate = predicate;
        this.offset = offset;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.LOCATION_CHECK;
    }

    @Override
    public boolean test(LootContext lootContext) {
        Vec3 vec3 = lootContext.getParamOrNull(LootContextParams.ORIGIN);
        return vec3 != null && this.predicate.matches(lootContext.getLevel(), vec3.x() + (double)this.offset.getX(), vec3.y() + (double)this.offset.getY(), vec3.z() + (double)this.offset.getZ());
    }

    public static LootItemCondition.Builder checkLocation(LocationPredicate.Builder predicateBuilder) {
        return () -> {
            return new LocationCheck(predicateBuilder.build(), BlockPos.ZERO);
        };
    }

    public static LootItemCondition.Builder checkLocation(LocationPredicate.Builder predicateBuilder, BlockPos pos) {
        return () -> {
            return new LocationCheck(predicateBuilder.build(), pos);
        };
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<LocationCheck> {
        @Override
        public void serialize(JsonObject json, LocationCheck object, JsonSerializationContext context) {
            json.add("predicate", object.predicate.serializeToJson());
            if (object.offset.getX() != 0) {
                json.addProperty("offsetX", object.offset.getX());
            }

            if (object.offset.getY() != 0) {
                json.addProperty("offsetY", object.offset.getY());
            }

            if (object.offset.getZ() != 0) {
                json.addProperty("offsetZ", object.offset.getZ());
            }

        }

        @Override
        public LocationCheck deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LocationPredicate locationPredicate = LocationPredicate.fromJson(jsonObject.get("predicate"));
            int i = GsonHelper.getAsInt(jsonObject, "offsetX", 0);
            int j = GsonHelper.getAsInt(jsonObject, "offsetY", 0);
            int k = GsonHelper.getAsInt(jsonObject, "offsetZ", 0);
            return new LocationCheck(locationPredicate, new BlockPos(i, j, k));
        }
    }
}
