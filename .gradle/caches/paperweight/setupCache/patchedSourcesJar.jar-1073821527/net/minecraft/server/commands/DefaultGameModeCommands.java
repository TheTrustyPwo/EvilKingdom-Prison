package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class DefaultGameModeCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("defaultgamemode").requires((source) -> {
            return source.hasPermission(2);
        });

        for(GameType gameType : GameType.values()) {
            literalArgumentBuilder.then(Commands.literal(gameType.getName()).executes((context) -> {
                return setMode(context.getSource(), gameType);
            }));
        }

        dispatcher.register(literalArgumentBuilder);
    }

    private static int setMode(CommandSourceStack source, GameType defaultGameMode) {
        int i = 0;
        MinecraftServer minecraftServer = source.getServer();
        minecraftServer.setDefaultGameType(defaultGameMode);
        GameType gameType = minecraftServer.getForcedGameType();
        if (gameType != null) {
            for(ServerPlayer serverPlayer : minecraftServer.getPlayerList().getPlayers()) {
                if (serverPlayer.setGameMode(gameType)) {
                    ++i;
                }
            }
        }

        source.sendSuccess(new TranslatableComponent("commands.defaultgamemode.success", defaultGameMode.getLongDisplayName()), true);
        return i;
    }
}
