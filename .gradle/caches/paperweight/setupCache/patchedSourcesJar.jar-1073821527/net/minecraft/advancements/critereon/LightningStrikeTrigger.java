package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.storage.loot.LootContext;

public class LightningStrikeTrigger extends SimpleCriterionTrigger<LightningStrikeTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("lightning_strike");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public LightningStrikeTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "lightning", deserializationContext);
        EntityPredicate.Composite composite3 = EntityPredicate.Composite.fromJson(jsonObject, "bystander", deserializationContext);
        return new LightningStrikeTrigger.TriggerInstance(composite, composite2, composite3);
    }

    public void trigger(ServerPlayer player, LightningBolt lightning, List<Entity> bystanders) {
        List<LootContext> list = bystanders.stream().map((bystander) -> {
            return EntityPredicate.createContext(player, bystander);
        }).collect(Collectors.toList());
        LootContext lootContext = EntityPredicate.createContext(player, lightning);
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext, list);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final EntityPredicate.Composite lightning;
        private final EntityPredicate.Composite bystander;

        public TriggerInstance(EntityPredicate.Composite player, EntityPredicate.Composite lightning, EntityPredicate.Composite bystander) {
            super(LightningStrikeTrigger.ID, player);
            this.lightning = lightning;
            this.bystander = bystander;
        }

        public static LightningStrikeTrigger.TriggerInstance lighthingStrike(EntityPredicate lightning, EntityPredicate bystander) {
            return new LightningStrikeTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(lightning), EntityPredicate.Composite.wrap(bystander));
        }

        public boolean matches(LootContext lightning, List<LootContext> bystanders) {
            if (!this.lightning.matches(lightning)) {
                return false;
            } else {
                return this.bystander == EntityPredicate.Composite.ANY || !bystanders.stream().noneMatch(this.bystander::matches);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("lightning", this.lightning.toJson(predicateSerializer));
            jsonObject.add("bystander", this.bystander.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
