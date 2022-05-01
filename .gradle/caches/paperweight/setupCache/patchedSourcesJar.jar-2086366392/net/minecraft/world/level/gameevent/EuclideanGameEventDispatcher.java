package net.minecraft.world.level.gameevent;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class EuclideanGameEventDispatcher implements GameEventDispatcher {
    private final List<GameEventListener> listeners = Lists.newArrayList();
    private final Level level;

    public EuclideanGameEventDispatcher(Level world) {
        this.level = world;
    }

    @Override
    public boolean isEmpty() {
        return this.listeners.isEmpty();
    }

    @Override
    public void register(GameEventListener listener) {
        this.listeners.add(listener);
        DebugPackets.sendGameEventListenerInfo(this.level, listener);
    }

    @Override
    public void unregister(GameEventListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void post(GameEvent event, @Nullable Entity entity, BlockPos pos) {
        boolean bl = false;

        for(GameEventListener gameEventListener : this.listeners) {
            if (this.postToListener(this.level, event, entity, pos, gameEventListener)) {
                bl = true;
            }
        }

        if (bl) {
            DebugPackets.sendGameEventInfo(this.level, event, pos);
        }

    }

    private boolean postToListener(Level world, GameEvent event, @Nullable Entity entity, BlockPos pos, GameEventListener listener) {
        Optional<BlockPos> optional = listener.getListenerSource().getPosition(world);
        if (!optional.isPresent()) {
            return false;
        } else {
            double d = optional.get().distSqr(pos);
            int i = listener.getListenerRadius() * listener.getListenerRadius();
            return d <= (double)i && listener.handleGameEvent(world, event, entity, pos);
        }
    }
}
