package com.mojang.math;

import com.mojang.datafixers.util.Pair;
import java.nio.FloatBuffer;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.tuple.Triple;

public final class Matrix3f {
    private static final int ORDER = 3;
    private static final float G = 3.0F + 2.0F * (float)Math.sqrt(2.0D);
    private static final float CS = (float)Math.cos((Math.PI / 8D));
    private static final float SS = (float)Math.sin((Math.PI / 8D));
    private static final float SQ2 = 1.0F / (float)Math.sqrt(2.0D);
    protected float m00;
    protected float m01;
    protected float m02;
    protected float m10;
    protected float m11;
    protected float m12;
    protected float m20;
    protected float m21;
    protected float m22;

    public Matrix3f() {
    }

    public Matrix3f(Quaternion quaternion) {
        float f = quaternion.i();
        float g = quaternion.j();
        float h = quaternion.k();
        float i = quaternion.r();
        float j = 2.0F * f * f;
        float k = 2.0F * g * g;
        float l = 2.0F * h * h;
        this.m00 = 1.0F - k - l;
        this.m11 = 1.0F - l - j;
        this.m22 = 1.0F - j - k;
        float m = f * g;
        float n = g * h;
        float o = h * f;
        float p = f * i;
        float q = g * i;
        float r = h * i;
        this.m10 = 2.0F * (m + r);
        this.m01 = 2.0F * (m - r);
        this.m20 = 2.0F * (o - q);
        this.m02 = 2.0F * (o + q);
        this.m21 = 2.0F * (n + p);
        this.m12 = 2.0F * (n - p);
    }

    public static Matrix3f createScaleMatrix(float x, float y, float z) {
        Matrix3f matrix3f = new Matrix3f();
        matrix3f.m00 = x;
        matrix3f.m11 = y;
        matrix3f.m22 = z;
        return matrix3f;
    }

    public Matrix3f(Matrix4f matrix) {
        this.m00 = matrix.m00;
        this.m01 = matrix.m01;
        this.m02 = matrix.m02;
        this.m10 = matrix.m10;
        this.m11 = matrix.m11;
        this.m12 = matrix.m12;
        this.m20 = matrix.m20;
        this.m21 = matrix.m21;
        this.m22 = matrix.m22;
    }

    public Matrix3f(Matrix3f source) {
        this.m00 = source.m00;
        this.m01 = source.m01;
        this.m02 = source.m02;
        this.m10 = source.m10;
        this.m11 = source.m11;
        this.m12 = source.m12;
        this.m20 = source.m20;
        this.m21 = source.m21;
        this.m22 = source.m22;
    }

    private static Pair<Float, Float> approxGivensQuat(float upperLeft, float diagonalAverage, float lowerRight) {
        float f = 2.0F * (upperLeft - lowerRight);
        if (G * diagonalAverage * diagonalAverage < f * f) {
            float h = Mth.fastInvSqrt(diagonalAverage * diagonalAverage + f * f);
            return Pair.of(h * diagonalAverage, h * f);
        } else {
            return Pair.of(SS, CS);
        }
    }

    private static Pair<Float, Float> qrGivensQuat(float f, float g) {
        float h = (float)Math.hypot((double)f, (double)g);
        float i = h > 1.0E-6F ? g : 0.0F;
        float j = Math.abs(f) + Math.max(h, 1.0E-6F);
        if (f < 0.0F) {
            float k = i;
            i = j;
            j = k;
        }

        float l = Mth.fastInvSqrt(j * j + i * i);
        j *= l;
        i *= l;
        return Pair.of(i, j);
    }

