package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

public class SpectateCommand {
    private static final SimpleCommandExceptionType ERROR_SELF = new SimpleCommandExceptionType(new TranslatableComponent("commands.spectate.self"));
    private static final DynamicCommandExceptionType ERROR_NOT_SPECTATOR = new DynamicCommandExceptionType((playerName) -> {
        return new TranslatableComponent("commands.spectate.not_spectator", playerName);
    });

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spectate").requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            return spectate(context.getSource(), (Entity)null, context.getSource().getPlayerOrException());
        }).then(Commands.argument("target", EntityArgument.entity()).executes((context) -> {
            return spectate(context.getSource(), EntityArgument.getEntity(context, "target"), context.getSource().getPlayerOrException());
        }).then(Commands.argument("player", EntityArgument.player()).executes((context) -> {
            return spectate(context.getSource(), EntityArgument.getEntity(context, "target"), EntityArgument.getPlayer(context, "player"));
        }))));
    }

    private static int spectate(CommandSourceStack source, @Nullable Entity entity, ServerPlayer player) throws CommandSyntaxException {
        if (player == entity) {
            throw ERROR_SELF.create();
        } else if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            throw ERROR_NOT_SPECTATOR.create(player.getDisplayName());
        } else {
            player.setCamera(entity);
            if (entity != null) {
                source.sendSuccess(new TranslatableComponent("commands.spectate.success.started", entity.getDisplayName()), false);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.spectate.success.stopped"), false);
            }

            return 1;
        }
    }
}
