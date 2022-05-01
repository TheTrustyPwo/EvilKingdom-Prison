package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BeehiveBlockEntity extends BlockEntity {
    public static final String TAG_FLOWER_POS = "FlowerPos";
    public static final String MIN_OCCUPATION_TICKS = "MinOccupationTicks";
    public static final String ENTITY_DATA = "EntityData";
    public static final String TICKS_IN_HIVE = "TicksInHive";
    public static final String HAS_NECTAR = "HasNectar";
    public static final String BEES = "Bees";
    private static final List<String> IGNORED_BEE_TAGS = Arrays.asList("Air", "ArmorDropChances", "ArmorItems", "Brain", "CanPickUpLoot", "DeathTime", "FallDistance", "FallFlying", "Fire", "HandDropChances", "HandItems", "HurtByTimestamp", "HurtTime", "LeftHanded", "Motion", "NoGravity", "OnGround", "PortalCooldown", "Pos", "Rotation", "CannotEnterHiveTicks", "TicksSincePollination", "CropsGrownSincePollination", "HivePos", "Passengers", "Leash", "UUID");
    public static final int MAX_OCCUPANTS = 3;
    private static final int MIN_TICKS_BEFORE_REENTERING_HIVE = 400;
    private static final int MIN_OCCUPATION_TICKS_NECTAR = 2400;
    public static final int MIN_OCCUPATION_TICKS_NECTARLESS = 600;
    private final List<BeehiveBlockEntity.BeeData> stored = Lists.newArrayList();
    @Nullable
    public BlockPos savedFlowerPos;

    public BeehiveBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.BEEHIVE, pos, state);
    }

    @Override
    public void setChanged() {
        if (this.isFireNearby()) {
            this.emptyAllLivingFromHive((Player)null, this.level.getBlockState(this.getBlockPos()), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
        }

        super.setChanged();
    }

    public boolean isFireNearby() {
        if (this.level == null) {
            return false;
        } else {
            for(BlockPos blockPos : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
                if (this.level.getBlockState(blockPos).getBlock() instanceof FireBlock) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isEmpty() {
        return this.stored.isEmpty();
    }

    public boolean isFull() {
        return this.stored.size() == 3;
    }

    public void emptyAllLivingFromHive(@Nullable Player player, BlockState state, BeehiveBlockEntity.BeeReleaseStatus beeState) {
        List<Entity> list = this.releaseAllOccupants(state, beeState);
        if (player != null) {
            for(Entity entity : list) {
                if (entity instanceof Bee) {
                    Bee bee = (Bee)entity;
                    if (player.position().distanceToSqr(entity.position()) <= 16.0D) {
                        if (!this.isSedated()) {
                            bee.setTarget(player);
                        } else {
                            bee.setStayOutOfHiveCountdown(400);
                        }
                    }
                }
            }
        }

    }

    private List<Entity> releaseAllOccupants(BlockState state, BeehiveBlockEntity.BeeReleaseStatus beeState) {
        List<Entity> list = Lists.newArrayList();
        this.stored.removeIf((bee) -> {
            return releaseOccupant(this.level, this.worldPosition, state, bee, list, beeState, this.savedFlowerPos);
        });
        if (!list.isEmpty()) {
            super.setChanged();
        }

        return list;
    }

    public void addOccupant(Entity entity, boolean hasNectar) {
        this.addOccupantWithPresetTicks(entity, hasNectar, 0);
    }

    @VisibleForDebug
    public int getOccupantCount() {
        return this.stored.size();
    }

    public static int getHoneyLevel(BlockState state) {
        return state.getValue(BeehiveBlock.HONEY_LEVEL);
    }

    @VisibleForDebug
    public boolean isSedated() {
        return CampfireBlock.isSmokeyPos(this.level, this.getBlockPos());
    }

    public void addOccupantWithPresetTicks(Entity entity, boolean hasNectar, int ticksInHive) {
        if (this.stored.size() < 3) {
            entity.stopRiding();
            entity.ejectPassengers();
            CompoundTag compoundTag = new CompoundTag();
            entity.save(compoundTag);
            this.storeBee(compoundTag, ticksInHive, hasNectar);
            if (this.level != null) {
                if (entity instanceof Bee) {
                    Bee bee = (Bee)entity;
                    if (bee.hasSavedFlowerPos() && (!this.hasSavedFlowerPos() || this.level.random.nextBoolean())) {
                        this.savedFlowerPos = bee.getSavedFlowerPos();
                    }
                }

                BlockPos blockPos = this.getBlockPos();
                this.level.playSound((Player)null, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            entity.discard();
            super.setChanged();
        }
    }

    public void storeBee(CompoundTag nbtCompound, int ticksInHive, boolean hasNectar) {
        this.stored.add(new BeehiveBlockEntity.BeeData(nbtCompound, ticksInHive, hasNectar ? 2400 : 600));
    }

    private static boolean releaseOccupant(Level world, BlockPos pos, BlockState state, BeehiveBlockEntity.BeeData bee, @Nullable List<Entity> entities, BeehiveBlockEntity.BeeReleaseStatus beeState, @Nullable BlockPos flowerPos) {
        if ((world.isNight() || world.isRaining()) && beeState != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
            return false;
        } else {
            CompoundTag compoundTag = bee.entityData.copy();
            removeIgnoredBeeTags(compoundTag);
            compoundTag.put("HivePos", NbtUtils.writeBlockPos(pos));
            compoundTag.putBoolean("NoGravity", true);
            Direction direction = state.getValue(BeehiveBlock.FACING);
            BlockPos blockPos = pos.relative(direction);
            boolean bl = !world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty();
            if (bl && beeState != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
                return false;
            } else {
                Entity entity = EntityType.loadEntityRecursive(compoundTag, world, (entityx) -> {
                    return entityx;
                });
                if (entity != null) {
                    if (!entity.getType().is(EntityTypeTags.BEEHIVE_INHABITORS)) {
                        return false;
                    } else {
                        if (entity instanceof Bee) {
                            Bee bee2 = (Bee)entity;
                            if (flowerPos != null && !bee2.hasSavedFlowerPos() && world.random.nextFloat() < 0.9F) {
                                bee2.setSavedFlowerPos(flowerPos);
                            }

                            if (beeState == BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED) {
                                bee2.dropOffNectar();
                                if (state.is(BlockTags.BEEHIVES, (statex) -> {
                                    return statex.hasProperty(BeehiveBlock.HONEY_LEVEL);
                                })) {
                                    int i = getHoneyLevel(state);
                                    if (i < 5) {
                                        int j = world.random.nextInt(100) == 0 ? 2 : 1;
                                        if (i + j > 5) {
                                            --j;
                                        }

                                        world.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, Integer.valueOf(i + j)));
                                    }
                                }
                            }

                            setBeeReleaseData(bee.ticksInHive, bee2);
                            if (entities != null) {
                                entities.add(bee2);
                            }

                            float f = entity.getBbWidth();
                            double d = bl ? 0.0D : 0.55D + (double)(f / 2.0F);
                            double e = (double)pos.getX() + 0.5D + d * (double)direction.getStepX();
                            double g = (double)pos.getY() + 0.5D - (double)(entity.getBbHeight() / 2.0F);
                            double h = (double)pos.getZ() + 0.5D + d * (double)direction.getStepZ();
                            entity.moveTo(e, g, h, entity.getYRot(), entity.getXRot());
                        }

                        world.playSound((Player)null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
                        return world.addFreshEntity(entity);
                    }
                } else {
                    return false;
                }
            }
        }
    }

    static void removeIgnoredBeeTags(CompoundTag compound) {
        for(String string : IGNORED_BEE_TAGS) {
            compound.remove(string);
        }

    }

    private static void setBeeReleaseData(int ticks, Bee bee) {
        int i = bee.getAge();
        if (i < 0) {
            bee.setAge(Math.min(0, i + ticks));
        } else if (i > 0) {
            bee.setAge(Math.max(0, i - ticks));
        }

        bee.setInLoveTime(Math.max(0, bee.getInLoveTime() - ticks));
    }

    private boolean hasSavedFlowerPos() {
        return this.savedFlowerPos != null;
    }

    private static void tickOccupants(Level world, BlockPos pos, BlockState state, List<BeehiveBlockEntity.BeeData> bees, @Nullable BlockPos flowerPos) {
        boolean bl = false;

        BeehiveBlockEntity.BeeData beeData;
        for(Iterator<BeehiveBlockEntity.BeeData> iterator = bees.iterator(); iterator.hasNext(); ++beeData.ticksInHive) {
            beeData = iterator.next();
            if (beeData.ticksInHive > beeData.minOccupationTicks) {
                BeehiveBlockEntity.BeeReleaseStatus beeReleaseStatus = beeData.entityData.getBoolean("HasNectar") ? BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED : BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED;
                if (releaseOccupant(world, pos, state, beeData, (List<Entity>)null, beeReleaseStatus, flowerPos)) {
                    bl = true;
                    iterator.remove();
                }
            }
        }

        if (bl) {
            setChanged(world, pos, state);
        }

    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, BeehiveBlockEntity blockEntity) {
        tickOccupants(world, pos, state, blockEntity.stored, blockEntity.savedFlowerPos);
        if (!blockEntity.stored.isEmpty() && world.getRandom().nextDouble() < 0.005D) {
            double d = (double)pos.getX() + 0.5D;
            double e = (double)pos.getY();
            double f = (double)pos.getZ() + 0.5D;
            world.playSound((Player)null, d, e, f, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        DebugPackets.sendHiveInfo(world, pos, state, blockEntity);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.stored.clear();
        ListTag listTag = nbt.getList("Bees", 10);

        for(int i = 0; i < listTag.size(); ++i) {
            CompoundTag compoundTag = listTag.getCompound(i);
            BeehiveBlockEntity.BeeData beeData = new BeehiveBlockEntity.BeeData(compoundTag.getCompound("EntityData"), compoundTag.getInt("TicksInHive"), compoundTag.getInt("MinOccupationTicks"));
            this.stored.add(beeData);
        }

        this.savedFlowerPos = null;
        if (nbt.contains("FlowerPos")) {
            this.savedFlowerPos = NbtUtils.readBlockPos(nbt.getCompound("FlowerPos"));
        }

    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("Bees", this.writeBees());
        if (this.hasSavedFlowerPos()) {
            nbt.put("FlowerPos", NbtUtils.writeBlockPos(this.savedFlowerPos));
        }

    }

    public ListTag writeBees() {
        ListTag listTag = new ListTag();

        for(BeehiveBlockEntity.BeeData beeData : this.stored) {
            CompoundTag compoundTag = beeData.entityData.copy();
            compoundTag.remove("UUID");
            CompoundTag compoundTag2 = new CompoundTag();
            compoundTag2.put("EntityData", compoundTag);
            compoundTag2.putInt("TicksInHive", beeData.ticksInHive);
            compoundTag2.putInt("MinOccupationTicks", beeData.minOccupationTicks);
            listTag.add(compoundTag2);
        }

        return listTag;
    }

    static class BeeData {
        final CompoundTag entityData;
        int ticksInHive;
        final int minOccupationTicks;

        BeeData(CompoundTag entityData, int ticksInHive, int minOccupationTicks) {
            BeehiveBlockEntity.removeIgnoredBeeTags(entityData);
            this.entityData = entityData;
            this.ticksInHive = ticksInHive;
            this.minOccupationTicks = minOccupationTicks;
        }
    }

    public static enum BeeReleaseStatus {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY;
    }
}
