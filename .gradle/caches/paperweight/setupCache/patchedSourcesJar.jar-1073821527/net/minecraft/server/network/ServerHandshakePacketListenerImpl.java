package net.minecraft.server.network;

import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.MinecraftServer;

public class ServerHandshakePacketListenerImpl implements ServerHandshakePacketListener {
    private static final Component IGNORE_STATUS_REASON = new TextComponent("Ignoring status request");
    private final MinecraftServer server;
    private final Connection connection;

    public ServerHandshakePacketListenerImpl(MinecraftServer server, Connection connection) {
        this.server = server;
        this.connection = connection;
    }

    @Override
    public void handleIntention(ClientIntentionPacket packet) {
        switch(packet.getIntention()) {
        case LOGIN:
            this.connection.setProtocol(ConnectionProtocol.LOGIN);
            if (packet.getProtocolVersion() != SharedConstants.getCurrentVersion().getProtocolVersion()) {
                Component component;
                if (packet.getProtocolVersion() < 754) {
                    component = new TranslatableComponent("multiplayer.disconnect.outdated_client", SharedConstants.getCurrentVersion().getName());
                } else {
                    component = new TranslatableComponent("multiplayer.disconnect.incompatible", SharedConstants.getCurrentVersion().getName());
                }

                this.connection.send(new ClientboundLoginDisconnectPacket(component));
                this.connection.disconnect(component);
            } else {
                this.connection.setListener(new ServerLoginPacketListenerImpl(this.server, this.connection));
            }
            break;
        case STATUS:
            if (this.server.repliesToStatus()) {
                this.connection.setProtocol(ConnectionProtocol.STATUS);
                this.connection.setListener(new ServerStatusPacketListenerImpl(this.server, this.connection));
            } else {
                this.connection.disconnect(IGNORE_STATUS_REASON);
            }
            break;
        default:
            throw new UnsupportedOperationException("Invalid intention " + packet.getIntention());
        }

    }

    @Override
    public void onDisconnect(Component reason) {
    }

    @Override
    public Connection getConnection() {
        return this.connection;
    }
}
