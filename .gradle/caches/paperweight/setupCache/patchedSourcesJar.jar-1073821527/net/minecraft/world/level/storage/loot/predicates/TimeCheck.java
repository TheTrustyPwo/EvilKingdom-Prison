package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public class TimeCheck implements LootItemCondition {
    @Nullable
    final Long period;
    final IntRange value;

    TimeCheck(@Nullable Long period, IntRange value) {
        this.period = period;
        this.value = value;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.TIME_CHECK;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.value.getReferencedContextParams();
    }

    @Override
    public boolean test(LootContext lootContext) {
        ServerLevel serverLevel = lootContext.getLevel();
        long l = serverLevel.getDayTime();
        if (this.period != null) {
            l %= this.period;
        }

        return this.value.test(lootContext, (int)l);
    }

    public static TimeCheck.Builder time(IntRange value) {
        return new TimeCheck.Builder(value);
    }

    public static class Builder implements LootItemCondition.Builder {
        @Nullable
        private Long period;
        private final IntRange value;

        public Builder(IntRange value) {
            this.value = value;
        }

        public TimeCheck.Builder setPeriod(long period) {
            this.period = period;
            return this;
        }

        @Override
        public TimeCheck build() {
            return new TimeCheck(this.period, this.value);
        }
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<TimeCheck> {
        @Override
        public void serialize(JsonObject json, TimeCheck object, JsonSerializationContext context) {
            json.addProperty("period", object.period);
            json.add("value", context.serialize(object.value));
        }

        @Override
        public TimeCheck deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            Long long_ = jsonObject.has("period") ? GsonHelper.getAsLong(jsonObject, "period") : null;
            IntRange intRange = GsonHelper.getAsObject(jsonObject, "value", jsonDeserializationContext, IntRange.class);
            return new TimeCheck(long_, intRange);
        }
    }
}
