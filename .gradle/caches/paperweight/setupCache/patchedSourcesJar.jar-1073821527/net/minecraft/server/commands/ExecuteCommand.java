package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.PredicateManager;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public class ExecuteCommand {
    private static final int MAX_TEST_AREA = 32768;
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((maxCount, count) -> {
        return new TranslatableComponent("commands.execute.blocks.toobig", maxCount, count);
    });
    private static final SimpleCommandExceptionType ERROR_CONDITIONAL_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.execute.conditional.fail"));
    private static final DynamicCommandExceptionType ERROR_CONDITIONAL_FAILED_COUNT = new DynamicCommandExceptionType((count) -> {
        return new TranslatableComponent("commands.execute.conditional.fail_count", count);
    });
    private static final BinaryOperator<ResultConsumer<CommandSourceStack>> CALLBACK_CHAINER = (resultConsumer, resultConsumer2) -> {
        return (context, success, result) -> {
            resultConsumer.onCommandComplete(context, success, result);
            resultConsumer2.onCommandComplete(context, success, result);
        };
    };
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PREDICATE = (context, builder) -> {
        PredicateManager predicateManager = context.getSource().getServer().getPredicateManager();
        return SharedSuggestionProvider.suggestResource(predicateManager.getKeys(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(Commands.literal("execute").requires((source) -> {
            return source.hasPermission(2);
        }));
        dispatcher.register(Commands.literal("execute").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("run").redirect(dispatcher.getRoot())).then(addConditionals(literalCommandNode, Commands.literal("if"), true)).then(addConditionals(literalCommandNode, Commands.literal("unless"), false)).then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(literalCommandNode, (context) -> {
            List<CommandSourceStack> list = Lists.newArrayList();

            for(Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withEntity(entity));
            }

            return list;
        }))).then(Commands.literal("at").then(Commands.argument("targets", EntityArgument.entities()).fork(literalCommandNode, (context) -> {
            List<CommandSourceStack> list = Lists.newArrayList();

            for(Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withLevel((ServerLevel)entity.level).withPosition(entity.position()).withRotation(entity.getRotationVector()));
            }

            return list;
        }))).then(Commands.literal("store").then(wrapStores(literalCommandNode, Commands.literal("result"), true)).then(wrapStores(literalCommandNode, Commands.literal("success"), false))).then(Commands.literal("positioned").then(Commands.argument("pos", Vec3Argument.vec3()).redirect(literalCommandNode, (context) -> {
            return context.getSource().withPosition(Vec3Argument.getVec3(context, "pos")).withAnchor(EntityAnchorArgument.Anchor.FEET);
        })).then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(literalCommandNode, (context) -> {
            List<CommandSourceStack> list = Lists.newArrayList();

            for(Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withPosition(entity.position()));
            }

            return list;
        })))).then(Commands.literal("rotated").then(Commands.argument("rot", RotationArgument.rotation()).redirect(literalCommandNode, (context) -> {
            return context.getSource().withRotation(RotationArgument.getRotation(context, "rot").getRotation(context.getSource()));
        })).then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(literalCommandNode, (context) -> {
            List<CommandSourceStack> list = Lists.newArrayList();

            for(Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().withRotation(entity.getRotationVector()));
            }

            return list;
        })))).then(Commands.literal("facing").then(Commands.literal("entity").then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("anchor", EntityAnchorArgument.anchor()).fork(literalCommandNode, (context) -> {
            List<CommandSourceStack> list = Lists.newArrayList();
            EntityAnchorArgument.Anchor anchor = EntityAnchorArgument.getAnchor(context, "anchor");

            for(Entity entity : EntityArgument.getOptionalEntities(context, "targets")) {
                list.add(context.getSource().facing(entity, anchor));
            }

            return list;
        })))).then(Commands.argument("pos", Vec3Argument.vec3()).redirect(literalCommandNode, (context) -> {
            return context.getSource().facing(Vec3Argument.getVec3(context, "pos"));
        }))).then(Commands.literal("align").then(Commands.argument("axes", SwizzleArgument.swizzle()).redirect(literalCommandNode, (context) -> {
            return context.getSource().withPosition(context.getSource().getPosition().align(SwizzleArgument.getSwizzle(context, "axes")));
        }))).then(Commands.literal("anchored").then(Commands.argument("anchor", EntityAnchorArgument.anchor()).redirect(literalCommandNode, (context) -> {
            return context.getSource().withAnchor(EntityAnchorArgument.getAnchor(context, "anchor"));
        }))).then(Commands.literal("in").then(Commands.argument("dimension", DimensionArgument.dimension()).redirect(literalCommandNode, (context) -> {
            return context.getSource().withLevel(DimensionArgument.getDimension(context, "dimension"));
        }))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> wrapStores(LiteralCommandNode<CommandSourceStack> node, LiteralArgumentBuilder<CommandSourceStack> builder, boolean requestResult) {
        builder.then(Commands.literal("score").then(Commands.argument("targets", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("objective", ObjectiveArgument.objective()).redirect(node, (context) -> {
            return storeValue(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "targets"), ObjectiveArgument.getObjective(context, "objective"), requestResult);
        }))));
        builder.then(Commands.literal("bossbar").then(Commands.argument("id", ResourceLocationArgument.id()).suggests(BossBarCommands.SUGGEST_BOSS_BAR).then(Commands.literal("value").redirect(node, (context) -> {
            return storeValue(context.getSource(), BossBarCommands.getBossBar(context), true, requestResult);
        })).then(Commands.literal("max").redirect(node, (context) -> {
            return storeValue(context.getSource(), BossBarCommands.getBossBar(context), false, requestResult);
        }))));

        for(DataCommands.DataProvider dataProvider : DataCommands.TARGET_PROVIDERS) {
            dataProvider.wrap(builder, (builderx) -> {
                return builderx.then(Commands.argument("path", NbtPathArgument.nbtPath()).then(Commands.literal("int").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), NbtPathArgument.getPath(context, "path"), (result) -> {
                        return IntTag.valueOf((int)((double)result * DoubleArgumentType.getDouble(context, "scale")));
                    }, requestResult);
                }))).then(Commands.literal("float").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), NbtPathArgument.getPath(context, "path"), (result) -> {
                        return FloatTag.valueOf((float)((double)result * DoubleArgumentType.getDouble(context, "scale")));
                    }, requestResult);
                }))).then(Commands.literal("short").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), NbtPathArgument.getPath(context, "path"), (result) -> {
                        return ShortTag.valueOf((short)((int)((double)result * DoubleArgumentType.getDouble(context, "scale"))));
                    }, requestResult);
                }))).then(Commands.literal("long").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), NbtPathArgument.getPath(context, "path"), (result) -> {
                        return LongTag.valueOf((long)((double)result * DoubleArgumentType.getDouble(context, "scale")));
                    }, requestResult);
                }))).then(Commands.literal("double").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), NbtPathArgument.getPath(context, "path"), (result) -> {
                        return DoubleTag.valueOf((double)result * DoubleArgumentType.getDouble(context, "scale"));
                    }, requestResult);
                }))).then(Commands.literal("byte").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(node, (context) -> {
                    return storeData(context.getSource(), dataProvider.access(context), NbtPathArgument.getPath(context, "path"), (result) -> {
                        return ByteTag.valueOf((byte)((int)((double)result * DoubleArgumentType.getDouble(context, "scale"))));
                    }, requestResult);
                }))));
            });
        }

        return builder;
    }

    private static CommandSourceStack storeValue(CommandSourceStack source, Collection<String> targets, Objective objective, boolean requestResult) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        return source.withCallback((context, success, result) -> {
            for(String string : targets) {
                Score score = scoreboard.getOrCreatePlayerScore(string, objective);
                int i = requestResult ? result : (success ? 1 : 0);
                score.setScore(i);
            }

        }, CALLBACK_CHAINER);
    }

    private static CommandSourceStack storeValue(CommandSourceStack source, CustomBossEvent bossBar, boolean storeInValue, boolean requestResult) {
        return source.withCallback((context, success, result) -> {
            int i = requestResult ? result : (success ? 1 : 0);
            if (storeInValue) {
                bossBar.setValue(i);
            } else {
                bossBar.setMax(i);
            }

        }, CALLBACK_CHAINER);
    }

    private static CommandSourceStack storeData(CommandSourceStack source, DataAccessor object, NbtPathArgument.NbtPath path, IntFunction<Tag> nbtSetter, boolean requestResult) {
        return source.withCallback((context, success, result) -> {
            try {
                CompoundTag compoundTag = object.getData();
                int i = requestResult ? result : (success ? 1 : 0);
                path.set(compoundTag, () -> {
                    return nbtSetter.apply(i);
                });
                object.setData(compoundTag);
            } catch (CommandSyntaxException var9) {
            }

        }, CALLBACK_CHAINER);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addConditionals(CommandNode<CommandSourceStack> root, LiteralArgumentBuilder<CommandSourceStack> argumentBuilder, boolean positive) {
        argumentBuilder.then(Commands.literal("block").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(addConditional(root, Commands.argument("block", BlockPredicateArgument.blockPredicate()), positive, (context) -> {
            return BlockPredicateArgument.getBlockPredicate(context, "block").test(new BlockInWorld(context.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(context, "pos"), true));
        })))).then(Commands.literal("score").then(Commands.argument("target", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("targetObjective", ObjectiveArgument.objective()).then(Commands.literal("=").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(addConditional(root, Commands.argument("sourceObjective", ObjectiveArgument.objective()), positive, (context) -> {
            return checkScore(context, Integer::equals);
        })))).then(Commands.literal("<").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(addConditional(root, Commands.argument("sourceObjective", ObjectiveArgument.objective()), positive, (context) -> {
            return checkScore(context, (a, b) -> {
                return a < b;
            });
        })))).then(Commands.literal("<=").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(addConditional(root, Commands.argument("sourceObjective", ObjectiveArgument.objective()), positive, (context) -> {
            return checkScore(context, (a, b) -> {
                return a <= b;
            });
        })))).then(Commands.literal(">").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(addConditional(root, Commands.argument("sourceObjective", ObjectiveArgument.objective()), positive, (context) -> {
            return checkScore(context, (a, b) -> {
                return a > b;
            });
        })))).then(Commands.literal(">=").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(addConditional(root, Commands.argument("sourceObjective", ObjectiveArgument.objective()), positive, (context) -> {
            return checkScore(context, (a, b) -> {
                return a >= b;
            });
        })))).then(Commands.literal("matches").then(addConditional(root, Commands.argument("range", RangeArgument.intRange()), positive, (context) -> {
            return checkScore(context, RangeArgument.Ints.getRange(context, "range"));
        })))))).then(Commands.literal("blocks").then(Commands.argument("start", BlockPosArgument.blockPos()).then(Commands.argument("end", BlockPosArgument.blockPos()).then(Commands.argument("destination", BlockPosArgument.blockPos()).then(addIfBlocksConditional(root, Commands.literal("all"), positive, false)).then(addIfBlocksConditional(root, Commands.literal("masked"), positive, true)))))).then(Commands.literal("entity").then(Commands.argument("entities", EntityArgument.entities()).fork(root, (context) -> {
            return expect(context, positive, !EntityArgument.getOptionalEntities(context, "entities").isEmpty());
        }).executes(createNumericConditionalHandler(positive, (context) -> {
            return EntityArgument.getOptionalEntities(context, "entities").size();
        })))).then(Commands.literal("predicate").then(addConditional(root, Commands.argument("predicate", ResourceLocationArgument.id()).suggests(SUGGEST_PREDICATE), positive, (context) -> {
            return checkCustomPredicate(context.getSource(), ResourceLocationArgument.getPredicate(context, "predicate"));
        })));

        for(DataCommands.DataProvider dataProvider : DataCommands.SOURCE_PROVIDERS) {
            argumentBuilder.then(dataProvider.wrap(Commands.literal("data"), (builder) -> {
                return builder.then(Commands.argument("path", NbtPathArgument.nbtPath()).fork(root, (commandContext) -> {
                    return expect(commandContext, positive, checkMatchingData(dataProvider.access(commandContext), NbtPathArgument.getPath(commandContext, "path")) > 0);
                }).executes(createNumericConditionalHandler(positive, (context) -> {
                    return checkMatchingData(dataProvider.access(context), NbtPathArgument.getPath(context, "path"));
                })));
            }));
        }

        return argumentBuilder;
    }

    private static Command<CommandSourceStack> createNumericConditionalHandler(boolean positive, ExecuteCommand.CommandNumericPredicate condition) {
        return positive ? (context) -> {
            int i = condition.test(context);
            if (i > 0) {
                context.getSource().sendSuccess(new TranslatableComponent("commands.execute.conditional.pass_count", i), false);
                return i;
            } else {
                throw ERROR_CONDITIONAL_FAILED.create();
            }
        } : (context) -> {
            int i = condition.test(context);
            if (i == 0) {
                context.getSource().sendSuccess(new TranslatableComponent("commands.execute.conditional.pass"), false);
                return 1;
            } else {
                throw ERROR_CONDITIONAL_FAILED_COUNT.create(i);
            }
        };
    }

    private static int checkMatchingData(DataAccessor object, NbtPathArgument.NbtPath path) throws CommandSyntaxException {
        return path.countMatching(object.getData());
    }

    private static boolean checkScore(CommandContext<CommandSourceStack> context, BiPredicate<Integer, Integer> condition) throws CommandSyntaxException {
        String string = ScoreHolderArgument.getName(context, "target");
        Objective objective = ObjectiveArgument.getObjective(context, "targetObjective");
        String string2 = ScoreHolderArgument.getName(context, "source");
        Objective objective2 = ObjectiveArgument.getObjective(context, "sourceObjective");
        Scoreboard scoreboard = context.getSource().getServer().getScoreboard();
        if (scoreboard.hasPlayerScore(string, objective) && scoreboard.hasPlayerScore(string2, objective2)) {
            Score score = scoreboard.getOrCreatePlayerScore(string, objective);
            Score score2 = scoreboard.getOrCreatePlayerScore(string2, objective2);
            return condition.test(score.getScore(), score2.getScore());
        } else {
            return false;
        }
    }

    private static boolean checkScore(CommandContext<CommandSourceStack> context, MinMaxBounds.Ints range) throws CommandSyntaxException {
        String string = ScoreHolderArgument.getName(context, "target");
        Objective objective = ObjectiveArgument.getObjective(context, "targetObjective");
        Scoreboard scoreboard = context.getSource().getServer().getScoreboard();
        return !scoreboard.hasPlayerScore(string, objective) ? false : range.matches(scoreboard.getOrCreatePlayerScore(string, objective).getScore());
    }

    private static boolean checkCustomPredicate(CommandSourceStack source, LootItemCondition condition) {
        ServerLevel serverLevel = source.getLevel();
        LootContext.Builder builder = (new LootContext.Builder(serverLevel)).withParameter(LootContextParams.ORIGIN, source.getPosition()).withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity());
        return condition.test(builder.create(LootContextParamSets.COMMAND));
    }

    private static Collection<CommandSourceStack> expect(CommandContext<CommandSourceStack> context, boolean positive, boolean value) {
        return (Collection<CommandSourceStack>)(value == positive ? Collections.singleton(context.getSource()) : Collections.emptyList());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addConditional(CommandNode<CommandSourceStack> root, ArgumentBuilder<CommandSourceStack, ?> builder, boolean positive, ExecuteCommand.CommandPredicate condition) {
        return builder.fork(root, (context) -> {
            return expect(context, positive, condition.test(context));
        }).executes((context) -> {
            if (positive == condition.test(context)) {
                context.getSource().sendSuccess(new TranslatableComponent("commands.execute.conditional.pass"), false);
                return 1;
            } else {
                throw ERROR_CONDITIONAL_FAILED.create();
            }
        });
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addIfBlocksConditional(CommandNode<CommandSourceStack> root, ArgumentBuilder<CommandSourceStack, ?> builder, boolean positive, boolean masked) {
        return builder.fork(root, (context) -> {
            return expect(context, positive, checkRegions(context, masked).isPresent());
        }).executes(positive ? (context) -> {
            return checkIfRegions(context, masked);
        } : (context) -> {
            return checkUnlessRegions(context, masked);
        });
    }

    private static int checkIfRegions(CommandContext<CommandSourceStack> context, boolean masked) throws CommandSyntaxException {
        OptionalInt optionalInt = checkRegions(context, masked);
        if (optionalInt.isPresent()) {
            context.getSource().sendSuccess(new TranslatableComponent("commands.execute.conditional.pass_count", optionalInt.getAsInt()), false);
            return optionalInt.getAsInt();
        } else {
            throw ERROR_CONDITIONAL_FAILED.create();
        }
    }

    private static int checkUnlessRegions(CommandContext<CommandSourceStack> context, boolean masked) throws CommandSyntaxException {
        OptionalInt optionalInt = checkRegions(context, masked);
        if (optionalInt.isPresent()) {
            throw ERROR_CONDITIONAL_FAILED_COUNT.create(optionalInt.getAsInt());
        } else {
            context.getSource().sendSuccess(new TranslatableComponent("commands.execute.conditional.pass"), false);
            return 1;
        }
    }

    private static OptionalInt checkRegions(CommandContext<CommandSourceStack> context, boolean masked) throws CommandSyntaxException {
        return checkRegions(context.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(context, "start"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), masked);
    }

    private static OptionalInt checkRegions(ServerLevel world, BlockPos start, BlockPos end, BlockPos destination, boolean masked) throws CommandSyntaxException {
        BoundingBox boundingBox = BoundingBox.fromCorners(start, end);
        BoundingBox boundingBox2 = BoundingBox.fromCorners(destination, destination.offset(boundingBox.getLength()));
        BlockPos blockPos = new BlockPos(boundingBox2.minX() - boundingBox.minX(), boundingBox2.minY() - boundingBox.minY(), boundingBox2.minZ() - boundingBox.minZ());
        int i = boundingBox.getXSpan() * boundingBox.getYSpan() * boundingBox.getZSpan();
        if (i > 32768) {
            throw ERROR_AREA_TOO_LARGE.create(32768, i);
        } else {
            int j = 0;

            for(int k = boundingBox.minZ(); k <= boundingBox.maxZ(); ++k) {
                for(int l = boundingBox.minY(); l <= boundingBox.maxY(); ++l) {
                    for(int m = boundingBox.minX(); m <= boundingBox.maxX(); ++m) {
                        BlockPos blockPos2 = new BlockPos(m, l, k);
                        BlockPos blockPos3 = blockPos2.offset(blockPos);
                        BlockState blockState = world.getBlockState(blockPos2);
                        if (!masked || !blockState.is(Blocks.AIR)) {
                            if (blockState != world.getBlockState(blockPos3)) {
                                return OptionalInt.empty();
                            }

                            BlockEntity blockEntity = world.getBlockEntity(blockPos2);
                            BlockEntity blockEntity2 = world.getBlockEntity(blockPos3);
                            if (blockEntity != null) {
                                if (blockEntity2 == null) {
                                    return OptionalInt.empty();
                                }

                                if (blockEntity2.getType() != blockEntity.getType()) {
                                    return OptionalInt.empty();
                                }

                                CompoundTag compoundTag = blockEntity.saveWithoutMetadata();
                                CompoundTag compoundTag2 = blockEntity2.saveWithoutMetadata();
                                if (!compoundTag.equals(compoundTag2)) {
                                    return OptionalInt.empty();
                                }
                            }

                            ++j;
                        }
                    }
                }
            }

            return OptionalInt.of(j);
        }
    }

    @FunctionalInterface
    interface CommandNumericPredicate {
        int test(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface CommandPredicate {
        boolean test(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }
}
