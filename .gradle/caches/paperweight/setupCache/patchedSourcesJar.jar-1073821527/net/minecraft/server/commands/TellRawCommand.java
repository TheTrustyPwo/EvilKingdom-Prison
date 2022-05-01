package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;

public class TellRawCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tellraw").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("message", ComponentArgument.textComponent()).executes((context) -> {
            int i = 0;

            for(ServerPlayer serverPlayer : EntityArgument.getPlayers(context, "targets")) {
                serverPlayer.sendMessage(ComponentUtils.updateForEntity(context.getSource(), ComponentArgument.getComponent(context, "message"), serverPlayer, 0), Util.NIL_UUID);
                ++i;
            }

            return i;
        }))));
    }
}
