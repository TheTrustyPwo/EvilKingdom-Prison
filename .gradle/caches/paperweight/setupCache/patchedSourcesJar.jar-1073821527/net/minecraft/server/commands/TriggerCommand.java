package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class TriggerCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_PRIMED = new SimpleCommandExceptionType(new TranslatableComponent("commands.trigger.failed.unprimed"));
    private static final SimpleCommandExceptionType ERROR_INVALID_OBJECTIVE = new SimpleCommandExceptionType(new TranslatableComponent("commands.trigger.failed.invalid"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trigger").then(Commands.argument("objective", ObjectiveArgument.objective()).suggests((context, builder) -> {
            return suggestObjectives(context.getSource(), builder);
        }).executes((context) -> {
            return simpleTrigger(context.getSource(), getScore(context.getSource().getPlayerOrException(), ObjectiveArgument.getObjective(context, "objective")));
        }).then(Commands.literal("add").then(Commands.argument("value", IntegerArgumentType.integer()).executes((context) -> {
            return addValue(context.getSource(), getScore(context.getSource().getPlayerOrException(), ObjectiveArgument.getObjective(context, "objective")), IntegerArgumentType.getInteger(context, "value"));
        }))).then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer()).executes((context) -> {
            return setValue(context.getSource(), getScore(context.getSource().getPlayerOrException(), ObjectiveArgument.getObjective(context, "objective")), IntegerArgumentType.getInteger(context, "value"));
        })))));
    }

    public static CompletableFuture<Suggestions> suggestObjectives(CommandSourceStack source, SuggestionsBuilder builder) {
        Entity entity = source.getEntity();
        List<String> list = Lists.newArrayList();
        if (entity != null) {
            Scoreboard scoreboard = source.getServer().getScoreboard();
            String string = entity.getScoreboardName();

            for(Objective objective : scoreboard.getObjectives()) {
                if (objective.getCriteria() == ObjectiveCriteria.TRIGGER && scoreboard.hasPlayerScore(string, objective)) {
                    Score score = scoreboard.getOrCreatePlayerScore(string, objective);
                    if (!score.isLocked()) {
                        list.add(objective.getName());
                    }
                }
            }
        }

        return SharedSuggestionProvider.suggest(list, builder);
    }

    private static int addValue(CommandSourceStack source, Score score, int value) {
        score.add(value);
        source.sendSuccess(new TranslatableComponent("commands.trigger.add.success", score.getObjective().getFormattedDisplayName(), value), true);
        return score.getScore();
    }

    private static int setValue(CommandSourceStack source, Score score, int value) {
        score.setScore(value);
        source.sendSuccess(new TranslatableComponent("commands.trigger.set.success", score.getObjective().getFormattedDisplayName(), value), true);
        return value;
    }

    private static int simpleTrigger(CommandSourceStack source, Score score) {
        score.add(1);
        source.sendSuccess(new TranslatableComponent("commands.trigger.simple.success", score.getObjective().getFormattedDisplayName()), true);
        return score.getScore();
    }

    private static Score getScore(ServerPlayer player, Objective objective) throws CommandSyntaxException {
        if (objective.getCriteria() != ObjectiveCriteria.TRIGGER) {
            throw ERROR_INVALID_OBJECTIVE.create();
        } else {
            Scoreboard scoreboard = player.getScoreboard();
            String string = player.getScoreboardName();
            if (!scoreboard.hasPlayerScore(string, objective)) {
                throw ERROR_NOT_PRIMED.create();
            } else {
                Score score = scoreboard.getOrCreatePlayerScore(string, objective);
                if (score.isLocked()) {
                    throw ERROR_NOT_PRIMED.create();
                } else {
                    score.setLocked(true);
                    return score;
                }
            }
        }
    }
}
