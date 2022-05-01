package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class EmoteCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("me").then(Commands.argument("action", StringArgumentType.greedyString()).executes((context) -> {
            String string = StringArgumentType.getString(context, "action");
            Entity entity = context.getSource().getEntity();
            MinecraftServer minecraftServer = context.getSource().getServer();
            if (entity != null) {
                if (entity instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer)entity;
                    serverPlayer.getTextFilter().processStreamMessage(string).thenAcceptAsync((message) -> {
                        String string = message.getFiltered();
                        Component component = string.isEmpty() ? null : createMessage(context, string);
                        Component component2 = createMessage(context, message.getRaw());
                        minecraftServer.getPlayerList().broadcastMessage(component2, (player) -> {
                            return serverPlayer.shouldFilterMessageTo(player) ? component : component2;
                        }, ChatType.CHAT, entity.getUUID());
                    }, minecraftServer);
                    return 1;
                }

                minecraftServer.getPlayerList().broadcastMessage(createMessage(context, string), ChatType.CHAT, entity.getUUID());
            } else {
                minecraftServer.getPlayerList().broadcastMessage(createMessage(context, string), ChatType.SYSTEM, Util.NIL_UUID);
            }

            return 1;
        })));
    }

    private static Component createMessage(CommandContext<CommandSourceStack> context, String arg) {
        return new TranslatableComponent("chat.type.emote", context.getSource().getDisplayName(), arg);
    }
}
