package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class MsgCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(Commands.literal("msg").then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("message", MessageArgument.message()).executes((context) -> {
            return sendMessage(context.getSource(), EntityArgument.getPlayers(context, "targets"), MessageArgument.getMessage(context, "message"));
        }))));
        dispatcher.register(Commands.literal("tell").redirect(literalCommandNode));
        dispatcher.register(Commands.literal("w").redirect(literalCommandNode));
    }

    private static int sendMessage(CommandSourceStack source, Collection<ServerPlayer> targets, Component message) {
        UUID uUID = source.getEntity() == null ? Util.NIL_UUID : source.getEntity().getUUID();
        Entity entity = source.getEntity();
        Consumer<Component> consumer;
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            consumer = (playerName) -> {
                serverPlayer.sendMessage((new TranslatableComponent("commands.message.display.outgoing", playerName, message)).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}), serverPlayer.getUUID());
            };
        } else {
            consumer = (playerName) -> {
                source.sendSuccess((new TranslatableComponent("commands.message.display.outgoing", playerName, message)).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}), false);
            };
        }

        for(ServerPlayer serverPlayer2 : targets) {
            consumer.accept(serverPlayer2.getDisplayName());
            serverPlayer2.sendMessage((new TranslatableComponent("commands.message.display.incoming", source.getDisplayName(), message)).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}), uUID);
        }

        return targets.size();
    }
}
