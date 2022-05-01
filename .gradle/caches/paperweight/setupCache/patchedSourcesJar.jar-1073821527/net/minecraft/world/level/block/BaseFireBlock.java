package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseFireBlock extends Block {
    private static final int SECONDS_ON_FIRE = 8;
    private final float fireDamage;
    protected static final float AABB_OFFSET = 1.0F;
    protected static final VoxelShape DOWN_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public BaseFireBlock(BlockBehaviour.Properties settings, float damage) {
        super(settings);
        this.fireDamage = damage;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return getState(ctx.getLevel(), ctx.getClickedPos());
    }

    public static BlockState getState(BlockGetter world, BlockPos pos) {
        BlockPos blockPos = pos.below();
        BlockState blockState = world.getBlockState(blockPos);
        return SoulFireBlock.canSurviveOnBlock(blockState) ? Blocks.SOUL_FIRE.defaultBlockState() : ((FireBlock)Blocks.FIRE).getStateForPlacement(world, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return DOWN_AABB;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
        if (random.nextInt(24) == 0) {
            world.playLocalSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
        }

        BlockPos blockPos = pos.below();
        BlockState blockState = world.getBlockState(blockPos);
        if (!this.canBurn(blockState) && !blockState.isFaceSturdy(world, blockPos, Direction.UP)) {
            if (this.canBurn(world.getBlockState(pos.west()))) {
                for(int j = 0; j < 2; ++j) {
                    double g = (double)pos.getX() + random.nextDouble() * (double)0.1F;
                    double h = (double)pos.getY() + random.nextDouble();
                    double k = (double)pos.getZ() + random.nextDouble();
                    world.addParticle(ParticleTypes.LARGE_SMOKE, g, h, k, 0.0D, 0.0D, 0.0D);
                }
            }

            if (this.canBurn(world.getBlockState(pos.east()))) {
                for(int l = 0; l < 2; ++l) {
                    double m = (double)(pos.getX() + 1) - random.nextDouble() * (double)0.1F;
                    double n = (double)pos.getY() + random.nextDouble();
                    double o = (double)pos.getZ() + random.nextDouble();
                    world.addParticle(ParticleTypes.LARGE_SMOKE, m, n, o, 0.0D, 0.0D, 0.0D);
                }
            }

            if (this.canBurn(world.getBlockState(pos.north()))) {
                for(int p = 0; p < 2; ++p) {
                    double q = (double)pos.getX() + random.nextDouble();
                    double r = (double)pos.getY() + random.nextDouble();
                    double s = (double)pos.getZ() + random.nextDouble() * (double)0.1F;
                    world.addParticle(ParticleTypes.LARGE_SMOKE, q, r, s, 0.0D, 0.0D, 0.0D);
                }
            }

            if (this.canBurn(world.getBlockState(pos.south()))) {
                for(int t = 0; t < 2; ++t) {
                    double u = (double)pos.getX() + random.nextDouble();
                    double v = (double)pos.getY() + random.nextDouble();
                    double w = (double)(pos.getZ() + 1) - random.nextDouble() * (double)0.1F;
                    world.addParticle(ParticleTypes.LARGE_SMOKE, u, v, w, 0.0D, 0.0D, 0.0D);
                }
            }

            if (this.canBurn(world.getBlockState(pos.above()))) {
                for(int x = 0; x < 2; ++x) {
                    double y = (double)pos.getX() + random.nextDouble();
                    double z = (double)(pos.getY() + 1) - random.nextDouble() * (double)0.1F;
                    double aa = (double)pos.getZ() + random.nextDouble();
                    world.addParticle(ParticleTypes.LARGE_SMOKE, y, z, aa, 0.0D, 0.0D, 0.0D);
                }
            }
        } else {
            for(int i = 0; i < 3; ++i) {
                double d = (double)pos.getX() + random.nextDouble();
                double e = (double)pos.getY() + random.nextDouble() * 0.5D + 0.5D;
                double f = (double)pos.getZ() + random.nextDouble();
                world.addParticle(ParticleTypes.LARGE_SMOKE, d, e, f, 0.0D, 0.0D, 0.0D);
            }
        }

    }

    protected abstract boolean canBurn(BlockState state);

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!entity.fireImmune()) {
            entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 1);
            if (entity.getRemainingFireTicks() == 0) {
                entity.setSecondsOnFire(8);
            }

            entity.hurt(DamageSource.IN_FIRE, this.fireDamage);
        }

        super.entityInside(state, world, pos, entity);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            if (inPortalDimension(world)) {
                Optional<PortalShape> optional = PortalShape.findEmptyPortalShape(world, pos, Direction.Axis.X);
                if (optional.isPresent()) {
                    optional.get().createPortalBlocks();
                    return;
                }
            }

            if (!state.canSurvive(world, pos)) {
                world.removeBlock(pos, false);
            }

        }
    }

    private static boolean inPortalDimension(Level world) {
        return world.dimension() == Level.OVERWORLD || world.dimension() == Level.NETHER;
    }

    @Override
    protected void spawnDestroyParticles(Level world, Player player, BlockPos pos, BlockState state) {
    }

    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide()) {
            world.levelEvent((Player)null, 1009, pos, 0);
        }

        super.playerWillDestroy(world, pos, state, player);
    }

    public static boolean canBePlacedAt(Level world, BlockPos pos, Direction direction) {
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.isAir()) {
            return false;
        } else {
            return getState(world, pos).canSurvive(world, pos) || isPortal(world, pos, direction);
        }
    }

    private static boolean isPortal(Level world, BlockPos pos, Direction direction) {
        if (!inPortalDimension(world)) {
            return false;
        } else {
            BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
            boolean bl = false;

            for(Direction direction2 : Direction.values()) {
                if (world.getBlockState(mutableBlockPos.set(pos).move(direction2)).is(Blocks.OBSIDIAN)) {
                    bl = true;
                    break;
                }
            }

            if (!bl) {
                return false;
            } else {
                Direction.Axis axis = direction.getAxis().isHorizontal() ? direction.getCounterClockWise().getAxis() : Direction.Plane.HORIZONTAL.getRandomAxis(world.random);
                return PortalShape.findEmptyPortalShape(world, pos, axis).isPresent();
            }
        }
    }
}
