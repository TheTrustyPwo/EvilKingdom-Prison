package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class PlaceFeatureCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.placefeature.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("placefeature").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("feature", ResourceKeyArgument.key(Registry.CONFIGURED_FEATURE_REGISTRY)).executes((context) -> {
            return placeFeature(context.getSource(), ResourceKeyArgument.getConfiguredFeature(context, "feature"), new BlockPos(context.getSource().getPosition()));
        }).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((context) -> {
            return placeFeature(context.getSource(), ResourceKeyArgument.getConfiguredFeature(context, "feature"), BlockPosArgument.getLoadedBlockPos(context, "pos"));
        }))));
    }

    public static int placeFeature(CommandSourceStack source, Holder<ConfiguredFeature<?, ?>> feature, BlockPos pos) throws CommandSyntaxException {
        ServerLevel serverLevel = source.getLevel();
        ConfiguredFeature<?, ?> configuredFeature = feature.value();
        if (!configuredFeature.place(serverLevel, serverLevel.getChunkSource().getGenerator(), serverLevel.getRandom(), pos)) {
            throw ERROR_FAILED.create();
        } else {
            String string = feature.unwrapKey().map((key) -> {
                return key.location().toString();
            }).orElse("[unregistered]");
            source.sendSuccess(new TranslatableComponent("commands.placefeature.success", string, pos.getX(), pos.getY(), pos.getZ()), true);
            return 1;
        }
    }
}
