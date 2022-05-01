package net.minecraft.util.thread;

import com.google.common.collect.Queues;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

public interface StrictQueue<T, F> {
    @Nullable
    F pop();

    boolean push(T message);

    boolean isEmpty();

    int size();

    public static final class FixedPriorityQueue implements StrictQueue<StrictQueue.IntRunnable, Runnable> {
        private final Queue<Runnable>[] queues;
        private final AtomicInteger size = new AtomicInteger();

        public FixedPriorityQueue(int priorityCount) {
            this.queues = new Queue[priorityCount];

            for(int i = 0; i < priorityCount; ++i) {
                this.queues[i] = Queues.newConcurrentLinkedQueue();
            }

        }

        @Nullable
        @Override
        public Runnable pop() {
            for(Queue<Runnable> queue : this.queues) {
                Runnable runnable = queue.poll();
                if (runnable != null) {
                    this.size.decrementAndGet();
                    return runnable;
                }
            }

            return null;
        }

        @Override
        public boolean push(StrictQueue.IntRunnable message) {
            int i = message.priority;
            if (i < this.queues.length && i >= 0) {
                this.queues[i].add(message);
                this.size.incrementAndGet();
                return true;
            } else {
                throw new IndexOutOfBoundsException("Priority %d not supported. Expected range [0-%d]".formatted(i, this.queues.length - 1));
            }
        }

        @Override
        public boolean isEmpty() {
            return this.size.get() == 0;
        }

        @Override
        public int size() {
            return this.size.get();
        }
    }

    public static final class IntRunnable implements Runnable {
        final int priority;
        private final Runnable task;

        public IntRunnable(int priority, Runnable runnable) {
            this.priority = priority;
            this.task = runnable;
        }

        @Override
        public void run() {
            this.task.run();
        }

        public int getPriority() {
            return this.priority;
        }
    }

    public static final class QueueStrictQueue<T> implements StrictQueue<T, T> {
        private final Queue<T> queue;

        public QueueStrictQueue(Queue<T> queue) {
            this.queue = queue;
        }

        @Nullable
        @Override
        public T pop() {
            return this.queue.poll();
        }

        @Override
        public boolean push(T message) {
            return this.queue.add(message);
        }

        @Override
        public boolean isEmpty() {
            return this.queue.isEmpty();
        }

        @Override
        public int size() {
            return this.queue.size();
        }
    }
}
