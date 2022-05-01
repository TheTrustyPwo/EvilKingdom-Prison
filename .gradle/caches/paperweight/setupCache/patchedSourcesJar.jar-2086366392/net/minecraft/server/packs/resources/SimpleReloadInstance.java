package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.InactiveProfiler;

public class SimpleReloadInstance<S> implements ReloadInstance {
    private static final int PREPARATION_PROGRESS_WEIGHT = 2;
    private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
    private static final int LISTENER_PROGRESS_WEIGHT = 1;
    protected final CompletableFuture<Unit> allPreparations = new CompletableFuture<>();
    protected final CompletableFuture<List<S>> allDone;
    final Set<PreparableReloadListener> preparingListeners;
    private final int listenerCount;
    private int startedReloads;
    private int finishedReloads;
    private final AtomicInteger startedTaskCounter = new AtomicInteger();
    private final AtomicInteger doneTaskCounter = new AtomicInteger();

    public static SimpleReloadInstance<Void> of(ResourceManager manager, List<PreparableReloadListener> reloaders, Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage) {
        return new SimpleReloadInstance<>(prepareExecutor, applyExecutor, manager, reloaders, (synchronizer, resourceManager, reloader, prepare, apply) -> {
            return reloader.reload(synchronizer, resourceManager, InactiveProfiler.INSTANCE, InactiveProfiler.INSTANCE, prepareExecutor, apply);
        }, initialStage);
    }

    protected SimpleReloadInstance(Executor prepareExecutor, Executor applyExecutor, ResourceManager manager, List<PreparableReloadListener> reloaders, SimpleReloadInstance.StateFactory<S> factory, CompletableFuture<Unit> initialStage) {
        this.listenerCount = reloaders.size();
        this.startedTaskCounter.incrementAndGet();
        initialStage.thenRun(this.doneTaskCounter::incrementAndGet);
        List<CompletableFuture<S>> list = Lists.newArrayList();
        CompletableFuture<?> completableFuture = initialStage;
        this.preparingListeners = Sets.newHashSet(reloaders);

        for(final PreparableReloadListener preparableReloadListener : reloaders) {
            final CompletableFuture<?> completableFuture2 = completableFuture;
            CompletableFuture<S> completableFuture3 = factory.create(new PreparableReloadListener.PreparationBarrier() {
                @Override
                public <T> CompletableFuture<T> wait(T preparedObject) {
                    applyExecutor.execute(() -> {
                        SimpleReloadInstance.this.preparingListeners.remove(preparableReloadListener);
                        if (SimpleReloadInstance.this.preparingListeners.isEmpty()) {
                            SimpleReloadInstance.this.allPreparations.complete(Unit.INSTANCE);
                        }

                    });
                    return SimpleReloadInstance.this.allPreparations.thenCombine(completableFuture2, (unit, object2) -> {
                        return preparedObject;
                    });
                }
            }, manager, preparableReloadListener, (preparation) -> {
                this.startedTaskCounter.incrementAndGet();
                prepareExecutor.execute(() -> {
                    preparation.run();
                    this.doneTaskCounter.incrementAndGet();
                });
            }, (application) -> {
                ++this.startedReloads;
                applyExecutor.execute(() -> {
                    application.run();
                    ++this.finishedReloads;
                });
            });
            list.add(completableFuture3);
            completableFuture = completableFuture3;
        }

        this.allDone = Util.sequenceFailFast(list);
    }

    @Override
    public CompletableFuture<?> done() {
        return this.allDone;
    }

    @Override
    public float getActualProgress() {
        int i = this.listenerCount - this.preparingListeners.size();
        float f = (float)(this.doneTaskCounter.get() * 2 + this.finishedReloads * 2 + i * 1);
        float g = (float)(this.startedTaskCounter.get() * 2 + this.startedReloads * 2 + this.listenerCount * 1);
        return f / g;
    }

    public static ReloadInstance create(ResourceManager manager, List<PreparableReloadListener> reloaders, Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, boolean profiled) {
        return (ReloadInstance)(profiled ? new ProfiledReloadInstance(manager, reloaders, prepareExecutor, applyExecutor, initialStage) : of(manager, reloaders, prepareExecutor, applyExecutor, initialStage));
    }

    protected interface StateFactory<S> {
        CompletableFuture<S> create(PreparableReloadListener.PreparationBarrier synchronizer, ResourceManager manager, PreparableReloadListener reloader, Executor prepareExecutor, Executor applyExecutor);
    }
}
