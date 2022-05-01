package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

public class MultiPackResourceManager implements CloseableResourceManager {
    private final Map<String, FallbackResourceManager> namespacedManagers;
    private final List<PackResources> packs;

    public MultiPackResourceManager(PackType type, List<PackResources> packs) {
        this.packs = List.copyOf(packs);
        Map<String, FallbackResourceManager> map = new HashMap<>();

        for(PackResources packResources : packs) {
            for(String string : packResources.getNamespaces(type)) {
                map.computeIfAbsent(string, (namespace) -> {
                    return new FallbackResourceManager(type, namespace);
                }).add(packResources);
            }
        }

        this.namespacedManagers = map;
    }

    @Override
    public Set<String> getNamespaces() {
        return this.namespacedManagers.keySet();
    }

    @Override
    public Resource getResource(ResourceLocation id) throws IOException {
        ResourceManager resourceManager = this.namespacedManagers.get(id.getNamespace());
        if (resourceManager != null) {
            return resourceManager.getResource(id);
        } else {
            throw new FileNotFoundException(id.toString());
        }
    }

    @Override
    public boolean hasResource(ResourceLocation id) {
        ResourceManager resourceManager = this.namespacedManagers.get(id.getNamespace());
        return resourceManager != null ? resourceManager.hasResource(id) : false;
    }

    @Override
    public List<Resource> getResources(ResourceLocation id) throws IOException {
        ResourceManager resourceManager = this.namespacedManagers.get(id.getNamespace());
        if (resourceManager != null) {
            return resourceManager.getResources(id);
        } else {
            throw new FileNotFoundException(id.toString());
        }
    }

    @Override
    public Collection<ResourceLocation> listResources(String startingPath, Predicate<String> pathPredicate) {
        Set<ResourceLocation> set = Sets.newHashSet();

        for(FallbackResourceManager fallbackResourceManager : this.namespacedManagers.values()) {
            set.addAll(fallbackResourceManager.listResources(startingPath, pathPredicate));
        }

        List<ResourceLocation> list = Lists.newArrayList(set);
        Collections.sort(list);
        return list;
    }

    @Override
    public Stream<PackResources> listPacks() {
        return this.packs.stream();
    }

    @Override
    public void close() {
        this.packs.forEach(PackResources::close);
    }
}
