package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;

public class DoubleArgumentSerializer implements ArgumentSerializer<DoubleArgumentType> {
    @Override
    public void serializeToNetwork(DoubleArgumentType type, FriendlyByteBuf buf) {
        boolean bl = type.getMinimum() != -Double.MAX_VALUE;
        boolean bl2 = type.getMaximum() != Double.MAX_VALUE;
        buf.writeByte(BrigadierArgumentSerializers.createNumberFlags(bl, bl2));
        if (bl) {
            buf.writeDouble(type.getMinimum());
        }

        if (bl2) {
            buf.writeDouble(type.getMaximum());
        }

    }

    @Override
    public DoubleArgumentType deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
        byte b = friendlyByteBuf.readByte();
        double d = BrigadierArgumentSerializers.numberHasMin(b) ? friendlyByteBuf.readDouble() : -Double.MAX_VALUE;
        double e = BrigadierArgumentSerializers.numberHasMax(b) ? friendlyByteBuf.readDouble() : Double.MAX_VALUE;
        return DoubleArgumentType.doubleArg(d, e);
    }

    @Override
    public void serializeToJson(DoubleArgumentType type, JsonObject json) {
        if (type.getMinimum() != -Double.MAX_VALUE) {
            json.addProperty("min", type.getMinimum());
        }

        if (type.getMaximum() != Double.MAX_VALUE) {
            json.addProperty("max", type.getMaximum());
        }

    }
}
