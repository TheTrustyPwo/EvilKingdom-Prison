package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;

public class FloatArgumentSerializer implements ArgumentSerializer<FloatArgumentType> {
    @Override
    public void serializeToNetwork(FloatArgumentType type, FriendlyByteBuf buf) {
        boolean bl = type.getMinimum() != -Float.MAX_VALUE;
        boolean bl2 = type.getMaximum() != Float.MAX_VALUE;
        buf.writeByte(BrigadierArgumentSerializers.createNumberFlags(bl, bl2));
        if (bl) {
            buf.writeFloat(type.getMinimum());
        }

        if (bl2) {
            buf.writeFloat(type.getMaximum());
        }

    }

    @Override
    public FloatArgumentType deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
        byte b = friendlyByteBuf.readByte();
        float f = BrigadierArgumentSerializers.numberHasMin(b) ? friendlyByteBuf.readFloat() : -Float.MAX_VALUE;
        float g = BrigadierArgumentSerializers.numberHasMax(b) ? friendlyByteBuf.readFloat() : Float.MAX_VALUE;
        return FloatArgumentType.floatArg(f, g);
    }

    @Override
    public void serializeToJson(FloatArgumentType type, JsonObject json) {
        if (type.getMinimum() != -Float.MAX_VALUE) {
            json.addProperty("min", type.getMinimum());
        }

        if (type.getMaximum() != Float.MAX_VALUE) {
            json.addProperty("max", type.getMaximum());
        }

    }
}
