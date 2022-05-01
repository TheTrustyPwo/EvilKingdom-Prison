package net.minecraft.world.entity.animal.horse;

import java.util.Arrays;
import java.util.Comparator;

public enum Variant {
    WHITE(0),
    CREAMY(1),
    CHESTNUT(2),
    BROWN(3),
    BLACK(4),
    GRAY(5),
    DARKBROWN(6);

    private static final Variant[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(Variant::getId)).toArray((i) -> {
        return new Variant[i];
    });
    private final int id;

    private Variant(int index) {
        this.id = index;
    }

    public int getId() {
        return this.id;
    }

    public static Variant byId(int index) {
        return BY_ID[index % BY_ID.length];
    }
}
