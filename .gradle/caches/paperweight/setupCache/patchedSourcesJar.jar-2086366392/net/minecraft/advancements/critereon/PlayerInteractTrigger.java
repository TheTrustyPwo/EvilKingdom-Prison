package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerInteractTrigger extends SimpleCriterionTrigger<PlayerInteractTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("player_interacted_with_entity");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected PlayerInteractTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "entity", deserializationContext);
        return new PlayerInteractTrigger.TriggerInstance(composite, itemPredicate, composite2);
    }

    public void trigger(ServerPlayer player, ItemStack stack, Entity entity) {
        LootContext lootContext = EntityPredicate.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack, lootContext);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final ItemPredicate item;
        private final EntityPredicate.Composite entity;

        public TriggerInstance(EntityPredicate.Composite player, ItemPredicate item, EntityPredicate.Composite entity) {
            super(PlayerInteractTrigger.ID, player);
            this.item = item;
            this.entity = entity;
        }

        public static PlayerInteractTrigger.TriggerInstance itemUsedOnEntity(EntityPredicate.Composite player, ItemPredicate.Builder itemBuilder, EntityPredicate.Composite entity) {
            return new PlayerInteractTrigger.TriggerInstance(player, itemBuilder.build(), entity);
        }

        public boolean matches(ItemStack stack, LootContext context) {
            return !this.item.matches(stack) ? false : this.entity.matches(context);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
