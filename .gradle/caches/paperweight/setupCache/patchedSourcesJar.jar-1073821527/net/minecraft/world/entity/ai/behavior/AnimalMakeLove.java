package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Animal;

public class AnimalMakeLove extends Behavior<Animal> {
    private static final int BREED_RANGE = 3;
    private static final int MIN_DURATION = 60;
    private static final int MAX_DURATION = 110;
    private final EntityType<? extends Animal> partnerType;
    private final float speedModifier;
    private long spawnChildAtTime;

    public AnimalMakeLove(EntityType<? extends Animal> targetType, float speed) {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT, MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), 110);
        this.partnerType = targetType;
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Animal entity) {
        return entity.isInLove() && this.findValidBreedPartner(entity).isPresent();
    }

    @Override
    protected void start(ServerLevel world, Animal entity, long time) {
        Animal animal = this.findValidBreedPartner(entity).get();
        entity.getBrain().setMemory(MemoryModuleType.BREED_TARGET, animal);
        animal.getBrain().setMemory(MemoryModuleType.BREED_TARGET, entity);
        BehaviorUtils.lockGazeAndWalkToEachOther(entity, animal, this.speedModifier);
        int i = 60 + entity.getRandom().nextInt(50);
        this.spawnChildAtTime = time + (long)i;
    }

    @Override
    protected boolean canStillUse(ServerLevel world, Animal entity, long time) {
        if (!this.hasBreedTargetOfRightType(entity)) {
            return false;
        } else {
            Animal animal = this.getBreedTarget(entity);
            return animal.isAlive() && entity.canMate(animal) && BehaviorUtils.entityIsVisible(entity.getBrain(), animal) && time <= this.spawnChildAtTime;
        }
    }

    @Override
    protected void tick(ServerLevel serverLevel, Animal animal, long l) {
        Animal animal2 = this.getBreedTarget(animal);
        BehaviorUtils.lockGazeAndWalkToEachOther(animal, animal2, this.speedModifier);
        if (animal.closerThan(animal2, 3.0D)) {
            if (l >= this.spawnChildAtTime) {
                animal.spawnChildFromBreeding(serverLevel, animal2);
                animal.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
                animal2.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
            }

        }
    }

    @Override
    protected void stop(ServerLevel serverLevel, Animal animal, long l) {
        animal.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
        animal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        animal.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        this.spawnChildAtTime = 0L;
    }

    private Animal getBreedTarget(Animal animal) {
        return (Animal)animal.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
    }

    private boolean hasBreedTargetOfRightType(Animal animal) {
        Brain<?> brain = animal.getBrain();
        return brain.hasMemoryValue(MemoryModuleType.BREED_TARGET) && brain.getMemory(MemoryModuleType.BREED_TARGET).get().getType() == this.partnerType;
    }

    private Optional<? extends Animal> findValidBreedPartner(Animal animal) {
        return animal.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().findClosest((entity) -> {
            if (entity.getType() == this.partnerType && entity instanceof Animal) {
                Animal animal2 = (Animal)entity;
                if (animal.canMate(animal2)) {
                    return true;
                }
            }

            return false;
        }).map(Animal.class::cast);
    }
}
