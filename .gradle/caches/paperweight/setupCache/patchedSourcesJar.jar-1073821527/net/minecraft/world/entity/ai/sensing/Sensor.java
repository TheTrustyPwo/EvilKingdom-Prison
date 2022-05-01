package net.minecraft.world.entity.ai.sensing;

import java.util.Random;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public abstract class Sensor<E extends LivingEntity> {
    private static final Random RANDOM = new Random();
    private static final int DEFAULT_SCAN_RATE = 20;
    protected static final int TARGETING_RANGE = 16;
    private static final TargetingConditions TARGET_CONDITIONS = TargetingConditions.forNonCombat().range(16.0D);
    private static final TargetingConditions TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = TargetingConditions.forNonCombat().range(16.0D).ignoreInvisibilityTesting();
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS = TargetingConditions.forCombat().range(16.0D);
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = TargetingConditions.forCombat().range(16.0D).ignoreInvisibilityTesting();
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT = TargetingConditions.forCombat().range(16.0D).ignoreLineOfSight();
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT = TargetingConditions.forCombat().range(16.0D).ignoreLineOfSight().ignoreInvisibilityTesting();
    private final int scanRate;
    private long timeToTick;

    public Sensor(int senseInterval) {
        this.scanRate = senseInterval;
        this.timeToTick = (long)RANDOM.nextInt(senseInterval);
    }

    public Sensor() {
        this(20);
    }

    public final void tick(ServerLevel world, E entity) {
        if (--this.timeToTick <= 0L) {
            this.timeToTick = (long)this.scanRate;
            this.doTick(world, entity);
        }

    }

    protected abstract void doTick(ServerLevel world, E entity);

    public abstract Set<MemoryModuleType<?>> requires();

    public static boolean isEntityTargetable(LivingEntity entity, LivingEntity target) {
        return entity.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) ? TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(entity, target) : TARGET_CONDITIONS.test(entity, target);
    }

    public static boolean isEntityAttackable(LivingEntity entity, LivingEntity target) {
        return entity.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) ? ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(entity, target) : ATTACK_TARGET_CONDITIONS.test(entity, target);
    }

    public static boolean isEntityAttackableIgnoringLineOfSight(LivingEntity entity, LivingEntity target) {
        return entity.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, target) ? ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT.test(entity, target) : ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT.test(entity, target);
    }
}
