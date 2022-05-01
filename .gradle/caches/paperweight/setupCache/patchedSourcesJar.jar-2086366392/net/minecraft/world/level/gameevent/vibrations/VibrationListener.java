package net.minecraft.world.level.gameevent.vibrations;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftNamespacedKey;
import org.bukkit.event.block.BlockReceiveGameEvent;
// CraftBukkit end

public class VibrationListener implements GameEventListener {

    protected final PositionSource listenerSource;
    public int listenerRange;
    protected final VibrationListener.VibrationListenerConfig config;
    protected Optional<GameEvent> receivingEvent = Optional.empty();
    protected int receivingDistance;
    protected int travelTimeInTicks = 0;

    public VibrationListener(PositionSource positionSource, int range, VibrationListener.VibrationListenerConfig listener) {
        this.listenerSource = positionSource;
        this.listenerRange = range;
        this.config = listener;
    }

    public void tick(Level world) {
        if (this.receivingEvent.isPresent()) {
            --this.travelTimeInTicks;
            if (this.travelTimeInTicks <= 0) {
                this.travelTimeInTicks = 0;
                this.config.onSignalReceive(world, this, (GameEvent) this.receivingEvent.get(), this.receivingDistance);
                this.receivingEvent = Optional.empty();
            }
        }

    }

    @Override
    public PositionSource getListenerSource() {
        return this.listenerSource;
    }

    @Override
    public int getListenerRadius() {
        return this.listenerRange;
    }

    @Override
    public boolean handleGameEvent(Level world, GameEvent event, @Nullable Entity entity, BlockPos pos) {
        if (!this.isValidVibration(event, entity)) {
            return false;
        } else {
            Optional<BlockPos> optional = this.listenerSource.getPosition(world);

            if (!optional.isPresent()) {
                return false;
            } else {
                BlockPos blockposition1 = (BlockPos) optional.get();

                // CraftBukkit start
                boolean defaultCancel = !this.config.shouldListen(world, this, pos, event, entity);
                BlockReceiveGameEvent event1 = new BlockReceiveGameEvent(org.bukkit.GameEvent.getByKey(CraftNamespacedKey.fromMinecraft(Registry.GAME_EVENT.getKey(event))), CraftBlock.at(world, blockposition1), (entity == null) ? null : entity.getBukkitEntity());
                event1.setCancelled(defaultCancel);
                world.getCraftServer().getPluginManager().callEvent(event1);
                if (event1.isCancelled()) {
                    // CraftBukkit end
                    return false;
                } else if (this.isOccluded(world, pos, blockposition1)) {
                    return false;
                } else {
                    this.sendSignal(world, event, pos, blockposition1);
                    return true;
                }
            }
        }
    }

    private boolean isValidVibration(GameEvent event, @Nullable Entity entity) {
        if (this.receivingEvent.isPresent()) {
            return false;
        } else if (!event.is(GameEventTags.VIBRATIONS)) {
            return false;
        } else {
            if (entity != null) {
                if (event.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING) && entity.isSteppingCarefully()) {
                    return false;
                }

                if (entity.occludesVibrations()) {
                    return false;
                }
            }

            return entity == null || !entity.isSpectator();
        }
    }

    private void sendSignal(Level world, GameEvent event, BlockPos pos, BlockPos sourcePos) {
        this.receivingEvent = Optional.of(event);
        if (world instanceof ServerLevel) {
            this.receivingDistance = Mth.floor(Math.sqrt(pos.distSqr(sourcePos)));
            this.travelTimeInTicks = this.receivingDistance;
            ((ServerLevel) world).sendVibrationParticle(new VibrationPath(pos, this.listenerSource, this.travelTimeInTicks));
        }

    }

    private boolean isOccluded(Level world, BlockPos pos, BlockPos sourcePos) {
        return world.isBlockInLine(new ClipBlockStateContext(Vec3.atCenterOf(pos), Vec3.atCenterOf(sourcePos), (iblockdata) -> {
            return iblockdata.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS);
        })).getType() == HitResult.Type.BLOCK;
    }

    public interface VibrationListenerConfig {

        boolean shouldListen(Level world, GameEventListener listener, BlockPos pos, GameEvent event, @Nullable Entity entity);

        void onSignalReceive(Level world, GameEventListener listener, GameEvent event, int distance);
    }
}
