package net.minecraft.world.entity.ai.goal;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;

public class RemoveBlockGoal extends MoveToBlockGoal {
    private final Block blockToRemove;
    private final Mob removerMob;
    private int ticksSinceReachedGoal;
    private static final int WAIT_AFTER_BLOCK_FOUND = 20;

    public RemoveBlockGoal(Block targetBlock, PathfinderMob mob, double speed, int maxYDifference) {
        super(mob, speed, 24, maxYDifference);
        this.blockToRemove = targetBlock;
        this.removerMob = mob;
    }

    @Override
    public boolean canUse() {
        if (!this.removerMob.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        } else if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        } else if (this.tryFindBlock()) {
            this.nextStartTick = reducedTickDelay(20);
            return true;
        } else {
            this.nextStartTick = this.nextStartTick(this.mob);
            return false;
        }
    }

    private boolean tryFindBlock() {
        return this.blockPos != null && this.isValidTarget(this.mob.level, this.blockPos) ? true : this.findNearestBlock();
    }

    @Override
    public void stop() {
        super.stop();
        this.removerMob.fallDistance = 1.0F;
    }

    @Override
    public void start() {
        super.start();
        this.ticksSinceReachedGoal = 0;
    }

    public void playDestroyProgressSound(LevelAccessor world, BlockPos pos) {
    }

    public void playBreakSound(Level world, BlockPos pos) {
    }

    @Override
    public void tick() {
        super.tick();
        Level level = this.removerMob.level;
        BlockPos blockPos = this.removerMob.blockPosition();
        BlockPos blockPos2 = this.getPosWithBlock(blockPos, level);
        Random random = this.removerMob.getRandom();
        if (this.isReachedTarget() && blockPos2 != null) {
            if (this.ticksSinceReachedGoal > 0) {
                Vec3 vec3 = this.removerMob.getDeltaMovement();
                this.removerMob.setDeltaMovement(vec3.x, 0.3D, vec3.z);
                if (!level.isClientSide) {
                    double d = 0.08D;
                    ((ServerLevel)level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EGG)), (double)blockPos2.getX() + 0.5D, (double)blockPos2.getY() + 0.7D, (double)blockPos2.getZ() + 0.5D, 3, ((double)random.nextFloat() - 0.5D) * 0.08D, ((double)random.nextFloat() - 0.5D) * 0.08D, ((double)random.nextFloat() - 0.5D) * 0.08D, (double)0.15F);
                }
            }

            if (this.ticksSinceReachedGoal % 2 == 0) {
                Vec3 vec32 = this.removerMob.getDeltaMovement();
                this.removerMob.setDeltaMovement(vec32.x, -0.3D, vec32.z);
                if (this.ticksSinceReachedGoal % 6 == 0) {
                    this.playDestroyProgressSound(level, this.blockPos);
                }
            }

            if (this.ticksSinceReachedGoal > 60) {
                level.removeBlock(blockPos2, false);
                if (!level.isClientSide) {
                    for(int i = 0; i < 20; ++i) {
                        double e = random.nextGaussian() * 0.02D;
                        double f = random.nextGaussian() * 0.02D;
                        double g = random.nextGaussian() * 0.02D;
                        ((ServerLevel)level).sendParticles(ParticleTypes.POOF, (double)blockPos2.getX() + 0.5D, (double)blockPos2.getY(), (double)blockPos2.getZ() + 0.5D, 1, e, f, g, (double)0.15F);
                    }

                    this.playBreakSound(level, blockPos2);
                }
            }

            ++this.ticksSinceReachedGoal;
        }

    }

    @Nullable
    private BlockPos getPosWithBlock(BlockPos pos, BlockGetter world) {
        if (world.getBlockState(pos).is(this.blockToRemove)) {
            return pos;
        } else {
            BlockPos[] blockPoss = new BlockPos[]{pos.below(), pos.west(), pos.east(), pos.north(), pos.south(), pos.below().below()};

            for(BlockPos blockPos : blockPoss) {
                if (world.getBlockState(blockPos).is(this.blockToRemove)) {
                    return blockPos;
                }
            }

            return null;
        }
    }

    @Override
    protected boolean isValidTarget(LevelReader world, BlockPos pos) {
        ChunkAccess chunkAccess = world.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
        if (chunkAccess == null) {
            return false;
        } else {
            return chunkAccess.getBlockState(pos).is(this.blockToRemove) && chunkAccess.getBlockState(pos.above()).isAir() && chunkAccess.getBlockState(pos.above(2)).isAir();
        }
    }
}
