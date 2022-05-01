package net.minecraft.world.entity.ai.gossip;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.SerializableUUID;
import net.minecraft.util.VisibleForDebug;

public class GossipContainer {
    public static final int DISCARD_THRESHOLD = 2;
    private final Map<UUID, GossipContainer.EntityGossips> gossips = Maps.newHashMap();

    @VisibleForDebug
    public Map<UUID, Object2IntMap<GossipType>> getGossipEntries() {
        Map<UUID, Object2IntMap<GossipType>> map = Maps.newHashMap();
        this.gossips.keySet().forEach((uuid) -> {
            GossipContainer.EntityGossips entityGossips = this.gossips.get(uuid);
            map.put(uuid, entityGossips.entries);
        });
        return map;
    }

    public void decay() {
        Iterator<GossipContainer.EntityGossips> iterator = this.gossips.values().iterator();

        while(iterator.hasNext()) {
            GossipContainer.EntityGossips entityGossips = iterator.next();
            entityGossips.decay();
            if (entityGossips.isEmpty()) {
                iterator.remove();
            }
        }

    }

    private Stream<GossipContainer.GossipEntry> unpack() {
        return this.gossips.entrySet().stream().flatMap((entry) -> {
            return entry.getValue().unpack(entry.getKey());
        });
    }

    private Collection<GossipContainer.GossipEntry> selectGossipsForTransfer(Random random, int count) {
        List<GossipContainer.GossipEntry> list = this.unpack().collect(Collectors.toList());
        if (list.isEmpty()) {
            return Collections.emptyList();
        } else {
            int[] is = new int[list.size()];
            int i = 0;

            for(int j = 0; j < list.size(); ++j) {
                GossipContainer.GossipEntry gossipEntry = list.get(j);
                i += Math.abs(gossipEntry.weightedValue());
                is[j] = i - 1;
            }

            Set<GossipContainer.GossipEntry> set = Sets.newIdentityHashSet();

            for(int k = 0; k < count; ++k) {
                int l = random.nextInt(i);
                int m = Arrays.binarySearch(is, l);
                set.add(list.get(m < 0 ? -m - 1 : m));
            }

            return set;
        }
    }

    private GossipContainer.EntityGossips getOrCreate(UUID target) {
        return this.gossips.computeIfAbsent(target, (uuid) -> {
            return new GossipContainer.EntityGossips();
        });
    }

    public void transferFrom(GossipContainer from, Random random, int count) {
        Collection<GossipContainer.GossipEntry> collection = from.selectGossipsForTransfer(random, count);
        collection.forEach((gossip) -> {
            int i = gossip.value - gossip.type.decayPerTransfer;
            if (i >= 2) {
                this.getOrCreate(gossip.target).entries.mergeInt(gossip.type, i, GossipContainer::mergeValuesForTransfer);
            }

        });
    }

    public int getReputation(UUID target, Predicate<GossipType> gossipTypeFilter) {
        GossipContainer.EntityGossips entityGossips = this.gossips.get(target);
        return entityGossips != null ? entityGossips.weightedValue(gossipTypeFilter) : 0;
    }

    public long getCountForType(GossipType type, DoublePredicate predicate) {
        return this.gossips.values().stream().filter((entityGossips) -> {
            return predicate.test((double)(entityGossips.entries.getOrDefault(type, 0) * type.weight));
        }).count();
    }

    public void add(UUID target, GossipType type, int value) {
        GossipContainer.EntityGossips entityGossips = this.getOrCreate(target);
        entityGossips.entries.mergeInt(type, value, (left, right) -> {
            return this.mergeValuesForAddition(type, left, right);
        });
        entityGossips.makeSureValueIsntTooLowOrTooHigh(type);
        if (entityGossips.isEmpty()) {
            this.gossips.remove(target);
        }

    }

    public void remove(UUID target, GossipType type, int value) {
        this.add(target, type, -value);
    }

    public void remove(UUID target, GossipType type) {
        GossipContainer.EntityGossips entityGossips = this.gossips.get(target);
        if (entityGossips != null) {
            entityGossips.remove(type);
            if (entityGossips.isEmpty()) {
                this.gossips.remove(target);
            }
        }

    }

