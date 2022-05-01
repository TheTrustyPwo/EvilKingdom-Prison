package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class DynamicLoot extends LootPoolSingletonContainer {
    final ResourceLocation name;

    DynamicLoot(ResourceLocation name, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
        super(weight, quality, conditions, functions);
        this.name = name;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.DYNAMIC;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> lootConsumer, LootContext context) {
        context.addDynamicDrops(this.name, lootConsumer);
    }

    public static LootPoolSingletonContainer.Builder<?> dynamicEntry(ResourceLocation name) {
        return simpleBuilder((weight, quality, conditions, functions) -> {
            return new DynamicLoot(name, weight, quality, conditions, functions);
        });
    }

    public static class Serializer extends LootPoolSingletonContainer.Serializer<DynamicLoot> {
        @Override
        public void serializeCustom(JsonObject json, DynamicLoot entry, JsonSerializationContext context) {
            super.serializeCustom(json, entry, context);
            json.addProperty("name", entry.name.toString());
        }

        @Override
        protected DynamicLoot deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, int i, int j, LootItemCondition[] lootItemConditions, LootItemFunction[] lootItemFunctions) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "name"));
            return new DynamicLoot(resourceLocation, i, j, lootItemConditions, lootItemFunctions);
        }
    }
}
