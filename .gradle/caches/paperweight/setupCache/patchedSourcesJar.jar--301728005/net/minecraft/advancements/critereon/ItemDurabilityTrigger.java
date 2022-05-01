package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ItemDurabilityTrigger extends SimpleCriterionTrigger<ItemDurabilityTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("item_durability_changed");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public ItemDurabilityTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("durability"));
        MinMaxBounds.Ints ints2 = MinMaxBounds.Ints.fromJson(jsonObject.get("delta"));
        return new ItemDurabilityTrigger.TriggerInstance(composite, itemPredicate, ints, ints2);
    }

    public void trigger(ServerPlayer player, ItemStack stack, int durability) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack, durability);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final ItemPredicate item;
        private final MinMaxBounds.Ints durability;
        private final MinMaxBounds.Ints delta;

        public TriggerInstance(EntityPredicate.Composite player, ItemPredicate item, MinMaxBounds.Ints durability, MinMaxBounds.Ints delta) {
            super(ItemDurabilityTrigger.ID, player);
            this.item = item;
            this.durability = durability;
            this.delta = delta;
        }

        public static ItemDurabilityTrigger.TriggerInstance changedDurability(ItemPredicate item, MinMaxBounds.Ints durability) {
            return changedDurability(EntityPredicate.Composite.ANY, item, durability);
        }

        public static ItemDurabilityTrigger.TriggerInstance changedDurability(EntityPredicate.Composite player, ItemPredicate item, MinMaxBounds.Ints durability) {
            return new ItemDurabilityTrigger.TriggerInstance(player, item, durability, MinMaxBounds.Ints.ANY);
        }

        public boolean matches(ItemStack stack, int durability) {
            if (!this.item.matches(stack)) {
                return false;
            } else if (!this.durability.matches(stack.getMaxDamage() - durability)) {
                return false;
            } else {
                return this.delta.matches(stack.getDamageValue() - durability);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("durability", this.durability.serializeToJson());
            jsonObject.add("delta", this.delta.serializeToJson());
            return jsonObject;
        }
    }
}
