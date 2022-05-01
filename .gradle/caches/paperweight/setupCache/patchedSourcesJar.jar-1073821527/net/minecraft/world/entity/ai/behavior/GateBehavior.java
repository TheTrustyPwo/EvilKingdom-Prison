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
    private final ShufflingList<Behavior<? super E>> behaviors = new ShufflingList<>();

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
        return this.behaviors.stream().filter((task) -> {
            return task.getStatus() == Behavior.Status.RUNNING;
        }).anyMatch((task) -> {
            return task.canStillUse(world, entity, time);
        });
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        this.orderPolicy.apply(this.behaviors);
        this.runningPolicy.apply(this.behaviors.stream(), world, entity, time);
    }

    @Override
    protected void tick(ServerLevel world, E entity, long time) {
        this.behaviors.stream().filter((task) -> {
            return task.getStatus() == Behavior.Status.RUNNING;
        }).forEach((task) -> {
            task.tickOrStop(world, entity, time);
        });
    }

    @Override
    protected void stop(ServerLevel world, E entity, long time) {
        this.behaviors.stream().filter((task) -> {
            return task.getStatus() == Behavior.Status.RUNNING;
        }).forEach((task) -> {
            task.doStop(world, entity, time);
        });
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
            public <E extends LivingEntity> void apply(Stream<Behavior<? super E>> tasks, ServerLevel world, E entity, long time) {
                tasks.filter((task) -> {
                    return task.getStatus() == Behavior.Status.STOPPED;
                }).filter((task) -> {
                    return task.tryStart(world, entity, time);
                }).findFirst();
            }
        },
        TRY_ALL {
            @Override
            public <E extends LivingEntity> void apply(Stream<Behavior<? super E>> tasks, ServerLevel world, E entity, long time) {
                tasks.filter((task) -> {
                    return task.getStatus() == Behavior.Status.STOPPED;
                }).forEach((task) -> {
                    task.tryStart(world, entity, time);
                });
            }
        };

        public abstract <E extends LivingEntity> void apply(Stream<Behavior<? super E>> tasks, ServerLevel world, E entity, long time);
    }
}
