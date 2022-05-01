package net.minecraft.stats;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class StatType<T> implements Iterable<Stat<T>> {
    private final Registry<T> registry;
    private final Map<T, Stat<T>> map = new IdentityHashMap<>();
    @Nullable
    private Component displayName;

    public StatType(Registry<T> registry) {
        this.registry = registry;
    }

    public boolean contains(T key) {
        return this.map.containsKey(key);
    }

    public Stat<T> get(T key, StatFormatter formatter) {
        return this.map.computeIfAbsent(key, (value) -> {
            return new Stat<>(this, value, formatter);
        });
    }

    public Registry<T> getRegistry() {
        return this.registry;
    }

    @Override
    public Iterator<Stat<T>> iterator() {
        return this.map.values().iterator();
    }

    public Stat<T> get(T key) {
        return this.get(key, StatFormatter.DEFAULT);
    }

    public String getTranslationKey() {
        return "stat_type." + Registry.STAT_TYPE.getKey(this).toString().replace(':', '.');
    }

    public Component getDisplayName() {
        if (this.displayName == null) {
            this.displayName = new TranslatableComponent(this.getTranslationKey());
        }

        return this.displayName;
    }
}
