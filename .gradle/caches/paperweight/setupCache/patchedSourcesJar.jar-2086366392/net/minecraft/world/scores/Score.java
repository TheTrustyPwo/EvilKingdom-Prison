package net.minecraft.world.scores;

import java.util.Comparator;
import javax.annotation.Nullable;

public class Score {
    public static final Comparator<Score> SCORE_COMPARATOR = (a, b) -> {
        if (a.getScore() > b.getScore()) {
            return 1;
        } else {
            return a.getScore() < b.getScore() ? -1 : b.getOwner().compareToIgnoreCase(a.getOwner());
        }
    };
    private final Scoreboard scoreboard;
    @Nullable
    private final Objective objective;
    private final String owner;
    private int count;
    private boolean locked;
    private boolean forceUpdate;

    public Score(Scoreboard scoreboard, Objective objective, String playerName) {
        this.scoreboard = scoreboard;
        this.objective = objective;
        this.owner = playerName;
        this.locked = true;
        this.forceUpdate = true;
    }

    public void add(int amount) {
        if (this.objective.getCriteria().isReadOnly()) {
            throw new IllegalStateException("Cannot modify read-only score");
        } else {
            this.setScore(this.getScore() + amount);
        }
    }

    public void increment() {
        this.add(1);
    }

    public int getScore() {
        return this.count;
    }

    public void reset() {
        this.setScore(0);
    }

    public void setScore(int score) {
        int i = this.count;
        this.count = score;
        if (i != score || this.forceUpdate) {
            this.forceUpdate = false;
            this.getScoreboard().onScoreChanged(this);
        }

    }

    @Nullable
    public Objective getObjective() {
        return this.objective;
    }

    public String getOwner() {
        return this.owner;
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
