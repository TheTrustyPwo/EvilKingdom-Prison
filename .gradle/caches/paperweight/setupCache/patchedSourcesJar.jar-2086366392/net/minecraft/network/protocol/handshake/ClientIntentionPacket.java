// mc-dev import
package net.minecraft.network.protocol.handshake;

import net.minecraft.SharedConstants;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientIntentionPacket implements Packet<ServerHandshakePacketListener> {

    private static final int MAX_HOST_LENGTH = 255;
    private final int protocolVersion;
    public String hostName;
    public final int port;
    private final ConnectionProtocol intention;

    public ClientIntentionPacket(String address, int port, ConnectionProtocol intendedState) {
        this.protocolVersion = SharedConstants.getCurrentVersion().getProtocolVersion();
        this.hostName = address;
        this.port = port;
        this.intention = intendedState;
    }

    public ClientIntentionPacket(FriendlyByteBuf buf) {
        this.protocolVersion = buf.readVarInt();
        this.hostName = buf.readUtf(Short.MAX_VALUE); // Spigot
        this.port = buf.readUnsignedShort();
        this.intention = ConnectionProtocol.getById(buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.protocolVersion);
        buf.writeUtf(this.hostName);
        buf.writeShort(this.port);
        buf.writeVarInt(this.intention.getId());
    }

    public void handle(ServerHandshakePacketListener listener) {
        listener.handleIntention(this);
    }

    public ConnectionProtocol getIntention() {
        return this.intention;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    public String getHostName() {
        return this.hostName;
    }

    public int getPort() {
        return this.port;
    }
}