    private static Quaternion stepJacobi(Matrix3f matrix) {
        Matrix3f matrix3f = new Matrix3f();
        Quaternion quaternion = Quaternion.ONE.copy();
        if (matrix.m01 * matrix.m01 + matrix.m10 * matrix.m10 > 1.0E-6F) {
            Pair<Float, Float> pair = approxGivensQuat(matrix.m00, 0.5F * (matrix.m01 + matrix.m10), matrix.m11);
            Float float_ = pair.getFirst();
            Float float2 = pair.getSecond();
            Quaternion quaternion2 = new Quaternion(0.0F, 0.0F, float_, float2);
            float f = float2 * float2 - float_ * float_;
            float g = -2.0F * float_ * float2;
            float h = float2 * float2 + float_ * float_;
            quaternion.mul(quaternion2);
            matrix3f.setIdentity();
            matrix3f.m00 = f;
            matrix3f.m11 = f;
            matrix3f.m10 = -g;
            matrix3f.m01 = g;
            matrix3f.m22 = h;
            matrix.mul(matrix3f);
            matrix3f.transpose();
            matrix3f.mul(matrix);
            matrix.load(matrix3f);
        }

        if (matrix.m02 * matrix.m02 + matrix.m20 * matrix.m20 > 1.0E-6F) {
            Pair<Float, Float> pair2 = approxGivensQuat(matrix.m00, 0.5F * (matrix.m02 + matrix.m20), matrix.m22);
            float i = -pair2.getFirst();
            Float float3 = pair2.getSecond();
            Quaternion quaternion3 = new Quaternion(0.0F, i, 0.0F, float3);
            float j = float3 * float3 - i * i;
            float k = -2.0F * i * float3;
            float l = float3 * float3 + i * i;
            quaternion.mul(quaternion3);
            matrix3f.setIdentity();
            matrix3f.m00 = j;
            matrix3f.m22 = j;
            matrix3f.m20 = k;
            matrix3f.m02 = -k;
            matrix3f.m11 = l;
            matrix.mul(matrix3f);
            matrix3f.transpose();
            matrix3f.mul(matrix);
            matrix.load(matrix3f);
        }

        if (matrix.m12 * matrix.m12 + matrix.m21 * matrix.m21 > 1.0E-6F) {
            Pair<Float, Float> pair3 = approxGivensQuat(matrix.m11, 0.5F * (matrix.m12 + matrix.m21), matrix.m22);
            Float float4 = pair3.getFirst();
            Float float5 = pair3.getSecond();
            Quaternion quaternion4 = new Quaternion(float4, 0.0F, 0.0F, float5);
            float m = float5 * float5 - float4 * float4;
            float n = -2.0F * float4 * float5;
            float o = float5 * float5 + float4 * float4;
            quaternion.mul(quaternion4);
            matrix3f.setIdentity();
            matrix3f.m11 = m;
            matrix3f.m22 = m;
            matrix3f.m21 = -n;
            matrix3f.m12 = n;
            matrix3f.m00 = o;
            matrix.mul(matrix3f);
            matrix3f.transpose();
            matrix3f.mul(matrix);
            matrix.load(matrix3f);
        }

        return quaternion;
    }

    private static void sortSingularValues(Matrix3f matrix, Quaternion quaternion) {
        float f = matrix.m00 * matrix.m00 + matrix.m10 * matrix.m10 + matrix.m20 * matrix.m20;
        float g = matrix.m01 * matrix.m01 + matrix.m11 * matrix.m11 + matrix.m21 * matrix.m21;
        float h = matrix.m02 * matrix.m02 + matrix.m12 * matrix.m12 + matrix.m22 * matrix.m22;
        if (f < g) {
            float i = matrix.m10;
            matrix.m10 = -matrix.m00;
            matrix.m00 = i;
            i = matrix.m11;
            matrix.m11 = -matrix.m01;
            matrix.m01 = i;
            i = matrix.m12;
            matrix.m12 = -matrix.m02;
            matrix.m02 = i;
            Quaternion quaternion2 = new Quaternion(0.0F, 0.0F, SQ2, SQ2);
            quaternion.mul(quaternion2);
            i = f;
            f = g;
            g = i;
        }

        if (f < h) {
            float j = matrix.m20;
            matrix.m20 = -matrix.m00;
            matrix.m00 = j;
            j = matrix.m21;
            matrix.m21 = -matrix.m01;
            matrix.m01 = j;
            j = matrix.m22;
            matrix.m22 = -matrix.m02;
            matrix.m02 = j;
            Quaternion quaternion3 = new Quaternion(0.0F, SQ2, 0.0F, SQ2);
            quaternion.mul(quaternion3);
            h = f;
        }

        if (g < h) {
            float k = matrix.m20;
            matrix.m20 = -matrix.m10;
            matrix.m10 = k;
            k = matrix.m21;
            matrix.m21 = -matrix.m11;
            matrix.m11 = k;
            k = matrix.m22;
            matrix.m22 = -matrix.m12;
            matrix.m12 = k;
            Quaternion quaternion4 = new Quaternion(SQ2, 0.0F, 0.0F, SQ2);
            quaternion.mul(quaternion4);
        }

    }

    public void transpose() {
        float f = this.m01;
        this.m01 = this.m10;
        this.m10 = f;
        f = this.m02;
        this.m02 = this.m20;
        this.m20 = f;
        f = this.m12;
        this.m12 = this.m21;
        this.m21 = f;
    }

