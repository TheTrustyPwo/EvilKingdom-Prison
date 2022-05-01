package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ClientboundSetCameraPacket implements Packet<ClientGamePacketListener> {
    private final int cameraId;

    public ClientboundSetCameraPacket(Entity entity) {
        this.cameraId = entity.getId();
    }

    public ClientboundSetCameraPacket(FriendlyByteBuf buf) {
        this.cameraId = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.cameraId);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetCamera(this);
    }

    @Nullable
    public Entity getEntity(Level world) {
        return world.getEntity(this.cameraId);
    }
}
