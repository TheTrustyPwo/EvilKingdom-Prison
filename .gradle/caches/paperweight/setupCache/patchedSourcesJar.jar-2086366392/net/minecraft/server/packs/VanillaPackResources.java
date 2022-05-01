package net.minecraft.server.packs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.slf4j.Logger;

public class VanillaPackResources implements PackResources, ResourceProvider {
    @Nullable
    public static Path generatedDir;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Class<?> clientObject;
    private static final Map<PackType, Path> ROOT_DIR_BY_TYPE = Util.make(() -> {
        synchronized(VanillaPackResources.class) {
            Builder<PackType, Path> builder = ImmutableMap.builder();

            for(PackType packType : PackType.values()) {
                String string = "/" + packType.getDirectory() + "/.mcassetsroot";
                URL uRL = VanillaPackResources.class.getResource(string);
                if (uRL == null) {
                    LOGGER.error("File {} does not exist in classpath", (Object)string);
                } else {
                    try {
                        URI uRI = uRL.toURI();
                        String string2 = uRI.getScheme();
                        if (!"jar".equals(string2) && !"file".equals(string2)) {
                            LOGGER.warn("Assets URL '{}' uses unexpected schema", (Object)uRI);
                        }

                        Path path = safeGetPath(uRI);
                        builder.put(packType, path.getParent());
                    } catch (Exception var12) {
                        LOGGER.error("Couldn't resolve path to vanilla assets", (Throwable)var12);
                    }
                }
            }

            return builder.build();
        }
    });
    public final PackMetadataSection packMetadata;
    public final Set<String> namespaces;

    private static Path safeGetPath(URI uri) throws IOException {
        try {
            return Paths.get(uri);
        } catch (FileSystemNotFoundException var3) {
        } catch (Throwable var4) {
            LOGGER.warn("Unable to get path for: {}", uri, var4);
        }

        try {
            FileSystems.newFileSystem(uri, Collections.emptyMap());
        } catch (FileSystemAlreadyExistsException var2) {
        }

        return Paths.get(uri);
    }

    public VanillaPackResources(PackMetadataSection metadata, String... namespaces) {
        this.packMetadata = metadata;
        this.namespaces = ImmutableSet.copyOf(namespaces);
    }

    @Override
    public InputStream getRootResource(String fileName) throws IOException {
        if (!fileName.contains("/") && !fileName.contains("\\")) {
            if (generatedDir != null) {
                Path path = generatedDir.resolve(fileName);
                if (Files.exists(path)) {
                    return Files.newInputStream(path);
                }
            }

            return this.getResourceAsStream(fileName);
        } else {
            throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
        }
    }

    @Override
    public InputStream getResource(PackType type, ResourceLocation id) throws IOException {
        InputStream inputStream = this.getResourceAsStream(type, id);
        if (inputStream != null) {
            return inputStream;
        } else {
            throw new FileNotFoundException(id.getPath());
        }
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter) {
        Set<ResourceLocation> set = Sets.newHashSet();
        if (generatedDir != null) {
            try {
                getResources(set, maxDepth, namespace, generatedDir.resolve(type.getDirectory()), prefix, pathFilter);
            } catch (IOException var13) {
            }

            if (type == PackType.CLIENT_RESOURCES) {
                Enumeration<URL> enumeration = null;

                try {
                    enumeration = clientObject.getClassLoader().getResources(type.getDirectory() + "/");
                } catch (IOException var12) {
                }

                while(enumeration != null && enumeration.hasMoreElements()) {
                    try {
                        URI uRI = enumeration.nextElement().toURI();
                        if ("file".equals(uRI.getScheme())) {
                            getResources(set, maxDepth, namespace, Paths.get(uRI), prefix, pathFilter);
                        }
                    } catch (IOException | URISyntaxException var11) {
                    }
                }
            }
        }

        try {
            Path path = ROOT_DIR_BY_TYPE.get(type);
            if (path != null) {
                getResources(set, maxDepth, namespace, path, prefix, pathFilter);
            } else {
                LOGGER.error("Can't access assets root for type: {}", (Object)type);
            }
        } catch (NoSuchFileException | FileNotFoundException var9) {
        } catch (IOException var10) {
            LOGGER.error("Couldn't get a list of all vanilla resources", (Throwable)var10);
        }

        return set;
    }

