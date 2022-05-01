package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class Silverfish extends Monster {
    @Nullable
    private Silverfish.SilverfishWakeUpFriendsGoal friendsGoal;

    public Silverfish(EntityType<? extends Silverfish> type, Level world) {
        super(type, world);
    }

    @Override
    protected void registerGoals() {
        this.friendsGoal = new Silverfish.SilverfishWakeUpFriendsGoal(this);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level));
        this.goalSelector.addGoal(3, this.friendsGoal);
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new Silverfish.SilverfishMergeWithStoneGoal(this));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public double getMyRidingOffset() {
        return 0.1D;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.13F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SILVERFISH_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            if ((source instanceof EntityDamageSource || source == DamageSource.MAGIC) && this.friendsGoal != null) {
                this.friendsGoal.notifyHurt();
            }

            return super.hurt(source, amount);
        }
    }

    @Override
    public void tick() {
        this.yBodyRot = this.getYRot();
        super.tick();
    }

    @Override
    public void setYBodyRot(float bodyYaw) {
        this.setYRot(bodyYaw);
        super.setYBodyRot(bodyYaw);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader world) {
        return InfestedBlock.isCompatibleHostBlock(world.getBlockState(pos.below())) ? 10.0F : super.getWalkTargetValue(pos, world);
    }

    public static boolean checkSilverfishSpawnRules(EntityType<Silverfish> type, LevelAccessor world, MobSpawnType spawnReason, BlockPos pos, Random random) {
        if (checkAnyLightMonsterSpawnRules(type, world, spawnReason, pos, random)) {
            Player player = world.getNearestPlayer((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, 5.0D, true);
            return player == null;
        } else {
            return false;
        }
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    static class SilverfishMergeWithStoneGoal extends RandomStrollGoal {
        @Nullable
        private Direction selectedDirection;
        private boolean doMerge;

        public SilverfishMergeWithStoneGoal(Silverfish silverfish) {
            super(silverfish, 1.0D, 10);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.mob.getTarget() != null) {
                return false;
            } else if (!this.mob.getNavigation().isDone()) {
                return false;
            } else {
                Random random = this.mob.getRandom();
                if (this.mob.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && random.nextInt(reducedTickDelay(10)) == 0) {
                    this.selectedDirection = Direction.getRandom(random);
                    BlockPos blockPos = (new BlockPos(this.mob.getX(), this.mob.getY() + 0.5D, this.mob.getZ())).relative(this.selectedDirection);
                    BlockState blockState = this.mob.level.getBlockState(blockPos);
                    if (InfestedBlock.isCompatibleHostBlock(blockState)) {
                        this.doMerge = true;
                        return true;
                    }
                }

                this.doMerge = false;
                return super.canUse();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.doMerge ? false : super.canContinueToUse();
        }

        @Override
        public void start() {
            if (!this.doMerge) {
                super.start();
            } else {
                LevelAccessor levelAccessor = this.mob.level;
                BlockPos blockPos = (new BlockPos(this.mob.getX(), this.mob.getY() + 0.5D, this.mob.getZ())).relative(this.selectedDirection);
                BlockState blockState = levelAccessor.getBlockState(blockPos);
                if (InfestedBlock.isCompatibleHostBlock(blockState)) {
                    levelAccessor.setBlock(blockPos, InfestedBlock.infestedStateByHost(blockState), 3);
                    this.mob.spawnAnim();
                    this.mob.discard();
                }

            }
        }
    }

    static class SilverfishWakeUpFriendsGoal extends Goal {
        private final Silverfish silverfish;
        private int lookForFriends;

        public SilverfishWakeUpFriendsGoal(Silverfish silverfish) {
            this.silverfish = silverfish;
        }

        public void notifyHurt() {
            if (this.lookForFriends == 0) {
                this.lookForFriends = this.adjustedTickDelay(20);
            }

        }

        @Override
        public boolean canUse() {
            return this.lookForFriends > 0;
        }

        @Override
        public void tick() {
            --this.lookForFriends;
            if (this.lookForFriends <= 0) {
                Level level = this.silverfish.level;
                Random random = this.silverfish.getRandom();
                BlockPos blockPos = this.silverfish.blockPosition();

                for(int i = 0; i <= 5 && i >= -5; i = (i <= 0 ? 1 : 0) - i) {
                    for(int j = 0; j <= 10 && j >= -10; j = (j <= 0 ? 1 : 0) - j) {
                        for(int k = 0; k <= 10 && k >= -10; k = (k <= 0 ? 1 : 0) - k) {
                            BlockPos blockPos2 = blockPos.offset(j, i, k);
                            BlockState blockState = level.getBlockState(blockPos2);
                            Block block = blockState.getBlock();
                            if (block instanceof InfestedBlock) {
                                if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                                    level.destroyBlock(blockPos2, true, this.silverfish);
                                } else {
                                    level.setBlock(blockPos2, ((InfestedBlock)block).hostStateByInfested(level.getBlockState(blockPos2)), 3);
                                }

                                if (random.nextBoolean()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}
