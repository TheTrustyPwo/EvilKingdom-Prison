package net.minecraft.server.commands;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;

public class TagCommand {
    private static final SimpleCommandExceptionType ERROR_ADD_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.tag.add.failed"));
    private static final SimpleCommandExceptionType ERROR_REMOVE_FAILED = new SimpleCommandExceptionType(new TranslatableComponent("commands.tag.remove.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tag").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.argument("targets", EntityArgument.entities()).then(Commands.literal("add").then(Commands.argument("name", StringArgumentType.word()).executes((context) -> {
            return addTag(context.getSource(), EntityArgument.getEntities(context, "targets"), StringArgumentType.getString(context, "name"));
        }))).then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word()).suggests((context, builder) -> {
            return SharedSuggestionProvider.suggest(getTags(EntityArgument.getEntities(context, "targets")), builder);
        }).executes((context) -> {
            return removeTag(context.getSource(), EntityArgument.getEntities(context, "targets"), StringArgumentType.getString(context, "name"));
        }))).then(Commands.literal("list").executes((context) -> {
            return listTags(context.getSource(), EntityArgument.getEntities(context, "targets"));
        }))));
    }

    private static Collection<String> getTags(Collection<? extends Entity> entities) {
        Set<String> set = Sets.newHashSet();

        for(Entity entity : entities) {
            set.addAll(entity.getTags());
        }

        return set;
    }

    private static int addTag(CommandSourceStack source, Collection<? extends Entity> targets, String tag) throws CommandSyntaxException {
        int i = 0;

        for(Entity entity : targets) {
            if (entity.addTag(tag)) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_ADD_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendSuccess(new TranslatableComponent("commands.tag.add.success.single", tag, targets.iterator().next().getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.tag.add.success.multiple", tag, targets.size()), true);
            }

            return i;
        }
    }

    private static int removeTag(CommandSourceStack source, Collection<? extends Entity> targets, String tag) throws CommandSyntaxException {
        int i = 0;

        for(Entity entity : targets) {
            if (entity.removeTag(tag)) {
                ++i;
            }
        }

        if (i == 0) {
            throw ERROR_REMOVE_FAILED.create();
        } else {
            if (targets.size() == 1) {
                source.sendSuccess(new TranslatableComponent("commands.tag.remove.success.single", tag, targets.iterator().next().getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.tag.remove.success.multiple", tag, targets.size()), true);
            }

            return i;
        }
    }

    private static int listTags(CommandSourceStack source, Collection<? extends Entity> targets) {
        Set<String> set = Sets.newHashSet();

        for(Entity entity : targets) {
            set.addAll(entity.getTags());
        }

        if (targets.size() == 1) {
            Entity entity2 = targets.iterator().next();
            if (set.isEmpty()) {
                source.sendSuccess(new TranslatableComponent("commands.tag.list.single.empty", entity2.getDisplayName()), false);
            } else {
                source.sendSuccess(new TranslatableComponent("commands.tag.list.single.success", entity2.getDisplayName(), set.size(), ComponentUtils.formatList(set)), false);
            }
        } else if (set.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("commands.tag.list.multiple.empty", targets.size()), false);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.tag.list.multiple.success", targets.size(), set.size(), ComponentUtils.formatList(set)), false);
        }

        return set.size();
    }
}
