package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum CaveSurface implements StringRepresentable {
    CEILING(Direction.UP, 1, "ceiling"),
    FLOOR(Direction.DOWN, -1, "floor");

    public static final Codec<CaveSurface> CODEC = StringRepresentable.fromEnum(CaveSurface::values, CaveSurface::byName);
    private final Direction direction;
    private final int y;
    private final String id;
    private static final CaveSurface[] VALUES = values();

    private CaveSurface(Direction direction, int offset, String name) {
        this.direction = direction;
        this.y = offset;
        this.id = name;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public int getY() {
        return this.y;
    }

    public static CaveSurface byName(String name) {
        for(CaveSurface caveSurface : VALUES) {
            if (caveSurface.getSerializedName().equals(name)) {
                return caveSurface;
            }
        }

        throw new IllegalArgumentException("Unknown Surface type: " + name);
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}
