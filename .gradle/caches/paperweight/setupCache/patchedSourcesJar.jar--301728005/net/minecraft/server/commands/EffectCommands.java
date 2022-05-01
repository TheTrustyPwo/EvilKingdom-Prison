package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MobEffectArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EffectCommands {

    private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.effect.give.failed"));
    private static final SimpleCommandExceptionType ERROR_CLEAR_EVERYTHING_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.effect.clear.everything.failed"));
    private static final SimpleCommandExceptionType ERROR_CLEAR_SPECIFIC_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.effect.clear.specific.failed"));

    public EffectCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.Commands.literal("effect").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(((LiteralArgumentBuilder) net.minecraft.commands.Commands.literal("clear").executes((commandcontext) -> {
            return EffectCommands.clearEffects((CommandSourceStack) commandcontext.getSource(), ImmutableList.of(((CommandSourceStack) commandcontext.getSource()).getEntityOrException()));
        })).then(((RequiredArgumentBuilder) net.minecraft.commands.Commands.argument("targets", EntityArgument.entities()).executes((commandcontext) -> {
            return EffectCommands.clearEffects((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"));
        })).then(net.minecraft.commands.Commands.argument("effect", MobEffectArgument.effect()).executes((commandcontext) -> {
            return EffectCommands.clearEffect((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), MobEffectArgument.getEffect(commandcontext, "effect"));
        }))))).then(net.minecraft.commands.Commands.literal("give").then(net.minecraft.commands.Commands.argument("targets", EntityArgument.entities()).then(((RequiredArgumentBuilder) net.minecraft.commands.Commands.argument("effect", MobEffectArgument.effect()).executes((commandcontext) -> {
            return EffectCommands.giveEffect((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), MobEffectArgument.getEffect(commandcontext, "effect"), (Integer) null, 0, true);
        })).then(((RequiredArgumentBuilder) net.minecraft.commands.Commands.argument("seconds", IntegerArgumentType.integer(1, 1000000)).executes((commandcontext) -> {
            return EffectCommands.giveEffect((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), MobEffectArgument.getEffect(commandcontext, "effect"), IntegerArgumentType.getInteger(commandcontext, "seconds"), 0, true);
        })).then(((RequiredArgumentBuilder) net.minecraft.commands.Commands.argument("amplifier", IntegerArgumentType.integer(0, 255)).executes((commandcontext) -> {
            return EffectCommands.giveEffect((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), MobEffectArgument.getEffect(commandcontext, "effect"), IntegerArgumentType.getInteger(commandcontext, "seconds"), IntegerArgumentType.getInteger(commandcontext, "amplifier"), true);
        })).then(net.minecraft.commands.Commands.argument("hideParticles", BoolArgumentType.bool()).executes((commandcontext) -> {
            return EffectCommands.giveEffect((CommandSourceStack) commandcontext.getSource(), EntityArgument.getEntities(commandcontext, "targets"), MobEffectArgument.getEffect(commandcontext, "effect"), IntegerArgumentType.getInteger(commandcontext, "seconds"), IntegerArgumentType.getInteger(commandcontext, "amplifier"), !BoolArgumentType.getBool(commandcontext, "hideParticles"));
        }))))))));
    }

    private static int giveEffect(CommandSourceStack source, Collection<? extends Entity> targets, MobEffect effect, @Nullable Integer seconds, int amplifier, boolean showParticles) throws CommandSyntaxException {
        int j = 0;
        int k;

        if (seconds != null) {
            if (effect.isInstantenous()) {
                k = seconds;
            } else {
                k = seconds * 20;
            }
        } else if (effect.isInstantenous()) {
            k = 1;
        } else {
            k = 600;
        }

        Iterator iterator = targets.iterator();

        while (iterator.hasNext()) {
            Entity entity = (Entity) iterator.next();

            if (entity instanceof LivingEntity) {
                MobEffectInstance mobeffect = new MobEffectInstance(effect, k, amplifier, false, showParticles);

                if (((LivingEntity) entity).addEffect(mobeffect, source.getEntity(), org.bukkit.event.entity.EntityPotionEffectEvent.Cause.COMMAND)) { // CraftBukkit
                    ++j;
                }
            }
        }

        if (j == 0) {
            throw EffectCommands.ERROR_GIVE_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendSuccess(new TranslatableComponent("commands.effect.give.success.single", new Object[]{effect.getDisplayName(), ((Entity) targets.iterator().next()).getDisplayName(), k / 20}), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.effect.give.success.multiple", new Object[]{effect.getDisplayName(), targets.size(), k / 20}), true);
            }

            return j;
        }
    }

    private static int clearEffects(CommandSourceStack source, Collection<? extends Entity> targets) throws CommandSyntaxException {
        int i = 0;
        Iterator iterator = targets.iterator();

        while (iterator.hasNext()) {
            Entity entity = (Entity) iterator.next();

            if (entity instanceof LivingEntity && ((LivingEntity) entity).removeAllEffects(org.bukkit.event.entity.EntityPotionEffectEvent.Cause.COMMAND)) { // CraftBukkit
                ++i;
            }
        }

        if (i == 0) {
            throw EffectCommands.ERROR_CLEAR_EVERYTHING_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendSuccess(new TranslatableComponent("commands.effect.clear.everything.success.single", new Object[]{((Entity) targets.iterator().next()).getDisplayName()}), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.effect.clear.everything.success.multiple", new Object[]{targets.size()}), true);
            }

            return i;
        }
    }

    private static int clearEffect(CommandSourceStack source, Collection<? extends Entity> targets, MobEffect effect) throws CommandSyntaxException {
        int i = 0;
        Iterator iterator = targets.iterator();

        while (iterator.hasNext()) {
            Entity entity = (Entity) iterator.next();

            if (entity instanceof LivingEntity && ((LivingEntity) entity).removeEffect(effect, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.COMMAND)) { // CraftBukkit
                ++i;
            }
        }

        if (i == 0) {
            throw EffectCommands.ERROR_CLEAR_SPECIFIC_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendSuccess(new TranslatableComponent("commands.effect.clear.specific.success.single", new Object[]{effect.getDisplayName(), ((Entity) targets.iterator().next()).getDisplayName()}), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.effect.clear.specific.success.multiple", new Object[]{effect.getDisplayName(), targets.size()}), true);
            }

            return i;
        }
    }
}
