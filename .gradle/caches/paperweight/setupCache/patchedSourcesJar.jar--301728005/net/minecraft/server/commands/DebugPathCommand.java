package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

public class DebugPathCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_MOB = new SimpleCommandExceptionType(new TextComponent("Source is not a mob"));
    private static final SimpleCommandExceptionType ERROR_NO_PATH = new SimpleCommandExceptionType(new TextComponent("Path not found"));
    private static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(new TextComponent("Target not reached"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("debugpath").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("to", BlockPosArgument.blockPos()).executes((context) -> {
            return fillBlocks(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "to"));
        })));
    }

    private static int fillBlocks(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
        Entity entity = source.getEntity();
        if (!(entity instanceof Mob)) {
            throw ERROR_NOT_MOB.create();
        } else {
            Mob mob = (Mob)entity;
            PathNavigation pathNavigation = new GroundPathNavigation(mob, source.getLevel());
            Path path = pathNavigation.createPath(pos, 0);
            DebugPackets.sendPathFindingPacket(source.getLevel(), mob, path, pathNavigation.getMaxDistanceToWaypoint());
            if (path == null) {
                throw ERROR_NO_PATH.create();
            } else if (!path.canReach()) {
                throw ERROR_NOT_COMPLETE.create();
            } else {
                source.sendSuccess(new TextComponent("Made path"), true);
                return 1;
            }
        }
    }
}
