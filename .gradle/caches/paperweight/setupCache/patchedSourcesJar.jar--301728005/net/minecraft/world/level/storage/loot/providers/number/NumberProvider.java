package net.minecraft.world.level.storage.loot.providers.number;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;

public interface NumberProvider extends LootContextUser {
    float getFloat(LootContext context);

    default int getInt(LootContext context) {
        return Math.round(this.getFloat(context));
    }

    LootNumberProviderType getType();
}
