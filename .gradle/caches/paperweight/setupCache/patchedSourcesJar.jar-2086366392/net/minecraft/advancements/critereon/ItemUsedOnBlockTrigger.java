package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ItemUsedOnBlockTrigger extends SimpleCriterionTrigger<ItemUsedOnBlockTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("item_used_on_block");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public ItemUsedOnBlockTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        LocationPredicate locationPredicate = LocationPredicate.fromJson(jsonObject.get("location"));
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        return new ItemUsedOnBlockTrigger.TriggerInstance(composite, locationPredicate, itemPredicate);
    }

    public void trigger(ServerPlayer player, BlockPos pos, ItemStack stack) {
        BlockState blockState = player.getLevel().getBlockState(pos);
        this.trigger(player, (conditions) -> {
            return conditions.matches(blockState, player.getLevel(), pos, stack);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final LocationPredicate location;
        private final ItemPredicate item;

        public TriggerInstance(EntityPredicate.Composite player, LocationPredicate location, ItemPredicate item) {
            super(ItemUsedOnBlockTrigger.ID, player);
            this.location = location;
            this.item = item;
        }

        public static ItemUsedOnBlockTrigger.TriggerInstance itemUsedOnBlock(LocationPredicate.Builder location, ItemPredicate.Builder item) {
            return new ItemUsedOnBlockTrigger.TriggerInstance(EntityPredicate.Composite.ANY, location.build(), item.build());
        }

        public boolean matches(BlockState state, ServerLevel world, BlockPos pos, ItemStack stack) {
            return !this.location.matches(world, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D) ? false : this.item.matches(stack);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("location", this.location.serializeToJson());
            jsonObject.add("item", this.item.serializeToJson());
            return jsonObject;
        }
    }
}