    public Triple<Quaternion, Vector3f, Quaternion> svdDecompose() {
        Quaternion quaternion = Quaternion.ONE.copy();
        Quaternion quaternion2 = Quaternion.ONE.copy();
        Matrix3f matrix3f = this.copy();
        matrix3f.transpose();
        matrix3f.mul(this);

        for(int i = 0; i < 5; ++i) {
            quaternion2.mul(stepJacobi(matrix3f));
        }

        quaternion2.normalize();
        Matrix3f matrix3f2 = new Matrix3f(this);
        matrix3f2.mul(new Matrix3f(quaternion2));
        float f = 1.0F;
        Pair<Float, Float> pair = qrGivensQuat(matrix3f2.m00, matrix3f2.m10);
        Float float_ = pair.getFirst();
        Float float2 = pair.getSecond();
        float g = float2 * float2 - float_ * float_;
        float h = -2.0F * float_ * float2;
        float j = float2 * float2 + float_ * float_;
        Quaternion quaternion3 = new Quaternion(0.0F, 0.0F, float_, float2);
        quaternion.mul(quaternion3);
        Matrix3f matrix3f3 = new Matrix3f();
        matrix3f3.setIdentity();
        matrix3f3.m00 = g;
        matrix3f3.m11 = g;
        matrix3f3.m10 = h;
        matrix3f3.m01 = -h;
        matrix3f3.m22 = j;
        f *= j;
        matrix3f3.mul(matrix3f2);
        pair = qrGivensQuat(matrix3f3.m00, matrix3f3.m20);
        float k = -pair.getFirst();
        Float float3 = pair.getSecond();
        float l = float3 * float3 - k * k;
        float m = -2.0F * k * float3;
        float n = float3 * float3 + k * k;
        Quaternion quaternion4 = new Quaternion(0.0F, k, 0.0F, float3);
        quaternion.mul(quaternion4);
        Matrix3f matrix3f4 = new Matrix3f();
        matrix3f4.setIdentity();
        matrix3f4.m00 = l;
        matrix3f4.m22 = l;
        matrix3f4.m20 = -m;
        matrix3f4.m02 = m;
        matrix3f4.m11 = n;
        f *= n;
        matrix3f4.mul(matrix3f3);
        pair = qrGivensQuat(matrix3f4.m11, matrix3f4.m21);
        Float float4 = pair.getFirst();
        Float float5 = pair.getSecond();
        float o = float5 * float5 - float4 * float4;
        float p = -2.0F * float4 * float5;
        float q = float5 * float5 + float4 * float4;
        Quaternion quaternion5 = new Quaternion(float4, 0.0F, 0.0F, float5);
        quaternion.mul(quaternion5);
        Matrix3f matrix3f5 = new Matrix3f();
        matrix3f5.setIdentity();
        matrix3f5.m11 = o;
        matrix3f5.m22 = o;
        matrix3f5.m21 = p;
        matrix3f5.m12 = -p;
        matrix3f5.m00 = q;
        f *= q;
        matrix3f5.mul(matrix3f4);
        f = 1.0F / f;
        quaternion.mul((float)Math.sqrt((double)f));
        Vector3f vector3f = new Vector3f(matrix3f5.m00 * f, matrix3f5.m11 * f, matrix3f5.m22 * f);
        return Triple.of(quaternion, vector3f, quaternion2);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            Matrix3f matrix3f = (Matrix3f)object;
            return Float.compare(matrix3f.m00, this.m00) == 0 && Float.compare(matrix3f.m01, this.m01) == 0 && Float.compare(matrix3f.m02, this.m02) == 0 && Float.compare(matrix3f.m10, this.m10) == 0 && Float.compare(matrix3f.m11, this.m11) == 0 && Float.compare(matrix3f.m12, this.m12) == 0 && Float.compare(matrix3f.m20, this.m20) == 0 && Float.compare(matrix3f.m21, this.m21) == 0 && Float.compare(matrix3f.m22, this.m22) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int i = this.m00 != 0.0F ? Float.floatToIntBits(this.m00) : 0;
        i = 31 * i + (this.m01 != 0.0F ? Float.floatToIntBits(this.m01) : 0);
        i = 31 * i + (this.m02 != 0.0F ? Float.floatToIntBits(this.m02) : 0);
        i = 31 * i + (this.m10 != 0.0F ? Float.floatToIntBits(this.m10) : 0);
        i = 31 * i + (this.m11 != 0.0F ? Float.floatToIntBits(this.m11) : 0);
        i = 31 * i + (this.m12 != 0.0F ? Float.floatToIntBits(this.m12) : 0);
        i = 31 * i + (this.m20 != 0.0F ? Float.floatToIntBits(this.m20) : 0);
        i = 31 * i + (this.m21 != 0.0F ? Float.floatToIntBits(this.m21) : 0);
        return 31 * i + (this.m22 != 0.0F ? Float.floatToIntBits(this.m22) : 0);
    }

