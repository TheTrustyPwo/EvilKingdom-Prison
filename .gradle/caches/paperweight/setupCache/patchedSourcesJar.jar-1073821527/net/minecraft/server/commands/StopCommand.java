package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;

public class StopCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stop").requires((source) -> {
            return source.hasPermission(4);
        }).executes((context) -> {
            context.getSource().sendSuccess(new TranslatableComponent("commands.stop.stopping"), true);
            context.getSource().getServer().halt(false);
            return 1;
        }));
    }
}
