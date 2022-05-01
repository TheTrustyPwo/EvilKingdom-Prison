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
// CraftBukkit start
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityInteractEvent;
// CraftBukkit end

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
        this.registerDefaultState((BlockState) ((BlockState) ((BlockState) ((BlockState) this.stateDefinition.any()).setValue(ButtonBlock.FACING, Direction.NORTH)).setValue(ButtonBlock.POWERED, false)).setValue(ButtonBlock.FACE, AttachFace.WALL));
        this.sensitive = wooden;
    }

    private int getPressDuration() {
        return this.sensitive ? 30 : 20;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction enumdirection = (Direction) state.getValue(ButtonBlock.FACING);
        boolean flag = (Boolean) state.getValue(ButtonBlock.POWERED);

        switch ((AttachFace) state.getValue(ButtonBlock.FACE)) {
            case FLOOR:
                if (enumdirection.getAxis() == Direction.Axis.X) {
                    return flag ? ButtonBlock.PRESSED_FLOOR_AABB_X : ButtonBlock.FLOOR_AABB_X;
                }

                return flag ? ButtonBlock.PRESSED_FLOOR_AABB_Z : ButtonBlock.FLOOR_AABB_Z;
            case WALL:
                switch (enumdirection) {
                    case EAST:
                        return flag ? ButtonBlock.PRESSED_EAST_AABB : ButtonBlock.EAST_AABB;
                    case WEST:
                        return flag ? ButtonBlock.PRESSED_WEST_AABB : ButtonBlock.WEST_AABB;
                    case SOUTH:
                        return flag ? ButtonBlock.PRESSED_SOUTH_AABB : ButtonBlock.SOUTH_AABB;
                    case NORTH:
                    default:
                        return flag ? ButtonBlock.PRESSED_NORTH_AABB : ButtonBlock.NORTH_AABB;
                }
            case CEILING:
            default:
                return enumdirection.getAxis() == Direction.Axis.X ? (flag ? ButtonBlock.PRESSED_CEILING_AABB_X : ButtonBlock.CEILING_AABB_X) : (flag ? ButtonBlock.PRESSED_CEILING_AABB_Z : ButtonBlock.CEILING_AABB_Z);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if ((Boolean) state.getValue(ButtonBlock.POWERED)) {
            return InteractionResult.CONSUME;
        } else {
            // CraftBukkit start
            boolean powered = ((Boolean) state.getValue(POWERED));
            org.bukkit.block.Block block = world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            int old = (powered) ? 15 : 0;
            int current = (!powered) ? 15 : 0;

            BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(block, old, current);
            world.getCraftServer().getPluginManager().callEvent(eventRedstone);

            if ((eventRedstone.getNewCurrent() > 0) != (!powered)) {
                return InteractionResult.SUCCESS;
            }
            // CraftBukkit end
            this.press(state, world, pos);
            this.playSound(player, world, pos, true);
            world.gameEvent(player, GameEvent.BLOCK_PRESS, pos);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    public void press(BlockState state, Level world, BlockPos pos) {
        world.setBlock(pos, (BlockState) state.setValue(ButtonBlock.POWERED, true), 3);
        this.updateNeighbours(state, world, pos);
        world.scheduleTick(pos, (Block) this, this.getPressDuration());
    }

    protected void playSound(@Nullable Player player, LevelAccessor world, BlockPos pos, boolean powered) {
        world.playSound(powered ? player : null, pos, this.getSound(powered), SoundSource.BLOCKS, 0.3F, powered ? 0.6F : 0.5F);
    }

    protected abstract SoundEvent getSound(boolean powered);

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            if ((Boolean) state.getValue(ButtonBlock.POWERED)) {
                this.updateNeighbours(state, world, pos);
            }

            super.onRemove(state, world, pos, newState, moved);
        }
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return (Boolean) state.getValue(ButtonBlock.POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return (Boolean) state.getValue(ButtonBlock.POWERED) && getConnectedDirection(state) == direction ? 15 : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if ((Boolean) state.getValue(ButtonBlock.POWERED)) {
            if (this.sensitive) {
                this.checkPressed(state, world, pos);
            } else {
                // CraftBukkit start
                org.bukkit.block.Block block = world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());

                BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(block, 15, 0);
                world.getCraftServer().getPluginManager().callEvent(eventRedstone);

                if (eventRedstone.getNewCurrent() > 0) {
                    return;
                }
                // CraftBukkit end
                world.setBlock(pos, (BlockState) state.setValue(ButtonBlock.POWERED, false), 3);
                this.updateNeighbours(state, world, pos);
                this.playSound((Player) null, world, pos, false);
                world.gameEvent(GameEvent.BLOCK_UNPRESS, pos);
            }

        }
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!new io.papermc.paper.event.entity.EntityInsideBlockEvent(entity.getBukkitEntity(), org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock.at(world, pos)).callEvent()) { return; } // Paper
        if (!world.isClientSide && this.sensitive && !(Boolean) state.getValue(ButtonBlock.POWERED)) {
            this.checkPressed(state, world, pos);
        }
    }

    private void checkPressed(BlockState state, Level world, BlockPos pos) {
        List<? extends Entity> list = world.getEntitiesOfClass(AbstractArrow.class, state.getShape(world, pos).bounds().move(pos));
        boolean flag = !list.isEmpty();
        boolean flag1 = (Boolean) state.getValue(ButtonBlock.POWERED);

        // CraftBukkit start - Call interact event when arrows turn on wooden buttons
        if (flag1 != flag && flag) {
            org.bukkit.block.Block block = world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            boolean allowed = false;

            // If all of the events are cancelled block the button press, else allow
            for (Object object : list) {
                if (object != null) {
                    EntityInteractEvent event = new EntityInteractEvent(((Entity) object).getBukkitEntity(), block);
                    world.getCraftServer().getPluginManager().callEvent(event);

                    if (!event.isCancelled()) {
                        allowed = true;
                        break;
                    }
                }
            }

            if (!allowed) {
                return;
            }
        }
        // CraftBukkit end

        if (flag != flag1) {
            // CraftBukkit start
            boolean powered = flag1;
            org.bukkit.block.Block block = world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            int old = (powered) ? 15 : 0;
            int current = (!powered) ? 15 : 0;

            BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(block, old, current);
            world.getCraftServer().getPluginManager().callEvent(eventRedstone);

            if ((flag && eventRedstone.getNewCurrent() <= 0) || (!flag && eventRedstone.getNewCurrent() > 0)) {
                return;
            }
            // CraftBukkit end
            world.setBlock(pos, (BlockState) state.setValue(ButtonBlock.POWERED, flag), 3);
            this.updateNeighbours(state, world, pos);
            this.playSound((Player) null, world, pos, flag);
            world.gameEvent((Entity) list.stream().findFirst().orElse(null), flag ? GameEvent.BLOCK_PRESS : GameEvent.BLOCK_UNPRESS, pos); // CraftBukkit - decompile error
        }

        if (flag) {
            world.scheduleTick(new BlockPos(pos), (Block) this, this.getPressDuration());
        }

    }

    private void updateNeighbours(BlockState state, Level world, BlockPos pos) {
        world.updateNeighborsAt(pos, this);
        world.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ButtonBlock.FACING, ButtonBlock.POWERED, ButtonBlock.FACE);
    }
}
