package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.profiling.ProfilerFiller;

public abstract class SimplePreparableReloadListener<T> implements PreparableReloadListener {
    @Override
    public final CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier synchronizer, ResourceManager manager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            return this.prepare(manager, prepareProfiler);
        }, prepareExecutor).thenCompose(synchronizer::wait).thenAcceptAsync((object) -> {
            this.apply(object, manager, applyProfiler);
        }, applyExecutor);
    }

    protected abstract T prepare(ResourceManager manager, ProfilerFiller profiler);

    protected abstract void apply(T prepared, ResourceManager manager, ProfilerFiller profiler);
}
