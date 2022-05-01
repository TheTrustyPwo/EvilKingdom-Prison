package com.mojang.math;

import net.minecraft.util.Mth;

public final class Quaternion {
    public static final Quaternion ONE = new Quaternion(0.0F, 0.0F, 0.0F, 1.0F);
    private float i;
    private float j;
    private float k;
    private float r;

    public Quaternion(float x, float y, float z, float w) {
        this.i = x;
        this.j = y;
        this.k = z;
        this.r = w;
    }

    public Quaternion(Vector3f axis, float rotationAngle, boolean degrees) {
        if (degrees) {
            rotationAngle *= ((float)Math.PI / 180F);
        }

        float f = sin(rotationAngle / 2.0F);
        this.i = axis.x() * f;
        this.j = axis.y() * f;
        this.k = axis.z() * f;
        this.r = cos(rotationAngle / 2.0F);
    }

    public Quaternion(float x, float y, float z, boolean degrees) {
        if (degrees) {
            x *= ((float)Math.PI / 180F);
            y *= ((float)Math.PI / 180F);
            z *= ((float)Math.PI / 180F);
        }

        float f = sin(0.5F * x);
        float g = cos(0.5F * x);
        float h = sin(0.5F * y);
        float i = cos(0.5F * y);
        float j = sin(0.5F * z);
        float k = cos(0.5F * z);
        this.i = f * i * k + g * h * j;
        this.j = g * h * k - f * i * j;
        this.k = f * h * k + g * i * j;
        this.r = g * i * k - f * h * j;
    }

    public Quaternion(Quaternion other) {
        this.i = other.i;
        this.j = other.j;
        this.k = other.k;
        this.r = other.r;
    }

    public static Quaternion fromYXZ(float x, float y, float z) {
        Quaternion quaternion = ONE.copy();
        quaternion.mul(new Quaternion(0.0F, (float)Math.sin((double)(x / 2.0F)), 0.0F, (float)Math.cos((double)(x / 2.0F))));
        quaternion.mul(new Quaternion((float)Math.sin((double)(y / 2.0F)), 0.0F, 0.0F, (float)Math.cos((double)(y / 2.0F))));
        quaternion.mul(new Quaternion(0.0F, 0.0F, (float)Math.sin((double)(z / 2.0F)), (float)Math.cos((double)(z / 2.0F))));
        return quaternion;
    }

    public static Quaternion fromXYZDegrees(Vector3f vector) {
        return fromXYZ((float)Math.toRadians((double)vector.x()), (float)Math.toRadians((double)vector.y()), (float)Math.toRadians((double)vector.z()));
    }

    public static Quaternion fromXYZ(Vector3f vector) {
        return fromXYZ(vector.x(), vector.y(), vector.z());
    }

    public static Quaternion fromXYZ(float x, float y, float z) {
        Quaternion quaternion = ONE.copy();
        quaternion.mul(new Quaternion((float)Math.sin((double)(x / 2.0F)), 0.0F, 0.0F, (float)Math.cos((double)(x / 2.0F))));
        quaternion.mul(new Quaternion(0.0F, (float)Math.sin((double)(y / 2.0F)), 0.0F, (float)Math.cos((double)(y / 2.0F))));
        quaternion.mul(new Quaternion(0.0F, 0.0F, (float)Math.sin((double)(z / 2.0F)), (float)Math.cos((double)(z / 2.0F))));
        return quaternion;
    }

    public Vector3f toXYZ() {
        float f = this.r() * this.r();
        float g = this.i() * this.i();
        float h = this.j() * this.j();
        float i = this.k() * this.k();
        float j = f + g + h + i;
        float k = 2.0F * this.r() * this.i() - 2.0F * this.j() * this.k();
        float l = (float)Math.asin((double)(k / j));
        return Math.abs(k) > 0.999F * j ? new Vector3f(2.0F * (float)Math.atan2((double)this.i(), (double)this.r()), l, 0.0F) : new Vector3f((float)Math.atan2((double)(2.0F * this.j() * this.k() + 2.0F * this.i() * this.r()), (double)(f - g - h + i)), l, (float)Math.atan2((double)(2.0F * this.i() * this.j() + 2.0F * this.r() * this.k()), (double)(f + g - h - i)));
    }

