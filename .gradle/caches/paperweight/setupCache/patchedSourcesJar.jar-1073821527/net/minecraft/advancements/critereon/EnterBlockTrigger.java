package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EnterBlockTrigger extends SimpleCriterionTrigger<EnterBlockTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("enter_block");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public EnterBlockTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        Block block = deserializeBlock(jsonObject);
        StatePropertiesPredicate statePropertiesPredicate = StatePropertiesPredicate.fromJson(jsonObject.get("state"));
        if (block != null) {
            statePropertiesPredicate.checkState(block.getStateDefinition(), (name) -> {
                throw new JsonSyntaxException("Block " + block + " has no property " + name);
            });
        }

        return new EnterBlockTrigger.TriggerInstance(composite, block, statePropertiesPredicate);
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

    public void trigger(ServerPlayer player, BlockState state) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(state);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        @Nullable
        private final Block block;
        private final StatePropertiesPredicate state;

        public TriggerInstance(EntityPredicate.Composite player, @Nullable Block block, StatePropertiesPredicate state) {
            super(EnterBlockTrigger.ID, player);
            this.block = block;
            this.state = state;
        }

        public static EnterBlockTrigger.TriggerInstance entersBlock(Block block) {
            return new EnterBlockTrigger.TriggerInstance(EntityPredicate.Composite.ANY, block, StatePropertiesPredicate.ANY);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (this.block != null) {
                jsonObject.addProperty("block", Registry.BLOCK.getKey(this.block).toString());
            }

            jsonObject.add("state", this.state.serializeToJson());
            return jsonObject;
        }

        public boolean matches(BlockState state) {
            if (this.block != null && !state.is(this.block)) {
                return false;
            } else {
                return this.state.matches(state);
            }
        }
    }
}
