package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

public class LongJumpMidJump extends Behavior<Mob> {
    public static final int TIME_OUT_DURATION = 100;
    private final UniformInt timeBetweenLongJumps;
    private SoundEvent landingSound;

    public LongJumpMidJump(UniformInt cooldownRange, SoundEvent sound) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_PRESENT), 100);
        this.timeBetweenLongJumps = cooldownRange;
        this.landingSound = sound;
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Mob mob, long l) {
        return !mob.isOnGround();
    }

    @Override
    protected void start(ServerLevel serverLevel, Mob mob, long l) {
        mob.setDiscardFriction(true);
        mob.setPose(Pose.LONG_JUMPING);
    }

    @Override
    protected void stop(ServerLevel world, Mob entity, long time) {
        if (entity.isOnGround()) {
            entity.setDeltaMovement(entity.getDeltaMovement().scale((double)0.1F));
            world.playSound((Player)null, entity, this.landingSound, SoundSource.NEUTRAL, 2.0F, 1.0F);
        }

        entity.setDiscardFriction(false);
        entity.setPose(Pose.STANDING);
        entity.getBrain().eraseMemory(MemoryModuleType.LONG_JUMP_MID_JUMP);
        entity.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(world.random));
    }
}
