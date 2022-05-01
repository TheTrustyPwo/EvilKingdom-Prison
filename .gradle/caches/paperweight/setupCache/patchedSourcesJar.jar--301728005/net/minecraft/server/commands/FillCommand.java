package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class FillCommand {
    private static final int MAX_FILL_AREA = 32768;
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((maxCount, count) -> {
        return new TranslatableComponent("commands.fill.toobig", maxCount, count);
    });
    static final BlockInput HOLLOW_CORE = new BlockInput(Blocks.AIR.defaultBlockState(), Collections.emptySet(), (CompoundTag)null);
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.fill.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fill").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos()).then(Commands.argument("block", BlockStateArgument.block()).executes((context) -> {
            return fillBlocks(context.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to")), BlockStateArgument.getBlock(context, "block"), FillCommand.Mode.REPLACE, (Predicate<BlockInWorld>)null);
        }).then(Commands.literal("replace").executes((context) -> {
            return fillBlocks(context.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to")), BlockStateArgument.getBlock(context, "block"), FillCommand.Mode.REPLACE, (Predicate<BlockInWorld>)null);
        }).then(Commands.argument("filter", BlockPredicateArgument.blockPredicate()).executes((context) -> {
            return fillBlocks(context.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to")), BlockStateArgument.getBlock(context, "block"), FillCommand.Mode.REPLACE, BlockPredicateArgument.getBlockPredicate(context, "filter"));
        }))).then(Commands.literal("keep").executes((context) -> {
            return fillBlocks(context.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to")), BlockStateArgument.getBlock(context, "block"), FillCommand.Mode.REPLACE, (pos) -> {
                return pos.getLevel().isEmptyBlock(pos.getPos());
            });
        })).then(Commands.literal("outline").executes((context) -> {
            return fillBlocks(context.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to")), BlockStateArgument.getBlock(context, "block"), FillCommand.Mode.OUTLINE, (Predicate<BlockInWorld>)null);
        })).then(Commands.literal("hollow").executes((context) -> {
            return fillBlocks(context.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to")), BlockStateArgument.getBlock(context, "block"), FillCommand.Mode.HOLLOW, (Predicate<BlockInWorld>)null);
        })).then(Commands.literal("destroy").executes((context) -> {
            return fillBlocks(context.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to")), BlockStateArgument.getBlock(context, "block"), FillCommand.Mode.DESTROY, (Predicate<BlockInWorld>)null);
        }))))));
    }

    private static int fillBlocks(CommandSourceStack source, BoundingBox range, BlockInput block, FillCommand.Mode mode, @Nullable Predicate<BlockInWorld> filter) throws CommandSyntaxException {
        int i = range.getXSpan() * range.getYSpan() * range.getZSpan();
        if (i > 32768) {
            throw ERROR_AREA_TOO_LARGE.create(32768, i);
        } else {
            List<BlockPos> list = Lists.newArrayList();
            ServerLevel serverLevel = source.getLevel();
            int j = 0;

            for(BlockPos blockPos : BlockPos.betweenClosed(range.minX(), range.minY(), range.minZ(), range.maxX(), range.maxY(), range.maxZ())) {
                if (filter == null || filter.test(new BlockInWorld(serverLevel, blockPos, true))) {
                    BlockInput blockInput = mode.filter.filter(range, blockPos, block, serverLevel);
                    if (blockInput != null) {
                        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
                        Clearable.tryClear(blockEntity);
                        if (blockInput.place(serverLevel, blockPos, 2)) {
                            list.add(blockPos.immutable());
                            ++j;
                        }
                    }
                }
            }

            for(BlockPos blockPos2 : list) {
                Block block2 = serverLevel.getBlockState(blockPos2).getBlock();
                serverLevel.blockUpdated(blockPos2, block2);
            }

            if (j == 0) {
                throw ERROR_FAILED.create();
            } else {
                source.sendSuccess(new TranslatableComponent("commands.fill.success", j), true);
                return j;
            }
        }
    }

    static enum Mode {
        REPLACE((range, pos, block, world) -> {
            return block;
        }),
        OUTLINE((range, pos, block, world) -> {
            return pos.getX() != range.minX() && pos.getX() != range.maxX() && pos.getY() != range.minY() && pos.getY() != range.maxY() && pos.getZ() != range.minZ() && pos.getZ() != range.maxZ() ? null : block;
        }),
        HOLLOW((range, pos, block, world) -> {
            return pos.getX() != range.minX() && pos.getX() != range.maxX() && pos.getY() != range.minY() && pos.getY() != range.maxY() && pos.getZ() != range.minZ() && pos.getZ() != range.maxZ() ? FillCommand.HOLLOW_CORE : block;
        }),
        DESTROY((range, pos, block, world) -> {
            world.destroyBlock(pos, true);
            return block;
        });

        public final SetBlockCommand.Filter filter;

        private Mode(SetBlockCommand.Filter filter) {
            this.filter = filter;
        }
    }
}
