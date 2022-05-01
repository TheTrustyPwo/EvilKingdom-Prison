package net.minecraft.world.scores;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class Scoreboard {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int DISPLAY_SLOT_LIST = 0;
    public static final int DISPLAY_SLOT_SIDEBAR = 1;
    public static final int DISPLAY_SLOT_BELOW_NAME = 2;
    public static final int DISPLAY_SLOT_TEAMS_SIDEBAR_START = 3;
    public static final int DISPLAY_SLOT_TEAMS_SIDEBAR_END = 18;
    public static final int DISPLAY_SLOTS = 19;
    private final Map<String, Objective> objectivesByName = Maps.newHashMap();
    private final Map<ObjectiveCriteria, List<Objective>> objectivesByCriteria = Maps.newHashMap();
    private final Map<String, Map<Objective, Score>> playerScores = Maps.newHashMap();
    private final Objective[] displayObjectives = new Objective[19];
    private final Map<String, PlayerTeam> teamsByName = Maps.newHashMap();
    private final Map<String, PlayerTeam> teamsByPlayer = Maps.newHashMap();
    @Nullable
    private static String[] displaySlotNames;

    public boolean hasObjective(String name) {
        return this.objectivesByName.containsKey(name);
    }

    public Objective getOrCreateObjective(String name) {
        return this.objectivesByName.get(name);
    }

    @Nullable
    public Objective getObjective(@Nullable String name) {
        return this.objectivesByName.get(name);
    }

    public Objective addObjective(String name, ObjectiveCriteria criterion, Component displayName, ObjectiveCriteria.RenderType renderType) {
        if (this.objectivesByName.containsKey(name)) {
            throw new IllegalArgumentException("An objective with the name '" + name + "' already exists!");
        } else {
            Objective objective = new Objective(this, name, criterion, displayName, renderType);
            this.objectivesByCriteria.computeIfAbsent(criterion, (criterionx) -> {
                return Lists.newArrayList();
            }).add(objective);
            this.objectivesByName.put(name, objective);
            this.onObjectiveAdded(objective);
            return objective;
        }
    }

    public final void forAllObjectives(ObjectiveCriteria criterion, String player, Consumer<Score> action) {
        this.objectivesByCriteria.getOrDefault(criterion, Collections.emptyList()).forEach((objective) -> {
            action.accept(this.getOrCreatePlayerScore(player, objective));
        });
    }

    public boolean hasPlayerScore(String playerName, Objective objective) {
        Map<Objective, Score> map = this.playerScores.get(playerName);
        if (map == null) {
            return false;
        } else {
            Score score = map.get(objective);
            return score != null;
        }
    }

    public Score getOrCreatePlayerScore(String playerName, Objective objective) {
        Map<Objective, Score> map = this.playerScores.computeIfAbsent(playerName, (string) -> {
            return Maps.newHashMap();
        });
        return map.computeIfAbsent(objective, (objectivex) -> {
            Score score = new Score(this, objectivex, playerName);
            score.setScore(0);
            return score;
        });
    }

    public Collection<Score> getPlayerScores(Objective objective) {
        List<Score> list = Lists.newArrayList();

        for(Map<Objective, Score> map : this.playerScores.values()) {
            Score score = map.get(objective);
            if (score != null) {
                list.add(score);
            }
        }

        list.sort(Score.SCORE_COMPARATOR);
        return list;
    }

    public Collection<Objective> getObjectives() {
        return this.objectivesByName.values();
    }

    public Collection<String> getObjectiveNames() {
        return this.objectivesByName.keySet();
    }

    public Collection<String> getTrackedPlayers() {
        return Lists.newArrayList(this.playerScores.keySet());
    }

    public void resetPlayerScore(String playerName, @Nullable Objective objective) {
        if (objective == null) {
            Map<Objective, Score> map = this.playerScores.remove(playerName);
            if (map != null) {
                this.onPlayerRemoved(playerName);
            }
        } else {
            Map<Objective, Score> map2 = this.playerScores.get(playerName);
            if (map2 != null) {
                Score score = map2.remove(objective);
                if (map2.size() < 1) {
                    Map<Objective, Score> map3 = this.playerScores.remove(playerName);
                    if (map3 != null) {
                        this.onPlayerRemoved(playerName);
                    }
                } else if (score != null) {
                    this.onPlayerScoreRemoved(playerName, objective);
                }
            }
        }

    }

    public Map<Objective, Score> getPlayerScores(String playerName) {
        Map<Objective, Score> map = this.playerScores.get(playerName);
        if (map == null) {
            map = Maps.newHashMap();
        }

        return map;
    }

    public void removeObjective(Objective objective) {
        this.objectivesByName.remove(objective.getName());

        for(int i = 0; i < 19; ++i) {
            if (this.getDisplayObjective(i) == objective) {
                this.setDisplayObjective(i, (Objective)null);
            }
        }

        List<Objective> list = this.objectivesByCriteria.get(objective.getCriteria());
        if (list != null) {
            list.remove(objective);
        }

        for(Map<Objective, Score> map : this.playerScores.values()) {
            map.remove(objective);
        }

        this.onObjectiveRemoved(objective);
    }

    public void setDisplayObjective(int slot, @Nullable Objective objective) {
        this.displayObjectives[slot] = objective;
    }

    @Nullable
    public Objective getDisplayObjective(int slot) {
        return this.displayObjectives[slot];
    }

    @Nullable
    public PlayerTeam getPlayerTeam(String name) {
        return this.teamsByName.get(name);
    }

    public PlayerTeam addPlayerTeam(String name) {
        PlayerTeam playerTeam = this.getPlayerTeam(name);
        if (playerTeam != null) {
            LOGGER.warn("Requested creation of existing team '{}'", (Object)name);
            return playerTeam;
        } else {
            playerTeam = new PlayerTeam(this, name);
            this.teamsByName.put(name, playerTeam);
            this.onTeamAdded(playerTeam);
            return playerTeam;
        }
    }

    public void removePlayerTeam(PlayerTeam team) {
        this.teamsByName.remove(team.getName());

        for(String string : team.getPlayers()) {
            this.teamsByPlayer.remove(string);
        }

        this.onTeamRemoved(team);
    }

    public boolean addPlayerToTeam(String playerName, PlayerTeam team) {
        if (this.getPlayersTeam(playerName) != null) {
            this.removePlayerFromTeam(playerName);
        }

        this.teamsByPlayer.put(playerName, team);
        return team.getPlayers().add(playerName);
    }

    public boolean removePlayerFromTeam(String playerName) {
        PlayerTeam playerTeam = this.getPlayersTeam(playerName);
        if (playerTeam != null) {
            this.removePlayerFromTeam(playerName, playerTeam);
            return true;
        } else {
            return false;
        }
    }

    public void removePlayerFromTeam(String playerName, PlayerTeam team) {
        if (this.getPlayersTeam(playerName) != team) {
            throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + team.getName() + "'.");
        } else {
            this.teamsByPlayer.remove(playerName);
            team.getPlayers().remove(playerName);
        }
    }

    public Collection<String> getTeamNames() {
        return this.teamsByName.keySet();
    }

    public Collection<PlayerTeam> getPlayerTeams() {
        return this.teamsByName.values();
    }

    @Nullable
    public PlayerTeam getPlayersTeam(String playerName) {
        return this.teamsByPlayer.get(playerName);
    }

    public void onObjectiveAdded(Objective objective) {
    }

    public void onObjectiveChanged(Objective objective) {
    }

    public void onObjectiveRemoved(Objective objective) {
    }

    public void onScoreChanged(Score score) {
    }

    public void onPlayerRemoved(String playerName) {
    }

    public void onPlayerScoreRemoved(String playerName, Objective objective) {
    }

    public void onTeamAdded(PlayerTeam team) {
    }

    public void onTeamChanged(PlayerTeam team) {
    }

    public void onTeamRemoved(PlayerTeam team) {
    }

    public static String getDisplaySlotName(int slotId) {
        switch(slotId) {
        case 0:
            return "list";
        case 1:
            return "sidebar";
        case 2:
            return "belowName";
        default:
            if (slotId >= 3 && slotId <= 18) {
                ChatFormatting chatFormatting = ChatFormatting.getById(slotId - 3);
                if (chatFormatting != null && chatFormatting != ChatFormatting.RESET) {
                    return "sidebar.team." + chatFormatting.getName();
                }
            }

            return null;
        }
    }

    public static int getDisplaySlotByName(String slotName) {
        if ("list".equalsIgnoreCase(slotName)) {
            return 0;
        } else if ("sidebar".equalsIgnoreCase(slotName)) {
            return 1;
        } else if ("belowName".equalsIgnoreCase(slotName)) {
            return 2;
        } else {
            if (slotName.startsWith("sidebar.team.")) {
                String string = slotName.substring("sidebar.team.".length());
                ChatFormatting chatFormatting = ChatFormatting.getByName(string);
                if (chatFormatting != null && chatFormatting.getId() >= 0) {
                    return chatFormatting.getId() + 3;
                }
            }

            return -1;
        }
    }

    public static String[] getDisplaySlotNames() {
        if (displaySlotNames == null) {
            displaySlotNames = new String[19];

            for(int i = 0; i < 19; ++i) {
                displaySlotNames[i] = getDisplaySlotName(i);
            }
        }

        return displaySlotNames;
    }

    public void entityRemoved(Entity entity) {
        if (entity != null && !(entity instanceof Player) && !entity.isAlive()) {
            String string = entity.getStringUUID();
            this.resetPlayerScore(string, (Objective)null);
            this.removePlayerFromTeam(string);
        }
    }

    protected ListTag savePlayerScores() {
        ListTag listTag = new ListTag();
        this.playerScores.values().stream().map(Map::values).forEach((scores) -> {
            scores.stream().filter((score) -> {
                return score.getObjective() != null;
            }).forEach((score) -> {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putString("Name", score.getOwner());
                compoundTag.putString("Objective", score.getObjective().getName());
                compoundTag.putInt("Score", score.getScore());
                compoundTag.putBoolean("Locked", score.isLocked());
                listTag.add(compoundTag);
            });
        });
        return listTag;
    }

    protected void loadPlayerScores(ListTag list) {
        for(int i = 0; i < list.size(); ++i) {
            CompoundTag compoundTag = list.getCompound(i);
            Objective objective = this.getOrCreateObjective(compoundTag.getString("Objective"));
            String string = compoundTag.getString("Name");
            Score score = this.getOrCreatePlayerScore(string, objective);
            score.setScore(compoundTag.getInt("Score"));
            if (compoundTag.contains("Locked")) {
                score.setLocked(compoundTag.getBoolean("Locked"));
            }
        }

    }
}
