package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;

public class LongArgumentSerializer implements ArgumentSerializer<LongArgumentType> {
    @Override
    public void serializeToNetwork(LongArgumentType type, FriendlyByteBuf buf) {
        boolean bl = type.getMinimum() != Long.MIN_VALUE;
        boolean bl2 = type.getMaximum() != Long.MAX_VALUE;
        buf.writeByte(BrigadierArgumentSerializers.createNumberFlags(bl, bl2));
        if (bl) {
            buf.writeLong(type.getMinimum());
        }

        if (bl2) {
            buf.writeLong(type.getMaximum());
        }

    }

    @Override
    public LongArgumentType deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
        byte b = friendlyByteBuf.readByte();
        long l = BrigadierArgumentSerializers.numberHasMin(b) ? friendlyByteBuf.readLong() : Long.MIN_VALUE;
        long m = BrigadierArgumentSerializers.numberHasMax(b) ? friendlyByteBuf.readLong() : Long.MAX_VALUE;
        return LongArgumentType.longArg(l, m);
    }

    @Override
    public void serializeToJson(LongArgumentType type, JsonObject json) {
        if (type.getMinimum() != Long.MIN_VALUE) {
            json.addProperty("min", type.getMinimum());
        }

        if (type.getMaximum() != Long.MAX_VALUE) {
            json.addProperty("max", type.getMaximum());
        }

    }
}
