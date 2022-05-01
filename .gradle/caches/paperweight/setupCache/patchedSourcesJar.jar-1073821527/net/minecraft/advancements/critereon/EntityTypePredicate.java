package net.minecraft.advancements.critereon;

import com.google.common.base.Joiner;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;

public abstract class EntityTypePredicate {
    public static final EntityTypePredicate ANY = new EntityTypePredicate() {
        @Override
        public boolean matches(EntityType<?> type) {
            return true;
        }

        @Override
        public JsonElement serializeToJson() {
            return JsonNull.INSTANCE;
        }
    };
    private static final Joiner COMMA_JOINER = Joiner.on(", ");

    public abstract boolean matches(EntityType<?> type);

    public abstract JsonElement serializeToJson();

    public static EntityTypePredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            String string = GsonHelper.convertToString(json, "type");
            if (string.startsWith("#")) {
                ResourceLocation resourceLocation = new ResourceLocation(string.substring(1));
                return new EntityTypePredicate.TagPredicate(TagKey.create(Registry.ENTITY_TYPE_REGISTRY, resourceLocation));
            } else {
                ResourceLocation resourceLocation2 = new ResourceLocation(string);
                EntityType<?> entityType = Registry.ENTITY_TYPE.getOptional(resourceLocation2).orElseThrow(() -> {
                    return new JsonSyntaxException("Unknown entity type '" + resourceLocation2 + "', valid types are: " + COMMA_JOINER.join(Registry.ENTITY_TYPE.keySet()));
                });
                return new EntityTypePredicate.TypePredicate(entityType);
            }
        } else {
            return ANY;
        }
    }

    public static EntityTypePredicate of(EntityType<?> type) {
        return new EntityTypePredicate.TypePredicate(type);
    }

    public static EntityTypePredicate of(TagKey<EntityType<?>> tag) {
        return new EntityTypePredicate.TagPredicate(tag);
    }

    static class TagPredicate extends EntityTypePredicate {
        private final TagKey<EntityType<?>> tag;

        public TagPredicate(TagKey<EntityType<?>> tag) {
            this.tag = tag;
        }

        @Override
        public boolean matches(EntityType<?> type) {
            return type.is(this.tag);
        }

        @Override
        public JsonElement serializeToJson() {
            return new JsonPrimitive("#" + this.tag.location());
        }
    }

    static class TypePredicate extends EntityTypePredicate {
        private final EntityType<?> type;

        public TypePredicate(EntityType<?> type) {
            this.type = type;
        }

        @Override
        public boolean matches(EntityType<?> type) {
            return this.type == type;
        }

        @Override
        public JsonElement serializeToJson() {
            return new JsonPrimitive(Registry.ENTITY_TYPE.getKey(this.type).toString());
        }
    }
}
