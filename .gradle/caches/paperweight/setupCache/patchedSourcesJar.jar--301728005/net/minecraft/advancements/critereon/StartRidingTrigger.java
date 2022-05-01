package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class StartRidingTrigger extends SimpleCriterionTrigger<StartRidingTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("started_riding");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public StartRidingTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        return new StartRidingTrigger.TriggerInstance(composite);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, (conditions) -> {
            return true;
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        public TriggerInstance(EntityPredicate.Composite player) {
            super(StartRidingTrigger.ID, player);
        }

        public static StartRidingTrigger.TriggerInstance playerStartsRiding(EntityPredicate.Builder player) {
            return new StartRidingTrigger.TriggerInstance(EntityPredicate.Composite.wrap(player.build()));
        }
    }
}
