package net.minecraft.tags;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public class TagManager implements PreparableReloadListener {
    private static final Map<ResourceKey<? extends Registry<?>>, String> CUSTOM_REGISTRY_DIRECTORIES = Map.of(Registry.BLOCK_REGISTRY, "tags/blocks", Registry.ENTITY_TYPE_REGISTRY, "tags/entity_types", Registry.FLUID_REGISTRY, "tags/fluids", Registry.GAME_EVENT_REGISTRY, "tags/game_events", Registry.ITEM_REGISTRY, "tags/items");
    private final RegistryAccess registryAccess;
    private List<TagManager.LoadResult<?>> results = List.of();

    public TagManager(RegistryAccess registryManager) {
        this.registryAccess = registryManager;
    }

    public List<TagManager.LoadResult<?>> getResult() {
        return this.results;
    }

    public static String getTagDir(ResourceKey<? extends Registry<?>> registry) {
        String string = CUSTOM_REGISTRY_DIRECTORIES.get(registry);
        return string != null ? string : "tags/" + registry.location().getPath();
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier synchronizer, ResourceManager manager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        List<? extends CompletableFuture<? extends TagManager.LoadResult<?>>> list = this.registryAccess.registries().map((registryEntry) -> {
            return this.createLoader(manager, prepareExecutor, registryEntry);
        }).toList();
        return CompletableFuture.allOf(list.toArray((i) -> {
            return new CompletableFuture[i];
        })).thenCompose(synchronizer::wait).thenAcceptAsync((void_) -> {
            this.results = list.stream().map(CompletableFuture::join).collect(Collectors.toUnmodifiableList());
        }, applyExecutor);
    }

    private <T> CompletableFuture<TagManager.LoadResult<T>> createLoader(ResourceManager resourceManager, Executor prepareExecutor, RegistryAccess.RegistryEntry<T> requirement) {
        ResourceKey<? extends Registry<T>> resourceKey = requirement.key();
        Registry<T> registry = requirement.value();
        TagLoader<Holder<T>> tagLoader = new TagLoader<>((resourceLocation) -> {
            return registry.getHolder(ResourceKey.create(resourceKey, resourceLocation));
        }, getTagDir(resourceKey));
        return CompletableFuture.supplyAsync(() -> {
            return new TagManager.LoadResult<>(resourceKey, tagLoader.loadAndBuild(resourceManager));
        }, prepareExecutor);
    }

    public static record LoadResult<T>(ResourceKey<? extends Registry<T>> key, Map<ResourceLocation, Tag<Holder<T>>> tags) {
    }
}
