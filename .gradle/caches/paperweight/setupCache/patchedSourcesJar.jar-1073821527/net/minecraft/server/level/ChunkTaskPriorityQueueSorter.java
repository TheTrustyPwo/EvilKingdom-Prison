package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class ChunkTaskPriorityQueueSorter implements ChunkHolder.LevelChangeListener, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<ProcessorHandle<?>, ChunkTaskPriorityQueue<? extends Function<ProcessorHandle<Unit>, ?>>> queues;
    private final Set<ProcessorHandle<?>> sleeping;
    private final ProcessorMailbox<StrictQueue.IntRunnable> mailbox;

    public ChunkTaskPriorityQueueSorter(List<ProcessorHandle<?>> actors, Executor executor, int maxQueues) {
        this.queues = actors.stream().collect(Collectors.toMap(Function.identity(), (actor) -> {
            return new ChunkTaskPriorityQueue<>(actor.name() + "_queue", maxQueues);
        }));
        this.sleeping = Sets.newHashSet(actors);
        this.mailbox = new ProcessorMailbox<>(new StrictQueue.FixedPriorityQueue(4), executor, "sorter");
    }

    public boolean hasWork() {
        return this.mailbox.hasWork() || this.queues.values().stream().anyMatch(ChunkTaskPriorityQueue::hasWork);
    }

    public static <T> ChunkTaskPriorityQueueSorter.Message<T> message(Function<ProcessorHandle<Unit>, T> taskFunction, long pos, IntSupplier lastLevelUpdatedToProvider) {
        return new ChunkTaskPriorityQueueSorter.Message<>(taskFunction, pos, lastLevelUpdatedToProvider);
    }

    public static ChunkTaskPriorityQueueSorter.Message<Runnable> message(Runnable task, long pos, IntSupplier lastLevelUpdatedToProvider) {
        return new ChunkTaskPriorityQueueSorter.Message<>((yield) -> {
            return () -> {
                task.run();
                yield.tell(Unit.INSTANCE);
            };
        }, pos, lastLevelUpdatedToProvider);
    }

    public static ChunkTaskPriorityQueueSorter.Message<Runnable> message(ChunkHolder holder, Runnable task) {
        return message(task, holder.getPos().toLong(), holder::getQueueLevel);
    }

    public static <T> ChunkTaskPriorityQueueSorter.Message<T> message(ChunkHolder holder, Function<ProcessorHandle<Unit>, T> taskFunction) {
        return message(taskFunction, holder.getPos().toLong(), holder::getQueueLevel);
    }

    public static ChunkTaskPriorityQueueSorter.Release release(Runnable task, long pos, boolean removeTask) {
        return new ChunkTaskPriorityQueueSorter.Release(task, pos, removeTask);
    }

    public <T> ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<T>> getProcessor(ProcessorHandle<T> executor, boolean addBlocker) {
        return this.mailbox.ask((yield) -> {
            return new StrictQueue.IntRunnable(0, () -> {
                this.getQueue(executor);
                yield.tell(ProcessorHandle.of("chunk priority sorter around " + executor.name(), (task) -> {
                    this.submit(executor, task.task, task.pos, task.level, addBlocker);
                }));
            });
        }).join();
    }

    public ProcessorHandle<ChunkTaskPriorityQueueSorter.Release> getReleaseProcessor(ProcessorHandle<Runnable> executor) {
        return this.mailbox.ask((yield) -> {
            return new StrictQueue.IntRunnable(0, () -> {
                yield.tell(ProcessorHandle.of("chunk priority sorter around " + executor.name(), (message) -> {
                    this.release(executor, message.pos, message.task, message.clearQueue);
                }));
            });
        }).join();
    }

    @Override
    public void onLevelChange(ChunkPos pos, IntSupplier levelGetter, int targetLevel, IntConsumer levelSetter) {
        this.mailbox.tell(new StrictQueue.IntRunnable(0, () -> {
            int j = levelGetter.getAsInt();
            this.queues.values().forEach((queue) -> {
                queue.resortChunkTasks(j, pos, targetLevel);
            });
            levelSetter.accept(targetLevel);
        }));
    }

    private <T> void release(ProcessorHandle<T> actor, long chunkPos, Runnable callback, boolean clearTask) {
        this.mailbox.tell(new StrictQueue.IntRunnable(1, () -> {
            ChunkTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> chunkTaskPriorityQueue = this.getQueue(actor);
            chunkTaskPriorityQueue.release(chunkPos, clearTask);
            if (this.sleeping.remove(actor)) {
                this.pollTask(chunkTaskPriorityQueue, actor);
            }

            callback.run();
        }));
    }

    private <T> void submit(ProcessorHandle<T> actor, Function<ProcessorHandle<Unit>, T> task, long chunkPos, IntSupplier lastLevelUpdatedToProvider, boolean addBlocker) {
        this.mailbox.tell(new StrictQueue.IntRunnable(2, () -> {
            ChunkTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> chunkTaskPriorityQueue = this.getQueue(actor);
            int i = lastLevelUpdatedToProvider.getAsInt();
            chunkTaskPriorityQueue.submit(Optional.of(task), chunkPos, i);
            if (addBlocker) {
                chunkTaskPriorityQueue.submit(Optional.empty(), chunkPos, i);
            }

            if (this.sleeping.remove(actor)) {
                this.pollTask(chunkTaskPriorityQueue, actor);
            }

        }));
    }

    private <T> void pollTask(ChunkTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> queue, ProcessorHandle<T> actor) {
        this.mailbox.tell(new StrictQueue.IntRunnable(3, () -> {
            Stream<Either<Function<ProcessorHandle<Unit>, T>, Runnable>> stream = queue.pop();
            if (stream == null) {
                this.sleeping.add(actor);
            } else {
                CompletableFuture.allOf(stream.map((executeOrAddBlocking) -> {
                    return executeOrAddBlocking.map(actor::ask, (addBlocking) -> {
                        addBlocking.run();
                        return CompletableFuture.completedFuture(Unit.INSTANCE);
                    });
                }).toArray((i) -> {
                    return new CompletableFuture[i];
                })).thenAccept((void_) -> {
                    this.pollTask(queue, actor);
                });
            }

        }));
    }

    private <T> ChunkTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> getQueue(ProcessorHandle<T> actor) {
        ChunkTaskPriorityQueue<? extends Function<ProcessorHandle<Unit>, ?>> chunkTaskPriorityQueue = this.queues.get(actor);
        if (chunkTaskPriorityQueue == null) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("No queue for: " + actor));
        } else {
            return chunkTaskPriorityQueue;
        }
    }

    @VisibleForTesting
    public String getDebugStatus() {
        return (String)this.queues.entrySet().stream().map((entry) -> {
            return entry.getKey().name() + "=[" + (String)entry.getValue().getAcquired().stream().map((pos) -> {
                return pos + ":" + new ChunkPos(pos);
            }).collect(Collectors.joining(",")) + "]";
        }).collect(Collectors.joining(",")) + ", s=" + this.sleeping.size();
    }

    @Override
    public void close() {
        this.queues.keySet().forEach(ProcessorHandle::close);
    }

    public static final class Message<T> {
        final Function<ProcessorHandle<Unit>, T> task;
        final long pos;
        final IntSupplier level;

        Message(Function<ProcessorHandle<Unit>, T> taskFunction, long pos, IntSupplier lastLevelUpdatedToProvider) {
            this.task = taskFunction;
            this.pos = pos;
            this.level = lastLevelUpdatedToProvider;
        }
    }

    public static final class Release {
        final Runnable task;
        final long pos;
        final boolean clearQueue;

        Release(Runnable callback, long pos, boolean removeTask) {
            this.task = callback;
            this.pos = pos;
            this.clearQueue = removeTask;
        }
    }
}
