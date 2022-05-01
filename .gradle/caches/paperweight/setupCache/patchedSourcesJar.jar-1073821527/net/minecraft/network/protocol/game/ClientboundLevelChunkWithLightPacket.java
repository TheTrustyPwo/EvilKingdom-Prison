package net.minecraft.network.protocol.game;

import java.util.BitSet;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ClientboundLevelChunkWithLightPacket implements Packet<ClientGamePacketListener> {
    private final int x;
    private final int z;
    private final ClientboundLevelChunkPacketData chunkData;
    private final ClientboundLightUpdatePacketData lightData;

    public ClientboundLevelChunkWithLightPacket(LevelChunk chunk, LevelLightEngine lightProvider, @Nullable BitSet skyBits, @Nullable BitSet blockBits, boolean nonEdge) {
        ChunkPos chunkPos = chunk.getPos();
        this.x = chunkPos.x;
        this.z = chunkPos.z;
        this.chunkData = new ClientboundLevelChunkPacketData(chunk);
        this.lightData = new ClientboundLightUpdatePacketData(chunkPos, lightProvider, skyBits, blockBits, nonEdge);
    }

    public ClientboundLevelChunkWithLightPacket(FriendlyByteBuf buf) {
        this.x = buf.readInt();
        this.z = buf.readInt();
        this.chunkData = new ClientboundLevelChunkPacketData(buf, this.x, this.z);
        this.lightData = new ClientboundLightUpdatePacketData(buf, this.x, this.z);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.z);
        this.chunkData.write(buf);
        this.lightData.write(buf);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleLevelChunkWithLight(this);
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public ClientboundLevelChunkPacketData getChunkData() {
        return this.chunkData;
    }

    public ClientboundLightUpdatePacketData getLightData() {
        return this.lightData;
    }
}
