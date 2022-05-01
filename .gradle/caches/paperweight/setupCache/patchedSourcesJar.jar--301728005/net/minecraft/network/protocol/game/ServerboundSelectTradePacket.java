package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundSelectTradePacket implements Packet<ServerGamePacketListener> {
    private final int item;

    public ServerboundSelectTradePacket(int tradeId) {
        this.item = tradeId;
    }

    public ServerboundSelectTradePacket(FriendlyByteBuf buf) {
        this.item = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.item);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSelectTrade(this);
    }

    public int getItem() {
        return this.item;
    }
}
