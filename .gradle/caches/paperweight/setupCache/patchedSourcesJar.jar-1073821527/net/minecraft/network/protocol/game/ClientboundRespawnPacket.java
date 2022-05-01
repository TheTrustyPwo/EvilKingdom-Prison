package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ClientboundRespawnPacket implements Packet<ClientGamePacketListener> {
    private final Holder<DimensionType> dimensionType;
    private final ResourceKey<Level> dimension;
    private final long seed;
    private final GameType playerGameType;
    @Nullable
    private final GameType previousPlayerGameType;
    private final boolean isDebug;
    private final boolean isFlat;
    private final boolean keepAllPlayerData;

    public ClientboundRespawnPacket(Holder<DimensionType> holder, ResourceKey<Level> dimension, long sha256Seed, GameType gameMode, @Nullable GameType previousGameMode, boolean debugWorld, boolean flatWorld, boolean keepPlayerAttributes) {
        this.dimensionType = holder;
        this.dimension = dimension;
        this.seed = sha256Seed;
        this.playerGameType = gameMode;
        this.previousPlayerGameType = previousGameMode;
        this.isDebug = debugWorld;
        this.isFlat = flatWorld;
        this.keepAllPlayerData = keepPlayerAttributes;
    }

    public ClientboundRespawnPacket(FriendlyByteBuf buf) {
        this.dimensionType = buf.readWithCodec(DimensionType.CODEC);
        this.dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
        this.seed = buf.readLong();
        this.playerGameType = GameType.byId(buf.readUnsignedByte());
        this.previousPlayerGameType = GameType.byNullableId(buf.readByte());
        this.isDebug = buf.readBoolean();
        this.isFlat = buf.readBoolean();
        this.keepAllPlayerData = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeWithCodec(DimensionType.CODEC, this.dimensionType);
        buf.writeResourceLocation(this.dimension.location());
        buf.writeLong(this.seed);
        buf.writeByte(this.playerGameType.getId());
        buf.writeByte(GameType.getNullableId(this.previousPlayerGameType));
        buf.writeBoolean(this.isDebug);
        buf.writeBoolean(this.isFlat);
        buf.writeBoolean(this.keepAllPlayerData);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleRespawn(this);
    }

    public Holder<DimensionType> getDimensionType() {
        return this.dimensionType;
    }

    public ResourceKey<Level> getDimension() {
        return this.dimension;
    }

    public long getSeed() {
        return this.seed;
    }

    public GameType getPlayerGameType() {
        return this.playerGameType;
    }

    @Nullable
    public GameType getPreviousPlayerGameType() {
        return this.previousPlayerGameType;
    }

    public boolean isDebug() {
        return this.isDebug;
    }

    public boolean isFlat() {
        return this.isFlat;
    }

    public boolean shouldKeepAllPlayerData() {
        return this.keepAllPlayerData;
    }
}
