package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Unit;
import org.slf4j.Logger;

public class ReloadableResourceManager implements ResourceManager, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private CloseableResourceManager resources;
    private final List<PreparableReloadListener> listeners = Lists.newArrayList();
    private final PackType type;

    public ReloadableResourceManager(PackType type) {
        this.type = type;
        this.resources = new MultiPackResourceManager(type, List.of());
    }

    @Override
    public void close() {
        this.resources.close();
    }

    public void registerReloadListener(PreparableReloadListener reloader) {
        this.listeners.add(reloader);
    }

    public ReloadInstance createReload(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<PackResources> packs) {
        LOGGER.info("Reloading ResourceManager: {}", LogUtils.defer(() -> {
            return packs.stream().map(PackResources::getName).collect(Collectors.joining(", "));
        }));
        this.resources.close();
        this.resources = new MultiPackResourceManager(this.type, packs);
        return SimpleReloadInstance.create(this.resources, this.listeners, prepareExecutor, applyExecutor, initialStage, LOGGER.isDebugEnabled());
    }

    @Override
    public Resource getResource(ResourceLocation id) throws IOException {
        return this.resources.getResource(id);
    }

    @Override
    public Set<String> getNamespaces() {
        return this.resources.getNamespaces();
    }

    @Override
    public boolean hasResource(ResourceLocation id) {
        return this.resources.hasResource(id);
    }

    @Override
    public List<Resource> getResources(ResourceLocation id) throws IOException {
        return this.resources.getResources(id);
    }

    @Override
    public Collection<ResourceLocation> listResources(String startingPath, Predicate<String> pathPredicate) {
        return this.resources.listResources(startingPath, pathPredicate);
    }

    @Override
    public Stream<PackResources> listPacks() {
        return this.resources.listPacks();
    }
}
