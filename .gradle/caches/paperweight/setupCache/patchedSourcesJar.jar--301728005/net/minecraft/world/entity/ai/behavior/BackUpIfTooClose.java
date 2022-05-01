package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BackUpIfTooClose<E extends Mob> extends Behavior<E> {
    private final int tooCloseDistance;
    private final float strafeSpeed;

    public BackUpIfTooClose(int distance, float forwardMovement) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.tooCloseDistance = distance;
        this.strafeSpeed = forwardMovement;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        return this.isTargetVisible(entity) && this.isTargetTooClose(entity);
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.getTarget(entity), true));
        entity.getMoveControl().strafe(-this.strafeSpeed, 0.0F);
        entity.setYRot(Mth.rotateIfNecessary(entity.getYRot(), entity.yHeadRot, 0.0F));
    }

    private boolean isTargetVisible(E entity) {
        return entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().contains(this.getTarget(entity));
    }

    private boolean isTargetTooClose(E entity) {
        return this.getTarget(entity).closerThan(entity, (double)this.tooCloseDistance);
    }

    private LivingEntity getTarget(E entity) {
        return entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }
}
