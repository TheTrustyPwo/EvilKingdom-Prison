package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;

public class TeamMsgCommand {
    private static final Style SUGGEST_STYLE = Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("chat.type.team.hover"))).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/teammsg "));
    private static final SimpleCommandExceptionType ERROR_NOT_ON_TEAM = new SimpleCommandExceptionType(new TranslatableComponent("commands.teammsg.failed.noteam"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(Commands.literal("teammsg").then(Commands.argument("message", MessageArgument.message()).executes((context) -> {
            return sendMessage(context.getSource(), MessageArgument.getMessage(context, "message"));
        })));
        dispatcher.register(Commands.literal("tm").redirect(literalCommandNode));
    }

    private static int sendMessage(CommandSourceStack source, Component message) throws CommandSyntaxException {
        Entity entity = source.getEntityOrException();
        PlayerTeam playerTeam = (PlayerTeam)entity.getTeam();
        if (playerTeam == null) {
            throw ERROR_NOT_ON_TEAM.create();
        } else {
            Component component = playerTeam.getFormattedDisplayName().withStyle(SUGGEST_STYLE);
            List<ServerPlayer> list = source.getServer().getPlayerList().getPlayers();

            for(ServerPlayer serverPlayer : list) {
                if (serverPlayer == entity) {
                    serverPlayer.sendMessage(new TranslatableComponent("chat.type.team.sent", component, source.getDisplayName(), message), entity.getUUID());
                } else if (serverPlayer.getTeam() == playerTeam) {
                    serverPlayer.sendMessage(new TranslatableComponent("chat.type.team.text", component, source.getDisplayName(), message), entity.getUUID());
                }
            }

            return list.size();
        }
    }
}
