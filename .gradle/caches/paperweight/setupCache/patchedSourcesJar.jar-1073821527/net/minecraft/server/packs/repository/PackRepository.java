package net.minecraft.server.packs.repository;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

public class PackRepository implements AutoCloseable {
    private final Set<RepositorySource> sources;
    private Map<String, Pack> available = ImmutableMap.of();
    private List<Pack> selected = ImmutableList.of();
    private final Pack.PackConstructor constructor;

    public PackRepository(Pack.PackConstructor profileFactory, RepositorySource... providers) {
        this.constructor = profileFactory;
        this.sources = ImmutableSet.copyOf(providers);
    }

    public PackRepository(PackType type, RepositorySource... providers) {
        this((name, displayName, alwaysEnabled, packFactory, metadata, direction, source) -> {
            return new Pack(name, displayName, alwaysEnabled, packFactory, metadata, type, direction, source);
        }, providers);
    }

    public void reload() {
        List<String> list = this.selected.stream().map(Pack::getId).collect(ImmutableList.toImmutableList());
        this.close();
        this.available = this.discoverAvailable();
        this.selected = this.rebuildSelected(list);
    }

    private Map<String, Pack> discoverAvailable() {
        Map<String, Pack> map = Maps.newTreeMap();

        for(RepositorySource repositorySource : this.sources) {
            repositorySource.loadPacks((profile) -> {
                map.put(profile.getId(), profile);
            }, this.constructor);
        }

        return ImmutableMap.copyOf(map);
    }

    public void setSelected(Collection<String> enabled) {
        this.selected = this.rebuildSelected(enabled);
    }

    private List<Pack> rebuildSelected(Collection<String> enabledNames) {
        List<Pack> list = this.getAvailablePacks(enabledNames).collect(Collectors.toList());

        for(Pack pack : this.available.values()) {
            if (pack.isRequired() && !list.contains(pack)) {
                pack.getDefaultPosition().insert(list, pack, Functions.identity(), false);
            }
        }

        return ImmutableList.copyOf(list);
    }

    private Stream<Pack> getAvailablePacks(Collection<String> names) {
        return names.stream().map(this.available::get).filter(Objects::nonNull);
    }

    public Collection<String> getAvailableIds() {
        return this.available.keySet();
    }

    public Collection<Pack> getAvailablePacks() {
        return this.available.values();
    }

    public Collection<String> getSelectedIds() {
        return this.selected.stream().map(Pack::getId).collect(ImmutableSet.toImmutableSet());
    }

    public Collection<Pack> getSelectedPacks() {
        return this.selected;
    }

    @Nullable
    public Pack getPack(String name) {
        return this.available.get(name);
    }

    @Override
    public void close() {
        this.available.values().forEach(Pack::close);
    }

    public boolean isAvailable(String name) {
        return this.available.containsKey(name);
    }

    public List<PackResources> openAllSelected() {
        return this.selected.stream().map(Pack::open).collect(ImmutableList.toImmutableList());
    }
}
