package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class Shapes {
    public static final double EPSILON = 1.0E-7D;
    public static final double BIG_EPSILON = 1.0E-6D;
    private static final VoxelShape BLOCK = Util.make(() -> {
        DiscreteVoxelShape discreteVoxelShape = new BitSetDiscreteVoxelShape(1, 1, 1);
        discreteVoxelShape.fill(0, 0, 0);
        return new CubeVoxelShape(discreteVoxelShape);
    }); public static VoxelShape getFullUnoptimisedCube() { return BLOCK; } // Paper - OBFHELPER
    public static final VoxelShape INFINITY = box(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    private static final VoxelShape EMPTY = new ArrayVoxelShape(new BitSetDiscreteVoxelShape(0, 0, 0), (DoubleList)(new DoubleArrayList(new double[]{0.0D})), (DoubleList)(new DoubleArrayList(new double[]{0.0D})), (DoubleList)(new DoubleArrayList(new double[]{0.0D})));
    public static final io.papermc.paper.voxel.AABBVoxelShape BLOCK_OPTIMISED = new io.papermc.paper.voxel.AABBVoxelShape(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)); // Paper

    public static VoxelShape empty() {
        return EMPTY;
    }

    public static VoxelShape block() {
        return BLOCK_OPTIMISED; // Paper
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!(minX > maxX) && !(minY > maxY) && !(minZ > maxZ)) {
            return create(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
        }
    }

    public static VoxelShape create(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!(maxX - minX < 1.0E-7D) && !(maxY - minY < 1.0E-7D) && !(maxZ - minZ < 1.0E-7D)) {
            return new io.papermc.paper.voxel.AABBVoxelShape(new AABB(minX, minY, minZ, maxX, maxY, maxZ)); // Paper
        } else {
            return empty();
        }
    }

    public static VoxelShape create(AABB box) {
        return new io.papermc.paper.voxel.AABBVoxelShape(box); // Paper
    }

    @VisibleForTesting
    protected static int findBits(double min, double max) {
        if (!(min < -1.0E-7D) && !(max > 1.0000001D)) {
            for(int i = 0; i <= 3; ++i) {
                int j = 1 << i;
                double d = min * (double)j;
                double e = max * (double)j;
                boolean bl = Math.abs(d - (double)Math.round(d)) < 1.0E-7D * (double)j;
                boolean bl2 = Math.abs(e - (double)Math.round(e)) < 1.0E-7D * (double)j;
                if (bl && bl2) {
                    return i;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    protected static long lcm(int a, int b) {
        return (long)a * (long)(b / IntMath.gcd(a, b));
    }

    public static VoxelShape or(VoxelShape first, VoxelShape second) {
        return join(first, second, BooleanOp.OR);
    }

    public static VoxelShape or(VoxelShape first, VoxelShape... others) {
        return Arrays.stream(others).reduce(first, Shapes::or);
    }

    public static VoxelShape join(VoxelShape first, VoxelShape second, BooleanOp function) {
        return joinUnoptimized(first, second, function).optimize();
    }

    public static VoxelShape joinUnoptimized(VoxelShape one, VoxelShape two, BooleanOp function) {
        if (function.apply(false, false)) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
        } else if (one == two) {
            return function.apply(true, true) ? one : empty();
        } else {
            boolean bl = function.apply(true, false);
            boolean bl2 = function.apply(false, true);
            if (one.isEmpty()) {
                return bl2 ? two : empty();
            } else if (two.isEmpty()) {
                return bl ? one : empty();
            } else {
                IndexMerger indexMerger = createIndexMerger(1, one.getCoords(Direction.Axis.X), two.getCoords(Direction.Axis.X), bl, bl2);
                IndexMerger indexMerger2 = createIndexMerger(indexMerger.size() - 1, one.getCoords(Direction.Axis.Y), two.getCoords(Direction.Axis.Y), bl, bl2);
                IndexMerger indexMerger3 = createIndexMerger((indexMerger.size() - 1) * (indexMerger2.size() - 1), one.getCoords(Direction.Axis.Z), two.getCoords(Direction.Axis.Z), bl, bl2);
                BitSetDiscreteVoxelShape bitSetDiscreteVoxelShape = BitSetDiscreteVoxelShape.join(one.shape, two.shape, indexMerger, indexMerger2, indexMerger3, function);
                return (VoxelShape)(indexMerger instanceof DiscreteCubeMerger && indexMerger2 instanceof DiscreteCubeMerger && indexMerger3 instanceof DiscreteCubeMerger ? new CubeVoxelShape(bitSetDiscreteVoxelShape) : new ArrayVoxelShape(bitSetDiscreteVoxelShape, indexMerger.getList(), indexMerger2.getList(), indexMerger3.getList()));
            }
        }
    }

    public static boolean joinIsNotEmpty(VoxelShape shape1, VoxelShape shape2, BooleanOp predicate) {
        // Paper start - optimise voxelshape
        if (predicate == BooleanOp.AND) {
            if (shape1 instanceof io.papermc.paper.voxel.AABBVoxelShape && shape2 instanceof io.papermc.paper.voxel.AABBVoxelShape) {
                return io.papermc.paper.util.CollisionUtil.voxelShapeIntersect(((io.papermc.paper.voxel.AABBVoxelShape)shape1).aabb, ((io.papermc.paper.voxel.AABBVoxelShape)shape2).aabb);
            } else if (shape1 instanceof io.papermc.paper.voxel.AABBVoxelShape && shape2 instanceof ArrayVoxelShape) {
                return ((ArrayVoxelShape)shape2).intersects(((io.papermc.paper.voxel.AABBVoxelShape)shape1).aabb);
            } else if (shape2 instanceof io.papermc.paper.voxel.AABBVoxelShape && shape1 instanceof ArrayVoxelShape) {
                return ((ArrayVoxelShape)shape1).intersects(((io.papermc.paper.voxel.AABBVoxelShape)shape2).aabb);
            }
        }
        return joinIsNotEmptyVanilla(shape1, shape2, predicate);
    }
    public static boolean joinIsNotEmptyVanilla(VoxelShape shape1, VoxelShape shape2, BooleanOp predicate) {
        // Paper end - optimise voxelshape
        if (predicate.apply(false, false)) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
        } else {
            boolean bl = shape1.isEmpty();
            boolean bl2 = shape2.isEmpty();
            if (!bl && !bl2) {
                if (shape1 == shape2) {
                    return predicate.apply(true, true);
                } else {
                    boolean bl3 = predicate.apply(true, false);
                    boolean bl4 = predicate.apply(false, true);

                    for(Direction.Axis axis : AxisCycle.AXIS_VALUES) {
                        if (shape1.max(axis) < shape2.min(axis) - 1.0E-7D) {
                            return bl3 || bl4;
                        }

                        if (shape2.max(axis) < shape1.min(axis) - 1.0E-7D) {
                            return bl3 || bl4;
                        }
                    }

                    IndexMerger indexMerger = createIndexMerger(1, shape1.getCoords(Direction.Axis.X), shape2.getCoords(Direction.Axis.X), bl3, bl4);
                    IndexMerger indexMerger2 = createIndexMerger(indexMerger.size() - 1, shape1.getCoords(Direction.Axis.Y), shape2.getCoords(Direction.Axis.Y), bl3, bl4);
                    IndexMerger indexMerger3 = createIndexMerger((indexMerger.size() - 1) * (indexMerger2.size() - 1), shape1.getCoords(Direction.Axis.Z), shape2.getCoords(Direction.Axis.Z), bl3, bl4);
                    return joinIsNotEmpty(indexMerger, indexMerger2, indexMerger3, shape1.shape, shape2.shape, predicate);
                }
            } else {
                return predicate.apply(!bl, !bl2);
            }
        }
    }

    private static boolean joinIsNotEmpty(IndexMerger mergedX, IndexMerger mergedY, IndexMerger mergedZ, DiscreteVoxelShape shape1, DiscreteVoxelShape shape2, BooleanOp predicate) {
        return !mergedX.forMergedIndexes((x1, x2, index1) -> {
            return mergedY.forMergedIndexes((y1, y2, index2) -> {
                return mergedZ.forMergedIndexes((z1, z2, index3) -> {
                    return !predicate.apply(shape1.isFullWide(x1, y1, z1), shape2.isFullWide(x2, y2, z2));
                });
            });
        });
    }

    public static double collide(Direction.Axis axis, AABB box, Iterable<VoxelShape> shapes, double maxDist) {
        for(VoxelShape voxelShape : shapes) {
            if (Math.abs(maxDist) < 1.0E-7D) {
                return 0.0D;
            }

            maxDist = voxelShape.collide(axis, box, maxDist);
        }

        return maxDist;
    }

    public static boolean blockOccudes(VoxelShape shape, VoxelShape neighbor, Direction direction) {
        if (shape == block() && neighbor == block()) {
            return true;
        } else if (neighbor.isEmpty()) {
            return false;
        } else {
            Direction.Axis axis = direction.getAxis();
            Direction.AxisDirection axisDirection = direction.getAxisDirection();
            VoxelShape voxelShape = axisDirection == Direction.AxisDirection.POSITIVE ? shape : neighbor;
            VoxelShape voxelShape2 = axisDirection == Direction.AxisDirection.POSITIVE ? neighbor : shape;
            BooleanOp booleanOp = axisDirection == Direction.AxisDirection.POSITIVE ? BooleanOp.ONLY_FIRST : BooleanOp.ONLY_SECOND;
            return DoubleMath.fuzzyEquals(voxelShape.max(axis), 1.0D, 1.0E-7D) && DoubleMath.fuzzyEquals(voxelShape2.min(axis), 0.0D, 1.0E-7D) && !joinIsNotEmpty(new SliceShape(voxelShape, axis, voxelShape.shape.getSize(axis) - 1), new SliceShape(voxelShape2, axis, 0), booleanOp);
        }
    }

    public static VoxelShape getFaceShape(VoxelShape shape, Direction direction) {
        // Paper start - optimise shape creation here for lighting, as this shape is going to be used
        // for transparency checks
        if (shape == BLOCK || shape == BLOCK_OPTIMISED) {
            return BLOCK_OPTIMISED;
        } else if (shape == empty()) {
            return empty();
        }

        if (shape instanceof io.papermc.paper.voxel.AABBVoxelShape) {
            final AABB box = ((io.papermc.paper.voxel.AABBVoxelShape)shape).aabb;
            switch (direction) {
                case WEST: // -X
                case EAST: { // +X
                    final boolean useEmpty = direction == Direction.EAST ? !DoubleMath.fuzzyEquals(box.maxX, 1.0, io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON) :
                            !DoubleMath.fuzzyEquals(box.minX, 0.0, io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON);
                    return useEmpty ? empty() : new io.papermc.paper.voxel.AABBVoxelShape(new AABB(0.0, box.minY, box.minZ, 1.0, box.maxY, box.maxZ)).optimize();
                }
                case DOWN: // -Y
                case UP: { // +Y
                    final boolean useEmpty = direction == Direction.UP ? !DoubleMath.fuzzyEquals(box.maxY, 1.0, io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON) :
                            !DoubleMath.fuzzyEquals(box.minY, 0.0, io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON);
                    return useEmpty ? empty() : new io.papermc.paper.voxel.AABBVoxelShape(new AABB(box.minX, 0.0, box.minZ, box.maxX, 1.0, box.maxZ)).optimize();
                }
                case NORTH: // -Z
                case SOUTH: { // +Z
                    final boolean useEmpty = direction == Direction.SOUTH ? !DoubleMath.fuzzyEquals(box.maxZ, 1.0, io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON) :
                            !DoubleMath.fuzzyEquals(box.minZ,0.0, io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON);
                    return useEmpty ? empty() : new io.papermc.paper.voxel.AABBVoxelShape(new AABB(box.minX, box.minY, 0.0, box.maxX, box.maxY, 1.0)).optimize();
                }
            }
        }

        // fall back to vanilla
        return getFaceShapeVanilla(shape, direction);
    }
    public static VoxelShape getFaceShapeVanilla(VoxelShape shape, Direction direction) {
        // Paper end
        if (shape == block()) {
            return block();
        } else {
            Direction.Axis axis = direction.getAxis();
            boolean bl;
            int i;
            if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                bl = DoubleMath.fuzzyEquals(shape.max(axis), 1.0D, 1.0E-7D);
                i = shape.shape.getSize(axis) - 1;
            } else {
                bl = DoubleMath.fuzzyEquals(shape.min(axis), 0.0D, 1.0E-7D);
                i = 0;
            }

            return (VoxelShape)(!bl ? empty() : new SliceShape(shape, axis, i).optimize().optimize()); // Paper - first optimize converts to ArrayVoxelShape, second optimize could convert to AABBVoxelShape
        }
    }

    public static boolean mergedFaceOccludes(VoxelShape one, VoxelShape two, Direction direction) {
        if (one != block() && two != block()) {
            Direction.Axis axis = direction.getAxis();
            Direction.AxisDirection axisDirection = direction.getAxisDirection();
            VoxelShape voxelShape = axisDirection == Direction.AxisDirection.POSITIVE ? one : two;
            VoxelShape voxelShape2 = axisDirection == Direction.AxisDirection.POSITIVE ? two : one;
            if (!DoubleMath.fuzzyEquals(voxelShape.max(axis), 1.0D, 1.0E-7D)) {
                voxelShape = empty();
            }

            if (!DoubleMath.fuzzyEquals(voxelShape2.min(axis), 0.0D, 1.0E-7D)) {
                voxelShape2 = empty();
            }

            return !joinIsNotEmpty(block(), joinUnoptimized(new SliceShape(voxelShape, axis, voxelShape.shape.getSize(axis) - 1), new SliceShape(voxelShape2, axis, 0), BooleanOp.OR), BooleanOp.ONLY_FIRST);
        } else {
            return true;
        }
    }

    public static boolean faceShapeOccludes(VoxelShape one, VoxelShape two) {
        // Paper start - try to optimise for the case where the shapes do _not_ occlude
        // which is _most_ of the time in lighting
        if (one == getFullUnoptimisedCube() || one == BLOCK_OPTIMISED
                || two == getFullUnoptimisedCube() || two == BLOCK_OPTIMISED) {
            return true;
        }
        boolean v1Empty = one == empty();
        boolean v2Empty = two == empty();
        if (v1Empty && v2Empty) {
            return false;
        }
        if ((one instanceof io.papermc.paper.voxel.AABBVoxelShape || v1Empty)
            && (two instanceof io.papermc.paper.voxel.AABBVoxelShape || v2Empty)) {
            if (!v1Empty && !v2Empty && (one != two)) {
                AABB boundingBox1 = ((io.papermc.paper.voxel.AABBVoxelShape)one).aabb;
                AABB boundingBox2 = ((io.papermc.paper.voxel.AABBVoxelShape)two).aabb;
                // can call it here in some cases

                // check overall bounding box
                double minY = Math.min(boundingBox1.minY, boundingBox2.minY);
                double maxY = Math.max(boundingBox1.maxY, boundingBox2.maxY);
                if (minY > io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON || maxY < (1 - io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON)) {
                    return false;
                }
                double minX = Math.min(boundingBox1.minX, boundingBox2.minX);
                double maxX = Math.max(boundingBox1.maxX, boundingBox2.maxX);
                if (minX > io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON || maxX < (1 - io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON)) {
                    return false;
                }
                double minZ = Math.min(boundingBox1.minZ, boundingBox2.minZ);
                double maxZ = Math.max(boundingBox1.maxZ, boundingBox2.maxZ);
                if (minZ > io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON || maxZ < (1 - io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON)) {
                    return false;
                }
                // fall through to full merge check
            } else {
                AABB boundingBox = v1Empty ? ((io.papermc.paper.voxel.AABBVoxelShape)two).aabb : ((io.papermc.paper.voxel.AABBVoxelShape)one).aabb;
                // check if the bounding box encloses the full cube
                return (boundingBox.minY <= io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON && boundingBox.maxY >= (1 - io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON)) &&
                       (boundingBox.minX <= io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON && boundingBox.maxX >= (1 - io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON)) &&
                       (boundingBox.minZ <= io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON && boundingBox.maxZ >= (1 - io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON));
            }
        }
        return faceShapeOccludesVanilla(one, two);
    }
    public static boolean faceShapeOccludesVanilla(VoxelShape one, VoxelShape two) {
        // Paper end
        if (one != block() && two != block()) {
            if (one.isEmpty() && two.isEmpty()) {
                return false;
            } else {
                return !joinIsNotEmpty(block(), joinUnoptimized(one, two, BooleanOp.OR), BooleanOp.ONLY_FIRST);
            }
        } else {
            return true;
        }
    }

    @VisibleForTesting
    private static IndexMerger createIndexMerger(int size, DoubleList first, DoubleList second, boolean includeFirst, boolean includeSecond) { // Paper - private
        // Paper start - fast track the most common scenario
        // doublelist is usually a DoubleArrayList with Infinite head/tails that falls to the final else clause
        // This is actually the most common path, so jump to it straight away
        if (first.getDouble(0) == Double.NEGATIVE_INFINITY && first.getDouble(first.size() - 1) == Double.POSITIVE_INFINITY) {
            return new IndirectMerger(first, second, includeFirst, includeSecond);
        }
        // Split out rest to hopefully inline the above
        return lessCommonMerge(size, first, second, includeFirst, includeSecond);
    }

    private static IndexMerger lessCommonMerge(int size, DoubleList first, DoubleList second, boolean includeFirst, boolean includeSecond) {
        int i = first.size() - 1;
        int j = second.size() - 1;
        // Paper note - Rewrite below as optimized order if instead of nasty ternary
        if (first instanceof CubePointRange && second instanceof CubePointRange) {
            long l = lcm(i, j);
            if ((long)size * l <= 256L) {
                return new DiscreteCubeMerger(i, j);
            }
        }

        // Paper start - Identical happens more often than Disjoint
        if (i == j && Objects.equals(first, second)) {
            if (first instanceof IdenticalMerger) {
                return (IndexMerger) first;
            } else if (second instanceof IdenticalMerger) {
                return (IndexMerger) second;
            }
            return new IdenticalMerger(first);
        } else if (first.getDouble(i) < second.getDouble(0) - 1.0E-7D) {
            return new NonOverlappingMerger(first, second, false);
        } else if (second.getDouble(j) < first.getDouble(0) - 1.0E-7D) {
            return new NonOverlappingMerger(second, first, true);
        } else {
            return new IndirectMerger(first, second, includeFirst, includeSecond);
        }
        // Paper end
    }

    public interface DoubleLineConsumer {
        void consume(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    }
}
