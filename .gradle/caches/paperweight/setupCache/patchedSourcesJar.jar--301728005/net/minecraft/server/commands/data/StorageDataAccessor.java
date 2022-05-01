package net.minecraft.server.commands.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.CommandStorage;

public class StorageDataAccessor implements DataAccessor {
    static final SuggestionProvider<CommandSourceStack> SUGGEST_STORAGE = (context, builder) -> {
        return SharedSuggestionProvider.suggestResource(getGlobalTags(context).keys(), builder);
    };
    public static final Function<String, DataCommands.DataProvider> PROVIDER = (argumentName) -> {
        return new DataCommands.DataProvider() {
            @Override
            public DataAccessor access(CommandContext<CommandSourceStack> context) {
                return new StorageDataAccessor(StorageDataAccessor.getGlobalTags(context), ResourceLocationArgument.getId(context, argumentName));
            }

            @Override
            public ArgumentBuilder<CommandSourceStack, ?> wrap(ArgumentBuilder<CommandSourceStack, ?> argument, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> argumentAdder) {
                return argument.then(Commands.literal("storage").then(argumentAdder.apply(Commands.argument(argumentName, ResourceLocationArgument.id()).suggests(StorageDataAccessor.SUGGEST_STORAGE))));
            }
        };
    };
    private final CommandStorage storage;
    private final ResourceLocation id;

    static CommandStorage getGlobalTags(CommandContext<CommandSourceStack> context) {
        return context.getSource().getServer().getCommandStorage();
    }

    StorageDataAccessor(CommandStorage storage, ResourceLocation id) {
        this.storage = storage;
        this.id = id;
    }

    @Override
    public void setData(CompoundTag nbt) {
        this.storage.set(this.id, nbt);
    }

    @Override
    public CompoundTag getData() {
        return this.storage.get(this.id);
    }

    @Override
    public Component getModifiedSuccess() {
        return new TranslatableComponent("commands.data.storage.modified", this.id);
    }

    @Override
    public Component getPrintSuccess(Tag element) {
        return new TranslatableComponent("commands.data.storage.query", this.id, NbtUtils.toPrettyComponent(element));
    }

    @Override
    public Component getPrintSuccess(NbtPathArgument.NbtPath path, double scale, int result) {
        return new TranslatableComponent("commands.data.storage.get", path, this.id, String.format(Locale.ROOT, "%.2f", scale), result);
    }
}
