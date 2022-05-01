package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListenerRegistrar;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public abstract class Entity implements Nameable, EntityAccess, CommandSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String ID_TAG = "id";
    public static final String PASSENGERS_TAG = "Passengers";
    private static final AtomicInteger ENTITY_COUNTER = new AtomicInteger();
    private static final List<ItemStack> EMPTY_LIST = Collections.emptyList();
    public static final int BOARDING_COOLDOWN = 60;
    public static final int TOTAL_AIR_SUPPLY = 300;
    public static final int MAX_ENTITY_TAG_COUNT = 1024;
    public static final double DELTA_AFFECTED_BY_BLOCKS_BELOW = 0.5000001D;
    public static final float BREATHING_DISTANCE_BELOW_EYES = 0.11111111F;
    public static final int BASE_TICKS_REQUIRED_TO_FREEZE = 140;
    public static final int FREEZE_HURT_FREQUENCY = 40;
    private static final AABB INITIAL_AABB = new AABB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    private static final double WATER_FLOW_SCALE = 0.014D;
    private static final double LAVA_FAST_FLOW_SCALE = 0.007D;
    private static final double LAVA_SLOW_FLOW_SCALE = 0.0023333333333333335D;
    public static final String UUID_TAG = "UUID";
    private static double viewScale = 1.0D;
    private final EntityType<?> type;
    private int id = ENTITY_COUNTER.incrementAndGet();
    public boolean blocksBuilding;
    public ImmutableList<Entity> passengers = ImmutableList.of();
    protected int boardingCooldown;
    @Nullable
    private Entity vehicle;
    public Level level;
    public double xo;
    public double yo;
    public double zo;
    private Vec3 position;
    private BlockPos blockPosition;
    private ChunkPos chunkPosition;
    private Vec3 deltaMovement = Vec3.ZERO;
    private float yRot;
    private float xRot;
    public float yRotO;
    public float xRotO;
    private AABB bb = INITIAL_AABB;
    public boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean verticalCollisionBelow;
    public boolean minorHorizontalCollision;
    public boolean hurtMarked;
    protected Vec3 stuckSpeedMultiplier = Vec3.ZERO;
    @Nullable
    private Entity.RemovalReason removalReason;
    public static final float DEFAULT_BB_WIDTH = 0.6F;
    public static final float DEFAULT_BB_HEIGHT = 1.8F;
    public float walkDistO;
    public float walkDist;
    public float moveDist;
    public float flyDist;
    public float fallDistance;
    private float nextStep = 1.0F;
    public double xOld;
    public double yOld;
    public double zOld;
    public float maxUpStep;
    public boolean noPhysics;
    protected final Random random = new Random();
    public int tickCount;
    public int remainingFireTicks = -this.getFireImmuneTicks();
    public boolean wasTouchingWater;
    protected Object2DoubleMap<TagKey<Fluid>> fluidHeight = new Object2DoubleArrayMap<>(2);
    protected boolean wasEyeInWater;
    private final Set<TagKey<Fluid>> fluidOnEyes = new HashSet<>();
    public int invulnerableTime;
    protected boolean firstTick = true;
    protected final SynchedEntityData entityData;
    protected static final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BYTE);
    protected static final int FLAG_ONFIRE = 0;
    private static final int FLAG_SHIFT_KEY_DOWN = 1;
    private static final int FLAG_SPRINTING = 3;
    private static final int FLAG_SWIMMING = 4;
    private static final int FLAG_INVISIBLE = 5;
    protected static final int FLAG_GLOWING = 6;
    protected static final int FLAG_FALL_FLYING = 7;
    private static final EntityDataAccessor<Integer> DATA_AIR_SUPPLY_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<Component>> DATA_CUSTOM_NAME = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.OPTIONAL_COMPONENT);
    private static final EntityDataAccessor<Boolean> DATA_CUSTOM_NAME_VISIBLE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SILENT = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_NO_GRAVITY = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Pose> DATA_POSE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.POSE);
    private static final EntityDataAccessor<Integer> DATA_TICKS_FROZEN = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
    private EntityInLevelCallback levelCallback = EntityInLevelCallback.NULL;
    private Vec3 packetCoordinates;
    public boolean noCulling;
    public boolean hasImpulse;
    public int portalCooldown;
    public boolean isInsidePortal;
    protected int portalTime;
    protected BlockPos portalEntrancePos;
    private boolean invulnerable;
    protected UUID uuid = Mth.createInsecureUUID(this.random);
    protected String stringUUID = this.uuid.toString();
    private boolean hasGlowingTag;
    private final Set<String> tags = Sets.newHashSet();
    private final double[] pistonDeltas = new double[]{0.0D, 0.0D, 0.0D};
    private long pistonDeltasGameTime;
    private EntityDimensions dimensions;
    private float eyeHeight;
    public boolean isInPowderSnow;
    public boolean wasInPowderSnow;
    public boolean wasOnFire;
    private float crystalSoundIntensity;
    private int lastCrystalSoundPlayTick;
    public boolean hasVisualFire;
    @Nullable
    private BlockState feetBlockState = null;

    public Entity(EntityType<?> type, Level world) {
        this.type = type;
        this.level = world;
        this.dimensions = type.getDimensions();
        this.position = Vec3.ZERO;
        this.blockPosition = BlockPos.ZERO;
        this.chunkPosition = ChunkPos.ZERO;
        this.packetCoordinates = Vec3.ZERO;
        this.entityData = new SynchedEntityData(this);
        this.entityData.define(DATA_SHARED_FLAGS_ID, (byte)0);
        this.entityData.define(DATA_AIR_SUPPLY_ID, this.getMaxAirSupply());
        this.entityData.define(DATA_CUSTOM_NAME_VISIBLE, false);
        this.entityData.define(DATA_CUSTOM_NAME, Optional.empty());
        this.entityData.define(DATA_SILENT, false);
        this.entityData.define(DATA_NO_GRAVITY, false);
        this.entityData.define(DATA_POSE, Pose.STANDING);
        this.entityData.define(DATA_TICKS_FROZEN, 0);
        this.defineSynchedData();
        this.setPos(0.0D, 0.0D, 0.0D);
        this.eyeHeight = this.getEyeHeight(Pose.STANDING, this.dimensions);
    }

    public boolean isColliding(BlockPos pos, BlockState state) {
        VoxelShape voxelShape = state.getCollisionShape(this.level, pos, CollisionContext.of(this));
        VoxelShape voxelShape2 = voxelShape.move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        return Shapes.joinIsNotEmpty(voxelShape2, Shapes.create(this.getBoundingBox()), BooleanOp.AND);
    }

    public int getTeamColor() {
        Team team = this.getTeam();
        return team != null && team.getColor().getColor() != null ? team.getColor().getColor() : 16777215;
    }

    public boolean isSpectator() {
        return false;
    }

    public final void unRide() {
        if (this.isVehicle()) {
            this.ejectPassengers();
        }

        if (this.isPassenger()) {
            this.stopRiding();
        }

    }

    public void setPacketCoordinates(double x, double y, double z) {
        this.setPacketCoordinates(new Vec3(x, y, z));
    }

    public void setPacketCoordinates(Vec3 pos) {
        this.packetCoordinates = pos;
    }

    public Vec3 getPacketCoordinates() {
        return this.packetCoordinates;
    }

    public EntityType<?> getType() {
        return this.type;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public boolean addTag(String tag) {
        return this.tags.size() >= 1024 ? false : this.tags.add(tag);
    }

    public boolean removeTag(String tag) {
        return this.tags.remove(tag);
    }

    public void kill() {
        this.remove(Entity.RemovalReason.KILLED);
    }

    public final void discard() {
        this.remove(Entity.RemovalReason.DISCARDED);
    }

    protected abstract void defineSynchedData();

    public SynchedEntityData getEntityData() {
        return this.entityData;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Entity) {
            return ((Entity)object).id == this.id;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    public void remove(Entity.RemovalReason reason) {
        this.setRemoved(reason);
        if (reason == Entity.RemovalReason.KILLED) {
            this.gameEvent(GameEvent.ENTITY_KILLED);
        }

    }

    public void onClientRemoval() {
    }

    public void setPose(Pose pose) {
        this.entityData.set(DATA_POSE, pose);
    }

    public Pose getPose() {
        return this.entityData.get(DATA_POSE);
    }

    public boolean closerThan(Entity other, double radius) {
        double d = other.position.x - this.position.x;
        double e = other.position.y - this.position.y;
        double f = other.position.z - this.position.z;
        return d * d + e * e + f * f < radius * radius;
    }

    public void setRot(float yaw, float pitch) {
        this.setYRot(yaw % 360.0F);
        this.setXRot(pitch % 360.0F);
    }

    public final void setPos(Vec3 pos) {
        this.setPos(pos.x(), pos.y(), pos.z());
    }

    public void setPos(double x, double y, double z) {
        this.setPosRaw(x, y, z);
        this.setBoundingBox(this.makeBoundingBox());
    }

    protected AABB makeBoundingBox() {
        return this.dimensions.makeBoundingBox(this.position);
    }

    protected void reapplyPosition() {
        this.setPos(this.position.x, this.position.y, this.position.z);
    }

    public void turn(double cursorDeltaX, double cursorDeltaY) {
        float f = (float)cursorDeltaY * 0.15F;
        float g = (float)cursorDeltaX * 0.15F;
        this.setXRot(this.getXRot() + f);
        this.setYRot(this.getYRot() + g);
        this.setXRot(Mth.clamp(this.getXRot(), -90.0F, 90.0F));
        this.xRotO += f;
        this.yRotO += g;
        this.xRotO = Mth.clamp(this.xRotO, -90.0F, 90.0F);
        if (this.vehicle != null) {
            this.vehicle.onPassengerTurned(this);
        }

    }

    public void tick() {
        this.baseTick();
    }

    public void baseTick() {
        this.level.getProfiler().push("entityBaseTick");
        this.feetBlockState = null;
        if (this.isPassenger() && this.getVehicle().isRemoved()) {
            this.stopRiding();
        }

        if (this.boardingCooldown > 0) {
            --this.boardingCooldown;
        }

        this.walkDistO = this.walkDist;
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.handleNetherPortal();
        if (this.canSpawnSprintParticle()) {
            this.spawnSprintParticle();
        }

        this.wasInPowderSnow = this.isInPowderSnow;
        this.isInPowderSnow = false;
        this.updateInWaterStateAndDoFluidPushing();
        this.updateFluidOnEyes();
        this.updateSwimming();
        if (this.level.isClientSide) {
            this.clearFire();
        } else if (this.remainingFireTicks > 0) {
            if (this.fireImmune()) {
                this.setRemainingFireTicks(this.remainingFireTicks - 4);
                if (this.remainingFireTicks < 0) {
                    this.clearFire();
                }
            } else {
                if (this.remainingFireTicks % 20 == 0 && !this.isInLava()) {
                    this.hurt(DamageSource.ON_FIRE, 1.0F);
                }

                this.setRemainingFireTicks(this.remainingFireTicks - 1);
            }

            if (this.getTicksFrozen() > 0) {
                this.setTicksFrozen(0);
                this.level.levelEvent((Player)null, 1009, this.blockPosition, 1);
            }
        }

        if (this.isInLava()) {
            this.lavaHurt();
            this.fallDistance *= 0.5F;
        }

        this.checkOutOfWorld();
        if (!this.level.isClientSide) {
            this.setSharedFlagOnFire(this.remainingFireTicks > 0);
        }

        this.firstTick = false;
        this.level.getProfiler().pop();
    }

    public void setSharedFlagOnFire(boolean onFire) {
        this.setSharedFlag(0, onFire || this.hasVisualFire);
    }

    public void checkOutOfWorld() {
        if (this.getY() < (double)(this.level.getMinBuildHeight() - 64)) {
            this.outOfWorld();
        }

    }

    public void setPortalCooldown() {
        this.portalCooldown = this.getDimensionChangingDelay();
    }

    public boolean isOnPortalCooldown() {
        return this.portalCooldown > 0;
    }

    protected void processPortalCooldown() {
        if (this.isOnPortalCooldown()) {
            --this.portalCooldown;
        }

    }

    public int getPortalWaitTime() {
        return 0;
    }

    public void lavaHurt() {
        if (!this.fireImmune()) {
            this.setSecondsOnFire(15);
            if (this.hurt(DamageSource.LAVA, 4.0F)) {
                this.playSound(SoundEvents.GENERIC_BURN, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);
            }

        }
    }

    public void setSecondsOnFire(int seconds) {
        int i = seconds * 20;
        if (this instanceof LivingEntity) {
            i = ProtectionEnchantment.getFireAfterDampener((LivingEntity)this, i);
        }

        if (this.remainingFireTicks < i) {
            this.setRemainingFireTicks(i);
        }

    }

    public void setRemainingFireTicks(int fireTicks) {
        this.remainingFireTicks = fireTicks;
    }

    public int getRemainingFireTicks() {
        return this.remainingFireTicks;
    }

    public void clearFire() {
        this.setRemainingFireTicks(0);
    }

    protected void outOfWorld() {
        this.discard();
    }

    public boolean isFree(double offsetX, double offsetY, double offsetZ) {
        return this.isFree(this.getBoundingBox().move(offsetX, offsetY, offsetZ));
    }

    private boolean isFree(AABB box) {
        return this.level.noCollision(this, box) && !this.level.containsAnyLiquid(box);
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public void move(MoverType movementType, Vec3 movement) {
        if (this.noPhysics) {
            this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        } else {
            this.wasOnFire = this.isOnFire();
            if (movementType == MoverType.PISTON) {
                movement = this.limitPistonMovement(movement);
                if (movement.equals(Vec3.ZERO)) {
                    return;
                }
            }

            this.level.getProfiler().push("move");
            if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7D) {
                movement = movement.multiply(this.stuckSpeedMultiplier);
                this.stuckSpeedMultiplier = Vec3.ZERO;
                this.setDeltaMovement(Vec3.ZERO);
            }

            movement = this.maybeBackOffFromEdge(movement, movementType);
            Vec3 vec3 = this.collide(movement);
            double d = vec3.lengthSqr();
            if (d > 1.0E-7D) {
                if (this.fallDistance != 0.0F && d >= 1.0D) {
                    BlockHitResult blockHitResult = this.level.clip(new ClipContext(this.position(), this.position().add(vec3), ClipContext.Block.FALLDAMAGE_RESETTING, ClipContext.Fluid.WATER, this));
                    if (blockHitResult.getType() != HitResult.Type.MISS) {
                        this.resetFallDistance();
                    }
                }

                this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
            }

            this.level.getProfiler().pop();
            this.level.getProfiler().push("rest");
            boolean bl = !Mth.equal(movement.x, vec3.x);
            boolean bl2 = !Mth.equal(movement.z, vec3.z);
            this.horizontalCollision = bl || bl2;
            this.verticalCollision = movement.y != vec3.y;
            this.verticalCollisionBelow = this.verticalCollision && movement.y < 0.0D;
            if (this.horizontalCollision) {
                this.minorHorizontalCollision = this.isHorizontalCollisionMinor(vec3);
            } else {
                this.minorHorizontalCollision = false;
            }

            this.onGround = this.verticalCollision && movement.y < 0.0D;
            BlockPos blockPos = this.getOnPos();
            BlockState blockState = this.level.getBlockState(blockPos);
            this.checkFallDamage(vec3.y, this.onGround, blockState, blockPos);
            if (this.isRemoved()) {
                this.level.getProfiler().pop();
            } else {
                if (this.horizontalCollision) {
                    Vec3 vec32 = this.getDeltaMovement();
                    this.setDeltaMovement(bl ? 0.0D : vec32.x, vec32.y, bl2 ? 0.0D : vec32.z);
                }

                Block block = blockState.getBlock();
                if (movement.y != vec3.y) {
                    block.updateEntityAfterFallOn(this.level, this);
                }

                if (this.onGround && !this.isSteppingCarefully()) {
                    block.stepOn(this.level, blockPos, blockState, this);
                }

                Entity.MovementEmission movementEmission = this.getMovementEmission();
                if (movementEmission.emitsAnything() && !this.isPassenger()) {
                    double e = vec3.x;
                    double f = vec3.y;
                    double g = vec3.z;
                    this.flyDist += (float)(vec3.length() * 0.6D);
                    if (!blockState.is(BlockTags.CLIMBABLE) && !blockState.is(Blocks.POWDER_SNOW)) {
                        f = 0.0D;
                    }

                    this.walkDist += (float)vec3.horizontalDistance() * 0.6F;
                    this.moveDist += (float)Math.sqrt(e * e + f * f + g * g) * 0.6F;
                    if (this.moveDist > this.nextStep && !blockState.isAir()) {
                        this.nextStep = this.nextStep();
                        if (this.isInWater()) {
                            if (movementEmission.emitsSounds()) {
                                Entity entity = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;
                                float h = entity == this ? 0.35F : 0.4F;
                                Vec3 vec33 = entity.getDeltaMovement();
                                float i = Math.min(1.0F, (float)Math.sqrt(vec33.x * vec33.x * (double)0.2F + vec33.y * vec33.y + vec33.z * vec33.z * (double)0.2F) * h);
                                this.playSwimSound(i);
                            }

                            if (movementEmission.emitsEvents()) {
                                this.gameEvent(GameEvent.SWIM);
                            }
                        } else {
                            if (movementEmission.emitsSounds()) {
                                this.playAmethystStepSound(blockState);
                                this.playStepSound(blockPos, blockState);
                            }

                            if (movementEmission.emitsEvents() && !blockState.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS)) {
                                this.gameEvent(GameEvent.STEP);
                            }
                        }
                    } else if (blockState.isAir()) {
                        this.processFlappingMovement();
                    }
                }

                this.tryCheckInsideBlocks();
                float j = this.getBlockSpeedFactor();
                this.setDeltaMovement(this.getDeltaMovement().multiply((double)j, 1.0D, (double)j));
                if (this.level.getBlockStatesIfLoaded(this.getBoundingBox().deflate(1.0E-6D)).noneMatch((state) -> {
                    return state.is(BlockTags.FIRE) || state.is(Blocks.LAVA);
                })) {
                    if (this.remainingFireTicks <= 0) {
                        this.setRemainingFireTicks(-this.getFireImmuneTicks());
                    }

                    if (this.wasOnFire && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
                        this.playEntityOnFireExtinguishedSound();
                    }
                }

                if (this.isOnFire() && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
                    this.setRemainingFireTicks(-this.getFireImmuneTicks());
                }

                this.level.getProfiler().pop();
            }
        }
    }

    protected boolean isHorizontalCollisionMinor(Vec3 adjustedMovement) {
        return false;
    }

    protected void tryCheckInsideBlocks() {
        try {
            this.checkInsideBlocks();
        } catch (Throwable var4) {
            CrashReport crashReport = CrashReport.forThrowable(var4, "Checking entity block collision");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Entity being checked for collision");
            this.fillCrashReportCategory(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    protected void playEntityOnFireExtinguishedSound() {
        this.playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    }

    protected void processFlappingMovement() {
        if (this.isFlapping()) {
            this.onFlap();
            if (this.getMovementEmission().emitsEvents()) {
                this.gameEvent(GameEvent.FLAP);
            }
        }

    }

    public BlockPos getOnPos() {
        int i = Mth.floor(this.position.x);
        int j = Mth.floor(this.position.y - (double)0.2F);
        int k = Mth.floor(this.position.z);
        BlockPos blockPos = new BlockPos(i, j, k);
        if (this.level.getBlockState(blockPos).isAir()) {
            BlockPos blockPos2 = blockPos.below();
            BlockState blockState = this.level.getBlockState(blockPos2);
            if (blockState.is(BlockTags.FENCES) || blockState.is(BlockTags.WALLS) || blockState.getBlock() instanceof FenceGateBlock) {
                return blockPos2;
            }
        }

        return blockPos;
    }

    protected float getBlockJumpFactor() {
        float f = this.level.getBlockState(this.blockPosition()).getBlock().getJumpFactor();
        float g = this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
        return (double)f == 1.0D ? g : f;
    }

    protected float getBlockSpeedFactor() {
        BlockState blockState = this.level.getBlockState(this.blockPosition());
        float f = blockState.getBlock().getSpeedFactor();
        if (!blockState.is(Blocks.WATER) && !blockState.is(Blocks.BUBBLE_COLUMN)) {
            return (double)f == 1.0D ? this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getSpeedFactor() : f;
        } else {
            return f;
        }
    }

    protected BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return new BlockPos(this.position.x, this.getBoundingBox().minY - 0.5000001D, this.position.z);
    }

    protected Vec3 maybeBackOffFromEdge(Vec3 movement, MoverType type) {
        return movement;
    }

    protected Vec3 limitPistonMovement(Vec3 movement) {
        if (movement.lengthSqr() <= 1.0E-7D) {
            return movement;
        } else {
            long l = this.level.getGameTime();
            if (l != this.pistonDeltasGameTime) {
                Arrays.fill(this.pistonDeltas, 0.0D);
                this.pistonDeltasGameTime = l;
            }

            if (movement.x != 0.0D) {
                double d = this.applyPistonMovementRestriction(Direction.Axis.X, movement.x);
                return Math.abs(d) <= (double)1.0E-5F ? Vec3.ZERO : new Vec3(d, 0.0D, 0.0D);
            } else if (movement.y != 0.0D) {
                double e = this.applyPistonMovementRestriction(Direction.Axis.Y, movement.y);
                return Math.abs(e) <= (double)1.0E-5F ? Vec3.ZERO : new Vec3(0.0D, e, 0.0D);
            } else if (movement.z != 0.0D) {
                double f = this.applyPistonMovementRestriction(Direction.Axis.Z, movement.z);
                return Math.abs(f) <= (double)1.0E-5F ? Vec3.ZERO : new Vec3(0.0D, 0.0D, f);
            } else {
                return Vec3.ZERO;
            }
        }
    }

    private double applyPistonMovementRestriction(Direction.Axis axis, double offsetFactor) {
        int i = axis.ordinal();
        double d = Mth.clamp(offsetFactor + this.pistonDeltas[i], -0.51D, 0.51D);
        offsetFactor = d - this.pistonDeltas[i];
        this.pistonDeltas[i] = d;
        return offsetFactor;
    }

    private Vec3 collide(Vec3 movement) {
        AABB aABB = this.getBoundingBox();
        List<VoxelShape> list = this.level.getEntityCollisions(this, aABB.expandTowards(movement));
        Vec3 vec3 = movement.lengthSqr() == 0.0D ? movement : collideBoundingBox(this, movement, aABB, this.level, list);
        boolean bl = movement.x != vec3.x;
        boolean bl2 = movement.y != vec3.y;
        boolean bl3 = movement.z != vec3.z;
        boolean bl4 = this.onGround || bl2 && movement.y < 0.0D;
        if (this.maxUpStep > 0.0F && bl4 && (bl || bl3)) {
            Vec3 vec32 = collideBoundingBox(this, new Vec3(movement.x, (double)this.maxUpStep, movement.z), aABB, this.level, list);
            Vec3 vec33 = collideBoundingBox(this, new Vec3(0.0D, (double)this.maxUpStep, 0.0D), aABB.expandTowards(movement.x, 0.0D, movement.z), this.level, list);
            if (vec33.y < (double)this.maxUpStep) {
                Vec3 vec34 = collideBoundingBox(this, new Vec3(movement.x, 0.0D, movement.z), aABB.move(vec33), this.level, list).add(vec33);
                if (vec34.horizontalDistanceSqr() > vec32.horizontalDistanceSqr()) {
                    vec32 = vec34;
                }
            }

            if (vec32.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
                return vec32.add(collideBoundingBox(this, new Vec3(0.0D, -vec32.y + movement.y, 0.0D), aABB.move(vec32), this.level, list));
            }
        }

        return vec3;
    }

    public static Vec3 collideBoundingBox(@Nullable Entity entity, Vec3 movement, AABB entityBoundingBox, Level world, List<VoxelShape> collisions) {
        Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 1);
        if (!collisions.isEmpty()) {
            builder.addAll(collisions);
        }

        WorldBorder worldBorder = world.getWorldBorder();
        boolean bl = entity != null && worldBorder.isInsideCloseToBorder(entity, entityBoundingBox.expandTowards(movement));
        if (bl) {
            builder.add(worldBorder.getCollisionShape());
        }

        builder.addAll(world.getBlockCollisions(entity, entityBoundingBox.expandTowards(movement)));
        return collideWithShapes(movement, entityBoundingBox, builder.build());
    }

    private static Vec3 collideWithShapes(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions) {
        if (collisions.isEmpty()) {
            return movement;
        } else {
            double d = movement.x;
            double e = movement.y;
            double f = movement.z;
            if (e != 0.0D) {
                e = Shapes.collide(Direction.Axis.Y, entityBoundingBox, collisions, e);
                if (e != 0.0D) {
                    entityBoundingBox = entityBoundingBox.move(0.0D, e, 0.0D);
                }
            }

            boolean bl = Math.abs(d) < Math.abs(f);
            if (bl && f != 0.0D) {
                f = Shapes.collide(Direction.Axis.Z, entityBoundingBox, collisions, f);
                if (f != 0.0D) {
                    entityBoundingBox = entityBoundingBox.move(0.0D, 0.0D, f);
                }
            }

            if (d != 0.0D) {
                d = Shapes.collide(Direction.Axis.X, entityBoundingBox, collisions, d);
                if (!bl && d != 0.0D) {
                    entityBoundingBox = entityBoundingBox.move(d, 0.0D, 0.0D);
                }
            }

            if (!bl && f != 0.0D) {
                f = Shapes.collide(Direction.Axis.Z, entityBoundingBox, collisions, f);
            }

            return new Vec3(d, e, f);
        }
    }

    protected float nextStep() {
        return (float)((int)this.moveDist + 1);
    }

    protected SoundEvent getSwimSound() {
        return SoundEvents.GENERIC_SWIM;
    }

    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.GENERIC_SPLASH;
    }

    protected SoundEvent getSwimHighSpeedSplashSound() {
        return SoundEvents.GENERIC_SPLASH;
    }

    protected void checkInsideBlocks() {
        AABB aABB = this.getBoundingBox();
        BlockPos blockPos = new BlockPos(aABB.minX + 0.001D, aABB.minY + 0.001D, aABB.minZ + 0.001D);
        BlockPos blockPos2 = new BlockPos(aABB.maxX - 0.001D, aABB.maxY - 0.001D, aABB.maxZ - 0.001D);
        if (this.level.hasChunksAt(blockPos, blockPos2)) {
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for(int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                for(int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                    for(int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                        mutableBlockPos.set(i, j, k);
                        BlockState blockState = this.level.getBlockState(mutableBlockPos);

                        try {
                            blockState.entityInside(this.level, mutableBlockPos, this);
                            this.onInsideBlock(blockState);
                        } catch (Throwable var12) {
                            CrashReport crashReport = CrashReport.forThrowable(var12, "Colliding entity with block");
                            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being collided with");
                            CrashReportCategory.populateBlockDetails(crashReportCategory, this.level, mutableBlockPos, blockState);
                            throw new ReportedException(crashReport);
                        }
                    }
                }
            }
        }

    }

    protected void onInsideBlock(BlockState state) {
    }

    public void gameEvent(GameEvent event, @Nullable Entity entity, BlockPos pos) {
        this.level.gameEvent(entity, event, pos);
    }

    public void gameEvent(GameEvent event, @Nullable Entity entity) {
        this.gameEvent(event, entity, this.blockPosition);
    }

    public void gameEvent(GameEvent event, BlockPos pos) {
        this.gameEvent(event, this, pos);
    }

    public void gameEvent(GameEvent event) {
        this.gameEvent(event, this.blockPosition);
    }

    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!state.getMaterial().isLiquid()) {
            BlockState blockState = this.level.getBlockState(pos.above());
            SoundType soundType = blockState.is(BlockTags.INSIDE_STEP_SOUND_BLOCKS) ? blockState.getSoundType() : state.getSoundType();
            this.playSound(soundType.getStepSound(), soundType.getVolume() * 0.15F, soundType.getPitch());
        }
    }

    private void playAmethystStepSound(BlockState state) {
        if (state.is(BlockTags.CRYSTAL_SOUND_BLOCKS) && this.tickCount >= this.lastCrystalSoundPlayTick + 20) {
            this.crystalSoundIntensity *= (float)Math.pow(0.997D, (double)(this.tickCount - this.lastCrystalSoundPlayTick));
            this.crystalSoundIntensity = Math.min(1.0F, this.crystalSoundIntensity + 0.07F);
            float f = 0.5F + this.crystalSoundIntensity * this.random.nextFloat() * 1.2F;
            float g = 0.1F + this.crystalSoundIntensity * 1.2F;
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, g, f);
            this.lastCrystalSoundPlayTick = this.tickCount;
        }

    }

    protected void playSwimSound(float volume) {
        this.playSound(this.getSwimSound(), volume, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    }

    protected void onFlap() {
    }

    protected boolean isFlapping() {
        return false;
    }

    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!this.isSilent()) {
            this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch);
        }

    }

    public boolean isSilent() {
        return this.entityData.get(DATA_SILENT);
    }

    public void setSilent(boolean silent) {
        this.entityData.set(DATA_SILENT, silent);
    }

    public boolean isNoGravity() {
        return this.entityData.get(DATA_NO_GRAVITY);
    }

    public void setNoGravity(boolean noGravity) {
        this.entityData.set(DATA_NO_GRAVITY, noGravity);
    }

    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.ALL;
    }

    public boolean occludesVibrations() {
        return false;
    }

    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
        if (onGround) {
            if (this.fallDistance > 0.0F) {
                landedState.getBlock().fallOn(this.level, landedState, landedPosition, this, this.fallDistance);
                if (!landedState.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS)) {
                    this.gameEvent(GameEvent.HIT_GROUND);
                }
            }

            this.resetFallDistance();
        } else if (heightDifference < 0.0D) {
            this.fallDistance -= (float)heightDifference;
        }

    }

    public boolean fireImmune() {
        return this.getType().fireImmune();
    }

    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (this.isVehicle()) {
            for(Entity entity : this.getPassengers()) {
                entity.causeFallDamage(fallDistance, damageMultiplier, damageSource);
            }
        }

        return false;
    }

    public boolean isInWater() {
        return this.wasTouchingWater;
    }

    public boolean isInRain() {
        BlockPos blockPos = this.blockPosition();
        return this.level.isRainingAt(blockPos) || this.level.isRainingAt(new BlockPos((double)blockPos.getX(), this.getBoundingBox().maxY, (double)blockPos.getZ()));
    }

    public boolean isInBubbleColumn() {
        return this.level.getBlockState(this.blockPosition()).is(Blocks.BUBBLE_COLUMN);
    }

    public boolean isInWaterOrRain() {
        return this.isInWater() || this.isInRain();
    }

    public boolean isInWaterRainOrBubble() {
        return this.isInWater() || this.isInRain() || this.isInBubbleColumn();
    }

    public boolean isInWaterOrBubble() {
        return this.isInWater() || this.isInBubbleColumn();
    }

    public boolean isUnderWater() {
        return this.wasEyeInWater && this.isInWater();
    }

    public void updateSwimming() {
        if (this.isSwimming()) {
            this.setSwimming(this.isSprinting() && this.isInWater() && !this.isPassenger());
        } else {
            this.setSwimming(this.isSprinting() && this.isUnderWater() && !this.isPassenger() && this.level.getFluidState(this.blockPosition).is(FluidTags.WATER));
        }

    }

    protected boolean updateInWaterStateAndDoFluidPushing() {
        this.fluidHeight.clear();
        this.updateInWaterStateAndDoWaterCurrentPushing();
        double d = this.level.dimensionType().ultraWarm() ? 0.007D : 0.0023333333333333335D;
        boolean bl = this.updateFluidHeightAndDoFluidPushing(FluidTags.LAVA, d);
        return this.isInWater() || bl;
    }

    void updateInWaterStateAndDoWaterCurrentPushing() {
        if (this.getVehicle() instanceof Boat) {
            this.wasTouchingWater = false;
        } else if (this.updateFluidHeightAndDoFluidPushing(FluidTags.WATER, 0.014D)) {
            if (!this.wasTouchingWater && !this.firstTick) {
                this.doWaterSplashEffect();
            }

            this.resetFallDistance();
            this.wasTouchingWater = true;
            this.clearFire();
        } else {
            this.wasTouchingWater = false;
        }

    }

    private void updateFluidOnEyes() {
        this.wasEyeInWater = this.isEyeInFluid(FluidTags.WATER);
        this.fluidOnEyes.clear();
        double d = this.getEyeY() - (double)0.11111111F;
        Entity entity = this.getVehicle();
        if (entity instanceof Boat) {
            Boat boat = (Boat)entity;
            if (!boat.isUnderWater() && boat.getBoundingBox().maxY >= d && boat.getBoundingBox().minY <= d) {
                return;
            }
        }

        BlockPos blockPos = new BlockPos(this.getX(), d, this.getZ());
        FluidState fluidState = this.level.getFluidState(blockPos);
        double e = (double)((float)blockPos.getY() + fluidState.getHeight(this.level, blockPos));
        if (e > d) {
            fluidState.getTags().forEach(this.fluidOnEyes::add);
        }

    }

    protected void doWaterSplashEffect() {
        Entity entity = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;
        float f = entity == this ? 0.2F : 0.9F;
        Vec3 vec3 = entity.getDeltaMovement();
        float g = Math.min(1.0F, (float)Math.sqrt(vec3.x * vec3.x * (double)0.2F + vec3.y * vec3.y + vec3.z * vec3.z * (double)0.2F) * f);
        if (g < 0.25F) {
            this.playSound(this.getSwimSplashSound(), g, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        } else {
            this.playSound(this.getSwimHighSpeedSplashSound(), g, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        }

        float h = (float)Mth.floor(this.getY());

        for(int i = 0; (float)i < 1.0F + this.dimensions.width * 20.0F; ++i) {
            double d = (this.random.nextDouble() * 2.0D - 1.0D) * (double)this.dimensions.width;
            double e = (this.random.nextDouble() * 2.0D - 1.0D) * (double)this.dimensions.width;
            this.level.addParticle(ParticleTypes.BUBBLE, this.getX() + d, (double)(h + 1.0F), this.getZ() + e, vec3.x, vec3.y - this.random.nextDouble() * (double)0.2F, vec3.z);
        }

        for(int j = 0; (float)j < 1.0F + this.dimensions.width * 20.0F; ++j) {
            double k = (this.random.nextDouble() * 2.0D - 1.0D) * (double)this.dimensions.width;
            double l = (this.random.nextDouble() * 2.0D - 1.0D) * (double)this.dimensions.width;
            this.level.addParticle(ParticleTypes.SPLASH, this.getX() + k, (double)(h + 1.0F), this.getZ() + l, vec3.x, vec3.y, vec3.z);
        }

        this.gameEvent(GameEvent.SPLASH);
    }

    protected BlockState getBlockStateOn() {
        return this.level.getBlockState(this.getOnPos());
    }

    public boolean canSpawnSprintParticle() {
        return this.isSprinting() && !this.isInWater() && !this.isSpectator() && !this.isCrouching() && !this.isInLava() && this.isAlive();
    }

    protected void spawnSprintParticle() {
        int i = Mth.floor(this.getX());
        int j = Mth.floor(this.getY() - (double)0.2F);
        int k = Mth.floor(this.getZ());
        BlockPos blockPos = new BlockPos(i, j, k);
        BlockState blockState = this.level.getBlockState(blockPos);
        if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
            Vec3 vec3 = this.getDeltaMovement();
            this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState), this.getX() + (this.random.nextDouble() - 0.5D) * (double)this.dimensions.width, this.getY() + 0.1D, this.getZ() + (this.random.nextDouble() - 0.5D) * (double)this.dimensions.width, vec3.x * -4.0D, 1.5D, vec3.z * -4.0D);
        }

    }

    public boolean isEyeInFluid(TagKey<Fluid> fluidTag) {
        return this.fluidOnEyes.contains(fluidTag);
    }

    public boolean isInLava() {
        return !this.firstTick && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0D;
    }

    public void moveRelative(float speed, Vec3 movementInput) {
        Vec3 vec3 = getInputVector(movementInput, speed, this.getYRot());
        this.setDeltaMovement(this.getDeltaMovement().add(vec3));
    }

    private static Vec3 getInputVector(Vec3 movementInput, float speed, float yaw) {
        double d = movementInput.lengthSqr();
        if (d < 1.0E-7D) {
            return Vec3.ZERO;
        } else {
            Vec3 vec3 = (d > 1.0D ? movementInput.normalize() : movementInput).scale((double)speed);
            float f = Mth.sin(yaw * ((float)Math.PI / 180F));
            float g = Mth.cos(yaw * ((float)Math.PI / 180F));
            return new Vec3(vec3.x * (double)g - vec3.z * (double)f, vec3.y, vec3.z * (double)g + vec3.x * (double)f);
        }
    }

    public float getBrightness() {
        return this.level.hasChunkAt(this.getBlockX(), this.getBlockZ()) ? this.level.getBrightness(new BlockPos(this.getX(), this.getEyeY(), this.getZ())) : 0.0F;
    }

    public void absMoveTo(double x, double y, double z, float yaw, float pitch) {
        this.absMoveTo(x, y, z);
        this.setYRot(yaw % 360.0F);
        this.setXRot(Mth.clamp(pitch, -90.0F, 90.0F) % 360.0F);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void absMoveTo(double x, double y, double z) {
        double d = Mth.clamp(x, -3.0E7D, 3.0E7D);
        double e = Mth.clamp(z, -3.0E7D, 3.0E7D);
        this.xo = d;
        this.yo = y;
        this.zo = e;
        this.setPos(d, y, e);
    }

    public void moveTo(Vec3 pos) {
        this.moveTo(pos.x, pos.y, pos.z);
    }

    public void moveTo(double x, double y, double z) {
        this.moveTo(x, y, z, this.getYRot(), this.getXRot());
    }

    public void moveTo(BlockPos pos, float yaw, float pitch) {
        this.moveTo((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D, yaw, pitch);
    }

    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        this.setPosRaw(x, y, z);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setOldPosAndRot();
        this.reapplyPosition();
    }

    public final void setOldPosAndRot() {
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        this.xo = d;
        this.yo = e;
        this.zo = f;
        this.xOld = d;
        this.yOld = e;
        this.zOld = f;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public float distanceTo(Entity entity) {
        float f = (float)(this.getX() - entity.getX());
        float g = (float)(this.getY() - entity.getY());
        float h = (float)(this.getZ() - entity.getZ());
        return Mth.sqrt(f * f + g * g + h * h);
    }

    public double distanceToSqr(double x, double y, double z) {
        double d = this.getX() - x;
        double e = this.getY() - y;
        double f = this.getZ() - z;
        return d * d + e * e + f * f;
    }

    public double distanceToSqr(Entity entity) {
        return this.distanceToSqr(entity.position());
    }

    public double distanceToSqr(Vec3 vector) {
        double d = this.getX() - vector.x;
        double e = this.getY() - vector.y;
        double f = this.getZ() - vector.z;
        return d * d + e * e + f * f;
    }

    public void playerTouch(Player player) {
    }

    public void push(Entity entity) {
        if (!this.isPassengerOfSameVehicle(entity)) {
            if (!entity.noPhysics && !this.noPhysics) {
                double d = entity.getX() - this.getX();
                double e = entity.getZ() - this.getZ();
                double f = Mth.absMax(d, e);
                if (f >= (double)0.01F) {
                    f = Math.sqrt(f);
                    d /= f;
                    e /= f;
                    double g = 1.0D / f;
                    if (g > 1.0D) {
                        g = 1.0D;
                    }

                    d *= g;
                    e *= g;
                    d *= (double)0.05F;
                    e *= (double)0.05F;
                    if (!this.isVehicle()) {
                        this.push(-d, 0.0D, -e);
                    }

                    if (!entity.isVehicle()) {
                        entity.push(d, 0.0D, e);
                    }
                }

            }
        }
    }

    public void push(double deltaX, double deltaY, double deltaZ) {
        this.setDeltaMovement(this.getDeltaMovement().add(deltaX, deltaY, deltaZ));
        this.hasImpulse = true;
    }

    protected void markHurt() {
        this.hurtMarked = true;
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            this.markHurt();
            return false;
        }
    }

    public final Vec3 getViewVector(float tickDelta) {
        return this.calculateViewVector(this.getViewXRot(tickDelta), this.getViewYRot(tickDelta));
    }

    public float getViewXRot(float tickDelta) {
        return tickDelta == 1.0F ? this.getXRot() : Mth.lerp(tickDelta, this.xRotO, this.getXRot());
    }

    public float getViewYRot(float tickDelta) {
        return tickDelta == 1.0F ? this.getYRot() : Mth.lerp(tickDelta, this.yRotO, this.getYRot());
    }

    protected final Vec3 calculateViewVector(float pitch, float yaw) {
        float f = pitch * ((float)Math.PI / 180F);
        float g = -yaw * ((float)Math.PI / 180F);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3((double)(i * j), (double)(-k), (double)(h * j));
    }

    public final Vec3 getUpVector(float tickDelta) {
        return this.calculateUpVector(this.getViewXRot(tickDelta), this.getViewYRot(tickDelta));
    }

    protected final Vec3 calculateUpVector(float pitch, float yaw) {
        return this.calculateViewVector(pitch - 90.0F, yaw);
    }

    public final Vec3 getEyePosition() {
        return new Vec3(this.getX(), this.getEyeY(), this.getZ());
    }

    public final Vec3 getEyePosition(float tickDelta) {
        double d = Mth.lerp((double)tickDelta, this.xo, this.getX());
        double e = Mth.lerp((double)tickDelta, this.yo, this.getY()) + (double)this.getEyeHeight();
        double f = Mth.lerp((double)tickDelta, this.zo, this.getZ());
        return new Vec3(d, e, f);
    }

    public Vec3 getLightProbePosition(float tickDelta) {
        return this.getEyePosition(tickDelta);
    }

    public final Vec3 getPosition(float delta) {
        double d = Mth.lerp((double)delta, this.xo, this.getX());
        double e = Mth.lerp((double)delta, this.yo, this.getY());
        double f = Mth.lerp((double)delta, this.zo, this.getZ());
        return new Vec3(d, e, f);
    }

    public HitResult pick(double maxDistance, float tickDelta, boolean includeFluids) {
        Vec3 vec3 = this.getEyePosition(tickDelta);
        Vec3 vec32 = this.getViewVector(tickDelta);
        Vec3 vec33 = vec3.add(vec32.x * maxDistance, vec32.y * maxDistance, vec32.z * maxDistance);
        return this.level.clip(new ClipContext(vec3, vec33, ClipContext.Block.OUTLINE, includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
    }

    public boolean isPickable() {
        return false;
    }

    public boolean isPushable() {
        return false;
    }

    public void awardKillScore(Entity entityKilled, int score, DamageSource damageSource) {
        if (entityKilled instanceof ServerPlayer) {
            CriteriaTriggers.ENTITY_KILLED_PLAYER.trigger((ServerPlayer)entityKilled, this, damageSource);
        }

    }

    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        double d = this.getX() - cameraX;
        double e = this.getY() - cameraY;
        double f = this.getZ() - cameraZ;
        double g = d * d + e * e + f * f;
        return this.shouldRenderAtSqrDistance(g);
    }

    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize();
        if (Double.isNaN(d)) {
            d = 1.0D;
        }

        d *= 64.0D * viewScale;
        return distance < d * d;
    }

    public boolean saveAsPassenger(CompoundTag nbt) {
        if (this.removalReason != null && !this.removalReason.shouldSave()) {
            return false;
        } else {
            String string = this.getEncodeId();
            if (string == null) {
                return false;
            } else {
                nbt.putString("id", string);
                this.saveWithoutId(nbt);
                return true;
            }
        }
    }

    public boolean save(CompoundTag nbt) {
        return this.isPassenger() ? false : this.saveAsPassenger(nbt);
    }

    public CompoundTag saveWithoutId(CompoundTag nbt) {
        try {
            if (this.vehicle != null) {
                nbt.put("Pos", this.newDoubleList(this.vehicle.getX(), this.getY(), this.vehicle.getZ()));
            } else {
                nbt.put("Pos", this.newDoubleList(this.getX(), this.getY(), this.getZ()));
            }

            Vec3 vec3 = this.getDeltaMovement();
            nbt.put("Motion", this.newDoubleList(vec3.x, vec3.y, vec3.z));
            nbt.put("Rotation", this.newFloatList(this.getYRot(), this.getXRot()));
            nbt.putFloat("FallDistance", this.fallDistance);
            nbt.putShort("Fire", (short)this.remainingFireTicks);
            nbt.putShort("Air", (short)this.getAirSupply());
            nbt.putBoolean("OnGround", this.onGround);
            nbt.putBoolean("Invulnerable", this.invulnerable);
            nbt.putInt("PortalCooldown", this.portalCooldown);
            nbt.putUUID("UUID", this.getUUID());
            Component component = this.getCustomName();
            if (component != null) {
                nbt.putString("CustomName", Component.Serializer.toJson(component));
            }

            if (this.isCustomNameVisible()) {
                nbt.putBoolean("CustomNameVisible", this.isCustomNameVisible());
            }

            if (this.isSilent()) {
                nbt.putBoolean("Silent", this.isSilent());
            }

            if (this.isNoGravity()) {
                nbt.putBoolean("NoGravity", this.isNoGravity());
            }

            if (this.hasGlowingTag) {
                nbt.putBoolean("Glowing", true);
            }

            int i = this.getTicksFrozen();
            if (i > 0) {
                nbt.putInt("TicksFrozen", this.getTicksFrozen());
            }

            if (this.hasVisualFire) {
                nbt.putBoolean("HasVisualFire", this.hasVisualFire);
            }

            if (!this.tags.isEmpty()) {
                ListTag listTag = new ListTag();

                for(String string : this.tags) {
                    listTag.add(StringTag.valueOf(string));
                }

                nbt.put("Tags", listTag);
            }

            this.addAdditionalSaveData(nbt);
            if (this.isVehicle()) {
                ListTag listTag2 = new ListTag();

                for(Entity entity : this.getPassengers()) {
                    CompoundTag compoundTag = new CompoundTag();
                    if (entity.saveAsPassenger(compoundTag)) {
                        listTag2.add(compoundTag);
                    }
                }

                if (!listTag2.isEmpty()) {
                    nbt.put("Passengers", listTag2);
                }
            }

            return nbt;
        } catch (Throwable var9) {
            CrashReport crashReport = CrashReport.forThrowable(var9, "Saving entity NBT");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Entity being saved");
            this.fillCrashReportCategory(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    public void load(CompoundTag nbt) {
        try {
            ListTag listTag = nbt.getList("Pos", 6);
            ListTag listTag2 = nbt.getList("Motion", 6);
            ListTag listTag3 = nbt.getList("Rotation", 5);
            double d = listTag2.getDouble(0);
            double e = listTag2.getDouble(1);
            double f = listTag2.getDouble(2);
            this.setDeltaMovement(Math.abs(d) > 10.0D ? 0.0D : d, Math.abs(e) > 10.0D ? 0.0D : e, Math.abs(f) > 10.0D ? 0.0D : f);
            this.setPosRaw(listTag.getDouble(0), Mth.clamp(listTag.getDouble(1), -2.0E7D, 2.0E7D), listTag.getDouble(2));
            this.setYRot(listTag3.getFloat(0));
            this.setXRot(listTag3.getFloat(1));
            this.setOldPosAndRot();
            this.setYHeadRot(this.getYRot());
            this.setYBodyRot(this.getYRot());
            this.fallDistance = nbt.getFloat("FallDistance");
            this.remainingFireTicks = nbt.getShort("Fire");
            if (nbt.contains("Air")) {
                this.setAirSupply(nbt.getShort("Air"));
            }

            this.onGround = nbt.getBoolean("OnGround");
            this.invulnerable = nbt.getBoolean("Invulnerable");
            this.portalCooldown = nbt.getInt("PortalCooldown");
            if (nbt.hasUUID("UUID")) {
                this.uuid = nbt.getUUID("UUID");
                this.stringUUID = this.uuid.toString();
            }

            if (Double.isFinite(this.getX()) && Double.isFinite(this.getY()) && Double.isFinite(this.getZ())) {
                if (Double.isFinite((double)this.getYRot()) && Double.isFinite((double)this.getXRot())) {
                    this.reapplyPosition();
                    this.setRot(this.getYRot(), this.getXRot());
                    if (nbt.contains("CustomName", 8)) {
                        String string = nbt.getString("CustomName");

                        try {
                            this.setCustomName(Component.Serializer.fromJson(string));
                        } catch (Exception var14) {
                            LOGGER.warn("Failed to parse entity custom name {}", string, var14);
                        }
                    }

                    this.setCustomNameVisible(nbt.getBoolean("CustomNameVisible"));
                    this.setSilent(nbt.getBoolean("Silent"));
                    this.setNoGravity(nbt.getBoolean("NoGravity"));
                    this.setGlowingTag(nbt.getBoolean("Glowing"));
                    this.setTicksFrozen(nbt.getInt("TicksFrozen"));
                    this.hasVisualFire = nbt.getBoolean("HasVisualFire");
                    if (nbt.contains("Tags", 9)) {
                        this.tags.clear();
                        ListTag listTag4 = nbt.getList("Tags", 8);
                        int i = Math.min(listTag4.size(), 1024);

                        for(int j = 0; j < i; ++j) {
                            this.tags.add(listTag4.getString(j));
                        }
                    }

                    this.readAdditionalSaveData(nbt);
                    if (this.repositionEntityAfterLoad()) {
                        this.reapplyPosition();
                    }

                } else {
                    throw new IllegalStateException("Entity has invalid rotation");
                }
            } else {
                throw new IllegalStateException("Entity has invalid position");
            }
        } catch (Throwable var15) {
            CrashReport crashReport = CrashReport.forThrowable(var15, "Loading entity NBT");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Entity being loaded");
            this.fillCrashReportCategory(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    protected boolean repositionEntityAfterLoad() {
        return true;
    }

    @Nullable
    public final String getEncodeId() {
        EntityType<?> entityType = this.getType();
        ResourceLocation resourceLocation = EntityType.getKey(entityType);
        return entityType.canSerialize() && resourceLocation != null ? resourceLocation.toString() : null;
    }

    protected abstract void readAdditionalSaveData(CompoundTag nbt);

    protected abstract void addAdditionalSaveData(CompoundTag nbt);

    protected ListTag newDoubleList(double... values) {
        ListTag listTag = new ListTag();

        for(double d : values) {
            listTag.add(DoubleTag.valueOf(d));
        }

        return listTag;
    }

    protected ListTag newFloatList(float... values) {
        ListTag listTag = new ListTag();

        for(float f : values) {
            listTag.add(FloatTag.valueOf(f));
        }

        return listTag;
    }

    @Nullable
    public ItemEntity spawnAtLocation(ItemLike item) {
        return this.spawnAtLocation(item, 0);
    }

    @Nullable
    public ItemEntity spawnAtLocation(ItemLike item, int yOffset) {
        return this.spawnAtLocation(new ItemStack(item), (float)yOffset);
    }

    @Nullable
    public ItemEntity spawnAtLocation(ItemStack stack) {
        return this.spawnAtLocation(stack, 0.0F);
    }

    @Nullable
    public ItemEntity spawnAtLocation(ItemStack stack, float yOffset) {
        if (stack.isEmpty()) {
            return null;
        } else if (this.level.isClientSide) {
            return null;
        } else {
            ItemEntity itemEntity = new ItemEntity(this.level, this.getX(), this.getY() + (double)yOffset, this.getZ(), stack);
            itemEntity.setDefaultPickUpDelay();
            this.level.addFreshEntity(itemEntity);
            return itemEntity;
        }
    }

    public boolean isAlive() {
        return !this.isRemoved();
    }

    public boolean isInWall() {
        if (this.noPhysics) {
            return false;
        } else {
            float f = this.dimensions.width * 0.8F;
            AABB aABB = AABB.ofSize(this.getEyePosition(), (double)f, 1.0E-6D, (double)f);
            return BlockPos.betweenClosedStream(aABB).anyMatch((pos) -> {
                BlockState blockState = this.level.getBlockState(pos);
                return !blockState.isAir() && blockState.isSuffocating(this.level, pos) && Shapes.joinIsNotEmpty(blockState.getCollisionShape(this.level, pos).move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()), Shapes.create(aABB), BooleanOp.AND);
            });
        }
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    public boolean canCollideWith(Entity other) {
        return other.canBeCollidedWith() && !this.isPassengerOfSameVehicle(other);
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    public void rideTick() {
        this.setDeltaMovement(Vec3.ZERO);
        this.tick();
        if (this.isPassenger()) {
            this.getVehicle().positionRider(this);
        }
    }

    public void positionRider(Entity passenger) {
        this.positionRider(passenger, Entity::setPos);
    }

    private void positionRider(Entity passenger, Entity.MoveFunction positionUpdater) {
        if (this.hasPassenger(passenger)) {
            double d = this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset();
            positionUpdater.accept(passenger, this.getX(), d, this.getZ());
        }
    }

    public void onPassengerTurned(Entity passenger) {
    }

    public double getMyRidingOffset() {
        return 0.0D;
    }

    public double getPassengersRidingOffset() {
        return (double)this.dimensions.height * 0.75D;
    }

    public boolean startRiding(Entity entity) {
        return this.startRiding(entity, false);
    }

    public boolean showVehicleHealth() {
        return this instanceof LivingEntity;
    }

    public boolean startRiding(Entity entity, boolean force) {
        if (entity == this.vehicle) {
            return false;
        } else {
            for(Entity entity2 = entity; entity2.vehicle != null; entity2 = entity2.vehicle) {
                if (entity2.vehicle == this) {
                    return false;
                }
            }

            if (force || this.canRide(entity) && entity.canAddPassenger(this)) {
                if (this.isPassenger()) {
                    this.stopRiding();
                }

                this.setPose(Pose.STANDING);
                this.vehicle = entity;
                this.vehicle.addPassenger(this);
                entity.getIndirectPassengersStream().filter((passenger) -> {
                    return passenger instanceof ServerPlayer;
                }).forEach((player) -> {
                    CriteriaTriggers.START_RIDING_TRIGGER.trigger((ServerPlayer)player);
                });
                return true;
            } else {
                return false;
            }
        }
    }

    protected boolean canRide(Entity entity) {
        return !this.isShiftKeyDown() && this.boardingCooldown <= 0;
    }

    protected boolean canEnterPose(Pose pose) {
        return this.level.noCollision(this, this.getBoundingBoxForPose(pose).deflate(1.0E-7D));
    }

    public void ejectPassengers() {
        for(int i = this.passengers.size() - 1; i >= 0; --i) {
            this.passengers.get(i).stopRiding();
        }

    }

    public void removeVehicle() {
        if (this.vehicle != null) {
            Entity entity = this.vehicle;
            this.vehicle = null;
            entity.removePassenger(this);
        }

    }

    public void stopRiding() {
        this.removeVehicle();
    }

    protected void addPassenger(Entity passenger) {
        if (passenger.getVehicle() != this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        } else {
            if (this.passengers.isEmpty()) {
                this.passengers = ImmutableList.of(passenger);
            } else {
                List<Entity> list = Lists.newArrayList(this.passengers);
                if (!this.level.isClientSide && passenger instanceof Player && !(this.getControllingPassenger() instanceof Player)) {
                    list.add(0, passenger);
                } else {
                    list.add(passenger);
                }

                this.passengers = ImmutableList.copyOf(list);
            }

        }
    }

    protected void removePassenger(Entity passenger) {
        if (passenger.getVehicle() == this) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        } else {
            if (this.passengers.size() == 1 && this.passengers.get(0) == passenger) {
                this.passengers = ImmutableList.of();
            } else {
                this.passengers = this.passengers.stream().filter((entity2) -> {
                    return entity2 != passenger;
                }).collect(ImmutableList.toImmutableList());
            }

            passenger.boardingCooldown = 60;
        }
    }

    protected boolean canAddPassenger(Entity passenger) {
        return this.passengers.isEmpty();
    }

    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.setPos(x, y, z);
        this.setRot(yaw, pitch);
    }

    public void lerpHeadTo(float yaw, int interpolationSteps) {
        this.setYHeadRot(yaw);
    }

    public float getPickRadius() {
        return 0.0F;
    }

    public Vec3 getLookAngle() {
        return this.calculateViewVector(this.getXRot(), this.getYRot());
    }

    public Vec3 getHandHoldingItemAngle(Item item) {
        if (!(this instanceof Player)) {
            return Vec3.ZERO;
        } else {
            Player player = (Player)this;
            boolean bl = player.getOffhandItem().is(item) && !player.getMainHandItem().is(item);
            HumanoidArm humanoidArm = bl ? player.getMainArm().getOpposite() : player.getMainArm();
            return this.calculateViewVector(0.0F, this.getYRot() + (float)(humanoidArm == HumanoidArm.RIGHT ? 80 : -80)).scale(0.5D);
        }
    }

    public Vec2 getRotationVector() {
        return new Vec2(this.getXRot(), this.getYRot());
    }

    public Vec3 getForward() {
        return Vec3.directionFromRotation(this.getRotationVector());
    }

    public void handleInsidePortal(BlockPos pos) {
        if (this.isOnPortalCooldown()) {
            this.setPortalCooldown();
        } else {
            if (!this.level.isClientSide && !pos.equals(this.portalEntrancePos)) {
                this.portalEntrancePos = pos.immutable();
            }

            this.isInsidePortal = true;
        }
    }

    protected void handleNetherPortal() {
        if (this.level instanceof ServerLevel) {
            int i = this.getPortalWaitTime();
            ServerLevel serverLevel = (ServerLevel)this.level;
            if (this.isInsidePortal) {
                MinecraftServer minecraftServer = serverLevel.getServer();
                ResourceKey<Level> resourceKey = this.level.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
                ServerLevel serverLevel2 = minecraftServer.getLevel(resourceKey);
                if (serverLevel2 != null && minecraftServer.isNetherEnabled() && !this.isPassenger() && this.portalTime++ >= i) {
                    this.level.getProfiler().push("portal");
                    this.portalTime = i;
                    this.setPortalCooldown();
                    this.changeDimension(serverLevel2);
                    this.level.getProfiler().pop();
                }

                this.isInsidePortal = false;
            } else {
                if (this.portalTime > 0) {
                    this.portalTime -= 4;
                }

                if (this.portalTime < 0) {
                    this.portalTime = 0;
                }
            }

            this.processPortalCooldown();
        }
    }

    public int getDimensionChangingDelay() {
        return 300;
    }

    public void lerpMotion(double x, double y, double z) {
        this.setDeltaMovement(x, y, z);
    }

    public void handleEntityEvent(byte status) {
        switch(status) {
        case 53:
            HoneyBlock.showSlideParticles(this);
        default:
        }
    }

    public void animateHurt() {
    }

    public Iterable<ItemStack> getHandSlots() {
        return EMPTY_LIST;
    }

    public Iterable<ItemStack> getArmorSlots() {
        return EMPTY_LIST;
    }

    public Iterable<ItemStack> getAllSlots() {
        return Iterables.concat(this.getHandSlots(), this.getArmorSlots());
    }

    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    public boolean isOnFire() {
        boolean bl = this.level != null && this.level.isClientSide;
        return !this.fireImmune() && (this.remainingFireTicks > 0 || bl && this.getSharedFlag(0));
    }

    public boolean isPassenger() {
        return this.getVehicle() != null;
    }

    public boolean isVehicle() {
        return !this.passengers.isEmpty();
    }

    public boolean rideableUnderWater() {
        return true;
    }

    public void setShiftKeyDown(boolean sneaking) {
        this.setSharedFlag(1, sneaking);
    }

    public boolean isShiftKeyDown() {
        return this.getSharedFlag(1);
    }

    public boolean isSteppingCarefully() {
        return this.isShiftKeyDown();
    }

    public boolean isSuppressingBounce() {
        return this.isShiftKeyDown();
    }

    public boolean isDiscrete() {
        return this.isShiftKeyDown();
    }

    public boolean isDescending() {
        return this.isShiftKeyDown();
    }

    public boolean isCrouching() {
        return this.getPose() == Pose.CROUCHING;
    }

    public boolean isSprinting() {
        return this.getSharedFlag(3);
    }

    public void setSprinting(boolean sprinting) {
        this.setSharedFlag(3, sprinting);
    }

    public boolean isSwimming() {
        return this.getSharedFlag(4);
    }

    public boolean isVisuallySwimming() {
        return this.getPose() == Pose.SWIMMING;
    }

    public boolean isVisuallyCrawling() {
        return this.isVisuallySwimming() && !this.isInWater();
    }

    public void setSwimming(boolean swimming) {
        this.setSharedFlag(4, swimming);
    }

    public final boolean hasGlowingTag() {
        return this.hasGlowingTag;
    }

    public final void setGlowingTag(boolean glowing) {
        this.hasGlowingTag = glowing;
        this.setSharedFlag(6, this.isCurrentlyGlowing());
    }

    public boolean isCurrentlyGlowing() {
        return this.level.isClientSide() ? this.getSharedFlag(6) : this.hasGlowingTag;
    }

    public boolean isInvisible() {
        return this.getSharedFlag(5);
    }

    public boolean isInvisibleTo(Player player) {
        if (player.isSpectator()) {
            return false;
        } else {
            Team team = this.getTeam();
            return team != null && player != null && player.getTeam() == team && team.canSeeFriendlyInvisibles() ? false : this.isInvisible();
        }
    }

    @Nullable
    public GameEventListenerRegistrar getGameEventListenerRegistrar() {
        return null;
    }

    @Nullable
    public Team getTeam() {
        return this.level.getScoreboard().getPlayersTeam(this.getScoreboardName());
    }

    public boolean isAlliedTo(Entity other) {
        return this.isAlliedTo(other.getTeam());
    }

    public boolean isAlliedTo(Team team) {
        return this.getTeam() != null ? this.getTeam().isAlliedTo(team) : false;
    }

    public void setInvisible(boolean invisible) {
        this.setSharedFlag(5, invisible);
    }

    public boolean getSharedFlag(int index) {
        return (this.entityData.get(DATA_SHARED_FLAGS_ID) & 1 << index) != 0;
    }

    public void setSharedFlag(int index, boolean value) {
        byte b = this.entityData.get(DATA_SHARED_FLAGS_ID);
        if (value) {
            this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(b | 1 << index));
        } else {
            this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(b & ~(1 << index)));
        }

    }

    public int getMaxAirSupply() {
        return 300;
    }

    public int getAirSupply() {
        return this.entityData.get(DATA_AIR_SUPPLY_ID);
    }

    public void setAirSupply(int air) {
        this.entityData.set(DATA_AIR_SUPPLY_ID, air);
    }

    public int getTicksFrozen() {
        return this.entityData.get(DATA_TICKS_FROZEN);
    }

    public void setTicksFrozen(int frozenTicks) {
        this.entityData.set(DATA_TICKS_FROZEN, frozenTicks);
    }

    public float getPercentFrozen() {
        int i = this.getTicksRequiredToFreeze();
        return (float)Math.min(this.getTicksFrozen(), i) / (float)i;
    }

    public boolean isFullyFrozen() {
        return this.getTicksFrozen() >= this.getTicksRequiredToFreeze();
    }

    public int getTicksRequiredToFreeze() {
        return 140;
    }

    public void thunderHit(ServerLevel world, LightningBolt lightning) {
        this.setRemainingFireTicks(this.remainingFireTicks + 1);
        if (this.remainingFireTicks == 0) {
            this.setSecondsOnFire(8);
        }

        this.hurt(DamageSource.LIGHTNING_BOLT, 5.0F);
    }

    public void onAboveBubbleCol(boolean drag) {
        Vec3 vec3 = this.getDeltaMovement();
        double d;
        if (drag) {
            d = Math.max(-0.9D, vec3.y - 0.03D);
        } else {
            d = Math.min(1.8D, vec3.y + 0.1D);
        }

        this.setDeltaMovement(vec3.x, d, vec3.z);
    }

    public void onInsideBubbleColumn(boolean drag) {
        Vec3 vec3 = this.getDeltaMovement();
        double d;
        if (drag) {
            d = Math.max(-0.3D, vec3.y - 0.03D);
        } else {
            d = Math.min(0.7D, vec3.y + 0.06D);
        }

        this.setDeltaMovement(vec3.x, d, vec3.z);
        this.resetFallDistance();
    }

    public void killed(ServerLevel world, LivingEntity other) {
    }

    public void resetFallDistance() {
        this.fallDistance = 0.0F;
    }

    protected void moveTowardsClosestSpace(double x, double y, double z) {
        BlockPos blockPos = new BlockPos(x, y, z);
        Vec3 vec3 = new Vec3(x - (double)blockPos.getX(), y - (double)blockPos.getY(), z - (double)blockPos.getZ());
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        Direction direction = Direction.UP;
        double d = Double.MAX_VALUE;

        for(Direction direction2 : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
            mutableBlockPos.setWithOffset(blockPos, direction2);
            if (!this.level.getBlockState(mutableBlockPos).isCollisionShapeFullBlock(this.level, mutableBlockPos)) {
                double e = vec3.get(direction2.getAxis());
                double f = direction2.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0D - e : e;
                if (f < d) {
                    d = f;
                    direction = direction2;
                }
            }
        }

        float g = this.random.nextFloat() * 0.2F + 0.1F;
        float h = (float)direction.getAxisDirection().getStep();
        Vec3 vec32 = this.getDeltaMovement().scale(0.75D);
        if (direction.getAxis() == Direction.Axis.X) {
            this.setDeltaMovement((double)(h * g), vec32.y, vec32.z);
        } else if (direction.getAxis() == Direction.Axis.Y) {
            this.setDeltaMovement(vec32.x, (double)(h * g), vec32.z);
        } else if (direction.getAxis() == Direction.Axis.Z) {
            this.setDeltaMovement(vec32.x, vec32.y, (double)(h * g));
        }

    }

    public void makeStuckInBlock(BlockState state, Vec3 multiplier) {
        this.resetFallDistance();
        this.stuckSpeedMultiplier = multiplier;
    }

    private static Component removeAction(Component textComponent) {
        MutableComponent mutableComponent = textComponent.plainCopy().setStyle(textComponent.getStyle().withClickEvent((ClickEvent)null));

        for(Component component : textComponent.getSiblings()) {
            mutableComponent.append(removeAction(component));
        }

        return mutableComponent;
    }

    @Override
    public Component getName() {
        Component component = this.getCustomName();
        return component != null ? removeAction(component) : this.getTypeName();
    }

    protected Component getTypeName() {
        return this.type.getDescription();
    }

    public boolean is(Entity entity) {
        return this == entity;
    }

    public float getYHeadRot() {
        return 0.0F;
    }

    public void setYHeadRot(float headYaw) {
    }

    public void setYBodyRot(float bodyYaw) {
    }

    public boolean isAttackable() {
        return true;
    }

    public boolean skipAttackInteraction(Entity attacker) {
        return false;
    }

    @Override
    public String toString() {
        String string = this.level == null ? "~NULL~" : this.level.toString();
        return this.removalReason != null ? String.format(Locale.ROOT, "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f, removed=%s]", this.getClass().getSimpleName(), this.getName().getString(), this.id, string, this.getX(), this.getY(), this.getZ(), this.removalReason) : String.format(Locale.ROOT, "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f]", this.getClass().getSimpleName(), this.getName().getString(), this.id, string, this.getX(), this.getY(), this.getZ());
    }

    public boolean isInvulnerableTo(DamageSource damageSource) {
        return this.isRemoved() || this.invulnerable && damageSource != DamageSource.OUT_OF_WORLD && !damageSource.isCreativePlayer();
    }

    public boolean isInvulnerable() {
        return this.invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public void copyPosition(Entity entity) {
        this.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
    }

    public void restoreFrom(Entity original) {
        CompoundTag compoundTag = original.saveWithoutId(new CompoundTag());
        compoundTag.remove("Dimension");
        this.load(compoundTag);
        this.portalCooldown = original.portalCooldown;
        this.portalEntrancePos = original.portalEntrancePos;
    }

    @Nullable
    public Entity changeDimension(ServerLevel destination) {
        if (this.level instanceof ServerLevel && !this.isRemoved()) {
            this.level.getProfiler().push("changeDimension");
            this.unRide();
            this.level.getProfiler().push("reposition");
            PortalInfo portalInfo = this.findDimensionEntryPoint(destination);
            if (portalInfo == null) {
                return null;
            } else {
                this.level.getProfiler().popPush("reloading");
                Entity entity = this.getType().create(destination);
                if (entity != null) {
                    entity.restoreFrom(this);
                    entity.moveTo(portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z, portalInfo.yRot, entity.getXRot());
                    entity.setDeltaMovement(portalInfo.speed);
                    destination.addDuringTeleport(entity);
                    if (destination.dimension() == Level.END) {
                        ServerLevel.makeObsidianPlatform(destination);
                    }
                }

                this.removeAfterChangingDimensions();
                this.level.getProfiler().pop();
                ((ServerLevel)this.level).resetEmptyTime();
                destination.resetEmptyTime();
                this.level.getProfiler().pop();
                return entity;
            }
        } else {
            return null;
        }
    }

    protected void removeAfterChangingDimensions() {
        this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
    }

    @Nullable
    protected PortalInfo findDimensionEntryPoint(ServerLevel destination) {
        boolean bl = this.level.dimension() == Level.END && destination.dimension() == Level.OVERWORLD;
        boolean bl2 = destination.dimension() == Level.END;
        if (!bl && !bl2) {
            boolean bl3 = destination.dimension() == Level.NETHER;
            if (this.level.dimension() != Level.NETHER && !bl3) {
                return null;
            } else {
                WorldBorder worldBorder = destination.getWorldBorder();
                double d = DimensionType.getTeleportationScale(this.level.dimensionType(), destination.dimensionType());
                BlockPos blockPos3 = worldBorder.clampToBounds(this.getX() * d, this.getY(), this.getZ() * d);
                return this.getExitPortal(destination, blockPos3, bl3, worldBorder).map((rect) -> {
                    BlockState blockState = this.level.getBlockState(this.portalEntrancePos);
                    Direction.Axis axis;
                    Vec3 vec3;
                    if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                        axis = blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
                        BlockUtil.FoundRectangle foundRectangle = BlockUtil.getLargestRectangleAround(this.portalEntrancePos, axis, 21, Direction.Axis.Y, 21, (blockPos) -> {
                            return this.level.getBlockState(blockPos) == blockState;
                        });
                        vec3 = this.getRelativePortalPosition(axis, foundRectangle);
                    } else {
                        axis = Direction.Axis.X;
                        vec3 = new Vec3(0.5D, 0.0D, 0.0D);
                    }

                    return PortalShape.createPortalInfo(destination, rect, axis, vec3, this.getDimensions(this.getPose()), this.getDeltaMovement(), this.getYRot(), this.getXRot());
                }).orElse((PortalInfo)null);
            }
        } else {
            BlockPos blockPos;
            if (bl2) {
                blockPos = ServerLevel.END_SPAWN_POINT;
            } else {
                blockPos = destination.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, destination.getSharedSpawnPos());
            }

            return new PortalInfo(new Vec3((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D), this.getDeltaMovement(), this.getYRot(), this.getXRot());
        }
    }

    protected Vec3 getRelativePortalPosition(Direction.Axis portalAxis, BlockUtil.FoundRectangle portalRect) {
        return PortalShape.getRelativePosition(portalRect, portalAxis, this.position(), this.getDimensions(this.getPose()));
    }

    protected Optional<BlockUtil.FoundRectangle> getExitPortal(ServerLevel destWorld, BlockPos destPos, boolean destIsNether, WorldBorder worldBorder) {
        return destWorld.getPortalForcer().findPortalAround(destPos, destIsNether, worldBorder);
    }

    public boolean canChangeDimensions() {
        return true;
    }

    public float getBlockExplosionResistance(Explosion explosion, BlockGetter world, BlockPos pos, BlockState blockState, FluidState fluidState, float max) {
        return max;
    }

    public boolean shouldBlockExplode(Explosion explosion, BlockGetter world, BlockPos pos, BlockState state, float explosionPower) {
        return true;
    }

    public int getMaxFallDistance() {
        return 3;
    }

    public boolean isIgnoringBlockTriggers() {
        return false;
    }

    public void fillCrashReportCategory(CrashReportCategory section) {
        section.setDetail("Entity Type", () -> {
            return EntityType.getKey(this.getType()) + " (" + this.getClass().getCanonicalName() + ")";
        });
        section.setDetail("Entity ID", this.id);
        section.setDetail("Entity Name", () -> {
            return this.getName().getString();
        });
        section.setDetail("Entity's Exact location", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
        section.setDetail("Entity's Block location", CrashReportCategory.formatLocation(this.level, Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ())));
        Vec3 vec3 = this.getDeltaMovement();
        section.setDetail("Entity's Momentum", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec3.x, vec3.y, vec3.z));
        section.setDetail("Entity's Passengers", () -> {
            return this.getPassengers().toString();
        });
        section.setDetail("Entity's Vehicle", () -> {
            return String.valueOf((Object)this.getVehicle());
        });
    }

    public boolean displayFireAnimation() {
        return this.isOnFire() && !this.isSpectator();
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
        this.stringUUID = this.uuid.toString();
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    public String getStringUUID() {
        return this.stringUUID;
    }

    public String getScoreboardName() {
        return this.stringUUID;
    }

    public boolean isPushedByFluid() {
        return true;
    }

    public static double getViewScale() {
        return viewScale;
    }

    public static void setViewScale(double value) {
        viewScale = value;
    }

    @Override
    public Component getDisplayName() {
        return PlayerTeam.formatNameForTeam(this.getTeam(), this.getName()).withStyle((style) -> {
            return style.withHoverEvent(this.createHoverEvent()).withInsertion(this.getStringUUID());
        });
    }

    public void setCustomName(@Nullable Component name) {
        this.entityData.set(DATA_CUSTOM_NAME, Optional.ofNullable(name));
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.entityData.get(DATA_CUSTOM_NAME).orElse((Component)null);
    }

    @Override
    public boolean hasCustomName() {
        return this.entityData.get(DATA_CUSTOM_NAME).isPresent();
    }

    public void setCustomNameVisible(boolean visible) {
        this.entityData.set(DATA_CUSTOM_NAME_VISIBLE, visible);
    }

    public boolean isCustomNameVisible() {
        return this.entityData.get(DATA_CUSTOM_NAME_VISIBLE);
    }

    public final void teleportToWithTicket(double destX, double destY, double destZ) {
        if (this.level instanceof ServerLevel) {
            ChunkPos chunkPos = new ChunkPos(new BlockPos(destX, destY, destZ));
            ((ServerLevel)this.level).getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 0, this.getId());
            this.level.getChunk(chunkPos.x, chunkPos.z);
            this.teleportTo(destX, destY, destZ);
        }
    }

    public void dismountTo(double destX, double destY, double destZ) {
        this.teleportTo(destX, destY, destZ);
    }

    public void teleportTo(double destX, double destY, double destZ) {
        if (this.level instanceof ServerLevel) {
            this.moveTo(destX, destY, destZ, this.getYRot(), this.getXRot());
            this.getSelfAndPassengers().forEach((entity) -> {
                for(Entity entity2 : entity.passengers) {
                    entity.positionRider(entity2, Entity::moveTo);
                }

            });
        }
    }

    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (DATA_POSE.equals(data)) {
            this.refreshDimensions();
        }

    }

    public void refreshDimensions() {
        EntityDimensions entityDimensions = this.dimensions;
        Pose pose = this.getPose();
        EntityDimensions entityDimensions2 = this.getDimensions(pose);
        this.dimensions = entityDimensions2;
        this.eyeHeight = this.getEyeHeight(pose, entityDimensions2);
        this.reapplyPosition();
        boolean bl = (double)entityDimensions2.width <= 4.0D && (double)entityDimensions2.height <= 4.0D;
        if (!this.level.isClientSide && !this.firstTick && !this.noPhysics && bl && (entityDimensions2.width > entityDimensions.width || entityDimensions2.height > entityDimensions.height) && !(this instanceof Player)) {
            Vec3 vec3 = this.position().add(0.0D, (double)entityDimensions.height / 2.0D, 0.0D);
            double d = (double)Math.max(0.0F, entityDimensions2.width - entityDimensions.width) + 1.0E-6D;
            double e = (double)Math.max(0.0F, entityDimensions2.height - entityDimensions.height) + 1.0E-6D;
            VoxelShape voxelShape = Shapes.create(AABB.ofSize(vec3, d, e, d));
            this.level.findFreePosition(this, voxelShape, vec3, (double)entityDimensions2.width, (double)entityDimensions2.height, (double)entityDimensions2.width).ifPresent((pos) -> {
                this.setPos(pos.add(0.0D, (double)(-entityDimensions2.height) / 2.0D, 0.0D));
            });
        }

    }

    public Direction getDirection() {
        return Direction.fromYRot((double)this.getYRot());
    }

    public Direction getMotionDirection() {
        return this.getDirection();
    }

    protected HoverEvent createHoverEvent() {
        return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityTooltipInfo(this.getType(), this.getUUID(), this.getName()));
    }

    public boolean broadcastToPlayer(ServerPlayer spectator) {
        return true;
    }

    @Override
    public final AABB getBoundingBox() {
        return this.bb;
    }

    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox();
    }

    protected AABB getBoundingBoxForPose(Pose pos) {
        EntityDimensions entityDimensions = this.getDimensions(pos);
        float f = entityDimensions.width / 2.0F;
        Vec3 vec3 = new Vec3(this.getX() - (double)f, this.getY(), this.getZ() - (double)f);
        Vec3 vec32 = new Vec3(this.getX() + (double)f, this.getY() + (double)entityDimensions.height, this.getZ() + (double)f);
        return new AABB(vec3, vec32);
    }

    public final void setBoundingBox(AABB boundingBox) {
        this.bb = boundingBox;
    }

    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.85F;
    }

    public float getEyeHeight(Pose pose) {
        return this.getEyeHeight(pose, this.getDimensions(pose));
    }

    public final float getEyeHeight() {
        return this.eyeHeight;
    }

    public Vec3 getLeashOffset() {
        return new Vec3(0.0D, (double)this.getEyeHeight(), (double)(this.getBbWidth() * 0.4F));
    }

    public SlotAccess getSlot(int mappedIndex) {
        return SlotAccess.NULL;
    }

    @Override
    public void sendMessage(Component message, UUID sender) {
    }

    public Level getCommandSenderWorld() {
        return this.level;
    }

    @Nullable
    public MinecraftServer getServer() {
        return this.level.getServer();
    }

    public InteractionResult interactAt(Player player, Vec3 hitPos, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    public boolean ignoreExplosion() {
        return false;
    }

    public void doEnchantDamageEffects(LivingEntity attacker, Entity target) {
        if (target instanceof LivingEntity) {
            EnchantmentHelper.doPostHurtEffects((LivingEntity)target, attacker);
        }

        EnchantmentHelper.doPostDamageEffects(attacker, target);
    }

    public void startSeenByPlayer(ServerPlayer player) {
    }

    public void stopSeenByPlayer(ServerPlayer player) {
    }

    public float rotate(Rotation rotation) {
        float f = Mth.wrapDegrees(this.getYRot());
        switch(rotation) {
        case CLOCKWISE_180:
            return f + 180.0F;
        case COUNTERCLOCKWISE_90:
            return f + 270.0F;
        case CLOCKWISE_90:
            return f + 90.0F;
        default:
            return f;
        }
    }

    public float mirror(Mirror mirror) {
        float f = Mth.wrapDegrees(this.getYRot());
        switch(mirror) {
        case FRONT_BACK:
            return -f;
        case LEFT_RIGHT:
            return 180.0F - f;
        default:
            return f;
        }
    }

    public boolean onlyOpCanSetNbt() {
        return false;
    }

    @Nullable
    public Entity getControllingPassenger() {
        return null;
    }

    public final List<Entity> getPassengers() {
        return this.passengers;
    }

    @Nullable
    public Entity getFirstPassenger() {
        return this.passengers.isEmpty() ? null : this.passengers.get(0);
    }

    public boolean hasPassenger(Entity passenger) {
        return this.passengers.contains(passenger);
    }

    public boolean hasPassenger(Predicate<Entity> predicate) {
        for(Entity entity : this.passengers) {
            if (predicate.test(entity)) {
                return true;
            }
        }

        return false;
    }

    private Stream<Entity> getIndirectPassengersStream() {
        return this.passengers.stream().flatMap(Entity::getSelfAndPassengers);
    }

    @Override
    public Stream<Entity> getSelfAndPassengers() {
        return Stream.concat(Stream.of(this), this.getIndirectPassengersStream());
    }

    @Override
    public Stream<Entity> getPassengersAndSelf() {
        return Stream.concat(this.passengers.stream().flatMap(Entity::getPassengersAndSelf), Stream.of(this));
    }

    public Iterable<Entity> getIndirectPassengers() {
        return () -> {
            return this.getIndirectPassengersStream().iterator();
        };
    }

    public boolean hasExactlyOnePlayerPassenger() {
        return this.getIndirectPassengersStream().filter((entity) -> {
            return entity instanceof Player;
        }).count() == 1L;
    }

    public Entity getRootVehicle() {
        Entity entity;
        for(entity = this; entity.isPassenger(); entity = entity.getVehicle()) {
        }

        return entity;
    }

    public boolean isPassengerOfSameVehicle(Entity entity) {
        return this.getRootVehicle() == entity.getRootVehicle();
    }

    public boolean hasIndirectPassenger(Entity passenger) {
        return this.getIndirectPassengersStream().anyMatch((entity) -> {
            return entity == passenger;
        });
    }

    public boolean isControlledByLocalInstance() {
        Entity entity = this.getControllingPassenger();
        if (entity instanceof Player) {
            return ((Player)entity).isLocalPlayer();
        } else {
            return !this.level.isClientSide;
        }
    }

    protected static Vec3 getCollisionHorizontalEscapeVector(double vehicleWidth, double passengerWidth, float passengerYaw) {
        double d = (vehicleWidth + passengerWidth + (double)1.0E-5F) / 2.0D;
        float f = -Mth.sin(passengerYaw * ((float)Math.PI / 180F));
        float g = Mth.cos(passengerYaw * ((float)Math.PI / 180F));
        float h = Math.max(Math.abs(f), Math.abs(g));
        return new Vec3((double)f * d / (double)h, 0.0D, (double)g * d / (double)h);
    }

    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    @Nullable
    public Entity getVehicle() {
        return this.vehicle;
    }

    public PushReaction getPistonPushReaction() {
        return PushReaction.NORMAL;
    }

    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    public int getFireImmuneTicks() {
        return 1;
    }

    public CommandSourceStack createCommandSourceStack() {
        return new CommandSourceStack(this, this.position(), this.getRotationVector(), this.level instanceof ServerLevel ? (ServerLevel)this.level : null, this.getPermissionLevel(), this.getName().getString(), this.getDisplayName(), this.level.getServer(), this);
    }

    protected int getPermissionLevel() {
        return 0;
    }

    public boolean hasPermissions(int permissionLevel) {
        return this.getPermissionLevel() >= permissionLevel;
    }

    @Override
    public boolean acceptsSuccess() {
        return this.level.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK);
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return true;
    }

    public void lookAt(EntityAnchorArgument.Anchor anchorPoint, Vec3 target) {
        Vec3 vec3 = anchorPoint.apply(this);
        double d = target.x - vec3.x;
        double e = target.y - vec3.y;
        double f = target.z - vec3.z;
        double g = Math.sqrt(d * d + f * f);
        this.setXRot(Mth.wrapDegrees((float)(-(Mth.atan2(e, g) * (double)(180F / (float)Math.PI)))));
        this.setYRot(Mth.wrapDegrees((float)(Mth.atan2(f, d) * (double)(180F / (float)Math.PI)) - 90.0F));
        this.setYHeadRot(this.getYRot());
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> tag, double speed) {
        if (this.touchingUnloadedChunk()) {
            return false;
        } else {
            AABB aABB = this.getBoundingBox().deflate(0.001D);
            int i = Mth.floor(aABB.minX);
            int j = Mth.ceil(aABB.maxX);
            int k = Mth.floor(aABB.minY);
            int l = Mth.ceil(aABB.maxY);
            int m = Mth.floor(aABB.minZ);
            int n = Mth.ceil(aABB.maxZ);
            double d = 0.0D;
            boolean bl = this.isPushedByFluid();
            boolean bl2 = false;
            Vec3 vec3 = Vec3.ZERO;
            int o = 0;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for(int p = i; p < j; ++p) {
                for(int q = k; q < l; ++q) {
                    for(int r = m; r < n; ++r) {
                        mutableBlockPos.set(p, q, r);
                        FluidState fluidState = this.level.getFluidState(mutableBlockPos);
                        if (fluidState.is(tag)) {
                            double e = (double)((float)q + fluidState.getHeight(this.level, mutableBlockPos));
                            if (e >= aABB.minY) {
                                bl2 = true;
                                d = Math.max(e - aABB.minY, d);
                                if (bl) {
                                    Vec3 vec32 = fluidState.getFlow(this.level, mutableBlockPos);
                                    if (d < 0.4D) {
                                        vec32 = vec32.scale(d);
                                    }

                                    vec3 = vec3.add(vec32);
                                    ++o;
                                }
                            }
                        }
                    }
                }
            }

            if (vec3.length() > 0.0D) {
                if (o > 0) {
                    vec3 = vec3.scale(1.0D / (double)o);
                }

                if (!(this instanceof Player)) {
                    vec3 = vec3.normalize();
                }

                Vec3 vec33 = this.getDeltaMovement();
                vec3 = vec3.scale(speed * 1.0D);
                double f = 0.003D;
                if (Math.abs(vec33.x) < 0.003D && Math.abs(vec33.z) < 0.003D && vec3.length() < 0.0045000000000000005D) {
                    vec3 = vec3.normalize().scale(0.0045000000000000005D);
                }

                this.setDeltaMovement(this.getDeltaMovement().add(vec3));
            }

            this.fluidHeight.put(tag, d);
            return bl2;
        }
    }

    public boolean touchingUnloadedChunk() {
        AABB aABB = this.getBoundingBox().inflate(1.0D);
        int i = Mth.floor(aABB.minX);
        int j = Mth.ceil(aABB.maxX);
        int k = Mth.floor(aABB.minZ);
        int l = Mth.ceil(aABB.maxZ);
        return !this.level.hasChunksAt(i, k, j, l);
    }

    public double getFluidHeight(TagKey<Fluid> fluid) {
        return this.fluidHeight.getDouble(fluid);
    }

    public double getFluidJumpThreshold() {
        return (double)this.getEyeHeight() < 0.4D ? 0.0D : 0.4D;
    }

    public final float getBbWidth() {
        return this.dimensions.width;
    }

    public final float getBbHeight() {
        return this.dimensions.height;
    }

    public abstract Packet<?> getAddEntityPacket();

    public EntityDimensions getDimensions(Pose pose) {
        return this.type.getDimensions();
    }

    public Vec3 position() {
        return this.position;
    }

    @Override
    public BlockPos blockPosition() {
        return this.blockPosition;
    }

    public BlockState getFeetBlockState() {
        if (this.feetBlockState == null) {
            this.feetBlockState = this.level.getBlockState(this.blockPosition());
        }

        return this.feetBlockState;
    }

    public BlockPos eyeBlockPosition() {
        return new BlockPos(this.getEyePosition(1.0F));
    }

    public ChunkPos chunkPosition() {
        return this.chunkPosition;
    }

    public Vec3 getDeltaMovement() {
        return this.deltaMovement;
    }

    public void setDeltaMovement(Vec3 velocity) {
        this.deltaMovement = velocity;
    }

    public void setDeltaMovement(double x, double y, double z) {
        this.setDeltaMovement(new Vec3(x, y, z));
    }

    public final int getBlockX() {
        return this.blockPosition.getX();
    }

    public final double getX() {
        return this.position.x;
    }

    public double getX(double widthScale) {
        return this.position.x + (double)this.getBbWidth() * widthScale;
    }

    public double getRandomX(double widthScale) {
        return this.getX((2.0D * this.random.nextDouble() - 1.0D) * widthScale);
    }

    public final int getBlockY() {
        return this.blockPosition.getY();
    }

    public final double getY() {
        return this.position.y;
    }

    public double getY(double heightScale) {
        return this.position.y + (double)this.getBbHeight() * heightScale;
    }

    public double getRandomY() {
        return this.getY(this.random.nextDouble());
    }

    public double getEyeY() {
        return this.position.y + (double)this.eyeHeight;
    }

    public final int getBlockZ() {
        return this.blockPosition.getZ();
    }

    public final double getZ() {
        return this.position.z;
    }

    public double getZ(double widthScale) {
        return this.position.z + (double)this.getBbWidth() * widthScale;
    }

    public double getRandomZ(double widthScale) {
        return this.getZ((2.0D * this.random.nextDouble() - 1.0D) * widthScale);
    }

    public final void setPosRaw(double x, double y, double z) {
        if (this.position.x != x || this.position.y != y || this.position.z != z) {
            this.position = new Vec3(x, y, z);
            int i = Mth.floor(x);
            int j = Mth.floor(y);
            int k = Mth.floor(z);
            if (i != this.blockPosition.getX() || j != this.blockPosition.getY() || k != this.blockPosition.getZ()) {
                this.blockPosition = new BlockPos(i, j, k);
                this.feetBlockState = null;
                if (SectionPos.blockToSectionCoord(i) != this.chunkPosition.x || SectionPos.blockToSectionCoord(k) != this.chunkPosition.z) {
                    this.chunkPosition = new ChunkPos(this.blockPosition);
                }
            }

            this.levelCallback.onMove();
            GameEventListenerRegistrar gameEventListenerRegistrar = this.getGameEventListenerRegistrar();
            if (gameEventListenerRegistrar != null) {
                gameEventListenerRegistrar.onListenerMove(this.level);
            }
        }

    }

    public void checkDespawn() {
    }

    public Vec3 getRopeHoldPosition(float delta) {
        return this.getPosition(delta).add(0.0D, (double)this.eyeHeight * 0.7D, 0.0D);
    }

    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        int i = packet.getId();
        double d = packet.getX();
        double e = packet.getY();
        double f = packet.getZ();
        this.setPacketCoordinates(d, e, f);
        this.moveTo(d, e, f);
        this.setXRot((float)(packet.getxRot() * 360) / 256.0F);
        this.setYRot((float)(packet.getyRot() * 360) / 256.0F);
        this.setId(i);
        this.setUUID(packet.getUUID());
    }

    @Nullable
    public ItemStack getPickResult() {
        return null;
    }

    public void setIsInPowderSnow(boolean inPowderSnow) {
        this.isInPowderSnow = inPowderSnow;
    }

    public boolean canFreeze() {
        return !this.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
    }

    public boolean isFreezing() {
        return (this.isInPowderSnow || this.wasInPowderSnow) && this.canFreeze();
    }

    public float getYRot() {
        return this.yRot;
    }

    public void setYRot(float yaw) {
        if (!Float.isFinite(yaw)) {
            Util.logAndPauseIfInIde("Invalid entity rotation: " + yaw + ", discarding.");
        } else {
            this.yRot = yaw;
        }
    }

    public float getXRot() {
        return this.xRot;
    }

    public void setXRot(float pitch) {
        if (!Float.isFinite(pitch)) {
            Util.logAndPauseIfInIde("Invalid entity rotation: " + pitch + ", discarding.");
        } else {
            this.xRot = pitch;
        }
    }

    public final boolean isRemoved() {
        return this.removalReason != null;
    }

    @Nullable
    public Entity.RemovalReason getRemovalReason() {
        return this.removalReason;
    }

    @Override
    public final void setRemoved(Entity.RemovalReason reason) {
        if (this.removalReason == null) {
            this.removalReason = reason;
        }

        if (this.removalReason.shouldDestroy()) {
            this.stopRiding();
        }

        this.getPassengers().forEach(Entity::stopRiding);
        this.levelCallback.onRemove(reason);
    }

    public void unsetRemoved() {
        this.removalReason = null;
    }

    @Override
    public void setLevelCallback(EntityInLevelCallback changeListener) {
        this.levelCallback = changeListener;
    }

    @Override
    public boolean shouldBeSaved() {
        if (this.removalReason != null && !this.removalReason.shouldSave()) {
            return false;
        } else if (this.isPassenger()) {
            return false;
        } else {
            return !this.isVehicle() || !this.hasExactlyOnePlayerPassenger();
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        return false;
    }

    public boolean mayInteract(Level world, BlockPos pos) {
        return true;
    }

    public Level getLevel() {
        return this.level;
    }

    @FunctionalInterface
    public interface MoveFunction {
        void accept(Entity entity, double x, double y, double z);
    }

    public static enum MovementEmission {
        NONE(false, false),
        SOUNDS(true, false),
        EVENTS(false, true),
        ALL(true, true);

        final boolean sounds;
        final boolean events;

        private MovementEmission(boolean sounds, boolean events) {
            this.sounds = sounds;
            this.events = events;
        }

        public boolean emitsAnything() {
            return this.events || this.sounds;
        }

        public boolean emitsEvents() {
            return this.events;
        }

        public boolean emitsSounds() {
            return this.sounds;
        }
    }

    public static enum RemovalReason {
        KILLED(true, false),
        DISCARDED(true, false),
        UNLOADED_TO_CHUNK(false, true),
        UNLOADED_WITH_PLAYER(false, false),
        CHANGED_DIMENSION(false, false);

        private final boolean destroy;
        private final boolean save;

        private RemovalReason(boolean destroy, boolean save) {
            this.destroy = destroy;
            this.save = save;
        }

        public boolean shouldDestroy() {
            return this.destroy;
        }

        public boolean shouldSave() {
            return this.save;
        }
    }
}
