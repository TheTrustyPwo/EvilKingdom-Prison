package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ConduitBlockEntity extends BlockEntity {
    private static final int BLOCK_REFRESH_RATE = 2;
    private static final int EFFECT_DURATION = 13;
    private static final float ROTATION_SPEED = -0.0375F;
    private static final int MIN_ACTIVE_SIZE = 16;
    private static final int MIN_KILL_SIZE = 42;
    private static final int KILL_RANGE = 8;
    private static final Block[] VALID_BLOCKS = new Block[]{Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE};
    public int tickCount;
    private float activeRotation;
    private boolean isActive;
    private boolean isHunting;
    private final List<BlockPos> effectBlocks = Lists.newArrayList();
    @Nullable
    private LivingEntity destroyTarget;
    @Nullable
    private UUID destroyTargetUUID;
    private long nextAmbientSoundActivation;

    public ConduitBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.CONDUIT, pos, state);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        if (nbt.hasUUID("Target")) {
            this.destroyTargetUUID = nbt.getUUID("Target");
        } else {
            this.destroyTargetUUID = null;
        }

    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (this.destroyTarget != null) {
            nbt.putUUID("Target", this.destroyTarget.getUUID());
        }

    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public static void clientTick(Level world, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity) {
        ++blockEntity.tickCount;
        long l = world.getGameTime();
        List<BlockPos> list = blockEntity.effectBlocks;
        if (l % 40L == 0L) {
            blockEntity.isActive = updateShape(world, pos, list);
            updateHunting(blockEntity, list);
        }

        updateClientTarget(world, pos, blockEntity);
        animationTick(world, pos, list, blockEntity.destroyTarget, blockEntity.tickCount);
        if (blockEntity.isActive()) {
            ++blockEntity.activeRotation;
        }

    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity) {
        ++blockEntity.tickCount;
        long l = world.getGameTime();
        List<BlockPos> list = blockEntity.effectBlocks;
        if (l % 40L == 0L) {
            boolean bl = updateShape(world, pos, list);
            if (bl != blockEntity.isActive) {
                SoundEvent soundEvent = bl ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE;
                world.playSound((Player)null, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            blockEntity.isActive = bl;
            updateHunting(blockEntity, list);
            if (bl) {
                applyEffects(world, pos, list);
                updateDestroyTarget(world, pos, state, list, blockEntity);
            }
        }

        if (blockEntity.isActive()) {
            if (l % 80L == 0L) {
                world.playSound((Player)null, pos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            if (l > blockEntity.nextAmbientSoundActivation) {
                blockEntity.nextAmbientSoundActivation = l + 60L + (long)world.getRandom().nextInt(40);
                world.playSound((Player)null, pos, SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

    }

    private static void updateHunting(ConduitBlockEntity blockEntity, List<BlockPos> activatingBlocks) {
        blockEntity.setHunting(activatingBlocks.size() >= 42);
    }

    private static boolean updateShape(Level world, BlockPos pos, List<BlockPos> activatingBlocks) {
        activatingBlocks.clear();

        for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
                for(int k = -1; k <= 1; ++k) {
                    BlockPos blockPos = pos.offset(i, j, k);
                    if (!world.isWaterAt(blockPos)) {
                        return false;
                    }
                }
            }
        }

        for(int l = -2; l <= 2; ++l) {
            for(int m = -2; m <= 2; ++m) {
                for(int n = -2; n <= 2; ++n) {
                    int o = Math.abs(l);
                    int p = Math.abs(m);
                    int q = Math.abs(n);
                    if ((o > 1 || p > 1 || q > 1) && (l == 0 && (p == 2 || q == 2) || m == 0 && (o == 2 || q == 2) || n == 0 && (o == 2 || p == 2))) {
                        BlockPos blockPos2 = pos.offset(l, m, n);
                        BlockState blockState = world.getBlockState(blockPos2);

                        for(Block block : VALID_BLOCKS) {
                            if (blockState.is(block)) {
                                activatingBlocks.add(blockPos2);
                            }
                        }
                    }
                }
            }
        }

        return activatingBlocks.size() >= 16;
    }

    private static void applyEffects(Level world, BlockPos pos, List<BlockPos> activatingBlocks) {
        int i = activatingBlocks.size();
        int j = i / 7 * 16;
        int k = pos.getX();
        int l = pos.getY();
        int m = pos.getZ();
        AABB aABB = (new AABB((double)k, (double)l, (double)m, (double)(k + 1), (double)(l + 1), (double)(m + 1))).inflate((double)j).expandTowards(0.0D, (double)world.getHeight(), 0.0D);
        List<Player> list = world.getEntitiesOfClass(Player.class, aABB);
        if (!list.isEmpty()) {
            for(Player player : list) {
                if (pos.closerThan(player.blockPosition(), (double)j) && player.isInWaterOrRain()) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, true));
                }
            }

        }
    }

    private static void updateDestroyTarget(Level world, BlockPos pos, BlockState state, List<BlockPos> activatingBlocks, ConduitBlockEntity blockEntity) {
        LivingEntity livingEntity = blockEntity.destroyTarget;
        int i = activatingBlocks.size();
        if (i < 42) {
            blockEntity.destroyTarget = null;
        } else if (blockEntity.destroyTarget == null && blockEntity.destroyTargetUUID != null) {
            blockEntity.destroyTarget = findDestroyTarget(world, pos, blockEntity.destroyTargetUUID);
            blockEntity.destroyTargetUUID = null;
        } else if (blockEntity.destroyTarget == null) {
            List<LivingEntity> list = world.getEntitiesOfClass(LivingEntity.class, getDestroyRangeAABB(pos), (entity) -> {
                return entity instanceof Enemy && entity.isInWaterOrRain();
            });
            if (!list.isEmpty()) {
                blockEntity.destroyTarget = list.get(world.random.nextInt(list.size()));
            }
        } else if (!blockEntity.destroyTarget.isAlive() || !pos.closerThan(blockEntity.destroyTarget.blockPosition(), 8.0D)) {
            blockEntity.destroyTarget = null;
        }

        if (blockEntity.destroyTarget != null) {
            world.playSound((Player)null, blockEntity.destroyTarget.getX(), blockEntity.destroyTarget.getY(), blockEntity.destroyTarget.getZ(), SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 1.0F, 1.0F);
            blockEntity.destroyTarget.hurt(DamageSource.MAGIC, 4.0F);
        }

        if (livingEntity != blockEntity.destroyTarget) {
            world.sendBlockUpdated(pos, state, state, 2);
        }

    }

    private static void updateClientTarget(Level world, BlockPos pos, ConduitBlockEntity blockEntity) {
        if (blockEntity.destroyTargetUUID == null) {
            blockEntity.destroyTarget = null;
        } else if (blockEntity.destroyTarget == null || !blockEntity.destroyTarget.getUUID().equals(blockEntity.destroyTargetUUID)) {
            blockEntity.destroyTarget = findDestroyTarget(world, pos, blockEntity.destroyTargetUUID);
            if (blockEntity.destroyTarget == null) {
                blockEntity.destroyTargetUUID = null;
            }
        }

    }

    private static AABB getDestroyRangeAABB(BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        return (new AABB((double)i, (double)j, (double)k, (double)(i + 1), (double)(j + 1), (double)(k + 1))).inflate(8.0D);
    }

    @Nullable
    private static LivingEntity findDestroyTarget(Level world, BlockPos pos, UUID uuid) {
        List<LivingEntity> list = world.getEntitiesOfClass(LivingEntity.class, getDestroyRangeAABB(pos), (entity) -> {
            return entity.getUUID().equals(uuid);
        });
        return list.size() == 1 ? list.get(0) : null;
    }

    private static void animationTick(Level world, BlockPos pos, List<BlockPos> activatingBlocks, @Nullable Entity entity, int ticks) {
        Random random = world.random;
        double d = (double)(Mth.sin((float)(ticks + 35) * 0.1F) / 2.0F + 0.5F);
        d = (d * d + d) * (double)0.3F;
        Vec3 vec3 = new Vec3((double)pos.getX() + 0.5D, (double)pos.getY() + 1.5D + d, (double)pos.getZ() + 0.5D);

        for(BlockPos blockPos : activatingBlocks) {
            if (random.nextInt(50) == 0) {
                BlockPos blockPos2 = blockPos.subtract(pos);
                float f = -0.5F + random.nextFloat() + (float)blockPos2.getX();
                float g = -2.0F + random.nextFloat() + (float)blockPos2.getY();
                float h = -0.5F + random.nextFloat() + (float)blockPos2.getZ();
                world.addParticle(ParticleTypes.NAUTILUS, vec3.x, vec3.y, vec3.z, (double)f, (double)g, (double)h);
            }
        }

        if (entity != null) {
            Vec3 vec32 = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
            float i = (-0.5F + random.nextFloat()) * (3.0F + entity.getBbWidth());
            float j = -1.0F + random.nextFloat() * entity.getBbHeight();
            float k = (-0.5F + random.nextFloat()) * (3.0F + entity.getBbWidth());
            Vec3 vec33 = new Vec3((double)i, (double)j, (double)k);
            world.addParticle(ParticleTypes.NAUTILUS, vec32.x, vec32.y, vec32.z, vec33.x, vec33.y, vec33.z);
        }

    }

    public boolean isActive() {
        return this.isActive;
    }

    public boolean isHunting() {
        return this.isHunting;
    }

    private void setHunting(boolean eyeOpen) {
        this.isHunting = eyeOpen;
    }

    public float getActiveRotation(float tickDelta) {
        return (this.activeRotation + tickDelta) * -0.0375F;
    }
}
