package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class RespawnAnchorBlock extends Block {
    public static final int MIN_CHARGES = 0;
    public static final int MAX_CHARGES = 4;
    public static final IntegerProperty CHARGE = BlockStateProperties.RESPAWN_ANCHOR_CHARGES;
    private static final ImmutableList<Vec3i> RESPAWN_HORIZONTAL_OFFSETS = ImmutableList.of(new Vec3i(0, 0, -1), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(1, 0, 0), new Vec3i(-1, 0, -1), new Vec3i(1, 0, -1), new Vec3i(-1, 0, 1), new Vec3i(1, 0, 1));
    private static final ImmutableList<Vec3i> RESPAWN_OFFSETS = (new Builder<Vec3i>()).addAll(RESPAWN_HORIZONTAL_OFFSETS).addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::below).iterator()).addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::above).iterator()).add(new Vec3i(0, 1, 0)).build();

    public RespawnAnchorBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE, Integer.valueOf(0)));
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && !isRespawnFuel(itemStack) && isRespawnFuel(player.getItemInHand(InteractionHand.OFF_HAND))) {
            return InteractionResult.PASS;
        } else if (isRespawnFuel(itemStack) && canBeCharged(state)) {
            charge(world, pos, state);
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        } else if (state.getValue(CHARGE) == 0) {
            return InteractionResult.PASS;
        } else if (!canSetSpawn(world)) {
            if (!world.isClientSide) {
                this.explode(state, world, pos);
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        } else {
            if (!world.isClientSide) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                if (serverPlayer.getRespawnDimension() != world.dimension() || !pos.equals(serverPlayer.getRespawnPosition())) {
                    serverPlayer.setRespawnPosition(world.dimension(), pos, 0.0F, false, true);
                    world.playSound((Player)null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.CONSUME;
        }
    }

    private static boolean isRespawnFuel(ItemStack stack) {
        return stack.is(Items.GLOWSTONE);
    }

    private static boolean canBeCharged(BlockState state) {
        return state.getValue(CHARGE) < 4;
    }

    private static boolean isWaterThatWouldFlow(BlockPos pos, Level world) {
        FluidState fluidState = world.getFluidState(pos);
        if (!fluidState.is(FluidTags.WATER)) {
            return false;
        } else if (fluidState.isSource()) {
            return true;
        } else {
            float f = (float)fluidState.getAmount();
            if (f < 2.0F) {
                return false;
            } else {
                FluidState fluidState2 = world.getFluidState(pos.below());
                return !fluidState2.is(FluidTags.WATER);
            }
        }
    }

    private void explode(BlockState state, Level world, BlockPos explodedPos) {
        world.removeBlock(explodedPos, false);
        boolean bl = Direction.Plane.HORIZONTAL.stream().map(explodedPos::relative).anyMatch((pos) -> {
            return isWaterThatWouldFlow(pos, world);
        });
        final boolean bl2 = bl || world.getFluidState(explodedPos.above()).is(FluidTags.WATER);
        ExplosionDamageCalculator explosionDamageCalculator = new ExplosionDamageCalculator() {
            @Override
            public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter world, BlockPos pos, BlockState blockState, FluidState fluidState) {
                return pos.equals(explodedPos) && bl2 ? Optional.of(Blocks.WATER.getExplosionResistance()) : super.getBlockExplosionResistance(explosion, world, pos, blockState, fluidState);
            }
        };
        world.explode((Entity)null, DamageSource.badRespawnPointExplosion(), explosionDamageCalculator, (double)explodedPos.getX() + 0.5D, (double)explodedPos.getY() + 0.5D, (double)explodedPos.getZ() + 0.5D, 5.0F, true, Explosion.BlockInteraction.DESTROY);
    }

    public static boolean canSetSpawn(Level world) {
        return world.dimensionType().respawnAnchorWorks();
    }

    public static void charge(Level world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state.setValue(CHARGE, Integer.valueOf(state.getValue(CHARGE) + 1)), 3);
        world.playSound((Player)null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
        if (state.getValue(CHARGE) != 0) {
            if (random.nextInt(100) == 0) {
                world.playSound((Player)null, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            double d = (double)pos.getX() + 0.5D + (0.5D - random.nextDouble());
            double e = (double)pos.getY() + 1.0D;
            double f = (double)pos.getZ() + 0.5D + (0.5D - random.nextDouble());
            double g = (double)random.nextFloat() * 0.04D;
            world.addParticle(ParticleTypes.REVERSE_PORTAL, d, e, f, 0.0D, g, 0.0D);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    public static int getScaledChargeLevel(BlockState state, int maxLevel) {
        return Mth.floor((float)(state.getValue(CHARGE) - 0) / 4.0F * (float)maxLevel);
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return getScaledChargeLevel(state, 15);
    }

    public static Optional<Vec3> findStandUpPosition(EntityType<?> entity, CollisionGetter world, BlockPos pos) {
        Optional<Vec3> optional = findStandUpPosition(entity, world, pos, true);
        return optional.isPresent() ? optional : findStandUpPosition(entity, world, pos, false);
    }

    private static Optional<Vec3> findStandUpPosition(EntityType<?> entity, CollisionGetter world, BlockPos pos, boolean ignoreInvalidPos) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(Vec3i vec3i : RESPAWN_OFFSETS) {
            mutableBlockPos.set(pos).move(vec3i);
            Vec3 vec3 = DismountHelper.findSafeDismountLocation(entity, world, mutableBlockPos, ignoreInvalidPos);
            if (vec3 != null) {
                return Optional.of(vec3);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
        return false;
    }
}
