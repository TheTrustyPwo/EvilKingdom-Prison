package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Iterator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class PlaySoundCommand {
    private static final SimpleCommandExceptionType ERROR_TOO_FAR = new SimpleCommandExceptionType(new TranslatableComponent("commands.playsound.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> requiredArgumentBuilder = Commands.argument("sound", ResourceLocationArgument.id()).suggests(SuggestionProviders.AVAILABLE_SOUNDS);

        for(SoundSource soundSource : SoundSource.values()) {
            requiredArgumentBuilder.then(source(soundSource));
        }

        dispatcher.register(Commands.literal("playsound").requires((source) -> {
            return source.hasPermission(2);
        }).then(requiredArgumentBuilder));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> source(SoundSource category) {
        return Commands.literal(category.getName()).then(Commands.argument("targets", EntityArgument.players()).executes((context) -> {
            return playSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), ResourceLocationArgument.getId(context, "sound"), category, context.getSource().getPosition(), 1.0F, 1.0F, 0.0F);
        }).then(Commands.argument("pos", Vec3Argument.vec3()).executes((context) -> {
            return playSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), ResourceLocationArgument.getId(context, "sound"), category, Vec3Argument.getVec3(context, "pos"), 1.0F, 1.0F, 0.0F);
        }).then(Commands.argument("volume", FloatArgumentType.floatArg(0.0F)).executes((context) -> {
            return playSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), ResourceLocationArgument.getId(context, "sound"), category, Vec3Argument.getVec3(context, "pos"), context.getArgument("volume", Float.class), 1.0F, 0.0F);
        }).then(Commands.argument("pitch", FloatArgumentType.floatArg(0.0F, 2.0F)).executes((context) -> {
            return playSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), ResourceLocationArgument.getId(context, "sound"), category, Vec3Argument.getVec3(context, "pos"), context.getArgument("volume", Float.class), context.getArgument("pitch", Float.class), 0.0F);
        }).then(Commands.argument("minVolume", FloatArgumentType.floatArg(0.0F, 1.0F)).executes((context) -> {
            return playSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), ResourceLocationArgument.getId(context, "sound"), category, Vec3Argument.getVec3(context, "pos"), context.getArgument("volume", Float.class), context.getArgument("pitch", Float.class), context.getArgument("minVolume", Float.class));
        }))))));
    }

    private static int playSound(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation sound, SoundSource category, Vec3 pos, float volume, float pitch, float minVolume) throws CommandSyntaxException {
        double d = Math.pow(volume > 1.0F ? (double)(volume * 16.0F) : 16.0D, 2.0D);
        int i = 0;
        Iterator var11 = targets.iterator();

        while(true) {
            ServerPlayer serverPlayer;
            Vec3 vec3;
            float j;
            while(true) {
                if (!var11.hasNext()) {
                    if (i == 0) {
                        throw ERROR_TOO_FAR.create();
                    }

                    if (targets.size() == 1) {
                        source.sendSuccess(new TranslatableComponent("commands.playsound.success.single", sound, targets.iterator().next().getDisplayName()), true);
                    } else {
                        source.sendSuccess(new TranslatableComponent("commands.playsound.success.multiple", sound, targets.size()), true);
                    }

                    return i;
                }

                serverPlayer = (ServerPlayer)var11.next();
                double e = pos.x - serverPlayer.getX();
                double f = pos.y - serverPlayer.getY();
                double g = pos.z - serverPlayer.getZ();
                double h = e * e + f * f + g * g;
                vec3 = pos;
                j = volume;
                if (!(h > d)) {
                    break;
                }

                if (!(minVolume <= 0.0F)) {
                    double k = Math.sqrt(h);
                    vec3 = new Vec3(serverPlayer.getX() + e / k * 2.0D, serverPlayer.getY() + f / k * 2.0D, serverPlayer.getZ() + g / k * 2.0D);
                    j = minVolume;
                    break;
                }
            }

            serverPlayer.connection.send(new ClientboundCustomSoundPacket(sound, category, vec3, j, pitch));
            ++i;
        }
    }
}
