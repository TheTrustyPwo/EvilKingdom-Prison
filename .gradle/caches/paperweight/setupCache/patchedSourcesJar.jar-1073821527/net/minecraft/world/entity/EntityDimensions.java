package net.minecraft.world.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityDimensions {
    public final float width;
    public final float height;
    public final boolean fixed;

    public EntityDimensions(float width, float height, boolean fixed) {
        this.width = width;
        this.height = height;
        this.fixed = fixed;
    }

    public AABB makeBoundingBox(Vec3 pos) {
        return this.makeBoundingBox(pos.x, pos.y, pos.z);
    }

    public AABB makeBoundingBox(double x, double y, double z) {
        float f = this.width / 2.0F;
        float g = this.height;
        return new AABB(x - (double)f, y, z - (double)f, x + (double)f, y + (double)g, z + (double)f);
    }

    public EntityDimensions scale(float ratio) {
        return this.scale(ratio, ratio);
    }

    public EntityDimensions scale(float widthRatio, float heightRatio) {
        return !this.fixed && (widthRatio != 1.0F || heightRatio != 1.0F) ? scalable(this.width * widthRatio, this.height * heightRatio) : this;
    }

    public static EntityDimensions scalable(float width, float height) {
        return new EntityDimensions(width, height, false);
    }

    public static EntityDimensions fixed(float width, float height) {
        return new EntityDimensions(width, height, true);
    }

    @Override
    public String toString() {
        return "EntityDimensions w=" + this.width + ", h=" + this.height + ", fixed=" + this.fixed;
    }
}
