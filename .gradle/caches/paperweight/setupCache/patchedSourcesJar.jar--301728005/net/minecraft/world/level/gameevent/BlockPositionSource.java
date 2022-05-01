package net.minecraft.world.level.gameevent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

public class BlockPositionSource implements PositionSource {
    public static final Codec<BlockPositionSource> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockPos.CODEC.fieldOf("pos").xmap(Optional::of, Optional::get).forGetter((blockPositionSource) -> {
            return blockPositionSource.pos;
        })).apply(instance, BlockPositionSource::new);
    });
    final Optional<BlockPos> pos;

    public BlockPositionSource(BlockPos pos) {
        this(Optional.of(pos));
    }

    public BlockPositionSource(Optional<BlockPos> pos) {
        this.pos = pos;
    }

    @Override
    public Optional<BlockPos> getPosition(Level world) {
        return this.pos;
    }

    @Override
    public PositionSourceType<?> getType() {
        return PositionSourceType.BLOCK;
    }

    public static class Type implements PositionSourceType<BlockPositionSource> {
        @Override
        public BlockPositionSource read(FriendlyByteBuf friendlyByteBuf) {
            return new BlockPositionSource(Optional.of(friendlyByteBuf.readBlockPos()));
        }

        @Override
        public void write(FriendlyByteBuf buf, BlockPositionSource positionSource) {
            positionSource.pos.ifPresent(buf::writeBlockPos);
        }

        @Override
        public Codec<BlockPositionSource> codec() {
            return BlockPositionSource.CODEC;
        }
    }
}
