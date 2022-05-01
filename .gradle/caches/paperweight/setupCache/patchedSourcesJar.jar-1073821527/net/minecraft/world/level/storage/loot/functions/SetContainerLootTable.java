package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetContainerLootTable extends LootItemConditionalFunction {
    final ResourceLocation name;
    final long seed;
    final BlockEntityType<?> type;

    SetContainerLootTable(LootItemCondition[] conditions, ResourceLocation id, long seed, BlockEntityType<?> type) {
        super(conditions);
        this.name = id;
        this.seed = seed;
        this.type = type;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_LOOT_TABLE;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        if (stack.isEmpty()) {
            return stack;
        } else {
            CompoundTag compoundTag = BlockItem.getBlockEntityData(stack);
            if (compoundTag == null) {
                compoundTag = new CompoundTag();
            }

            compoundTag.putString("LootTable", this.name.toString());
            if (this.seed != 0L) {
                compoundTag.putLong("LootTableSeed", this.seed);
            }

            BlockItem.setBlockEntityData(stack, this.type, compoundTag);
            return stack;
        }
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

    public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> type, ResourceLocation id) {
        return simpleBuilder((conditions) -> {
            return new SetContainerLootTable(conditions, id, 0L, type);
        });
    }

    public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> type, ResourceLocation id, long seed) {
        return simpleBuilder((conditions) -> {
            return new SetContainerLootTable(conditions, id, seed, type);
        });
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetContainerLootTable> {
        @Override
        public void serialize(JsonObject json, SetContainerLootTable object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("name", object.name.toString());
            json.addProperty("type", Registry.BLOCK_ENTITY_TYPE.getKey(object.type).toString());
            if (object.seed != 0L) {
                json.addProperty("seed", object.seed);
            }

        }

        @Override
        public SetContainerLootTable deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "name"));
            long l = GsonHelper.getAsLong(jsonObject, "seed", 0L);
            ResourceLocation resourceLocation2 = new ResourceLocation(GsonHelper.getAsString(jsonObject, "type"));
            BlockEntityType<?> blockEntityType = Registry.BLOCK_ENTITY_TYPE.getOptional(resourceLocation2).orElseThrow(() -> {
                return new JsonSyntaxException("Unknown block entity type id '" + resourceLocation2 + "'");
            });
            return new SetContainerLootTable(lootItemConditions, resourceLocation, l, blockEntityType);
        }
    }
}
