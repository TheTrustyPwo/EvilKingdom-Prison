package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundPlayerInputPacket implements Packet<ServerGamePacketListener> {
    private static final int FLAG_JUMPING = 1;
    private static final int FLAG_SHIFT_KEY_DOWN = 2;
    private final float xxa;
    private final float zza;
    private final boolean isJumping;
    private final boolean isShiftKeyDown;

    public ServerboundPlayerInputPacket(float sideways, float forward, boolean jumping, boolean sneaking) {
        this.xxa = sideways;
        this.zza = forward;
        this.isJumping = jumping;
        this.isShiftKeyDown = sneaking;
    }

    public ServerboundPlayerInputPacket(FriendlyByteBuf buf) {
        this.xxa = buf.readFloat();
        this.zza = buf.readFloat();
        byte b = buf.readByte();
        this.isJumping = (b & 1) > 0;
        this.isShiftKeyDown = (b & 2) > 0;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(this.xxa);
        buf.writeFloat(this.zza);
        byte b = 0;
        if (this.isJumping) {
            b = (byte)(b | 1);
        }

        if (this.isShiftKeyDown) {
            b = (byte)(b | 2);
        }

        buf.writeByte(b);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handlePlayerInput(this);
    }

    public float getXxa() {
        return this.xxa;
    }

    public float getZza() {
        return this.zza;
    }

    public boolean isJumping() {
        return this.isJumping;
    }

    public boolean isShiftKeyDown() {
        return this.isShiftKeyDown;
    }
}
