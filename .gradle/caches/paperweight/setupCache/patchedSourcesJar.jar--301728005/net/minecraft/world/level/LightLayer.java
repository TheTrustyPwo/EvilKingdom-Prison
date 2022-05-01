package net.minecraft.world.level;

public enum LightLayer {
    SKY(15),
    BLOCK(0);

    public final int surrounding;

    private LightLayer(int value) {
        this.surrounding = value;
    }
}
