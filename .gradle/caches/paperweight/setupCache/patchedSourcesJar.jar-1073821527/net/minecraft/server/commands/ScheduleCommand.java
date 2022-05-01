package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
    private static final DynamicCommandExceptionType ERROR_CANT_REMOVE = new DynamicCommandExceptionType((eventName) -> {
        return new TranslatableComponent("commands.schedule.cleared.failure", eventName);
    });
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_SCHEDULE = (context, builder) -> {
        return SharedSuggestionProvider.suggest(context.getSource().getServer().getWorldData().overworldData().getScheduledEvents().getEventsIds(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("schedule").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("function").then(Commands.argument("function", FunctionArgument.functions()).suggests(FunctionCommand.SUGGEST_FUNCTION).then(Commands.argument("time", TimeArgument.time()).executes((context) -> {
            return schedule(context.getSource(), FunctionArgument.getFunctionOrTag(context, "function"), IntegerArgumentType.getInteger(context, "time"), true);
        }).then(Commands.literal("append").executes((context) -> {
            return schedule(context.getSource(), FunctionArgument.getFunctionOrTag(context, "function"), IntegerArgumentType.getInteger(context, "time"), false);
        })).then(Commands.literal("replace").executes((context) -> {
            return schedule(context.getSource(), FunctionArgument.getFunctionOrTag(context, "function"), IntegerArgumentType.getInteger(context, "time"), true);
        }))))).then(Commands.literal("clear").then(Commands.argument("function", StringArgumentType.greedyString()).suggests(SUGGEST_SCHEDULE).executes((context) -> {
            return remove(context.getSource(), StringArgumentType.getString(context, "function"));
        }))));
    }

    private static int schedule(CommandSourceStack source, Pair<ResourceLocation, Either<CommandFunction, Tag<CommandFunction>>> function, int time, boolean replace) throws CommandSyntaxException {
        if (time == 0) {
            throw ERROR_SAME_TICK.create();
        } else {
            long l = source.getLevel().getGameTime() + (long)time;
            ResourceLocation resourceLocation = function.getFirst();
            TimerQueue<MinecraftServer> timerQueue = source.getServer().getWorldData().overworldData().getScheduledEvents();
            function.getSecond().ifLeft((functionx) -> {
                String string = resourceLocation.toString();
                if (replace) {
                    timerQueue.remove(string);
                }

                timerQueue.schedule(string, l, new FunctionCallback(resourceLocation));
                source.sendSuccess(new TranslatableComponent("commands.schedule.created.function", resourceLocation, time, l), true);
            }).ifRight((tag) -> {
                String string = "#" + resourceLocation;
                if (replace) {
                    timerQueue.remove(string);
                }

                timerQueue.schedule(string, l, new FunctionTagCallback(resourceLocation));
                source.sendSuccess(new TranslatableComponent("commands.schedule.created.tag", resourceLocation, time, l), true);
            });
            return Math.floorMod(l, Integer.MAX_VALUE);
        }
    }

    private static int remove(CommandSourceStack source, String eventName) throws CommandSyntaxException {
        int i = source.getServer().getWorldData().overworldData().getScheduledEvents().remove(eventName);
        if (i == 0) {
            throw ERROR_CANT_REMOVE.create(eventName);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.schedule.cleared.success", i, eventName), true);
            return i;
        }
    }
}
