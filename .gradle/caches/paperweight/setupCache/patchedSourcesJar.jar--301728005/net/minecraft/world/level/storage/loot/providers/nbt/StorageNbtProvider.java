package net.minecraft.world.level.storage.loot.providers.nbt;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public class StorageNbtProvider implements NbtProvider {
    final ResourceLocation id;

    StorageNbtProvider(ResourceLocation source) {
        this.id = source;
    }

    @Override
    public LootNbtProviderType getType() {
        return NbtProviders.STORAGE;
    }

    @Nullable
    @Override
    public Tag get(LootContext context) {
        return context.getLevel().getServer().getCommandStorage().get(this.id);
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of();
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<StorageNbtProvider> {
        @Override
        public void serialize(JsonObject json, StorageNbtProvider object, JsonSerializationContext context) {
            json.addProperty("source", object.id.toString());
        }

        @Override
        public StorageNbtProvider deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            String string = GsonHelper.getAsString(jsonObject, "source");
            return new StorageNbtProvider(new ResourceLocation(string));
        }
    }
}
