package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;

public interface ResourceManager extends ResourceProvider {
    Set<String> getNamespaces();

    boolean hasResource(ResourceLocation id);

    List<Resource> getResources(ResourceLocation id) throws IOException;

    Collection<ResourceLocation> listResources(String startingPath, Predicate<String> pathPredicate);

    Stream<PackResources> listPacks();

    public static enum Empty implements ResourceManager {
        INSTANCE;

        @Override
        public Set<String> getNamespaces() {
            return ImmutableSet.of();
        }

        @Override
        public Resource getResource(ResourceLocation id) throws IOException {
            throw new FileNotFoundException(id.toString());
        }

        @Override
        public boolean hasResource(ResourceLocation id) {
            return false;
        }

        @Override
        public List<Resource> getResources(ResourceLocation id) {
            return ImmutableList.of();
        }

        @Override
        public Collection<ResourceLocation> listResources(String startingPath, Predicate<String> pathPredicate) {
            return ImmutableSet.of();
        }

        @Override
        public Stream<PackResources> listPacks() {
            return Stream.of();
        }
    }
}
