package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;

public class NoteBlock extends Block {

    public static final EnumProperty<NoteBlockInstrument> INSTRUMENT = BlockStateProperties.NOTEBLOCK_INSTRUMENT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty NOTE = BlockStateProperties.NOTE;

    public NoteBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState) ((BlockState) ((BlockState) ((BlockState) this.stateDefinition.any()).setValue(NoteBlock.INSTRUMENT, NoteBlockInstrument.HARP)).setValue(NoteBlock.NOTE, 0)).setValue(NoteBlock.POWERED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return (BlockState) this.defaultBlockState().setValue(NoteBlock.INSTRUMENT, NoteBlockInstrument.byState(ctx.getLevel().getBlockState(ctx.getClickedPos().below())));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN ? (BlockState) state.setValue(NoteBlock.INSTRUMENT, NoteBlockInstrument.byState(neighborState)) : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        boolean flag1 = world.hasNeighborSignal(pos);

        if (flag1 != (Boolean) state.getValue(NoteBlock.POWERED)) {
            if (flag1) {
                this.playNote(world, pos, state); // CraftBukkit
                state = world.getBlockState(pos); // CraftBukkit - SPIGOT-5617: update in case changed in event
            }

            world.setBlock(pos, (BlockState) state.setValue(NoteBlock.POWERED, flag1), 3);
        }

    }

    private void playNote(Level world, BlockPos blockposition, BlockState data) { // CraftBukkit
        if (world.getBlockState(blockposition.above()).isAir()) {
            // CraftBukkit start
            // Paper start - move NotePlayEvent call to fix instrument/note changes
                world.blockEvent(blockposition, this, 0, 0);
            // Paper end
            // CraftBukkit end
        }

    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            state = (BlockState) state.cycle(NoteBlock.NOTE);
            world.setBlock(pos, state, 3);
            this.playNote(world, pos, state); // CraftBukkit
            player.awardStat(Stats.TUNE_NOTEBLOCK);
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void attack(BlockState state, Level world, BlockPos pos, Player player) {
        if (!world.isClientSide) {
            this.playNote(world, pos, state); // CraftBukkit
            player.awardStat(Stats.PLAY_NOTEBLOCK);
        }
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        // Paper start - move NotePlayEvent call to fix instrument/note changes
        org.bukkit.event.block.NotePlayEvent event = org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callNotePlayEvent(world, pos, state.getValue(INSTRUMENT), state.getValue(NOTE));
        if (event.isCancelled()) return false;
        int k = event.getNote().getId();
        float f = (float) Math.pow(2.0D, (double) (k - 12) / 12.0D);

        world.playSound(null, pos, org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData.toNMS(event.getInstrument(), NoteBlockInstrument.class).getSoundEvent(), SoundSource.RECORDS, 3.0F, f);
        // Paper end
        world.addParticle(ParticleTypes.NOTE, (double) pos.getX() + 0.5D, (double) pos.getY() + 1.2D, (double) pos.getZ() + 0.5D, (double) k / 24.0D, 0.0D, 0.0D);
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NoteBlock.INSTRUMENT, NoteBlock.POWERED, NoteBlock.NOTE);
    }
}
