package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class ButtonBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final int PRESSED_DEPTH = 1;
    private static final int UNPRESSED_DEPTH = 2;
    protected static final int HALF_AABB_HEIGHT = 2;
    protected static final int HALF_AABB_WIDTH = 3;
    protected static final VoxelShape CEILING_AABB_X = Block.box(6.0D, 14.0D, 5.0D, 10.0D, 16.0D, 11.0D);
    protected static final VoxelShape CEILING_AABB_Z = Block.box(5.0D, 14.0D, 6.0D, 11.0D, 16.0D, 10.0D);
    protected static final VoxelShape FLOOR_AABB_X = Block.box(6.0D, 0.0D, 5.0D, 10.0D, 2.0D, 11.0D);
    protected static final VoxelShape FLOOR_AABB_Z = Block.box(5.0D, 0.0D, 6.0D, 11.0D, 2.0D, 10.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(5.0D, 6.0D, 14.0D, 11.0D, 10.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(5.0D, 6.0D, 0.0D, 11.0D, 10.0D, 2.0D);
    protected static final VoxelShape WEST_AABB = Block.box(14.0D, 6.0D, 5.0D, 16.0D, 10.0D, 11.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 6.0D, 5.0D, 2.0D, 10.0D, 11.0D);
    protected static final VoxelShape PRESSED_CEILING_AABB_X = Block.box(6.0D, 15.0D, 5.0D, 10.0D, 16.0D, 11.0D);
    protected static final VoxelShape PRESSED_CEILING_AABB_Z = Block.box(5.0D, 15.0D, 6.0D, 11.0D, 16.0D, 10.0D);
    protected static final VoxelShape PRESSED_FLOOR_AABB_X = Block.box(6.0D, 0.0D, 5.0D, 10.0D, 1.0D, 11.0D);
    protected static final VoxelShape PRESSED_FLOOR_AABB_Z = Block.box(5.0D, 0.0D, 6.0D, 11.0D, 1.0D, 10.0D);
    protected static final VoxelShape PRESSED_NORTH_AABB = Block.box(5.0D, 6.0D, 15.0D, 11.0D, 10.0D, 16.0D);
    protected static final VoxelShape PRESSED_SOUTH_AABB = Block.box(5.0D, 6.0D, 0.0D, 11.0D, 10.0D, 1.0D);
    protected static final VoxelShape PRESSED_WEST_AABB = Block.box(15.0D, 6.0D, 5.0D, 16.0D, 10.0D, 11.0D);
    protected static final VoxelShape PRESSED_EAST_AABB = Block.box(0.0D, 6.0D, 5.0D, 1.0D, 10.0D, 11.0D);
    private final boolean sensitive;

    protected ButtonBlock(boolean wooden, BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(FACE, AttachFace.WALL));
        this.sensitive = wooden;
    }

    private int getPressDuration() {
        return this.sensitive ? 30 : 20;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        boolean bl = state.getValue(POWERED);
        switch((AttachFace)state.getValue(FACE)) {
        case FLOOR:
            if (direction.getAxis() == Direction.Axis.X) {
                return bl ? PRESSED_FLOOR_AABB_X : FLOOR_AABB_X;
            }

            return bl ? PRESSED_FLOOR_AABB_Z : FLOOR_AABB_Z;
        case WALL:
            switch(direction) {
            case EAST:
                return bl ? PRESSED_EAST_AABB : EAST_AABB;
            case WEST:
                return bl ? PRESSED_WEST_AABB : WEST_AABB;
            case SOUTH:
                return bl ? PRESSED_SOUTH_AABB : SOUTH_AABB;
            case NORTH:
            default:
                return bl ? PRESSED_NORTH_AABB : NORTH_AABB;
            }
        case CEILING:
        default:
            if (direction.getAxis() == Direction.Axis.X) {
                return bl ? PRESSED_CEILING_AABB_X : CEILING_AABB_X;
            } else {
                return bl ? PRESSED_CEILING_AABB_Z : CEILING_AABB_Z;
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(POWERED)) {
            return InteractionResult.CONSUME;
        } else {
            this.press(state, world, pos);
            this.playSound(player, world, pos, true);
            world.gameEvent(player, GameEvent.BLOCK_PRESS, pos);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    public void press(BlockState state, Level world, BlockPos pos) {
        world.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(true)), 3);
        this.updateNeighbours(state, world, pos);
        world.scheduleTick(pos, this, this.getPressDuration());
    }

    protected void playSound(@Nullable Player player, LevelAccessor world, BlockPos pos, boolean powered) {
        world.playSound(powered ? player : null, pos, this.getSound(powered), SoundSource.BLOCKS, 0.3F, powered ? 0.6F : 0.5F);
    }

    protected abstract SoundEvent getSound(boolean powered);

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            if (state.getValue(POWERED)) {
                this.updateNeighbours(state, world, pos);
            }

            super.onRemove(state, world, pos, newState, moved);
        }
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && getConnectedDirection(state) == direction ? 15 : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (state.getValue(POWERED)) {
            if (this.sensitive) {
                this.checkPressed(state, world, pos);
            } else {
                world.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(false)), 3);
                this.updateNeighbours(state, world, pos);
                this.playSound((Player)null, world, pos, false);
                world.gameEvent(GameEvent.BLOCK_UNPRESS, pos);
            }

        }
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!world.isClientSide && this.sensitive && !state.getValue(POWERED)) {
            this.checkPressed(state, world, pos);
        }
    }

    private void checkPressed(BlockState state, Level world, BlockPos pos) {
        List<? extends Entity> list = world.getEntitiesOfClass(AbstractArrow.class, state.getShape(world, pos).bounds().move(pos));
        boolean bl = !list.isEmpty();
        boolean bl2 = state.getValue(POWERED);
        if (bl != bl2) {
            world.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(bl)), 3);
            this.updateNeighbours(state, world, pos);
            this.playSound((Player)null, world, pos, bl);
            world.gameEvent(list.stream().findFirst().orElse((Entity)null), bl ? GameEvent.BLOCK_PRESS : GameEvent.BLOCK_UNPRESS, pos);
        }

        if (bl) {
            world.scheduleTick(new BlockPos(pos), this, this.getPressDuration());
        }

    }

    private void updateNeighbours(BlockState state, Level world, BlockPos pos) {
        world.updateNeighborsAt(pos, this);
        world.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, FACE);
    }
}
