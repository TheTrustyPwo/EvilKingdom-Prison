package net.minecraft.advancements;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class AdvancementList {

    private static final Logger LOGGER = LogUtils.getLogger();
    public final Map<ResourceLocation, Advancement> advancements = Maps.newHashMap();
    private final Set<Advancement> roots = Sets.newLinkedHashSet();
    private final Set<Advancement> tasks = Sets.newLinkedHashSet();
    @Nullable
    private AdvancementList.Listener listener;

    public AdvancementList() {}

    private void remove(Advancement advancement) {
        Iterator iterator = advancement.getChildren().iterator();

        while (iterator.hasNext()) {
            Advancement advancement1 = (Advancement) iterator.next();

            this.remove(advancement1);
        }

        AdvancementList.LOGGER.info("Forgot about advancement {}", advancement.getId());
        this.advancements.remove(advancement.getId());
        if (advancement.getParent() == null) {
            this.roots.remove(advancement);
            if (this.listener != null) {
                this.listener.onRemoveAdvancementRoot(advancement);
            }
        } else {
            this.tasks.remove(advancement);
            if (this.listener != null) {
                this.listener.onRemoveAdvancementTask(advancement);
            }
        }

    }

    public void remove(Set<ResourceLocation> advancements) {
        Iterator iterator = advancements.iterator();

        while (iterator.hasNext()) {
            ResourceLocation minecraftkey = (ResourceLocation) iterator.next();
            Advancement advancement = (Advancement) this.advancements.get(minecraftkey);

            if (advancement == null) {
                AdvancementList.LOGGER.warn("Told to remove advancement {} but I don't know what that is", minecraftkey);
            } else {
                this.remove(advancement);
            }
        }

    }

    public void add(Map<ResourceLocation, Advancement.Builder> map) {
        HashMap hashmap = Maps.newHashMap(map);

        label42:
        while (!hashmap.isEmpty()) {
            boolean flag = false;
            Iterator iterator = hashmap.entrySet().iterator();

            Entry entry;

            while (iterator.hasNext()) {
                entry = (Entry) iterator.next();
                ResourceLocation minecraftkey = (ResourceLocation) entry.getKey();
                Advancement.Builder advancement_serializedadvancement = (Advancement.Builder) entry.getValue();
                Map<ResourceLocation, Advancement> map1 = this.advancements; // CraftBukkit - decompile error

                Objects.requireNonNull(this.advancements);
                if (advancement_serializedadvancement.canBuild(map1::get)) {
                    Advancement advancement = advancement_serializedadvancement.build(minecraftkey);

                    this.advancements.put(minecraftkey, advancement);
                    flag = true;
                    iterator.remove();
                    if (advancement.getParent() == null) {
                        this.roots.add(advancement);
                        if (this.listener != null) {
                            this.listener.onAddAdvancementRoot(advancement);
                        }
                    } else {
                        this.tasks.add(advancement);
                        if (this.listener != null) {
                            this.listener.onAddAdvancementTask(advancement);
                        }
                    }
                }
            }

            if (!flag) {
                iterator = hashmap.entrySet().iterator();

                while (true) {
                    if (!iterator.hasNext()) {
                        break label42;
                    }

                    entry = (Entry) iterator.next();
                    AdvancementList.LOGGER.error("Couldn't load advancement {}: {}", entry.getKey(), entry.getValue());
                }
            }
        }

        // Advancements.LOGGER.info("Loaded {} advancements", this.advancements.size()); // CraftBukkit - moved to AdvancementDataWorld#reload
    }

    public void clear() {
        this.advancements.clear();
        this.roots.clear();
        this.tasks.clear();
        if (this.listener != null) {
            this.listener.onAdvancementsCleared();
        }

    }

    public Iterable<Advancement> getRoots() {
        return this.roots;
    }

    public Collection<Advancement> getAllAdvancements() {
        return this.advancements.values();
    }

    @Nullable
    public Advancement get(ResourceLocation id) {
        return (Advancement) this.advancements.get(id);
    }

    public void setListener(@Nullable AdvancementList.Listener listener) {
        this.listener = listener;
        if (listener != null) {
            Iterator iterator = this.roots.iterator();

            Advancement advancement;

            while (iterator.hasNext()) {
                advancement = (Advancement) iterator.next();
                listener.onAddAdvancementRoot(advancement);
            }

            iterator = this.tasks.iterator();

            while (iterator.hasNext()) {
                advancement = (Advancement) iterator.next();
                listener.onAddAdvancementTask(advancement);
            }
        }

    }

    public interface Listener {

        void onAddAdvancementRoot(Advancement root);

        void onRemoveAdvancementRoot(Advancement root);

        void onAddAdvancementTask(Advancement dependent);

        void onRemoveAdvancementTask(Advancement dependent);

        void onAdvancementsCleared();
    }
}
