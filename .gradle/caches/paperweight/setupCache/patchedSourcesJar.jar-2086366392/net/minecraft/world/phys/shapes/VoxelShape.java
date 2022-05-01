package net.minecraft.world.phys.shapes;

import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class VoxelShape {
    public final DiscreteVoxelShape shape; // Paper - public
    @Nullable
    private VoxelShape[] faces;

    // Paper start
    public boolean intersects(AABB shape) {
        return Shapes.joinIsNotEmpty(this, new io.papermc.paper.voxel.AABBVoxelShape(shape), BooleanOp.AND);
    }
    // Paper end

    protected VoxelShape(DiscreteVoxelShape voxels) { // Paper - protected
        this.shape = voxels;
    }

    public double min(Direction.Axis axis) {
        int i = this.shape.firstFull(axis);
        return i >= this.shape.getSize(axis) ? Double.POSITIVE_INFINITY : this.get(axis, i);
    }

    public double max(Direction.Axis axis) {
        int i = this.shape.lastFull(axis);
        return i <= 0 ? Double.NEGATIVE_INFINITY : this.get(axis, i);
    }

    public AABB bounds() {
        if (this.isEmpty()) {
            throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("No bounds for empty shape."));
        } else {
            return new AABB(this.min(Direction.Axis.X), this.min(Direction.Axis.Y), this.min(Direction.Axis.Z), this.max(Direction.Axis.X), this.max(Direction.Axis.Y), this.max(Direction.Axis.Z));
        }
    }

    protected double get(Direction.Axis axis, int index) {
        return this.getCoords(axis).getDouble(index);
    }

    protected abstract DoubleList getCoords(Direction.Axis axis);

    public boolean isEmpty() {
        return this.shape.isEmpty();
    }

    public VoxelShape move(double x, double y, double z) {
        return (VoxelShape)(this.isEmpty() ? Shapes.empty() : new ArrayVoxelShape(this.shape, (DoubleList)(new OffsetDoubleList(this.getCoords(Direction.Axis.X), x)), (DoubleList)(new OffsetDoubleList(this.getCoords(Direction.Axis.Y), y)), (DoubleList)(new OffsetDoubleList(this.getCoords(Direction.Axis.Z), z))));
    }

    public VoxelShape optimize() {
        VoxelShape[] voxelShapes = new VoxelShape[]{Shapes.empty()};
        this.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            voxelShapes[0] = Shapes.joinUnoptimized(voxelShapes[0], Shapes.box(minX, minY, minZ, maxX, maxY, maxZ), BooleanOp.OR);
        });
        return voxelShapes[0];
    }

    public void forAllEdges(Shapes.DoubleLineConsumer consumer) {
        this.shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            consumer.consume(this.get(Direction.Axis.X, minX), this.get(Direction.Axis.Y, minY), this.get(Direction.Axis.Z, minZ), this.get(Direction.Axis.X, maxX), this.get(Direction.Axis.Y, maxY), this.get(Direction.Axis.Z, maxZ));
        }, true);
    }

    public void forAllBoxes(Shapes.DoubleLineConsumer consumer) {
        DoubleList doubleList = this.getCoords(Direction.Axis.X);
        DoubleList doubleList2 = this.getCoords(Direction.Axis.Y);
        DoubleList doubleList3 = this.getCoords(Direction.Axis.Z);
        this.shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            consumer.consume(doubleList.getDouble(minX), doubleList2.getDouble(minY), doubleList3.getDouble(minZ), doubleList.getDouble(maxX), doubleList2.getDouble(maxY), doubleList3.getDouble(maxZ));
        }, true);
    }

    public List<AABB> toAabbs() {
        List<AABB> list = Lists.newArrayList();
        this.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            list.add(new AABB(x1, y1, z1, x2, y2, z2));
        });
        return list;
    }

    public double min(Direction.Axis axis, double from, double to) {
        Direction.Axis axis2 = AxisCycle.FORWARD.cycle(axis);
        Direction.Axis axis3 = AxisCycle.BACKWARD.cycle(axis);
        int i = this.findIndex(axis2, from);
        int j = this.findIndex(axis3, to);
        int k = this.shape.firstFull(axis, i, j);
        return k >= this.shape.getSize(axis) ? Double.POSITIVE_INFINITY : this.get(axis, k);
    }

    public double max(Direction.Axis axis, double from, double to) {
        Direction.Axis axis2 = AxisCycle.FORWARD.cycle(axis);
        Direction.Axis axis3 = AxisCycle.BACKWARD.cycle(axis);
        int i = this.findIndex(axis2, from);
        int j = this.findIndex(axis3, to);
        int k = this.shape.lastFull(axis, i, j);
        return k <= 0 ? Double.NEGATIVE_INFINITY : this.get(axis, k);
    }

    protected int findIndex(Direction.Axis axis, double coord) {
        return Mth.binarySearch(0, this.shape.getSize(axis) + 1, (i) -> {
            return coord < this.get(axis, i);
        }) - 1;
    }

    @Nullable
    public BlockHitResult clip(Vec3 start, Vec3 end, BlockPos pos) {
        if (this.isEmpty()) {
            return null;
        } else {
            Vec3 vec3 = end.subtract(start);
            if (vec3.lengthSqr() < 1.0E-7D) {
                return null;
            } else {
                Vec3 vec32 = start.add(vec3.scale(0.001D));
                return this.shape.isFullWide(this.findIndex(Direction.Axis.X, vec32.x - (double)pos.getX()), this.findIndex(Direction.Axis.Y, vec32.y - (double)pos.getY()), this.findIndex(Direction.Axis.Z, vec32.z - (double)pos.getZ())) ? new BlockHitResult(vec32, Direction.getNearest(vec3.x, vec3.y, vec3.z).getOpposite(), pos, true) : AABB.clip(this.toAabbs(), start, end, pos);
            }
        }
    }

    public Optional<Vec3> closestPointTo(Vec3 target) {
        if (this.isEmpty()) {
            return Optional.empty();
        } else {
            Vec3[] vec3s = new Vec3[1];
            this.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
                double d = Mth.clamp(target.x(), minX, maxX);
                double e = Mth.clamp(target.y(), minY, maxY);
                double f = Mth.clamp(target.z(), minZ, maxZ);
                if (vec3s[0] == null || target.distanceToSqr(d, e, f) < target.distanceToSqr(vec3s[0])) {
                    vec3s[0] = new Vec3(d, e, f);
                }

            });
            return Optional.of(vec3s[0]);
        }
    }

    public VoxelShape getFaceShape(Direction facing) {
        if (!this.isEmpty() && this != Shapes.block()) {
            if (this.faces != null) {
                VoxelShape voxelShape = this.faces[facing.ordinal()];
                if (voxelShape != null) {
                    return voxelShape;
                }
            } else {
                this.faces = new VoxelShape[6];
            }

            VoxelShape voxelShape2 = this.calculateFace(facing);
            this.faces[facing.ordinal()] = voxelShape2;
            return voxelShape2;
        } else {
            return this;
        }
    }

    protected VoxelShape calculateFace(Direction direction) { // Paper
        Direction.Axis axis = direction.getAxis();
        DoubleList doubleList = this.getCoords(axis);
        if (doubleList.size() == 2 && DoubleMath.fuzzyEquals(doubleList.getDouble(0), 0.0D, 1.0E-7D) && DoubleMath.fuzzyEquals(doubleList.getDouble(1), 1.0D, 1.0E-7D)) {
            return this;
        } else {
            Direction.AxisDirection axisDirection = direction.getAxisDirection();
            int i = this.findIndex(axis, axisDirection == Direction.AxisDirection.POSITIVE ? 0.9999999D : 1.0E-7D);
            return new SliceShape(this, axis, i);
        }
    }

    public double collide(Direction.Axis axis, AABB box, double maxDist) {
        return this.collideX(AxisCycle.between(axis, Direction.Axis.X), box, maxDist);
    }

    protected double collideX(AxisCycle axisCycle, AABB box, double maxDist) {
        if (this.isEmpty()) {
            return maxDist;
        } else if (Math.abs(maxDist) < 1.0E-7D) {
            return 0.0D;
        } else {
            AxisCycle axisCycle2 = axisCycle.inverse();
            Direction.Axis axis = axisCycle2.cycle(Direction.Axis.X);
            Direction.Axis axis2 = axisCycle2.cycle(Direction.Axis.Y);
            Direction.Axis axis3 = axisCycle2.cycle(Direction.Axis.Z);
            double d = box.max(axis);
            double e = box.min(axis);
            int i = this.findIndex(axis, e + 1.0E-7D);
            int j = this.findIndex(axis, d - 1.0E-7D);
            int k = Math.max(0, this.findIndex(axis2, box.min(axis2) + 1.0E-7D));
            int l = Math.min(this.shape.getSize(axis2), this.findIndex(axis2, box.max(axis2) - 1.0E-7D) + 1);
            int m = Math.max(0, this.findIndex(axis3, box.min(axis3) + 1.0E-7D));
            int n = Math.min(this.shape.getSize(axis3), this.findIndex(axis3, box.max(axis3) - 1.0E-7D) + 1);
            int o = this.shape.getSize(axis);
            if (maxDist > 0.0D) {
                for(int p = j + 1; p < o; ++p) {
                    for(int q = k; q < l; ++q) {
                        for(int r = m; r < n; ++r) {
                            if (this.shape.isFullWide(axisCycle2, p, q, r)) {
                                double f = this.get(axis, p) - d;
                                if (f >= -1.0E-7D) {
                                    maxDist = Math.min(maxDist, f);
                                }

                                return maxDist;
                            }
                        }
                    }
                }
            } else if (maxDist < 0.0D) {
                for(int s = i - 1; s >= 0; --s) {
                    for(int t = k; t < l; ++t) {
                        for(int u = m; u < n; ++u) {
                            if (this.shape.isFullWide(axisCycle2, s, t, u)) {
                                double g = this.get(axis, s + 1) - e;
                                if (g <= 1.0E-7D) {
                                    maxDist = Math.max(maxDist, g);
                                }

                                return maxDist;
                            }
                        }
                    }
                }
            }

            return maxDist;
        }
    }

    @Override
    public String toString() {
        return this.isEmpty() ? "EMPTY" : "VoxelShape[" + this.bounds() + "]";
    }
}
