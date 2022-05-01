package net.minecraft.world.entity;

import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;

public class ItemBasedSteering {

    private static final int MIN_BOOST_TIME = 140;
    private static final int MAX_BOOST_TIME = 700;
    private final SynchedEntityData entityData;
    private final EntityDataAccessor<Integer> boostTimeAccessor;
    private final EntityDataAccessor<Boolean> hasSaddleAccessor;
    public boolean boosting;
    public int boostTime;
    public int boostTimeTotal;

    public ItemBasedSteering(SynchedEntityData dataTracker, EntityDataAccessor<Integer> boostTime, EntityDataAccessor<Boolean> saddled) {
        this.entityData = dataTracker;
        this.boostTimeAccessor = boostTime;
        this.hasSaddleAccessor = saddled;
    }

    public void onSynced() {
        this.boosting = true;
        this.boostTime = 0;
        this.boostTimeTotal = (Integer) this.entityData.get(this.boostTimeAccessor);
    }

    public boolean boost(Random random) {
        if (this.boosting) {
            return false;
        } else {
            this.boosting = true;
            this.boostTime = 0;
            this.boostTimeTotal = random.nextInt(841) + 140;
            this.entityData.set(this.boostTimeAccessor, this.boostTimeTotal);
            return true;
        }
    }

    // CraftBukkit add setBoostTicks(int)
    public void setBoostTicks(int ticks) {
        this.boosting = true;
        this.boostTime = 0;
        this.boostTimeTotal = ticks;
        this.entityData.set(this.boostTimeAccessor, this.boostTimeTotal);
    }
    // CraftBukkit end

    public void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putBoolean("Saddle", this.hasSaddle());
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        this.setSaddle(nbt.getBoolean("Saddle"));
    }

    public void setSaddle(boolean saddled) {
        this.entityData.set(this.hasSaddleAccessor, saddled);
    }

    public boolean hasSaddle() {
        return (Boolean) this.entityData.get(this.hasSaddleAccessor);
    }
}
