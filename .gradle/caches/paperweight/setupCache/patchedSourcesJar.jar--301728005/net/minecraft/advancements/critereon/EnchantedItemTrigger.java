package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class EnchantedItemTrigger extends SimpleCriterionTrigger<EnchantedItemTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("enchanted_item");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public EnchantedItemTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("levels"));
        return new EnchantedItemTrigger.TriggerInstance(composite, itemPredicate, ints);
    }

    public void trigger(ServerPlayer player, ItemStack stack, int levels) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack, levels);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final ItemPredicate item;
        private final MinMaxBounds.Ints levels;

        public TriggerInstance(EntityPredicate.Composite player, ItemPredicate item, MinMaxBounds.Ints levels) {
            super(EnchantedItemTrigger.ID, player);
            this.item = item;
            this.levels = levels;
        }

        public static EnchantedItemTrigger.TriggerInstance enchantedItem() {
            return new EnchantedItemTrigger.TriggerInstance(EntityPredicate.Composite.ANY, ItemPredicate.ANY, MinMaxBounds.Ints.ANY);
        }

        public boolean matches(ItemStack stack, int levels) {
            if (!this.item.matches(stack)) {
                return false;
            } else {
                return this.levels.matches(levels);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("levels", this.levels.serializeToJson());
            return jsonObject;
        }
    }
}
