package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.timers.FunctionCallback;
import net.minecraft.world.level.timers.FunctionTagCallback;
import net.minecraft.world.level.timers.TimerQueue;

public class ScheduleCommand {

    private static final SimpleCommandExceptionType ERROR_SAME_TICK = new SimpleCommandExceptionType(new TranslatableComponent("commands.schedule.same_tick"));
    private static final DynamicCommandExceptionType ERROR_CANT_REMOVE = new DynamicCommandExceptionType((object) -> {
        return new TranslatableComponent("commands.schedule.cleared.failure", new Object[]{object});
    });
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_SCHEDULE = (commandcontext, suggestionsbuilder) -> {
        return SharedSuggestionProvider.suggest((Iterable) ((net.minecraft.commands.CommandSourceStack) commandcontext.getSource()).getLevel().serverLevelData.getScheduledEvents().getEventsIds(), suggestionsbuilder); // Paper
    };

    public ScheduleCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.Commands.literal("schedule").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.Commands.literal("function").then(net.minecraft.commands.Commands.argument("function", FunctionArgument.functions()).suggests(FunctionCommand.SUGGEST_FUNCTION).then(((RequiredArgumentBuilder) ((RequiredArgumentBuilder) net.minecraft.commands.Commands.argument("time", TimeArgument.time()).executes((commandcontext) -> {
            return ScheduleCommand.schedule((CommandSourceStack) commandcontext.getSource(), FunctionArgument.getFunctionOrTag(commandcontext, "function"), IntegerArgumentType.getInteger(commandcontext, "time"), true);
        })).then(net.minecraft.commands.Commands.literal("append").executes((commandcontext) -> {
            return ScheduleCommand.schedule((CommandSourceStack) commandcontext.getSource(), FunctionArgument.getFunctionOrTag(commandcontext, "function"), IntegerArgumentType.getInteger(commandcontext, "time"), false);
        }))).then(net.minecraft.commands.Commands.literal("replace").executes((commandcontext) -> {
            return ScheduleCommand.schedule((CommandSourceStack) commandcontext.getSource(), FunctionArgument.getFunctionOrTag(commandcontext, "function"), IntegerArgumentType.getInteger(commandcontext, "time"), true);
        })))))).then(net.minecraft.commands.Commands.literal("clear").then(net.minecraft.commands.Commands.argument("function", StringArgumentType.greedyString()).suggests(ScheduleCommand.SUGGEST_SCHEDULE).executes((commandcontext) -> {
            return ScheduleCommand.remove((CommandSourceStack) commandcontext.getSource(), StringArgumentType.getString(commandcontext, "function"));
        }))));
    }

    private static int schedule(CommandSourceStack source, Pair<ResourceLocation, Either<CommandFunction, Tag<CommandFunction>>> function, int time, boolean replace) throws CommandSyntaxException {
        if (time == 0) {
            throw ScheduleCommand.ERROR_SAME_TICK.create();
        } else {
            long j = source.getLevel().getGameTime() + (long) time;
            ResourceLocation minecraftkey = (ResourceLocation) function.getFirst();
            TimerQueue<MinecraftServer> customfunctioncallbacktimerqueue = source.getLevel().serverLevelData.overworldData().getScheduledEvents(); // CraftBukkit - SPIGOT-6667: Use world specific function timer

            ((Either) function.getSecond()).ifLeft((customfunction) -> {
                String s = minecraftkey.toString();

                if (replace) {
                    customfunctioncallbacktimerqueue.remove(s);
                }

                customfunctioncallbacktimerqueue.schedule(s, j, new FunctionCallback(minecraftkey));
                source.sendSuccess(new TranslatableComponent("commands.schedule.created.function", new Object[]{minecraftkey, time, j}), true);
            }).ifRight((tag) -> {
                String s = "#" + minecraftkey;

                if (replace) {
                    customfunctioncallbacktimerqueue.remove(s);
                }

                customfunctioncallbacktimerqueue.schedule(s, j, new FunctionTagCallback(minecraftkey));
                source.sendSuccess(new TranslatableComponent("commands.schedule.created.tag", new Object[]{minecraftkey, time, j}), true);
            });
            return Math.floorMod(j, Integer.MAX_VALUE);
        }
    }

    private static int remove(CommandSourceStack source, String eventName) throws CommandSyntaxException {
        int i = source.getLevel().serverLevelData.getScheduledEvents().remove(eventName); // Paper

        if (i == 0) {
            throw ScheduleCommand.ERROR_CANT_REMOVE.create(eventName);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.schedule.cleared.success", new Object[]{i, eventName}), true);
            return i;
        }
    }
}
