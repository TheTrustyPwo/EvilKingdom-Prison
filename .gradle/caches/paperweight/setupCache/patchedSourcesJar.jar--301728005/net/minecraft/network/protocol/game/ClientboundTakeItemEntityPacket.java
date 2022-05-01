package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundTakeItemEntityPacket implements Packet<ClientGamePacketListener> {
    private final int itemId;
    private final int playerId;
    private final int amount;

    public ClientboundTakeItemEntityPacket(int entityId, int collectorId, int stackAmount) {
        this.itemId = entityId;
        this.playerId = collectorId;
        this.amount = stackAmount;
    }

    public ClientboundTakeItemEntityPacket(FriendlyByteBuf buf) {
        this.itemId = buf.readVarInt();
        this.playerId = buf.readVarInt();
        this.amount = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.itemId);
        buf.writeVarInt(this.playerId);
        buf.writeVarInt(this.amount);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleTakeItemEntity(this);
    }

    public int getItemId() {
        return this.itemId;
    }

    public int getPlayerId() {
        return this.playerId;
    }

    public int getAmount() {
        return this.amount;
    }
}
