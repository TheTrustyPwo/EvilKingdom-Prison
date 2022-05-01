package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public abstract class BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockEntityType<?> type;
    @Nullable
    protected Level level;
    protected final BlockPos worldPosition;
    protected boolean remove;
    private BlockState blockState;

    public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this.type = type;
        this.worldPosition = pos.immutable();
        this.blockState = state;
    }

    public static BlockPos getPosFromTag(CompoundTag nbt) {
        return new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
    }

    @Nullable
    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level world) {
        this.level = world;
    }

    public boolean hasLevel() {
        return this.level != null;
    }

    public void load(CompoundTag nbt) {
    }

    protected void saveAdditional(CompoundTag nbt) {
    }

    public final CompoundTag saveWithFullMetadata() {
        CompoundTag compoundTag = this.saveWithoutMetadata();
        this.saveMetadata(compoundTag);
        return compoundTag;
    }

    public final CompoundTag saveWithId() {
        CompoundTag compoundTag = this.saveWithoutMetadata();
        this.saveId(compoundTag);
        return compoundTag;
    }

    public final CompoundTag saveWithoutMetadata() {
        CompoundTag compoundTag = new CompoundTag();
        this.saveAdditional(compoundTag);
        return compoundTag;
    }

    private void saveId(CompoundTag nbt) {
        ResourceLocation resourceLocation = BlockEntityType.getKey(this.getType());
        if (resourceLocation == null) {
            throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
        } else {
            nbt.putString("id", resourceLocation.toString());
        }
    }

    public static void addEntityType(CompoundTag nbt, BlockEntityType<?> type) {
        nbt.putString("id", BlockEntityType.getKey(type).toString());
    }

    public void saveToItem(ItemStack stack) {
        BlockItem.setBlockEntityData(stack, this.getType(), this.saveWithoutMetadata());
    }

    private void saveMetadata(CompoundTag nbt) {
        this.saveId(nbt);
        nbt.putInt("x", this.worldPosition.getX());
        nbt.putInt("y", this.worldPosition.getY());
        nbt.putInt("z", this.worldPosition.getZ());
    }

    @Nullable
    public static BlockEntity loadStatic(BlockPos pos, BlockState state, CompoundTag nbt) {
        String string = nbt.getString("id");
        ResourceLocation resourceLocation = ResourceLocation.tryParse(string);
        if (resourceLocation == null) {
            LOGGER.error("Block entity has invalid type: {}", (Object)string);
            return null;
        } else {
            return Registry.BLOCK_ENTITY_TYPE.getOptional(resourceLocation).map((type) -> {
                try {
                    return type.create(pos, state);
                } catch (Throwable var5) {
                    LOGGER.error("Failed to create block entity {}", string, var5);
                    return null;
                }
            }).map((blockEntity) -> {
                try {
                    blockEntity.load(nbt);
                    return blockEntity;
                } catch (Throwable var4) {
                    LOGGER.error("Failed to load data for block entity {}", string, var4);
                    return null;
                }
            }).orElseGet(() -> {
                LOGGER.warn("Skipping BlockEntity with id {}", (Object)string);
                return null;
            });
        }
    }

    public void setChanged() {
        if (this.level != null) {
            setChanged(this.level, this.worldPosition, this.blockState);
        }

    }

    protected static void setChanged(Level world, BlockPos pos, BlockState state) {
        world.blockEntityChanged(pos);
        if (!state.isAir()) {
            world.updateNeighbourForOutputSignal(pos, state.getBlock());
        }

    }

    public BlockPos getBlockPos() {
        return this.worldPosition;
    }

    public BlockState getBlockState() {
        return this.blockState;
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return null;
    }

    public CompoundTag getUpdateTag() {
        return new CompoundTag();
    }

    public boolean isRemoved() {
        return this.remove;
    }

    public void setRemoved() {
        this.remove = true;
    }

    public void clearRemoved() {
        this.remove = false;
    }

    public boolean triggerEvent(int type, int data) {
        return false;
    }

    public void fillCrashReportCategory(CrashReportCategory crashReportSection) {
        crashReportSection.setDetail("Name", () -> {
            return Registry.BLOCK_ENTITY_TYPE.getKey(this.getType()) + " // " + this.getClass().getCanonicalName();
        });
        if (this.level != null) {
            CrashReportCategory.populateBlockDetails(crashReportSection, this.level, this.worldPosition, this.getBlockState());
            CrashReportCategory.populateBlockDetails(crashReportSection, this.level, this.worldPosition, this.level.getBlockState(this.worldPosition));
        }
    }

    public boolean onlyOpCanSetNbt() {
        return false;
    }

    public BlockEntityType<?> getType() {
        return this.type;
    }

    /** @deprecated */
    @Deprecated
    public void setBlockState(BlockState state) {
        this.blockState = state;
    }
}
