package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.ItemLike;

public class InventoryChangeTrigger extends SimpleCriterionTrigger<InventoryChangeTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("inventory_changed");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public InventoryChangeTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        JsonObject jsonObject2 = GsonHelper.getAsJsonObject(jsonObject, "slots", new JsonObject());
        MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject2.get("occupied"));
        MinMaxBounds.Ints ints2 = MinMaxBounds.Ints.fromJson(jsonObject2.get("full"));
        MinMaxBounds.Ints ints3 = MinMaxBounds.Ints.fromJson(jsonObject2.get("empty"));
        ItemPredicate[] itemPredicates = ItemPredicate.fromJsonArray(jsonObject.get("items"));
        return new InventoryChangeTrigger.TriggerInstance(composite, ints, ints2, ints3, itemPredicates);
    }

    public void trigger(ServerPlayer player, Inventory inventory, ItemStack stack) {
        int i = 0;
        int j = 0;
        int k = 0;

        for(int l = 0; l < inventory.getContainerSize(); ++l) {
            ItemStack itemStack = inventory.getItem(l);
            if (itemStack.isEmpty()) {
                ++j;
            } else {
                ++k;
                if (itemStack.getCount() >= itemStack.getMaxStackSize()) {
                    ++i;
                }
            }
        }

        this.trigger(player, inventory, stack, i, j, k);
    }

    private void trigger(ServerPlayer player, Inventory inventory, ItemStack stack, int full, int empty, int occupied) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(inventory, stack, full, empty, occupied);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final MinMaxBounds.Ints slotsOccupied;
        private final MinMaxBounds.Ints slotsFull;
        private final MinMaxBounds.Ints slotsEmpty;
        private final ItemPredicate[] predicates;

        public TriggerInstance(EntityPredicate.Composite player, MinMaxBounds.Ints occupied, MinMaxBounds.Ints full, MinMaxBounds.Ints empty, ItemPredicate[] items) {
            super(InventoryChangeTrigger.ID, player);
            this.slotsOccupied = occupied;
            this.slotsFull = full;
            this.slotsEmpty = empty;
            this.predicates = items;
        }

        public static InventoryChangeTrigger.TriggerInstance hasItems(ItemPredicate... items) {
            return new InventoryChangeTrigger.TriggerInstance(EntityPredicate.Composite.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, items);
        }

        public static InventoryChangeTrigger.TriggerInstance hasItems(ItemLike... items) {
            ItemPredicate[] itemPredicates = new ItemPredicate[items.length];

            for(int i = 0; i < items.length; ++i) {
                itemPredicates[i] = new ItemPredicate((TagKey<Item>)null, ImmutableSet.of(items[i].asItem()), MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, EnchantmentPredicate.NONE, EnchantmentPredicate.NONE, (Potion)null, NbtPredicate.ANY);
            }

            return hasItems(itemPredicates);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (!this.slotsOccupied.isAny() || !this.slotsFull.isAny() || !this.slotsEmpty.isAny()) {
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.add("occupied", this.slotsOccupied.serializeToJson());
                jsonObject2.add("full", this.slotsFull.serializeToJson());
                jsonObject2.add("empty", this.slotsEmpty.serializeToJson());
                jsonObject.add("slots", jsonObject2);
            }

            if (this.predicates.length > 0) {
                JsonArray jsonArray = new JsonArray();

                for(ItemPredicate itemPredicate : this.predicates) {
                    jsonArray.add(itemPredicate.serializeToJson());
                }

                jsonObject.add("items", jsonArray);
            }

            return jsonObject;
        }

        public boolean matches(Inventory inventory, ItemStack stack, int full, int empty, int occupied) {
            if (!this.slotsFull.matches(full)) {
                return false;
            } else if (!this.slotsEmpty.matches(empty)) {
                return false;
            } else if (!this.slotsOccupied.matches(occupied)) {
                return false;
            } else {
                int i = this.predicates.length;
                if (i == 0) {
                    return true;
                } else if (i != 1) {
                    List<ItemPredicate> list = new ObjectArrayList<>(this.predicates);
                    int j = inventory.getContainerSize();

                    for(int k = 0; k < j; ++k) {
                        if (list.isEmpty()) {
                            return true;
                        }

                        ItemStack itemStack = inventory.getItem(k);
                        if (!itemStack.isEmpty()) {
                            list.removeIf((item) -> {
                                return item.matches(itemStack);
                            });
                        }
                    }

                    return list.isEmpty();
                } else {
                    return !stack.isEmpty() && this.predicates[0].matches(stack);
                }
            }
        }
    }
}
