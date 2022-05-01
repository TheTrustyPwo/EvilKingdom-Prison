package net.minecraft.network.protocol.game;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.SynchedEntityData;

public class ClientboundSetEntityDataPacket implements Packet<ClientGamePacketListener> {
    private final int id;
    @Nullable
    private final List<SynchedEntityData.DataItem<?>> packedItems;

    public ClientboundSetEntityDataPacket(int id, SynchedEntityData tracker, boolean forceUpdateAll) {
        this.id = id;
        if (forceUpdateAll) {
            this.packedItems = tracker.getAll();
            tracker.clearDirty();
        } else {
            this.packedItems = tracker.packDirty();
        }

    }

    public ClientboundSetEntityDataPacket(FriendlyByteBuf buf) {
        this.id = buf.readVarInt();
        this.packedItems = SynchedEntityData.unpack(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        SynchedEntityData.pack(this.packedItems, buf);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetEntityData(this);
    }

    @Nullable
    public List<SynchedEntityData.DataItem<?>> getUnpackedData() {
        return this.packedItems;
    }

    public int getId() {
        return this.id;
    }
}