    private static int bufferIndex(int x, int y) {
        return y * 3 + x;
    }

    public void load(FloatBuffer buf) {
        this.m00 = buf.get(bufferIndex(0, 0));
        this.m01 = buf.get(bufferIndex(0, 1));
        this.m02 = buf.get(bufferIndex(0, 2));
        this.m10 = buf.get(bufferIndex(1, 0));
        this.m11 = buf.get(bufferIndex(1, 1));
        this.m12 = buf.get(bufferIndex(1, 2));
        this.m20 = buf.get(bufferIndex(2, 0));
        this.m21 = buf.get(bufferIndex(2, 1));
        this.m22 = buf.get(bufferIndex(2, 2));
    }

    public void loadTransposed(FloatBuffer buf) {
        this.m00 = buf.get(bufferIndex(0, 0));
        this.m01 = buf.get(bufferIndex(1, 0));
        this.m02 = buf.get(bufferIndex(2, 0));
        this.m10 = buf.get(bufferIndex(0, 1));
        this.m11 = buf.get(bufferIndex(1, 1));
        this.m12 = buf.get(bufferIndex(2, 1));
        this.m20 = buf.get(bufferIndex(0, 2));
        this.m21 = buf.get(bufferIndex(1, 2));
        this.m22 = buf.get(bufferIndex(2, 2));
    }

    public void load(FloatBuffer buf, boolean rowMajor) {
        if (rowMajor) {
            this.loadTransposed(buf);
        } else {
            this.load(buf);
        }

    }

