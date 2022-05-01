package net.minecraft.server.packs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;

public interface PackResources extends AutoCloseable {
    String METADATA_EXTENSION = ".mcmeta";
    String PACK_META = "pack.mcmeta";

    @Nullable
    InputStream getRootResource(String fileName) throws IOException;

    InputStream getResource(PackType type, ResourceLocation id) throws IOException;

    Collection<ResourceLocation> getResources(PackType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter);

    boolean hasResource(PackType type, ResourceLocation id);

    Set<String> getNamespaces(PackType type);

    @Nullable
    <T> T getMetadataSection(MetadataSectionSerializer<T> metaReader) throws IOException;

    String getName();

    @Override
    void close();
}
