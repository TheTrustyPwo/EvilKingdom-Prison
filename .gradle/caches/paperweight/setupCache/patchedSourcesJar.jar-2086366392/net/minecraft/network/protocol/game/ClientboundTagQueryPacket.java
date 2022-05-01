package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundTagQueryPacket implements Packet<ClientGamePacketListener> {
    private final int transactionId;
    @Nullable
    private final CompoundTag tag;

    public ClientboundTagQueryPacket(int transactionId, @Nullable CompoundTag nbt) {
        this.transactionId = transactionId;
        this.tag = nbt;
    }

    public ClientboundTagQueryPacket(FriendlyByteBuf buf) {
        this.transactionId = buf.readVarInt();
        this.tag = buf.readNbt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.transactionId);
        buf.writeNbt(this.tag);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleTagQueryPacket(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    @Nullable
    public CompoundTag getTag() {
        return this.tag;
    }

    @Override
    public boolean isSkippable() {
        return true;
    }
}
