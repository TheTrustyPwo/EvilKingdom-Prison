package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class UsedEnderEyeTrigger extends SimpleCriterionTrigger<UsedEnderEyeTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("used_ender_eye");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public UsedEnderEyeTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        MinMaxBounds.Doubles doubles = MinMaxBounds.Doubles.fromJson(jsonObject.get("distance"));
        return new UsedEnderEyeTrigger.TriggerInstance(composite, doubles);
    }

    public void trigger(ServerPlayer player, BlockPos strongholdPos) {
        double d = player.getX() - (double)strongholdPos.getX();
        double e = player.getZ() - (double)strongholdPos.getZ();
        double f = d * d + e * e;
        this.trigger(player, (conditions) -> {
            return conditions.matches(f);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final MinMaxBounds.Doubles level;

        public TriggerInstance(EntityPredicate.Composite player, MinMaxBounds.Doubles distance) {
            super(UsedEnderEyeTrigger.ID, player);
            this.level = distance;
        }

        public boolean matches(double distance) {
            return this.level.matchesSqr(distance);
        }
    }
}
