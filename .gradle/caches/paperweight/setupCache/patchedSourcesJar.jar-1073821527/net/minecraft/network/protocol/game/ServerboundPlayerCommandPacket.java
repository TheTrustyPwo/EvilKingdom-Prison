package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;

public class ServerboundPlayerCommandPacket implements Packet<ServerGamePacketListener> {
    private final int id;
    private final ServerboundPlayerCommandPacket.Action action;
    private final int data;

    public ServerboundPlayerCommandPacket(Entity entity, ServerboundPlayerCommandPacket.Action mode) {
        this(entity, mode, 0);
    }

    public ServerboundPlayerCommandPacket(Entity entity, ServerboundPlayerCommandPacket.Action mode, int mountJumpHeight) {
        this.id = entity.getId();
        this.action = mode;
        this.data = mountJumpHeight;
    }

    public ServerboundPlayerCommandPacket(FriendlyByteBuf buf) {
        this.id = buf.readVarInt();
        this.action = buf.readEnum(ServerboundPlayerCommandPacket.Action.class);
        this.data = buf.readVarInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        buf.writeEnum(this.action);
        buf.writeVarInt(this.data);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handlePlayerCommand(this);
    }

    public int getId() {
        return this.id;
    }

    public ServerboundPlayerCommandPacket.Action getAction() {
        return this.action;
    }

    public int getData() {
        return this.data;
    }

    public static enum Action {
        PRESS_SHIFT_KEY,
        RELEASE_SHIFT_KEY,
        STOP_SLEEPING,
        START_SPRINTING,
        STOP_SPRINTING,
        START_RIDING_JUMP,
        STOP_RIDING_JUMP,
        OPEN_INVENTORY,
        START_FALL_FLYING;
    }
}
