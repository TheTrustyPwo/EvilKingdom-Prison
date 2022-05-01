// mc-dev import
package net.minecraft.server;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.PredicateManager;
import org.slf4j.Logger;

public class ServerAdvancementManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Gson GSON = (new GsonBuilder()).create();
    public AdvancementList advancements = new AdvancementList();
    private final PredicateManager predicateManager;

    public ServerAdvancementManager(PredicateManager conditionManager) {
        super(ServerAdvancementManager.GSON, "advancements");
        this.predicateManager = conditionManager;
    }

    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Advancement.Builder> map1 = Maps.newHashMap();

        prepared.forEach((minecraftkey, jsonelement) -> {
            // Spigot start
            if (org.spigotmc.SpigotConfig.disabledAdvancements != null && (org.spigotmc.SpigotConfig.disabledAdvancements.contains("*") || org.spigotmc.SpigotConfig.disabledAdvancements.contains(minecraftkey.toString()) || org.spigotmc.SpigotConfig.disabledAdvancements.contains(minecraftkey.getNamespace()))) {
                return;
            }
            // Spigot end

            try {
                JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "advancement");
                Advancement.Builder advancement_serializedadvancement = Advancement.Builder.fromJson(jsonobject, new DeserializationContext(minecraftkey, this.predicateManager));

                map1.put(minecraftkey, advancement_serializedadvancement);
            } catch (Exception exception) {
                ServerAdvancementManager.LOGGER.error("Parsing error loading custom advancement {}: {}", minecraftkey, exception.getMessage());
            }

        });
        AdvancementList advancements = new AdvancementList();

        advancements.add(map1);
        Iterator iterator = advancements.getRoots().iterator();

        while (iterator.hasNext()) {
            Advancement advancement = (Advancement) iterator.next();

            if (advancement.getDisplay() != null) {
                TreeNodePosition.run(advancement);
            }
        }

        this.advancements = advancements;
    }

    @Nullable
    public Advancement getAdvancement(ResourceLocation id) {
        return this.advancements.get(id);
    }

    public Collection<Advancement> getAllAdvancements() {
        return this.advancements.getAllAdvancements();
    }
}
