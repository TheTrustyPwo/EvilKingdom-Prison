package net.minecraft.world.level.gameevent;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

public class GameEventListenerRegistrar {
    private final GameEventListener listener;
    @Nullable
    private SectionPos sectionPos;

    public GameEventListenerRegistrar(GameEventListener listener) {
        this.listener = listener;
    }

    public void onListenerRemoved(Level world) {
        this.ifEventDispatcherExists(world, this.sectionPos, (dispatcher) -> {
            dispatcher.unregister(this.listener);
        });
    }

    public void onListenerMove(Level world) {
        Optional<BlockPos> optional = this.listener.getListenerSource().getPosition(world);
        if (optional.isPresent()) {
            long l = SectionPos.blockToSection(optional.get().asLong());
            if (this.sectionPos == null || this.sectionPos.asLong() != l) {
                SectionPos sectionPos = this.sectionPos;
                this.sectionPos = SectionPos.of(l);
                this.ifEventDispatcherExists(world, sectionPos, (dispatcher) -> {
                    dispatcher.unregister(this.listener);
                });
                this.ifEventDispatcherExists(world, this.sectionPos, (dispatcher) -> {
                    dispatcher.register(this.listener);
                });
            }
        }

    }

    private void ifEventDispatcherExists(Level world, @Nullable SectionPos sectionPos, Consumer<GameEventDispatcher> action) {
        if (sectionPos != null) {
            ChunkAccess chunkAccess = world.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.FULL, false);
            if (chunkAccess != null) {
                action.accept(chunkAccess.getEventDispatcher(sectionPos.y()));
            }

        }
    }
}
