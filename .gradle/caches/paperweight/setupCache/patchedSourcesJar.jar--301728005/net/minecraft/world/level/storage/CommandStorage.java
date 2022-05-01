package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;

public class CommandStorage {
    private static final String ID_PREFIX = "command_storage_";
    private final Map<String, CommandStorage.Container> namespaces = Maps.newHashMap();
    private final DimensionDataStorage storage;

    public CommandStorage(DimensionDataStorage stateManager) {
        this.storage = stateManager;
    }

    private CommandStorage.Container newStorage(String namespace) {
        CommandStorage.Container container = new CommandStorage.Container();
        this.namespaces.put(namespace, container);
        return container;
    }

    public CompoundTag get(ResourceLocation id) {
        String string = id.getNamespace();
        CommandStorage.Container container = this.storage.get((data) -> {
            return this.newStorage(string).load(data);
        }, createId(string));
        return container != null ? container.get(id.getPath()) : new CompoundTag();
    }

    public void set(ResourceLocation id, CompoundTag nbt) {
        String string = id.getNamespace();
        this.storage.computeIfAbsent((data) -> {
            return this.newStorage(string).load(data);
        }, () -> {
            return this.newStorage(string);
        }, createId(string)).put(id.getPath(), nbt);
    }

    public Stream<ResourceLocation> keys() {
        return this.namespaces.entrySet().stream().flatMap((entry) -> {
            return entry.getValue().getKeys(entry.getKey());
        });
    }

    private static String createId(String namespace) {
        return "command_storage_" + namespace;
    }

    static class Container extends SavedData {
        private static final String TAG_CONTENTS = "contents";
        private final Map<String, CompoundTag> storage = Maps.newHashMap();

        CommandStorage.Container load(CompoundTag nbt) {
            CompoundTag compoundTag = nbt.getCompound("contents");

            for(String string : compoundTag.getAllKeys()) {
                this.storage.put(string, compoundTag.getCompound(string));
            }

            return this;
        }

        @Override
        public CompoundTag save(CompoundTag nbt) {
            CompoundTag compoundTag = new CompoundTag();
            this.storage.forEach((key, value) -> {
                compoundTag.put(key, value.copy());
            });
            nbt.put("contents", compoundTag);
            return nbt;
        }

        public CompoundTag get(String name) {
            CompoundTag compoundTag = this.storage.get(name);
            return compoundTag != null ? compoundTag : new CompoundTag();
        }

        public void put(String name, CompoundTag nbt) {
            if (nbt.isEmpty()) {
                this.storage.remove(name);
            } else {
                this.storage.put(name, nbt);
            }

            this.setDirty();
        }

        public Stream<ResourceLocation> getKeys(String namespace) {
            return this.storage.keySet().stream().map((key) -> {
                return new ResourceLocation(namespace, key);
            });
        }
    }
}
