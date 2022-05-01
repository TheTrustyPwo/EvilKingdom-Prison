package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class ItemPickedUpByEntityTrigger extends SimpleCriterionTrigger<ItemPickedUpByEntityTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("thrown_item_picked_up_by_entity");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected ItemPickedUpByEntityTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        EntityPredicate.Composite composite2 = EntityPredicate.Composite.fromJson(jsonObject, "entity", deserializationContext);
        return new ItemPickedUpByEntityTrigger.TriggerInstance(composite, itemPredicate, composite2);
    }

    public void trigger(ServerPlayer player, ItemStack stack, Entity entity) {
        LootContext lootContext = EntityPredicate.createContext(player, entity);
        this.trigger(player, (conditions) -> {
            return conditions.matches(player, stack, lootContext);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final ItemPredicate item;
        private final EntityPredicate.Composite entity;

        public TriggerInstance(EntityPredicate.Composite player, ItemPredicate item, EntityPredicate.Composite entity) {
            super(ItemPickedUpByEntityTrigger.ID, player);
            this.item = item;
            this.entity = entity;
        }

        public static ItemPickedUpByEntityTrigger.TriggerInstance itemPickedUpByEntity(EntityPredicate.Composite player, ItemPredicate.Builder item, EntityPredicate.Composite entity) {
            return new ItemPickedUpByEntityTrigger.TriggerInstance(player, item.build(), entity);
        }

        public boolean matches(ServerPlayer player, ItemStack stack, LootContext entityContext) {
            if (!this.item.matches(stack)) {
                return false;
            } else {
                return this.entity.matches(entityContext);
            }
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
