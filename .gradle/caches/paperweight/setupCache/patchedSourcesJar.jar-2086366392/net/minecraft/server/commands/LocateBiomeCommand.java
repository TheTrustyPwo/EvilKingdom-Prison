package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.biome.Biome;

public class LocateBiomeCommand {
    private static final DynamicCommandExceptionType ERROR_BIOME_NOT_FOUND = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("commands.locatebiome.notFound", id);
    });
    private static final int MAX_SEARCH_RADIUS = 6400;
    private static final int SEARCH_STEP = 8;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("locatebiome").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("biome", ResourceOrTagLocationArgument.resourceOrTag(Registry.BIOME_REGISTRY)).executes((context) -> {
            return locateBiome(context.getSource(), ResourceOrTagLocationArgument.getBiome(context, "biome"));
        })));
    }

    private static int locateBiome(CommandSourceStack source, ResourceOrTagLocationArgument.Result<Biome> biome) throws CommandSyntaxException {
        BlockPos blockPos = new BlockPos(source.getPosition());
        Pair<BlockPos, Holder<Biome>> pair = source.getLevel().findNearestBiome(biome, blockPos, 6400, 8);
        if (pair == null) {
            throw ERROR_BIOME_NOT_FOUND.create(biome.asPrintable());
        } else {
            return LocateCommand.showLocateResult(source, biome, blockPos, pair, "commands.locatebiome.success");
        }
    }
}
