package net.minecraft.world.phys.shapes;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class SubShape extends DiscreteVoxelShape {
    private final DiscreteVoxelShape parent;
    private final int startX;
    private final int startY;
    private final int startZ;
    private final int endX;
    private final int endY;
    private final int endZ;

    protected SubShape(DiscreteVoxelShape parent, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        super(maxX - minX, maxY - minY, maxZ - minZ);
        this.parent = parent;
        this.startX = minX;
        this.startY = minY;
        this.startZ = minZ;
        this.endX = maxX;
        this.endY = maxY;
        this.endZ = maxZ;
    }

    @Override
    public boolean isFull(int x, int y, int z) {
        return this.parent.isFull(this.startX + x, this.startY + y, this.startZ + z);
    }

    @Override
    public void fill(int x, int y, int z) {
        this.parent.fill(this.startX + x, this.startY + y, this.startZ + z);
    }

    @Override
    public int firstFull(Direction.Axis axis) {
        return this.clampToShape(axis, this.parent.firstFull(axis));
    }

    @Override
    public int lastFull(Direction.Axis axis) {
        return this.clampToShape(axis, this.parent.lastFull(axis));
    }

    private int clampToShape(Direction.Axis axis, int value) {
        int i = axis.choose(this.startX, this.startY, this.startZ);
        int j = axis.choose(this.endX, this.endY, this.endZ);
        return Mth.clamp(value, i, j) - i;
    }
}
