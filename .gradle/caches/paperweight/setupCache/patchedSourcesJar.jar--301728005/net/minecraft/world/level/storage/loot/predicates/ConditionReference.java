package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import org.slf4j.Logger;

public class ConditionReference implements LootItemCondition {
    private static final Logger LOGGER = LogUtils.getLogger();
    final ResourceLocation name;

    ConditionReference(ResourceLocation id) {
        this.name = id;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.REFERENCE;
    }

    @Override
    public void validate(ValidationContext reporter) {
        if (reporter.hasVisitedCondition(this.name)) {
            reporter.reportProblem("Condition " + this.name + " is recursively called");
        } else {
            LootItemCondition.super.validate(reporter);
            LootItemCondition lootItemCondition = reporter.resolveCondition(this.name);
            if (lootItemCondition == null) {
                reporter.reportProblem("Unknown condition table called " + this.name);
            } else {
                lootItemCondition.validate(reporter.enterTable(".{" + this.name + "}", this.name));
            }

        }
    }

    @Override
    public boolean test(LootContext lootContext) {
        LootItemCondition lootItemCondition = lootContext.getCondition(this.name);
        if (lootContext.addVisitedCondition(lootItemCondition)) {
            boolean var3;
            try {
                var3 = lootItemCondition.test(lootContext);
            } finally {
                lootContext.removeVisitedCondition(lootItemCondition);
            }

            return var3;
        } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return false;
        }
    }

    public static LootItemCondition.Builder conditionReference(ResourceLocation id) {
        return () -> {
            return new ConditionReference(id);
        };
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ConditionReference> {
        @Override
        public void serialize(JsonObject json, ConditionReference object, JsonSerializationContext context) {
            json.addProperty("name", object.name.toString());
        }

        @Override
        public ConditionReference deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "name"));
            return new ConditionReference(resourceLocation);
        }
    }
}
