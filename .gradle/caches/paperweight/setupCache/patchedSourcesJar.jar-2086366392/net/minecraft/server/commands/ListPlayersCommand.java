package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

public class ListPlayersCommand {

    public ListPlayersCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.Commands.literal("list").executes((commandcontext) -> {
            return ListPlayersCommand.listPlayers((CommandSourceStack) commandcontext.getSource());
        })).then(net.minecraft.commands.Commands.literal("uuids").executes((commandcontext) -> {
            return ListPlayersCommand.listPlayersWithUuids((CommandSourceStack) commandcontext.getSource());
        })));
    }

    private static int listPlayers(CommandSourceStack source) {
        return ListPlayersCommand.format(source, Player::getDisplayName);
    }

    private static int listPlayersWithUuids(CommandSourceStack source) {
        return ListPlayersCommand.format(source, (entityplayer) -> {
            return new TranslatableComponent("commands.list.nameAndId", new Object[]{entityplayer.getName(), entityplayer.getGameProfile().getId()});
        });
    }

    private static int format(CommandSourceStack source, Function<ServerPlayer, Component> nameProvider) {
        PlayerList playerlist = source.getServer().getPlayerList();
        List<ServerPlayer> list = playerlist.getPlayers();
        // CraftBukkit start
        if (source.getBukkitSender() instanceof org.bukkit.entity.Player) {
            org.bukkit.entity.Player sender = (org.bukkit.entity.Player) source.getBukkitSender();
            list = list.stream().filter((ep) -> sender.canSee(ep.getBukkitEntity())).collect(java.util.stream.Collectors.toList());
        }
        // CraftBukkit end
        Component ichatbasecomponent = ComponentUtils.formatList(list, nameProvider);

        source.sendSuccess(new TranslatableComponent("commands.list.players", new Object[]{list.size(), playerlist.getMaxPlayers(), ichatbasecomponent}), false);
        return list.size();
    }
}
