package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundLockDifficultyPacket implements Packet<ServerGamePacketListener> {
    private final boolean locked;

    public ServerboundLockDifficultyPacket(boolean difficultyLocked) {
        this.locked = difficultyLocked;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleLockDifficulty(this);
    }

    public ServerboundLockDifficultyPacket(FriendlyByteBuf buf) {
        this.locked = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.locked);
    }

    public boolean isLocked() {
        return this.locked;
    }
}
