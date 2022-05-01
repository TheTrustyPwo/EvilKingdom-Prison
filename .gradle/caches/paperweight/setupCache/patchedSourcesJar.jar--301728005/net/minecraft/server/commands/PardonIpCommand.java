package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.regex.Matcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.players.IpBanList;

public class PardonIpCommand {
    private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(new TranslatableComponent("commands.pardonip.invalid"));
    private static final SimpleCommandExceptionType ERROR_NOT_BANNED = new SimpleCommandExceptionType(new TranslatableComponent("commands.pardonip.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pardon-ip").requires((source) -> {
            return source.hasPermission(3);
        }).then(Commands.argument("target", StringArgumentType.word()).suggests((context, builder) -> {
            return SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getIpBans().getUserList(), builder);
        }).executes((context) -> {
            return unban(context.getSource(), StringArgumentType.getString(context, "target"));
        })));
    }

    private static int unban(CommandSourceStack source, String target) throws CommandSyntaxException {
        Matcher matcher = BanIpCommands.IP_ADDRESS_PATTERN.matcher(target);
        if (!matcher.matches()) {
            throw ERROR_INVALID.create();
        } else {
            IpBanList ipBanList = source.getServer().getPlayerList().getIpBans();
            if (!ipBanList.isBanned(target)) {
                throw ERROR_NOT_BANNED.create();
            } else {
                ipBanList.remove(target);
                source.sendSuccess(new TranslatableComponent("commands.pardonip.success", target), true);
                return 1;
            }
        }
    }
}
