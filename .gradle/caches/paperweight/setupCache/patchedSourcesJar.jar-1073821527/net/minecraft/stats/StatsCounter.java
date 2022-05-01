package net.minecraft.stats;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.entity.player.Player;

public class StatsCounter {
    protected final Object2IntMap<Stat<?>> stats = Object2IntMaps.synchronize(new Object2IntOpenHashMap<>());

    public StatsCounter() {
        this.stats.defaultReturnValue(0);
    }

    public void increment(Player player, Stat<?> stat, int value) {
        int i = (int)Math.min((long)this.getValue(stat) + (long)value, 2147483647L);
        this.setValue(player, stat, i);
    }

    public void setValue(Player player, Stat<?> stat, int value) {
        this.stats.put(stat, value);
    }

    public <T> int getValue(StatType<T> type, T stat) {
        return type.contains(stat) ? this.getValue(type.get(stat)) : 0;
    }

    public int getValue(Stat<?> stat) {
        return this.stats.getInt(stat);
    }
}
