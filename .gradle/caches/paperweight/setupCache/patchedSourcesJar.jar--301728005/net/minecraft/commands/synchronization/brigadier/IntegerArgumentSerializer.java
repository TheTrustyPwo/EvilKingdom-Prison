package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;

public class IntegerArgumentSerializer implements ArgumentSerializer<IntegerArgumentType> {
    @Override
    public void serializeToNetwork(IntegerArgumentType type, FriendlyByteBuf buf) {
        boolean bl = type.getMinimum() != Integer.MIN_VALUE;
        boolean bl2 = type.getMaximum() != Integer.MAX_VALUE;
        buf.writeByte(BrigadierArgumentSerializers.createNumberFlags(bl, bl2));
        if (bl) {
            buf.writeInt(type.getMinimum());
        }

        if (bl2) {
            buf.writeInt(type.getMaximum());
        }

    }

    @Override
    public IntegerArgumentType deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
        byte b = friendlyByteBuf.readByte();
        int i = BrigadierArgumentSerializers.numberHasMin(b) ? friendlyByteBuf.readInt() : Integer.MIN_VALUE;
        int j = BrigadierArgumentSerializers.numberHasMax(b) ? friendlyByteBuf.readInt() : Integer.MAX_VALUE;
        return IntegerArgumentType.integer(i, j);
    }

    @Override
    public void serializeToJson(IntegerArgumentType type, JsonObject json) {
        if (type.getMinimum() != Integer.MIN_VALUE) {
            json.addProperty("min", type.getMinimum());
        }

        if (type.getMaximum() != Integer.MAX_VALUE) {
            json.addProperty("max", type.getMaximum());
        }

    }
}
