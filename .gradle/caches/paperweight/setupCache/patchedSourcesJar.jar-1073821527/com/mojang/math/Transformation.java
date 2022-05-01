package com.mojang.math;

import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import org.apache.commons.lang3.tuple.Triple;

public final class Transformation {
    private final Matrix4f matrix;
    private boolean decomposed;
    @Nullable
    private Vector3f translation;
    @Nullable
    private Quaternion leftRotation;
    @Nullable
    private Vector3f scale;
    @Nullable
    private Quaternion rightRotation;
    private static final Transformation IDENTITY = Util.make(() -> {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.setIdentity();
        Transformation transformation = new Transformation(matrix4f);
        transformation.getLeftRotation();
        return transformation;
    });

    public Transformation(@Nullable Matrix4f matrix) {
        if (matrix == null) {
            this.matrix = IDENTITY.matrix;
        } else {
            this.matrix = matrix;
        }

    }

    public Transformation(@Nullable Vector3f translation, @Nullable Quaternion rotation2, @Nullable Vector3f scale, @Nullable Quaternion rotation1) {
        this.matrix = compose(translation, rotation2, scale, rotation1);
        this.translation = translation != null ? translation : new Vector3f();
        this.leftRotation = rotation2 != null ? rotation2 : Quaternion.ONE.copy();
        this.scale = scale != null ? scale : new Vector3f(1.0F, 1.0F, 1.0F);
        this.rightRotation = rotation1 != null ? rotation1 : Quaternion.ONE.copy();
        this.decomposed = true;
    }

    public static Transformation identity() {
        return IDENTITY;
    }

    public Transformation compose(Transformation other) {
        Matrix4f matrix4f = this.getMatrix();
        matrix4f.multiply(other.getMatrix());
        return new Transformation(matrix4f);
    }

    @Nullable
    public Transformation inverse() {
        if (this == IDENTITY) {
            return this;
        } else {
            Matrix4f matrix4f = this.getMatrix();
            return matrix4f.invert() ? new Transformation(matrix4f) : null;
        }
    }

    private void ensureDecomposed() {
        if (!this.decomposed) {
            Pair<Matrix3f, Vector3f> pair = toAffine(this.matrix);
            Triple<Quaternion, Vector3f, Quaternion> triple = pair.getFirst().svdDecompose();
            this.translation = pair.getSecond();
            this.leftRotation = triple.getLeft();
            this.scale = triple.getMiddle();
            this.rightRotation = triple.getRight();
            this.decomposed = true;
        }

    }

    private static Matrix4f compose(@Nullable Vector3f translation, @Nullable Quaternion rotation2, @Nullable Vector3f scale, @Nullable Quaternion rotation1) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.setIdentity();
        if (rotation2 != null) {
            matrix4f.multiply(new Matrix4f(rotation2));
        }

        if (scale != null) {
            matrix4f.multiply(Matrix4f.createScaleMatrix(scale.x(), scale.y(), scale.z()));
        }

        if (rotation1 != null) {
            matrix4f.multiply(new Matrix4f(rotation1));
        }

        if (translation != null) {
            matrix4f.m03 = translation.x();
            matrix4f.m13 = translation.y();
            matrix4f.m23 = translation.z();
        }

        return matrix4f;
    }

    public static Pair<Matrix3f, Vector3f> toAffine(Matrix4f affineTransform) {
        affineTransform.multiply(1.0F / affineTransform.m33);
        Vector3f vector3f = new Vector3f(affineTransform.m03, affineTransform.m13, affineTransform.m23);
        Matrix3f matrix3f = new Matrix3f(affineTransform);
        return Pair.of(matrix3f, vector3f);
    }

    public Matrix4f getMatrix() {
        return this.matrix.copy();
    }

    public Vector3f getTranslation() {
        this.ensureDecomposed();
        return this.translation.copy();
    }

    public Quaternion getLeftRotation() {
        this.ensureDecomposed();
        return this.leftRotation.copy();
    }

    public Vector3f getScale() {
        this.ensureDecomposed();
        return this.scale.copy();
    }

    public Quaternion getRightRotation() {
        this.ensureDecomposed();
        return this.rightRotation.copy();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            Transformation transformation = (Transformation)object;
            return Objects.equals(this.matrix, transformation.matrix);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.matrix);
    }

    public Transformation slerp(Transformation transformation, float f) {
        Vector3f vector3f = this.getTranslation();
        Quaternion quaternion = this.getLeftRotation();
        Vector3f vector3f2 = this.getScale();
        Quaternion quaternion2 = this.getRightRotation();
        vector3f.lerp(transformation.getTranslation(), f);
        quaternion.slerp(transformation.getLeftRotation(), f);
        vector3f2.lerp(transformation.getScale(), f);
        quaternion2.slerp(transformation.getRightRotation(), f);
        return new Transformation(vector3f, quaternion, vector3f2, quaternion2);
    }
}
