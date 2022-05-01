package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class CloneCommands {
    private static final int MAX_CLONE_AREA = 32768;
    private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(new TranslatableComponent("commands.clone.overlap"));
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((maxCount, count) -> {
        return new TranslatableComponent("commands.clone.toobig", maxCount, count);
    });
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.clone.failed"));
    public static final Predicate<BlockInWorld> FILTER_AIR = (pos) -> {
        return !pos.getState().isAir();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("clone").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("begin", BlockPosArgument.blockPos()).then(Commands.argument("end", BlockPosArgument.blockPos()).then(Commands.argument("destination", BlockPosArgument.blockPos()).executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), (pos) -> {
                return true;
            }, CloneCommands.Mode.NORMAL);
        }).then(Commands.literal("replace").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), (pos) -> {
                return true;
            }, CloneCommands.Mode.NORMAL);
        }).then(Commands.literal("force").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), (pos) -> {
                return true;
            }, CloneCommands.Mode.FORCE);
        })).then(Commands.literal("move").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), (pos) -> {
                return true;
            }, CloneCommands.Mode.MOVE);
        })).then(Commands.literal("normal").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), (pos) -> {
                return true;
            }, CloneCommands.Mode.NORMAL);
        }))).then(Commands.literal("masked").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), FILTER_AIR, CloneCommands.Mode.NORMAL);
        }).then(Commands.literal("force").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), FILTER_AIR, CloneCommands.Mode.FORCE);
        })).then(Commands.literal("move").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), FILTER_AIR, CloneCommands.Mode.MOVE);
        })).then(Commands.literal("normal").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), FILTER_AIR, CloneCommands.Mode.NORMAL);
        }))).then(Commands.literal("filtered").then(Commands.argument("filter", BlockPredicateArgument.blockPredicate()).executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), BlockPredicateArgument.getBlockPredicate(context, "filter"), CloneCommands.Mode.NORMAL);
        }).then(Commands.literal("force").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), BlockPredicateArgument.getBlockPredicate(context, "filter"), CloneCommands.Mode.FORCE);
        })).then(Commands.literal("move").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), BlockPredicateArgument.getBlockPredicate(context, "filter"), CloneCommands.Mode.MOVE);
        })).then(Commands.literal("normal").executes((context) -> {
            return clone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "begin"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), BlockPredicateArgument.getBlockPredicate(context, "filter"), CloneCommands.Mode.NORMAL);
        }))))))));
    }

    private static int clone(CommandSourceStack source, BlockPos begin, BlockPos end, BlockPos destination, Predicate<BlockInWorld> filter, CloneCommands.Mode mode) throws CommandSyntaxException {
        BoundingBox boundingBox = BoundingBox.fromCorners(begin, end);
        BlockPos blockPos = destination.offset(boundingBox.getLength());
        BoundingBox boundingBox2 = BoundingBox.fromCorners(destination, blockPos);
        if (!mode.canOverlap() && boundingBox2.intersects(boundingBox)) {
            throw ERROR_OVERLAP.create();
        } else {
            int i = boundingBox.getXSpan() * boundingBox.getYSpan() * boundingBox.getZSpan();
            if (i > 32768) {
                throw ERROR_AREA_TOO_LARGE.create(32768, i);
            } else {
                ServerLevel serverLevel = source.getLevel();
                if (serverLevel.hasChunksAt(begin, end) && serverLevel.hasChunksAt(destination, blockPos)) {
                    List<CloneCommands.CloneBlockInfo> list = Lists.newArrayList();
                    List<CloneCommands.CloneBlockInfo> list2 = Lists.newArrayList();
                    List<CloneCommands.CloneBlockInfo> list3 = Lists.newArrayList();
                    Deque<BlockPos> deque = Lists.newLinkedList();
                    BlockPos blockPos2 = new BlockPos(boundingBox2.minX() - boundingBox.minX(), boundingBox2.minY() - boundingBox.minY(), boundingBox2.minZ() - boundingBox.minZ());

                    for(int j = boundingBox.minZ(); j <= boundingBox.maxZ(); ++j) {
                        for(int k = boundingBox.minY(); k <= boundingBox.maxY(); ++k) {
                            for(int l = boundingBox.minX(); l <= boundingBox.maxX(); ++l) {
                                BlockPos blockPos3 = new BlockPos(l, k, j);
                                BlockPos blockPos4 = blockPos3.offset(blockPos2);
                                BlockInWorld blockInWorld = new BlockInWorld(serverLevel, blockPos3, false);
                                BlockState blockState = blockInWorld.getState();
                                if (filter.test(blockInWorld)) {
                                    BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos3);
                                    if (blockEntity != null) {
                                        CompoundTag compoundTag = blockEntity.saveWithoutMetadata();
                                        list2.add(new CloneCommands.CloneBlockInfo(blockPos4, blockState, compoundTag));
                                        deque.addLast(blockPos3);
                                    } else if (!blockState.isSolidRender(serverLevel, blockPos3) && !blockState.isCollisionShapeFullBlock(serverLevel, blockPos3)) {
                                        list3.add(new CloneCommands.CloneBlockInfo(blockPos4, blockState, (CompoundTag)null));
                                        deque.addFirst(blockPos3);
                                    } else {
                                        list.add(new CloneCommands.CloneBlockInfo(blockPos4, blockState, (CompoundTag)null));
                                        deque.addLast(blockPos3);
                                    }
                                }
                            }
                        }
                    }

                    if (mode == CloneCommands.Mode.MOVE) {
                        for(BlockPos blockPos5 : deque) {
                            BlockEntity blockEntity2 = serverLevel.getBlockEntity(blockPos5);
                            Clearable.tryClear(blockEntity2);
                            serverLevel.setBlock(blockPos5, Blocks.BARRIER.defaultBlockState(), 2);
                        }

                        for(BlockPos blockPos6 : deque) {
                            serverLevel.setBlock(blockPos6, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }

                    List<CloneCommands.CloneBlockInfo> list4 = Lists.newArrayList();
                    list4.addAll(list);
                    list4.addAll(list2);
                    list4.addAll(list3);
                    List<CloneCommands.CloneBlockInfo> list5 = Lists.reverse(list4);

                    for(CloneCommands.CloneBlockInfo cloneBlockInfo : list5) {
                        BlockEntity blockEntity3 = serverLevel.getBlockEntity(cloneBlockInfo.pos);
                        Clearable.tryClear(blockEntity3);
                        serverLevel.setBlock(cloneBlockInfo.pos, Blocks.BARRIER.defaultBlockState(), 2);
                    }

                    int m = 0;

                    for(CloneCommands.CloneBlockInfo cloneBlockInfo2 : list4) {
                        if (serverLevel.setBlock(cloneBlockInfo2.pos, cloneBlockInfo2.state, 2)) {
                            ++m;
                        }
                    }

                    for(CloneCommands.CloneBlockInfo cloneBlockInfo3 : list2) {
                        BlockEntity blockEntity4 = serverLevel.getBlockEntity(cloneBlockInfo3.pos);
                        if (cloneBlockInfo3.tag != null && blockEntity4 != null) {
                            blockEntity4.load(cloneBlockInfo3.tag);
                            blockEntity4.setChanged();
                        }

                        serverLevel.setBlock(cloneBlockInfo3.pos, cloneBlockInfo3.state, 2);
                    }

                    for(CloneCommands.CloneBlockInfo cloneBlockInfo4 : list5) {
                        serverLevel.blockUpdated(cloneBlockInfo4.pos, cloneBlockInfo4.state.getBlock());
                    }

                    serverLevel.getBlockTicks().copyArea(boundingBox, blockPos2);
                    if (m == 0) {
                        throw ERROR_FAILED.create();
                    } else {
                        source.sendSuccess(new TranslatableComponent("commands.clone.success", m), true);
                        return m;
                    }
                } else {
                    throw BlockPosArgument.ERROR_NOT_LOADED.create();
                }
            }
        }
    }

    static class CloneBlockInfo {
        public final BlockPos pos;
        public final BlockState state;
        @Nullable
        public final CompoundTag tag;

        public CloneBlockInfo(BlockPos pos, BlockState state, @Nullable CompoundTag blockEntityNbt) {
            this.pos = pos;
            this.state = state;
            this.tag = blockEntityNbt;
        }
    }

    static enum Mode {
        FORCE(true),
        MOVE(true),
        NORMAL(false);

        private final boolean canOverlap;

        private Mode(boolean allowsOverlap) {
            this.canOverlap = allowsOverlap;
        }

        public boolean canOverlap() {
            return this.canOverlap;
        }
    }
}
