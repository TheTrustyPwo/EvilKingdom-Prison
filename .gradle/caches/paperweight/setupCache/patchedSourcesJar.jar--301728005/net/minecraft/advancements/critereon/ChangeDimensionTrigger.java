package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;

public class ChangeDimensionTrigger extends SimpleCriterionTrigger<ChangeDimensionTrigger.TriggerInstance> {
    static final ResourceLocation ID = new ResourceLocation("changed_dimension");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public ChangeDimensionTrigger.TriggerInstance createInstance(JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext) {
        ResourceKey<Level> resourceKey = jsonObject.has("from") ? ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(GsonHelper.getAsString(jsonObject, "from"))) : null;
        ResourceKey<Level> resourceKey2 = jsonObject.has("to") ? ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(GsonHelper.getAsString(jsonObject, "to"))) : null;
        return new ChangeDimensionTrigger.TriggerInstance(composite, resourceKey, resourceKey2);
    }

    public void trigger(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(from, to);
        });
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        @Nullable
        private final ResourceKey<Level> from;
        @Nullable
        private final ResourceKey<Level> to;

        public TriggerInstance(EntityPredicate.Composite player, @Nullable ResourceKey<Level> from, @Nullable ResourceKey<Level> to) {
            super(ChangeDimensionTrigger.ID, player);
            this.from = from;
            this.to = to;
        }

        public static ChangeDimensionTrigger.TriggerInstance changedDimension() {
            return new ChangeDimensionTrigger.TriggerInstance(EntityPredicate.Composite.ANY, (ResourceKey<Level>)null, (ResourceKey<Level>)null);
        }

        public static ChangeDimensionTrigger.TriggerInstance changedDimension(ResourceKey<Level> from, ResourceKey<Level> to) {
            return new ChangeDimensionTrigger.TriggerInstance(EntityPredicate.Composite.ANY, from, to);
        }

        public static ChangeDimensionTrigger.TriggerInstance changedDimensionTo(ResourceKey<Level> to) {
            return new ChangeDimensionTrigger.TriggerInstance(EntityPredicate.Composite.ANY, (ResourceKey<Level>)null, to);
        }

        public static ChangeDimensionTrigger.TriggerInstance changedDimensionFrom(ResourceKey<Level> from) {
            return new ChangeDimensionTrigger.TriggerInstance(EntityPredicate.Composite.ANY, from, (ResourceKey<Level>)null);
        }

        public boolean matches(ResourceKey<Level> from, ResourceKey<Level> to) {
            if (this.from != null && this.from != from) {
                return false;
            } else {
                return this.to == null || this.to == to;
            }
        }

        @Override
        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (this.from != null) {
                jsonObject.addProperty("from", this.from.location().toString());
            }

            if (this.to != null) {
                jsonObject.addProperty("to", this.to.location().toString());
            }

            return jsonObject;
        }
    }
}
