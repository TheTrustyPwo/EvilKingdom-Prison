package net.minecraft.world.level.portal;

import net.minecraft.world.phys.Vec3;

public class PortalInfo {
    public final Vec3 pos;
    public final Vec3 speed;
    public final float yRot;
    public final float xRot;

    public PortalInfo(Vec3 position, Vec3 velocity, float yaw, float pitch) {
        this.pos = position;
        this.speed = velocity;
        this.yRot = yaw;
        this.xRot = pitch;
    }
}
