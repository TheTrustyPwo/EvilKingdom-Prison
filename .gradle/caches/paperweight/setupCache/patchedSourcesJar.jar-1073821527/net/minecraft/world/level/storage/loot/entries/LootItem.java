package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItem extends LootPoolSingletonContainer {
    final Item item;

    LootItem(Item item, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
        super(weight, quality, conditions, functions);
        this.item = item;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.ITEM;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> lootConsumer, LootContext context) {
        lootConsumer.accept(new ItemStack(this.item));
    }

    public static LootPoolSingletonContainer.Builder<?> lootTableItem(ItemLike drop) {
        return simpleBuilder((weight, quality, conditions, functions) -> {
            return new LootItem(drop.asItem(), weight, quality, conditions, functions);
        });
    }

    public static class Serializer extends LootPoolSingletonContainer.Serializer<LootItem> {
        @Override
        public void serializeCustom(JsonObject json, LootItem entry, JsonSerializationContext context) {
            super.serializeCustom(json, entry, context);
            ResourceLocation resourceLocation = Registry.ITEM.getKey(entry.item);
            if (resourceLocation == null) {
                throw new IllegalArgumentException("Can't serialize unknown item " + entry.item);
            } else {
                json.addProperty("name", resourceLocation.toString());
            }
        }

        @Override
        protected LootItem deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, int i, int j, LootItemCondition[] lootItemConditions, LootItemFunction[] lootItemFunctions) {
            Item item = GsonHelper.getAsItem(jsonObject, "name");
            return new LootItem(item, i, j, lootItemConditions, lootItemFunctions);
        }
    }
}
