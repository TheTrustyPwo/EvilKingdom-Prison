package net.minecraft.world.phys;

import net.minecraft.util.Mth;

public class Vec2 {
    public static final Vec2 ZERO = new Vec2(0.0F, 0.0F);
    public static final Vec2 ONE = new Vec2(1.0F, 1.0F);
    public static final Vec2 UNIT_X = new Vec2(1.0F, 0.0F);
    public static final Vec2 NEG_UNIT_X = new Vec2(-1.0F, 0.0F);
    public static final Vec2 UNIT_Y = new Vec2(0.0F, 1.0F);
    public static final Vec2 NEG_UNIT_Y = new Vec2(0.0F, -1.0F);
    public static final Vec2 MAX = new Vec2(Float.MAX_VALUE, Float.MAX_VALUE);
    public static final Vec2 MIN = new Vec2(Float.MIN_VALUE, Float.MIN_VALUE);
    public final float x;
    public final float y;

    public Vec2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2 scale(float value) {
        return new Vec2(this.x * value, this.y * value);
    }

    public float dot(Vec2 vec) {
        return this.x * vec.x + this.y * vec.y;
    }

    public Vec2 add(Vec2 vec) {
        return new Vec2(this.x + vec.x, this.y + vec.y);
    }

    public Vec2 add(float value) {
        return new Vec2(this.x + value, this.y + value);
    }

    public boolean equals(Vec2 other) {
        return this.x == other.x && this.y == other.y;
    }

    public Vec2 normalized() {
        float f = Mth.sqrt(this.x * this.x + this.y * this.y);
        return f < 1.0E-4F ? ZERO : new Vec2(this.x / f, this.y / f);
    }

    public float length() {
        return Mth.sqrt(this.x * this.x + this.y * this.y);
    }

    public float lengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    public float distanceToSqr(Vec2 vec) {
        float f = vec.x - this.x;
        float g = vec.y - this.y;
        return f * f + g * g;
    }

    public Vec2 negated() {
        return new Vec2(-this.x, -this.y);
    }
}
