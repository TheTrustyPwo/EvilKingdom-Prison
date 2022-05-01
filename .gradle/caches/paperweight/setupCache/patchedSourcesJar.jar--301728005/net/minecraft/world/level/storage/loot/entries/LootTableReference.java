package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootTableReference extends LootPoolSingletonContainer {
    final ResourceLocation name;

    LootTableReference(ResourceLocation id, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
        super(weight, quality, conditions, functions);
        this.name = id;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.REFERENCE;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> lootConsumer, LootContext context) {
        LootTable lootTable = context.getLootTable(this.name);
        lootTable.getRandomItemsRaw(context, lootConsumer);
    }

    @Override
    public void validate(ValidationContext reporter) {
        if (reporter.hasVisitedTable(this.name)) {
            reporter.reportProblem("Table " + this.name + " is recursively called");
        } else {
            super.validate(reporter);
            LootTable lootTable = reporter.resolveLootTable(this.name);
            if (lootTable == null) {
                reporter.reportProblem("Unknown loot table called " + this.name);
            } else {
                lootTable.validate(reporter.enterTable("->{" + this.name + "}", this.name));
            }

        }
    }

    public static LootPoolSingletonContainer.Builder<?> lootTableReference(ResourceLocation id) {
        return simpleBuilder((weight, quality, conditions, functions) -> {
            return new LootTableReference(id, weight, quality, conditions, functions);
        });
    }

    public static class Serializer extends LootPoolSingletonContainer.Serializer<LootTableReference> {
        @Override
        public void serializeCustom(JsonObject json, LootTableReference entry, JsonSerializationContext context) {
            super.serializeCustom(json, entry, context);
            json.addProperty("name", entry.name.toString());
        }

        @Override
        protected LootTableReference deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, int i, int j, LootItemCondition[] lootItemConditions, LootItemFunction[] lootItemFunctions) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "name"));
            return new LootTableReference(resourceLocation, i, j, lootItemConditions, lootItemFunctions);
        }
    }
}
