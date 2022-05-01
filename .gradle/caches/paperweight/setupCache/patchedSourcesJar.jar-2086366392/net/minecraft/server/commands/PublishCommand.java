package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

public class PublishCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.publish.failed"));
    private static final DynamicCommandExceptionType ERROR_ALREADY_PUBLISHED = new DynamicCommandExceptionType((port) -> {
        return new TranslatableComponent("commands.publish.alreadyPublished", port);
    });

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("publish").requires((source) -> {
            return source.hasPermission(4);
        }).executes((context) -> {
            return publish(context.getSource(), HttpUtil.getAvailablePort());
        }).then(Commands.argument("port", IntegerArgumentType.integer(0, 65535)).executes((context) -> {
            return publish(context.getSource(), IntegerArgumentType.getInteger(context, "port"));
        })));
    }

    private static int publish(CommandSourceStack source, int port) throws CommandSyntaxException {
        if (source.getServer().isPublished()) {
            throw ERROR_ALREADY_PUBLISHED.create(source.getServer().getPort());
        } else if (!source.getServer().publishServer((GameType)null, false, port)) {
            throw ERROR_FAILED.create();
        } else {
            source.sendSuccess(new TranslatableComponent("commands.publish.success", port), true);
            return port;
        }
    }
}
