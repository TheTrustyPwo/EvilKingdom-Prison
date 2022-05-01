package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PistonBaseBlock extends DirectionalBlock {
    public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
    public static final int TRIGGER_EXTEND = 0;
    public static final int TRIGGER_CONTRACT = 1;
    public static final int TRIGGER_DROP = 2;
    public static final float PLATFORM_THICKNESS = 4.0F;
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(4.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 12.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape UP_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    protected static final VoxelShape DOWN_AABB = Block.box(0.0D, 4.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private final boolean isSticky;

    public PistonBaseBlock(boolean sticky, BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(EXTENDED, Boolean.valueOf(false)));
        this.isSticky = sticky;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(EXTENDED)) {
            switch((Direction)state.getValue(FACING)) {
            case DOWN:
                return DOWN_AABB;
            case UP:
            default:
                return UP_AABB;
            case NORTH:
                return NORTH_AABB;
            case SOUTH:
                return SOUTH_AABB;
            case WEST:
                return WEST_AABB;
            case EAST:
                return EAST_AABB;
            }
        } else {
            return Shapes.block();
        }
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (!world.isClientSide) {
            this.checkIfExtend(world, pos, state);
        }

    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (!world.isClientSide) {
            this.checkIfExtend(world, pos, state);
        }

    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            if (!world.isClientSide && world.getBlockEntity(pos) == null) {
                this.checkIfExtend(world, pos, state);
            }

        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite()).setValue(EXTENDED, Boolean.valueOf(false));
    }

    private void checkIfExtend(Level world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        boolean bl = this.getNeighborSignal(world, pos, direction);
        if (bl && !state.getValue(EXTENDED)) {
            if ((new PistonStructureResolver(world, pos, direction, true)).resolve()) {
                world.blockEvent(pos, this, 0, direction.get3DDataValue());
            }
        } else if (!bl && state.getValue(EXTENDED)) {
            BlockPos blockPos = pos.relative(direction, 2);
            BlockState blockState = world.getBlockState(blockPos);
            int i = 1;
            if (blockState.is(Blocks.MOVING_PISTON) && blockState.getValue(FACING) == direction) {
                BlockEntity blockEntity = world.getBlockEntity(blockPos);
                if (blockEntity instanceof PistonMovingBlockEntity) {
                    PistonMovingBlockEntity pistonMovingBlockEntity = (PistonMovingBlockEntity)blockEntity;
                    if (pistonMovingBlockEntity.isExtending() && (pistonMovingBlockEntity.getProgress(0.0F) < 0.5F || world.getGameTime() == pistonMovingBlockEntity.getLastTicked() || ((ServerLevel)world).isHandlingTick())) {
                        i = 2;
                    }
                }
            }

            world.blockEvent(pos, this, i, direction.get3DDataValue());
        }

    }

    private boolean getNeighborSignal(Level world, BlockPos pos, Direction pistonFace) {
        for(Direction direction : Direction.values()) {
            if (direction != pistonFace && world.hasSignal(pos.relative(direction), direction)) {
                return true;
            }
        }

        if (world.hasSignal(pos, Direction.DOWN)) {
            return true;
        } else {
            BlockPos blockPos = pos.above();

            for(Direction direction2 : Direction.values()) {
                if (direction2 != Direction.DOWN && world.hasSignal(blockPos.relative(direction2), direction2)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        Direction direction = state.getValue(FACING);
        if (!world.isClientSide) {
            boolean bl = this.getNeighborSignal(world, pos, direction);
            if (bl && (type == 1 || type == 2)) {
                world.setBlock(pos, state.setValue(EXTENDED, Boolean.valueOf(true)), 2);
                return false;
            }

            if (!bl && type == 0) {
                return false;
            }
        }

        if (type == 0) {
            if (!this.moveBlocks(world, pos, direction, true)) {
                return false;
            }

            world.setBlock(pos, state.setValue(EXTENDED, Boolean.valueOf(true)), 67);
            world.playSound((Player)null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, world.random.nextFloat() * 0.25F + 0.6F);
            world.gameEvent(GameEvent.PISTON_EXTEND, pos);
        } else if (type == 1 || type == 2) {
            BlockEntity blockEntity = world.getBlockEntity(pos.relative(direction));
            if (blockEntity instanceof PistonMovingBlockEntity) {
                ((PistonMovingBlockEntity)blockEntity).finalTick();
            }

            BlockState blockState = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, direction).setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
            world.setBlock(pos, blockState, 20);
            world.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(pos, blockState, this.defaultBlockState().setValue(FACING, Direction.from3DDataValue(data & 7)), direction, false, true));
            world.blockUpdated(pos, blockState.getBlock());
            blockState.updateNeighbourShapes(world, pos, 2);
            if (this.isSticky) {
                BlockPos blockPos = pos.offset(direction.getStepX() * 2, direction.getStepY() * 2, direction.getStepZ() * 2);
                BlockState blockState2 = world.getBlockState(blockPos);
                boolean bl2 = false;
                if (blockState2.is(Blocks.MOVING_PISTON)) {
                    BlockEntity blockEntity2 = world.getBlockEntity(blockPos);
                    if (blockEntity2 instanceof PistonMovingBlockEntity) {
                        PistonMovingBlockEntity pistonMovingBlockEntity = (PistonMovingBlockEntity)blockEntity2;
                        if (pistonMovingBlockEntity.getDirection() == direction && pistonMovingBlockEntity.isExtending()) {
                            pistonMovingBlockEntity.finalTick();
                            bl2 = true;
                        }
                    }
                }

                if (!bl2) {
                    if (type != 1 || blockState2.isAir() || !isPushable(blockState2, world, blockPos, direction.getOpposite(), false, direction) || blockState2.getPistonPushReaction() != PushReaction.NORMAL && !blockState2.is(Blocks.PISTON) && !blockState2.is(Blocks.STICKY_PISTON)) {
                        world.removeBlock(pos.relative(direction), false);
                    } else {
                        this.moveBlocks(world, pos, direction, false);
                    }
                }
            } else {
                world.removeBlock(pos.relative(direction), false);
            }

            world.playSound((Player)null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, world.random.nextFloat() * 0.15F + 0.6F);
            world.gameEvent(GameEvent.PISTON_CONTRACT, pos);
        }

        return true;
    }

    public static boolean isPushable(BlockState state, Level world, BlockPos pos, Direction direction, boolean canBreak, Direction pistonDir) {
        if (pos.getY() >= world.getMinBuildHeight() && pos.getY() <= world.getMaxBuildHeight() - 1 && world.getWorldBorder().isWithinBounds(pos)) {
            if (state.isAir()) {
                return true;
            } else if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.CRYING_OBSIDIAN) && !state.is(Blocks.RESPAWN_ANCHOR)) {
                if (direction == Direction.DOWN && pos.getY() == world.getMinBuildHeight()) {
                    return false;
                } else if (direction == Direction.UP && pos.getY() == world.getMaxBuildHeight() - 1) {
                    return false;
                } else {
                    if (!state.is(Blocks.PISTON) && !state.is(Blocks.STICKY_PISTON)) {
                        if (state.getDestroySpeed(world, pos) == -1.0F) {
                            return false;
                        }

                        switch(state.getPistonPushReaction()) {
                        case BLOCK:
                            return false;
                        case DESTROY:
                            return canBreak;
                        case PUSH_ONLY:
                            return direction == pistonDir;
                        }
                    } else if (state.getValue(EXTENDED)) {
                        return false;
                    }

                    return !state.hasBlockEntity();
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean moveBlocks(Level world, BlockPos pos, Direction dir, boolean retract) {
        BlockPos blockPos = pos.relative(dir);
        if (!retract && world.getBlockState(blockPos).is(Blocks.PISTON_HEAD)) {
            world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 20);
        }

        PistonStructureResolver pistonStructureResolver = new PistonStructureResolver(world, pos, dir, retract);
        if (!pistonStructureResolver.resolve()) {
            return false;
        } else {
            Map<BlockPos, BlockState> map = Maps.newHashMap();
            List<BlockPos> list = pistonStructureResolver.getToPush();
            List<BlockState> list2 = Lists.newArrayList();

            for(int i = 0; i < list.size(); ++i) {
                BlockPos blockPos2 = list.get(i);
                BlockState blockState = world.getBlockState(blockPos2);
                list2.add(blockState);
                map.put(blockPos2, blockState);
            }

            List<BlockPos> list3 = pistonStructureResolver.getToDestroy();
            BlockState[] blockStates = new BlockState[list.size() + list3.size()];
            Direction direction = retract ? dir : dir.getOpposite();
            int j = 0;

            for(int k = list3.size() - 1; k >= 0; --k) {
                BlockPos blockPos3 = list3.get(k);
                BlockState blockState2 = world.getBlockState(blockPos3);
                BlockEntity blockEntity = blockState2.hasBlockEntity() ? world.getBlockEntity(blockPos3) : null;
                dropResources(blockState2, world, blockPos3, blockEntity);
                world.setBlock(blockPos3, Blocks.AIR.defaultBlockState(), 18);
                if (!blockState2.is(BlockTags.FIRE)) {
                    world.addDestroyBlockEffect(blockPos3, blockState2);
                }

                blockStates[j++] = blockState2;
            }

            for(int l = list.size() - 1; l >= 0; --l) {
                BlockPos blockPos4 = list.get(l);
                BlockState blockState3 = world.getBlockState(blockPos4);
                blockPos4 = blockPos4.relative(direction);
                map.remove(blockPos4);
                BlockState blockState4 = Blocks.MOVING_PISTON.defaultBlockState().setValue(FACING, dir);
                world.setBlock(blockPos4, blockState4, 68);
                world.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(blockPos4, blockState4, list2.get(l), dir, retract, false));
                blockStates[j++] = blockState3;
            }

            if (retract) {
                PistonType pistonType = this.isSticky ? PistonType.STICKY : PistonType.DEFAULT;
                BlockState blockState5 = Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING, dir).setValue(PistonHeadBlock.TYPE, pistonType);
                BlockState blockState6 = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, dir).setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
                map.remove(blockPos);
                world.setBlock(blockPos, blockState6, 68);
                world.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(blockPos, blockState6, blockState5, dir, true, true));
            }

            BlockState blockState7 = Blocks.AIR.defaultBlockState();

            for(BlockPos blockPos5 : map.keySet()) {
                world.setBlock(blockPos5, blockState7, 82);
            }

            for(Entry<BlockPos, BlockState> entry : map.entrySet()) {
                BlockPos blockPos6 = entry.getKey();
                BlockState blockState8 = entry.getValue();
                blockState8.updateIndirectNeighbourShapes(world, blockPos6, 2);
                blockState7.updateNeighbourShapes(world, blockPos6, 2);
                blockState7.updateIndirectNeighbourShapes(world, blockPos6, 2);
            }

            j = 0;

            for(int m = list3.size() - 1; m >= 0; --m) {
                BlockState blockState9 = blockStates[j++];
                BlockPos blockPos7 = list3.get(m);
                blockState9.updateIndirectNeighbourShapes(world, blockPos7, 2);
                world.updateNeighborsAt(blockPos7, blockState9.getBlock());
            }

            for(int n = list.size() - 1; n >= 0; --n) {
                world.updateNeighborsAt(list.get(n), blockStates[j++].getBlock());
            }

            if (retract) {
                world.updateNeighborsAt(blockPos, Blocks.PISTON_HEAD);
            }

            return true;
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
        builder.add(FACING, EXTENDED);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return state.getValue(EXTENDED);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
        return false;
    }
}