    public void load(Matrix3f source) {
        this.m00 = source.m00;
        this.m01 = source.m01;
        this.m02 = source.m02;
        this.m10 = source.m10;
        this.m11 = source.m11;
        this.m12 = source.m12;
        this.m20 = source.m20;
        this.m21 = source.m21;
        this.m22 = source.m22;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Matrix3f:\n");
        stringBuilder.append(this.m00);
        stringBuilder.append(" ");
        stringBuilder.append(this.m01);
        stringBuilder.append(" ");
        stringBuilder.append(this.m02);
        stringBuilder.append("\n");
        stringBuilder.append(this.m10);
        stringBuilder.append(" ");
        stringBuilder.append(this.m11);
        stringBuilder.append(" ");
        stringBuilder.append(this.m12);
        stringBuilder.append("\n");
        stringBuilder.append(this.m20);
        stringBuilder.append(" ");
        stringBuilder.append(this.m21);
        stringBuilder.append(" ");
        stringBuilder.append(this.m22);
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    public void store(FloatBuffer buf) {
        buf.put(bufferIndex(0, 0), this.m00);
        buf.put(bufferIndex(0, 1), this.m01);
        buf.put(bufferIndex(0, 2), this.m02);
        buf.put(bufferIndex(1, 0), this.m10);
        buf.put(bufferIndex(1, 1), this.m11);
        buf.put(bufferIndex(1, 2), this.m12);
        buf.put(bufferIndex(2, 0), this.m20);
        buf.put(bufferIndex(2, 1), this.m21);
        buf.put(bufferIndex(2, 2), this.m22);
    }

    public void storeTransposed(FloatBuffer buf) {
        buf.put(bufferIndex(0, 0), this.m00);
        buf.put(bufferIndex(1, 0), this.m01);
        buf.put(bufferIndex(2, 0), this.m02);
        buf.put(bufferIndex(0, 1), this.m10);
        buf.put(bufferIndex(1, 1), this.m11);
        buf.put(bufferIndex(2, 1), this.m12);
        buf.put(bufferIndex(0, 2), this.m20);
        buf.put(bufferIndex(1, 2), this.m21);
        buf.put(bufferIndex(2, 2), this.m22);
    }

    public void store(FloatBuffer buf, boolean rowMajor) {
        if (rowMajor) {
            this.storeTransposed(buf);
        } else {
            this.store(buf);
        }

    }

    public void setIdentity() {
        this.m00 = 1.0F;
        this.m01 = 0.0F;
        this.m02 = 0.0F;
        this.m10 = 0.0F;
        this.m11 = 1.0F;
        this.m12 = 0.0F;
        this.m20 = 0.0F;
        this.m21 = 0.0F;
        this.m22 = 1.0F;
    }

    public float adjugateAndDet() {
        float f = this.m11 * this.m22 - this.m12 * this.m21;
        float g = -(this.m10 * this.m22 - this.m12 * this.m20);
        float h = this.m10 * this.m21 - this.m11 * this.m20;
        float i = -(this.m01 * this.m22 - this.m02 * this.m21);
        float j = this.m00 * this.m22 - this.m02 * this.m20;
        float k = -(this.m00 * this.m21 - this.m01 * this.m20);
        float l = this.m01 * this.m12 - this.m02 * this.m11;
        float m = -(this.m00 * this.m12 - this.m02 * this.m10);
        float n = this.m00 * this.m11 - this.m01 * this.m10;
        float o = this.m00 * f + this.m01 * g + this.m02 * h;
        this.m00 = f;
        this.m10 = g;
        this.m20 = h;
        this.m01 = i;
        this.m11 = j;
        this.m21 = k;
        this.m02 = l;
        this.m12 = m;
        this.m22 = n;
        return o;
    }

    public float determinant() {
        float f = this.m11 * this.m22 - this.m12 * this.m21;
        float g = -(this.m10 * this.m22 - this.m12 * this.m20);
        float h = this.m10 * this.m21 - this.m11 * this.m20;
        return this.m00 * f + this.m01 * g + this.m02 * h;
    }

    public boolean invert() {
        float f = this.adjugateAndDet();
        if (Math.abs(f) > 1.0E-6F) {
            this.mul(f);
            return true;
        } else {
            return false;
        }
    }

    public void set(int x, int y, float value) {
        if (x == 0) {
            if (y == 0) {
                this.m00 = value;
            } else if (y == 1) {
                this.m01 = value;
            } else {
                this.m02 = value;
            }
        } else if (x == 1) {
            if (y == 0) {
                this.m10 = value;
            } else if (y == 1) {
                this.m11 = value;
            } else {
                this.m12 = value;
            }
        } else if (y == 0) {
            this.m20 = value;
        } else if (y == 1) {
            this.m21 = value;
        } else {
            this.m22 = value;
        }

    }

    public void mul(Matrix3f other) {
        float f = this.m00 * other.m00 + this.m01 * other.m10 + this.m02 * other.m20;
        float g = this.m00 * other.m01 + this.m01 * other.m11 + this.m02 * other.m21;
        float h = this.m00 * other.m02 + this.m01 * other.m12 + this.m02 * other.m22;
        float i = this.m10 * other.m00 + this.m11 * other.m10 + this.m12 * other.m20;
        float j = this.m10 * other.m01 + this.m11 * other.m11 + this.m12 * other.m21;
        float k = this.m10 * other.m02 + this.m11 * other.m12 + this.m12 * other.m22;
        float l = this.m20 * other.m00 + this.m21 * other.m10 + this.m22 * other.m20;
        float m = this.m20 * other.m01 + this.m21 * other.m11 + this.m22 * other.m21;
        float n = this.m20 * other.m02 + this.m21 * other.m12 + this.m22 * other.m22;
        this.m00 = f;
        this.m01 = g;
        this.m02 = h;
        this.m10 = i;
        this.m11 = j;
        this.m12 = k;
        this.m20 = l;
        this.m21 = m;
        this.m22 = n;
    }

    public void mul(Quaternion quaternion) {
        this.mul(new Matrix3f(quaternion));
    }

    public void mul(float scalar) {
        this.m00 *= scalar;
        this.m01 *= scalar;
        this.m02 *= scalar;
        this.m10 *= scalar;
        this.m11 *= scalar;
        this.m12 *= scalar;
        this.m20 *= scalar;
        this.m21 *= scalar;
        this.m22 *= scalar;
    }

    public void add(Matrix3f matrix) {
        this.m00 += matrix.m00;
        this.m01 += matrix.m01;
        this.m02 += matrix.m02;
        this.m10 += matrix.m10;
        this.m11 += matrix.m11;
        this.m12 += matrix.m12;
        this.m20 += matrix.m20;
        this.m21 += matrix.m21;
        this.m22 += matrix.m22;
    }

    public void sub(Matrix3f matrix) {
        this.m00 -= matrix.m00;
        this.m01 -= matrix.m01;
        this.m02 -= matrix.m02;
        this.m10 -= matrix.m10;
        this.m11 -= matrix.m11;
        this.m12 -= matrix.m12;
        this.m20 -= matrix.m20;
        this.m21 -= matrix.m21;
        this.m22 -= matrix.m22;
    }

    public float trace() {
        return this.m00 + this.m11 + this.m22;
    }

    public Matrix3f copy() {
        return new Matrix3f(this);
    }
}
