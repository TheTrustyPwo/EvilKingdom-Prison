package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundChatPacket implements Packet<ClientGamePacketListener> {
    private final Component message;
    private final ChatType type;
    private final UUID sender;

    public ClientboundChatPacket(Component message, ChatType type, UUID sender) {
        this.message = message;
        this.type = type;
        this.sender = sender;
    }

    public ClientboundChatPacket(FriendlyByteBuf buf) {
        this.message = buf.readComponent();
        this.type = ChatType.getForIndex(buf.readByte());
        this.sender = buf.readUUID();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeComponent(this.message);
        buf.writeByte(this.type.getIndex());
        buf.writeUUID(this.sender);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleChat(this);
    }

    public Component getMessage() {
        return this.message;
    }

    public ChatType getType() {
        return this.type;
    }

    public UUID getSender() {
        return this.sender;
    }

    @Override
    public boolean isSkippable() {
        return true;
    }
}
