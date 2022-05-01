package com.mojang.math;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Vector3f {
    public static final Codec<Vector3f> CODEC = Codec.FLOAT.listOf().comapFlatMap((vec) -> {
        return Util.fixedSize(vec, 3).map((vecx) -> {
            return new Vector3f(vecx.get(0), vecx.get(1), vecx.get(2));
        });
    }, (vec) -> {
        return ImmutableList.of(vec.x, vec.y, vec.z);
    });
    public static Vector3f XN = new Vector3f(-1.0F, 0.0F, 0.0F);
    public static Vector3f XP = new Vector3f(1.0F, 0.0F, 0.0F);
    public static Vector3f YN = new Vector3f(0.0F, -1.0F, 0.0F);
    public static Vector3f YP = new Vector3f(0.0F, 1.0F, 0.0F);
    public static Vector3f ZN = new Vector3f(0.0F, 0.0F, -1.0F);
    public static Vector3f ZP = new Vector3f(0.0F, 0.0F, 1.0F);
    public static Vector3f ZERO = new Vector3f(0.0F, 0.0F, 0.0F);
    private float x;
    private float y;
    private float z;

    public Vector3f() {
    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3f(Vector4f vec) {
        this(vec.x(), vec.y(), vec.z());
    }

    public Vector3f(Vec3 other) {
        this((float)other.x, (float)other.y, (float)other.z);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            Vector3f vector3f = (Vector3f)object;
            if (Float.compare(vector3f.x, this.x) != 0) {
                return false;
            } else if (Float.compare(vector3f.y, this.y) != 0) {
                return false;
            } else {
                return Float.compare(vector3f.z, this.z) == 0;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int i = Float.floatToIntBits(this.x);
        i = 31 * i + Float.floatToIntBits(this.y);
        return 31 * i + Float.floatToIntBits(this.z);
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

    public void mul(float scale) {
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;
    }

    public void mul(float x, float y, float z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
    }

    public void clamp(Vector3f min, Vector3f max) {
        this.x = Mth.clamp(this.x, min.x(), max.x());
        this.y = Mth.clamp(this.y, min.x(), max.y());
        this.z = Mth.clamp(this.z, min.z(), max.z());
    }

    public void clamp(float min, float max) {
        this.x = Mth.clamp(this.x, min, max);
        this.y = Mth.clamp(this.y, min, max);
        this.z = Mth.clamp(this.z, min, max);
    }

    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void load(Vector3f vec) {
        this.x = vec.x;
        this.y = vec.y;
        this.z = vec.z;
    }

    public void add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
    }

    public void add(Vector3f vector) {
        this.x += vector.x;
        this.y += vector.y;
        this.z += vector.z;
    }

    public void sub(Vector3f other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
    }

    public float dot(Vector3f other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public boolean normalize() {
        float f = this.x * this.x + this.y * this.y + this.z * this.z;
        if ((double)f < 1.0E-5D) {
            return false;
        } else {
            float g = Mth.fastInvSqrt(f);
            this.x *= g;
            this.y *= g;
            this.z *= g;
            return true;
        }
    }

    public void cross(Vector3f vector) {
        float f = this.x;
        float g = this.y;
        float h = this.z;
        float i = vector.x();
        float j = vector.y();
        float k = vector.z();
        this.x = g * k - h * j;
        this.y = h * i - f * k;
        this.z = f * j - g * i;
    }

    public void transform(Matrix3f matrix) {
        float f = this.x;
        float g = this.y;
        float h = this.z;
        this.x = matrix.m00 * f + matrix.m01 * g + matrix.m02 * h;
        this.y = matrix.m10 * f + matrix.m11 * g + matrix.m12 * h;
        this.z = matrix.m20 * f + matrix.m21 * g + matrix.m22 * h;
    }

    public void transform(Quaternion rotation) {
        Quaternion quaternion = new Quaternion(rotation);
        quaternion.mul(new Quaternion(this.x(), this.y(), this.z(), 0.0F));
        Quaternion quaternion2 = new Quaternion(rotation);
        quaternion2.conj();
        quaternion.mul(quaternion2);
        this.set(quaternion.i(), quaternion.j(), quaternion.k());
    }

    public void lerp(Vector3f vector, float delta) {
        float f = 1.0F - delta;
        this.x = this.x * f + vector.x * delta;
        this.y = this.y * f + vector.y * delta;
        this.z = this.z * f + vector.z * delta;
    }

    public Quaternion rotation(float angle) {
        return new Quaternion(this, angle, false);
    }

    public Quaternion rotationDegrees(float angle) {
        return new Quaternion(this, angle, true);
    }

    public Vector3f copy() {
        return new Vector3f(this.x, this.y, this.z);
    }

    public void map(Float2FloatFunction function) {
        this.x = function.get(this.x);
        this.y = function.get(this.y);
        this.z = function.get(this.z);
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.y + ", " + this.z + "]";
    }
}
