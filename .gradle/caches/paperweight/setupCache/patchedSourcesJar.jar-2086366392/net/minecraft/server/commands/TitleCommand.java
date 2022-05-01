package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public class TitleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("title").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("clear").executes((context) -> {
            return clearTitle(context.getSource(), EntityArgument.getPlayers(context, "targets"));
        })).then(Commands.literal("reset").executes((context) -> {
            return resetTitle(context.getSource(), EntityArgument.getPlayers(context, "targets"));
        })).then(Commands.literal("title").then(Commands.argument("title", ComponentArgument.textComponent()).executes((context) -> {
            return showTitle(context.getSource(), EntityArgument.getPlayers(context, "targets"), ComponentArgument.getComponent(context, "title"), "title", ClientboundSetTitleTextPacket::new);
        }))).then(Commands.literal("subtitle").then(Commands.argument("title", ComponentArgument.textComponent()).executes((context) -> {
            return showTitle(context.getSource(), EntityArgument.getPlayers(context, "targets"), ComponentArgument.getComponent(context, "title"), "subtitle", ClientboundSetSubtitleTextPacket::new);
        }))).then(Commands.literal("actionbar").then(Commands.argument("title", ComponentArgument.textComponent()).executes((context) -> {
            return showTitle(context.getSource(), EntityArgument.getPlayers(context, "targets"), ComponentArgument.getComponent(context, "title"), "actionbar", ClientboundSetActionBarTextPacket::new);
        }))).then(Commands.literal("times").then(Commands.argument("fadeIn", IntegerArgumentType.integer(0)).then(Commands.argument("stay", IntegerArgumentType.integer(0)).then(Commands.argument("fadeOut", IntegerArgumentType.integer(0)).executes((context) -> {
            return setTimes(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "fadeIn"), IntegerArgumentType.getInteger(context, "stay"), IntegerArgumentType.getInteger(context, "fadeOut"));
        })))))));
    }

    private static int clearTitle(CommandSourceStack source, Collection<ServerPlayer> targets) {
        ClientboundClearTitlesPacket clientboundClearTitlesPacket = new ClientboundClearTitlesPacket(false);

        for(ServerPlayer serverPlayer : targets) {
            serverPlayer.connection.send(clientboundClearTitlesPacket);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.title.cleared.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.title.cleared.multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int resetTitle(CommandSourceStack source, Collection<ServerPlayer> targets) {
        ClientboundClearTitlesPacket clientboundClearTitlesPacket = new ClientboundClearTitlesPacket(true);

        for(ServerPlayer serverPlayer : targets) {
            serverPlayer.connection.send(clientboundClearTitlesPacket);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.title.reset.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.title.reset.multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int showTitle(CommandSourceStack source, Collection<ServerPlayer> targets, Component title, String titleType, Function<Component, Packet<?>> constructor) throws CommandSyntaxException {
        for(ServerPlayer serverPlayer : targets) {
            serverPlayer.connection.send(constructor.apply(ComponentUtils.updateForEntity(source, title, serverPlayer, 0)));
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.title.show." + titleType + ".single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.title.show." + titleType + ".multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int setTimes(CommandSourceStack source, Collection<ServerPlayer> targets, int fadeIn, int stay, int fadeOut) {
        ClientboundSetTitlesAnimationPacket clientboundSetTitlesAnimationPacket = new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);

        for(ServerPlayer serverPlayer : targets) {
            serverPlayer.connection.send(clientboundSetTitlesAnimationPacket);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.title.times.single", targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.title.times.multiple", targets.size()), true);
        }

        return targets.size();
    }
}
