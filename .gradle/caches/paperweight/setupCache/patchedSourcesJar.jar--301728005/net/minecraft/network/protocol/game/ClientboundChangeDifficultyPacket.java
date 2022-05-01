package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.Difficulty;

public class ClientboundChangeDifficultyPacket implements Packet<ClientGamePacketListener> {
    private final Difficulty difficulty;
    private final boolean locked;

    public ClientboundChangeDifficultyPacket(Difficulty difficulty, boolean difficultyLocked) {
        this.difficulty = difficulty;
        this.locked = difficultyLocked;
    }

    public ClientboundChangeDifficultyPacket(FriendlyByteBuf buf) {
        this.difficulty = Difficulty.byId(buf.readUnsignedByte());
        this.locked = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.difficulty.getId());
        buf.writeBoolean(this.locked);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleChangeDifficulty(this);
    }

    public boolean isLocked() {
        return this.locked;
    }

    public Difficulty getDifficulty() {
        return this.difficulty;
    }
}
