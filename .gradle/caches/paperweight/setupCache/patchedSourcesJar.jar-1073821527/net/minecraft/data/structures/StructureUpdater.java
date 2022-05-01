package net.minecraft.data.structures;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

public class StructureUpdater implements SnbtToNbt.Filter {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public CompoundTag apply(String name, CompoundTag nbt) {
        return name.startsWith("data/minecraft/structures/") ? update(name, nbt) : nbt;
    }

    public static CompoundTag update(String name, CompoundTag nbt) {
        return updateStructure(name, patchVersion(nbt));
    }

    private static CompoundTag patchVersion(CompoundTag nbt) {
        if (!nbt.contains("DataVersion", 99)) {
            nbt.putInt("DataVersion", 500);
        }

        return nbt;
    }

    private static CompoundTag updateStructure(String name, CompoundTag nbt) {
        StructureTemplate structureTemplate = new StructureTemplate();
        int i = nbt.getInt("DataVersion");
        int j = 2965;
        if (i < 2965) {
            LOGGER.warn("SNBT Too old, do not forget to update: {} < {}: {}", i, 2965, name);
        }

        CompoundTag compoundTag = NbtUtils.update(DataFixers.getDataFixer(), DataFixTypes.STRUCTURE, nbt, i);
        structureTemplate.load(compoundTag);
        return structureTemplate.save(new CompoundTag());
    }
}