    public void remove(GossipType type) {
        Iterator<GossipContainer.EntityGossips> iterator = this.gossips.values().iterator();

        while(iterator.hasNext()) {
            GossipContainer.EntityGossips entityGossips = iterator.next();
            entityGossips.remove(type);
            if (entityGossips.isEmpty()) {
                iterator.remove();
            }
        }

    }

    public <T> Dynamic<T> store(DynamicOps<T> dynamicOps) {
        return new Dynamic<>(dynamicOps, dynamicOps.createList(this.unpack().map((gossipEntry) -> {
            return gossipEntry.store(dynamicOps);
        }).map(Dynamic::getValue)));
    }

    public void update(Dynamic<?> dynamic) {
        dynamic.asStream().map(GossipContainer.GossipEntry::load).flatMap((dataResult) -> {
            return dataResult.result().stream();
        }).forEach((gossipEntry) -> {
            this.getOrCreate(gossipEntry.target).entries.put(gossipEntry.type, gossipEntry.value);
        });
    }

    private static int mergeValuesForTransfer(int left, int right) {
        return Math.max(left, right);
    }

    private int mergeValuesForAddition(GossipType type, int left, int right) {
        int i = left + right;
        return i > type.max ? Math.max(type.max, left) : i;
    }

    public static class EntityGossips {
        final Object2IntMap<GossipType> entries = new Object2IntOpenHashMap<>();

        public int weightedValue(Predicate<GossipType> gossipTypeFilter) {
            return this.entries.object2IntEntrySet().stream().filter((entry) -> {
                return gossipTypeFilter.test(entry.getKey());
            }).mapToInt((entry) -> {
                return entry.getIntValue() * (entry.getKey()).weight;
            }).sum();
        }

        public Stream<GossipContainer.GossipEntry> unpack(UUID target) {
            return this.entries.object2IntEntrySet().stream().map((entry) -> {
                return new GossipContainer.GossipEntry(target, entry.getKey(), entry.getIntValue());
            });
        }

        public void decay() {
            ObjectIterator<Entry<GossipType>> objectIterator = this.entries.object2IntEntrySet().iterator();

            while(objectIterator.hasNext()) {
                Entry<GossipType> entry = objectIterator.next();
                int i = entry.getIntValue() - (entry.getKey()).decayPerDay;
                if (i < 2) {
                    objectIterator.remove();
                } else {
                    entry.setValue(i);
                }
            }

        }

        public boolean isEmpty() {
            return this.entries.isEmpty();
        }

        public void makeSureValueIsntTooLowOrTooHigh(GossipType gossipType) {
            int i = this.entries.getInt(gossipType);
            if (i > gossipType.max) {
                this.entries.put(gossipType, gossipType.max);
            }

            if (i < 2) {
                this.remove(gossipType);
            }

        }

        public void remove(GossipType gossipType) {
            this.entries.removeInt(gossipType);
        }
    }

    static class GossipEntry {
        public static final String TAG_TARGET = "Target";
        public static final String TAG_TYPE = "Type";
        public static final String TAG_VALUE = "Value";
        public final UUID target;
        public final GossipType type;
        public final int value;

        public GossipEntry(UUID target, GossipType type, int value) {
            this.target = target;
            this.type = type;
            this.value = value;
        }

        public int weightedValue() {
            return this.value * this.type.weight;
        }

        @Override
        public String toString() {
            return "GossipEntry{target=" + this.target + ", type=" + this.type + ", value=" + this.value + "}";
        }

        public <T> Dynamic<T> store(DynamicOps<T> dynamicOps) {
            return new Dynamic<>(dynamicOps, dynamicOps.createMap(ImmutableMap.of(dynamicOps.createString("Target"), SerializableUUID.CODEC.encodeStart(dynamicOps, this.target).result().orElseThrow(RuntimeException::new), dynamicOps.createString("Type"), dynamicOps.createString(this.type.id), dynamicOps.createString("Value"), dynamicOps.createInt(this.value))));
        }

        public static DataResult<GossipContainer.GossipEntry> load(Dynamic<?> dynamic) {
            return DataResult.unbox(DataResult.instance().group(dynamic.get("Target").read(SerializableUUID.CODEC), dynamic.get("Type").asString().map(GossipType::byId), dynamic.get("Value").asNumber().map(Number::intValue)).apply(DataResult.instance(), GossipContainer.GossipEntry::new));
        }
    }
}
