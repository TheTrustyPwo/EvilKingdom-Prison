// mc-dev import
package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundChatPacket implements Packet<ClientGamePacketListener> {

    private static final int MAX_LENGTH = Short.MAX_VALUE * 8 + 8; // Paper
    private final Component message;
    public net.kyori.adventure.text.Component adventure$message; // Paper
    public net.md_5.bungee.api.chat.BaseComponent[] components; // Spigot
    private final ChatType type;
    private final UUID sender;

    public ClientboundChatPacket(Component message, ChatType type, UUID sender) {
        this.message = message;
        this.type = type;
        this.sender = sender != null ? sender : net.minecraft.Util.NIL_UUID;
    }

    public ClientboundChatPacket(FriendlyByteBuf buf) {
        this.message = buf.readComponent();
        this.type = ChatType.getForIndex(buf.readByte());
        this.sender = buf.readUUID();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        // Paper start
        if (this.adventure$message != null) {
            buf.writeComponent(this.adventure$message);
        } else
        // Paper end
        // Spigot start
        if (this.components != null) {
            // buf.writeUtf(net.md_5.bungee.chat.ComponentSerializer.toString(components)); // Paper - comment, replaced with below
            // Paper start - don't nest if we don't need to so that we can preserve formatting
            if (this.components.length == 1) {
                buf.writeUtf(net.md_5.bungee.chat.ComponentSerializer.toString(this.components[0]), MAX_LENGTH); // Paper - use proper max length
            } else {
                buf.writeUtf(net.md_5.bungee.chat.ComponentSerializer.toString(this.components), MAX_LENGTH); // Paper - use proper max length
            }
            // Paper end
        } else {
            buf.writeComponent(this.message);
        }
        // Spigot end
        buf.writeByte(this.type.getIndex());
        buf.writeUUID(this.sender);
    }

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
