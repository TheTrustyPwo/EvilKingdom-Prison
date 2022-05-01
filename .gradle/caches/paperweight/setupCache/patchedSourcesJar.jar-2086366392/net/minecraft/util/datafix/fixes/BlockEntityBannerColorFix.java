package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class BlockEntityBannerColorFix extends NamedEntityFix {
    public BlockEntityBannerColorFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "BlockEntityBannerColorFix", References.BLOCK_ENTITY, "minecraft:banner");
    }

    public Dynamic<?> fixTag(Dynamic<?> dynamic) {
        dynamic = dynamic.update("Base", (dynamicx) -> {
            return dynamicx.createInt(15 - dynamicx.asInt(0));
        });
        return dynamic.update("Patterns", (dynamicx) -> {
            return DataFixUtils.orElse(dynamicx.asStreamOpt().map((stream) -> {
                return stream.map((dynamic) -> {
                    return dynamic.update("Color", (dynamicx) -> {
                        return dynamicx.createInt(15 - dynamicx.asInt(0));
                    });
                });
            }).map(dynamicx::createList).result(), dynamicx);
        });
    }

    @Override
    protected Typed<?> fix(Typed<?> inputType) {
        return inputType.update(DSL.remainderFinder(), this::fixTag);
    }
}
