package net.minecraft.core.particles;

public class ParticleGroup {
    private final int limit;
    public static final ParticleGroup SPORE_BLOSSOM = new ParticleGroup(1000);

    public ParticleGroup(int maxCount) {
        this.limit = maxCount;
    }

    public int getLimit() {
        return this.limit;
    }
}
