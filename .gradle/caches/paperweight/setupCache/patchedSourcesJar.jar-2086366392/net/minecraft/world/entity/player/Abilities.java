package net.minecraft.world.entity.player;

import net.minecraft.nbt.CompoundTag;

public class Abilities {
    public boolean invulnerable;
    public boolean flying;
    public boolean mayfly;
    public boolean instabuild;
    public boolean mayBuild = true;
    public float flyingSpeed = 0.05F;
    public float walkingSpeed = 0.1F;

    public void addSaveData(CompoundTag nbt) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putBoolean("invulnerable", this.invulnerable);
        compoundTag.putBoolean("flying", this.flying);
        compoundTag.putBoolean("mayfly", this.mayfly);
        compoundTag.putBoolean("instabuild", this.instabuild);
        compoundTag.putBoolean("mayBuild", this.mayBuild);
        compoundTag.putFloat("flySpeed", this.flyingSpeed);
        compoundTag.putFloat("walkSpeed", this.walkingSpeed);
        nbt.put("abilities", compoundTag);
    }

    public void loadSaveData(CompoundTag nbt) {
        if (nbt.contains("abilities", 10)) {
            CompoundTag compoundTag = nbt.getCompound("abilities");
            this.invulnerable = compoundTag.getBoolean("invulnerable");
            this.flying = compoundTag.getBoolean("flying");
            this.mayfly = compoundTag.getBoolean("mayfly");
            this.instabuild = compoundTag.getBoolean("instabuild");
            if (compoundTag.contains("flySpeed", 99)) {
                this.flyingSpeed = compoundTag.getFloat("flySpeed");
                this.walkingSpeed = compoundTag.getFloat("walkSpeed");
            }

            if (compoundTag.contains("mayBuild", 1)) {
                this.mayBuild = compoundTag.getBoolean("mayBuild");
            }
        }

    }

    public float getFlyingSpeed() {
        return this.flyingSpeed;
    }

    public void setFlyingSpeed(float flySpeed) {
        this.flyingSpeed = flySpeed;
    }

    public float getWalkingSpeed() {
        return this.walkingSpeed;
    }

    public void setWalkingSpeed(float walkSpeed) {
        this.walkingSpeed = walkSpeed;
    }
}
