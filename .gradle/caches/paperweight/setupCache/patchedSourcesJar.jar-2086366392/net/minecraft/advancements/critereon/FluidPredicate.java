package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public class FluidPredicate {
    public static final FluidPredicate ANY = new FluidPredicate((TagKey<Fluid>)null, (Fluid)null, StatePropertiesPredicate.ANY);
    @Nullable
    private final TagKey<Fluid> tag;
    @Nullable
    private final Fluid fluid;
    private final StatePropertiesPredicate properties;

    public FluidPredicate(@Nullable TagKey<Fluid> tag, @Nullable Fluid fluid, StatePropertiesPredicate state) {
        this.tag = tag;
        this.fluid = fluid;
        this.properties = state;
    }

    public boolean matches(ServerLevel world, BlockPos pos) {
        if (this == ANY) {
            return true;
        } else if (!world.isLoaded(pos)) {
            return false;
        } else {
            FluidState fluidState = world.getFluidState(pos);
            if (this.tag != null && !fluidState.is(this.tag)) {
                return false;
            } else if (this.fluid != null && !fluidState.is(this.fluid)) {
                return false;
            } else {
                return this.properties.matches(fluidState);
            }
        }
    }

    public static FluidPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "fluid");
            Fluid fluid = null;
            if (jsonObject.has("fluid")) {
                ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "fluid"));
                fluid = Registry.FLUID.get(resourceLocation);
            }

            TagKey<Fluid> tagKey = null;
            if (jsonObject.has("tag")) {
                ResourceLocation resourceLocation2 = new ResourceLocation(GsonHelper.getAsString(jsonObject, "tag"));
                tagKey = TagKey.create(Registry.FLUID_REGISTRY, resourceLocation2);
            }

            StatePropertiesPredicate statePropertiesPredicate = StatePropertiesPredicate.fromJson(jsonObject.get("state"));
            return new FluidPredicate(tagKey, fluid, statePropertiesPredicate);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.fluid != null) {
                jsonObject.addProperty("fluid", Registry.FLUID.getKey(this.fluid).toString());
            }

            if (this.tag != null) {
                jsonObject.addProperty("tag", this.tag.location().toString());
            }

            jsonObject.add("state", this.properties.serializeToJson());
            return jsonObject;
        }
    }

    public static class Builder {
        @Nullable
        private Fluid fluid;
        @Nullable
        private TagKey<Fluid> fluids;
        private StatePropertiesPredicate properties = StatePropertiesPredicate.ANY;

        private Builder() {
        }

        public static FluidPredicate.Builder fluid() {
            return new FluidPredicate.Builder();
        }

        public FluidPredicate.Builder of(Fluid fluid) {
            this.fluid = fluid;
            return this;
        }

        public FluidPredicate.Builder of(TagKey<Fluid> tag) {
            this.fluids = tag;
            return this;
        }

        public FluidPredicate.Builder setProperties(StatePropertiesPredicate state) {
            this.properties = state;
            return this;
        }

        public FluidPredicate build() {
            return new FluidPredicate(this.fluids, this.fluid, this.properties);
        }
    }
}
