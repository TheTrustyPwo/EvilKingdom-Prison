package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetLoreFunction extends LootItemConditionalFunction {
    final boolean replace;
    final List<Component> lore;
    @Nullable
    final LootContext.EntityTarget resolutionContext;

    public SetLoreFunction(LootItemCondition[] conditions, boolean replace, List<Component> lore, @Nullable LootContext.EntityTarget entity) {
        super(conditions);
        this.replace = replace;
        this.lore = ImmutableList.copyOf(lore);
        this.resolutionContext = entity;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_LORE;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.resolutionContext != null ? ImmutableSet.of(this.resolutionContext.getParam()) : ImmutableSet.of();
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        ListTag listTag = this.getLoreTag(stack, !this.lore.isEmpty());
        if (listTag != null) {
            if (this.replace) {
                listTag.clear();
            }

            UnaryOperator<Component> unaryOperator = SetNameFunction.createResolver(context, this.resolutionContext);
            this.lore.stream().map(unaryOperator).map(Component.Serializer::toJson).map(StringTag::valueOf).forEach(listTag::add);
        }

        return stack;
    }

    @Nullable
    private ListTag getLoreTag(ItemStack stack, boolean otherLoreExists) {
        CompoundTag compoundTag;
        if (stack.hasTag()) {
            compoundTag = stack.getTag();
        } else {
            if (!otherLoreExists) {
                return null;
            }

            compoundTag = new CompoundTag();
            stack.setTag(compoundTag);
        }

        CompoundTag compoundTag4;
        if (compoundTag.contains("display", 10)) {
            compoundTag4 = compoundTag.getCompound("display");
        } else {
            if (!otherLoreExists) {
                return null;
            }

            compoundTag4 = new CompoundTag();
            compoundTag.put("display", compoundTag4);
        }

        if (compoundTag4.contains("Lore", 9)) {
            return compoundTag4.getList("Lore", 8);
        } else if (otherLoreExists) {
            ListTag listTag = new ListTag();
            compoundTag4.put("Lore", listTag);
            return listTag;
        } else {
            return null;
        }
    }

    public static SetLoreFunction.Builder setLore() {
        return new SetLoreFunction.Builder();
    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetLoreFunction.Builder> {
        private boolean replace;
        private LootContext.EntityTarget resolutionContext;
        private final List<Component> lore = Lists.newArrayList();

        public SetLoreFunction.Builder setReplace(boolean replace) {
            this.replace = replace;
            return this;
        }

        public SetLoreFunction.Builder setResolutionContext(LootContext.EntityTarget target) {
            this.resolutionContext = target;
            return this;
        }

        public SetLoreFunction.Builder addLine(Component lore) {
            this.lore.add(lore);
            return this;
        }

        @Override
        protected SetLoreFunction.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetLoreFunction(this.getConditions(), this.replace, this.lore, this.resolutionContext);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetLoreFunction> {
        @Override
        public void serialize(JsonObject json, SetLoreFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("replace", object.replace);
            JsonArray jsonArray = new JsonArray();

            for(Component component : object.lore) {
                jsonArray.add(Component.Serializer.toJsonTree(component));
            }

            json.add("lore", jsonArray);
            if (object.resolutionContext != null) {
                json.add("entity", context.serialize(object.resolutionContext));
            }

        }

        @Override
        public SetLoreFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            boolean bl = GsonHelper.getAsBoolean(jsonObject, "replace", false);
            List<Component> list = Streams.stream(GsonHelper.getAsJsonArray(jsonObject, "lore")).map(Component.Serializer::fromJson).collect(ImmutableList.toImmutableList());
            LootContext.EntityTarget entityTarget = GsonHelper.getAsObject(jsonObject, "entity", (LootContext.EntityTarget)null, jsonDeserializationContext, LootContext.EntityTarget.class);
            return new SetLoreFunction(lootItemConditions, bl, list, entityTarget);
        }
    }
}
