package net.minecraft.world.level.saveddata.maps;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class MapIndex extends SavedData {
    public static final String FILE_NAME = "idcounts";
    private final Object2IntMap<String> usedAuxIds = new Object2IntOpenHashMap<>();

    public MapIndex() {
        this.usedAuxIds.defaultReturnValue(-1);
    }

    public static MapIndex load(CompoundTag nbt) {
        MapIndex mapIndex = new MapIndex();

        for(String string : nbt.getAllKeys()) {
            if (nbt.contains(string, 99)) {
                mapIndex.usedAuxIds.put(string, nbt.getInt(string));
            }
        }

        return mapIndex;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        for(Entry<String> entry : this.usedAuxIds.object2IntEntrySet()) {
            nbt.putInt(entry.getKey(), entry.getIntValue());
        }

        return nbt;
    }

    public int getFreeAuxValueForMap() {
        int i = this.usedAuxIds.getInt("map") + 1;
        this.usedAuxIds.put("map", i);
        this.setDirty();
        return i;
    }
}
