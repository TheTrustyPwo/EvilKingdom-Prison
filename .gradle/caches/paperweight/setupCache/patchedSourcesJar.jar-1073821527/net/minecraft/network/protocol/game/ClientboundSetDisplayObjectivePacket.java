package net.minecraft.network.protocol.game;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.scores.Objective;

public class ClientboundSetDisplayObjectivePacket implements Packet<ClientGamePacketListener> {
    private final int slot;
    private final String objectiveName;

    public ClientboundSetDisplayObjectivePacket(int slot, @Nullable Objective objective) {
        this.slot = slot;
        if (objective == null) {
            this.objectiveName = "";
        } else {
            this.objectiveName = objective.getName();
        }

    }

    public ClientboundSetDisplayObjectivePacket(FriendlyByteBuf buf) {
        this.slot = buf.readByte();
        this.objectiveName = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.slot);
        buf.writeUtf(this.objectiveName);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetDisplayObjective(this);
    }

    public int getSlot() {
        return this.slot;
    }

    @Nullable
    public String getObjectiveName() {
        return Objects.equals(this.objectiveName, "") ? null : this.objectiveName;
    }
}
