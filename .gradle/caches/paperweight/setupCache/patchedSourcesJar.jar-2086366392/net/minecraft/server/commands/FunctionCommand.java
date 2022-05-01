package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.ServerFunctionManager;

public class FunctionCommand {
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_FUNCTION = (context, builder) -> {
        ServerFunctionManager serverFunctionManager = context.getSource().getServer().getFunctions();
        SharedSuggestionProvider.suggestResource(serverFunctionManager.getTagNames(), builder, "#");
        return SharedSuggestionProvider.suggestResource(serverFunctionManager.getFunctionNames(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("function").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("name", FunctionArgument.functions()).suggests(SUGGEST_FUNCTION).executes((context) -> {
            return runFunction(context.getSource(), FunctionArgument.getFunctions(context, "name"));
        })));
    }

    private static int runFunction(CommandSourceStack source, Collection<CommandFunction> functions) {
        int i = 0;

        for(CommandFunction commandFunction : functions) {
            i += source.getServer().getFunctions().execute(commandFunction, source.withSuppressedOutput().withMaximumPermission(2));
        }

        if (functions.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.function.success.single", i, functions.iterator().next().getId()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.function.success.multiple", i, functions.size()), true);
        }

        return i;
    }
}
