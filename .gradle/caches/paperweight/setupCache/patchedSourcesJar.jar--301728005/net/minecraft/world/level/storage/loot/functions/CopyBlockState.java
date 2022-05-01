package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyBlockState extends LootItemConditionalFunction {
    final Block block;
    final Set<Property<?>> properties;

    CopyBlockState(LootItemCondition[] conditions, Block block, Set<Property<?>> properties) {
        super(conditions);
        this.block = block;
        this.properties = properties;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.COPY_STATE;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.BLOCK_STATE);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        BlockState blockState = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (blockState != null) {
            CompoundTag compoundTag = stack.getOrCreateTag();
            CompoundTag compoundTag2;
            if (compoundTag.contains("BlockStateTag", 10)) {
                compoundTag2 = compoundTag.getCompound("BlockStateTag");
            } else {
                compoundTag2 = new CompoundTag();
                compoundTag.put("BlockStateTag", compoundTag2);
            }

            this.properties.stream().filter(blockState::hasProperty).forEach((property) -> {
                compoundTag2.putString(property.getName(), serialize(blockState, property));
            });
        }

        return stack;
    }

    public static CopyBlockState.Builder copyState(Block block) {
        return new CopyBlockState.Builder(block);
    }

    private static <T extends Comparable<T>> String serialize(BlockState state, Property<T> property) {
        T comparable = state.getValue(property);
        return property.getName(comparable);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<CopyBlockState.Builder> {
        private final Block block;
        private final Set<Property<?>> properties = Sets.newHashSet();

        Builder(Block block) {
            this.block = block;
        }

        public CopyBlockState.Builder copy(Property<?> property) {
            if (!this.block.getStateDefinition().getProperties().contains(property)) {
                throw new IllegalStateException("Property " + property + " is not present on block " + this.block);
            } else {
                this.properties.add(property);
                return this;
            }
        }

        @Override
        protected CopyBlockState.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new CopyBlockState(this.getConditions(), this.block, this.properties);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<CopyBlockState> {
        @Override
        public void serialize(JsonObject json, CopyBlockState object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("block", Registry.BLOCK.getKey(object.block).toString());
            JsonArray jsonArray = new JsonArray();
            object.properties.forEach((property) -> {
                jsonArray.add(property.getName());
            });
            json.add("properties", jsonArray);
        }

        @Override
        public CopyBlockState deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "block"));
            Block block = Registry.BLOCK.getOptional(resourceLocation).orElseThrow(() -> {
                return new IllegalArgumentException("Can't find block " + resourceLocation);
            });
            StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();
            Set<Property<?>> set = Sets.newHashSet();
            JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "properties", (JsonArray)null);
            if (jsonArray != null) {
                jsonArray.forEach((jsonElement) -> {
                    set.add(stateDefinition.getProperty(GsonHelper.convertToString(jsonElement, "property")));
                });
            }

            return new CopyBlockState(lootItemConditions, block, set);
        }
    }
}