    public Vector3f toXYZDegrees() {
        Vector3f vector3f = this.toXYZ();
        return new Vector3f((float)Math.toDegrees((double)vector3f.x()), (float)Math.toDegrees((double)vector3f.y()), (float)Math.toDegrees((double)vector3f.z()));
    }

    public Vector3f toYXZ() {
        float f = this.r() * this.r();
        float g = this.i() * this.i();
        float h = this.j() * this.j();
        float i = this.k() * this.k();
        float j = f + g + h + i;
        float k = 2.0F * this.r() * this.i() - 2.0F * this.j() * this.k();
        float l = (float)Math.asin((double)(k / j));
        return Math.abs(k) > 0.999F * j ? new Vector3f(l, 2.0F * (float)Math.atan2((double)this.j(), (double)this.r()), 0.0F) : new Vector3f(l, (float)Math.atan2((double)(2.0F * this.i() * this.k() + 2.0F * this.j() * this.r()), (double)(f - g - h + i)), (float)Math.atan2((double)(2.0F * this.i() * this.j() + 2.0F * this.r() * this.k()), (double)(f - g + h - i)));
    }

    public Vector3f toYXZDegrees() {
        Vector3f vector3f = this.toYXZ();
        return new Vector3f((float)Math.toDegrees((double)vector3f.x()), (float)Math.toDegrees((double)vector3f.y()), (float)Math.toDegrees((double)vector3f.z()));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            Quaternion quaternion = (Quaternion)object;
            if (Float.compare(quaternion.i, this.i) != 0) {
                return false;
            } else if (Float.compare(quaternion.j, this.j) != 0) {
                return false;
            } else if (Float.compare(quaternion.k, this.k) != 0) {
                return false;
            } else {
                return Float.compare(quaternion.r, this.r) == 0;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int i = Float.floatToIntBits(this.i);
        i = 31 * i + Float.floatToIntBits(this.j);
        i = 31 * i + Float.floatToIntBits(this.k);
        return 31 * i + Float.floatToIntBits(this.r);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Quaternion[").append(this.r()).append(" + ");
        stringBuilder.append(this.i()).append("i + ");
        stringBuilder.append(this.j()).append("j + ");
        stringBuilder.append(this.k()).append("k]");
        return stringBuilder.toString();
    }

    public float i() {
        return this.i;
    }

    public float j() {
        return this.j;
    }

    public float k() {
        return this.k;
    }

    public float r() {
        return this.r;
    }

    public void mul(Quaternion other) {
        float f = this.i();
        float g = this.j();
        float h = this.k();
        float i = this.r();
        float j = other.i();
        float k = other.j();
        float l = other.k();
        float m = other.r();
        this.i = i * j + f * m + g * l - h * k;
        this.j = i * k - f * l + g * m + h * j;
        this.k = i * l + f * k - g * j + h * m;
        this.r = i * m - f * j - g * k - h * l;
    }

    public void mul(float scale) {
        this.i *= scale;
        this.j *= scale;
        this.k *= scale;
        this.r *= scale;
    }

    public void conj() {
        this.i = -this.i;
        this.j = -this.j;
        this.k = -this.k;
    }

    public void set(float x, float y, float z, float w) {
        this.i = x;
        this.j = y;
        this.k = z;
        this.r = w;
    }

    private static float cos(float value) {
        return (float)Math.cos((double)value);
    }

    private static float sin(float value) {
        return (float)Math.sin((double)value);
    }

    public void normalize() {
        float f = this.i() * this.i() + this.j() * this.j() + this.k() * this.k() + this.r() * this.r();
        if (f > 1.0E-6F) {
            float g = Mth.fastInvSqrt(f);
            this.i *= g;
            this.j *= g;
            this.k *= g;
            this.r *= g;
        } else {
            this.i = 0.0F;
            this.j = 0.0F;
            this.k = 0.0F;
            this.r = 0.0F;
        }

    }

    public void slerp(Quaternion quaternion, float f) {
        throw new UnsupportedOperationException();
    }

    public Quaternion copy() {
        return new Quaternion(this);
    }
}
