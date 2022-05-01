package net.minecraft.world.level.storage.loot.providers.nbt;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.nbt.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.GsonAdapterFactory;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ContextNbtProvider implements NbtProvider {
    private static final String BLOCK_ENTITY_ID = "block_entity";
    private static final ContextNbtProvider.Getter BLOCK_ENTITY_PROVIDER = new ContextNbtProvider.Getter() {
        @Override
        public Tag get(LootContext context) {
            BlockEntity blockEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
            return blockEntity != null ? blockEntity.saveWithFullMetadata() : null;
        }

        @Override
        public String getId() {
            return "block_entity";
        }

        @Override
        public Set<LootContextParam<?>> getReferencedContextParams() {
            return ImmutableSet.of(LootContextParams.BLOCK_ENTITY);
        }
    };
    public static final ContextNbtProvider BLOCK_ENTITY = new ContextNbtProvider(BLOCK_ENTITY_PROVIDER);
    final ContextNbtProvider.Getter getter;

    private static ContextNbtProvider.Getter forEntity(LootContext.EntityTarget entityTarget) {
        return new ContextNbtProvider.Getter() {
            @Nullable
            @Override
            public Tag get(LootContext context) {
                Entity entity = context.getParamOrNull(entityTarget.getParam());
                return entity != null ? NbtPredicate.getEntityTagToCompare(entity) : null;
            }

            @Override
            public String getId() {
                return entityTarget.name();
            }

            @Override
            public Set<LootContextParam<?>> getReferencedContextParams() {
                return ImmutableSet.of(entityTarget.getParam());
            }
        };
    }

    private ContextNbtProvider(ContextNbtProvider.Getter target) {
        this.getter = target;
    }

    @Override
    public LootNbtProviderType getType() {
        return NbtProviders.CONTEXT;
    }

    @Nullable
    @Override
    public Tag get(LootContext context) {
        return this.getter.get(context);
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.getter.getReferencedContextParams();
    }

    public static NbtProvider forContextEntity(LootContext.EntityTarget target) {
        return new ContextNbtProvider(forEntity(target));
    }

    static ContextNbtProvider createFromContext(String target) {
        if (target.equals("block_entity")) {
            return new ContextNbtProvider(BLOCK_ENTITY_PROVIDER);
        } else {
            LootContext.EntityTarget entityTarget = LootContext.EntityTarget.getByName(target);
            return new ContextNbtProvider(forEntity(entityTarget));
        }
    }

    interface Getter {
        @Nullable
        Tag get(LootContext context);

        String getId();

        Set<LootContextParam<?>> getReferencedContextParams();
    }

    public static class InlineSerializer implements GsonAdapterFactory.InlineSerializer<ContextNbtProvider> {
        @Override
        public JsonElement serialize(ContextNbtProvider object, JsonSerializationContext context) {
            return new JsonPrimitive(object.getter.getId());
        }

        @Override
        public ContextNbtProvider deserialize(JsonElement jsonElement, JsonDeserializationContext jsonDeserializationContext) {
            String string = jsonElement.getAsString();
            return ContextNbtProvider.createFromContext(string);
        }
    }

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ContextNbtProvider> {
        @Override
        public void serialize(JsonObject json, ContextNbtProvider object, JsonSerializationContext context) {
            json.addProperty("target", object.getter.getId());
        }

        @Override
        public ContextNbtProvider deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            String string = GsonHelper.getAsString(jsonObject, "target");
            return ContextNbtProvider.createFromContext(string);
        }
    }
}
