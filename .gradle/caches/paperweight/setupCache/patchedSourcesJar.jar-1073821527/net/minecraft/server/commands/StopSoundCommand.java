package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class StopSoundCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> requiredArgumentBuilder = Commands.argument("targets", EntityArgument.players()).executes((context) -> {
            return stopSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), (SoundSource)null, (ResourceLocation)null);
        }).then(Commands.literal("*").then(Commands.argument("sound", ResourceLocationArgument.id()).suggests(SuggestionProviders.AVAILABLE_SOUNDS).executes((context) -> {
            return stopSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), (SoundSource)null, ResourceLocationArgument.getId(context, "sound"));
        })));

        for(SoundSource soundSource : SoundSource.values()) {
            requiredArgumentBuilder.then(Commands.literal(soundSource.getName()).executes((context) -> {
                return stopSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), soundSource, (ResourceLocation)null);
            }).then(Commands.argument("sound", ResourceLocationArgument.id()).suggests(SuggestionProviders.AVAILABLE_SOUNDS).executes((context) -> {
                return stopSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), soundSource, ResourceLocationArgument.getId(context, "sound"));
            })));
        }

        dispatcher.register(Commands.literal("stopsound").requires((source) -> {
            return source.hasPermission(2);
        }).then(requiredArgumentBuilder));
    }

    private static int stopSound(CommandSourceStack source, Collection<ServerPlayer> targets, @Nullable SoundSource category, @Nullable ResourceLocation sound) {
        ClientboundStopSoundPacket clientboundStopSoundPacket = new ClientboundStopSoundPacket(sound, category);

        for(ServerPlayer serverPlayer : targets) {
            serverPlayer.connection.send(clientboundStopSoundPacket);
        }

        if (category != null) {
            if (sound != null) {
                source.sendSuccess(new TranslatableComponent("commands.stopsound.success.source.sound", sound, category.getName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.stopsound.success.source.any", category.getName()), true);
            }
        } else if (sound != null) {
            source.sendSuccess(new TranslatableComponent("commands.stopsound.success.sourceless.sound", sound), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.stopsound.success.sourceless.any"), true);
        }

        return targets.size();
    }
}
