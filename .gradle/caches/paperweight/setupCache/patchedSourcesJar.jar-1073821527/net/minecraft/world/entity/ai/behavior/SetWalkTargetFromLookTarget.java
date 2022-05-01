package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetFromLookTarget extends Behavior<LivingEntity> {
    private final Function<LivingEntity, Float> speedModifier;
    private final int closeEnoughDistance;
    private final Predicate<LivingEntity> canSetWalkTargetPredicate;

    public SetWalkTargetFromLookTarget(float speed, int completionRange) {
        this((entity) -> {
            return true;
        }, (entity) -> {
            return speed;
        }, completionRange);
    }

    public SetWalkTargetFromLookTarget(Predicate<LivingEntity> predicate, Function<LivingEntity, Float> speed, int completionRange) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_PRESENT));
        this.speedModifier = speed;
        this.closeEnoughDistance = completionRange;
        this.canSetWalkTargetPredicate = predicate;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        return this.canSetWalkTargetPredicate.test(entity);
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        Brain<?> brain = entity.getBrain();
        PositionTracker positionTracker = brain.getMemory(MemoryModuleType.LOOK_TARGET).get();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(positionTracker, this.speedModifier.apply(entity), this.closeEnoughDistance));
    }
}
