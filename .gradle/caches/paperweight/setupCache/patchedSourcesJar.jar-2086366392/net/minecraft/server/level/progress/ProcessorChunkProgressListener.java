package net.minecraft.server.level.progress;

import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

public class ProcessorChunkProgressListener implements ChunkProgressListener {
    private final ChunkProgressListener delegate;
    private final ProcessorMailbox<Runnable> mailbox;

    private ProcessorChunkProgressListener(ChunkProgressListener progressListener, Executor executor) {
        this.delegate = progressListener;
        this.mailbox = ProcessorMailbox.create(executor, "progressListener");
    }

    public static ProcessorChunkProgressListener createStarted(ChunkProgressListener progressListener, Executor executor) {
        ProcessorChunkProgressListener processorChunkProgressListener = new ProcessorChunkProgressListener(progressListener, executor);
        processorChunkProgressListener.start();
        return processorChunkProgressListener;
    }

    @Override
    public void updateSpawnPos(ChunkPos spawnPos) {
        this.mailbox.tell(() -> {
            this.delegate.updateSpawnPos(spawnPos);
        });
    }

    @Override
    public void onStatusChange(ChunkPos pos, @Nullable ChunkStatus status) {
        this.mailbox.tell(() -> {
            this.delegate.onStatusChange(pos, status);
        });
    }

    @Override
    public void start() {
        this.mailbox.tell(this.delegate::start);
    }

    @Override
    public void stop() {
        this.mailbox.tell(this.delegate::stop);
    }
}
