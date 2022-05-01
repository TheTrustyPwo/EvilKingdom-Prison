package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class SetNameFunction extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    final Component name;
    @Nullable
    final LootContext.EntityTarget resolutionContext;

    SetNameFunction(LootItemCondition[] conditions, @Nullable Component name, @Nullable LootContext.EntityTarget entity) {
        super(conditions);
        this.name = name;
        this.resolutionContext = entity;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_NAME;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.resolutionContext != null ? ImmutableSet.of(this.resolutionContext.getParam()) : ImmutableSet.of();
    }

    public static UnaryOperator<Component> createResolver(LootContext context, @Nullable LootContext.EntityTarget sourceEntity) {
        if (sourceEntity != null) {
            Entity entity = context.getParamOrNull(sourceEntity.getParam());
            if (entity != null) {
                CommandSourceStack commandSourceStack = entity.createCommandSourceStack().withPermission(2);
                return (textComponent) -> {
                    try {
                        return ComponentUtils.updateForEntity(commandSourceStack, textComponent, entity, 0);
                    } catch (CommandSyntaxException var4) {
                        LOGGER.warn("Failed to resolve text component", (Throwable)var4);
                        return textComponent;
                    }
                };
            }
        }

        return (textComponent) -> {
            return textComponent;
        };
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        if (this.name != null) {
            stack.setHoverName(createResolver(context, this.resolutionContext).apply(this.name));
        }

        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> setName(Component name) {
        return simpleBuilder((conditions) -> {
            return new SetNameFunction(conditions, name, (LootContext.EntityTarget)null);
        });
    }

    public static LootItemConditionalFunction.Builder<?> setName(Component name, LootContext.EntityTarget target) {
        return simpleBuilder((conditions) -> {
            return new SetNameFunction(conditions, name, target);
        });
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetNameFunction> {
        @Override
        public void serialize(JsonObject json, SetNameFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            if (object.name != null) {
                json.add("name", Component.Serializer.toJsonTree(object.name));
            }

            if (object.resolutionContext != null) {
                json.add("entity", context.serialize(object.resolutionContext));
            }

        }

        @Override
        public SetNameFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            Component component = Component.Serializer.fromJson(jsonObject.get("name"));
            LootContext.EntityTarget entityTarget = GsonHelper.getAsObject(jsonObject, "entity", (LootContext.EntityTarget)null, jsonDeserializationContext, LootContext.EntityTarget.class);
            return new SetNameFunction(lootItemConditions, component, entityTarget);
        }
    }
}
