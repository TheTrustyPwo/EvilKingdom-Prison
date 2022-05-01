package net.minecraft.world.level.gameevent;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public interface GameEventDispatcher {
    GameEventDispatcher NOOP = new GameEventDispatcher() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void register(GameEventListener listener) {
        }

        @Override
        public void unregister(GameEventListener listener) {
        }

        @Override
        public void post(GameEvent event, @Nullable Entity entity, BlockPos pos) {
        }
    };

    boolean isEmpty();

    void register(GameEventListener listener);

    void unregister(GameEventListener listener);

    void post(GameEvent event, @Nullable Entity entity, BlockPos pos);
}
