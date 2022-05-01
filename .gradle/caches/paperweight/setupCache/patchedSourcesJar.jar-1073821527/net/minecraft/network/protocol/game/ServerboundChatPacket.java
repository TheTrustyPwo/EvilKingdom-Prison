package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundChatPacket implements Packet<ServerGamePacketListener> {
    private static final int MAX_MESSAGE_LENGTH = 256;
    private final String message;

    public ServerboundChatPacket(String chatMessage) {
        if (chatMessage.length() > 256) {
            chatMessage = chatMessage.substring(0, 256);
        }

        this.message = chatMessage;
    }

    public ServerboundChatPacket(FriendlyByteBuf buf) {
        this.message = buf.readUtf(256);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.message);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleChat(this);
    }

    public String getMessage() {
        return this.message;
    }
}
