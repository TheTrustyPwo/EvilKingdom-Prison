package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;

public class FunctionArgument implements ArgumentType<FunctionArgument.Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "#foo");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("arguments.function.tag.unknown", id);
    });
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_FUNCTION = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("arguments.function.unknown", id);
    });

    public static FunctionArgument functions() {
        return new FunctionArgument();
    }

    public FunctionArgument.Result parse(StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '#') {
            stringReader.skip();
            final ResourceLocation resourceLocation = ResourceLocation.read(stringReader);
            return new FunctionArgument.Result() {
                @Override
                public Collection<CommandFunction> create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                    Tag<CommandFunction> tag = FunctionArgument.getFunctionTag(context, resourceLocation);
                    return tag.getValues();
                }

                @Override
                public Pair<ResourceLocation, Either<CommandFunction, Tag<CommandFunction>>> unwrap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                    return Pair.of(resourceLocation, Either.right(FunctionArgument.getFunctionTag(context, resourceLocation)));
                }
            };
        } else {
            final ResourceLocation resourceLocation2 = ResourceLocation.read(stringReader);
            return new FunctionArgument.Result() {
                @Override
                public Collection<CommandFunction> create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                    return Collections.singleton(FunctionArgument.getFunction(context, resourceLocation2));
                }

                @Override
                public Pair<ResourceLocation, Either<CommandFunction, Tag<CommandFunction>>> unwrap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                    return Pair.of(resourceLocation2, Either.left(FunctionArgument.getFunction(context, resourceLocation2)));
                }
            };
        }
    }

    static CommandFunction getFunction(CommandContext<CommandSourceStack> context, ResourceLocation id) throws CommandSyntaxException {
        return context.getSource().getServer().getFunctions().get(id).orElseThrow(() -> {
            return ERROR_UNKNOWN_FUNCTION.create(id.toString());
        });
    }

    static Tag<CommandFunction> getFunctionTag(CommandContext<CommandSourceStack> context, ResourceLocation id) throws CommandSyntaxException {
        Tag<CommandFunction> tag = context.getSource().getServer().getFunctions().getTag(id);
        if (tag == null) {
            throw ERROR_UNKNOWN_TAG.create(id.toString());
        } else {
            return tag;
        }
    }

    public static Collection<CommandFunction> getFunctions(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, FunctionArgument.Result.class).create(context);
    }

    public static Pair<ResourceLocation, Either<CommandFunction, Tag<CommandFunction>>> getFunctionOrTag(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, FunctionArgument.Result.class).unwrap(context);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public interface Result {
        Collection<CommandFunction> create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

        Pair<ResourceLocation, Either<CommandFunction, Tag<CommandFunction>>> unwrap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
    }
}
