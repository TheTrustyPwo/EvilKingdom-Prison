package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class GateBehavior<E extends LivingEntity> extends Behavior<E> {
    private final Set<MemoryModuleType<?>> exitErasedMemories;
    private final GateBehavior.OrderPolicy orderPolicy;
    private final GateBehavior.RunningPolicy runningPolicy;
    private final ShufflingList<Behavior<? super E>> behaviors = new ShufflingList<>(false); // Paper  - don't use a clone

    public GateBehavior(Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState, Set<MemoryModuleType<?>> memoriesToForgetWhenStopped, GateBehavior.OrderPolicy order, GateBehavior.RunningPolicy runMode, List<Pair<Behavior<? super E>, Integer>> tasks) {
        super(requiredMemoryState);
        this.exitErasedMemories = memoriesToForgetWhenStopped;
        this.orderPolicy = order;
        this.runningPolicy = runMode;
        tasks.forEach((pair) -> {
            this.behaviors.add(pair.getFirst(), pair.getSecond());
        });
    }

    @Override
    protected boolean canStillUse(ServerLevel world, E entity, long time) {
        // Paper start - remove streams
        List<ShufflingList.WeightedEntry<Behavior<? super E>>> entries = this.behaviors.entries;
        for (int i = 0; i < entries.size(); i++) {
            ShufflingList.WeightedEntry<Behavior<? super E>> entry = entries.get(i);
            Behavior<? super E> behavior = entry.getData();
            if (behavior.getStatus() == Status.RUNNING) {
                if (behavior.canStillUse(world, entity, time)) {
                    return true;
                }
            }
        }
        return false;
        // Paper end - remove streams
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        this.orderPolicy.apply(this.behaviors);
        this.runningPolicy.apply(this.behaviors.entries, world, entity, time); // Paper - remove streams
    }

    @Override
    protected void tick(ServerLevel world, E entity, long time) {
        // Paper start - remove streams
        List<ShufflingList.WeightedEntry<Behavior<? super E>>> entries = this.behaviors.entries;
        for (int i = 0; i < entries.size(); i++) {
            ShufflingList.WeightedEntry<Behavior<? super E>> entry = entries.get(i);
            Behavior<? super E> behavior = entry.getData();
            if (behavior.getStatus() == Status.RUNNING) {
                behavior.tickOrStop(world, entity, time);
            }
        }
        // Paper end - remove streams
    }

    @Override
    protected void stop(ServerLevel world, E entity, long time) {
        // Paper start - remove streams
        List<ShufflingList.WeightedEntry<Behavior<? super E>>> entries = this.behaviors.entries;
        for (int i = 0; i < entries.size(); i++) {
            ShufflingList.WeightedEntry<Behavior<? super E>> entry = entries.get(i);
            Behavior<? super E> behavior = entry.getData();
            if (behavior.getStatus() == Status.RUNNING) {
                behavior.doStop(world, entity, time);
            }
        }
        // Paper end - remove streams
        this.exitErasedMemories.forEach(entity.getBrain()::eraseMemory);
    }

    @Override
    public String toString() {
        Set<? extends Behavior<? super E>> set = this.behaviors.stream().filter((task) -> {
            return task.getStatus() == Behavior.Status.RUNNING;
        }).collect(Collectors.toSet());
        return "(" + this.getClass().getSimpleName() + "): " + set;
    }

    public static enum OrderPolicy {
        ORDERED((shufflingList) -> {
        }),
        SHUFFLED(ShufflingList::shuffle);

        private final Consumer<ShufflingList<?>> consumer;

        private OrderPolicy(Consumer<ShufflingList<?>> listModifier) {
            this.consumer = listModifier;
        }

        public void apply(ShufflingList<?> list) {
            this.consumer.accept(list);
        }
    }

    public static enum RunningPolicy {
        RUN_ONE {
            @Override
            // Paper start - remove streams
            public <E extends LivingEntity> void apply(List<ShufflingList.WeightedEntry<Behavior<? super E>>> tasks, ServerLevel world, E entity, long time) {
                for (int i = 0; i < tasks.size(); i++) {
                    ShufflingList.WeightedEntry<Behavior<? super E>> task = tasks.get(i);
                    Behavior<? super E> behavior = task.getData();
                    if (behavior.getStatus() == Status.STOPPED && behavior.tryStart(world, entity, time)) {
                        break;
                    }
                }
                // Paper end - remove streams
            }
        },
        TRY_ALL {
            @Override
            // Paper start - remove streams
            public <E extends LivingEntity> void apply(List<ShufflingList.WeightedEntry<Behavior<? super E>>> tasks, ServerLevel world, E entity, long time) {
                for (int i = 0; i < tasks.size(); i++) {
                    ShufflingList.WeightedEntry<Behavior<? super E>> task = tasks.get(i);
                    Behavior<? super E> behavior = task.getData();
                    if (behavior.getStatus() == Status.STOPPED) {
                        behavior.tryStart(world, entity, time);
                    }
                }
                // Paper end - remove streams
            }
        };

        public abstract <E extends LivingEntity> void apply(List<ShufflingList.WeightedEntry<Behavior<? super E>>> tasks, ServerLevel world, E entity, long time); // Paper - remove streams
    }
}
