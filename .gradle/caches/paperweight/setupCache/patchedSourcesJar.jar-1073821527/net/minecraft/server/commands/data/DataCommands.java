package net.minecraft.server.commands.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;

public class DataCommands {
    private static final SimpleCommandExceptionType ERROR_MERGE_UNCHANGED = new SimpleCommandExceptionType(new TranslatableComponent("commands.data.merge.failed"));
    private static final DynamicCommandExceptionType ERROR_GET_NOT_NUMBER = new DynamicCommandExceptionType((path) -> {
        return new TranslatableComponent("commands.data.get.invalid", path);
    });
    private static final DynamicCommandExceptionType ERROR_GET_NON_EXISTENT = new DynamicCommandExceptionType((path) -> {
        return new TranslatableComponent("commands.data.get.unknown", path);
    });
    private static final SimpleCommandExceptionType ERROR_MULTIPLE_TAGS = new SimpleCommandExceptionType(new TranslatableComponent("commands.data.get.multiple"));
    private static final DynamicCommandExceptionType ERROR_EXPECTED_LIST = new DynamicCommandExceptionType((nbt) -> {
        return new TranslatableComponent("commands.data.modify.expected_list", nbt);
    });
    private static final DynamicCommandExceptionType ERROR_EXPECTED_OBJECT = new DynamicCommandExceptionType((nbt) -> {
        return new TranslatableComponent("commands.data.modify.expected_object", nbt);
    });
    private static final DynamicCommandExceptionType ERROR_INVALID_INDEX = new DynamicCommandExceptionType((index) -> {
        return new TranslatableComponent("commands.data.modify.invalid_index", index);
    });
    public static final List<Function<String, DataCommands.DataProvider>> ALL_PROVIDERS = ImmutableList.of(EntityDataAccessor.PROVIDER, BlockDataAccessor.PROVIDER, StorageDataAccessor.PROVIDER);
    public static final List<DataCommands.DataProvider> TARGET_PROVIDERS = ALL_PROVIDERS.stream().map((factory) -> {
        return factory.apply("target");
    }).collect(ImmutableList.toImmutableList());
    public static final List<DataCommands.DataProvider> SOURCE_PROVIDERS = ALL_PROVIDERS.stream().map((factory) -> {
        return factory.apply("source");
    }).collect(ImmutableList.toImmutableList());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("data").requires((source) -> {
            return source.hasPermission(2);
        });

