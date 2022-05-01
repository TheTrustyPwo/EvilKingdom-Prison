package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.players.BanListEntry;
import net.minecraft.server.players.PlayerList;

public class BanListCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("banlist").requires((source) -> {
            return source.hasPermission(3);
        }).executes((context) -> {
            PlayerList playerList = context.getSource().getServer().getPlayerList();
            return showList(context.getSource(), Lists.newArrayList(Iterables.concat(playerList.getBans().getEntries(), playerList.getIpBans().getEntries())));
        }).then(Commands.literal("ips").executes((context) -> {
            return showList(context.getSource(), context.getSource().getServer().getPlayerList().getIpBans().getEntries());
        })).then(Commands.literal("players").executes((context) -> {
            return showList(context.getSource(), context.getSource().getServer().getPlayerList().getBans().getEntries());
        })));
    }

    private static int showList(CommandSourceStack source, Collection<? extends BanListEntry<?>> targets) {
        if (targets.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("commands.banlist.none"), false);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.banlist.list", targets.size()), false);

            for(BanListEntry<?> banListEntry : targets) {
                source.sendSuccess(new TranslatableComponent("commands.banlist.entry", banListEntry.getDisplayName(), banListEntry.getSource(), banListEntry.getReason()), false);
            }
        }

        return targets.size();
    }
}
