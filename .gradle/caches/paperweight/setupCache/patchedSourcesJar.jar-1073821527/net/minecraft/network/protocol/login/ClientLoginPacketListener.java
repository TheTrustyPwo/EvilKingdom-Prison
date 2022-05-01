package net.minecraft.network.protocol.login;

import net.minecraft.network.PacketListener;

public interface ClientLoginPacketListener extends PacketListener {
    void handleHello(ClientboundHelloPacket packet);

    void handleGameProfile(ClientboundGameProfilePacket packet);

    void handleDisconnect(ClientboundLoginDisconnectPacket packet);

    void handleCompression(ClientboundLoginCompressionPacket packet);

    void handleCustomQuery(ClientboundCustomQueryPacket packet);
}
