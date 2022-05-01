package net.minecraft.world.level.timers;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.UnsignedLong;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

public class TimerQueue<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CALLBACK_DATA_TAG = "Callback";
    private static final String TIMER_NAME_TAG = "Name";
    private static final String TIMER_TRIGGER_TIME_TAG = "TriggerTime";
    private final TimerCallbacks<T> callbacksRegistry;
    private final Queue<TimerQueue.Event<T>> queue = new PriorityQueue<>(createComparator());
    private UnsignedLong sequentialId = UnsignedLong.ZERO;
    private final Table<String, Long, TimerQueue.Event<T>> events = HashBasedTable.create();

    private static <T> Comparator<TimerQueue.Event<T>> createComparator() {
        return Comparator.comparingLong((event) -> {
            return event.triggerTime;
        }).thenComparing((event) -> {
            return event.sequentialId;
        });
    }

    public TimerQueue(TimerCallbacks<T> timerCallbackSerializer, Stream<Dynamic<Tag>> stream) {
        this(timerCallbackSerializer);
        this.queue.clear();
        this.events.clear();
        this.sequentialId = UnsignedLong.ZERO;
        stream.forEach((dynamic) -> {
            if (!(dynamic.getValue() instanceof CompoundTag)) {
                LOGGER.warn("Invalid format of events: {}", (Object)dynamic);
            } else {
                this.loadEvent((CompoundTag)dynamic.getValue());
            }
        });
    }

    public TimerQueue(TimerCallbacks<T> timerCallbackSerializer) {
        this.callbacksRegistry = timerCallbackSerializer;
    }

    public void tick(T server, long time) {
        while(true) {
            TimerQueue.Event<T> event = this.queue.peek();
            if (event == null || event.triggerTime > time) {
                return;
            }

            this.queue.remove();
            this.events.remove(event.id, time);
            event.callback.handle(server, this, time);
        }
    }

    public void schedule(String name, long triggerTime, TimerCallback<T> callback) {
        if (!this.events.contains(name, triggerTime)) {
            this.sequentialId = this.sequentialId.plus(UnsignedLong.ONE);
            TimerQueue.Event<T> event = new TimerQueue.Event<>(triggerTime, this.sequentialId, name, callback);
            this.events.put(name, triggerTime, event);
            this.queue.add(event);
        }
    }

    public int remove(String name) {
        Collection<TimerQueue.Event<T>> collection = this.events.row(name).values();
        collection.forEach(this.queue::remove);
        int i = collection.size();
        collection.clear();
        return i;
    }

    public Set<String> getEventsIds() {
        return Collections.unmodifiableSet(this.events.rowKeySet());
    }

    private void loadEvent(CompoundTag nbt) {
        CompoundTag compoundTag = nbt.getCompound("Callback");
        TimerCallback<T> timerCallback = this.callbacksRegistry.deserialize(compoundTag);
        if (timerCallback != null) {
            String string = nbt.getString("Name");
            long l = nbt.getLong("TriggerTime");
            this.schedule(string, l, timerCallback);
        }

    }

    private CompoundTag storeEvent(TimerQueue.Event<T> event) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("Name", event.id);
        compoundTag.putLong("TriggerTime", event.triggerTime);
        compoundTag.put("Callback", this.callbacksRegistry.serialize(event.callback));
        return compoundTag;
    }

    public ListTag store() {
        ListTag listTag = new ListTag();
        this.queue.stream().sorted(createComparator()).map(this::storeEvent).forEach(listTag::add);
        return listTag;
    }

    public static class Event<T> {
        public final long triggerTime;
        public final UnsignedLong sequentialId;
        public final String id;
        public final TimerCallback<T> callback;

        Event(long triggerTime, UnsignedLong id, String name, TimerCallback<T> callback) {
            this.triggerTime = triggerTime;
            this.sequentialId = id;
            this.id = name;
            this.callback = callback;
        }
    }
}
