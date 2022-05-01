package com.mojang.math;

import net.minecraft.util.Mth;

public class Vector4f {
    private float x;
    private float y;
    private float z;
    private float w;

    public Vector4f() {
    }

    public Vector4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4f(Vector3f vector) {
        this(vector.x(), vector.y(), vector.z(), 1.0F);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            Vector4f vector4f = (Vector4f)object;
            if (Float.compare(vector4f.x, this.x) != 0) {
                return false;
            } else if (Float.compare(vector4f.y, this.y) != 0) {
                return false;
            } else if (Float.compare(vector4f.z, this.z) != 0) {
                return false;
            } else {
                return Float.compare(vector4f.w, this.w) == 0;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int i = Float.floatToIntBits(this.x);
        i = 31 * i + Float.floatToIntBits(this.y);
        i = 31 * i + Float.floatToIntBits(this.z);
        return 31 * i + Float.floatToIntBits(this.w);
    }

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float z() {
        return this.z;
    }

    public float w() {
        return this.w;
    }

    public void mul(float value) {
        this.x *= value;
        this.y *= value;
        this.z *= value;
        this.w *= value;
    }

    public void mul(Vector3f vector) {
        this.x *= vector.x();
        this.y *= vector.y();
        this.z *= vector.z();
    }

    public void set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void add(float x, float y, float z, float w) {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
    }

    public float dot(Vector4f other) {
        return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
    }

    public boolean normalize() {
        float f = this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w;
        if ((double)f < 1.0E-5D) {
            return false;
        } else {
            float g = Mth.fastInvSqrt(f);
            this.x *= g;
            this.y *= g;
            this.z *= g;
            this.w *= g;
            return true;
        }
    }

    public void transform(Matrix4f matrix) {
        float f = this.x;
        float g = this.y;
        float h = this.z;
        float i = this.w;
        this.x = matrix.m00 * f + matrix.m01 * g + matrix.m02 * h + matrix.m03 * i;
        this.y = matrix.m10 * f + matrix.m11 * g + matrix.m12 * h + matrix.m13 * i;
        this.z = matrix.m20 * f + matrix.m21 * g + matrix.m22 * h + matrix.m23 * i;
        this.w = matrix.m30 * f + matrix.m31 * g + matrix.m32 * h + matrix.m33 * i;
    }

    public void transform(Quaternion rotation) {
        Quaternion quaternion = new Quaternion(rotation);
        quaternion.mul(new Quaternion(this.x(), this.y(), this.z(), 0.0F));
        Quaternion quaternion2 = new Quaternion(rotation);
        quaternion2.conj();
        quaternion.mul(quaternion2);
        this.set(quaternion.i(), quaternion.j(), quaternion.k(), this.w());
    }

    public void perspectiveDivide() {
        this.x /= this.w;
        this.y /= this.w;
        this.z /= this.w;
        this.w = 1.0F;
    }

    public void lerp(Vector4f to, float delta) {
        float f = 1.0F - delta;
        this.x = this.x * f + to.x * delta;
        this.y = this.y * f + to.y * delta;
        this.z = this.z * f + to.z * delta;
        this.w = this.w * f + to.w * delta;
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.y + ", " + this.z + ", " + this.w + "]";
    }
}
