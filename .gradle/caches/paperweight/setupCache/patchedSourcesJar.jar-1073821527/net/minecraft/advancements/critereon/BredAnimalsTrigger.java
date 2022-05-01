package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class BredAnimalsTrigger extends SimpleCriterionTrigger<BredAnimalsTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("bred_animals");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public BredAnimalsTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "parent", deserializationContext);
        EntityPredicate.Composite composite3 = EntityPredicate.Composite.fromJson(jsonObject, "partner", deserializationContext);
        EntityPredicate.Composite composite4 = EntityPredicate.Composite.fromJson(jsonObject, "child", deserializationContext);
        return new BredAnimalsTrigger.TriggerInstance(composite, composite2, composite3, composite4);
    }

    public void trigger(ServerPlayer player, Animal parent, Animal partner, @Nullable AgeableMob child) {
        LootContext lootContext = EntityPredicate.createContext(player, parent);
        LootContext lootContext2 = EntityPredicate.createContext(player, partner);
        LootContext lootContext3 = child != null ? EntityPredicate.createContext(player, child) : null;
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext, lootContext2, lootContext3);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final EntityPredicate.Composite parent;
        private final EntityPredicate.Composite partner;
        private final EntityPredicate.Composite child;

        public TriggerInstance(EntityPredicate.Composite player, EntityPredicate.Composite parent, EntityPredicate.Composite partner, EntityPredicate.Composite child) {
            super(BredAnimalsTrigger.ID, player);
            this.parent = parent;
            this.partner = partner;
            this.child = child;
        }

        public static BredAnimalsTrigger.TriggerInstance bredAnimals() {
            return new BredAnimalsTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY);
        }

        public static BredAnimalsTrigger.TriggerInstance bredAnimals(EntityPredicate.Builder child) {
            return new BredAnimalsTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(child.build()));
        }

        public static BredAnimalsTrigger.TriggerInstance bredAnimals(EntityPredicate parent, EntityPredicate partner, EntityPredicate child) {
            return new BredAnimalsTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(parent), EntityPredicate.Composite.wrap(partner), EntityPredicate.Composite.wrap(child));
        }

        public boolean matches(LootContext parentContext, LootContext partnerContext, @Nullable LootContext childContext) {
            if (this.child == EntityPredicate.Composite.ANY || childContext != null && this.child.matches(childContext)) {
                return this.parent.matches(parentContext) && this.partner.matches(partnerContext) || this.parent.matches(partnerContext) && this.partner.matches(parentContext);
            } else {
                return false;
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("parent", this.parent.toJson(predicateSerializer));
            jsonObject.add("partner", this.partner.toJson(predicateSerializer));
            jsonObject.add("child", this.child.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
