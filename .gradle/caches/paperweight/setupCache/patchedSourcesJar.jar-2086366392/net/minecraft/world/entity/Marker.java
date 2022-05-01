package net.minecraft.world.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;

public class Marker extends Entity {
    private static final String DATA_TAG = "data";
    private CompoundTag data = new CompoundTag();

    public Marker(EntityType<?> type, Level world) {
        super(type, world);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.data = nbt.getCompound("data");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.put("data", this.data.copy());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        throw new IllegalStateException("Markers should never be sent");
    }

    @Override
    protected void addPassenger(Entity passenger) {
        passenger.stopRiding();
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }
}
