package net.minecraft.world.level.block.entity;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SkullBlockEntity extends BlockEntity {
    public static final String TAG_SKULL_OWNER = "SkullOwner";
    @Nullable
    private static GameProfileCache profileCache;
    @Nullable
    private static MinecraftSessionService sessionService;
    @Nullable
    private static Executor mainThreadExecutor;
    @Nullable
    public GameProfile owner;
    private int mouthTickCount;
    private boolean isMovingMouth;

    public SkullBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.SKULL, pos, state);
    }

    public static void setup(GameProfileCache userCache, MinecraftSessionService sessionService, Executor executor) {
        profileCache = userCache;
        SkullBlockEntity.sessionService = sessionService;
        mainThreadExecutor = executor;
    }

    public static void clear() {
        profileCache = null;
        sessionService = null;
        mainThreadExecutor = null;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (this.owner != null) {
            CompoundTag compoundTag = new CompoundTag();
            NbtUtils.writeGameProfile(compoundTag, this.owner);
            nbt.put("SkullOwner", compoundTag);
        }

    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("SkullOwner", 10)) {
            this.setOwner(NbtUtils.readGameProfile(nbt.getCompound("SkullOwner")));
        } else if (nbt.contains("ExtraType", 8)) {
            String string = nbt.getString("ExtraType");
            if (!StringUtil.isNullOrEmpty(string)) {
                this.setOwner(new GameProfile((UUID)null, string));
            }
        }

    }

    public static void dragonHeadAnimation(Level world, BlockPos pos, BlockState state, SkullBlockEntity blockEntity) {
        if (world.hasNeighborSignal(pos)) {
            blockEntity.isMovingMouth = true;
            ++blockEntity.mouthTickCount;
        } else {
            blockEntity.isMovingMouth = false;
        }

    }

    public float getMouthAnimation(float tickDelta) {
        return this.isMovingMouth ? (float)this.mouthTickCount + tickDelta : (float)this.mouthTickCount;
    }

    @Nullable
    public GameProfile getOwnerProfile() {
        return this.owner;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, e -> sanitizeTileEntityUUID(e.getUpdateTag())); // Paper
    }

    // Paper start
    public static CompoundTag sanitizeTileEntityUUID(CompoundTag cmp) {
        CompoundTag owner = cmp.getCompound("Owner");
        if (!owner.isEmpty()) {
            sanitizeUUID(owner);
        }
        return cmp;
    }

    public static void sanitizeUUID(CompoundTag owner) {
        CompoundTag properties = owner.getCompound("Properties");
        ListTag list = null;
        if (!properties.isEmpty()) {
            list = properties.getList("textures", 10);
        }

        if (list != null && !list.isEmpty()) {
            String textures = ((CompoundTag)list.get(0)).getString("Value");
            if (textures != null && textures.length() > 3) {
                UUID uuid = UUID.nameUUIDFromBytes(textures.getBytes());
                owner.putUUID("Id", uuid);
                return;
            }
        }
        owner.putUUID("Id", UUID.randomUUID());
    }
    // Paper end

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public void setOwner(@Nullable GameProfile owner) {
        synchronized(this) {
            this.owner = owner;
        }

        this.updateOwnerProfile();
    }

    private void updateOwnerProfile() {
        updateGameprofile(this.owner, (owner) -> {
            this.owner = owner;
            this.setChanged();
            // Paper start
        }, () -> {
            final @Nullable Level level = this.getLevel();
            return "SkullBlockEntity at " + this.getBlockPos() + (level == null ? "" : (" in level: " + level.dimension().location()));
            // Paper end
        });
    }

    public static void updateGameprofile(@Nullable GameProfile owner, Consumer<GameProfile> callback) {
        // Paper start
        updateGameprofile(owner, callback, null);
    }

    private static final boolean DEBUG_INVALID_SKULL_PROFILES = Boolean.getBoolean("Paper.debugInvalidSkullProfiles");

    public static void updateGameprofile(@Nullable GameProfile owner, Consumer<GameProfile> callback, final @Nullable java.util.function.Supplier<String> debugInfo) {
        if (owner != null && !StringUtil.isNullOrEmpty(owner.getName()) && (!owner.isComplete() || !owner.getProperties().containsKey("textures")) && profileCache != null && sessionService != null) {
            final @Nullable Throwable trace = DEBUG_INVALID_SKULL_PROFILES ? new Throwable("updateGameprofile caller debug trace") : null;
            profileCache.getAsync(owner.getName(), (profile) -> {
                Util.PROFILE_EXECUTOR.execute(() -> { // Paper - not a good idea to use BLOCKING OPERATIONS on the worldgen executor
                    try {
                    // Paper end
                    Util.ifElse(profile, (profilex) -> {
                        Property property = Iterables.getFirst(profilex.getProperties().get("textures"), (Property)null);
                        if (property == null) {
                            profilex = sessionService.fillProfileProperties(profilex, true);
                        }

                        GameProfile gameProfile = profilex;
                        mainThreadExecutor.execute(() -> {
                            profileCache.add(gameProfile);
                            callback.accept(gameProfile);
                        });
                    }, () -> {
                        mainThreadExecutor.execute(() -> {
                            callback.accept(owner);
                        });
                    });
                    // Paper start
                    } catch (final Exception ex) {
                        if (trace != null) {
                            ex.addSuppressed(trace);
                        }
                        final String ownerMessage = "Original profile: '" + owner + "'";
                        final String debugMessage = " Run with -DPaper.debugInvalidSkullProfiles=true for further debug information.";
                        final String message = ownerMessage + (trace == null ? debugMessage : "");
                        if (debugInfo == null) {
                            throw new RuntimeException(message, ex);
                        }
                        throw new RuntimeException(debugInfo.get() + " " + message, ex);
                    }
                    // Paper end
                });
            });
        } else {
            callback.accept(owner);
        }
    }
}
