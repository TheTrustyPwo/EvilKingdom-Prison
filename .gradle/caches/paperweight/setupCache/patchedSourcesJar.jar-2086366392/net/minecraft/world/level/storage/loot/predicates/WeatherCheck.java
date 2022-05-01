package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;

public class WeatherCheck implements LootItemCondition {
    @Nullable
    final Boolean isRaining;
    @Nullable
    final Boolean isThundering;

    WeatherCheck(@Nullable Boolean raining, @Nullable Boolean thundering) {
        this.isRaining = raining;
        this.isThundering = thundering;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.WEATHER_CHECK;
    }

    @Override
    public boolean test(LootContext lootContext) {
        ServerLevel serverLevel = lootContext.getLevel();
        if (this.isRaining != null && this.isRaining != serverLevel.isRaining()) {
            return false;
        } else {
            return this.isThundering == null || this.isThundering == serverLevel.isThundering();
        }
    }

    public static WeatherCheck.Builder weather() {
        return new WeatherCheck.Builder();
    }

    public static class Builder implements LootItemCondition.Builder {
        @Nullable
        private Boolean isRaining;
        @Nullable
        private Boolean isThundering;

        public WeatherCheck.Builder setRaining(@Nullable Boolean raining) {
            this.isRaining = raining;
            return this;
        }

        public WeatherCheck.Builder setThundering(@Nullable Boolean thundering) {
            this.isThundering = thundering;
            return this;
        }

        @Override
        public WeatherCheck build() {
            return new WeatherCheck(this.isRaining, this.isThundering);
        }
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<WeatherCheck> {
        @Override
        public void serialize(JsonObject json, WeatherCheck object, JsonSerializationContext context) {
            json.addProperty("raining", object.isRaining);
            json.addProperty("thundering", object.isThundering);
        }

        @Override
        public WeatherCheck deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            Boolean boolean_ = jsonObject.has("raining") ? GsonHelper.getAsBoolean(jsonObject, "raining") : null;
            Boolean boolean2 = jsonObject.has("thundering") ? GsonHelper.getAsBoolean(jsonObject, "thundering") : null;
            return new WeatherCheck(boolean_, boolean2);
        }
    }
}
