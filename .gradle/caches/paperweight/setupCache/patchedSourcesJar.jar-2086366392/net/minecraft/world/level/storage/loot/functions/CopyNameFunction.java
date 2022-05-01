package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyNameFunction extends LootItemConditionalFunction {
    final CopyNameFunction.NameSource source;

    CopyNameFunction(LootItemCondition[] conditions, CopyNameFunction.NameSource source) {
        super(conditions);
        this.source = source;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.COPY_NAME;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(this.source.param);
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        Object object = context.getParamOrNull(this.source.param);
        if (object instanceof Nameable) {
            Nameable nameable = (Nameable)object;
            if (nameable.hasCustomName()) {
                stack.setHoverName(nameable.getDisplayName());
            }
        }

        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> copyName(CopyNameFunction.NameSource source) {
        return simpleBuilder((conditions) -> {
            return new CopyNameFunction(conditions, source);
        });
    }

    public static enum NameSource {
        THIS("this", LootContextParams.THIS_ENTITY),
        KILLER("killer", LootContextParams.KILLER_ENTITY),
        KILLER_PLAYER("killer_player", LootContextParams.LAST_DAMAGE_PLAYER),
        BLOCK_ENTITY("block_entity", LootContextParams.BLOCK_ENTITY);

        public final String name;
        public final LootContextParam<?> param;

        private NameSource(String name, LootContextParam<?> parameter) {
            this.name = name;
            this.param = parameter;
        }

        public static CopyNameFunction.NameSource getByName(String name) {
            for(CopyNameFunction.NameSource nameSource : values()) {
                if (nameSource.name.equals(name)) {
                    return nameSource;
                }
            }

            throw new IllegalArgumentException("Invalid name source " + name);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<CopyNameFunction> {
        @Override
        public void serialize(JsonObject json, CopyNameFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("source", object.source.name);
        }

        @Override
        public CopyNameFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            CopyNameFunction.NameSource nameSource = CopyNameFunction.NameSource.getByName(GsonHelper.getAsString(jsonObject, "source"));
            return new CopyNameFunction(lootItemConditions, nameSource);
        }
    }
}
