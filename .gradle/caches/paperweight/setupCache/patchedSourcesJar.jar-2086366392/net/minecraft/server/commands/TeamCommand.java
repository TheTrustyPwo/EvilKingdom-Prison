package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class TeamCommand {
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_EXISTS = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.add.duplicate"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_EMPTY = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.empty.unchanged"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_NAME = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.option.name.unchanged"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_COLOR = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.option.color.unchanged"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYFIRE_ENABLED = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.option.friendlyfire.alreadyEnabled"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYFIRE_DISABLED = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.option.friendlyfire.alreadyDisabled"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_ENABLED = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.option.seeFriendlyInvisibles.alreadyEnabled"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_DISABLED = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.option.seeFriendlyInvisibles.alreadyDisabled"));
    private static final SimpleCommandExceptionType ERROR_TEAM_NAMETAG_VISIBLITY_UNCHANGED = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.option.nametagVisibility.unchanged"));
    private static final SimpleCommandExceptionType ERROR_TEAM_DEATH_MESSAGE_VISIBLITY_UNCHANGED = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.option.deathMessageVisibility.unchanged"));
    private static final SimpleCommandExceptionType ERROR_TEAM_COLLISION_UNCHANGED = new SimpleCommandExceptionType(new TranslatableComponent("commands.team.option.collisionRule.unchanged"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("team").requires((source) -> {
            return source.hasPermission(2);
        }).then(Commands.literal("list").executes((context) -> {
            return listTeams(context.getSource());
        }).then(Commands.argument("team", TeamArgument.team()).executes((context) -> {
            return listMembers(context.getSource(), TeamArgument.getTeam(context, "team"));
        }))).then(Commands.literal("add").then(Commands.argument("team", StringArgumentType.word()).executes((context) -> {
            return createTeam(context.getSource(), StringArgumentType.getString(context, "team"));
        }).then(Commands.argument("displayName", ComponentArgument.textComponent()).executes((context) -> {
            return createTeam(context.getSource(), StringArgumentType.getString(context, "team"), ComponentArgument.getComponent(context, "displayName"));
        })))).then(Commands.literal("remove").then(Commands.argument("team", TeamArgument.team()).executes((context) -> {
            return deleteTeam(context.getSource(), TeamArgument.getTeam(context, "team"));
        }))).then(Commands.literal("empty").then(Commands.argument("team", TeamArgument.team()).executes((context) -> {
            return emptyTeam(context.getSource(), TeamArgument.getTeam(context, "team"));
        }))).then(Commands.literal("join").then(Commands.argument("team", TeamArgument.team()).executes((context) -> {
            return joinTeam(context.getSource(), TeamArgument.getTeam(context, "team"), Collections.singleton(context.getSource().getEntityOrException().getScoreboardName()));
        }).then(Commands.argument("members", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).executes((context) -> {
            return joinTeam(context.getSource(), TeamArgument.getTeam(context, "team"), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "members"));
        })))).then(Commands.literal("leave").then(Commands.argument("members", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).executes((context) -> {
            return leaveTeam(context.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(context, "members"));
        }))).then(Commands.literal("modify").then(Commands.argument("team", TeamArgument.team()).then(Commands.literal("displayName").then(Commands.argument("displayName", ComponentArgument.textComponent()).executes((context) -> {
            return setDisplayName(context.getSource(), TeamArgument.getTeam(context, "team"), ComponentArgument.getComponent(context, "displayName"));
        }))).then(Commands.literal("color").then(Commands.argument("value", ColorArgument.color()).executes((context) -> {
            return setColor(context.getSource(), TeamArgument.getTeam(context, "team"), ColorArgument.getColor(context, "value"));
        }))).then(Commands.literal("friendlyFire").then(Commands.argument("allowed", BoolArgumentType.bool()).executes((context) -> {
            return setFriendlyFire(context.getSource(), TeamArgument.getTeam(context, "team"), BoolArgumentType.getBool(context, "allowed"));
        }))).then(Commands.literal("seeFriendlyInvisibles").then(Commands.argument("allowed", BoolArgumentType.bool()).executes((context) -> {
            return setFriendlySight(context.getSource(), TeamArgument.getTeam(context, "team"), BoolArgumentType.getBool(context, "allowed"));
        }))).then(Commands.literal("nametagVisibility").then(Commands.literal("never").executes((context) -> {
            return setNametagVisibility(context.getSource(), TeamArgument.getTeam(context, "team"), Team.Visibility.NEVER);
        })).then(Commands.literal("hideForOtherTeams").executes((context) -> {
            return setNametagVisibility(context.getSource(), TeamArgument.getTeam(context, "team"), Team.Visibility.HIDE_FOR_OTHER_TEAMS);
        })).then(Commands.literal("hideForOwnTeam").executes((context) -> {
            return setNametagVisibility(context.getSource(), TeamArgument.getTeam(context, "team"), Team.Visibility.HIDE_FOR_OWN_TEAM);
        })).then(Commands.literal("always").executes((context) -> {
            return setNametagVisibility(context.getSource(), TeamArgument.getTeam(context, "team"), Team.Visibility.ALWAYS);
        }))).then(Commands.literal("deathMessageVisibility").then(Commands.literal("never").executes((context) -> {
            return setDeathMessageVisibility(context.getSource(), TeamArgument.getTeam(context, "team"), Team.Visibility.NEVER);
        })).then(Commands.literal("hideForOtherTeams").executes((context) -> {
            return setDeathMessageVisibility(context.getSource(), TeamArgument.getTeam(context, "team"), Team.Visibility.HIDE_FOR_OTHER_TEAMS);
        })).then(Commands.literal("hideForOwnTeam").executes((context) -> {
            return setDeathMessageVisibility(context.getSource(), TeamArgument.getTeam(context, "team"), Team.Visibility.HIDE_FOR_OWN_TEAM);
        })).then(Commands.literal("always").executes((context) -> {
            return setDeathMessageVisibility(context.getSource(), TeamArgument.getTeam(context, "team"), Team.Visibility.ALWAYS);
        }))).then(Commands.literal("collisionRule").then(Commands.literal("never").executes((context) -> {
            return setCollision(context.getSource(), TeamArgument.getTeam(context, "team"), Team.CollisionRule.NEVER);
        })).then(Commands.literal("pushOwnTeam").executes((context) -> {
            return setCollision(context.getSource(), TeamArgument.getTeam(context, "team"), Team.CollisionRule.PUSH_OWN_TEAM);
        })).then(Commands.literal("pushOtherTeams").executes((context) -> {
            return setCollision(context.getSource(), TeamArgument.getTeam(context, "team"), Team.CollisionRule.PUSH_OTHER_TEAMS);
        })).then(Commands.literal("always").executes((context) -> {
            return setCollision(context.getSource(), TeamArgument.getTeam(context, "team"), Team.CollisionRule.ALWAYS);
        }))).then(Commands.literal("prefix").then(Commands.argument("prefix", ComponentArgument.textComponent()).executes((context) -> {
            return setPrefix(context.getSource(), TeamArgument.getTeam(context, "team"), ComponentArgument.getComponent(context, "prefix"));
        }))).then(Commands.literal("suffix").then(Commands.argument("suffix", ComponentArgument.textComponent()).executes((context) -> {
            return setSuffix(context.getSource(), TeamArgument.getTeam(context, "team"), ComponentArgument.getComponent(context, "suffix"));
        }))))));
    }

    private static int leaveTeam(CommandSourceStack source, Collection<String> members) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(String string : members) {
            scoreboard.removePlayerFromTeam(string);
        }

        if (members.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.team.leave.success.single", members.iterator().next()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.team.leave.success.multiple", members.size()), true);
        }

        return members.size();
    }

    private static int joinTeam(CommandSourceStack source, PlayerTeam team, Collection<String> members) {
        Scoreboard scoreboard = source.getServer().getScoreboard();

        for(String string : members) {
            scoreboard.addPlayerToTeam(string, team);
        }

        if (members.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.team.join.success.single", members.iterator().next(), team.getFormattedDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.team.join.success.multiple", members.size(), team.getFormattedDisplayName()), true);
        }

        return members.size();
    }

    private static int setNametagVisibility(CommandSourceStack source, PlayerTeam team, Team.Visibility visibility) throws CommandSyntaxException {
        if (team.getNameTagVisibility() == visibility) {
            throw ERROR_TEAM_NAMETAG_VISIBLITY_UNCHANGED.create();
        } else {
            team.setNameTagVisibility(visibility);
            source.sendSuccess(new TranslatableComponent("commands.team.option.nametagVisibility.success", team.getFormattedDisplayName(), visibility.getDisplayName()), true);
            return 0;
        }
    }

    private static int setDeathMessageVisibility(CommandSourceStack source, PlayerTeam team, Team.Visibility visibility) throws CommandSyntaxException {
        if (team.getDeathMessageVisibility() == visibility) {
            throw ERROR_TEAM_DEATH_MESSAGE_VISIBLITY_UNCHANGED.create();
        } else {
            team.setDeathMessageVisibility(visibility);
            source.sendSuccess(new TranslatableComponent("commands.team.option.deathMessageVisibility.success", team.getFormattedDisplayName(), visibility.getDisplayName()), true);
            return 0;
        }
    }

    private static int setCollision(CommandSourceStack source, PlayerTeam team, Team.CollisionRule collisionRule) throws CommandSyntaxException {
        if (team.getCollisionRule() == collisionRule) {
            throw ERROR_TEAM_COLLISION_UNCHANGED.create();
        } else {
            team.setCollisionRule(collisionRule);
            source.sendSuccess(new TranslatableComponent("commands.team.option.collisionRule.success", team.getFormattedDisplayName(), collisionRule.getDisplayName()), true);
            return 0;
        }
    }

    private static int setFriendlySight(CommandSourceStack source, PlayerTeam team, boolean allowed) throws CommandSyntaxException {
        if (team.canSeeFriendlyInvisibles() == allowed) {
            if (allowed) {
                throw ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_ENABLED.create();
            } else {
                throw ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_DISABLED.create();
            }
        } else {
            team.setSeeFriendlyInvisibles(allowed);
            source.sendSuccess(new TranslatableComponent("commands.team.option.seeFriendlyInvisibles." + (allowed ? "enabled" : "disabled"), team.getFormattedDisplayName()), true);
            return 0;
        }
    }

    private static int setFriendlyFire(CommandSourceStack source, PlayerTeam team, boolean allowed) throws CommandSyntaxException {
        if (team.isAllowFriendlyFire() == allowed) {
            if (allowed) {
                throw ERROR_TEAM_ALREADY_FRIENDLYFIRE_ENABLED.create();
            } else {
                throw ERROR_TEAM_ALREADY_FRIENDLYFIRE_DISABLED.create();
            }
        } else {
            team.setAllowFriendlyFire(allowed);
            source.sendSuccess(new TranslatableComponent("commands.team.option.friendlyfire." + (allowed ? "enabled" : "disabled"), team.getFormattedDisplayName()), true);
            return 0;
        }
    }

    private static int setDisplayName(CommandSourceStack source, PlayerTeam team, Component displayName) throws CommandSyntaxException {
        if (team.getDisplayName().equals(displayName)) {
            throw ERROR_TEAM_ALREADY_NAME.create();
        } else {
            team.setDisplayName(displayName);
            source.sendSuccess(new TranslatableComponent("commands.team.option.name.success", team.getFormattedDisplayName()), true);
            return 0;
        }
    }

    private static int setColor(CommandSourceStack source, PlayerTeam team, ChatFormatting color) throws CommandSyntaxException {
        if (team.getColor() == color) {
            throw ERROR_TEAM_ALREADY_COLOR.create();
        } else {
            team.setColor(color);
            source.sendSuccess(new TranslatableComponent("commands.team.option.color.success", team.getFormattedDisplayName(), color.getName()), true);
            return 0;
        }
    }

    private static int emptyTeam(CommandSourceStack source, PlayerTeam team) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        Collection<String> collection = Lists.newArrayList(team.getPlayers());
        if (collection.isEmpty()) {
            throw ERROR_TEAM_ALREADY_EMPTY.create();
        } else {
            for(String string : collection) {
                scoreboard.removePlayerFromTeam(string, team);
            }

            source.sendSuccess(new TranslatableComponent("commands.team.empty.success", collection.size(), team.getFormattedDisplayName()), true);
            return collection.size();
        }
    }

    private static int deleteTeam(CommandSourceStack source, PlayerTeam team) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        scoreboard.removePlayerTeam(team);
        source.sendSuccess(new TranslatableComponent("commands.team.remove.success", team.getFormattedDisplayName()), true);
        return scoreboard.getPlayerTeams().size();
    }

    private static int createTeam(CommandSourceStack source, String team) throws CommandSyntaxException {
        return createTeam(source, team, new TextComponent(team));
    }

    private static int createTeam(CommandSourceStack source, String team, Component displayName) throws CommandSyntaxException {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        if (scoreboard.getPlayerTeam(team) != null) {
            throw ERROR_TEAM_ALREADY_EXISTS.create();
        } else {
            PlayerTeam playerTeam = scoreboard.addPlayerTeam(team);
            playerTeam.setDisplayName(displayName);
            source.sendSuccess(new TranslatableComponent("commands.team.add.success", playerTeam.getFormattedDisplayName()), true);
            return scoreboard.getPlayerTeams().size();
        }
    }

    private static int listMembers(CommandSourceStack source, PlayerTeam team) {
        Collection<String> collection = team.getPlayers();
        if (collection.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("commands.team.list.members.empty", team.getFormattedDisplayName()), false);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.team.list.members.success", team.getFormattedDisplayName(), collection.size(), ComponentUtils.formatList(collection)), false);
        }

        return collection.size();
    }

    private static int listTeams(CommandSourceStack source) {
        Collection<PlayerTeam> collection = source.getServer().getScoreboard().getPlayerTeams();
        if (collection.isEmpty()) {
            source.sendSuccess(new TranslatableComponent("commands.team.list.teams.empty"), false);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.team.list.teams.success", collection.size(), ComponentUtils.formatList(collection, PlayerTeam::getFormattedDisplayName)), false);
        }

        return collection.size();
    }

    private static int setPrefix(CommandSourceStack source, PlayerTeam team, Component prefix) {
        team.setPlayerPrefix(prefix);
        source.sendSuccess(new TranslatableComponent("commands.team.option.prefix.success", prefix), false);
        return 1;
    }

    private static int setSuffix(CommandSourceStack source, PlayerTeam team, Component suffix) {
        team.setPlayerSuffix(suffix);
        source.sendSuccess(new TranslatableComponent("commands.team.option.suffix.success", suffix), false);
        return 1;
    }
}
