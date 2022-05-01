package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundRenameItemPacket implements Packet<ServerGamePacketListener> {
    private final String name;

    public ServerboundRenameItemPacket(String name) {
        this.name = name;
    }

    public ServerboundRenameItemPacket(FriendlyByteBuf buf) {
        this.name = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.name);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleRenameItem(this);
    }

    public String getName() {
        return this.name;
    }
}
