package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ServerboundCustomPayloadPacket implements Packet<ServerGamePacketListener> {
    private static final int MAX_PAYLOAD_SIZE = 32767;
    public static final ResourceLocation BRAND = new ResourceLocation("brand");
    public final ResourceLocation identifier;
    public final FriendlyByteBuf data;

    public ServerboundCustomPayloadPacket(ResourceLocation channel, FriendlyByteBuf data) {
        this.identifier = channel;
        this.data = data;
    }

    public ServerboundCustomPayloadPacket(FriendlyByteBuf buf) {
        this.identifier = buf.readResourceLocation();
        int i = buf.readableBytes();
        if (i >= 0 && i <= 32767) {
            this.data = new FriendlyByteBuf(buf.readBytes(i));
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 32767 bytes");
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.identifier);
        buf.writeBytes((ByteBuf)this.data);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleCustomPayload(this);
        this.data.release();
    }

    public ResourceLocation getIdentifier() {
        return this.identifier;
    }

    public FriendlyByteBuf getData() {
        return this.data;
    }
}
