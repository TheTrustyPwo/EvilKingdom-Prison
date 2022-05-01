package net.minecraft.world.entity;

public interface PlayerRideableJumping extends PlayerRideable {
    void onPlayerJump(int strength);

    boolean canJump();

    void handleStartJump(int height);

    void handleStopJump();
}
