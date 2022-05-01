package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ConfiguredStructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ExplorationMapFunction extends LootItemConditionalFunction {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final TagKey<ConfiguredStructureFeature<?, ?>> DEFAULT_FEATURE = ConfiguredStructureTags.ON_TREASURE_MAPS;
    public static final String DEFAULT_DECORATION_NAME = "mansion";
    public static final MapDecoration.Type DEFAULT_DECORATION = MapDecoration.Type.MANSION;
    public static final byte DEFAULT_ZOOM = 2;
    public static final int DEFAULT_SEARCH_RADIUS = 50;
    public static final boolean DEFAULT_SKIP_EXISTING = true;
    final TagKey<ConfiguredStructureFeature<?, ?>> destination;
    final MapDecoration.Type mapDecoration;
    final byte zoom;
    final int searchRadius;
    final boolean skipKnownStructures;

    ExplorationMapFunction(LootItemCondition[] conditions, TagKey<ConfiguredStructureFeature<?, ?>> destination, MapDecoration.Type decoration, byte zoom, int searchRadius, boolean skipExistingChunks) {
        super(conditions);
        this.destination = destination;
        this.mapDecoration = decoration;
        this.zoom = zoom;
        this.searchRadius = searchRadius;
        this.skipKnownStructures = skipExistingChunks;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.EXPLORATION_MAP;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.ORIGIN);
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        if (!stack.is(Items.MAP)) {
            return stack;
        } else {
            Vec3 vec3 = context.getParamOrNull(LootContextParams.ORIGIN);
            if (vec3 != null) {
                ServerLevel serverLevel = context.getLevel();
                // Paper start
                if (!serverLevel.paperConfig.enableTreasureMaps) {
                    /*
                     * NOTE: I fear users will just get a plain map as their "treasure"
                     * This is preferable to disrespecting the config.
                     */
                    return stack;
                }
                // Paper end
                BlockPos blockPos = serverLevel.findNearestMapFeature(this.destination, new BlockPos(vec3), this.searchRadius, !serverLevel.paperConfig.treasureMapsAlreadyDiscovered && this.skipKnownStructures); // Paper
                if (blockPos != null) {
                    ItemStack itemStack = MapItem.create(serverLevel, blockPos.getX(), blockPos.getZ(), this.zoom, true, true);
                    MapItem.renderBiomePreviewMap(serverLevel, itemStack);
                    MapItemSavedData.addTargetDecoration(itemStack, blockPos, "+", this.mapDecoration);
                    return itemStack;
                }
            }

            return stack;
        }
    }

    public static ExplorationMapFunction.Builder makeExplorationMap() {
        return new ExplorationMapFunction.Builder();
    }

    public static class Builder extends LootItemConditionalFunction.Builder<ExplorationMapFunction.Builder> {
        private TagKey<ConfiguredStructureFeature<?, ?>> destination = ExplorationMapFunction.DEFAULT_FEATURE;
        private MapDecoration.Type mapDecoration = ExplorationMapFunction.DEFAULT_DECORATION;
        private byte zoom = 2;
        private int searchRadius = 50;
        private boolean skipKnownStructures = true;

        @Override
        protected ExplorationMapFunction.Builder getThis() {
            return this;
        }

        public ExplorationMapFunction.Builder setDestination(TagKey<ConfiguredStructureFeature<?, ?>> destination) {
            this.destination = destination;
            return this;
        }

        public ExplorationMapFunction.Builder setMapDecoration(MapDecoration.Type decoration) {
            this.mapDecoration = decoration;
            return this;
        }

        public ExplorationMapFunction.Builder setZoom(byte zoom) {
            this.zoom = zoom;
            return this;
        }

        public ExplorationMapFunction.Builder setSearchRadius(int searchRadius) {
            this.searchRadius = searchRadius;
            return this;
        }

        public ExplorationMapFunction.Builder setSkipKnownStructures(boolean skipExistingChunks) {
            this.skipKnownStructures = skipExistingChunks;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new ExplorationMapFunction(this.getConditions(), this.destination, this.mapDecoration, this.zoom, this.searchRadius, this.skipKnownStructures);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<ExplorationMapFunction> {
        @Override
        public void serialize(JsonObject json, ExplorationMapFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            if (!object.destination.equals(ExplorationMapFunction.DEFAULT_FEATURE)) {
                json.addProperty("destination", object.destination.location().toString());
            }

            if (object.mapDecoration != ExplorationMapFunction.DEFAULT_DECORATION) {
                json.add("decoration", context.serialize(object.mapDecoration.toString().toLowerCase(Locale.ROOT)));
            }

            if (object.zoom != 2) {
                json.addProperty("zoom", object.zoom);
            }

            if (object.searchRadius != 50) {
                json.addProperty("search_radius", object.searchRadius);
            }

            if (!object.skipKnownStructures) {
                json.addProperty("skip_existing_chunks", object.skipKnownStructures);
            }

        }

        @Override
        public ExplorationMapFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            TagKey<ConfiguredStructureFeature<?, ?>> tagKey = readStructure(jsonObject);
            String string = jsonObject.has("decoration") ? GsonHelper.getAsString(jsonObject, "decoration") : "mansion";
            MapDecoration.Type type = ExplorationMapFunction.DEFAULT_DECORATION;

            try {
                type = MapDecoration.Type.valueOf(string.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException var10) {
                ExplorationMapFunction.LOGGER.error("Error while parsing loot table decoration entry. Found {}. Defaulting to {}", string, ExplorationMapFunction.DEFAULT_DECORATION);
            }

            byte b = GsonHelper.getAsByte(jsonObject, "zoom", (byte)2);
            int i = GsonHelper.getAsInt(jsonObject, "search_radius", 50);
            boolean bl = GsonHelper.getAsBoolean(jsonObject, "skip_existing_chunks", true);
            return new ExplorationMapFunction(lootItemConditions, tagKey, type, b, i, bl);
        }

        private static TagKey<ConfiguredStructureFeature<?, ?>> readStructure(JsonObject json) {
            if (json.has("destination")) {
                String string = GsonHelper.getAsString(json, "destination");
                return TagKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, new ResourceLocation(string));
            } else {
                return ExplorationMapFunction.DEFAULT_FEATURE;
            }
        }
    }
}
