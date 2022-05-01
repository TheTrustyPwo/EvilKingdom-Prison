package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;

public enum Direction implements StringRepresentable {
    DOWN(0, 1, -1, "down", Direction.AxisDirection.NEGATIVE, Direction.Axis.Y, new Vec3i(0, -1, 0)),
    UP(1, 0, -1, "up", Direction.AxisDirection.POSITIVE, Direction.Axis.Y, new Vec3i(0, 1, 0)),
    NORTH(2, 3, 2, "north", Direction.AxisDirection.NEGATIVE, Direction.Axis.Z, new Vec3i(0, 0, -1)),
    SOUTH(3, 2, 0, "south", Direction.AxisDirection.POSITIVE, Direction.Axis.Z, new Vec3i(0, 0, 1)),
    WEST(4, 5, 1, "west", Direction.AxisDirection.NEGATIVE, Direction.Axis.X, new Vec3i(-1, 0, 0)),
    EAST(5, 4, 3, "east", Direction.AxisDirection.POSITIVE, Direction.Axis.X, new Vec3i(1, 0, 0));

    public static final Codec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values, Direction::byName);
    public static final Codec<Direction> VERTICAL_CODEC = CODEC.flatXmap(Direction::verifyVertical, Direction::verifyVertical);
    private final int data3d;
    private final int oppositeIndex;
    private final int data2d;
    private final String name;
    private final Direction.Axis axis;
    private final Direction.AxisDirection axisDirection;
    private final Vec3i normal;
    private static final Direction[] VALUES = values();
    private static final Map<String, Direction> BY_NAME = Arrays.stream(VALUES).collect(Collectors.toMap(Direction::getName, (direction) -> {
        return direction;
    }));
    private static final Direction[] BY_3D_DATA = Arrays.stream(VALUES).sorted(Comparator.comparingInt((direction) -> {
        return direction.data3d;
    })).toArray((i) -> {
        return new Direction[i];
    });
    private static final Direction[] BY_2D_DATA = Arrays.stream(VALUES).filter((direction) -> {
        return direction.getAxis().isHorizontal();
    }).sorted(Comparator.comparingInt((direction) -> {
        return direction.data2d;
    })).toArray((i) -> {
        return new Direction[i];
    });
    private static final Long2ObjectMap<Direction> BY_NORMAL = Arrays.stream(VALUES).collect(Collectors.toMap((direction) -> {
        return (new BlockPos(direction.getNormal())).asLong();
    }, (direction) -> {
        return direction;
    }, (direction1, direction2) -> {
        throw new IllegalArgumentException("Duplicate keys");
    }, Long2ObjectOpenHashMap::new));
    // Paper start
    private final int adjX;
    private final int adjY;
    private final int adjZ;
    // Paper end

    private Direction(int id, int idOpposite, int idHorizontal, String name, Direction.AxisDirection direction, Direction.Axis axis, Vec3i vector) {
        this.data3d = id;
        this.data2d = idHorizontal;
        this.oppositeIndex = idOpposite;
        this.name = name;
        this.axis = axis;
        this.axisDirection = direction;
        this.normal = vector;
        // Paper start
        this.adjX = vector.getX();
        this.adjY = vector.getY();
        this.adjZ = vector.getZ();
        // Paper end
    }

    public static Direction[] orderedByNearest(Entity entity) {
        float f = entity.getViewXRot(1.0F) * ((float)Math.PI / 180F);
        float g = -entity.getViewYRot(1.0F) * ((float)Math.PI / 180F);
        float h = Mth.sin(f);
        float i = Mth.cos(f);
        float j = Mth.sin(g);
        float k = Mth.cos(g);
        boolean bl = j > 0.0F;
        boolean bl2 = h < 0.0F;
        boolean bl3 = k > 0.0F;
        float l = bl ? j : -j;
        float m = bl2 ? -h : h;
        float n = bl3 ? k : -k;
        float o = l * i;
        float p = n * i;
        Direction direction = bl ? EAST : WEST;
        Direction direction2 = bl2 ? UP : DOWN;
        Direction direction3 = bl3 ? SOUTH : NORTH;
        if (l > n) {
            if (m > o) {
                return makeDirectionArray(direction2, direction, direction3);
            } else {
                return p > m ? makeDirectionArray(direction, direction3, direction2) : makeDirectionArray(direction, direction2, direction3);
            }
        } else if (m > p) {
            return makeDirectionArray(direction2, direction3, direction);
        } else {
            return o > m ? makeDirectionArray(direction3, direction, direction2) : makeDirectionArray(direction3, direction2, direction);
        }
    }

    private static Direction[] makeDirectionArray(Direction first, Direction second, Direction third) {
        return new Direction[]{first, second, third, third.getOpposite(), second.getOpposite(), first.getOpposite()};
    }

    public static Direction rotate(Matrix4f matrix, Direction direction) {
        Vec3i vec3i = direction.getNormal();
        Vector4f vector4f = new Vector4f((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ(), 0.0F);
        vector4f.transform(matrix);
        return getNearest(vector4f.x(), vector4f.y(), vector4f.z());
    }

    public Quaternion getRotation() {
        Quaternion quaternion = Vector3f.XP.rotationDegrees(90.0F);
        Quaternion var10000;
        switch(this) {
        case DOWN:
            var10000 = Vector3f.XP.rotationDegrees(180.0F);
            break;
        case UP:
            var10000 = Quaternion.ONE.copy();
            break;
        case NORTH:
            quaternion.mul(Vector3f.ZP.rotationDegrees(180.0F));
            var10000 = quaternion;
            break;
        case SOUTH:
            var10000 = quaternion;
            break;
        case WEST:
            quaternion.mul(Vector3f.ZP.rotationDegrees(90.0F));
            var10000 = quaternion;
            break;
        case EAST:
            quaternion.mul(Vector3f.ZP.rotationDegrees(-90.0F));
            var10000 = quaternion;
            break;
        default:
            throw new IncompatibleClassChangeError();
        }

        return var10000;
    }

    public int get3DDataValue() {
        return this.data3d;
    }

    public int get2DDataValue() {
        return this.data2d;
    }

    public Direction.AxisDirection getAxisDirection() {
        return this.axisDirection;
    }

    public static Direction getFacingAxis(Entity entity, Direction.Axis axis) {
        Direction var10000;
        switch(axis) {
        case X:
            var10000 = EAST.isFacingAngle(entity.getViewYRot(1.0F)) ? EAST : WEST;
            break;
        case Z:
            var10000 = SOUTH.isFacingAngle(entity.getViewYRot(1.0F)) ? SOUTH : NORTH;
            break;
        case Y:
            var10000 = entity.getViewXRot(1.0F) < 0.0F ? UP : DOWN;
            break;
        default:
            throw new IncompatibleClassChangeError();
        }

        return var10000;
    }

    public Direction getOpposite() {
        return from3DDataValue(this.oppositeIndex);
    }

    public Direction getClockWise(Direction.Axis axis) {
        Direction var10000;
        switch(axis) {
        case X:
            var10000 = this != WEST && this != EAST ? this.getClockWiseX() : this;
            break;
        case Z:
            var10000 = this != NORTH && this != SOUTH ? this.getClockWiseZ() : this;
            break;
        case Y:
            var10000 = this != UP && this != DOWN ? this.getClockWise() : this;
            break;
        default:
            throw new IncompatibleClassChangeError();
        }

        return var10000;
    }

    public Direction getCounterClockWise(Direction.Axis axis) {
        Direction var10000;
        switch(axis) {
        case X:
            var10000 = this != WEST && this != EAST ? this.getCounterClockWiseX() : this;
            break;
        case Z:
            var10000 = this != NORTH && this != SOUTH ? this.getCounterClockWiseZ() : this;
            break;
        case Y:
            var10000 = this != UP && this != DOWN ? this.getCounterClockWise() : this;
            break;
        default:
            throw new IncompatibleClassChangeError();
        }

        return var10000;
    }

    public Direction getClockWise() {
        Direction var10000;
        switch(this) {
        case NORTH:
            var10000 = EAST;
            break;
        case SOUTH:
            var10000 = WEST;
            break;
        case WEST:
            var10000 = NORTH;
            break;
        case EAST:
            var10000 = SOUTH;
            break;
        default:
            throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
        }

        return var10000;
    }

    private Direction getClockWiseX() {
        Direction var10000;
        switch(this) {
        case DOWN:
            var10000 = SOUTH;
            break;
        case UP:
            var10000 = NORTH;
            break;
        case NORTH:
            var10000 = DOWN;
            break;
        case SOUTH:
            var10000 = UP;
            break;
        default:
            throw new IllegalStateException("Unable to get X-rotated facing of " + this);
        }

        return var10000;
    }

    private Direction getCounterClockWiseX() {
        Direction var10000;
        switch(this) {
        case DOWN:
            var10000 = NORTH;
            break;
        case UP:
            var10000 = SOUTH;
            break;
        case NORTH:
            var10000 = UP;
            break;
        case SOUTH:
            var10000 = DOWN;
            break;
        default:
            throw new IllegalStateException("Unable to get X-rotated facing of " + this);
        }

        return var10000;
    }

    private Direction getClockWiseZ() {
        Direction var10000;
        switch(this) {
        case DOWN:
            var10000 = WEST;
            break;
        case UP:
            var10000 = EAST;
            break;
        case NORTH:
        case SOUTH:
        default:
            throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
        case WEST:
            var10000 = UP;
            break;
        case EAST:
            var10000 = DOWN;
        }

        return var10000;
    }

    private Direction getCounterClockWiseZ() {
        Direction var10000;
        switch(this) {
        case DOWN:
            var10000 = EAST;
            break;
        case UP:
            var10000 = WEST;
            break;
        case NORTH:
        case SOUTH:
        default:
            throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
        case WEST:
            var10000 = DOWN;
            break;
        case EAST:
            var10000 = UP;
        }

        return var10000;
    }

    public Direction getCounterClockWise() {
        Direction var10000;
        switch(this) {
        case NORTH:
            var10000 = WEST;
            break;
        case SOUTH:
            var10000 = EAST;
            break;
        case WEST:
            var10000 = SOUTH;
            break;
        case EAST:
            var10000 = NORTH;
            break;
        default:
            throw new IllegalStateException("Unable to get CCW facing of " + this);
        }

        return var10000;
    }

    public int getStepX() {
        return this.adjX; // Paper
    }

    public int getStepY() {
        return this.adjY; // Paper
    }

    public int getStepZ() {
        return this.adjZ; // Paper
    }

    public Vector3f step() {
        return new Vector3f((float)this.getStepX(), (float)this.getStepY(), (float)this.getStepZ());
    }

    public String getName() {
        return this.name;
    }

    public Direction.Axis getAxis() {
        return this.axis;
    }

    @Nullable
    public static Direction byName(@Nullable String name) {
        return name == null ? null : BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    public static Direction from3DDataValue(int id) {
        return BY_3D_DATA[Mth.abs(id % BY_3D_DATA.length)];
    }

    public static Direction from2DDataValue(int value) {
        return BY_2D_DATA[Mth.abs(value % BY_2D_DATA.length)];
    }

    @Nullable
    public static Direction fromNormal(BlockPos pos) {
        return BY_NORMAL.get(pos.asLong());
    }

    @Nullable
    public static Direction fromNormal(int x, int y, int z) {
        return BY_NORMAL.get(BlockPos.asLong(x, y, z));
    }

    public static Direction fromYRot(double rotation) {
        return from2DDataValue(Mth.floor(rotation / 90.0D + 0.5D) & 3);
    }

    public static Direction fromAxisAndDirection(Direction.Axis axis, Direction.AxisDirection direction) {
        Direction var10000;
        switch(axis) {
        case X:
            var10000 = direction == Direction.AxisDirection.POSITIVE ? EAST : WEST;
            break;
        case Z:
            var10000 = direction == Direction.AxisDirection.POSITIVE ? SOUTH : NORTH;
            break;
        case Y:
            var10000 = direction == Direction.AxisDirection.POSITIVE ? UP : DOWN;
            break;
        default:
            throw new IncompatibleClassChangeError();
        }

        return var10000;
    }

    public float toYRot() {
        return (float)((this.data2d & 3) * 90);
    }

    public static Direction getRandom(Random random) {
        return Util.getRandom(VALUES, random);
    }

    public static Direction getNearest(double x, double y, double z) {
        return getNearest((float)x, (float)y, (float)z);
    }

    public static Direction getNearest(float x, float y, float z) {
        Direction direction = NORTH;
        float f = Float.MIN_VALUE;

        for(Direction direction2 : VALUES) {
            float g = x * (float)direction2.normal.getX() + y * (float)direction2.normal.getY() + z * (float)direction2.normal.getZ();
            if (g > f) {
                f = g;
                direction = direction2;
            }
        }

        return direction;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    private static DataResult<Direction> verifyVertical(Direction direction) {
        return direction.getAxis().isVertical() ? DataResult.success(direction) : DataResult.error("Expected a vertical direction");
    }

    public static Direction get(Direction.AxisDirection direction, Direction.Axis axis) {
        for(Direction direction2 : VALUES) {
            if (direction2.getAxisDirection() == direction && direction2.getAxis() == axis) {
                return direction2;
            }
        }

        throw new IllegalArgumentException("No such direction: " + direction + " " + axis);
    }

    public Vec3i getNormal() {
        return this.normal;
    }

    public boolean isFacingAngle(float yaw) {
        float f = yaw * ((float)Math.PI / 180F);
        float g = -Mth.sin(f);
        float h = Mth.cos(f);
        return (float)this.normal.getX() * g + (float)this.normal.getZ() * h > 0.0F;
    }

    public static enum Axis implements StringRepresentable, Predicate<Direction> {
        X("x") {
            @Override
            public int choose(int x, int y, int z) {
                return x;
            }

            @Override
            public double choose(double x, double y, double z) {
                return x;
            }
        },
        Y("y") {
            @Override
            public int choose(int x, int y, int z) {
                return y;
            }

            @Override
            public double choose(double x, double y, double z) {
                return y;
            }
        },
        Z("z") {
            @Override
            public int choose(int x, int y, int z) {
                return z;
            }

            @Override
            public double choose(double x, double y, double z) {
                return z;
            }
        };

        public static final Direction.Axis[] VALUES = values();
        public static final Codec<Direction.Axis> CODEC = StringRepresentable.fromEnum(Direction.Axis::values, Direction.Axis::byName);
        private static final Map<String, Direction.Axis> BY_NAME = Arrays.stream(VALUES).collect(Collectors.toMap(Direction.Axis::getName, (axis) -> {
            return axis;
        }));
        private final String name;

        Axis(String name) {
            this.name = name;
        }

        @Nullable
        public static Direction.Axis byName(String name) {
            return BY_NAME.get(name.toLowerCase(Locale.ROOT));
        }

        public String getName() {
            return this.name;
        }

        public boolean isVertical() {
            return this == Y;
        }

        public boolean isHorizontal() {
            return this == X || this == Z;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public static Direction.Axis getRandom(Random random) {
            return Util.getRandom(VALUES, random);
        }

        @Override
        public boolean test(@Nullable Direction direction) {
            return direction != null && direction.getAxis() == this;
        }

        public Direction.Plane getPlane() {
            Direction.Plane var10000;
            switch(this) {
            case X:
            case Z:
                var10000 = Direction.Plane.HORIZONTAL;
                break;
            case Y:
                var10000 = Direction.Plane.VERTICAL;
                break;
            default:
                throw new IncompatibleClassChangeError();
            }

            return var10000;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public abstract int choose(int x, int y, int z);

        public abstract double choose(double x, double y, double z);
    }

    public static enum AxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int step;
        private final String name;

        private AxisDirection(int offset, String description) {
            this.step = offset;
            this.name = description;
        }

        public int getStep() {
            return this.step;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public Direction.AxisDirection opposite() {
            return this == POSITIVE ? NEGATIVE : POSITIVE;
        }
    }

    public static enum Plane implements Iterable<Direction>, Predicate<Direction> {
        HORIZONTAL(new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}, new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}),
        VERTICAL(new Direction[]{Direction.UP, Direction.DOWN}, new Direction.Axis[]{Direction.Axis.Y});

        private final Direction[] faces;
        private final Direction.Axis[] axis;

        private Plane(Direction[] facingArray, Direction.Axis[] axisArray) {
            this.faces = facingArray;
            this.axis = axisArray;
        }

        public Direction getRandomDirection(Random random) {
            return Util.getRandom(this.faces, random);
        }

        public Direction.Axis getRandomAxis(Random random) {
            return Util.getRandom(this.axis, random);
        }

        @Override
        public boolean test(@Nullable Direction direction) {
            return direction != null && direction.getAxis().getPlane() == this;
        }

        @Override
        public Iterator<Direction> iterator() {
            return Iterators.forArray(this.faces);
        }

        public Stream<Direction> stream() {
            return Arrays.stream(this.faces);
        }
    }
}
