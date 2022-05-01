package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import com.mojang.math.OctahedralGroup;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.core.Direction;

public enum Rotation {
    NONE(OctahedralGroup.IDENTITY),
    CLOCKWISE_90(OctahedralGroup.ROT_90_Y_NEG),
    CLOCKWISE_180(OctahedralGroup.ROT_180_FACE_XZ),
    COUNTERCLOCKWISE_90(OctahedralGroup.ROT_90_Y_POS);

    private final OctahedralGroup rotation;

    private Rotation(OctahedralGroup directionTransformation) {
        this.rotation = directionTransformation;
    }

    public Rotation getRotated(Rotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            switch(this) {
            case NONE:
                return CLOCKWISE_180;
            case CLOCKWISE_90:
                return COUNTERCLOCKWISE_90;
            case CLOCKWISE_180:
                return NONE;
            case COUNTERCLOCKWISE_90:
                return CLOCKWISE_90;
            }
        case COUNTERCLOCKWISE_90:
            switch(this) {
            case NONE:
                return COUNTERCLOCKWISE_90;
            case CLOCKWISE_90:
                return NONE;
            case CLOCKWISE_180:
                return CLOCKWISE_90;
            case COUNTERCLOCKWISE_90:
                return CLOCKWISE_180;
            }
        case CLOCKWISE_90:
            switch(this) {
            case NONE:
                return CLOCKWISE_90;
            case CLOCKWISE_90:
                return CLOCKWISE_180;
            case CLOCKWISE_180:
                return COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90:
                return NONE;
            }
        default:
            return this;
        }
    }

    public OctahedralGroup rotation() {
        return this.rotation;
    }

    public Direction rotate(Direction direction) {
        if (direction.getAxis() == Direction.Axis.Y) {
            return direction;
        } else {
            switch(this) {
            case CLOCKWISE_90:
                return direction.getClockWise();
            case CLOCKWISE_180:
                return direction.getOpposite();
            case COUNTERCLOCKWISE_90:
                return direction.getCounterClockWise();
            default:
                return direction;
            }
        }
    }

    public int rotate(int rotation, int fullTurn) {
        switch(this) {
        case CLOCKWISE_90:
            return (rotation + fullTurn / 4) % fullTurn;
        case CLOCKWISE_180:
            return (rotation + fullTurn / 2) % fullTurn;
        case COUNTERCLOCKWISE_90:
            return (rotation + fullTurn * 3 / 4) % fullTurn;
        default:
            return rotation;
        }
    }

    public static Rotation getRandom(Random random) {
        return Util.getRandom(values(), random);
    }

    public static List<Rotation> getShuffled(Random random) {
        List<Rotation> list = Lists.newArrayList(values());
        Collections.shuffle(list, random);
        return list;
    }
}
