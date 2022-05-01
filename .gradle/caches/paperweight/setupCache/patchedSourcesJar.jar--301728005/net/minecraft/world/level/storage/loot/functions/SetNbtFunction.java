package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetNbtFunction extends LootItemConditionalFunction {
    final CompoundTag tag;

    SetNbtFunction(LootItemCondition[] conditions, CompoundTag nbt) {
        super(conditions);
        this.tag = nbt;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_NBT;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        stack.getOrCreateTag().merge(this.tag);
        return stack;
    }

    /** @deprecated */
    @Deprecated
    public static LootItemConditionalFunction.Builder<?> setTag(CompoundTag nbt) {
        return simpleBuilder((conditions) -> {
            return new SetNbtFunction(conditions, nbt);
        });
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetNbtFunction> {
        @Override
        public void serialize(JsonObject json, SetNbtFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("tag", object.tag.toString());
        }

        @Override
        public SetNbtFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            try {
                CompoundTag compoundTag = TagParser.parseTag(GsonHelper.getAsString(jsonObject, "tag"));
                return new SetNbtFunction(lootItemConditions, compoundTag);
            } catch (CommandSyntaxException var5) {
                throw new JsonSyntaxException(var5.getMessage());
            }
        }
    }
}
