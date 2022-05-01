package net.minecraft.data.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;

public class RegistryDumpReport implements DataProvider {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private final DataGenerator generator;

    public RegistryDumpReport(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(HashCache cache) throws IOException {
        JsonObject jsonObject = new JsonObject();
        Registry.REGISTRY.holders().forEach((entry) -> {
            jsonObject.add(entry.key().location().toString(), dumpRegistry(entry.value()));
        });
        Path path = this.generator.getOutputFolder().resolve("reports/registries.json");
        DataProvider.save(GSON, cache, jsonObject, path);
    }

    private static <T> JsonElement dumpRegistry(Registry<T> registry) {
        JsonObject jsonObject = new JsonObject();
        if (registry instanceof DefaultedRegistry) {
            ResourceLocation resourceLocation = ((DefaultedRegistry)registry).getDefaultKey();
            jsonObject.addProperty("default", resourceLocation.toString());
        }

        int i = Registry.REGISTRY.getId(registry);
        jsonObject.addProperty("protocol_id", i);
        JsonObject jsonObject2 = new JsonObject();
        registry.holders().forEach((entry) -> {
            T object = entry.value();
            int i = registry.getId(object);
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("protocol_id", i);
            jsonObject2.add(entry.key().location().toString(), jsonObject2);
        });
        jsonObject.add("entries", jsonObject2);
        return jsonObject;
    }

    @Override
    public String getName() {
        return "Registry Dump";
    }
}
