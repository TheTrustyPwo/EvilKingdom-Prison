package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;

public class EffectsChangedTrigger extends SimpleCriterionTrigger<EffectsChangedTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("effects_changed");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public EffectsChangedTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        MobEffectsPredicate mobEffectsPredicate = MobEffectsPredicate.fromJson(jsonObject.get("effects"));
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "source", deserializationContext);
        return new EffectsChangedTrigger.TriggerInstance(composite, mobEffectsPredicate, composite2);
    }

    public void trigger(ServerPlayer player, @Nullable Entity source) {
        LootContext lootContext = source != null ? EntityPredicate.createContext(player, source) : null;
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, lootContext);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final MobEffectsPredicate effects;
        private final EntityPredicate.Composite source;

        public TriggerInstance(EntityPredicate.Composite player, MobEffectsPredicate effects, EntityPredicate.Composite source) {
            super(EffectsChangedTrigger.ID, player);
            this.effects = effects;
            this.source = source;
        }

        public static EffectsChangedTrigger.TriggerInstance hasEffects(MobEffectsPredicate effects) {
            return new EffectsChangedTrigger.TriggerInstance(EntityPredicate.Composite.ANY, effects, EntityPredicate.Composite.ANY);
        }

        public static EffectsChangedTrigger.TriggerInstance gotEffectsFrom(EntityPredicate source) {
            return new EffectsChangedTrigger.TriggerInstance(EntityPredicate.Composite.ANY, MobEffectsPredicate.ANY, EntityPredicate.Composite.wrap(source));
        }

        public boolean matches(ServerPlayer player, @Nullable LootContext context) {
            if (!this.effects.matches((LivingEntity)player)) {
                return false;
            } else {
                return this.source == EntityPredicate.Composite.ANY || context != null && this.source.matches(context);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("effects", this.effects.serializeToJson());
            jsonObject.add("source", this.source.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
