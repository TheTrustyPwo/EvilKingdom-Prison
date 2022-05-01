package net.minecraft.data.models.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class ModelTemplate {
    private final Optional<ResourceLocation> model;
    private final Set<TextureSlot> requiredSlots;
    private final Optional<String> suffix;

    public ModelTemplate(Optional<ResourceLocation> parent, Optional<String> variant, TextureSlot... requiredTextureKeys) {
        this.model = parent;
        this.suffix = variant;
        this.requiredSlots = ImmutableSet.copyOf(requiredTextureKeys);
    }

    public ResourceLocation create(Block block, TextureMapping textures, BiConsumer<ResourceLocation, Supplier<JsonElement>> modelCollector) {
        return this.create(ModelLocationUtils.getModelLocation(block, this.suffix.orElse("")), textures, modelCollector);
    }

    public ResourceLocation createWithSuffix(Block block, String suffix, TextureMapping textures, BiConsumer<ResourceLocation, Supplier<JsonElement>> modelCollector) {
        return this.create(ModelLocationUtils.getModelLocation(block, suffix + (String)this.suffix.orElse("")), textures, modelCollector);
    }

    public ResourceLocation createWithOverride(Block block, String suffix, TextureMapping textures, BiConsumer<ResourceLocation, Supplier<JsonElement>> modelCollector) {
        return this.create(ModelLocationUtils.getModelLocation(block, suffix), textures, modelCollector);
    }

    public ResourceLocation create(ResourceLocation id, TextureMapping textures, BiConsumer<ResourceLocation, Supplier<JsonElement>> modelCollector) {
        Map<TextureSlot, ResourceLocation> map = this.createMap(textures);
        modelCollector.accept(id, () -> {
            JsonObject jsonObject = new JsonObject();
            this.model.ifPresent((parentId) -> {
                jsonObject.addProperty("parent", parentId.toString());
            });
            if (!map.isEmpty()) {
                JsonObject jsonObject2 = new JsonObject();
                map.forEach((textureKey, textureId) -> {
                    jsonObject2.addProperty(textureKey.getId(), textureId.toString());
                });
                jsonObject.add("textures", jsonObject2);
            }

            return jsonObject;
        });
        return id;
    }

    private Map<TextureSlot, ResourceLocation> createMap(TextureMapping textures) {
        return Streams.concat(this.requiredSlots.stream(), textures.getForced()).collect(ImmutableMap.toImmutableMap(Function.identity(), textures::get));
    }
}
