package net.minecraft.network.protocol.game;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public class ServerboundTeleportToEntityPacket implements Packet<ServerGamePacketListener> {
    private final UUID uuid;

    public ServerboundTeleportToEntityPacket(UUID targetUuid) {
        this.uuid = targetUuid;
    }

    public ServerboundTeleportToEntityPacket(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.uuid);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleTeleportToEntityPacket(this);
    }

    @Nullable
    public Entity getEntity(ServerLevel world) {
        return world.getEntity(this.uuid);
    }
}
