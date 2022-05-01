package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetContainerContents extends LootItemConditionalFunction {
    final List<LootPoolEntryContainer> entries;
    final BlockEntityType<?> type;

    SetContainerContents(LootItemCondition[] conditions, BlockEntityType<?> type, List<LootPoolEntryContainer> entries) {
        super(conditions);
        this.type = type;
        this.entries = ImmutableList.copyOf(entries);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_CONTENTS;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        if (stack.isEmpty()) {
            return stack;
        } else {
            NonNullList<ItemStack> nonNullList = NonNullList.create();
            this.entries.forEach((entry) -> {
                entry.expand(context, (choice) -> {
                    choice.createItemStack(LootTable.createStackSplitter(nonNullList::add, context.getLevel()), context); // Paper - preserve overstacked items
                });
            });
            CompoundTag compoundTag = new CompoundTag();
            ContainerHelper.saveAllItems(compoundTag, nonNullList);
            CompoundTag compoundTag2 = BlockItem.getBlockEntityData(stack);
            if (compoundTag2 == null) {
                compoundTag2 = compoundTag;
            } else {
                compoundTag2.merge(compoundTag);
            }

            BlockItem.setBlockEntityData(stack, this.type, compoundTag2);
            return stack;
        }
    }

    @Override
    public void validate(ValidationContext reporter) {
        super.validate(reporter);

        for(int i = 0; i < this.entries.size(); ++i) {
            this.entries.get(i).validate(reporter.forChild(".entry[" + i + "]"));
        }

    }

    public static SetContainerContents.Builder setContents(BlockEntityType<?> type) {
        return new SetContainerContents.Builder(type);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetContainerContents.Builder> {
        private final List<LootPoolEntryContainer> entries = Lists.newArrayList();
        private final BlockEntityType<?> type;

        public Builder(BlockEntityType<?> type) {
            this.type = type;
        }

        @Override
        protected SetContainerContents.Builder getThis() {
            return this;
        }

        public SetContainerContents.Builder withEntry(LootPoolEntryContainer.Builder<?> entryBuilder) {
            this.entries.add(entryBuilder.build());
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetContainerContents(this.getConditions(), this.type, this.entries);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetContainerContents> {
        @Override
        public void serialize(JsonObject json, SetContainerContents object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("type", Registry.BLOCK_ENTITY_TYPE.getKey(object.type).toString());
            json.add("entries", context.serialize(object.entries));
        }

        @Override
        public SetContainerContents deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            LootPoolEntryContainer[] lootPoolEntryContainers = GsonHelper.getAsObject(jsonObject, "entries", jsonDeserializationContext, LootPoolEntryContainer[].class);
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "type"));
            BlockEntityType<?> blockEntityType = Registry.BLOCK_ENTITY_TYPE.getOptional(resourceLocation).orElseThrow(() -> {
                return new JsonSyntaxException("Unknown block entity type id '" + resourceLocation + "'");
            });
            return new SetContainerContents(lootItemConditions, blockEntityType, Arrays.asList(lootPoolEntryContainers));
        }
    }
}
