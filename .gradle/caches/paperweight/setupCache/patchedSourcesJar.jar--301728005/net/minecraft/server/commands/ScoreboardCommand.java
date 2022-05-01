package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ObjectiveCriteriaArgument;
import net.minecraft.commands.arguments.OperationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ScoreboardCommand {
    private static final SimpleCommandExceptionType ERROR_OBJECTIVE_ALREADY_EXISTS = new SimpleCommandExceptionType(new TranslatableComponent("commands.scoreboard.objectives.add.duplicate"));
    private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_EMPTY = new SimpleCommandExceptionType(new TranslatableComponent("commands.scoreboard.objectives.display.alreadyEmpty"));
    private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_SET = new SimpleCommandExceptionType(new TranslatableComponent("commands.scoreboard.objectives.display.alreadySet"));
    private static final SimpleCommandExceptionType ERROR_TRIGGER_ALREADY_ENABLED = new SimpleCommandExceptionType(new TranslatableComponent("commands.scoreboard.players.enable.failed"));
    private static final SimpleCommandExceptionType ERROR_NOT_TRIGGER = new SimpleCommandExceptionType(new TranslatableComponent("commands.scoreboard.players.enable.invalid"));
    private static final Dynamic2CommandExceptionType ERROR_NO_VALUE = new Dynamic2CommandExceptionType((objective, target) -> {
        return new TranslatableComponent("commands.scoreboard.players.get.null", objective, target);
    });

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scoreboard").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("objectives").then(Commands.literal("list").executes((context) -> {
            return listObjectives(context.getSource());
        })).then(Commands.literal("add").then(Commands.argument("objective", StringArgumentType.word()).then(Commands.argument("criteria", ObjectiveCriteriaArgument.criteria()).executes((context) -> {
            return addObjective(context.getSource(), StringArgumentType.getString(context, "objective"), ObjectiveCriteriaArgument.getCriteria(context, "criteria"), new TextComponent(StringArgumentType.getString(context, "objective")));
        }).then(Commands.argument("displayName", ComponentArgument.textComponent()).executes((context) -> {
            return addObjective(context.getSource(), StringArgumentType.getString(context, "objective"), ObjectiveCriteriaArgument.getCriteria(context, "criteria"), ComponentArgument.getComponent(context, "displayName"));
        }))))).then(Commands.literal("modify").then(Commands.argument("objective", ObjectiveArgument.objective()).then(Commands.literal("displayname").then(Commands.argument("displayName", ComponentArgument.textComponent()).executes((context) -> {
            return setDisplayName(context.getSource(), ObjectiveArgument.getObjective(context, "objective"), ComponentArgument.getComponent(context, "displayName"));
        }))).then(createRenderTypeModify()))).then(Commands.literal("remove").then(Commands.argument("objective", ObjectiveArgument.objective()).executes((context) -> {
            return removeObjective(context.getSource(), ObjectiveArgument.getObjective(context, "objective"));
        }))).then(Commands.literal("setdisplay").then(Commands.argument("slot", ScoreboardSlotArgument.displaySlot()).executes((context) -> {
            return clearDisplaySlot(context.getSource(), ScoreboardSlotArgument.getDisplaySlot(context, "slot"));
        }).then(Commands.argument("objective", ObjectiveArgument.objective()).executes((context) -> {
            return setDisplaySlot(context.getSource(), ScoreboardSlotArgument.getDisplaySlot(context, "slot"), ObjectiveArgument.getObjective(context, "objective"));
        }))))).then(Commands.literal("players").then(Commands.literal("list").executes((context) -> {
            return listTrackedPlayers(context.getSource());
        }).then(Commands.argument("target", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).executes((context) -> {
            return listTrackedPlayerScores(context.getSource(), ScoreHolderArgument.getName(context, "target"));
        }))).then(Commands.literal("set").then(Commands.argument("targets", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("objective", ObjectiveArgument.objective()).then(Commands.argument("score", IntegerArgumentType.integer()).executes((context) -> {
            return setScore(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "targets"), ObjectiveArgument.getWritableObjective(context, "objective"), IntegerArgumentType.getInteger(context, "score"));
        }))))).then(Commands.literal("get").then(Commands.argument("target", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("objective", ObjectiveArgument.objective()).executes((context) -> {
            return getScore(context.getSource(), ScoreHolderArgument.getName(context, "target"), ObjectiveArgument.getObjective(context, "objective"));
        })))).then(Commands.literal("add").then(Commands.argument("targets", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("objective", ObjectiveArgument.objective()).then(Commands.argument("score", IntegerArgumentType.integer(0)).executes((context) -> {
            return addScore(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "targets"), ObjectiveArgument.getWritableObjective(context, "objective"), IntegerArgumentType.getInteger(context, "score"));
        }))))).then(Commands.literal("remove").then(Commands.argument("targets", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("objective", ObjectiveArgument.objective()).then(Commands.argument("score", IntegerArgumentType.integer(0)).executes((context) -> {
            return removeScore(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "targets"), ObjectiveArgument.getWritableObjective(context, "objective"), IntegerArgumentType.getInteger(context, "score"));
        }))))).then(Commands.literal("reset").then(Commands.argument("targets", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).executes((context) -> {
            return resetScores(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "targets"));
        }).then(Commands.argument("objective", ObjectiveArgument.objective()).executes((context) -> {
            return resetScore(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "targets"), ObjectiveArgument.getObjective(context, "objective"));
        })))).then(Commands.literal("enable").then(Commands.argument("targets", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("objective", ObjectiveArgument.objective()).suggests((context, builder) -> {
            return suggestTriggers(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "targets"), builder);
        }).executes((context) -> {
            return enableTrigger(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "targets"), ObjectiveArgument.getObjective(context, "objective"));
        })))).then(Commands.literal("operation").then(Commands.argument("targets", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("targetObjective", ObjectiveArgument.objective()).then(Commands.argument("operation", OperationArgument.operation()).then(Commands.argument("source", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("sourceObjective", ObjectiveArgument.objective()).executes((context) -> {
            return performOperation(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "targets"), ObjectiveArgument.getWritableObjective(context, "targetObjective"), OperationArgument.getOperation(context, "operation"), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "source"), ObjectiveArgument.getObjective(context, "sourceObjective"));
        })))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRenderTypeModify() {
        LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("rendertype");

        for(ObjectiveCriteria.RenderType renderType : ObjectiveCriteria.RenderType.values()) {
            literalArgumentBuilder.then(Commands.literal(renderType.getId()).executes((context) -> {
                return setRenderType(context.getSource(), ObjectiveArgument.getObjective(context, "objective"), renderType);
            }));
        }

        return literalArgumentBuilder;
    }

    private static CompletableFuture<Suggestions> suggestTriggers(CommandSourceStack source, Collection<String> targets, SuggestionsBuilder builder) {
        List<String> list = Lists.newArrayList();
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(Objective objective : scoreboard.getObjectives()) {
            if (objective.getCriteria() == ObjectiveCriteria.TRIGGER) {
                boolean bl = false;

                for(String string : targets) {
                    if (!scoreboard.hasPlayerScore(string, objective) || scoreboard.getOrCreatePlayerScore(string, objective).isLocked()) {
                        bl = true;
                        break;
                    }
                }

                if (bl) {
                    list.add(objective.getName());
                }
            }
        }

        return SharedSuggestionProvider.suggest(list, builder);
    }

    private static int getScore(CommandSourceStack source, String target, Objective objective) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (!scoreboard.hasPlayerScore(target, objective)) {
            throw ERROR_NO_VALUE.create(objective.getName(), target);
        } else {
            Score score = scoreboard.getOrCreatePlayerScore(target, objective);
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.get.success", target, score.getScore(), objective.getFormattedDisplayName()), false);
            return score.getScore();
        }
    }

    private static int performOperation(CommandSourceStack source, Collection<String> targets, Objective targetObjective, OperationArgument.Operation operation, Collection<String> sources, Objective sourceObjectives) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int i = 0;

        for(String string : targets) {
            Score score = scoreboard.getOrCreatePlayerScore(string, targetObjective);

            for(String string2 : sources) {
                Score score2 = scoreboard.getOrCreatePlayerScore(string2, sourceObjectives);
                operation.apply(score, score2);
            }

            i += score.getScore();
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.operation.success.single", targetObjective.getFormattedDisplayName(), targets.iterator().next(), i), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.operation.success.multiple", targetObjective.getFormattedDisplayName(), targets.size()), true);
        }

        return i;
    }

    private static int enableTrigger(CommandSourceStack source, Collection<String> targets, Objective objective) throws CommandSyntaxException {
        if (objective.getCriteria() != ObjectiveCriteria.TRIGGER) {
            throw ERROR_NOT_TRIGGER.create();
        } else {
            Scoreboard scoreboard = source.getServer().getScoreboard();
            int i = 0;

            for(String string : targets) {
                Score score = scoreboard.getOrCreatePlayerScore(string, objective);
                if (score.isLocked()) {
                    score.setLocked(false);
                    ++i;
                }
            }

            if (i == 0) {
                throw ERROR_TRIGGER_ALREADY_ENABLED.create();
            } else {
                if (targets.size() == 1) {
                    source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.enable.success.single", objective.getFormattedDisplayName(), targets.iterator().next()), true);
                } else {
                    source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.enable.success.multiple", objective.getFormattedDisplayName(), targets.size()), true);
                }

                return i;
            }
        }
    }

    private static int resetScores(CommandSourceStack source, Collection<String> targets) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(String string : targets) {
            scoreboard.resetPlayerScore(string, (Objective)null);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.reset.all.single", targets.iterator().next()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.reset.all.multiple", targets.size()), true);
        }

        return targets.size();
    }

    private static int resetScore(CommandSourceStack source, Collection<String> targets, Objective objective) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(String string : targets) {
            scoreboard.resetPlayerScore(string, objective);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.reset.specific.single", objective.getFormattedDisplayName(), targets.iterator().next()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.reset.specific.multiple", objective.getFormattedDisplayName(), targets.size()), true);
        }

        return targets.size();
    }

    private static int setScore(CommandSourceStack source, Collection<String> targets, Objective objective, int score) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(String string : targets) {
            Score score2 = scoreboard.getOrCreatePlayerScore(string, objective);
            score2.setScore(score);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.set.success.single", objective.getFormattedDisplayName(), targets.iterator().next(), score), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.set.success.multiple", objective.getFormattedDisplayName(), targets.size(), score), true);
        }

        return score * targets.size();
    }

    private static int addScore(CommandSourceStack source, Collection<String> targets, Objective objective, int score) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int i = 0;

        for(String string : targets) {
            Score score2 = scoreboard.getOrCreatePlayerScore(string, objective);
            score2.setScore(score2.getScore() + score);
            i += score2.getScore();
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.add.success.single", score, objective.getFormattedDisplayName(), targets.iterator().next(), i), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.add.success.multiple", score, objective.getFormattedDisplayName(), targets.size()), true);
        }

        return i;
    }

    private static int removeScore(CommandSourceStack source, Collection<String> targets, Objective objective, int score) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        int i = 0;

        for(String string : targets) {
            Score score2 = scoreboard.getOrCreatePlayerScore(string, objective);
            score2.setScore(score2.getScore() - score);
            i += score2.getScore();
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.remove.success.single", score, objective.getFormattedDisplayName(), targets.iterator().next(), i), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.remove.success.multiple", score, objective.getFormattedDisplayName(), targets.size()), true);
        }

        return i;
    }

    private static int listTrackedPlayers(CommandSourceStack source) {
        Collection<String> collection = source.getServer().getScoreboard().getTrackedPlayers();
        if (collection.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.list.empty"), false);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.list.success", collection.size(), ComponentUtils.formatList(collection)), false);
        }

        return collection.size();
    }

    private static int listTrackedPlayerScores(CommandSourceStack source, String target) {
        Map<Objective, Score> map = source.getServer().getScoreboard().getPlayerScores(target);
        if (map.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.list.entity.empty", target), false);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.list.entity.success", target, map.size()), false);

            for(Entry<Objective, Score> entry : map.entrySet()) {
                source.sendSuccess(new TranslatableComponent("commands.scoreboard.players.list.entity.entry", entry.getKey().getFormattedDisplayName(), entry.getValue().getScore()), false);
            }
        }

        return map.size();
    }

    private static int clearDisplaySlot(CommandSourceStack source, int slot) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (scoreboard.getDisplayObjective(slot) == null) {
            throw ERROR_DISPLAY_SLOT_ALREADY_EMPTY.create();
        } else {
            scoreboard.setDisplayObjective(slot, (Objective)null);
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.objectives.display.cleared", Scoreboard.getDisplaySlotNames()[slot]), true);
            return 0;
        }
    }

    private static int setDisplaySlot(CommandSourceStack source, int slot, Objective objective) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (scoreboard.getDisplayObjective(slot) == objective) {
            throw ERROR_DISPLAY_SLOT_ALREADY_SET.create();
        } else {
            scoreboard.setDisplayObjective(slot, objective);
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.objectives.display.set", Scoreboard.getDisplaySlotNames()[slot], objective.getDisplayName()), true);
            return 0;
        }
    }

    private static int setDisplayName(CommandSourceStack source, Objective objective, Component displayName) {
        if (!objective.getDisplayName().equals(displayName)) {
            objective.setDisplayName(displayName);
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.objectives.modify.displayname", objective.getName(), objective.getFormattedDisplayName()), true);
        }

        return 0;
    }

    private static int setRenderType(CommandSourceStack source, Objective objective, ObjectiveCriteria.RenderType type) {
        if (objective.getRenderType() != type) {
            objective.setRenderType(type);
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.objectives.modify.rendertype", objective.getFormattedDisplayName()), true);
        }

        return 0;
    }

    private static int removeObjective(CommandSourceStack source, Objective objective) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        scoreboard.removeObjective(objective);
        source.sendSuccess(new TranslatableComponent("commands.scoreboard.objectives.remove.success", objective.getFormattedDisplayName()), true);
        return scoreboard.getObjectives().size();
    }

    private static int addObjective(CommandSourceStack source, String objective, ObjectiveCriteria criteria, Component displayName) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (scoreboard.getObjective(objective) != null) {
            throw ERROR_OBJECTIVE_ALREADY_EXISTS.create();
        } else {
            scoreboard.addObjective(objective, criteria, displayName, criteria.getDefaultRenderType());
            Objective objective2 = scoreboard.getObjective(objective);
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.objectives.add.success", objective2.getFormattedDisplayName()), true);
            return scoreboard.getObjectives().size();
        }
    }

    private static int listObjectives(CommandSourceStack source) {
        Collection<Objective> collection = source.getServer().getScoreboard().getObjectives();
        if (collection.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.objectives.list.empty"), false);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.scoreboard.objectives.list.success", collection.size(), ComponentUtils.formatList(collection, Objective::getFormattedDisplayName)), false);
        }

        return collection.size();
    }
}
