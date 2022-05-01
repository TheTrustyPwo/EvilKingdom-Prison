package net.minecraft.data;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class HashCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path path;
    private final Path cachePath;
    private int hits;
    private final Map<Path, String> oldCache = Maps.newHashMap();
    private final Map<Path, String> newCache = Maps.newHashMap();
    private final Set<Path> keep = Sets.newHashSet();

    public HashCache(Path root, String name) throws IOException {
        this.path = root;
        Path path = root.resolve(".cache");
        Files.createDirectories(path);
        this.cachePath = path.resolve(name);
        this.walkOutputFiles().forEach((pathx) -> {
            this.oldCache.put(pathx, "");
        });
        if (Files.isReadable(this.cachePath)) {
            IOUtils.readLines(Files.newInputStream(this.cachePath), Charsets.UTF_8).forEach((string) -> {
                int i = string.indexOf(32);
                this.oldCache.put(root.resolve(string.substring(i + 1)), string.substring(0, i));
            });
        }

    }

    public void purgeStaleAndWrite() throws IOException {
        this.removeStale();

        Writer writer;
        try {
            writer = Files.newBufferedWriter(this.cachePath);
        } catch (IOException var3) {
            LOGGER.warn("Unable write cachefile {}: {}", this.cachePath, var3.toString());
            return;
        }

        IOUtils.writeLines(this.newCache.entrySet().stream().map((entry) -> {
            return (String)entry.getValue() + " " + this.path.relativize(entry.getKey());
        }).collect(Collectors.toList()), System.lineSeparator(), writer);
        writer.close();
        LOGGER.debug("Caching: cache hits: {}, created: {} removed: {}", this.hits, this.newCache.size() - this.hits, this.oldCache.size());
    }

    @Nullable
    public String getHash(Path path) {
        return this.oldCache.get(path);
    }

    public void putNew(Path path, String sha1) {
        this.newCache.put(path, sha1);
        if (Objects.equals(this.oldCache.remove(path), sha1)) {
            ++this.hits;
        }

    }

    public boolean had(Path path) {
        return this.oldCache.containsKey(path);
    }

    public void keep(Path path) {
        this.keep.add(path);
    }

    private void removeStale() throws IOException {
        this.walkOutputFiles().forEach((path) -> {
            if (this.had(path) && !this.keep.contains(path)) {
                try {
                    Files.delete(path);
                } catch (IOException var3) {
                    LOGGER.debug("Unable to delete: {} ({})", path, var3.toString());
                }
            }

        });
    }

    private Stream<Path> walkOutputFiles() throws IOException {
        return Files.walk(this.path).filter((path) -> {
            return !Objects.equals(this.cachePath, path) && !Files.isDirectory(path);
        });
    }
}
