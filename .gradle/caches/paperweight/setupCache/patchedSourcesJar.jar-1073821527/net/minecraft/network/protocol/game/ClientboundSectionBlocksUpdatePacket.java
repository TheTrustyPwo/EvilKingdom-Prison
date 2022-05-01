package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class ClientboundSectionBlocksUpdatePacket implements Packet<ClientGamePacketListener> {
    private static final int POS_IN_SECTION_BITS = 12;
    private final SectionPos sectionPos;
    private final short[] positions;
    private final BlockState[] states;
    private final boolean suppressLightUpdates;

    public ClientboundSectionBlocksUpdatePacket(SectionPos sectionPos, ShortSet positions, LevelChunkSection section, boolean noLightingUpdates) {
        this.sectionPos = sectionPos;
        this.suppressLightUpdates = noLightingUpdates;
        int i = positions.size();
        this.positions = new short[i];
        this.states = new BlockState[i];
        int j = 0;

        for(short s : positions) {
            this.positions[j] = s;
            this.states[j] = section.getBlockState(SectionPos.sectionRelativeX(s), SectionPos.sectionRelativeY(s), SectionPos.sectionRelativeZ(s));
            ++j;
        }

    }

    public ClientboundSectionBlocksUpdatePacket(FriendlyByteBuf buf) {
        this.sectionPos = SectionPos.of(buf.readLong());
        this.suppressLightUpdates = buf.readBoolean();
        int i = buf.readVarInt();
        this.positions = new short[i];
        this.states = new BlockState[i];

        for(int j = 0; j < i; ++j) {
            long l = buf.readVarLong();
            this.positions[j] = (short)((int)(l & 4095L));
            this.states[j] = Block.BLOCK_STATE_REGISTRY.byId((int)(l >>> 12));
        }

    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeLong(this.sectionPos.asLong());
        buf.writeBoolean(this.suppressLightUpdates);
        buf.writeVarInt(this.positions.length);

        for(int i = 0; i < this.positions.length; ++i) {
            buf.writeVarLong((long)(Block.getId(this.states[i]) << 12 | this.positions[i]));
        }

    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleChunkBlocksUpdate(this);
    }

    public void runUpdates(BiConsumer<BlockPos, BlockState> biConsumer) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(int i = 0; i < this.positions.length; ++i) {
            short s = this.positions[i];
            mutableBlockPos.set(this.sectionPos.relativeToBlockX(s), this.sectionPos.relativeToBlockY(s), this.sectionPos.relativeToBlockZ(s));
            biConsumer.accept(mutableBlockPos, this.states[i]);
        }

    }

    public boolean shouldSuppressLightUpdates() {
        return this.suppressLightUpdates;
    }
}
