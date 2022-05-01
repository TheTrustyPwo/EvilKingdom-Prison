package net.minecraft.world.scores;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;

public class PlayerTeam extends Team {
    private static final int BIT_FRIENDLY_FIRE = 0;
    private static final int BIT_SEE_INVISIBLES = 1;
    private final Scoreboard scoreboard;
    private final String name;
    private final Set<String> players = Sets.newHashSet();
    private Component displayName;
    private Component playerPrefix = TextComponent.EMPTY;
    private Component playerSuffix = TextComponent.EMPTY;
    private boolean allowFriendlyFire = true;
    private boolean seeFriendlyInvisibles = true;
    private Team.Visibility nameTagVisibility = Team.Visibility.ALWAYS;
    private Team.Visibility deathMessageVisibility = Team.Visibility.ALWAYS;
    private ChatFormatting color = ChatFormatting.RESET;
    private Team.CollisionRule collisionRule = Team.CollisionRule.ALWAYS;
    private final Style displayNameStyle;

    public PlayerTeam(Scoreboard scoreboard, String name) {
        this.scoreboard = scoreboard;
        this.name = name;
        this.displayName = new TextComponent(name);
        this.displayNameStyle = Style.EMPTY.withInsertion(name).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(name)));
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public MutableComponent getFormattedDisplayName() {
        MutableComponent mutableComponent = ComponentUtils.wrapInSquareBrackets(this.displayName.copy().withStyle(this.displayNameStyle));
        ChatFormatting chatFormatting = this.getColor();
        if (chatFormatting != ChatFormatting.RESET) {
            mutableComponent.withStyle(chatFormatting);
        }

        return mutableComponent;
    }

    public void setDisplayName(Component displayName) {
        if (displayName == null) {
            throw new IllegalArgumentException("Name cannot be null");
        } else {
            this.displayName = displayName;
            this.scoreboard.onTeamChanged(this);
        }
    }

    public void setPlayerPrefix(@Nullable Component prefix) {
        this.playerPrefix = prefix == null ? TextComponent.EMPTY : prefix;
        this.scoreboard.onTeamChanged(this);
    }

    public Component getPlayerPrefix() {
        return this.playerPrefix;
    }

    public void setPlayerSuffix(@Nullable Component suffix) {
        this.playerSuffix = suffix == null ? TextComponent.EMPTY : suffix;
        this.scoreboard.onTeamChanged(this);
    }

    public Component getPlayerSuffix() {
        return this.playerSuffix;
    }

    @Override
    public Collection<String> getPlayers() {
        return this.players;
    }

    @Override
    public MutableComponent getFormattedName(Component name) {
        MutableComponent mutableComponent = (new TextComponent("")).append(this.playerPrefix).append(name).append(this.playerSuffix);
        ChatFormatting chatFormatting = this.getColor();
        if (chatFormatting != ChatFormatting.RESET) {
            mutableComponent.withStyle(chatFormatting);
        }

        return mutableComponent;
    }

    public static MutableComponent formatNameForTeam(@Nullable Team team, Component name) {
        return team == null ? name.copy() : team.getFormattedName(name);
    }

    @Override
    public boolean isAllowFriendlyFire() {
        return this.allowFriendlyFire;
    }

    public void setAllowFriendlyFire(boolean friendlyFire) {
        this.allowFriendlyFire = friendlyFire;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public boolean canSeeFriendlyInvisibles() {
        return this.seeFriendlyInvisibles;
    }

    public void setSeeFriendlyInvisibles(boolean showFriendlyInvisible) {
        this.seeFriendlyInvisibles = showFriendlyInvisible;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public Team.Visibility getNameTagVisibility() {
        return this.nameTagVisibility;
    }

    @Override
    public Team.Visibility getDeathMessageVisibility() {
        return this.deathMessageVisibility;
    }

    public void setNameTagVisibility(Team.Visibility nameTagVisibilityRule) {
        this.nameTagVisibility = nameTagVisibilityRule;
        this.scoreboard.onTeamChanged(this);
    }

    public void setDeathMessageVisibility(Team.Visibility deathMessageVisibilityRule) {
        this.deathMessageVisibility = deathMessageVisibilityRule;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public Team.CollisionRule getCollisionRule() {
        return this.collisionRule;
    }

    public void setCollisionRule(Team.CollisionRule collisionRule) {
        this.collisionRule = collisionRule;
        this.scoreboard.onTeamChanged(this);
    }

    public int packOptions() {
        int i = 0;
        if (this.isAllowFriendlyFire()) {
            i |= 1;
        }

        if (this.canSeeFriendlyInvisibles()) {
            i |= 2;
        }

        return i;
    }

    public void unpackOptions(int flags) {
        this.setAllowFriendlyFire((flags & 1) > 0);
        this.setSeeFriendlyInvisibles((flags & 2) > 0);
    }

    public void setColor(ChatFormatting color) {
        this.color = color;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public ChatFormatting getColor() {
        return this.color;
    }
}
