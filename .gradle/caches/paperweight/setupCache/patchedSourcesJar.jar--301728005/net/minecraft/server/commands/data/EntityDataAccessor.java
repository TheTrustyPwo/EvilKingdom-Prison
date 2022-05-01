package net.minecraft.server.commands.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class EntityDataAccessor implements DataAccessor {
    private static final SimpleCommandExceptionType ERROR_NO_PLAYERS = new SimpleCommandExceptionType(new TranslatableComponent("commands.data.entity.invalid"));
    public static final Function<String, DataCommands.DataProvider> PROVIDER = (argumentName) -> {
        return new DataCommands.DataProvider() {
            @Override
            public DataAccessor access(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                return new EntityDataAccessor(EntityArgument.getEntity(context, argumentName));
            }

            @Override
            public ArgumentBuilder<CommandSourceStack, ?> wrap(ArgumentBuilder<CommandSourceStack, ?> argument, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> argumentAdder) {
                return argument.then(Commands.literal("entity").then(argumentAdder.apply(Commands.argument(argumentName, EntityArgument.entity()))));
            }
        };
    };
    private final Entity entity;

    public EntityDataAccessor(Entity entity) {
        this.entity = entity;
    }

    @Override
    public void setData(CompoundTag nbt) throws CommandSyntaxException {
        if (this.entity instanceof Player) {
            throw ERROR_NO_PLAYERS.create();
        } else {
            UUID uUID = this.entity.getUUID();
            this.entity.load(nbt);
            this.entity.setUUID(uUID);
        }
    }

    @Override
    public CompoundTag getData() {
        return NbtPredicate.getEntityTagToCompare(this.entity);
    }

    @Override
    public Component getModifiedSuccess() {
        return new TranslatableComponent("commands.data.entity.modified", this.entity.getDisplayName());
    }

    @Override
    public Component getPrintSuccess(Tag element) {
        return new TranslatableComponent("commands.data.entity.query", this.entity.getDisplayName(), NbtUtils.toPrettyComponent(element));
    }

    @Override
    public Component getPrintSuccess(NbtPathArgument.NbtPath path, double scale, int result) {
        return new TranslatableComponent("commands.data.entity.get", path, this.entity.getDisplayName(), String.format(Locale.ROOT, "%.2f", scale), result);
    }
}
