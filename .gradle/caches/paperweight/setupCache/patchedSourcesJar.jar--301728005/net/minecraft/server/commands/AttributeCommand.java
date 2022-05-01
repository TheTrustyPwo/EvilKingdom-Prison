package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeCommand {
    private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType((name) -> {
        return new TranslatableComponent("commands.attribute.failed.entity", name);
    });
    private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType((entityName, attributeName) -> {
        return new TranslatableComponent("commands.attribute.failed.no_attribute", entityName, attributeName);
    });
    private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType((entityName, attributeName, uuid) -> {
        return new TranslatableComponent("commands.attribute.failed.no_modifier", attributeName, entityName, uuid);
    });
    private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType((entityName, attributeName, uuid) -> {
        return new TranslatableComponent("commands.attribute.failed.modifier_already_present", uuid, attributeName, entityName);
    });

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("attribute").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("target", EntityArgument.entity()).then(Commands.argument("attribute", ResourceKeyArgument.key(Registry.ATTRIBUTE_REGISTRY)).then(Commands.literal("get").executes((context) -> {
            return getAttributeValue(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), 1.0D);
        }).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes((context) -> {
            return getAttributeValue(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), DoubleArgumentType.getDouble(context, "scale"));
        }))).then(Commands.literal("base").then(Commands.literal("set").then(Commands.argument("value", DoubleArgumentType.doubleArg()).executes((context) -> {
            return setAttributeBase(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), DoubleArgumentType.getDouble(context, "value"));
        }))).then(Commands.literal("get").executes((context) -> {
            return getAttributeBase(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), 1.0D);
        }).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes((context) -> {
            return getAttributeBase(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), DoubleArgumentType.getDouble(context, "scale"));
        })))).then(Commands.literal("modifier").then(Commands.literal("add").then(Commands.argument("uuid", UuidArgument.uuid()).then(Commands.argument("name", StringArgumentType.string()).then(Commands.argument("value", DoubleArgumentType.doubleArg()).then(Commands.literal("add").executes((context) -> {
            return addModifier(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), UuidArgument.getUuid(context, "uuid"), StringArgumentType.getString(context, "name"), DoubleArgumentType.getDouble(context, "value"), AttributeModifier.Operation.ADDITION);
        })).then(Commands.literal("multiply").executes((context) -> {
            return addModifier(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), UuidArgument.getUuid(context, "uuid"), StringArgumentType.getString(context, "name"), DoubleArgumentType.getDouble(context, "value"), AttributeModifier.Operation.MULTIPLY_TOTAL);
        })).then(Commands.literal("multiply_base").executes((context) -> {
            return addModifier(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), UuidArgument.getUuid(context, "uuid"), StringArgumentType.getString(context, "name"), DoubleArgumentType.getDouble(context, "value"), AttributeModifier.Operation.MULTIPLY_BASE);
        })))))).then(Commands.literal("remove").then(Commands.argument("uuid", UuidArgument.uuid()).executes((context) -> {
            return removeModifier(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), UuidArgument.getUuid(context, "uuid"));
        }))).then(Commands.literal("value").then(Commands.literal("get").then(Commands.argument("uuid", UuidArgument.uuid()).executes((context) -> {
            return getAttributeModifier(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), UuidArgument.getUuid(context, "uuid"), 1.0D);
        }).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes((context) -> {
            return getAttributeModifier(context.getSource(), EntityArgument.getEntity(context, "target"), ResourceKeyArgument.getAttribute(context, "attribute"), UuidArgument.getUuid(context, "uuid"), DoubleArgumentType.getDouble(context, "scale"));
        })))))))));
    }

    private static AttributeInstance getAttributeInstance(Entity entity, Attribute attribute) throws CommandSyntaxException {
        AttributeInstance attributeInstance = getLivingEntity(entity).getAttributes().getInstance(attribute);
        if (attributeInstance == null) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create(entity.getName(), new TranslatableComponent(attribute.getDescriptionId()));
        } else {
            return attributeInstance;
        }
    }

    private static LivingEntity getLivingEntity(Entity entity) throws CommandSyntaxException {
        if (!(entity instanceof LivingEntity)) {
            throw ERROR_NOT_LIVING_ENTITY.create(entity.getName());
        } else {
            return (LivingEntity)entity;
        }
    }

    private static LivingEntity getEntityWithAttribute(Entity entity, Attribute attribute) throws CommandSyntaxException {
        LivingEntity livingEntity = getLivingEntity(entity);
        if (!livingEntity.getAttributes().hasAttribute(attribute)) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create(entity.getName(), new TranslatableComponent(attribute.getDescriptionId()));
        } else {
            return livingEntity;
        }
    }

    private static int getAttributeValue(CommandSourceStack source, Entity target, Attribute attribute, double multiplier) throws CommandSyntaxException {
        LivingEntity livingEntity = getEntityWithAttribute(target, attribute);
        double d = livingEntity.getAttributeValue(attribute);
        source.sendSuccess(new TranslatableComponent("commands.attribute.value.get.success", new TranslatableComponent(attribute.getDescriptionId()), target.getName(), d), false);
        return (int)(d * multiplier);
    }

    private static int getAttributeBase(CommandSourceStack source, Entity target, Attribute attribute, double multiplier) throws CommandSyntaxException {
        LivingEntity livingEntity = getEntityWithAttribute(target, attribute);
        double d = livingEntity.getAttributeBaseValue(attribute);
        source.sendSuccess(new TranslatableComponent("commands.attribute.base_value.get.success", new TranslatableComponent(attribute.getDescriptionId()), target.getName(), d), false);
        return (int)(d * multiplier);
    }

    private static int getAttributeModifier(CommandSourceStack source, Entity target, Attribute attribute, UUID uuid, double multiplier) throws CommandSyntaxException {
        LivingEntity livingEntity = getEntityWithAttribute(target, attribute);
        AttributeMap attributeMap = livingEntity.getAttributes();
        if (!attributeMap.hasModifier(attribute, uuid)) {
            throw ERROR_NO_SUCH_MODIFIER.create(target.getName(), new TranslatableComponent(attribute.getDescriptionId()), uuid);
        } else {
            double d = attributeMap.getModifierValue(attribute, uuid);
            source.sendSuccess(new TranslatableComponent("commands.attribute.modifier.value.get.success", uuid, new TranslatableComponent(attribute.getDescriptionId()), target.getName(), d), false);
            return (int)(d * multiplier);
        }
    }

    private static int setAttributeBase(CommandSourceStack source, Entity target, Attribute attribute, double value) throws CommandSyntaxException {
        getAttributeInstance(target, attribute).setBaseValue(value);
        source.sendSuccess(new TranslatableComponent("commands.attribute.base_value.set.success", new TranslatableComponent(attribute.getDescriptionId()), target.getName(), value), false);
        return 1;
    }

    private static int addModifier(CommandSourceStack source, Entity target, Attribute attribute, UUID uuid, String name, double value, AttributeModifier.Operation operation) throws CommandSyntaxException {
        AttributeInstance attributeInstance = getAttributeInstance(target, attribute);
        AttributeModifier attributeModifier = new AttributeModifier(uuid, name, value, operation);
        if (attributeInstance.hasModifier(attributeModifier)) {
            throw ERROR_MODIFIER_ALREADY_PRESENT.create(target.getName(), new TranslatableComponent(attribute.getDescriptionId()), uuid);
        } else {
            attributeInstance.addPermanentModifier(attributeModifier);
            source.sendSuccess(new TranslatableComponent("commands.attribute.modifier.add.success", uuid, new TranslatableComponent(attribute.getDescriptionId()), target.getName()), false);
            return 1;
        }
    }

    private static int removeModifier(CommandSourceStack source, Entity target, Attribute attribute, UUID uuid) throws CommandSyntaxException {
        AttributeInstance attributeInstance = getAttributeInstance(target, attribute);
        if (attributeInstance.removePermanentModifier(uuid)) {
            source.sendSuccess(new TranslatableComponent("commands.attribute.modifier.remove.success", uuid, new TranslatableComponent(attribute.getDescriptionId()), target.getName()), false);
            return 1;
        } else {
            throw ERROR_NO_SUCH_MODIFIER.create(target.getName(), new TranslatableComponent(attribute.getDescriptionId()), uuid);
        }
    }
}
