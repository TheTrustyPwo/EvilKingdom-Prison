package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ClientboundCustomQueryPacket implements Packet<ClientLoginPacketListener> {
    private static final int MAX_PAYLOAD_SIZE = 1048576;
    private final int transactionId;
    private final ResourceLocation identifier;
    private final FriendlyByteBuf data;

    public ClientboundCustomQueryPacket(int queryId, ResourceLocation channel, FriendlyByteBuf payload) {
        this.transactionId = queryId;
        this.identifier = channel;
        this.data = payload;
    }

    public ClientboundCustomQueryPacket(FriendlyByteBuf buf) {
        this.transactionId = buf.readVarInt();
        this.identifier = buf.readResourceLocation();
        int i = buf.readableBytes();
        if (i >= 0 && i <= 1048576) {
            this.data = new FriendlyByteBuf(buf.readBytes(i));
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.transactionId);
        buf.writeResourceLocation(this.identifier);
        buf.writeBytes(this.data.copy());
    }

    @Override
    public void handle(ClientLoginPacketListener listener) {
        listener.handleCustomQuery(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    public ResourceLocation getIdentifier() {
        return this.identifier;
    }

    public FriendlyByteBuf getData() {
        return this.data;
    }
}
