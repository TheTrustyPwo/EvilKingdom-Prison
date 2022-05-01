package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import net.minecraft.Util;
import net.minecraft.core.Direction;

// Paper start
import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
// Paper end
public class ArrayVoxelShape extends VoxelShape {
    private final DoubleList xs;
    private final DoubleList ys;
    private final DoubleList zs;

    protected ArrayVoxelShape(DiscreteVoxelShape shape, double[] xPoints, double[] yPoints, double[] zPoints) {
        this(shape, (DoubleList)DoubleArrayList.wrap(Arrays.copyOf(xPoints, shape.getXSize() + 1)), (DoubleList)DoubleArrayList.wrap(Arrays.copyOf(yPoints, shape.getYSize() + 1)), (DoubleList)DoubleArrayList.wrap(Arrays.copyOf(zPoints, shape.getZSize() + 1)));
    }

    ArrayVoxelShape(DiscreteVoxelShape shape, DoubleList xPoints, DoubleList yPoints, DoubleList zPoints) {
        // Paper start - optimise multi-aabb shapes
        this(shape, xPoints, yPoints, zPoints, null, 0.0, 0.0, 0.0);
    }
    ArrayVoxelShape(DiscreteVoxelShape shape, DoubleList xPoints, DoubleList yPoints, DoubleList zPoints, net.minecraft.world.phys.AABB[] boundingBoxesRepresentation, double offsetX, double offsetY, double offsetZ) {
        // Paper end - optimise multi-aabb shapes
        super(shape);
        int i = shape.getXSize() + 1;
        int j = shape.getYSize() + 1;
        int k = shape.getZSize() + 1;
        if (i == xPoints.size() && j == yPoints.size() && k == zPoints.size()) {
            this.xs = xPoints;
            this.ys = yPoints;
            this.zs = zPoints;
        } else {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Lengths of point arrays must be consistent with the size of the VoxelShape."));
        }
        // Paper start - optimise multi-aabb shapes
        this.boundingBoxesRepresentation = boundingBoxesRepresentation == null ? this.toAabbs().toArray(EMPTY) : boundingBoxesRepresentation;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        // Paper end - optimise multi-aabb shapes
    }

    @Override
    protected DoubleList getCoords(Direction.Axis axis) {
        switch(axis) {
        case X:
            return this.xs;
        case Y:
            return this.ys;
        case Z:
            return this.zs;
        default:
            throw new IllegalArgumentException();
        }
    }

    // Paper start
    public static final class DoubleListOffsetExposed extends AbstractDoubleList {

        public final DoubleArrayList list;
        public final double offset;

        public DoubleListOffsetExposed(final DoubleArrayList list, final double offset) {
            this.list = list;
            this.offset = offset;
        }

        @Override
        public double getDouble(final int index) {
            return this.list.getDouble(index) + this.offset;
        }

        @Override
        public int size() {
            return this.list.size();
        }
    }

    static final net.minecraft.world.phys.AABB[] EMPTY = new net.minecraft.world.phys.AABB[0];
    final net.minecraft.world.phys.AABB[] boundingBoxesRepresentation;

    final double offsetX;
    final double offsetY;
    final double offsetZ;

    public final net.minecraft.world.phys.AABB[] getBoundingBoxesRepresentation() {
        return this.boundingBoxesRepresentation;
    }

    public final double getOffsetX() {
        return this.offsetX;
    }

    public final double getOffsetY() {
        return this.offsetY;
    }

    public final double getOffsetZ() {
        return this.offsetZ;
    }

    @Override
    public java.util.List<net.minecraft.world.phys.AABB> toAabbs() {
        if (this.boundingBoxesRepresentation == null) {
            return super.toAabbs();
        }
        java.util.List<net.minecraft.world.phys.AABB> ret = new java.util.ArrayList<>(this.boundingBoxesRepresentation.length);

        double offX = this.offsetX;
        double offY = this.offsetY;
        double offZ = this.offsetZ;

        for (net.minecraft.world.phys.AABB boundingBox : this.boundingBoxesRepresentation) {
            ret.add(boundingBox.move(offX, offY, offZ));
        }

        return ret;
    }

