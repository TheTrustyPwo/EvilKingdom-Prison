package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.Hash.Strategy;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;

public record SavedTick<T>(T type, BlockPos pos, int delay, TickPriority priority) {
    private static final String TAG_ID = "i";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";
    private static final String TAG_DELAY = "t";
    private static final String TAG_PRIORITY = "p";
    public static final Strategy<SavedTick<?>> UNIQUE_TICK_HASH = new Strategy<SavedTick<?>>() {
        @Override
        public int hashCode(SavedTick<?> savedTick) {
            return 31 * savedTick.pos().hashCode() + savedTick.type().hashCode();
        }

        @Override
        public boolean equals(@Nullable SavedTick<?> savedTick, @Nullable SavedTick<?> savedTick2) {
            if (savedTick == savedTick2) {
                return true;
            } else if (savedTick != null && savedTick2 != null) {
                return savedTick.type() == savedTick2.type() && savedTick.pos().equals(savedTick2.pos());
            } else {
                return false;
            }
        }
    };

    public static <T> void loadTickList(ListTag tickList, Function<String, Optional<T>> nameToTypeFunction, ChunkPos pos, Consumer<SavedTick<T>> tickConsumer) {
        long l = pos.toLong();

        for(int i = 0; i < tickList.size(); ++i) {
            CompoundTag compoundTag = tickList.getCompound(i);
            loadTick(compoundTag, nameToTypeFunction).ifPresent((tick) -> {
                if (ChunkPos.asLong(tick.pos()) == l) {
                    tickConsumer.accept(tick);
                }

            });
        }

    }

    public static <T> Optional<SavedTick<T>> loadTick(CompoundTag nbt, Function<String, Optional<T>> nameToType) {
        return nameToType.apply(nbt.getString("i")).map((type) -> {
            BlockPos blockPos = new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
            return new SavedTick<>(type, blockPos, nbt.getInt("t"), TickPriority.byValue(nbt.getInt("p")));
        });
    }

    private static CompoundTag saveTick(String type, BlockPos pos, int delay, TickPriority priority) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("i", type);
        compoundTag.putInt("x", pos.getX());
        compoundTag.putInt("y", pos.getY());
        compoundTag.putInt("z", pos.getZ());
        compoundTag.putInt("t", delay);
        compoundTag.putInt("p", priority.getValue());
        return compoundTag;
    }

    public static <T> CompoundTag saveTick(ScheduledTick<T> orderedTick, Function<T, String> typeToNameFunction, long delay) {
        return saveTick(typeToNameFunction.apply(orderedTick.type()), orderedTick.pos(), (int)(orderedTick.triggerTick() - delay), orderedTick.priority());
    }

    public CompoundTag save(Function<T, String> typeToNameFunction) {
        return saveTick(typeToNameFunction.apply(this.type), this.pos, this.delay, this.priority);
    }

    public ScheduledTick<T> unpack(long time, long subTickOrder) {
        return new ScheduledTick<>(this.type, this.pos, time + (long)this.delay, this.priority, subTickOrder);
    }

    public static <T> SavedTick<T> probe(T type, BlockPos pos) {
        return new SavedTick<>(type, pos, 0, TickPriority.NORMAL);
    }
}
