package net.minecraft.world;

import java.util.Arrays;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public enum Difficulty {
    PEACEFUL(0, "peaceful"),
    EASY(1, "easy"),
    NORMAL(2, "normal"),
    HARD(3, "hard");

    private static final Difficulty[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(Difficulty::getId)).toArray((i) -> {
        return new Difficulty[i];
    });
    private final int id;
    private final String key;

    private Difficulty(int id, String name) {
        this.id = id;
        this.key = name;
    }

    public int getId() {
        return this.id;
    }

    public Component getDisplayName() {
        return new TranslatableComponent("options.difficulty." + this.key);
    }

    public static Difficulty byId(int ordinal) {
        return BY_ID[ordinal % BY_ID.length];
    }

    @Nullable
    public static Difficulty byName(String name) {
        for(Difficulty difficulty : values()) {
            if (difficulty.key.equals(name)) {
                return difficulty;
            }
        }

        return null;
    }

    public String getKey() {
        return this.key;
    }
}
