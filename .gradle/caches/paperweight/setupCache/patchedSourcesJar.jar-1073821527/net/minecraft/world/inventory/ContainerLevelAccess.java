package net.minecraft.world.inventory;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface ContainerLevelAccess {
    ContainerLevelAccess NULL = new ContainerLevelAccess() {
        @Override
        public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> getter) {
            return Optional.empty();
        }
    };

    static ContainerLevelAccess create(Level world, BlockPos pos) {
        return new ContainerLevelAccess() {
            @Override
            public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> getter) {
                return Optional.of(getter.apply(world, pos));
            }
        };
    }

    <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> getter);

    default <T> T evaluate(BiFunction<Level, BlockPos, T> getter, T defaultValue) {
        return this.evaluate(getter).orElse(defaultValue);
    }

    default void execute(BiConsumer<Level, BlockPos> function) {
        this.evaluate((world, pos) -> {
            function.accept(world, pos);
            return Optional.empty();
        });
    }
}
