package net.minecraft.data.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.slf4j.Logger;

public class WorldgenRegistryDumpReport implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private final DataGenerator generator;

    public WorldgenRegistryDumpReport(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(HashCache cache) {
        Path path = this.generator.getOutputFolder();
        RegistryAccess registryAccess = RegistryAccess.BUILTIN.get();
        int i = 0;
        Registry<LevelStem> registry = DimensionType.defaultDimensions(registryAccess, 0L, false);
        ChunkGenerator chunkGenerator = WorldGenSettings.makeDefaultOverworld(registryAccess, 0L, false);
        Registry<LevelStem> registry2 = WorldGenSettings.withOverworld(registryAccess.ownedRegistryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), registry, chunkGenerator);
        DynamicOps<JsonElement> dynamicOps = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        RegistryAccess.knownRegistries().forEach((info) -> {
            dumpRegistryCap(cache, path, registryAccess, dynamicOps, info);
        });
        dumpRegistry(path, cache, dynamicOps, Registry.LEVEL_STEM_REGISTRY, registry2, LevelStem.CODEC);
    }

    private static <T> void dumpRegistryCap(HashCache cache, Path path, RegistryAccess registryManager, DynamicOps<JsonElement> json, RegistryAccess.RegistryData<T> info) {
        dumpRegistry(path, cache, json, info.key(), registryManager.ownedRegistryOrThrow(info.key()), info.codec());
    }

    private static <E, T extends Registry<E>> void dumpRegistry(Path path, HashCache cache, DynamicOps<JsonElement> json, ResourceKey<? extends T> registryKey, T registry, Encoder<E> encoder) {
        for(Entry<ResourceKey<E>, E> entry : registry.entrySet()) {
            Path path2 = createPath(path, registryKey.location(), entry.getKey().location());
            dumpValue(path2, cache, json, encoder, entry.getValue());
        }

    }

    private static <E> void dumpValue(Path path, HashCache cache, DynamicOps<JsonElement> json, Encoder<E> encoder, E value) {
        try {
            Optional<JsonElement> optional = encoder.encodeStart(json, value).resultOrPartial((string) -> {
                LOGGER.error("Couldn't serialize element {}: {}", path, string);
            });
            if (optional.isPresent()) {
                DataProvider.save(GSON, cache, optional.get(), path);
            }
        } catch (IOException var6) {
            LOGGER.error("Couldn't save element {}", path, var6);
        }

    }

    private static Path createPath(Path root, ResourceLocation rootId, ResourceLocation id) {
        return resolveTopPath(root).resolve(id.getNamespace()).resolve(rootId.getPath()).resolve(id.getPath() + ".json");
    }

    private static Path resolveTopPath(Path path) {
        return path.resolve("reports").resolve("worldgen");
    }

    @Override
    public String getName() {
        return "Worldgen";
    }
}
