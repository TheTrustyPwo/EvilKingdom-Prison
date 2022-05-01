package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ItemEnchantmentArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class EnchantCommand {
    private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType((entityName) -> {
        return new TranslatableComponent("commands.enchant.failed.entity", entityName);
    });
    private static final DynamicCommandExceptionType ERROR_NO_ITEM = new DynamicCommandExceptionType((entityName) -> {
        return new TranslatableComponent("commands.enchant.failed.itemless", entityName);
    });
    private static final DynamicCommandExceptionType ERROR_INCOMPATIBLE = new DynamicCommandExceptionType((itemName) -> {
        return new TranslatableComponent("commands.enchant.failed.incompatible", itemName);
    });
    private static final Dynamic2CommandExceptionType ERROR_LEVEL_TOO_HIGH = new Dynamic2CommandExceptionType((level, maxLevel) -> {
        return new TranslatableComponent("commands.enchant.failed.level", level, maxLevel);
    });
    private static final SimpleCommandExceptionType ERROR_NOTHING_HAPPENED = new SimpleCommandExceptionType(new TranslatableComponent("commands.enchant.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("enchant").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("enchantment", ItemEnchantmentArgument.enchantment()).executes((context) -> {
            return enchant(context.getSource(), EntityArgument.getEntities(context, "targets"), ItemEnchantmentArgument.getEnchantment(context, "enchantment"), 1);
        }).then(Commands.argument("level", IntegerArgumentType.integer(0)).executes((context) -> {
            return enchant(context.getSource(), EntityArgument.getEntities(context, "targets"), ItemEnchantmentArgument.getEnchantment(context, "enchantment"), IntegerArgumentType.getInteger(context, "level"));
        })))));
    }

    private static int enchant(CommandSourceStack source, Collection<? extends Entity> targets, Enchantment enchantment, int level) throws CommandSyntaxException {
        if (level > enchantment.getMaxLevel()) {
            throw ERROR_LEVEL_TOO_HIGH.create(level, enchantment.getMaxLevel());
        } else {
            int i = 0;

            for(Entity entity : targets) {
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity)entity;
                    ItemStack itemStack = livingEntity.getMainHandItem();
                    if (!itemStack.isEmpty()) {
                        if (enchantment.canEnchant(itemStack) && EnchantmentHelper.isEnchantmentCompatible(EnchantmentHelper.getEnchantments(itemStack).keySet(), enchantment)) {
                            itemStack.enchant(enchantment, level);
                            ++i;
                        } else if (targets.size() == 1) {
                            throw ERROR_INCOMPATIBLE.create(itemStack.getItem().getName(itemStack).getString());
                        }
                    } else if (targets.size() == 1) {
                        throw ERROR_NO_ITEM.create(livingEntity.getName().getString());
                    }
                } else if (targets.size() == 1) {
                    throw ERROR_NOT_LIVING_ENTITY.create(entity.getName().getString());
                }
            }

            if (i == 0) {
                throw ERROR_NOTHING_HAPPENED.create();
            } else {
                if (targets.size() == 1) {
                    source.sendSuccess(new TranslatableComponent("commands.enchant.success.single", enchantment.getFullname(level), targets.iterator().next().getDisplayName()), true);
                } else {
                    source.sendSuccess(new TranslatableComponent("commands.enchant.success.multiple", enchantment.getFullname(level), targets.size()), true);
                }

                return i;
            }
        }
    }
}
