package net.minecraft.network.protocol.login;

import net.minecraft.network.protocol.game.ServerPacketListener;

public interface ServerLoginPacketListener extends ServerPacketListener {
    void handleHello(ServerboundHelloPacket packet);

    void handleKey(ServerboundKeyPacket packet);

    void handleCustomQueryPacket(ServerboundCustomQueryPacket packet);
}
