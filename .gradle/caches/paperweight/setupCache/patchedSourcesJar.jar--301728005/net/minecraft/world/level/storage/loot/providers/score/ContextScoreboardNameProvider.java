package net.minecraft.world.level.storage.loot.providers.score;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.GsonAdapterFactory;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public class ContextScoreboardNameProvider implements ScoreboardNameProvider {
    final LootContext.EntityTarget target;

    ContextScoreboardNameProvider(LootContext.EntityTarget target) {
        this.target = target;
    }

    public static ScoreboardNameProvider forTarget(LootContext.EntityTarget target) {
        return new ContextScoreboardNameProvider(target);
    }

    @Override
    public LootScoreProviderType getType() {
        return ScoreboardNameProviders.CONTEXT;
    }

    @Nullable
    @Override
    public String getScoreboardName(LootContext context) {
        Entity entity = context.getParamOrNull(this.target.getParam());
        return entity != null ? entity.getScoreboardName() : null;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(this.target.getParam());
    }

    public static class InlineSerializer implements GsonAdapterFactory.InlineSerializer<ContextScoreboardNameProvider> {
        @Override
        public JsonElement serialize(ContextScoreboardNameProvider object, JsonSerializationContext context) {
            return context.serialize(object.target);
        }

        @Override
        public ContextScoreboardNameProvider deserialize(JsonElement jsonElement, JsonDeserializationContext jsonDeserializationContext) {
            LootContext.EntityTarget entityTarget = jsonDeserializationContext.deserialize(jsonElement, LootContext.EntityTarget.class);
            return new ContextScoreboardNameProvider(entityTarget);
        }
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ContextScoreboardNameProvider> {
        @Override
        public void serialize(JsonObject json, ContextScoreboardNameProvider object, JsonSerializationContext context) {
            json.addProperty("target", object.target.name());
        }

        @Override
        public ContextScoreboardNameProvider deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LootContext.EntityTarget entityTarget = GsonHelper.getAsObject(jsonObject, "target", jsonDeserializationContext, LootContext.EntityTarget.class);
            return new ContextScoreboardNameProvider(entityTarget);
        }
    }
}
