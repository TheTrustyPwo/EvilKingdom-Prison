package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LecternBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty HAS_BOOK = BlockStateProperties.HAS_BOOK;
    public static final VoxelShape SHAPE_BASE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    public static final VoxelShape SHAPE_POST = Block.box(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D);
    public static final VoxelShape SHAPE_COMMON = Shapes.or(SHAPE_BASE, SHAPE_POST);
    public static final VoxelShape SHAPE_TOP_PLATE = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 15.0D, 16.0D);
    public static final VoxelShape SHAPE_COLLISION = Shapes.or(SHAPE_COMMON, SHAPE_TOP_PLATE);
    public static final VoxelShape SHAPE_WEST = Shapes.or(Block.box(1.0D, 10.0D, 0.0D, 5.333333D, 14.0D, 16.0D), Block.box(5.333333D, 12.0D, 0.0D, 9.666667D, 16.0D, 16.0D), Block.box(9.666667D, 14.0D, 0.0D, 14.0D, 18.0D, 16.0D), SHAPE_COMMON);
    public static final VoxelShape SHAPE_NORTH = Shapes.or(Block.box(0.0D, 10.0D, 1.0D, 16.0D, 14.0D, 5.333333D), Block.box(0.0D, 12.0D, 5.333333D, 16.0D, 16.0D, 9.666667D), Block.box(0.0D, 14.0D, 9.666667D, 16.0D, 18.0D, 14.0D), SHAPE_COMMON);
    public static final VoxelShape SHAPE_EAST = Shapes.or(Block.box(10.666667D, 10.0D, 0.0D, 15.0D, 14.0D, 16.0D), Block.box(6.333333D, 12.0D, 0.0D, 10.666667D, 16.0D, 16.0D), Block.box(2.0D, 14.0D, 0.0D, 6.333333D, 18.0D, 16.0D), SHAPE_COMMON);
    public static final VoxelShape SHAPE_SOUTH = Shapes.or(Block.box(0.0D, 10.0D, 10.666667D, 16.0D, 14.0D, 15.0D), Block.box(0.0D, 12.0D, 6.333333D, 16.0D, 16.0D, 10.666667D), Block.box(0.0D, 14.0D, 2.0D, 16.0D, 18.0D, 6.333333D), SHAPE_COMMON);
    private static final int PAGE_CHANGE_IMPULSE_TICKS = 2;

    protected LecternBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(HAS_BOOK, Boolean.valueOf(false)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return SHAPE_COMMON;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        ItemStack itemStack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        boolean bl = false;
        if (!level.isClientSide && player != null && player.canUseGameMasterBlocks()) {
            CompoundTag compoundTag = BlockItem.getBlockEntityData(itemStack);
            if (compoundTag != null && compoundTag.contains("Book")) {
                bl = true;
            }
        }

        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(HAS_BOOK, Boolean.valueOf(bl));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE_COLLISION;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        switch((Direction)state.getValue(FACING)) {
        case NORTH:
            return SHAPE_NORTH;
        case SOUTH:
            return SHAPE_SOUTH;
        case EAST:
            return SHAPE_EAST;
        case WEST:
            return SHAPE_WEST;
        default:
            return SHAPE_COMMON;
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, HAS_BOOK);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LecternBlockEntity(pos, state);
    }

    public static boolean tryPlaceBook(@Nullable Player player, Level world, BlockPos pos, BlockState state, ItemStack stack) {
        if (!state.getValue(HAS_BOOK)) {
            if (!world.isClientSide) {
                placeBook(player, world, pos, state, stack);
            }

            return true;
        } else {
            return false;
        }
    }

    private static void placeBook(@Nullable Player player, Level world, BlockPos pos, BlockState state, ItemStack stack) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof LecternBlockEntity) {
            LecternBlockEntity lecternBlockEntity = (LecternBlockEntity)blockEntity;
            lecternBlockEntity.setBook(stack.split(1));
            resetBookState(world, pos, state, true);
            world.playSound((Player)null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
            world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }

    }

    public static void resetBookState(Level world, BlockPos pos, BlockState state, boolean hasBook) {
        world.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(false)).setValue(HAS_BOOK, Boolean.valueOf(hasBook)), 3);
        updateBelow(world, pos, state);
    }

    public static void signalPageChange(Level world, BlockPos pos, BlockState state) {
        changePowered(world, pos, state, true);
        world.scheduleTick(pos, state.getBlock(), 2);
        world.levelEvent(1043, pos, 0);
    }

    private static void changePowered(Level world, BlockPos pos, BlockState state, boolean powered) {
        world.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(powered)), 3);
        updateBelow(world, pos, state);
    }

    private static void updateBelow(Level world, BlockPos pos, BlockState state) {
        world.updateNeighborsAt(pos.below(), state.getBlock());
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        changePowered(world, pos, state, false);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (state.getValue(HAS_BOOK)) {
                this.popBook(state, world, pos);
            }

            if (state.getValue(POWERED)) {
                world.updateNeighborsAt(pos.below(), this);
            }

            super.onRemove(state, world, pos, newState, moved);
        }
    }

    private void popBook(BlockState state, Level world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof LecternBlockEntity) {
            LecternBlockEntity lecternBlockEntity = (LecternBlockEntity)blockEntity;
            Direction direction = state.getValue(FACING);
            ItemStack itemStack = lecternBlockEntity.getBook().copy();
            float f = 0.25F * (float)direction.getStepX();
            float g = 0.25F * (float)direction.getStepZ();
            ItemEntity itemEntity = new ItemEntity(world, (double)pos.getX() + 0.5D + (double)f, (double)(pos.getY() + 1), (double)pos.getZ() + 0.5D + (double)g, itemStack);
            itemEntity.setDefaultPickUpDelay();
            world.addFreshEntity(itemEntity);
            lecternBlockEntity.clearContent();
        }

    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return direction == Direction.UP && state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (state.getValue(HAS_BOOK)) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof LecternBlockEntity) {
                return ((LecternBlockEntity)blockEntity).getRedstoneSignal();
            }
        }

        return 0;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(HAS_BOOK)) {
            if (!world.isClientSide) {
                this.openScreen(world, pos, player);
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        } else {
            ItemStack itemStack = player.getItemInHand(hand);
            return !itemStack.isEmpty() && !itemStack.is(ItemTags.LECTERN_BOOKS) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return !state.getValue(HAS_BOOK) ? null : super.getMenuProvider(state, world, pos);
    }

    private void openScreen(Level world, BlockPos pos, Player player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof LecternBlockEntity) {
            player.openMenu((LecternBlockEntity)blockEntity);
            player.awardStat(Stats.INTERACT_WITH_LECTERN);
        }

    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
        return false;
    }
}
