package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;

public class LocateCommand {
    private static final DynamicCommandExceptionType ERROR_FAILED = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("commands.locate.failed", id);
    });
    private static final DynamicCommandExceptionType ERROR_INVALID = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("commands.locate.invalid", id);
    });

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("locate").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("structure", ResourceOrTagLocationArgument.resourceOrTag(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY)).executes((context) -> {
            return locate(context.getSource(), ResourceOrTagLocationArgument.getStructureFeature(context, "structure"));
        })));
    }

    private static int locate(CommandSourceStack source, ResourceOrTagLocationArgument.Result<ConfiguredStructureFeature<?, ?>> structureFeature) throws CommandSyntaxException {
        Registry<ConfiguredStructureFeature<?, ?>> registry = source.getLevel().registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
        HolderSet<ConfiguredStructureFeature<?, ?>> holderSet = structureFeature.unwrap().map((key) -> {
            return registry.getHolder(key).map((entry) -> {
                return HolderSet.direct(entry);
            });
        }, registry::getTag).orElseThrow(() -> {
            return ERROR_INVALID.create(structureFeature.asPrintable());
        });
        BlockPos blockPos = new BlockPos(source.getPosition());
        ServerLevel serverLevel = source.getLevel();
        Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> pair = serverLevel.getChunkSource().getGenerator().findNearestMapFeature(serverLevel, holderSet, blockPos, 100, false);
        if (pair == null) {
            throw ERROR_FAILED.create(structureFeature.asPrintable());
        } else {
            return showLocateResult(source, structureFeature, blockPos, pair, "commands.locate.success");
        }
    }

    public static int showLocateResult(CommandSourceStack source, ResourceOrTagLocationArgument.Result<?> structureFeature, BlockPos currentPos, Pair<BlockPos, ? extends Holder<?>> structurePosAndEntry, String successMessage) {
        BlockPos blockPos = structurePosAndEntry.getFirst();
        String string = structureFeature.unwrap().map((key) -> {
            return key.location().toString();
        }, (key) -> {
            return "#" + key.location() + " (" + (String)structurePosAndEntry.getSecond().unwrapKey().map((keyx) -> {
                return keyx.location().toString();
            }).orElse("[unregistered]") + ")";
        });
        int i = Mth.floor(dist(currentPos.getX(), currentPos.getZ(), blockPos.getX(), blockPos.getZ()));
        Component component = ComponentUtils.wrapInSquareBrackets(new TranslatableComponent("chat.coordinates", blockPos.getX(), "~", blockPos.getZ())).withStyle((style) -> {
            return style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockPos.getX() + " ~ " + blockPos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("chat.coordinates.tooltip")));
        });
        source.sendSuccess(new TranslatableComponent(successMessage, string, component, i), false);
        return i;
    }

    private static float dist(int x1, int y1, int x2, int y2) {
        int i = x2 - x1;
        int j = y2 - y1;
        return Mth.sqrt((float)(i * i + j * j));
    }
}
