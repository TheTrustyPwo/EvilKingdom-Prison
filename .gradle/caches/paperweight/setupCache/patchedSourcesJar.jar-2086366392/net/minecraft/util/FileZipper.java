package net.minecraft.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import org.slf4j.Logger;

public class FileZipper implements Closeable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path outputFile;
    private final Path tempFile;
    private final FileSystem fs;

    public FileZipper(Path file) {
        this.outputFile = file;
        this.tempFile = file.resolveSibling(file.getFileName().toString() + "_tmp");

        try {
            this.fs = Util.ZIP_FILE_SYSTEM_PROVIDER.newFileSystem(this.tempFile, ImmutableMap.of("create", "true"));
        } catch (IOException var3) {
            throw new UncheckedIOException(var3);
        }
    }

    public void add(Path target, String content) {
        try {
            Path path = this.fs.getPath(File.separator);
            Path path2 = path.resolve(target.toString());
            Files.createDirectories(path2.getParent());
            Files.write(path2, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException var5) {
            throw new UncheckedIOException(var5);
        }
    }

    public void add(Path target, File source) {
        try {
            Path path = this.fs.getPath(File.separator);
            Path path2 = path.resolve(target.toString());
            Files.createDirectories(path2.getParent());
            Files.copy(source.toPath(), path2);
        } catch (IOException var5) {
            throw new UncheckedIOException(var5);
        }
    }

    public void add(Path source) {
        try {
            Path path = this.fs.getPath(File.separator);
            if (Files.isRegularFile(source)) {
                Path path2 = path.resolve(source.getParent().relativize(source).toString());
                Files.copy(path2, source);
            } else {
                Stream<Path> stream = Files.find(source, Integer.MAX_VALUE, (path, attributes) -> {
                    return attributes.isRegularFile();
                });

                try {
                    for(Path path3 : stream.collect(Collectors.toList())) {
                        Path path4 = path.resolve(source.relativize(path3).toString());
                        Files.createDirectories(path4.getParent());
                        Files.copy(path3, path4);
                    }
                } catch (Throwable var8) {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Throwable var7) {
                            var8.addSuppressed(var7);
                        }
                    }

                    throw var8;
                }

                if (stream != null) {
                    stream.close();
                }

            }
        } catch (IOException var9) {
            throw new UncheckedIOException(var9);
        }
    }

    @Override
    public void close() {
        try {
            this.fs.close();
            Files.move(this.tempFile, this.outputFile);
            LOGGER.info("Compressed to {}", (Object)this.outputFile);
        } catch (IOException var2) {
            throw new UncheckedIOException(var2);
        }
    }
}
