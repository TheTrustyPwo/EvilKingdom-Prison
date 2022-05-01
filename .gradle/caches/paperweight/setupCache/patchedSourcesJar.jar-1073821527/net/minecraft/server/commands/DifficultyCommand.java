package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

public class DifficultyCommand {
    private static final DynamicCommandExceptionType ERROR_ALREADY_DIFFICULT = new DynamicCommandExceptionType((difficulty) -> {
        return new TranslatableComponent("commands.difficulty.failure", difficulty);
    });

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("difficulty");

        for(Difficulty difficulty : Difficulty.values()) {
            literalArgumentBuilder.then(Commands.literal(difficulty.getKey()).executes((context) -> {
                return setDifficulty(context.getSource(), difficulty);
            }));
        }

        dispatcher.register(literalArgumentBuilder.requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            Difficulty difficulty = context.getSource().getLevel().getDifficulty();
            context.getSource().sendSuccess(new TranslatableComponent("commands.difficulty.query", difficulty.getDisplayName()), false);
            return difficulty.getId();
        }));
    }

    public static int setDifficulty(CommandSourceStack source, Difficulty difficulty) throws CommandSyntaxException {
        MinecraftServer minecraftServer = source.getServer();
        if (minecraftServer.getWorldData().getDifficulty() == difficulty) {
            throw ERROR_ALREADY_DIFFICULT.create(difficulty.getKey());
        } else {
            minecraftServer.setDifficulty(difficulty, true);
            source.sendSuccess(new TranslatableComponent("commands.difficulty.success", difficulty.getDisplayName()), true);
            return 0;
        }
    }
}
