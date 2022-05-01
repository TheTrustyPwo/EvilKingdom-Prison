package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;

public class WeatherCommand {
    private static final int DEFAULT_TIME = 6000;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("weather").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("clear").executes((context) -> {
            return setClear(context.getSource(), 6000);
        }).then(Commands.argument("duration", IntegerArgumentType.integer(0, 1000000)).executes((context) -> {
            return setClear(context.getSource(), IntegerArgumentType.getInteger(context, "duration") * 20);
        }))).then(Commands.literal("rain").executes((context) -> {
            return setRain(context.getSource(), 6000);
        }).then(Commands.argument("duration", IntegerArgumentType.integer(0, 1000000)).executes((context) -> {
            return setRain(context.getSource(), IntegerArgumentType.getInteger(context, "duration") * 20);
        }))).then(Commands.literal("thunder").executes((context) -> {
            return setThunder(context.getSource(), 6000);
        }).then(Commands.argument("duration", IntegerArgumentType.integer(0, 1000000)).executes((context) -> {
            return setThunder(context.getSource(), IntegerArgumentType.getInteger(context, "duration") * 20);
        }))));
    }

    private static int setClear(CommandSourceStack source, int duration) {
        source.getLevel().setWeatherParameters(duration, 0, false, false);
        source.sendSuccess(new TranslatableComponent("commands.weather.set.clear"), true);
        return duration;
    }

    private static int setRain(CommandSourceStack source, int duration) {
        source.getLevel().setWeatherParameters(0, duration, true, false);
        source.sendSuccess(new TranslatableComponent("commands.weather.set.rain"), true);
        return duration;
    }

    private static int setThunder(CommandSourceStack source, int duration) {
        source.getLevel().setWeatherParameters(0, duration, true, true);
        source.sendSuccess(new TranslatableComponent("commands.weather.set.thunder"), true);
        return duration;
    }
}
