package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.PositionSourceType;

public class VibrationPath {
    public static final Codec<VibrationPath> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockPos.CODEC.fieldOf("origin").forGetter((vibrationPath) -> {
            return vibrationPath.origin;
        }), PositionSource.CODEC.fieldOf("destination").forGetter((vibrationPath) -> {
            return vibrationPath.destination;
        }), Codec.INT.fieldOf("arrival_in_ticks").forGetter((vibrationPath) -> {
            return vibrationPath.arrivalInTicks;
        })).apply(instance, VibrationPath::new);
    });
    private final BlockPos origin;
    private final PositionSource destination;
    private final int arrivalInTicks;

    public VibrationPath(BlockPos origin, PositionSource destination, int arrivalInTicks) {
        this.origin = origin;
        this.destination = destination;
        this.arrivalInTicks = arrivalInTicks;
    }

    public int getArrivalInTicks() {
        return this.arrivalInTicks;
    }

    public BlockPos getOrigin() {
        return this.origin;
    }

    public PositionSource getDestination() {
        return this.destination;
    }

    public static VibrationPath read(FriendlyByteBuf buf) {
        BlockPos blockPos = buf.readBlockPos();
        PositionSource positionSource = PositionSourceType.fromNetwork(buf);
        int i = buf.readVarInt();
        return new VibrationPath(blockPos, positionSource, i);
    }

    public static void write(FriendlyByteBuf buf, VibrationPath vibration) {
        buf.writeBlockPos(vibration.origin);
        PositionSourceType.toNetwork(vibration.destination, buf);
        buf.writeVarInt(vibration.arrivalInTicks);
    }
}
