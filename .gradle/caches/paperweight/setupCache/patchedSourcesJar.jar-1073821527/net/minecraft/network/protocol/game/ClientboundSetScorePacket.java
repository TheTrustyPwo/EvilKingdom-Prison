package net.minecraft.network.protocol.game;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.ServerScoreboard;

public class ClientboundSetScorePacket implements Packet<ClientGamePacketListener> {
    private final String owner;
    @Nullable
    private final String objectiveName;
    private final int score;
    private final ServerScoreboard.Method method;

    public ClientboundSetScorePacket(ServerScoreboard.Method mode, @Nullable String objectiveName, String playerName, int score) {
        if (mode != ServerScoreboard.Method.REMOVE && objectiveName == null) {
            throw new IllegalArgumentException("Need an objective name");
        } else {
            this.owner = playerName;
            this.objectiveName = objectiveName;
            this.score = score;
            this.method = mode;
        }
    }

    public ClientboundSetScorePacket(FriendlyByteBuf buf) {
        this.owner = buf.readUtf();
        this.method = buf.readEnum(ServerScoreboard.Method.class);
        String string = buf.readUtf();
        this.objectiveName = Objects.equals(string, "") ? null : string;
        if (this.method != ServerScoreboard.Method.REMOVE) {
            this.score = buf.readVarInt();
        } else {
            this.score = 0;
        }

    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.owner);
        buf.writeEnum(this.method);
        buf.writeUtf(this.objectiveName == null ? "" : this.objectiveName);
        if (this.method != ServerScoreboard.Method.REMOVE) {
            buf.writeVarInt(this.score);
        }

    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetScore(this);
    }

    public String getOwner() {
        return this.owner;
    }

    @Nullable
    public String getObjectiveName() {
        return this.objectiveName;
    }

    public int getScore() {
        return this.score;
    }

    public ServerScoreboard.Method getMethod() {
        return this.method;
    }
}
