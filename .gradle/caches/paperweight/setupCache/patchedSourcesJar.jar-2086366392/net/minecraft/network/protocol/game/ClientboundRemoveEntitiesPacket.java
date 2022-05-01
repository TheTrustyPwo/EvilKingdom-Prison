package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundRemoveEntitiesPacket implements Packet<ClientGamePacketListener> {
    private final IntList entityIds;

    public ClientboundRemoveEntitiesPacket(IntList entityIds) {
        this.entityIds = new IntArrayList(entityIds);
    }

    public ClientboundRemoveEntitiesPacket(int... entityIds) {
        this.entityIds = new IntArrayList(entityIds);
    }

    public ClientboundRemoveEntitiesPacket(FriendlyByteBuf buf) {
        this.entityIds = buf.readIntIdList();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeIntIdList(this.entityIds);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleRemoveEntities(this);
    }

    public IntList getEntityIds() {
        return this.entityIds;
    }
}
