package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class RamTarget<E extends PathfinderMob> extends Behavior<E> {
    public static final int TIME_OUT_DURATION = 200;
    public static final float RAM_SPEED_FORCE_FACTOR = 1.65F;
    private final Function<E, UniformInt> getTimeBetweenRams;
    private final TargetingConditions ramTargeting;
    private final float speed;
    private final ToDoubleFunction<E> getKnockbackForce;
    private Vec3 ramDirection;
    private final Function<E, SoundEvent> getImpactSound;

    public RamTarget(Function<E, UniformInt> cooldownRangeFactory, TargetingConditions targetPredicate, float speed, ToDoubleFunction<E> strengthMultiplierFactory, Function<E, SoundEvent> soundFactory) {
        super(ImmutableMap.of(MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_PRESENT), 200);
        this.getTimeBetweenRams = cooldownRangeFactory;
        this.ramTargeting = targetPredicate;
        this.speed = speed;
        this.getKnockbackForce = strengthMultiplierFactory;
        this.getImpactSound = soundFactory;
        this.ramDirection = Vec3.ZERO;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, PathfinderMob entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, PathfinderMob pathfinderMob, long l) {
        return pathfinderMob.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
    }

    @Override
    protected void start(ServerLevel serverLevel, PathfinderMob pathfinderMob, long l) {
        BlockPos blockPos = pathfinderMob.blockPosition();
        Brain<?> brain = pathfinderMob.getBrain();
        Vec3 vec3 = brain.getMemory(MemoryModuleType.RAM_TARGET).get();
        this.ramDirection = (new Vec3((double)blockPos.getX() - vec3.x(), 0.0D, (double)blockPos.getZ() - vec3.z())).normalize();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(vec3, this.speed, 0));
    }

    @Override
    protected void tick(ServerLevel serverLevel, E pathfinderMob, long l) {
        List<LivingEntity> list = serverLevel.getNearbyEntities(LivingEntity.class, this.ramTargeting, pathfinderMob, pathfinderMob.getBoundingBox());
        Brain<?> brain = pathfinderMob.getBrain();
        if (!list.isEmpty()) {
            LivingEntity livingEntity = list.get(0);
            livingEntity.hurt(DamageSource.mobAttack(pathfinderMob).setNoAggro(), (float)pathfinderMob.getAttributeValue(Attributes.ATTACK_DAMAGE));
            int i = pathfinderMob.hasEffect(MobEffects.MOVEMENT_SPEED) ? pathfinderMob.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1 : 0;
            int j = pathfinderMob.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) ? pathfinderMob.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() + 1 : 0;
            float f = 0.25F * (float)(i - j);
            float g = Mth.clamp(pathfinderMob.getSpeed() * 1.65F, 0.2F, 3.0F) + f;
            float h = livingEntity.isDamageSourceBlocked(DamageSource.mobAttack(pathfinderMob)) ? 0.5F : 1.0F;
            livingEntity.knockback((double)(h * g) * this.getKnockbackForce.applyAsDouble(pathfinderMob), this.ramDirection.x(), this.ramDirection.z(), pathfinderMob); // Paper
            this.finishRam(serverLevel, pathfinderMob);
            serverLevel.playSound((Player)null, pathfinderMob, this.getImpactSound.apply(pathfinderMob), SoundSource.HOSTILE, 1.0F, 1.0F);
        } else {
            Optional<WalkTarget> optional = brain.getMemory(MemoryModuleType.WALK_TARGET);
            Optional<Vec3> optional2 = brain.getMemory(MemoryModuleType.RAM_TARGET);
            boolean bl = !optional.isPresent() || !optional2.isPresent() || optional.get().getTarget().currentPosition().distanceTo(optional2.get()) < 0.25D;
            if (bl) {
                this.finishRam(serverLevel, pathfinderMob);
            }
        }

    }

    protected void finishRam(ServerLevel world, E entity) {
        world.broadcastEntityEvent(entity, (byte)59);
        entity.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getTimeBetweenRams.apply(entity).sample(world.random));
        entity.getBrain().eraseMemory(MemoryModuleType.RAM_TARGET);
    }
}
