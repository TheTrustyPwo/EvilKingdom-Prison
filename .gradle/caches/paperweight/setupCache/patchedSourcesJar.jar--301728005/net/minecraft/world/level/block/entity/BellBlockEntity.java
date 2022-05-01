package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableInt;

public class BellBlockEntity extends BlockEntity {
    private static final int DURATION = 50;
    private static final int GLOW_DURATION = 60;
    private static final int MIN_TICKS_BETWEEN_SEARCHES = 60;
    private static final int MAX_RESONATION_TICKS = 40;
    private static final int TICKS_BEFORE_RESONATION = 5;
    private static final int SEARCH_RADIUS = 48;
    private static final int HEAR_BELL_RADIUS = 32;
    private static final int HIGHLIGHT_RAIDERS_RADIUS = 48;
    private long lastRingTimestamp;
    public int ticks;
    public boolean shaking;
    public Direction clickDirection;
    private List<LivingEntity> nearbyEntities;
    private boolean resonating;
    private int resonationTicks;

    public BellBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.BELL, pos, state);
    }

    @Override
    public boolean triggerEvent(int type, int data) {
        if (type == 1) {
            this.updateEntities();
            this.resonationTicks = 0;
            this.clickDirection = Direction.from3DDataValue(data);
            this.ticks = 0;
            this.shaking = true;
            return true;
        } else {
            return super.triggerEvent(type, data);
        }
    }

    private static void tick(Level world, BlockPos pos, BlockState state, BellBlockEntity blockEntity, BellBlockEntity.ResonationEndAction bellEffect) {
        if (blockEntity.shaking) {
            ++blockEntity.ticks;
        }

        if (blockEntity.ticks >= 50) {
            blockEntity.shaking = false;
            // Paper start
            if (!blockEntity.resonating) {
                blockEntity.nearbyEntities.clear();
            }
            // Paper end
            blockEntity.ticks = 0;
        }

        if (blockEntity.ticks >= 5 && blockEntity.resonationTicks == 0 && areRaidersNearby(pos, blockEntity.nearbyEntities)) {
            blockEntity.resonating = true;
            world.playSound((Player)null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        if (blockEntity.resonating) {
            if (blockEntity.resonationTicks < 40) {
                ++blockEntity.resonationTicks;
            } else {
                bellEffect.run(world, pos, blockEntity.nearbyEntities);
                blockEntity.nearbyEntities.clear(); // Paper
                blockEntity.resonating = false;
            }
        }

    }

    public static void clientTick(Level world, BlockPos pos, BlockState state, BellBlockEntity blockEntity) {
        tick(world, pos, state, blockEntity, BellBlockEntity::showBellParticles);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, BellBlockEntity blockEntity) {
        tick(world, pos, state, blockEntity, BellBlockEntity::makeRaidersGlow);
    }

    public void onHit(Direction direction) {
        BlockPos blockPos = this.getBlockPos();
        this.clickDirection = direction;
        if (this.shaking) {
            this.ticks = 0;
        } else {
            this.shaking = true;
        }

        this.level.blockEvent(blockPos, this.getBlockState().getBlock(), 1, direction.get3DDataValue());
    }

    private void updateEntities() {
        BlockPos blockPos = this.getBlockPos();
        if (this.level.getGameTime() > this.lastRingTimestamp + 60L || this.nearbyEntities == null) {
            this.lastRingTimestamp = this.level.getGameTime();
            AABB aABB = (new AABB(blockPos)).inflate(48.0D);
            this.nearbyEntities = this.level.getEntitiesOfClass(LivingEntity.class, aABB);
        }

        if (!this.level.isClientSide) {
            for(LivingEntity livingEntity : this.nearbyEntities) {
                if (livingEntity.isAlive() && !livingEntity.isRemoved() && blockPos.closerToCenterThan(livingEntity.position(), 32.0D)) {
                    livingEntity.getBrain().setMemory(MemoryModuleType.HEARD_BELL_TIME, this.level.getGameTime());
                }
            }
        }

        this.nearbyEntities.removeIf(e -> !e.isAlive()); // Paper
    }

    private static boolean areRaidersNearby(BlockPos pos, List<LivingEntity> hearingEntities) {
        for(LivingEntity livingEntity : hearingEntities) {
            if (livingEntity.isAlive() && !livingEntity.isRemoved() && pos.closerToCenterThan(livingEntity.position(), 32.0D) && livingEntity.getType().is(EntityTypeTags.RAIDERS)) {
                return true;
            }
        }

        return false;
    }

    private static void makeRaidersGlow(Level world, BlockPos pos, List<LivingEntity> hearingEntities) {
        hearingEntities.stream().filter((entity) -> {
            return isRaiderWithinRange(pos, entity);
        }).forEach(entity -> glow(entity, pos)); // Paper - pass BlockPos
    }

    private static void showBellParticles(Level world, BlockPos pos, List<LivingEntity> hearingEntities) {
        MutableInt mutableInt = new MutableInt(16700985);
        int i = (int)hearingEntities.stream().filter((entity) -> {
            return pos.closerToCenterThan(entity.position(), 48.0D);
        }).count();
        hearingEntities.stream().filter((entity) -> {
            return isRaiderWithinRange(pos, entity);
        }).forEach((entity) -> {
            float f = 1.0F;
            double d = Math.sqrt((entity.getX() - (double)pos.getX()) * (entity.getX() - (double)pos.getX()) + (entity.getZ() - (double)pos.getZ()) * (entity.getZ() - (double)pos.getZ()));
            double e = (double)((float)pos.getX() + 0.5F) + 1.0D / d * (entity.getX() - (double)pos.getX());
            double g = (double)((float)pos.getZ() + 0.5F) + 1.0D / d * (entity.getZ() - (double)pos.getZ());
            int j = Mth.clamp((i - 21) / -2, 3, 15);

            for(int k = 0; k < j; ++k) {
                int l = mutableInt.addAndGet(5);
                double h = (double)FastColor.ARGB32.red(l) / 255.0D;
                double m = (double)FastColor.ARGB32.green(l) / 255.0D;
                double n = (double)FastColor.ARGB32.blue(l) / 255.0D;
                world.addParticle(ParticleTypes.ENTITY_EFFECT, e, (double)((float)pos.getY() + 0.5F), g, h, m, n);
            }

        });
    }

    private static boolean isRaiderWithinRange(BlockPos pos, LivingEntity entity) {
        return entity.isAlive() && !entity.isRemoved() && pos.closerToCenterThan(entity.position(), 48.0D) && entity.getType().is(EntityTypeTags.RAIDERS);
    }

    // Paper start
    private static void glow(LivingEntity entity) { glow(entity, null); }
    private static void glow(LivingEntity entity, @javax.annotation.Nullable BlockPos pos) {
        if (pos != null && !new io.papermc.paper.event.block.BellRevealRaiderEvent(entity.level.getWorld().getBlockAt(net.minecraft.server.MCUtil.toLocation(entity.level, pos)), entity.getBukkitEntity()).callEvent()) return;
        // Paper end
        entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60));
    }

    @FunctionalInterface
    interface ResonationEndAction {
        void run(Level world, BlockPos pos, List<LivingEntity> hearingEntities);
    }
}
