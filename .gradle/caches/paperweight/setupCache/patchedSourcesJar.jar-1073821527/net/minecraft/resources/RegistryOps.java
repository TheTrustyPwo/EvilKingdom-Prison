package net.minecraft.resources;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;

public class RegistryOps<T> extends DelegatingOps<T> {
    private final Optional<RegistryLoader.Bound> loader;
    private final RegistryAccess registryAccess;
    private final DynamicOps<JsonElement> asJson;

    public static <T> RegistryOps<T> create(DynamicOps<T> delegate, RegistryAccess registryManager) {
        return new RegistryOps<>(delegate, registryManager, Optional.empty());
    }

    public static <T> RegistryOps<T> createAndLoad(DynamicOps<T> ops, RegistryAccess.Writable registryManager, ResourceManager resourceManager) {
        return createAndLoad(ops, registryManager, RegistryResourceAccess.forResourceManager(resourceManager));
    }

    public static <T> RegistryOps<T> createAndLoad(DynamicOps<T> ops, RegistryAccess.Writable registryManager, RegistryResourceAccess entryLoader) {
        RegistryLoader registryLoader = new RegistryLoader(entryLoader);
        RegistryOps<T> registryOps = new RegistryOps<>(ops, registryManager, Optional.of(registryLoader.bind(registryManager)));
        RegistryAccess.load(registryManager, registryOps.getAsJson(), registryLoader);
        return registryOps;
    }

    private RegistryOps(DynamicOps<T> delegate, RegistryAccess dynamicRegistryManager, Optional<RegistryLoader.Bound> loaderAccess) {
        super(delegate);
        this.loader = loaderAccess;
        this.registryAccess = dynamicRegistryManager;
        this.asJson = delegate == JsonOps.INSTANCE ? this : new RegistryOps<>(JsonOps.INSTANCE, dynamicRegistryManager, loaderAccess);
    }

    public <E> Optional<? extends Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> key) {
        return this.registryAccess.registry(key);
    }

    public Optional<RegistryLoader.Bound> registryLoader() {
        return this.loader;
    }

    public DynamicOps<JsonElement> getAsJson() {
        return this.asJson;
    }

    public static <E> MapCodec<Registry<E>> retrieveRegistry(ResourceKey<? extends Registry<? extends E>> registryRef) {
        return ExtraCodecs.retrieveContext((ops) -> {
            if (ops instanceof RegistryOps) {
                RegistryOps<?> registryOps = (RegistryOps)ops;
                return registryOps.registry(registryRef).map((registry) -> {
                    return DataResult.success(registry, registry.elementsLifecycle());
                }).orElseGet(() -> {
                    return DataResult.error("Unknown registry: " + registryRef);
                });
            } else {
                return DataResult.error("Not a registry ops");
            }
        });
    }
}
