package net.minecraft.core;

public class PositionImpl implements Position {
    protected final double x;
    protected final double y;
    protected final double z;

    public PositionImpl(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public double x() {
        return this.x;
    }

    @Override
    public double y() {
        return this.y;
    }

    @Override
    public double z() {
        return this.z;
    }
}
