package net.minecraft.world.entity.vehicle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractMinecart extends Entity {
    private static final EntityDataAccessor<Integer> DATA_ID_HURT = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ID_HURTDIR = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_BLOCK = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_OFFSET = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ID_CUSTOM_DISPLAY = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.BOOLEAN);
    private static final ImmutableMap<Pose, ImmutableList<Integer>> POSE_DISMOUNT_HEIGHTS = ImmutableMap.of(Pose.STANDING, ImmutableList.of(0, 1, -1), Pose.CROUCHING, ImmutableList.of(0, 1, -1), Pose.SWIMMING, ImmutableList.of(0, 1));
    protected static final float WATER_SLOWDOWN_FACTOR = 0.95F;
    private boolean flipped;
    private static final Map<RailShape, Pair<Vec3i, Vec3i>> EXITS = Util.make(Maps.newEnumMap(RailShape.class), (map) -> {
        Vec3i vec3i = Direction.WEST.getNormal();
        Vec3i vec3i2 = Direction.EAST.getNormal();
        Vec3i vec3i3 = Direction.NORTH.getNormal();
        Vec3i vec3i4 = Direction.SOUTH.getNormal();
        Vec3i vec3i5 = vec3i.below();
        Vec3i vec3i6 = vec3i2.below();
        Vec3i vec3i7 = vec3i3.below();
        Vec3i vec3i8 = vec3i4.below();
        map.put(RailShape.NORTH_SOUTH, Pair.of(vec3i3, vec3i4));
        map.put(RailShape.EAST_WEST, Pair.of(vec3i, vec3i2));
        map.put(RailShape.ASCENDING_EAST, Pair.of(vec3i5, vec3i2));
        map.put(RailShape.ASCENDING_WEST, Pair.of(vec3i, vec3i6));
        map.put(RailShape.ASCENDING_NORTH, Pair.of(vec3i3, vec3i8));
        map.put(RailShape.ASCENDING_SOUTH, Pair.of(vec3i7, vec3i4));
        map.put(RailShape.SOUTH_EAST, Pair.of(vec3i4, vec3i2));
        map.put(RailShape.SOUTH_WEST, Pair.of(vec3i4, vec3i));
        map.put(RailShape.NORTH_WEST, Pair.of(vec3i3, vec3i));
        map.put(RailShape.NORTH_EAST, Pair.of(vec3i3, vec3i2));
    });
    private int lSteps;
    private double lx;
    private double ly;
    private double lz;
    private double lyr;
    private double lxr;
    private double lxd;
    private double lyd;
    private double lzd;

    protected AbstractMinecart(EntityType<?> type, Level world) {
        super(type, world);
        this.blocksBuilding = true;
    }

    protected AbstractMinecart(EntityType<?> type, Level world, double x, double y, double z) {
        this(type, world);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public static AbstractMinecart createMinecart(Level world, double x, double y, double z, AbstractMinecart.Type type) {
        if (type == AbstractMinecart.Type.CHEST) {
            return new MinecartChest(world, x, y, z);
        } else if (type == AbstractMinecart.Type.FURNACE) {
            return new MinecartFurnace(world, x, y, z);
        } else if (type == AbstractMinecart.Type.TNT) {
            return new MinecartTNT(world, x, y, z);
        } else if (type == AbstractMinecart.Type.SPAWNER) {
            return new MinecartSpawner(world, x, y, z);
        } else if (type == AbstractMinecart.Type.HOPPER) {
            return new MinecartHopper(world, x, y, z);
        } else {
            return (AbstractMinecart)(type == AbstractMinecart.Type.COMMAND_BLOCK ? new MinecartCommandBlock(world, x, y, z) : new Minecart(world, x, y, z));
        }
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ID_HURT, 0);
        this.entityData.define(DATA_ID_HURTDIR, 1);
        this.entityData.define(DATA_ID_DAMAGE, 0.0F);
        this.entityData.define(DATA_ID_DISPLAY_BLOCK, Block.getId(Blocks.AIR.defaultBlockState()));
        this.entityData.define(DATA_ID_DISPLAY_OFFSET, 6);
        this.entityData.define(DATA_ID_CUSTOM_DISPLAY, false);
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return Boat.canVehicleCollide(this, other);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected Vec3 getRelativePortalPosition(Direction.Axis portalAxis, BlockUtil.FoundRectangle portalRect) {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(portalAxis, portalRect));
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0D;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction direction = this.getMotionDirection();
        if (direction.getAxis() == Direction.Axis.Y) {
            return super.getDismountLocationForPassenger(passenger);
        } else {
            int[][] is = DismountHelper.offsetsForDirection(direction);
            BlockPos blockPos = this.blockPosition();
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            ImmutableList<Pose> immutableList = passenger.getDismountPoses();

            for(Pose pose : immutableList) {
                EntityDimensions entityDimensions = passenger.getDimensions(pose);
                float f = Math.min(entityDimensions.width, 1.0F) / 2.0F;

                for(int i : POSE_DISMOUNT_HEIGHTS.get(pose)) {
                    for(int[] js : is) {
                        mutableBlockPos.set(blockPos.getX() + js[0], blockPos.getY() + i, blockPos.getZ() + js[1]);
                        double d = this.level.getBlockFloorHeight(DismountHelper.nonClimbableShape(this.level, mutableBlockPos), () -> {
                            return DismountHelper.nonClimbableShape(this.level, mutableBlockPos.below());
                        });
                        if (DismountHelper.isBlockFloorValid(d)) {
                            AABB aABB = new AABB((double)(-f), 0.0D, (double)(-f), (double)f, (double)entityDimensions.height, (double)f);
                            Vec3 vec3 = Vec3.upFromBottomCenterOf(mutableBlockPos, d);
                            if (DismountHelper.canDismountTo(this.level, passenger, aABB.move(vec3))) {
                                passenger.setPose(pose);
                                return vec3;
                            }
                        }
                    }
                }
            }

            double e = this.getBoundingBox().maxY;
            mutableBlockPos.set((double)blockPos.getX(), e, (double)blockPos.getZ());

            for(Pose pose2 : immutableList) {
                double g = (double)passenger.getDimensions(pose2).height;
                int j = Mth.ceil(e - (double)mutableBlockPos.getY() + g);
                double h = DismountHelper.findCeilingFrom(mutableBlockPos, j, (pos) -> {
                    return this.level.getBlockState(pos).getCollisionShape(this.level, pos);
                });
                if (e + g <= h) {
                    passenger.setPose(pose2);
                    break;
                }
            }

            return super.getDismountLocationForPassenger(passenger);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level.isClientSide && !this.isRemoved()) {
            if (this.isInvulnerableTo(source)) {
                return false;
            } else {
                this.setHurtDir(-this.getHurtDir());
                this.setHurtTime(10);
                this.markHurt();
                this.setDamage(this.getDamage() + amount * 10.0F);
                this.gameEvent(GameEvent.ENTITY_DAMAGED, source.getEntity());
                boolean bl = source.getEntity() instanceof Player && ((Player)source.getEntity()).getAbilities().instabuild;
                if (bl || this.getDamage() > 40.0F) {
                    this.ejectPassengers();
                    if (bl && !this.hasCustomName()) {
                        this.discard();
                    } else {
                        this.destroy(source);
                    }
                }

                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    protected float getBlockSpeedFactor() {
        BlockState blockState = this.level.getBlockState(this.blockPosition());
        return blockState.is(BlockTags.RAILS) ? 1.0F : super.getBlockSpeedFactor();
    }

    public void destroy(DamageSource damageSource) {
        this.remove(Entity.RemovalReason.KILLED);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            ItemStack itemStack = new ItemStack(Items.MINECART);
            if (this.hasCustomName()) {
                itemStack.setHoverName(this.getCustomName());
            }

            this.spawnAtLocation(itemStack);
        }

    }

    @Override
    public void animateHurt() {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() + this.getDamage() * 10.0F);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    private static Pair<Vec3i, Vec3i> exits(RailShape shape) {
        return EXITS.get(shape);
    }

    @Override
    public Direction getMotionDirection() {
        return this.flipped ? this.getDirection().getOpposite().getClockWise() : this.getDirection().getClockWise();
    }

    @Override
    public void tick() {
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        this.checkOutOfWorld();
        this.handleNetherPortal();
        if (this.level.isClientSide) {
            if (this.lSteps > 0) {
                double d = this.getX() + (this.lx - this.getX()) / (double)this.lSteps;
                double e = this.getY() + (this.ly - this.getY()) / (double)this.lSteps;
                double f = this.getZ() + (this.lz - this.getZ()) / (double)this.lSteps;
                double g = Mth.wrapDegrees(this.lyr - (double)this.getYRot());
                this.setYRot(this.getYRot() + (float)g / (float)this.lSteps);
                this.setXRot(this.getXRot() + (float)(this.lxr - (double)this.getXRot()) / (float)this.lSteps);
                --this.lSteps;
                this.setPos(d, e, f);
                this.setRot(this.getYRot(), this.getXRot());
            } else {
                this.reapplyPosition();
                this.setRot(this.getYRot(), this.getXRot());
            }

        } else {
            if (!this.isNoGravity()) {
                double h = this.isInWater() ? -0.005D : -0.04D;
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, h, 0.0D));
            }

            int i = Mth.floor(this.getX());
            int j = Mth.floor(this.getY());
            int k = Mth.floor(this.getZ());
            if (this.level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
                --j;
            }

            BlockPos blockPos = new BlockPos(i, j, k);
            BlockState blockState = this.level.getBlockState(blockPos);
            if (BaseRailBlock.isRail(blockState)) {
                this.moveAlongTrack(blockPos, blockState);
                if (blockState.is(Blocks.ACTIVATOR_RAIL)) {
                    this.activateMinecart(i, j, k, blockState.getValue(PoweredRailBlock.POWERED));
                }
            } else {
                this.comeOffTrack();
            }

            this.checkInsideBlocks();
            this.setXRot(0.0F);
            double l = this.xo - this.getX();
            double m = this.zo - this.getZ();
            if (l * l + m * m > 0.001D) {
                this.setYRot((float)(Mth.atan2(m, l) * 180.0D / Math.PI));
                if (this.flipped) {
                    this.setYRot(this.getYRot() + 180.0F);
                }
            }

            double n = (double)Mth.wrapDegrees(this.getYRot() - this.yRotO);
            if (n < -170.0D || n >= 170.0D) {
                this.setYRot(this.getYRot() + 180.0F);
                this.flipped = !this.flipped;
            }

            this.setRot(this.getYRot(), this.getXRot());
            if (this.getMinecartType() == AbstractMinecart.Type.RIDEABLE && this.getDeltaMovement().horizontalDistanceSqr() > 0.01D) {
                List<Entity> list = this.level.getEntities(this, this.getBoundingBox().inflate((double)0.2F, 0.0D, (double)0.2F), EntitySelector.pushableBy(this));
                if (!list.isEmpty()) {
                    for(int o = 0; o < list.size(); ++o) {
                        Entity entity = list.get(o);
                        if (!(entity instanceof Player) && !(entity instanceof IronGolem) && !(entity instanceof AbstractMinecart) && !this.isVehicle() && !entity.isPassenger()) {
                            entity.startRiding(this);
                        } else {
                            entity.push(this);
                        }
                    }
                }
            } else {
                for(Entity entity2 : this.level.getEntities(this, this.getBoundingBox().inflate((double)0.2F, 0.0D, (double)0.2F))) {
                    if (!this.hasPassenger(entity2) && entity2.isPushable() && entity2 instanceof AbstractMinecart) {
                        entity2.push(this);
                    }
                }
            }

            this.updateInWaterStateAndDoFluidPushing();
            if (this.isInLava()) {
                this.lavaHurt();
                this.fallDistance *= 0.5F;
            }

            this.firstTick = false;
        }
    }

    protected double getMaxSpeed() {
        return (this.isInWater() ? 4.0D : 8.0D) / 20.0D;
    }

    public void activateMinecart(int x, int y, int z, boolean powered) {
    }

    protected void comeOffTrack() {
        double d = this.getMaxSpeed();
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(Mth.clamp(vec3.x, -d, d), vec3.y, Mth.clamp(vec3.z, -d, d));
        if (this.onGround) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (!this.onGround) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.95D));
        }

    }

    protected void moveAlongTrack(BlockPos pos, BlockState state) {
        this.resetFallDistance();
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        Vec3 vec3 = this.getPos(d, e, f);
        e = (double)pos.getY();
        boolean bl = false;
        boolean bl2 = false;
        if (state.is(Blocks.POWERED_RAIL)) {
            bl = state.getValue(PoweredRailBlock.POWERED);
            bl2 = !bl;
        }

        double g = 0.0078125D;
        if (this.isInWater()) {
            g *= 0.2D;
        }

        Vec3 vec32 = this.getDeltaMovement();
        RailShape railShape = state.getValue(((BaseRailBlock)state.getBlock()).getShapeProperty());
        switch(railShape) {
        case ASCENDING_EAST:
            this.setDeltaMovement(vec32.add(-g, 0.0D, 0.0D));
            ++e;
            break;
        case ASCENDING_WEST:
            this.setDeltaMovement(vec32.add(g, 0.0D, 0.0D));
            ++e;
            break;
        case ASCENDING_NORTH:
            this.setDeltaMovement(vec32.add(0.0D, 0.0D, g));
            ++e;
            break;
        case ASCENDING_SOUTH:
            this.setDeltaMovement(vec32.add(0.0D, 0.0D, -g));
            ++e;
        }

        vec32 = this.getDeltaMovement();
        Pair<Vec3i, Vec3i> pair = exits(railShape);
        Vec3i vec3i = pair.getFirst();
        Vec3i vec3i2 = pair.getSecond();
        double h = (double)(vec3i2.getX() - vec3i.getX());
        double i = (double)(vec3i2.getZ() - vec3i.getZ());
        double j = Math.sqrt(h * h + i * i);
        double k = vec32.x * h + vec32.z * i;
        if (k < 0.0D) {
            h = -h;
            i = -i;
        }

        double l = Math.min(2.0D, vec32.horizontalDistance());
        vec32 = new Vec3(l * h / j, vec32.y, l * i / j);
        this.setDeltaMovement(vec32);
        Entity entity = this.getFirstPassenger();
        if (entity instanceof Player) {
            Vec3 vec33 = entity.getDeltaMovement();
            double m = vec33.horizontalDistanceSqr();
            double n = this.getDeltaMovement().horizontalDistanceSqr();
            if (m > 1.0E-4D && n < 0.01D) {
                this.setDeltaMovement(this.getDeltaMovement().add(vec33.x * 0.1D, 0.0D, vec33.z * 0.1D));
                bl2 = false;
            }
        }

        if (bl2) {
            double o = this.getDeltaMovement().horizontalDistance();
            if (o < 0.03D) {
                this.setDeltaMovement(Vec3.ZERO);
            } else {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, 0.0D, 0.5D));
            }
        }

        double p = (double)pos.getX() + 0.5D + (double)vec3i.getX() * 0.5D;
        double q = (double)pos.getZ() + 0.5D + (double)vec3i.getZ() * 0.5D;
        double r = (double)pos.getX() + 0.5D + (double)vec3i2.getX() * 0.5D;
        double s = (double)pos.getZ() + 0.5D + (double)vec3i2.getZ() * 0.5D;
        h = r - p;
        i = s - q;
        double t;
        if (h == 0.0D) {
            t = f - (double)pos.getZ();
        } else if (i == 0.0D) {
            t = d - (double)pos.getX();
        } else {
            double v = d - p;
            double w = f - q;
            t = (v * h + w * i) * 2.0D;
        }

        d = p + h * t;
        f = q + i * t;
        this.setPos(d, e, f);
        double y = this.isVehicle() ? 0.75D : 1.0D;
        double z = this.getMaxSpeed();
        vec32 = this.getDeltaMovement();
        this.move(MoverType.SELF, new Vec3(Mth.clamp(y * vec32.x, -z, z), 0.0D, Mth.clamp(y * vec32.z, -z, z)));
        if (vec3i.getY() != 0 && Mth.floor(this.getX()) - pos.getX() == vec3i.getX() && Mth.floor(this.getZ()) - pos.getZ() == vec3i.getZ()) {
            this.setPos(this.getX(), this.getY() + (double)vec3i.getY(), this.getZ());
        } else if (vec3i2.getY() != 0 && Mth.floor(this.getX()) - pos.getX() == vec3i2.getX() && Mth.floor(this.getZ()) - pos.getZ() == vec3i2.getZ()) {
            this.setPos(this.getX(), this.getY() + (double)vec3i2.getY(), this.getZ());
        }

        this.applyNaturalSlowdown();
        Vec3 vec34 = this.getPos(this.getX(), this.getY(), this.getZ());
        if (vec34 != null && vec3 != null) {
            double aa = (vec3.y - vec34.y) * 0.05D;
            Vec3 vec35 = this.getDeltaMovement();
            double ab = vec35.horizontalDistance();
            if (ab > 0.0D) {
                this.setDeltaMovement(vec35.multiply((ab + aa) / ab, 1.0D, (ab + aa) / ab));
            }

            this.setPos(this.getX(), vec34.y, this.getZ());
        }

        int ac = Mth.floor(this.getX());
        int ad = Mth.floor(this.getZ());
        if (ac != pos.getX() || ad != pos.getZ()) {
            Vec3 vec36 = this.getDeltaMovement();
            double ae = vec36.horizontalDistance();
            this.setDeltaMovement(ae * (double)(ac - pos.getX()), vec36.y, ae * (double)(ad - pos.getZ()));
        }

        if (bl) {
            Vec3 vec37 = this.getDeltaMovement();
            double af = vec37.horizontalDistance();
            if (af > 0.01D) {
                double ag = 0.06D;
                this.setDeltaMovement(vec37.add(vec37.x / af * 0.06D, 0.0D, vec37.z / af * 0.06D));
            } else {
                Vec3 vec38 = this.getDeltaMovement();
                double ah = vec38.x;
                double ai = vec38.z;
                if (railShape == RailShape.EAST_WEST) {
                    if (this.isRedstoneConductor(pos.west())) {
                        ah = 0.02D;
                    } else if (this.isRedstoneConductor(pos.east())) {
                        ah = -0.02D;
                    }
                } else {
                    if (railShape != RailShape.NORTH_SOUTH) {
                        return;
                    }

                    if (this.isRedstoneConductor(pos.north())) {
                        ai = 0.02D;
                    } else if (this.isRedstoneConductor(pos.south())) {
                        ai = -0.02D;
                    }
                }

                this.setDeltaMovement(ah, vec38.y, ai);
            }
        }

    }

    private boolean isRedstoneConductor(BlockPos pos) {
        return this.level.getBlockState(pos).isRedstoneConductor(this.level, pos);
    }

    protected void applyNaturalSlowdown() {
        double d = this.isVehicle() ? 0.997D : 0.96D;
        Vec3 vec3 = this.getDeltaMovement();
        vec3 = vec3.multiply(d, 0.0D, d);
        if (this.isInWater()) {
            vec3 = vec3.scale((double)0.95F);
        }

        this.setDeltaMovement(vec3);
    }

    @Nullable
    public Vec3 getPosOffs(double x, double y, double z, double offset) {
        int i = Mth.floor(x);
        int j = Mth.floor(y);
        int k = Mth.floor(z);
        if (this.level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
            --j;
        }

        BlockState blockState = this.level.getBlockState(new BlockPos(i, j, k));
        if (BaseRailBlock.isRail(blockState)) {
            RailShape railShape = blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty());
            y = (double)j;
            if (railShape.isAscending()) {
                y = (double)(j + 1);
            }

            Pair<Vec3i, Vec3i> pair = exits(railShape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i2 = pair.getSecond();
            double d = (double)(vec3i2.getX() - vec3i.getX());
            double e = (double)(vec3i2.getZ() - vec3i.getZ());
            double f = Math.sqrt(d * d + e * e);
            d /= f;
            e /= f;
            x += d * offset;
            z += e * offset;
            if (vec3i.getY() != 0 && Mth.floor(x) - i == vec3i.getX() && Mth.floor(z) - k == vec3i.getZ()) {
                y += (double)vec3i.getY();
            } else if (vec3i2.getY() != 0 && Mth.floor(x) - i == vec3i2.getX() && Mth.floor(z) - k == vec3i2.getZ()) {
                y += (double)vec3i2.getY();
            }

            return this.getPos(x, y, z);
        } else {
            return null;
        }
    }

    @Nullable
    public Vec3 getPos(double x, double y, double z) {
        int i = Mth.floor(x);
        int j = Mth.floor(y);
        int k = Mth.floor(z);
        if (this.level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
            --j;
        }

        BlockState blockState = this.level.getBlockState(new BlockPos(i, j, k));
        if (BaseRailBlock.isRail(blockState)) {
            RailShape railShape = blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty());
            Pair<Vec3i, Vec3i> pair = exits(railShape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i2 = pair.getSecond();
            double d = (double)i + 0.5D + (double)vec3i.getX() * 0.5D;
            double e = (double)j + 0.0625D + (double)vec3i.getY() * 0.5D;
            double f = (double)k + 0.5D + (double)vec3i.getZ() * 0.5D;
            double g = (double)i + 0.5D + (double)vec3i2.getX() * 0.5D;
            double h = (double)j + 0.0625D + (double)vec3i2.getY() * 0.5D;
            double l = (double)k + 0.5D + (double)vec3i2.getZ() * 0.5D;
            double m = g - d;
            double n = (h - e) * 2.0D;
            double o = l - f;
            double p;
            if (m == 0.0D) {
                p = z - (double)k;
            } else if (o == 0.0D) {
                p = x - (double)i;
            } else {
                double r = x - d;
                double s = z - f;
                p = (r * m + s * o) * 2.0D;
            }

            x = d + m * p;
            y = e + n * p;
            z = f + o * p;
            if (n < 0.0D) {
                ++y;
            } else if (n > 0.0D) {
                y += 0.5D;
            }

            return new Vec3(x, y, z);
        } else {
            return null;
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB aABB = this.getBoundingBox();
        return this.hasCustomDisplay() ? aABB.inflate((double)Math.abs(this.getDisplayOffset()) / 16.0D) : aABB;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.getBoolean("CustomDisplayTile")) {
            this.setDisplayBlockState(NbtUtils.readBlockState(nbt.getCompound("DisplayState")));
            this.setDisplayOffset(nbt.getInt("DisplayOffset"));
        }

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        if (this.hasCustomDisplay()) {
            nbt.putBoolean("CustomDisplayTile", true);
            nbt.put("DisplayState", NbtUtils.writeBlockState(this.getDisplayBlockState()));
            nbt.putInt("DisplayOffset", this.getDisplayOffset());
        }

    }

    @Override
    public void push(Entity entity) {
        if (!this.level.isClientSide) {
            if (!entity.noPhysics && !this.noPhysics) {
                if (!this.hasPassenger(entity)) {
                    double d = entity.getX() - this.getX();
                    double e = entity.getZ() - this.getZ();
                    double f = d * d + e * e;
                    if (f >= (double)1.0E-4F) {
                        f = Math.sqrt(f);
                        d /= f;
                        e /= f;
                        double g = 1.0D / f;
                        if (g > 1.0D) {
                            g = 1.0D;
                        }

                        d *= g;
                        e *= g;
                        d *= (double)0.1F;
                        e *= (double)0.1F;
                        d *= 0.5D;
                        e *= 0.5D;
                        if (entity instanceof AbstractMinecart) {
                            double h = entity.getX() - this.getX();
                            double i = entity.getZ() - this.getZ();
                            Vec3 vec3 = (new Vec3(h, 0.0D, i)).normalize();
                            Vec3 vec32 = (new Vec3((double)Mth.cos(this.getYRot() * ((float)Math.PI / 180F)), 0.0D, (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)))).normalize();
                            double j = Math.abs(vec3.dot(vec32));
                            if (j < (double)0.8F) {
                                return;
                            }

                            Vec3 vec33 = this.getDeltaMovement();
                            Vec3 vec34 = entity.getDeltaMovement();
                            if (((AbstractMinecart)entity).getMinecartType() == AbstractMinecart.Type.FURNACE && this.getMinecartType() != AbstractMinecart.Type.FURNACE) {
                                this.setDeltaMovement(vec33.multiply(0.2D, 1.0D, 0.2D));
                                this.push(vec34.x - d, 0.0D, vec34.z - e);
                                entity.setDeltaMovement(vec34.multiply(0.95D, 1.0D, 0.95D));
                            } else if (((AbstractMinecart)entity).getMinecartType() != AbstractMinecart.Type.FURNACE && this.getMinecartType() == AbstractMinecart.Type.FURNACE) {
                                entity.setDeltaMovement(vec34.multiply(0.2D, 1.0D, 0.2D));
                                entity.push(vec33.x + d, 0.0D, vec33.z + e);
                                this.setDeltaMovement(vec33.multiply(0.95D, 1.0D, 0.95D));
                            } else {
                                double k = (vec34.x + vec33.x) / 2.0D;
                                double l = (vec34.z + vec33.z) / 2.0D;
                                this.setDeltaMovement(vec33.multiply(0.2D, 1.0D, 0.2D));
                                this.push(k - d, 0.0D, l - e);
                                entity.setDeltaMovement(vec34.multiply(0.2D, 1.0D, 0.2D));
                                entity.push(k + d, 0.0D, l + e);
                            }
                        } else {
                            this.push(-d, 0.0D, -e);
                            entity.push(d / 4.0D, 0.0D, e / 4.0D);
                        }
                    }

                }
            }
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.lx = x;
        this.ly = y;
        this.lz = z;
        this.lyr = (double)yaw;
        this.lxr = (double)pitch;
        this.lSteps = interpolationSteps + 2;
        this.setDeltaMovement(this.lxd, this.lyd, this.lzd);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.lxd = x;
        this.lyd = y;
        this.lzd = z;
        this.setDeltaMovement(this.lxd, this.lyd, this.lzd);
    }

    public void setDamage(float damageWobbleStrength) {
        this.entityData.set(DATA_ID_DAMAGE, damageWobbleStrength);
    }

    public float getDamage() {
        return this.entityData.get(DATA_ID_DAMAGE);
    }

    public void setHurtTime(int wobbleTicks) {
        this.entityData.set(DATA_ID_HURT, wobbleTicks);
    }

    public int getHurtTime() {
        return this.entityData.get(DATA_ID_HURT);
    }

    public void setHurtDir(int wobbleSide) {
        this.entityData.set(DATA_ID_HURTDIR, wobbleSide);
    }

    public int getHurtDir() {
        return this.entityData.get(DATA_ID_HURTDIR);
    }

    public abstract AbstractMinecart.Type getMinecartType();

    public BlockState getDisplayBlockState() {
        return !this.hasCustomDisplay() ? this.getDefaultDisplayBlockState() : Block.stateById(this.getEntityData().get(DATA_ID_DISPLAY_BLOCK));
    }

    public BlockState getDefaultDisplayBlockState() {
        return Blocks.AIR.defaultBlockState();
    }

    public int getDisplayOffset() {
        return !this.hasCustomDisplay() ? this.getDefaultDisplayOffset() : this.getEntityData().get(DATA_ID_DISPLAY_OFFSET);
    }

    public int getDefaultDisplayOffset() {
        return 6;
    }

    public void setDisplayBlockState(BlockState state) {
        this.getEntityData().set(DATA_ID_DISPLAY_BLOCK, Block.getId(state));
        this.setCustomDisplay(true);
    }

    public void setDisplayOffset(int offset) {
        this.getEntityData().set(DATA_ID_DISPLAY_OFFSET, offset);
        this.setCustomDisplay(true);
    }

    public boolean hasCustomDisplay() {
        return this.getEntityData().get(DATA_ID_CUSTOM_DISPLAY);
    }

    public void setCustomDisplay(boolean present) {
        this.getEntityData().set(DATA_ID_CUSTOM_DISPLAY, present);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public ItemStack getPickResult() {
        Item item;
        switch(this.getMinecartType()) {
        case FURNACE:
            item = Items.FURNACE_MINECART;
            break;
        case CHEST:
            item = Items.CHEST_MINECART;
            break;
        case TNT:
            item = Items.TNT_MINECART;
            break;
        case HOPPER:
            item = Items.HOPPER_MINECART;
            break;
        case COMMAND_BLOCK:
            item = Items.COMMAND_BLOCK_MINECART;
            break;
        default:
            item = Items.MINECART;
        }

        return new ItemStack(item);
    }

    public static enum Type {
        RIDEABLE,
        CHEST,
        FURNACE,
        TNT,
        SPAWNER,
        HOPPER,
        COMMAND_BLOCK;
    }
}
