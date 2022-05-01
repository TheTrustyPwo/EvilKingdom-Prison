package net.minecraft.network.protocol.game;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ClientboundBlockEntityDataPacket implements Packet<ClientGamePacketListener> {
    private final BlockPos pos;
    private final BlockEntityType<?> type;
    @Nullable
    private final CompoundTag tag;

    public static ClientboundBlockEntityDataPacket create(BlockEntity blockEntity, Function<BlockEntity, CompoundTag> nbtGetter) {
        return new ClientboundBlockEntityDataPacket(blockEntity.getBlockPos(), blockEntity.getType(), nbtGetter.apply(blockEntity));
    }

    public static ClientboundBlockEntityDataPacket create(BlockEntity blockEntity) {
        return create(blockEntity, BlockEntity::getUpdateTag);
    }

    private ClientboundBlockEntityDataPacket(BlockPos pos, BlockEntityType<?> blockEntityType, CompoundTag nbt) {
        this.pos = pos;
        this.type = blockEntityType;
        this.tag = nbt.isEmpty() ? null : nbt;
    }

    public ClientboundBlockEntityDataPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.type = Registry.BLOCK_ENTITY_TYPE.byId(buf.readVarInt());
        this.tag = buf.readNbt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(Registry.BLOCK_ENTITY_TYPE.getId(this.type));
        buf.writeNbt(this.tag);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleBlockEntityData(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public BlockEntityType<?> getType() {
        return this.type;
    }

    @Nullable
    public CompoundTag getTag() {
        return this.tag;
    }
}
