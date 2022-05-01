package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

public class GameModeCommand {
    public static final int PERMISSION_LEVEL = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("gamemode").requires((source) -> {
            return source.hasPermission(2);
        });

        for(GameType gameType : GameType.values()) {
            literalArgumentBuilder.then(Commands.literal(gameType.getName()).executes((context) -> {
                return setMode(context, Collections.singleton(context.getSource().getPlayerOrException()), gameType);
            }).then(Commands.argument("target", EntityArgument.players()).executes((context) -> {
                return setMode(context, EntityArgument.getPlayers(context, "target"), gameType);
            })));
        }

        dispatcher.register(literalArgumentBuilder);
    }

    private static void logGamemodeChange(CommandSourceStack source, ServerPlayer player, GameType gameMode) {
        Component component = new TranslatableComponent("gameMode." + gameMode.getName());
        if (source.getEntity() == player) {
            source.sendSuccess(new TranslatableComponent("commands.gamemode.success.self", component), true);
        } else {
            if (source.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
                player.sendMessage(new TranslatableComponent("gameMode.changed", component), Util.NIL_UUID);
            }

            source.sendSuccess(new TranslatableComponent("commands.gamemode.success.other", player.getDisplayName(), component), true);
        }

    }

    private static int setMode(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, GameType gameMode) {
        int i = 0;

        for(ServerPlayer serverPlayer : targets) {
            // Paper start - extend PlayerGameModeChangeEvent
            org.bukkit.event.player.PlayerGameModeChangeEvent event = serverPlayer.setGameMode(gameMode, org.bukkit.event.player.PlayerGameModeChangeEvent.Cause.COMMAND, net.kyori.adventure.text.Component.empty());
            if (event != null && !event.isCancelled()) {
                logGamemodeChange(context.getSource(), serverPlayer, gameMode);
                ++i;
            } else if (event != null && event.cancelMessage() != null) {
                context.getSource().sendSuccess(io.papermc.paper.adventure.PaperAdventure.asVanilla(event.cancelMessage()), true);
                // Paper end
            }
        }

        return i;
    }
}
