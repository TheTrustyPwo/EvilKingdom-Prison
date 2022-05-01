package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PlacedBlockTrigger extends SimpleCriterionTrigger<PlacedBlockTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("placed_block");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public PlacedBlockTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        Block block = deserializeBlock(jsonObject);
        StatePropertiesPredicate statePropertiesPredicate = StatePropertiesPredicate.fromJson(jsonObject.get("state"));
        if (block != null) {
            statePropertiesPredicate.checkState(block.getStateDefinition(), (name) -> {
                throw new JsonSyntaxException("Block " + block + " has no property " + name + ":");
            });
        }

        LocationPredicate locationPredicate = LocationPredicate.fromJson(jsonObject.get("location"));
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        return new PlacedBlockTrigger.TriggerInstance(composite, block, statePropertiesPredicate, locationPredicate, itemPredicate);
    }

    @Nullable
    private static Block deserializeBlock(JsonObject obj) {
        if (obj.has("block")) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(obj, "block"));
            return Registry.BLOCK.getOptional(resourceLocation).orElseThrow(() -> {
                return new JsonSyntaxException("Unknown block type '" + resourceLocation + "'");
            });
        } else {
            return null;
        }
    }

    public void trigger(ServerPlayer player, BlockPos blockPos, ItemStack stack) {
        BlockState blockState = player.getLevel().getBlockState(blockPos);
        this.trigger(player, (conditions) -> {
            return conditions.matches(blockState, blockPos, player.getLevel(), stack);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        @Nullable
        private final Block block;
        private final StatePropertiesPredicate state;
        private final LocationPredicate location;
        private final ItemPredicate item;

        public TriggerInstance(EntityPredicate.Composite player, @Nullable Block block, StatePropertiesPredicate state, LocationPredicate location, ItemPredicate item) {
            super(PlacedBlockTrigger.ID, player);
            this.block = block;
            this.state = state;
            this.location = location;
            this.item = item;
        }

        public static PlacedBlockTrigger.TriggerInstance placedBlock(Block block) {
            return new PlacedBlockTrigger.TriggerInstance(EntityPredicate.Composite.ANY, block, StatePropertiesPredicate.ANY, LocationPredicate.ANY, ItemPredicate.ANY);
        }

        public boolean matches(BlockState state, BlockPos pos, ServerLevel world, ItemStack stack) {
            if (this.block != null && !state.is(this.block)) {
                return false;
            } else if (!this.state.matches(state)) {
                return false;
            } else if (!this.location.matches(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ())) {
                return false;
            } else {
                return this.item.matches(stack);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (this.block != null) {
                jsonObject.addProperty("block", Registry.BLOCK.getKey(this.block).toString());
            }

            jsonObject.add("state", this.state.serializeToJson());
            jsonObject.add("location", this.location.serializeToJson());
            jsonObject.add("item", this.item.serializeToJson());
            return jsonObject;
        }
    }
}
