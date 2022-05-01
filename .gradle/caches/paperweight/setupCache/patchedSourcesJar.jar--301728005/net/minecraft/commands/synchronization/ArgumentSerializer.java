package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.network.FriendlyByteBuf;

public interface ArgumentSerializer<T extends ArgumentType<?>> {
    void serializeToNetwork(T type, FriendlyByteBuf buf);

    T deserializeFromNetwork(FriendlyByteBuf buf);

    void serializeToJson(T type, JsonObject json);
}