    protected static DoubleArrayList getList(DoubleList from) {
        if (from instanceof DoubleArrayList) {
            return (DoubleArrayList)from;
        } else {
            return DoubleArrayList.wrap(from.toDoubleArray());
        }
    }

    @Override
    public VoxelShape move(double x, double y, double z) {
        if (x == 0.0 && y == 0.0 && z == 0.0) {
            return this;
        }
        DoubleListOffsetExposed xPoints, yPoints, zPoints;
        double offsetX, offsetY, offsetZ;

        if (this.xs instanceof DoubleListOffsetExposed) {
            xPoints = new DoubleListOffsetExposed(((DoubleListOffsetExposed)this.xs).list, offsetX = this.offsetX + x);
            yPoints = new DoubleListOffsetExposed(((DoubleListOffsetExposed)this.ys).list, offsetY = this.offsetY + y);
            zPoints = new DoubleListOffsetExposed(((DoubleListOffsetExposed)this.zs).list, offsetZ = this.offsetZ + z);
        } else {
            xPoints = new DoubleListOffsetExposed(getList(this.xs), offsetX = x);
            yPoints = new DoubleListOffsetExposed(getList(this.ys), offsetY = y);
            zPoints = new DoubleListOffsetExposed(getList(this.zs), offsetZ = z);
        }

        return new ArrayVoxelShape(this.shape, xPoints, yPoints, zPoints, this.boundingBoxesRepresentation, offsetX, offsetY, offsetZ);
    }

    @Override
    public final boolean intersects(net.minecraft.world.phys.AABB axisalingedbb) {
        // this can be optimised by checking an "overall shape" first, but not needed
        double offX = this.offsetX;
        double offY = this.offsetY;
        double offZ = this.offsetZ;

        for (net.minecraft.world.phys.AABB boundingBox : this.boundingBoxesRepresentation) {
            if (io.papermc.paper.util.CollisionUtil.voxelShapeIntersect(axisalingedbb, boundingBox.minX + offX, boundingBox.minY + offY, boundingBox.minZ + offZ,
                    boundingBox.maxX + offX, boundingBox.maxY + offY, boundingBox.maxZ + offZ)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void forAllBoxes(Shapes.DoubleLineConsumer doubleLineConsumer) {
        if (this.boundingBoxesRepresentation == null) {
            super.forAllBoxes(doubleLineConsumer);
            return;
        }
        for (final net.minecraft.world.phys.AABB boundingBox : this.boundingBoxesRepresentation) {
            doubleLineConsumer.consume(boundingBox.minX + this.offsetX, boundingBox.minY + this.offsetY, boundingBox.minZ + this.offsetZ,
                boundingBox.maxX + this.offsetX, boundingBox.maxY + this.offsetY, boundingBox.maxZ + this.offsetZ);
        }
    }

    @Override
    public VoxelShape optimize() {
        if (this == Shapes.empty() || this.boundingBoxesRepresentation.length == 0) {
            return this;
        }

        VoxelShape simplified = Shapes.empty();
        for (final net.minecraft.world.phys.AABB boundingBox : this.boundingBoxesRepresentation) {
            simplified = Shapes.joinUnoptimized(simplified, Shapes.box(boundingBox.minX + this.offsetX, boundingBox.minY + this.offsetY, boundingBox.minZ + this.offsetZ,
                    boundingBox.maxX + this.offsetX, boundingBox.maxY + this.offsetY, boundingBox.maxZ + this.offsetZ), BooleanOp.OR);
        }

        if (!(simplified instanceof ArrayVoxelShape)) {
            return simplified;
        }

        final net.minecraft.world.phys.AABB[] boundingBoxesRepresentation = ((ArrayVoxelShape)simplified).getBoundingBoxesRepresentation();

        if (boundingBoxesRepresentation.length == 1) {
            return new io.papermc.paper.voxel.AABBVoxelShape(boundingBoxesRepresentation[0]).optimize();
        }

        return simplified;
    }
    // Paper end

}
