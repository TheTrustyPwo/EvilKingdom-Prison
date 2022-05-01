package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;

public class SayCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("say").requires((commandSourceStack) -> {
            return commandSourceStack.hasPermission(2);
        }).then(Commands.argument("message", MessageArgument.message()).executes((context) -> {
            Component component = MessageArgument.getMessage(context, "message");
            Component component2 = new TranslatableComponent("chat.type.announcement", context.getSource().getDisplayName(), component);
            Entity entity = context.getSource().getEntity();
            if (entity != null) {
                context.getSource().getServer().getPlayerList().broadcastMessage(component2, ChatType.CHAT, entity.getUUID());
            } else {
                context.getSource().getServer().getPlayerList().broadcastMessage(component2, ChatType.SYSTEM, Util.NIL_UUID);
            }

            return 1;
        })));
    }
}
