package net.minecraft.world.entity.decoration;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public class ItemFrame extends HangingEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_ROTATION = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.INT);
    public static final int NUM_ROTATIONS = 8;
    public float dropChance = 1.0F;
    public boolean fixed;

    public ItemFrame(EntityType<? extends ItemFrame> type, Level world) {
        super(type, world);
    }

    public ItemFrame(Level world, BlockPos pos, Direction facing) {
        this(EntityType.ITEM_FRAME, world, pos, facing);
    }

    public ItemFrame(EntityType<? extends ItemFrame> type, Level world, BlockPos pos, Direction facing) {
        super(type, world, pos);
        this.setDirection(facing);
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.0F;
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(DATA_ITEM, ItemStack.EMPTY);
        this.getEntityData().define(DATA_ROTATION, 0);
    }

    @Override
    public void setDirection(Direction facing) {
        Validate.notNull(facing);
        this.direction = facing;
        if (facing.getAxis().isHorizontal()) {
            this.setXRot(0.0F);
            this.setYRot((float)(this.direction.get2DDataValue() * 90));
        } else {
            this.setXRot((float)(-90 * facing.getAxisDirection().getStep()));
            this.setYRot(0.0F);
        }

        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.recalculateBoundingBox();
    }

    @Override
    protected void recalculateBoundingBox() {
        if (this.direction != null) {
            double d = 0.46875D;
            double e = (double)this.pos.getX() + 0.5D - (double)this.direction.getStepX() * 0.46875D;
            double f = (double)this.pos.getY() + 0.5D - (double)this.direction.getStepY() * 0.46875D;
            double g = (double)this.pos.getZ() + 0.5D - (double)this.direction.getStepZ() * 0.46875D;
            this.setPosRaw(e, f, g);
            double h = (double)this.getWidth();
            double i = (double)this.getHeight();
            double j = (double)this.getWidth();
            Direction.Axis axis = this.direction.getAxis();
            switch(axis) {
            case X:
                h = 1.0D;
                break;
            case Y:
                i = 1.0D;
                break;
            case Z:
                j = 1.0D;
            }

            h /= 32.0D;
            i /= 32.0D;
            j /= 32.0D;
            this.setBoundingBox(new AABB(e - h, f - i, g - j, e + h, f + i, g + j));
        }
    }

    @Override
    public boolean survives() {
        if (this.fixed) {
            return true;
        } else if (!this.level.noCollision(this)) {
            return false;
        } else {
            BlockState blockState = this.level.getBlockState(this.pos.relative(this.direction.getOpposite()));
            return blockState.getMaterial().isSolid() || this.direction.getAxis().isHorizontal() && DiodeBlock.isDiode(blockState) ? this.level.getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty() : false;
        }
    }

    @Override
    public void move(MoverType movementType, Vec3 movement) {
        if (!this.fixed) {
            super.move(movementType, movement);
        }

    }

    @Override
    public void push(double deltaX, double deltaY, double deltaZ) {
        if (!this.fixed) {
            super.push(deltaX, deltaY, deltaZ);
        }

    }

    @Override
    public float getPickRadius() {
        return 0.0F;
    }

    @Override
    public void kill() {
        this.removeFramedMap(this.getItem());
        super.kill();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.fixed) {
            return source != DamageSource.OUT_OF_WORLD && !source.isCreativePlayer() ? false : super.hurt(source, amount);
        } else if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!source.isExplosion() && !this.getItem().isEmpty()) {
            if (!this.level.isClientSide) {
                this.dropItem(source.getEntity(), false);
                this.playSound(this.getRemoveItemSound(), 1.0F, 1.0F);
            }

            return true;
        } else {
            return super.hurt(source, amount);
        }
    }

    public SoundEvent getRemoveItemSound() {
        return SoundEvents.ITEM_FRAME_REMOVE_ITEM;
    }

    @Override
    public int getWidth() {
        return 12;
    }

    @Override
    public int getHeight() {
        return 12;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = 16.0D;
        d *= 64.0D * getViewScale();
        return distance < d * d;
    }

    @Override
    public void dropItem(@Nullable Entity entity) {
        this.playSound(this.getBreakSound(), 1.0F, 1.0F);
        this.dropItem(entity, true);
    }

    public SoundEvent getBreakSound() {
        return SoundEvents.ITEM_FRAME_BREAK;
    }

    @Override
    public void playPlacementSound() {
        this.playSound(this.getPlaceSound(), 1.0F, 1.0F);
    }

    public SoundEvent getPlaceSound() {
        return SoundEvents.ITEM_FRAME_PLACE;
    }

    private void dropItem(@Nullable Entity entity, boolean alwaysDrop) {
        if (!this.fixed) {
            ItemStack itemStack = this.getItem();
            this.setItem(ItemStack.EMPTY);
            if (!this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                if (entity == null) {
                    this.removeFramedMap(itemStack);
                }

            } else {
                if (entity instanceof Player) {
                    Player player = (Player)entity;
                    if (player.getAbilities().instabuild) {
                        this.removeFramedMap(itemStack);
                        return;
                    }
                }

                if (alwaysDrop) {
                    this.spawnAtLocation(this.getFrameItemStack());
                }

                if (!itemStack.isEmpty()) {
                    itemStack = itemStack.copy();
                    this.removeFramedMap(itemStack);
                    if (this.random.nextFloat() < this.dropChance) {
                        this.spawnAtLocation(itemStack);
                    }
                }

            }
        }
    }

    private void removeFramedMap(ItemStack map) {
        if (map.is(Items.FILLED_MAP)) {
            MapItemSavedData mapItemSavedData = MapItem.getSavedData(map, this.level);
            if (mapItemSavedData != null) {
                mapItemSavedData.removedFromFrame(this.pos, this.getId());
                mapItemSavedData.setDirty(true);
            }
        }

        map.setEntityRepresentation((Entity)null);
    }

    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM);
    }

    public void setItem(ItemStack stack) {
        this.setItem(stack, true);
    }

    public void setItem(ItemStack value, boolean update) {
        if (!value.isEmpty()) {
            value = value.copy();
            value.setCount(1);
            value.setEntityRepresentation(this);
        }

        this.getEntityData().set(DATA_ITEM, value);
        if (!value.isEmpty()) {
            this.playSound(this.getAddItemSound(), 1.0F, 1.0F);
        }

        if (update && this.pos != null) {
            this.level.updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
        }

    }

    public SoundEvent getAddItemSound() {
        return SoundEvents.ITEM_FRAME_ADD_ITEM;
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        return mappedIndex == 0 ? new SlotAccess() {
            @Override
            public ItemStack get() {
                return ItemFrame.this.getItem();
            }

            @Override
            public boolean set(ItemStack stack) {
                ItemFrame.this.setItem(stack);
                return true;
            }
        } : super.getSlot(mappedIndex);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (data.equals(DATA_ITEM)) {
            ItemStack itemStack = this.getItem();
            if (!itemStack.isEmpty() && itemStack.getFrame() != this) {
                itemStack.setEntityRepresentation(this);
            }
        }

    }

    public int getRotation() {
        return this.getEntityData().get(DATA_ROTATION);
    }

    public void setRotation(int value) {
        this.setRotation(value, true);
    }

    private void setRotation(int value, boolean updateComparators) {
        this.getEntityData().set(DATA_ROTATION, value % 8);
        if (updateComparators && this.pos != null) {
            this.level.updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
        }

    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (!this.getItem().isEmpty()) {
            nbt.put("Item", this.getItem().save(new CompoundTag()));
            nbt.putByte("ItemRotation", (byte)this.getRotation());
            nbt.putFloat("ItemDropChance", this.dropChance);
        }

        nbt.putByte("Facing", (byte)this.direction.get3DDataValue());
        nbt.putBoolean("Invisible", this.isInvisible());
        nbt.putBoolean("Fixed", this.fixed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        CompoundTag compoundTag = nbt.getCompound("Item");
        if (compoundTag != null && !compoundTag.isEmpty()) {
            ItemStack itemStack = ItemStack.of(compoundTag);
            if (itemStack.isEmpty()) {
                LOGGER.warn("Unable to load item from: {}", (Object)compoundTag);
            }

            ItemStack itemStack2 = this.getItem();
            if (!itemStack2.isEmpty() && !ItemStack.matches(itemStack, itemStack2)) {
                this.removeFramedMap(itemStack2);
            }

            this.setItem(itemStack, false);
            this.setRotation(nbt.getByte("ItemRotation"), false);
            if (nbt.contains("ItemDropChance", 99)) {
                this.dropChance = nbt.getFloat("ItemDropChance");
            }
        }

        this.setDirection(Direction.from3DDataValue(nbt.getByte("Facing")));
        this.setInvisible(nbt.getBoolean("Invisible"));
        this.fixed = nbt.getBoolean("Fixed");
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        boolean bl = !this.getItem().isEmpty();
        boolean bl2 = !itemStack.isEmpty();
        if (this.fixed) {
            return InteractionResult.PASS;
        } else if (!this.level.isClientSide) {
            if (!bl) {
                if (bl2 && !this.isRemoved()) {
                    if (itemStack.is(Items.FILLED_MAP)) {
                        MapItemSavedData mapItemSavedData = MapItem.getSavedData(itemStack, this.level);
                        if (mapItemSavedData != null && mapItemSavedData.isTrackedCountOverLimit(256)) {
                            return InteractionResult.FAIL;
                        }
                    }

                    this.setItem(itemStack);
                    if (!player.getAbilities().instabuild) {
                        itemStack.shrink(1);
                    }
                }
            } else {
                this.playSound(this.getRotateItemSound(), 1.0F, 1.0F);
                this.setRotation(this.getRotation() + 1);
            }

            return InteractionResult.CONSUME;
        } else {
            return !bl && !bl2 ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
    }

    public SoundEvent getRotateItemSound() {
        return SoundEvents.ITEM_FRAME_ROTATE_ITEM;
    }

    public int getAnalogOutput() {
        return this.getItem().isEmpty() ? 0 : this.getRotation() % 8 + 1;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, this.getType(), this.direction.get3DDataValue(), this.getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        ItemStack itemStack = this.getItem();
        return itemStack.isEmpty() ? this.getFrameItemStack() : itemStack.copy();
    }

    protected ItemStack getFrameItemStack() {
        return new ItemStack(Items.ITEM_FRAME);
    }
}
