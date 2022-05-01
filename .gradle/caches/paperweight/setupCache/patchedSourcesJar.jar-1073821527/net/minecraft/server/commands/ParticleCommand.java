package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ParticleCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.particle.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("particle").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("name", ParticleArgument.particle()).executes((context) -> {
            return sendParticles(context.getSource(), ParticleArgument.getParticle(context, "name"), context.getSource().getPosition(), Vec3.ZERO, 0.0F, 0, false, context.getSource().getServer().getPlayerList().getPlayers());
        }).then(Commands.argument("pos", Vec3Argument.vec3()).executes((context) -> {
            return sendParticles(context.getSource(), ParticleArgument.getParticle(context, "name"), Vec3Argument.getVec3(context, "pos"), Vec3.ZERO, 0.0F, 0, false, context.getSource().getServer().getPlayerList().getPlayers());
        }).then(Commands.argument("delta", Vec3Argument.vec3(false)).then(Commands.argument("speed", FloatArgumentType.floatArg(0.0F)).then(Commands.argument("count", IntegerArgumentType.integer(0)).executes((context) -> {
            return sendParticles(context.getSource(), ParticleArgument.getParticle(context, "name"), Vec3Argument.getVec3(context, "pos"), Vec3Argument.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"), IntegerArgumentType.getInteger(context, "count"), false, context.getSource().getServer().getPlayerList().getPlayers());
        }).then(Commands.literal("force").executes((context) -> {
            return sendParticles(context.getSource(), ParticleArgument.getParticle(context, "name"), Vec3Argument.getVec3(context, "pos"), Vec3Argument.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"), IntegerArgumentType.getInteger(context, "count"), true, context.getSource().getServer().getPlayerList().getPlayers());
        }).then(Commands.argument("viewers", EntityArgument.players()).executes((context) -> {
            return sendParticles(context.getSource(), ParticleArgument.getParticle(context, "name"), Vec3Argument.getVec3(context, "pos"), Vec3Argument.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"), IntegerArgumentType.getInteger(context, "count"), true, EntityArgument.getPlayers(context, "viewers"));
        }))).then(Commands.literal("normal").executes((context) -> {
            return sendParticles(context.getSource(), ParticleArgument.getParticle(context, "name"), Vec3Argument.getVec3(context, "pos"), Vec3Argument.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"), IntegerArgumentType.getInteger(context, "count"), false, context.getSource().getServer().getPlayerList().getPlayers());
        }).then(Commands.argument("viewers", EntityArgument.players()).executes((context) -> {
            return sendParticles(context.getSource(), ParticleArgument.getParticle(context, "name"), Vec3Argument.getVec3(context, "pos"), Vec3Argument.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"), IntegerArgumentType.getInteger(context, "count"), false, EntityArgument.getPlayers(context, "viewers"));
        })))))))));
    }

    private static int sendParticles(CommandSourceStack source, ParticleOptions parameters, Vec3 pos, Vec3 delta, float speed, int count, boolean force, Collection<ServerPlayer> viewers) throws CommandSyntaxException {
        int i = 0;

        for(ServerPlayer serverPlayer : viewers) {
            if (source.getLevel().sendParticles(serverPlayer, parameters, force, pos.x, pos.y, pos.z, count, delta.x, delta.y, delta.z, (double)speed)) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_FAILED.create();
        } else {
            source.sendSuccess(new TranslatableComponent("commands.particle.success", Registry.PARTICLE_TYPE.getKey(parameters.getType()).toString()), true);
            return i;
        }
    }
}
