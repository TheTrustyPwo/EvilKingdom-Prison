package net.minecraft.world.level.storage.loot.providers.score;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public class FixedScoreboardNameProvider implements ScoreboardNameProvider {
    final String name;

    FixedScoreboardNameProvider(String name) {
        this.name = name;
    }

    public static ScoreboardNameProvider forName(String name) {
        return new FixedScoreboardNameProvider(name);
    }

    @Override
    public LootScoreProviderType getType() {
        return ScoreboardNameProviders.FIXED;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    @Override
    public String getScoreboardName(LootContext context) {
        return this.name;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of();
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<FixedScoreboardNameProvider> {
        @Override
        public void serialize(JsonObject json, FixedScoreboardNameProvider object, JsonSerializationContext context) {
            json.addProperty("name", object.name);
        }

        @Override
        public FixedScoreboardNameProvider deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            String string = GsonHelper.getAsString(jsonObject, "name");
            return new FixedScoreboardNameProvider(string);
        }
    }
}
