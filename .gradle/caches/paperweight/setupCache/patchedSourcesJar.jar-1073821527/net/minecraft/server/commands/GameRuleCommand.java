package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.GameRules;

public class GameRuleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("gamerule").requires((source) -> {
            return source.hasPermission(2);
        });
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                literalArgumentBuilder.then(Commands.literal(key.getId()).executes((context) -> {
                    return GameRuleCommand.queryRule(context.getSource(), key);
                }).then(type.createArgument("value").executes((context) -> {
                    return GameRuleCommand.setRule(context, key);
                })));
            }
        });
        dispatcher.register(literalArgumentBuilder);
    }

    static <T extends GameRules.Value<T>> int setRule(CommandContext<CommandSourceStack> context, GameRules.Key<T> key) {
        CommandSourceStack commandSourceStack = context.getSource();
        T value = commandSourceStack.getServer().getGameRules().getRule(key);
        value.setFromArgument(context, "value");
        commandSourceStack.sendSuccess(new TranslatableComponent("commands.gamerule.set", key.getId(), value.toString()), true);
        return value.getCommandResult();
    }

    static <T extends GameRules.Value<T>> int queryRule(CommandSourceStack source, GameRules.Key<T> key) {
        T value = source.getServer().getGameRules().getRule(key);
        source.sendSuccess(new TranslatableComponent("commands.gamerule.query", key.getId(), value.toString()), false);
        return value.getCommandResult();
    }
}