        for(DataCommands.DataProvider dataProvider : TARGET_PROVIDERS) {
            literalArgumentBuilder.then(dataProvider.wrap(Commands.literal("merge"), (builder) -> {
                return builder.then(Commands.argument("nbt", CompoundTagArgument.compoundTag()).executes((context) -> {
                    return mergeData(context.getSource(), dataProvider.access(context), CompoundTagArgument.getCompoundTag(context, "nbt"));
                }));
            })).then(dataProvider.wrap(Commands.literal("get"), (builder) -> {
                return builder.executes((context) -> {
                    return getData(context.getSource(), dataProvider.access(context));
                }).then(Commands.argument("path", NbtPathArgument.nbtPath()).executes((context) -> {
                    return getData(context.getSource(), dataProvider.access(context), NbtPathArgument.getPath(context, "path"));
                }).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes((context) -> {
                    return getNumeric(context.getSource(), dataProvider.access(context), NbtPathArgument.getPath(context, "path"), DoubleArgumentType.getDouble(context, "scale"));
                })));
            })).then(dataProvider.wrap(Commands.literal("remove"), (builder) -> {
                return builder.then(Commands.argument("path", NbtPathArgument.nbtPath()).executes((context) -> {
                    return removeData(context.getSource(), dataProvider.access(context), NbtPathArgument.getPath(context, "path"));
                }));
            })).then(decorateModification((builder, modifier) -> {
                builder.then(Commands.literal("insert").then(Commands.argument("index", IntegerArgumentType.integer()).then(modifier.create((context, sourceNbt, path, elements) -> {
                    int i = IntegerArgumentType.getInteger(context, "index");
                    return insertAtIndex(i, sourceNbt, path, elements);
                })))).then(Commands.literal("prepend").then(modifier.create((context, sourceNbt, path, elements) -> {
                    return insertAtIndex(0, sourceNbt, path, elements);
                }))).then(Commands.literal("append").then(modifier.create((context, sourceNbt, path, elements) -> {
                    return insertAtIndex(-1, sourceNbt, path, elements);
                }))).then(Commands.literal("set").then(modifier.create((context, sourceNbt, path, elements) -> {
                    return path.set(sourceNbt, Iterables.getLast(elements)::copy);
                }))).then(Commands.literal("merge").then(modifier.create((context, sourceNbt, path, elements) -> {
                    Collection<Tag> collection = path.getOrCreate(sourceNbt, CompoundTag::new);
                    int i = 0;

                    for(Tag tag : collection) {
                        if (!(tag instanceof CompoundTag)) {
                            throw ERROR_EXPECTED_OBJECT.create(tag);
                        }

                        CompoundTag compoundTag = (CompoundTag)tag;
                        CompoundTag compoundTag2 = compoundTag.copy();

                        for(Tag tag2 : elements) {
                            if (!(tag2 instanceof CompoundTag)) {
                                throw ERROR_EXPECTED_OBJECT.create(tag2);
                            }

                            compoundTag.merge((CompoundTag)tag2);
                        }

                        i += compoundTag2.equals(compoundTag) ? 0 : 1;
                    }

                    return i;
                })));
            }));
        }

        dispatcher.register(literalArgumentBuilder);
    }

    private static int insertAtIndex(int integer, CompoundTag sourceNbt, NbtPathArgument.NbtPath path, List<Tag> elements) throws CommandSyntaxException {
        Collection<Tag> collection = path.getOrCreate(sourceNbt, ListTag::new);
        int i = 0;

        for(Tag tag : collection) {
            if (!(tag instanceof CollectionTag)) {
                throw ERROR_EXPECTED_LIST.create(tag);
            }

            boolean bl = false;
            CollectionTag<?> collectionTag = (CollectionTag)tag;
            int j = integer < 0 ? collectionTag.size() + integer + 1 : integer;

            for(Tag tag2 : elements) {
                try {
                    if (collectionTag.addTag(j, tag2.copy())) {
                        ++j;
                        bl = true;
                    }
                } catch (IndexOutOfBoundsException var14) {
                    throw ERROR_INVALID_INDEX.create(j);
                }
            }

            i += bl ? 1 : 0;
        }

        return i;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> decorateModification(BiConsumer<ArgumentBuilder<CommandSourceStack, ?>, DataCommands.DataManipulatorDecorator> subArgumentAdder) {
        LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("modify");

        for(DataCommands.DataProvider dataProvider : TARGET_PROVIDERS) {
            dataProvider.wrap(literalArgumentBuilder, (builder) -> {
                ArgumentBuilder<CommandSourceStack, ?> argumentBuilder = Commands.argument("targetPath", NbtPathArgument.nbtPath());

                for(DataCommands.DataProvider dataProvider2 : SOURCE_PROVIDERS) {
                    subArgumentAdder.accept(argumentBuilder, (modifier) -> {
                        return dataProvider2.wrap(Commands.literal("from"), (builder) -> {
                            return builder.executes((context) -> {
                                List<Tag> list = Collections.singletonList(dataProvider2.access(context).getData());
                                return manipulateData(context, dataProvider, modifier, list);
                            }).then(Commands.argument("sourcePath", NbtPathArgument.nbtPath()).executes((context) -> {
                                DataAccessor dataAccessor = dataProvider2.access(context);
                                NbtPathArgument.NbtPath nbtPath = NbtPathArgument.getPath(context, "sourcePath");
                                List<Tag> list = nbtPath.get(dataAccessor.getData());
                                return manipulateData(context, dataProvider, modifier, list);
                            }));
                        });
                    });
                }

                subArgumentAdder.accept(argumentBuilder, (modifier) -> {
                    return Commands.literal("value").then(Commands.argument("value", NbtTagArgument.nbtTag()).executes((context) -> {
                        List<Tag> list = Collections.singletonList(NbtTagArgument.getNbtTag(context, "value"));
                        return manipulateData(context, dataProvider, modifier, list);
                    }));
                });
                return builder.then(argumentBuilder);
            });
        }

        return literalArgumentBuilder;
    }

    private static int manipulateData(CommandContext<CommandSourceStack> context, DataCommands.DataProvider objectType, DataCommands.DataManipulator modifier, List<Tag> elements) throws CommandSyntaxException {
        DataAccessor dataAccessor = objectType.access(context);
        NbtPathArgument.NbtPath nbtPath = NbtPathArgument.getPath(context, "targetPath");
        CompoundTag compoundTag = dataAccessor.getData();
        int i = modifier.modify(context, compoundTag, nbtPath, elements);
        if (i == 0) {
            throw ERROR_MERGE_UNCHANGED.create();
        } else {
            dataAccessor.setData(compoundTag);
            context.getSource().sendSuccess(dataAccessor.getModifiedSuccess(), true);
            return i;
        }
    }

    private static int removeData(CommandSourceStack source, DataAccessor object, NbtPathArgument.NbtPath path) throws CommandSyntaxException {
        CompoundTag compoundTag = object.getData();
        int i = path.remove(compoundTag);
        if (i == 0) {
            throw ERROR_MERGE_UNCHANGED.create();
        } else {
            object.setData(compoundTag);
            source.sendSuccess(object.getModifiedSuccess(), true);
            return i;
        }
    }

    private static Tag getSingleTag(NbtPathArgument.NbtPath path, DataAccessor object) throws CommandSyntaxException {
        Collection<Tag> collection = path.get(object.getData());
        Iterator<Tag> iterator = collection.iterator();
        Tag tag = iterator.next();
        if (iterator.hasNext()) {
            throw ERROR_MULTIPLE_TAGS.create();
        } else {
            return tag;
        }
    }

    private static int getData(CommandSourceStack source, DataAccessor object, NbtPathArgument.NbtPath path) throws CommandSyntaxException {
        Tag tag = getSingleTag(path, object);
        int i;
        if (tag instanceof NumericTag) {
            i = Mth.floor(((NumericTag)tag).getAsDouble());
        } else if (tag instanceof CollectionTag) {
            i = ((CollectionTag)tag).size();
        } else if (tag instanceof CompoundTag) {
            i = ((CompoundTag)tag).size();
        } else {
            if (!(tag instanceof StringTag)) {
                throw ERROR_GET_NON_EXISTENT.create(path.toString());
            }

            i = tag.getAsString().length();
        }

        source.sendSuccess(object.getPrintSuccess(tag), false);
        return i;
    }

    private static int getNumeric(CommandSourceStack source, DataAccessor object, NbtPathArgument.NbtPath path, double scale) throws CommandSyntaxException {
        Tag tag = getSingleTag(path, object);
        if (!(tag instanceof NumericTag)) {
            throw ERROR_GET_NOT_NUMBER.create(path.toString());
        } else {
            int i = Mth.floor(((NumericTag)tag).getAsDouble() * scale);
            source.sendSuccess(object.getPrintSuccess(path, scale, i), false);
            return i;
        }
    }

    private static int getData(CommandSourceStack source, DataAccessor object) throws CommandSyntaxException {
        source.sendSuccess(object.getPrintSuccess(object.getData()), false);
        return 1;
    }

    private static int mergeData(CommandSourceStack source, DataAccessor object, CompoundTag nbt) throws CommandSyntaxException {
        CompoundTag compoundTag = object.getData();
        CompoundTag compoundTag2 = compoundTag.copy().merge(nbt);
        if (compoundTag.equals(compoundTag2)) {
            throw ERROR_MERGE_UNCHANGED.create();
        } else {
            object.setData(compoundTag2);
            source.sendSuccess(object.getModifiedSuccess(), true);
            return 1;
        }
    }

    interface DataManipulator {
        int modify(CommandContext<CommandSourceStack> context, CompoundTag sourceNbt, NbtPathArgument.NbtPath path, List<Tag> elements) throws CommandSyntaxException;
    }

    interface DataManipulatorDecorator {
        ArgumentBuilder<CommandSourceStack, ?> create(DataCommands.DataManipulator modifier);
    }

    public interface DataProvider {
        DataAccessor access(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

        ArgumentBuilder<CommandSourceStack, ?> wrap(ArgumentBuilder<CommandSourceStack, ?> argument, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> argumentAdder);
    }
}
