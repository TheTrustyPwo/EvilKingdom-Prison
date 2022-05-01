package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

public class ListPlayersCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("list").executes((context) -> {
            return listPlayers(context.getSource());
        }).then(Commands.literal("uuids").executes((context) -> {
            return listPlayersWithUuids(context.getSource());
        })));
    }

    private static int listPlayers(CommandSourceStack source) {
        return format(source, Player::getDisplayName);
    }

    private static int listPlayersWithUuids(CommandSourceStack source) {
        return format(source, (player) -> {
            return new TranslatableComponent("commands.list.nameAndId", player.getName(), player.getGameProfile().getId());
        });
    }

    private static int format(CommandSourceStack source, Function<ServerPlayer, Component> nameProvider) {
        PlayerList playerList = source.getServer().getPlayerList();
        List<ServerPlayer> list = playerList.getPlayers();
        Component component = ComponentUtils.formatList(list, nameProvider);
        source.sendSuccess(new TranslatableComponent("commands.list.players", list.size(), playerList.getMaxPlayers(), component), false);
        return list.size();
    }
}
