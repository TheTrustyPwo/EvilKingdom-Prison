package net.minecraft.network.protocol.login;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundCustomQueryPacket implements Packet<ServerLoginPacketListener> {
    private static final int MAX_PAYLOAD_SIZE = 1048576;
    private final int transactionId;
    @Nullable
    private final FriendlyByteBuf data;

    public ServerboundCustomQueryPacket(int queryId, @Nullable FriendlyByteBuf response) {
        this.transactionId = queryId;
        this.data = response;
    }

    public ServerboundCustomQueryPacket(FriendlyByteBuf buf) {
        this.transactionId = buf.readVarInt();
        if (buf.readBoolean()) {
            int i = buf.readableBytes();
            if (i < 0 || i > 1048576) {
                throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
            }

            this.data = new FriendlyByteBuf(buf.readBytes(i));
        } else {
            this.data = null;
        }

    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.transactionId);
        if (this.data != null) {
            buf.writeBoolean(true);
            buf.writeBytes(this.data.copy());
        } else {
            buf.writeBoolean(false);
        }

    }

    @Override
    public void handle(ServerLoginPacketListener listener) {
        listener.handleCustomQueryPacket(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    @Nullable
    public FriendlyByteBuf getData() {
        return this.data;
    }
}
