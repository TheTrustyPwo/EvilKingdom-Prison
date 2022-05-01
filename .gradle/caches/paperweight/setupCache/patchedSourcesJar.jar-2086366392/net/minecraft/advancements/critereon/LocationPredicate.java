package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import org.slf4j.Logger;

public class LocationPredicate {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final LocationPredicate ANY = new LocationPredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, (ResourceKey<Biome>)null, (ResourceKey<ConfiguredStructureFeature<?, ?>>)null, (ResourceKey<Level>)null, (Boolean)null, LightPredicate.ANY, BlockPredicate.ANY, FluidPredicate.ANY);
    private final MinMaxBounds.Doubles x;
    private final MinMaxBounds.Doubles y;
    private final MinMaxBounds.Doubles z;
    @Nullable
    private final ResourceKey<Biome> biome;
    @Nullable
    private final ResourceKey<ConfiguredStructureFeature<?, ?>> feature;
    @Nullable
    private final ResourceKey<Level> dimension;
    @Nullable
    private final Boolean smokey;
    private final LightPredicate light;
    private final BlockPredicate block;
    private final FluidPredicate fluid;

    public LocationPredicate(MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z, @Nullable ResourceKey<Biome> biome, @Nullable ResourceKey<ConfiguredStructureFeature<?, ?>> feature, @Nullable ResourceKey<Level> dimension, @Nullable Boolean smokey, LightPredicate light, BlockPredicate block, FluidPredicate fluid) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.biome = biome;
        this.feature = feature;
        this.dimension = dimension;
        this.smokey = smokey;
        this.light = light;
        this.block = block;
        this.fluid = fluid;
    }

    public static LocationPredicate inBiome(ResourceKey<Biome> biome) {
        return new LocationPredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, biome, (ResourceKey<ConfiguredStructureFeature<?, ?>>)null, (ResourceKey<Level>)null, (Boolean)null, LightPredicate.ANY, BlockPredicate.ANY, FluidPredicate.ANY);
    }

    public static LocationPredicate inDimension(ResourceKey<Level> dimension) {
        return new LocationPredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, (ResourceKey<Biome>)null, (ResourceKey<ConfiguredStructureFeature<?, ?>>)null, dimension, (Boolean)null, LightPredicate.ANY, BlockPredicate.ANY, FluidPredicate.ANY);
    }

    public static LocationPredicate inFeature(ResourceKey<ConfiguredStructureFeature<?, ?>> feature) {
        return new LocationPredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, (ResourceKey<Biome>)null, feature, (ResourceKey<Level>)null, (Boolean)null, LightPredicate.ANY, BlockPredicate.ANY, FluidPredicate.ANY);
    }

    public static LocationPredicate atYLocation(MinMaxBounds.Doubles y) {
        return new LocationPredicate(MinMaxBounds.Doubles.ANY, y, MinMaxBounds.Doubles.ANY, (ResourceKey<Biome>)null, (ResourceKey<ConfiguredStructureFeature<?, ?>>)null, (ResourceKey<Level>)null, (Boolean)null, LightPredicate.ANY, BlockPredicate.ANY, FluidPredicate.ANY);
    }

    public boolean matches(ServerLevel world, double x, double y, double z) {
        if (!this.x.matches(x)) {
            return false;
        } else if (!this.y.matches(y)) {
            return false;
        } else if (!this.z.matches(z)) {
            return false;
        } else if (this.dimension != null && this.dimension != world.dimension()) {
            return false;
        } else {
            BlockPos blockPos = new BlockPos(x, y, z);
            boolean bl = world.isLoaded(blockPos);
            if (this.biome == null || bl && world.getBiome(blockPos).is(this.biome)) {
                if (this.feature == null || bl && world.structureFeatureManager().getStructureWithPieceAt(blockPos, this.feature).isValid()) {
                    if (this.smokey == null || bl && this.smokey == CampfireBlock.isSmokeyPos(world, blockPos)) {
                        if (!this.light.matches(world, blockPos)) {
                            return false;
                        } else if (!this.block.matches(world, blockPos)) {
                            return false;
                        } else {
                            return this.fluid.matches(world, blockPos);
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (!this.x.isAny() || !this.y.isAny() || !this.z.isAny()) {
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.add("x", this.x.serializeToJson());
                jsonObject2.add("y", this.y.serializeToJson());
                jsonObject2.add("z", this.z.serializeToJson());
                jsonObject.add("position", jsonObject2);
            }

            if (this.dimension != null) {
                Level.RESOURCE_KEY_CODEC.encodeStart(JsonOps.INSTANCE, this.dimension).resultOrPartial(LOGGER::error).ifPresent((jsonElement) -> {
                    jsonObject.add("dimension", jsonElement);
                });
            }

            if (this.feature != null) {
                jsonObject.addProperty("feature", this.feature.location().toString());
            }

            if (this.biome != null) {
                jsonObject.addProperty("biome", this.biome.location().toString());
            }

            if (this.smokey != null) {
                jsonObject.addProperty("smokey", this.smokey);
            }

            jsonObject.add("light", this.light.serializeToJson());
            jsonObject.add("block", this.block.serializeToJson());
            jsonObject.add("fluid", this.fluid.serializeToJson());
            return jsonObject;
        }
    }

    public static LocationPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "location");
            JsonObject jsonObject2 = GsonHelper.getAsJsonObject(jsonObject, "position", new JsonObject());
            MinMaxBounds.Doubles doubles = MinMaxBounds.Doubles.fromJson(jsonObject2.get("x"));
            MinMaxBounds.Doubles doubles2 = MinMaxBounds.Doubles.fromJson(jsonObject2.get("y"));
            MinMaxBounds.Doubles doubles3 = MinMaxBounds.Doubles.fromJson(jsonObject2.get("z"));
            ResourceKey<Level> resourceKey = jsonObject.has("dimension") ? ResourceLocation.CODEC.parse(JsonOps.INSTANCE, jsonObject.get("dimension")).resultOrPartial(LOGGER::error).map((resourceLocation) -> {
                return ResourceKey.create(Registry.DIMENSION_REGISTRY, resourceLocation);
            }).orElse((ResourceKey<Level>)null) : null;
            ResourceKey<ConfiguredStructureFeature<?, ?>> resourceKey2 = jsonObject.has("feature") ? ResourceLocation.CODEC.parse(JsonOps.INSTANCE, jsonObject.get("feature")).resultOrPartial(LOGGER::error).map((resourceLocation) -> {
                return ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, resourceLocation);
            }).orElse((ResourceKey<ConfiguredStructureFeature<?, ?>>)null) : null;
            ResourceKey<Biome> resourceKey3 = null;
            if (jsonObject.has("biome")) {
                ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "biome"));
                resourceKey3 = ResourceKey.create(Registry.BIOME_REGISTRY, resourceLocation);
            }

            Boolean boolean_ = jsonObject.has("smokey") ? jsonObject.get("smokey").getAsBoolean() : null;
            LightPredicate lightPredicate = LightPredicate.fromJson(jsonObject.get("light"));
            BlockPredicate blockPredicate = BlockPredicate.fromJson(jsonObject.get("block"));
            FluidPredicate fluidPredicate = FluidPredicate.fromJson(jsonObject.get("fluid"));
            return new LocationPredicate(doubles, doubles2, doubles3, resourceKey3, resourceKey2, resourceKey, boolean_, lightPredicate, blockPredicate, fluidPredicate);
        } else {
            return ANY;
        }
    }

    public static class Builder {
        private MinMaxBounds.Doubles x = MinMaxBounds.Doubles.ANY;
        private MinMaxBounds.Doubles y = MinMaxBounds.Doubles.ANY;
        private MinMaxBounds.Doubles z = MinMaxBounds.Doubles.ANY;
        @Nullable
        private ResourceKey<Biome> biome;
        @Nullable
        private ResourceKey<ConfiguredStructureFeature<?, ?>> feature;
        @Nullable
        private ResourceKey<Level> dimension;
        @Nullable
        private Boolean smokey;
        private LightPredicate light = LightPredicate.ANY;
        private BlockPredicate block = BlockPredicate.ANY;
        private FluidPredicate fluid = FluidPredicate.ANY;

        public static LocationPredicate.Builder location() {
            return new LocationPredicate.Builder();
        }

        public LocationPredicate.Builder setX(MinMaxBounds.Doubles x) {
            this.x = x;
            return this;
        }

        public LocationPredicate.Builder setY(MinMaxBounds.Doubles y) {
            this.y = y;
            return this;
        }

        public LocationPredicate.Builder setZ(MinMaxBounds.Doubles z) {
            this.z = z;
            return this;
        }

        public LocationPredicate.Builder setBiome(@Nullable ResourceKey<Biome> biome) {
            this.biome = biome;
            return this;
        }

        public LocationPredicate.Builder setFeature(@Nullable ResourceKey<ConfiguredStructureFeature<?, ?>> feature) {
            this.feature = feature;
            return this;
        }

        public LocationPredicate.Builder setDimension(@Nullable ResourceKey<Level> dimension) {
            this.dimension = dimension;
            return this;
        }

        public LocationPredicate.Builder setLight(LightPredicate light) {
            this.light = light;
            return this;
        }

        public LocationPredicate.Builder setBlock(BlockPredicate block) {
            this.block = block;
            return this;
        }

        public LocationPredicate.Builder setFluid(FluidPredicate fluid) {
            this.fluid = fluid;
            return this;
        }

        public LocationPredicate.Builder setSmokey(Boolean smokey) {
            this.smokey = smokey;
            return this;
        }

        public LocationPredicate build() {
            return new LocationPredicate(this.x, this.y, this.z, this.biome, this.feature, this.dimension, this.smokey, this.light, this.block, this.fluid);
        }
    }
}
