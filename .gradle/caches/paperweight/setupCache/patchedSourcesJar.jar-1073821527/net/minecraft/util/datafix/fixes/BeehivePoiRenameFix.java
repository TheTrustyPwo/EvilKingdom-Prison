package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;

public class BeehivePoiRenameFix extends PoiTypeRename {
    public BeehivePoiRenameFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected String rename(String input) {
        return input.equals("minecraft:bee_hive") ? "minecraft:beehive" : input;
    }
}
