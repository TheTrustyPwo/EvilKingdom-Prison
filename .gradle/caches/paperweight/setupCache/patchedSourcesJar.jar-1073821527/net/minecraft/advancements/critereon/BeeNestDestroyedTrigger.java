package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BeeNestDestroyedTrigger extends SimpleCriterionTrigger<BeeNestDestroyedTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("bee_nest_destroyed");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public BeeNestDestroyedTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        Block block = deserializeBlock(jsonObject);
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("num_bees_inside"));
        return new BeeNestDestroyedTrigger.TriggerInstance(composite, block, itemPredicate, ints);
    }

    @Nullable
    private static Block deserializeBlock(JsonObject root) {
        if (root.has("block")) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(root, "block"));
            return Registry.BLOCK.getOptional(resourceLocation).orElseThrow(() -> {
                return new JsonSyntaxException("Unknown block type '" + resourceLocation + "'");
            });
        } else {
            return null;
        }
    }

    public void trigger(ServerPlayer player, BlockState state, ItemStack stack, int beeCount) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(state, stack, beeCount);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        @Nullable
        private final Block block;
        private final ItemPredicate item;
        private final MinMaxBounds.Ints numBees;

        public TriggerInstance(EntityPredicate.Composite player, @Nullable Block block, ItemPredicate item, MinMaxBounds.Ints beeCount) {
            super(BeeNestDestroyedTrigger.ID, player);
            this.block = block;
            this.item = item;
            this.numBees = beeCount;
        }

        public static BeeNestDestroyedTrigger.TriggerInstance destroyedBeeNest(Block block, ItemPredicate.Builder itemPredicateBuilder, MinMaxBounds.Ints beeCountRange) {
            return new BeeNestDestroyedTrigger.TriggerInstance(EntityPredicate.Composite.ANY, block, itemPredicateBuilder.build(), beeCountRange);
        }

        public boolean matches(BlockState state, ItemStack stack, int count) {
            if (this.block != null && !state.is(this.block)) {
                return false;
            } else {
                return !this.item.matches(stack) ? false : this.numBees.matches(count);
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (this.block != null) {
                jsonObject.addProperty("block", Registry.BLOCK.getKey(this.block).toString());
            }

            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("num_bees_inside", this.numBees.serializeToJson());
            return jsonObject;
        }
    }
}
