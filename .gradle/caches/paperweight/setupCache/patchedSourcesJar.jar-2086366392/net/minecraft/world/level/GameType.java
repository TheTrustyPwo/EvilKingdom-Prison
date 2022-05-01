package net.minecraft.world.level;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Abilities;

public enum GameType {
    SURVIVAL(0, "survival"),
    CREATIVE(1, "creative"),
    ADVENTURE(2, "adventure"),
    SPECTATOR(3, "spectator");

    public static final GameType DEFAULT_MODE = SURVIVAL;
    private static final int NOT_SET = -1;
    private final int id;
    private final String name;
    private final Component shortName;
    private final Component longName;

    private GameType(int id, String name) {
        this.id = id;
        this.name = name;
        this.shortName = new TranslatableComponent("selectWorld.gameMode." + name);
        this.longName = new TranslatableComponent("gameMode." + name);
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Component getLongDisplayName() {
        return this.longName;
    }

    public Component getShortDisplayName() {
        return this.shortName;
    }

    public void updatePlayerAbilities(Abilities abilities) {
        if (this == CREATIVE) {
            abilities.mayfly = true;
            abilities.instabuild = true;
            abilities.invulnerable = true;
        } else if (this == SPECTATOR) {
            abilities.mayfly = true;
            abilities.instabuild = false;
            abilities.invulnerable = true;
            abilities.flying = true;
        } else {
            abilities.mayfly = false;
            abilities.instabuild = false;
            abilities.invulnerable = false;
            abilities.flying = false;
        }

        abilities.mayBuild = !this.isBlockPlacingRestricted();
    }

    public boolean isBlockPlacingRestricted() {
        return this == ADVENTURE || this == SPECTATOR;
    }

    public boolean isCreative() {
        return this == CREATIVE;
    }

    public boolean isSurvival() {
        return this == SURVIVAL || this == ADVENTURE;
    }

    public static GameType byId(int id) {
        return byId(id, DEFAULT_MODE);
    }

    public static GameType byId(int id, GameType defaultMode) {
        for(GameType gameType : values()) {
            if (gameType.id == id) {
                return gameType;
            }
        }

        return defaultMode;
    }

    public static GameType byName(String name) {
        return byName(name, SURVIVAL);
    }

    public static GameType byName(String name, GameType defaultMode) {
        for(GameType gameType : values()) {
            if (gameType.name.equals(name)) {
                return gameType;
            }
        }

        return defaultMode;
    }

    public static int getNullableId(@Nullable GameType gameMode) {
        return gameMode != null ? gameMode.id : -1;
    }

    @Nullable
    public static GameType byNullableId(int id) {
        return id == -1 ? null : byId(id);
    }
}
