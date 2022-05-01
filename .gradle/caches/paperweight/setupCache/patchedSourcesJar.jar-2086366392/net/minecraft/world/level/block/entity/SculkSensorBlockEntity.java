package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationListener;

public class SculkSensorBlockEntity extends BlockEntity implements VibrationListener.VibrationListenerConfig {
    private final VibrationListener listener;
    public int lastVibrationFrequency;

    public SculkSensorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.SCULK_SENSOR, pos, state);
        this.listener = new VibrationListener(new BlockPositionSource(this.worldPosition), ((SculkSensorBlock)state.getBlock()).getListenerRange(), this);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.lastVibrationFrequency = nbt.getInt("last_vibration_frequency");
        if (nbt.contains(PAPER_LISTENER_RANGE_NBT_KEY)) this.listener.listenerRange = nbt.getInt(PAPER_LISTENER_RANGE_NBT_KEY); // Paper
    }

    private static final String PAPER_LISTENER_RANGE_NBT_KEY = "Paper.ListenerRange"; // Paper
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("last_vibration_frequency", this.lastVibrationFrequency);
        if (this.listener.listenerRange != ((SculkSensorBlock) net.minecraft.world.level.block.Blocks.SCULK_SENSOR).getListenerRange()) nbt.putInt(PAPER_LISTENER_RANGE_NBT_KEY, this.listener.listenerRange); // Paper - only save if it's different from the default
    }

    public VibrationListener getListener() {
        return this.listener;
    }

    public int getLastVibrationFrequency() {
        return this.lastVibrationFrequency;
    }

    @Override
    public boolean shouldListen(Level world, GameEventListener listener, BlockPos pos, GameEvent event, @Nullable Entity entity) {
        boolean bl = event == GameEvent.BLOCK_DESTROY && pos.equals(this.getBlockPos());
        boolean bl2 = event == GameEvent.BLOCK_PLACE && pos.equals(this.getBlockPos());
        return !bl && !bl2 && SculkSensorBlock.canActivate(this.getBlockState());
    }

    @Override
    public void onSignalReceive(Level world, GameEventListener listener, GameEvent event, int distance) {
        BlockState blockState = this.getBlockState();
        if (!world.isClientSide() && SculkSensorBlock.canActivate(blockState)) {
            this.lastVibrationFrequency = SculkSensorBlock.VIBRATION_STRENGTH_FOR_EVENT.getInt(event);
            SculkSensorBlock.activate(world, this.worldPosition, blockState, getRedstoneStrengthForDistance(distance, listener.getListenerRadius()));
        }

    }

    public static int getRedstoneStrengthForDistance(int distance, int range) {
        double d = (double)distance / (double)range;
        return Math.max(1, 15 - Mth.floor(d * 15.0D));
    }
}
