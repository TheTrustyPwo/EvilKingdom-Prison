package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;

public class FallbackResourceManager implements ResourceManager {
    static final Logger LOGGER = LogUtils.getLogger();
    protected final List<PackResources> fallbacks = Lists.newArrayList();
    private final PackType type;
    private final String namespace;

    public FallbackResourceManager(PackType type, String namespace) {
        this.type = type;
        this.namespace = namespace;
    }

    public void add(PackResources pack) {
        this.fallbacks.add(pack);
    }

    @Override
    public Set<String> getNamespaces() {
        return ImmutableSet.of(this.namespace);
    }

    @Override
    public Resource getResource(ResourceLocation id) throws IOException {
        this.validateLocation(id);
        PackResources packResources = null;
        ResourceLocation resourceLocation = getMetadataLocation(id);

        for(int i = this.fallbacks.size() - 1; i >= 0; --i) {
            PackResources packResources2 = this.fallbacks.get(i);
            if (packResources == null && packResources2.hasResource(this.type, resourceLocation)) {
                packResources = packResources2;
            }

            if (packResources2.hasResource(this.type, id)) {
                InputStream inputStream = null;
                if (packResources != null) {
                    inputStream = this.getWrappedResource(resourceLocation, packResources);
                }

                return new SimpleResource(packResources2.getName(), id, this.getWrappedResource(id, packResources2), inputStream);
            }
        }

        throw new FileNotFoundException(id.toString());
    }

    @Override
    public boolean hasResource(ResourceLocation id) {
        if (!this.isValidLocation(id)) {
            return false;
        } else {
            for(int i = this.fallbacks.size() - 1; i >= 0; --i) {
                PackResources packResources = this.fallbacks.get(i);
                if (packResources.hasResource(this.type, id)) {
                    return true;
                }
            }

            return false;
        }
    }

    protected InputStream getWrappedResource(ResourceLocation id, PackResources pack) throws IOException {
        InputStream inputStream = pack.getResource(this.type, id);
        return (InputStream)(LOGGER.isDebugEnabled() ? new FallbackResourceManager.LeakedResourceWarningInputStream(inputStream, id, pack.getName()) : inputStream);
    }

    private void validateLocation(ResourceLocation id) throws IOException {
        if (!this.isValidLocation(id)) {
            throw new IOException("Invalid relative path to resource: " + id);
        }
    }

    private boolean isValidLocation(ResourceLocation id) {
        return !id.getPath().contains("..");
    }

    @Override
    public List<Resource> getResources(ResourceLocation id) throws IOException {
        this.validateLocation(id);
        List<Resource> list = Lists.newArrayList();
        ResourceLocation resourceLocation = getMetadataLocation(id);

        for(PackResources packResources : this.fallbacks) {
            if (packResources.hasResource(this.type, id)) {
                InputStream inputStream = packResources.hasResource(this.type, resourceLocation) ? this.getWrappedResource(resourceLocation, packResources) : null;
                list.add(new SimpleResource(packResources.getName(), id, this.getWrappedResource(id, packResources), inputStream));
            }
        }

        if (list.isEmpty()) {
            throw new FileNotFoundException(id.toString());
        } else {
            return list;
        }
    }

    @Override
    public Collection<ResourceLocation> listResources(String startingPath, Predicate<String> pathPredicate) {
        List<ResourceLocation> list = Lists.newArrayList();

        for(PackResources packResources : this.fallbacks) {
            list.addAll(packResources.getResources(this.type, this.namespace, startingPath, Integer.MAX_VALUE, pathPredicate));
        }

        Collections.sort(list);
        return list;
    }

    @Override
    public Stream<PackResources> listPacks() {
        return this.fallbacks.stream();
    }

    static ResourceLocation getMetadataLocation(ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), id.getPath() + ".mcmeta");
    }

    static class LeakedResourceWarningInputStream extends FilterInputStream {
        private final String message;
        private boolean closed;

        public LeakedResourceWarningInputStream(InputStream parent, ResourceLocation id, String packName) {
            super(parent);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            (new Exception()).printStackTrace(new PrintStream(byteArrayOutputStream));
            this.message = "Leaked resource: '" + id + "' loaded from pack: '" + packName + "'\n" + byteArrayOutputStream;
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.closed = true;
        }

        @Override
        protected void finalize() throws Throwable {
            if (!this.closed) {
                FallbackResourceManager.LOGGER.warn(this.message);
            }

            super.finalize();
        }
    }
}
