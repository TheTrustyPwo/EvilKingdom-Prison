package net.minecraft.world.level.levelgen;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;

public abstract class Column {
    public static Column.Range around(int i, int j) {
        return new Column.Range(i - 1, j + 1);
    }

    public static Column.Range inside(int floor, int ceiling) {
        return new Column.Range(floor, ceiling);
    }

    public static Column below(int ceiling) {
        return new Column.Ray(ceiling, false);
    }

    public static Column fromHighest(int i) {
        return new Column.Ray(i + 1, false);
    }

    public static Column above(int floor) {
        return new Column.Ray(floor, true);
    }

    public static Column fromLowest(int i) {
        return new Column.Ray(i - 1, true);
    }

    public static Column line() {
        return Column.Line.INSTANCE;
    }

    public static Column create(OptionalInt ceilingHeight, OptionalInt floorHeight) {
        if (ceilingHeight.isPresent() && floorHeight.isPresent()) {
            return inside(ceilingHeight.getAsInt(), floorHeight.getAsInt());
        } else if (ceilingHeight.isPresent()) {
            return above(ceilingHeight.getAsInt());
        } else {
            return floorHeight.isPresent() ? below(floorHeight.getAsInt()) : line();
        }
    }

    public abstract OptionalInt getCeiling();

    public abstract OptionalInt getFloor();

    public abstract OptionalInt getHeight();

    public Column withFloor(OptionalInt floor) {
        return create(floor, this.getCeiling());
    }

    public Column withCeiling(OptionalInt ceiling) {
        return create(this.getFloor(), ceiling);
    }

    public static Optional<Column> scan(LevelSimulatedReader world, BlockPos pos, int height, Predicate<BlockState> canGenerate, Predicate<BlockState> canReplace) {
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
        if (!world.isStateAtPosition(pos, canGenerate)) {
            return Optional.empty();
        } else {
            int i = pos.getY();
            OptionalInt optionalInt = scanDirection(world, height, canGenerate, canReplace, mutableBlockPos, i, Direction.UP);
            OptionalInt optionalInt2 = scanDirection(world, height, canGenerate, canReplace, mutableBlockPos, i, Direction.DOWN);
            return Optional.of(create(optionalInt2, optionalInt));
        }
    }

    private static OptionalInt scanDirection(LevelSimulatedReader world, int height, Predicate<BlockState> canGenerate, Predicate<BlockState> canReplace, BlockPos.MutableBlockPos mutablePos, int y, Direction direction) {
        mutablePos.setY(y);

        for(int i = 1; i < height && world.isStateAtPosition(mutablePos, canGenerate); ++i) {
            mutablePos.move(direction);
        }

        return world.isStateAtPosition(mutablePos, canReplace) ? OptionalInt.of(mutablePos.getY()) : OptionalInt.empty();
    }

    public static final class Line extends Column {
        static final Column.Line INSTANCE = new Column.Line();

        private Line() {
        }

        @Override
        public OptionalInt getCeiling() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt getFloor() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt getHeight() {
            return OptionalInt.empty();
        }

        @Override
        public String toString() {
            return "C(-)";
        }
    }

    public static final class Range extends Column {
        private final int floor;
        private final int ceiling;

        protected Range(int floor, int ceiling) {
            this.floor = floor;
            this.ceiling = ceiling;
            if (this.height() < 0) {
                throw new IllegalArgumentException("Column of negative height: " + this);
            }
        }

        @Override
        public OptionalInt getCeiling() {
            return OptionalInt.of(this.ceiling);
        }

        @Override
        public OptionalInt getFloor() {
            return OptionalInt.of(this.floor);
        }

        @Override
        public OptionalInt getHeight() {
            return OptionalInt.of(this.height());
        }

        public int ceiling() {
            return this.ceiling;
        }

        public int floor() {
            return this.floor;
        }

        public int height() {
            return this.ceiling - this.floor - 1;
        }

        @Override
        public String toString() {
            return "C(" + this.ceiling + "-" + this.floor + ")";
        }
    }

    public static final class Ray extends Column {
        private final int edge;
        private final boolean pointingUp;

        public Ray(int height, boolean floor) {
            this.edge = height;
            this.pointingUp = floor;
        }

        @Override
        public OptionalInt getCeiling() {
            return this.pointingUp ? OptionalInt.empty() : OptionalInt.of(this.edge);
        }

        @Override
        public OptionalInt getFloor() {
            return this.pointingUp ? OptionalInt.of(this.edge) : OptionalInt.empty();
        }

        @Override
        public OptionalInt getHeight() {
            return OptionalInt.empty();
        }

        @Override
        public String toString() {
            return this.pointingUp ? "C(" + this.edge + "-)" : "C(-" + this.edge + ")";
        }
    }
}
