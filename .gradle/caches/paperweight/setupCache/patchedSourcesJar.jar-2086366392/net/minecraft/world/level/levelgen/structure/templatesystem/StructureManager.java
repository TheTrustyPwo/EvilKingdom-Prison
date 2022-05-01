package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

public class StructureManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String STRUCTURE_DIRECTORY_NAME = "structures";
    private static final String STRUCTURE_FILE_EXTENSION = ".nbt";
    private static final String STRUCTURE_TEXT_FILE_EXTENSION = ".snbt";
    public final Map<ResourceLocation, Optional<StructureTemplate>> structureRepository = Maps.newConcurrentMap();
    private final DataFixer fixerUpper;
    private ResourceManager resourceManager;
    private final Path generatedDir;

    public StructureManager(ResourceManager resourceManager, LevelStorageSource.LevelStorageAccess session, DataFixer dataFixer) {
        this.resourceManager = resourceManager;
        this.fixerUpper = dataFixer;
        this.generatedDir = session.getLevelPath(LevelResource.GENERATED_DIR).normalize();
    }

    public StructureTemplate getOrCreate(ResourceLocation id) {
        Optional<StructureTemplate> optional = this.get(id);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            StructureTemplate structureTemplate = new StructureTemplate();
            this.structureRepository.put(id, Optional.of(structureTemplate));
            return structureTemplate;
        }
    }

    public Optional<StructureTemplate> get(ResourceLocation id) {
        return this.structureRepository.computeIfAbsent(id, (identifier) -> {
            Optional<StructureTemplate> optional = this.loadFromGenerated(identifier);
            return optional.isPresent() ? optional : this.loadFromResource(identifier);
        });
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.structureRepository.clear();
    }

    public Optional<StructureTemplate> loadFromResource(ResourceLocation id) {
        ResourceLocation resourceLocation = new ResourceLocation(id.getNamespace(), "structures/" + id.getPath() + ".nbt");

        try {
            Resource resource = this.resourceManager.getResource(resourceLocation);

            Optional var4;
            try {
                var4 = Optional.of(this.readStructure(resource.getInputStream()));
            } catch (Throwable var7) {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (resource != null) {
                resource.close();
            }

            return var4;
        } catch (FileNotFoundException var8) {
            return Optional.empty();
        } catch (Throwable var9) {
            LOGGER.error("Couldn't load structure {}: {}", id, var9.toString());
            return Optional.empty();
        }
    }

    public Optional<StructureTemplate> loadFromGenerated(ResourceLocation id) {
        if (!this.generatedDir.toFile().isDirectory()) {
            return Optional.empty();
        } else {
            Path path = this.createAndValidatePathToStructure(id, ".nbt");

            try {
                InputStream inputStream = new FileInputStream(path.toFile());

                Optional var4;
                try {
                    var4 = Optional.of(this.readStructure(inputStream));
                } catch (Throwable var7) {
                    try {
                        inputStream.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }

                    throw var7;
                }

                inputStream.close();
                return var4;
            } catch (FileNotFoundException var8) {
                return Optional.empty();
            } catch (IOException var9) {
                LOGGER.error("Couldn't load structure from {}", path, var9);
                return Optional.empty();
            }
        }
    }

    public StructureTemplate readStructure(InputStream structureInputStream) throws IOException {
        CompoundTag compoundTag = NbtIo.readCompressed(structureInputStream);
        return this.readStructure(compoundTag);
    }

    public StructureTemplate readStructure(CompoundTag nbt) {
        if (!nbt.contains("DataVersion", 99)) {
            nbt.putInt("DataVersion", 500);
        }

        StructureTemplate structureTemplate = new StructureTemplate();
        structureTemplate.load(NbtUtils.update(this.fixerUpper, DataFixTypes.STRUCTURE, nbt, nbt.getInt("DataVersion")));
        return structureTemplate;
    }

    public boolean save(ResourceLocation id) {
        Optional<StructureTemplate> optional = this.structureRepository.get(id);
        if (!optional.isPresent()) {
            return false;
        } else {
            StructureTemplate structureTemplate = optional.get();
            Path path = this.createAndValidatePathToStructure(id, ".nbt");
            Path path2 = path.getParent();
            if (path2 == null) {
                return false;
            } else {
                try {
                    Files.createDirectories(Files.exists(path2) ? path2.toRealPath() : path2);
                } catch (IOException var13) {
                    LOGGER.error("Failed to create parent directory: {}", (Object)path2);
                    return false;
                }

                CompoundTag compoundTag = structureTemplate.save(new CompoundTag());

                try {
                    OutputStream outputStream = new FileOutputStream(path.toFile());

                    try {
                        NbtIo.writeCompressed(compoundTag, outputStream);
                    } catch (Throwable var11) {
                        try {
                            outputStream.close();
                        } catch (Throwable var10) {
                            var11.addSuppressed(var10);
                        }

                        throw var11;
                    }

                    outputStream.close();
                    return true;
                } catch (Throwable var12) {
                    return false;
                }
            }
        }
    }

    public Path createPathToStructure(ResourceLocation id, String extension) {
        try {
            Path path = this.generatedDir.resolve(id.getNamespace());
            Path path2 = path.resolve("structures");
            return FileUtil.createPathToResource(path2, id.getPath(), extension);
        } catch (InvalidPathException var5) {
            throw new ResourceLocationException("Invalid resource path: " + id, var5);
        }
    }

    public Path createAndValidatePathToStructure(ResourceLocation id, String extension) {
        if (id.getPath().contains("//")) {
            throw new ResourceLocationException("Invalid resource path: " + id);
        } else {
            Path path = this.createPathToStructure(id, extension);
            if (path.startsWith(this.generatedDir) && FileUtil.isPathNormalized(path) && FileUtil.isPathPortable(path)) {
                return path;
            } else {
                throw new ResourceLocationException("Invalid resource path: " + path);
            }
        }
    }

    public void remove(ResourceLocation id) {
        this.structureRepository.remove(id);
    }
}
