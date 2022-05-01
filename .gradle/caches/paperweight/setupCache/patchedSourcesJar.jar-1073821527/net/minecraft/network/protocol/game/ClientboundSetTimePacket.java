package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetTimePacket implements Packet<ClientGamePacketListener> {
    private final long gameTime;
    private final long dayTime;

    public ClientboundSetTimePacket(long time, long timeOfDay, boolean doDaylightCycle) {
        this.gameTime = time;
        long l = timeOfDay;
        if (!doDaylightCycle) {
            l = -timeOfDay;
            if (l == 0L) {
                l = -1L;
            }
        }

        this.dayTime = l;
    }

    public ClientboundSetTimePacket(FriendlyByteBuf buf) {
        this.gameTime = buf.readLong();
        this.dayTime = buf.readLong();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeLong(this.gameTime);
        buf.writeLong(this.dayTime);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetTime(this);
    }

    public long getGameTime() {
        return this.gameTime;
    }

    public long getDayTime() {
        return this.dayTime;
    }
}
