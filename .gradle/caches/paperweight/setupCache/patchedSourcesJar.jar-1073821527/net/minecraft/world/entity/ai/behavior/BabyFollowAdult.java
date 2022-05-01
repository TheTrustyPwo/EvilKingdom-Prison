package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BabyFollowAdult<E extends AgeableMob> extends Behavior<E> {
    private final UniformInt followRange;
    private final Function<LivingEntity, Float> speedModifier;

    public BabyFollowAdult(UniformInt executionRange, float speed) {
        this(executionRange, (entity) -> {
            return speed;
        });
    }

    public BabyFollowAdult(UniformInt executionRange, Function<LivingEntity, Float> speed) {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.followRange = executionRange;
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        if (!entity.isBaby()) {
            return false;
        } else {
            AgeableMob ageableMob = this.getNearestAdult(entity);
            return entity.closerThan(ageableMob, (double)(this.followRange.getMaxValue() + 1)) && !entity.closerThan(ageableMob, (double)this.followRange.getMinValue());
        }
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        BehaviorUtils.setWalkAndLookTargetMemories(entity, this.getNearestAdult(entity), this.speedModifier.apply(entity), this.followRange.getMinValue() - 1);
    }

    private AgeableMob getNearestAdult(E entity) {
        return entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT).get();
    }
}