    private static void getResources(Collection<ResourceLocation> results, int maxDepth, String namespace, Path root, String prefix, Predicate<String> pathFilter) throws IOException {
        Path path = root.resolve(namespace);
        Stream<Path> stream = Files.walk(path.resolve(prefix), maxDepth);

        try {
            stream.filter((pathx) -> {
                return !pathx.endsWith(".mcmeta") && Files.isRegularFile(pathx) && pathFilter.test(pathx.getFileName().toString());
            }).map((pathx) -> {
                return new ResourceLocation(namespace, path.relativize(pathx).toString().replaceAll("\\\\", "/"));
            }).forEach(results::add);
        } catch (Throwable var11) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable var10) {
                    var11.addSuppressed(var10);
                }
            }

            throw var11;
        }

        if (stream != null) {
            stream.close();
        }

    }

    @Nullable
    protected InputStream getResourceAsStream(PackType type, ResourceLocation id) {
        String string = createPath(type, id);
        if (generatedDir != null) {
            Path path = generatedDir.resolve(type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath());
            if (Files.exists(path)) {
                try {
                    return Files.newInputStream(path);
                } catch (IOException var7) {
                }
            }
        }

        try {
            URL uRL = VanillaPackResources.class.getResource(string);
            return isResourceUrlValid(string, uRL) ? uRL.openStream() : null;
        } catch (IOException var6) {
            return VanillaPackResources.class.getResourceAsStream(string);
        }
    }

    private static String createPath(PackType type, ResourceLocation id) {
        return "/" + type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath();
    }

    private static boolean isResourceUrlValid(String fileName, @Nullable URL url) throws IOException {
        return url != null && (url.getProtocol().equals("jar") || FolderPackResources.validatePath(new File(url.getFile()), fileName));
    }

    @Nullable
    protected InputStream getResourceAsStream(String path) {
        return VanillaPackResources.class.getResourceAsStream("/" + path);
    }

    @Override
    public boolean hasResource(PackType type, ResourceLocation id) {
        String string = createPath(type, id);
        if (generatedDir != null) {
            Path path = generatedDir.resolve(type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath());
            if (Files.exists(path)) {
                return true;
            }
        }

        try {
            URL uRL = VanillaPackResources.class.getResource(string);
            return isResourceUrlValid(string, uRL);
        } catch (IOException var5) {
            return false;
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return this.namespaces;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> metaReader) throws IOException {
        try {
            InputStream inputStream = this.getRootResource("pack.mcmeta");

            Object var4;
            label59: {
                try {
                    if (inputStream != null) {
                        T object = AbstractPackResources.getMetadataFromStream(metaReader, inputStream);
                        if (object != null) {
                            var4 = object;
                            break label59;
                        }
                    }
                } catch (Throwable var6) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable var5) {
                            var6.addSuppressed(var5);
                        }
                    }

                    throw var6;
                }

                if (inputStream != null) {
                    inputStream.close();
                }

                return (T)(metaReader == PackMetadataSection.SERIALIZER ? this.packMetadata : null);
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return (T)var4;
        } catch (FileNotFoundException | RuntimeException var7) {
            return (T)(metaReader == PackMetadataSection.SERIALIZER ? this.packMetadata : null);
        }
    }

    @Override
    public String getName() {
        return "Default";
    }

    @Override
    public void close() {
    }

    @Override
    public Resource getResource(ResourceLocation id) throws IOException {
        return new Resource() {
            @Nullable
            InputStream inputStream;

            @Override
            public void close() throws IOException {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }

            }

            @Override
            public ResourceLocation getLocation() {
                return id;
            }

            @Override
            public InputStream getInputStream() {
                try {
                    this.inputStream = VanillaPackResources.this.getResource(PackType.CLIENT_RESOURCES, id);
                } catch (IOException var2) {
                    throw new UncheckedIOException("Could not get client resource from vanilla pack", var2);
                }

                return this.inputStream;
            }

            @Override
            public boolean hasMetadata() {
                return false;
            }

            @Nullable
            @Override
            public <T> T getMetadata(MetadataSectionSerializer<T> metaReader) {
                return (T)null;
            }

            @Override
            public String getSourceName() {
                return id.toString();
            }
        };
    }
}
