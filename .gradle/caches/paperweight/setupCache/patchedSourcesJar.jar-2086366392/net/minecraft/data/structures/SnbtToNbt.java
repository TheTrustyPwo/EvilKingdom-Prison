package net.minecraft.data.structures;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtToNbt implements DataProvider {
    @Nullable
    private static final Path DUMP_SNBT_TO = null;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DataGenerator generator;
    private final List<SnbtToNbt.Filter> filters = Lists.newArrayList();

    public SnbtToNbt(DataGenerator generator) {
        this.generator = generator;
    }

    public SnbtToNbt addFilter(SnbtToNbt.Filter tweaker) {
        this.filters.add(tweaker);
        return this;
    }

    private CompoundTag applyFilters(String key, CompoundTag compound) {
        CompoundTag compoundTag = compound;

        for(SnbtToNbt.Filter filter : this.filters) {
            compoundTag = filter.apply(key, compoundTag);
        }

        return compoundTag;
    }

    @Override
    public void run(HashCache cache) throws IOException {
        Path path = this.generator.getOutputFolder();
        List<CompletableFuture<SnbtToNbt.TaskResult>> list = Lists.newArrayList();

        for(Path path2 : this.generator.getInputFolders()) {
            Files.walk(path2).filter((pathx) -> {
                return pathx.toString().endsWith(".snbt");
            }).forEach((path2x) -> {
                list.add(CompletableFuture.supplyAsync(() -> {
                    return this.readStructure(path2x, this.getName(path2, path2x));
                }, Util.backgroundExecutor()));
            });
        }

        boolean bl = false;

        for(CompletableFuture<SnbtToNbt.TaskResult> completableFuture : list) {
            try {
                this.storeStructureIfChanged(cache, completableFuture.get(), path);
            } catch (Exception var8) {
                LOGGER.error("Failed to process structure", (Throwable)var8);
                bl = true;
            }
        }

        if (bl) {
            throw new IllegalStateException("Failed to convert all structures, aborting");
        }
    }

    @Override
    public String getName() {
        return "SNBT -> NBT";
    }

    private String getName(Path root, Path file) {
        String string = root.relativize(file).toString().replaceAll("\\\\", "/");
        return string.substring(0, string.length() - ".snbt".length());
    }

    private SnbtToNbt.TaskResult readStructure(Path path, String name) {
        try {
            BufferedReader bufferedReader = Files.newBufferedReader(path);

            SnbtToNbt.TaskResult var10;
            try {
                String string = IOUtils.toString((Reader)bufferedReader);
                CompoundTag compoundTag = this.applyFilters(name, NbtUtils.snbtToStructure(string));
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                NbtIo.writeCompressed(compoundTag, byteArrayOutputStream);
                byte[] bs = byteArrayOutputStream.toByteArray();
                String string2 = SHA1.hashBytes(bs).toString();
                String string3;
                if (DUMP_SNBT_TO != null) {
                    string3 = NbtUtils.structureToSnbt(compoundTag);
                } else {
                    string3 = null;
                }

                var10 = new SnbtToNbt.TaskResult(name, bs, string3, string2);
            } catch (Throwable var12) {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                    }
                }

                throw var12;
            }

            if (bufferedReader != null) {
                bufferedReader.close();
            }

            return var10;
        } catch (Throwable var13) {
            throw new SnbtToNbt.StructureConversionException(path, var13);
        }
    }

    private void storeStructureIfChanged(HashCache cache, SnbtToNbt.TaskResult data, Path root) {
        if (data.snbtPayload != null) {
            Path path = DUMP_SNBT_TO.resolve(data.name + ".snbt");

            try {
                NbtToSnbt.writeSnbt(path, data.snbtPayload);
            } catch (IOException var9) {
                LOGGER.error("Couldn't write structure SNBT {} at {}", data.name, path, var9);
            }
        }

        Path path2 = root.resolve(data.name + ".nbt");

        try {
            if (!Objects.equals(cache.getHash(path2), data.hash) || !Files.exists(path2)) {
                Files.createDirectories(path2.getParent());
                OutputStream outputStream = Files.newOutputStream(path2);

                try {
                    outputStream.write(data.payload);
                } catch (Throwable var10) {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Throwable var8) {
                            var10.addSuppressed(var8);
                        }
                    }

                    throw var10;
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }

            cache.putNew(path2, data.hash);
        } catch (IOException var11) {
            LOGGER.error("Couldn't write structure {} at {}", data.name, path2, var11);
        }

    }

    @FunctionalInterface
    public interface Filter {
        CompoundTag apply(String name, CompoundTag nbt);
    }

    static class StructureConversionException extends RuntimeException {
        public StructureConversionException(Path path, Throwable cause) {
            super(path.toAbsolutePath().toString(), cause);
        }
    }

    static class TaskResult {
        final String name;
        final byte[] payload;
        @Nullable
        final String snbtPayload;
        final String hash;

        public TaskResult(String name, byte[] bytes, @Nullable String snbtContent, String sha1) {
            this.name = name;
            this.payload = bytes;
            this.snbtPayload = snbtContent;
            this.hash = sha1;
        }
    }
}
