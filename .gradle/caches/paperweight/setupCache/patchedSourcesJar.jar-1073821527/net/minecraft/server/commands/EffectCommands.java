package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("effect").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("clear").executes((context) -> {
            return clearEffects(context.getSource(), ImmutableList.of(context.getSource().getEntityOrException()));
        }).then(Commands.argument("targets", EntityArgument.entities()).executes((context) -> {
            return clearEffects(context.getSource(), EntityArgument.getEntities(context, "targets"));
        }).then(Commands.argument("effect", MobEffectArgument.effect()).executes((context) -> {
            return clearEffect(context.getSource(), EntityArgument.getEntities(context, "targets"), MobEffectArgument.getEffect(context, "effect"));
        })))).then(Commands.literal("give").then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("effect", MobEffectArgument.effect()).executes((context) -> {
            return giveEffect(context.getSource(), EntityArgument.getEntities(context, "targets"), MobEffectArgument.getEffect(context, "effect"), (Integer)null, 0, true);
        }).then(Commands.argument("seconds", IntegerArgumentType.integer(1, 1000000)).executes((context) -> {
            return giveEffect(context.getSource(), EntityArgument.getEntities(context, "targets"), MobEffectArgument.getEffect(context, "effect"), IntegerArgumentType.getInteger(context, "seconds"), 0, true);
        }).then(Commands.argument("amplifier", IntegerArgumentType.integer(0, 255)).executes((context) -> {
            return giveEffect(context.getSource(), EntityArgument.getEntities(context, "targets"), MobEffectArgument.getEffect(context, "effect"), IntegerArgumentType.getInteger(context, "seconds"), IntegerArgumentType.getInteger(context, "amplifier"), true);
        }).then(Commands.argument("hideParticles", BoolArgumentType.bool()).executes((context) -> {
            return giveEffect(context.getSource(), EntityArgument.getEntities(context, "targets"), MobEffectArgument.getEffect(context, "effect"), IntegerArgumentType.getInteger(context, "seconds"), IntegerArgumentType.getInteger(context, "amplifier"), !BoolArgumentType.getBool(context, "hideParticles"));
        }))))))));
    }

    private static int giveEffect(CommandSourceStack source, Collection<? extends Entity> targets, MobEffect effect, @Nullable Integer seconds, int amplifier, boolean showParticles) throws CommandSyntaxException {
        int i = 0;
        int j;
        if (seconds != null) {
            if (effect.isInstantenous()) {
                j = seconds;
            } else {
                j = seconds * 20;
            }
        } else if (effect.isInstantenous()) {
            j = 1;
        } else {
            j = 600;
        }

        for(Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                MobEffectInstance mobEffectInstance = new MobEffectInstance(effect, j, amplifier, false, showParticles);
                if (((LivingEntity)entity).addEffect(mobEffectInstance, source.getEntity())) {
                    ++i;
                }
            }
        }

        if (i == 0) {
            throw ERROR_GIVE_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendSuccess(new TranslatableComponent("commands.effect.give.success.single", effect.getDisplayName(), targets.iterator().next().getDisplayName(), j / 20), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.effect.give.success.multiple", effect.getDisplayName(), targets.size(), j / 20), true);
            }

            return i;
        }
    }

    private static int clearEffects(CommandSourceStack source, Collection<? extends Entity> targets) throws CommandSyntaxException {
        int i = 0;

        for(Entity entity : targets) {
            if (entity instanceof LivingEntity && ((LivingEntity)entity).removeAllEffects()) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_CLEAR_EVERYTHING_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendSuccess(new TranslatableComponent("commands.effect.clear.everything.success.single", targets.iterator().next().getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.effect.clear.everything.success.multiple", targets.size()), true);
            }

            return i;
        }
    }

    private static int clearEffect(CommandSourceStack source, Collection<? extends Entity> targets, MobEffect effect) throws CommandSyntaxException {
        int i = 0;

        for(Entity entity : targets) {
            if (entity instanceof LivingEntity && ((LivingEntity)entity).removeEffect(effect)) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_CLEAR_SPECIFIC_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendSuccess(new TranslatableComponent("commands.effect.clear.specific.success.single", effect.getDisplayName(), targets.iterator().next().getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.effect.clear.specific.success.multiple", effect.getDisplayName(), targets.size()), true);
            }

            return i;
        }
    }
}
