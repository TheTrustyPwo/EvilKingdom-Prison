package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

public class DebugMobSpawningCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("debugmobspawning").requires((source) -> {
            return source.hasPermission(2);
        });

        for(MobCategory mobCategory : MobCategory.values()) {
            literalArgumentBuilder.then(Commands.literal(mobCategory.getName()).then(Commands.argument("at", BlockPosArgument.blockPos()).executes((context) -> {
                return spawnMobs(context.getSource(), mobCategory, BlockPosArgument.getLoadedBlockPos(context, "at"));
            })));
        }

        dispatcher.register(literalArgumentBuilder);
    }

    private static int spawnMobs(CommandSourceStack source, MobCategory group, BlockPos pos) {
        NaturalSpawner.spawnCategoryForPosition(group, source.getLevel(), pos);
        return 1;
    }
}
