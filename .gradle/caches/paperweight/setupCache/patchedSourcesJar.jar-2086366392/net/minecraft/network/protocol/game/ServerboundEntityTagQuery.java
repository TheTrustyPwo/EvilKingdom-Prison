package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundEntityTagQuery implements Packet<ServerGamePacketListener> {
    private final int transactionId;
    private final int entityId;

    public ServerboundEntityTagQuery(int transactionId, int entityId) {
        this.transactionId = transactionId;
        this.entityId = entityId;
    }

    public ServerboundEntityTagQuery(FriendlyByteBuf buf) {
        this.transactionId = buf.readVarInt();
        this.entityId = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.transactionId);
        buf.writeVarInt(this.entityId);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleEntityTagQuery(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    public int getEntityId() {
        return this.entityId;
    }
}
