package net.minecraft.world.entity.ai.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.phys.Vec3;

public class WalkTarget {
    private final PositionTracker target;
    private final float speedModifier;
    private final int closeEnoughDist;

    public WalkTarget(BlockPos pos, float speed, int completionRange) {
        this(new BlockPosTracker(pos), speed, completionRange);
    }

    public WalkTarget(Vec3 pos, float speed, int completionRange) {
        this(new BlockPosTracker(new BlockPos(pos)), speed, completionRange);
    }

    public WalkTarget(Entity entity, float speed, int completionRange) {
        this(new EntityTracker(entity, false), speed, completionRange);
    }

    public WalkTarget(PositionTracker lookTarget, float speed, int completionRange) {
        this.target = lookTarget;
        this.speedModifier = speed;
        this.closeEnoughDist = completionRange;
    }

    public PositionTracker getTarget() {
        return this.target;
    }

    public float getSpeedModifier() {
        return this.speedModifier;
    }

    public int getCloseEnoughDist() {
        return this.closeEnoughDist;
    }
}
