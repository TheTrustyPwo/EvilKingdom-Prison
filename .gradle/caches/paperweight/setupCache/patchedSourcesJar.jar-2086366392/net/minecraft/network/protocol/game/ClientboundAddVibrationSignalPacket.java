package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.gameevent.vibrations.VibrationPath;

public class ClientboundAddVibrationSignalPacket implements Packet<ClientGamePacketListener> {
    private final VibrationPath vibrationPath;

    public ClientboundAddVibrationSignalPacket(VibrationPath vibration) {
        this.vibrationPath = vibration;
    }

    public ClientboundAddVibrationSignalPacket(FriendlyByteBuf buf) {
        this.vibrationPath = VibrationPath.read(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        VibrationPath.write(buf, this.vibrationPath);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleAddVibrationSignal(this);
    }

    public VibrationPath getVibrationPath() {
        return this.vibrationPath;
    }
}
