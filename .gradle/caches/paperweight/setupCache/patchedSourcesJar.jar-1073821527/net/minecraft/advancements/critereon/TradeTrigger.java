package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class TradeTrigger extends SimpleCriterionTrigger<TradeTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("villager_trade");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public TradeTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "villager", deserializationContext);
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        return new TradeTrigger.TriggerInstance(composite, composite2, itemPredicate);
    }

    public void trigger(ServerPlayer player, AbstractVillager merchant, ItemStack stack) {
        LootContext lootContext = EntityPredicate.createContext(player, merchant);
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext, stack);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final EntityPredicate.Composite villager;
        private final ItemPredicate item;

        public TriggerInstance(EntityPredicate.Composite player, EntityPredicate.Composite villager, ItemPredicate item) {
            super(TradeTrigger.ID, player);
            this.villager = villager;
            this.item = item;
        }

        public static TradeTrigger.TriggerInstance tradedWithVillager() {
            return new TradeTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, ItemPredicate.ANY);
        }

        public static TradeTrigger.TriggerInstance tradedWithVillager(EntityPredicate.Builder playerPredicate) {
            return new TradeTrigger.TriggerInstance(EntityPredicate.Composite.wrap(playerPredicate.build()), EntityPredicate.Composite.ANY, ItemPredicate.ANY);
        }

        public boolean matches(LootContext merchantContext, ItemStack stack) {
            if (!this.villager.matches(merchantContext)) {
                return false;
            } else {
                return this.item.matches(stack);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("villager", this.villager.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
