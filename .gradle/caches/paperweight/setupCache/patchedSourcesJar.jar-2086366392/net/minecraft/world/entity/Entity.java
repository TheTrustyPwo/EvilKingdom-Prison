package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
import net.minecraft.server.MCUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
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
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
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
import net.minecraft.world.level.dimension.LevelStem;
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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftPortalEvent;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.entity.Pose;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.PluginManager;
// CraftBukkit end

public abstract class Entity implements Nameable, EntityAccess, CommandSource {

    // CraftBukkit start
    private static final int CURRENT_LEVEL = 2;
    public boolean preserveMotion = true; // Paper - keep initial motion on first setPositionRotation
    static boolean isLevelAtLeast(CompoundTag tag, int level) {
        return tag.contains("Bukkit.updateLevel") && tag.getInt("Bukkit.updateLevel") >= level;
    }

    // Paper start
    public static Random SHARED_RANDOM = new Random() {
        private boolean locked = false;
        @Override
        public synchronized void setSeed(long seed) {
            if (locked) {
                LOGGER.error("Ignoring setSeed on Entity.SHARED_RANDOM", new Throwable());
            } else {
                super.setSeed(seed);
                locked = true;
            }
        }
    };
    public org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason spawnReason;
    // Paper end

    public com.destroystokyo.paper.loottable.PaperLootableInventoryData lootableData; // Paper
    public boolean collisionLoadChunks = false; // Paper
    private CraftEntity bukkitEntity;

    public @org.jetbrains.annotations.Nullable net.minecraft.server.level.ChunkMap.TrackedEntity tracker; // Paper
    public @Nullable Throwable addedToWorldStack; // Paper - entity debug
    public CraftEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = CraftEntity.getEntity(this.level.getCraftServer(), this);
        }
        return this.bukkitEntity;
    }

    @Override
    public CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return this.getBukkitEntity();
    }

    // CraftBukkit - SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
    public int getDefaultMaxAirSupply() {
        return Entity.TOTAL_AIR_SUPPLY;
    }
    // CraftBukkit end

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
    private int id;
    public boolean blocksBuilding;
    public ImmutableList<Entity> passengers;
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
    private Vec3 deltaMovement;
    public float yRot; // Paper - private->public
    private float xRot;
    public float yRotO;
    public float xRotO;
    private AABB bb;
    public boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean verticalCollisionBelow;
    public boolean minorHorizontalCollision;
    public boolean hurtMarked;
    protected Vec3 stuckSpeedMultiplier;
    @Nullable
    private Entity.RemovalReason removalReason;
    public static final float DEFAULT_BB_WIDTH = 0.6F;
    public static final float DEFAULT_BB_HEIGHT = 1.8F;
    public float walkDistO;
    public float walkDist;
    public float moveDist;
    public float flyDist;
    public float fallDistance;
    private float nextStep;
    public double xOld;
    public double yOld;
    public double zOld;
    public float maxUpStep;
    public boolean noPhysics;
    protected final Random random;
    public int tickCount;
    public int remainingFireTicks;
    public boolean wasTouchingWater;
    protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;
    protected boolean wasEyeInWater;
    private final Set<TagKey<Fluid>> fluidOnEyes;
    public int invulnerableTime;
    protected boolean firstTick;
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
    protected static final EntityDataAccessor<net.minecraft.world.entity.Pose> DATA_POSE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.POSE);
    private static final EntityDataAccessor<Integer> DATA_TICKS_FROZEN = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
    private EntityInLevelCallback levelCallback;
    private Vec3 packetCoordinates;
    public boolean noCulling;
    public boolean hasImpulse;
    public int portalCooldown;
    public boolean isInsidePortal;
    protected int portalTime;
    protected BlockPos portalEntrancePos;
    private boolean invulnerable;
    protected UUID uuid;
    protected String stringUUID;
    private boolean hasGlowingTag;
    private final Set<String> tags;
    private final double[] pistonDeltas;
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
    private BlockState feetBlockState;
    // CraftBukkit start
    public boolean persist = true;
    public boolean valid;
    public boolean generation;
    public int maxAirTicks = this.getDefaultMaxAirSupply(); // CraftBukkit - SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
    public org.bukkit.projectiles.ProjectileSource projectileSource; // For projectiles only
    public boolean lastDamageCancelled; // SPIGOT-5339, SPIGOT-6252, SPIGOT-6777: Keep track if the event was canceled
    public boolean persistentInvisibility = false;
    public BlockPos lastLavaContact;
    // Spigot start
    public final org.spigotmc.ActivationRange.ActivationType activationType = org.spigotmc.ActivationRange.initializeEntityActivationType(this);
    public final boolean defaultActivationState;
    public long activatedTick = Integer.MIN_VALUE;
    public void inactiveTick() { }
    // Spigot end
    // Paper start
    public long activatedImmunityTick = Integer.MIN_VALUE; // Paper
    public boolean isTemporarilyActive = false; // Paper
    public boolean fromNetherPortal; // Paper
    protected int numCollisions = 0; // Paper
    public boolean spawnedViaMobSpawner; // Paper - Yes this name is similar to above, upstream took the better one
    @javax.annotation.Nullable
    private org.bukkit.util.Vector origin;
    @javax.annotation.Nullable
    private UUID originWorld;
    public boolean freezeLocked = false; // Paper - Freeze Tick Lock API

    public void setOrigin(@javax.annotation.Nonnull Location location) {
        this.origin = location.toVector();
        this.originWorld = location.getWorld().getUID();
    }

    @javax.annotation.Nullable
    public org.bukkit.util.Vector getOriginVector() {
        return this.origin != null ? this.origin.clone() : null;
    }

    @javax.annotation.Nullable
    public UUID getOriginWorld() {
        return this.originWorld;
    }
    // Paper end
    public float getBukkitYaw() {
        return this.yRot;
    }

    public boolean isChunkLoaded() {
        return this.level.hasChunk((int) Math.floor(this.getX()) >> 4, (int) Math.floor(this.getZ()) >> 4);
    }
    // CraftBukkit end
    // Paper start
    public final AABB getBoundingBoxAt(double x, double y, double z) {
        return this.dimensions.makeBoundingBox(x, y, z);
    }
    // Paper end

    // Paper start - optimise entity tracking
    final org.spigotmc.TrackingRange.TrackingRangeType trackingRangeType = org.spigotmc.TrackingRange.getTrackingRangeType(this);

    public boolean isLegacyTrackingEntity = false;

    public final void setLegacyTrackingEntity(final boolean isLegacyTrackingEntity) {
        this.isLegacyTrackingEntity = isLegacyTrackingEntity;
    }

    public final com.destroystokyo.paper.util.misc.PooledLinkedHashSets.PooledObjectLinkedOpenHashSet<ServerPlayer> getPlayersInTrackRange() {
        // determine highest range of passengers
        if (this.passengers.isEmpty()) {
            return ((ServerLevel)this.level).getChunkSource().chunkMap.playerEntityTrackerTrackMaps[this.trackingRangeType.ordinal()]
                .getObjectsInRange(MCUtil.getCoordinateKey(this));
        }
        Iterable<Entity> passengers = this.getIndirectPassengers();
        net.minecraft.server.level.ChunkMap chunkMap = ((ServerLevel)this.level).getChunkSource().chunkMap;
        org.spigotmc.TrackingRange.TrackingRangeType type = this.trackingRangeType;
        int range = chunkMap.getEntityTrackerRange(type.ordinal());

        for (Entity passenger : passengers) {
            org.spigotmc.TrackingRange.TrackingRangeType passengerType = passenger.trackingRangeType;
            int passengerRange = chunkMap.getEntityTrackerRange(passengerType.ordinal());
            if (passengerRange > range) {
                type = passengerType;
                range = passengerRange;
            }
        }

        return chunkMap.playerEntityTrackerTrackMaps[type.ordinal()].getObjectsInRange(MCUtil.getCoordinateKey(this));
    }
    // Paper end - optimise entity tracking
    // Paper start - make end portalling safe
    public BlockPos portalBlock;
    public ServerLevel portalWorld;
    public void tickEndPortal() {
        BlockPos pos = this.portalBlock;
        ServerLevel world = this.portalWorld;
        this.portalBlock = null;
        this.portalWorld = null;

        if (pos == null || world == null || world != this.level) {
            return;
        }

        if (this.isPassenger() || this.isVehicle() || !this.canChangeDimensions() || this.isRemoved() || !this.valid || !this.isAlive()) {
            return;
        }

        ResourceKey<Level> resourcekey = world.getTypeKey() == LevelStem.END ? Level.OVERWORLD : Level.END; // CraftBukkit - SPIGOT-6152: send back to main overworld in custom ends
        ServerLevel worldserver = world.getServer().getLevel(resourcekey);

        org.bukkit.event.entity.EntityPortalEnterEvent event = new org.bukkit.event.entity.EntityPortalEnterEvent(this.getBukkitEntity(), new org.bukkit.Location(world.getWorld(), pos.getX(), pos.getY(), pos.getZ()));
        event.callEvent();

        if (this instanceof ServerPlayer) {
            ((ServerPlayer)this).changeDimension(worldserver, PlayerTeleportEvent.TeleportCause.END_PORTAL);
            return;
        }
        this.teleportTo(worldserver, null);
    }
    // Paper end - make end portalling safe

    // Paper start
    /**
     * Overriding this field will cause memory leaks.
     */
    private final boolean hardCollides;

    private static final java.util.Map<Class<? extends Entity>, Boolean> cachedOverrides = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    {
        /* // Goodbye, broken on reobf...
        Boolean hardCollides = cachedOverrides.get(this.getClass());
        if (hardCollides == null) {
            try {
                java.lang.reflect.Method getHardCollisionBoxEntityMethod = Entity.class.getMethod("canCollideWith", Entity.class);
                java.lang.reflect.Method hasHardCollisionBoxMethod = Entity.class.getMethod("canBeCollidedWith");
                if (!this.getClass().getMethod(hasHardCollisionBoxMethod.getName(), hasHardCollisionBoxMethod.getParameterTypes()).equals(hasHardCollisionBoxMethod)
                        || !this.getClass().getMethod(getHardCollisionBoxEntityMethod.getName(), getHardCollisionBoxEntityMethod.getParameterTypes()).equals(getHardCollisionBoxEntityMethod)) {
                    hardCollides = Boolean.TRUE;
                } else {
                    hardCollides = Boolean.FALSE;
                }
                cachedOverrides.put(this.getClass(), hardCollides);
            }
            catch (ThreadDeath thr) { throw thr; }
            catch (Throwable thr) {
                // shouldn't happen, just explode
                throw new RuntimeException(thr);
            }
        } */
        this.hardCollides = this instanceof Boat
            || this instanceof net.minecraft.world.entity.monster.Shulker
            || this instanceof net.minecraft.world.entity.vehicle.AbstractMinecart
            || this.shouldHardCollide();
    }

    // plugins can override
    protected boolean shouldHardCollide() {
        return false;
    }

    public final boolean hardCollides() {
        return this.hardCollides;
    }

    public net.minecraft.server.level.ChunkHolder.FullChunkStatus chunkStatus;

    public int sectionX = Integer.MIN_VALUE;
    public int sectionY = Integer.MIN_VALUE;
    public int sectionZ = Integer.MIN_VALUE;
    // Paper end

    public Entity(EntityType<?> type, Level world) {
        this.id = Entity.ENTITY_COUNTER.incrementAndGet();
        this.passengers = ImmutableList.of();
        this.deltaMovement = Vec3.ZERO;
        this.bb = Entity.INITIAL_AABB;
        this.stuckSpeedMultiplier = Vec3.ZERO;
        this.nextStep = 1.0F;
        this.random = SHARED_RANDOM; // Paper
        this.remainingFireTicks = -this.getFireImmuneTicks();
        this.fluidHeight = new Object2DoubleArrayMap(2);
        this.fluidOnEyes = new HashSet();
        this.firstTick = true;
        this.levelCallback = EntityInLevelCallback.NULL;
        this.uuid = Mth.createInsecureUUID(this.random);
        this.stringUUID = this.uuid.toString();
        this.tags = Sets.newHashSet();
        this.pistonDeltas = new double[]{0.0D, 0.0D, 0.0D};
        this.feetBlockState = null;
        this.type = type;
        this.level = world;
        this.dimensions = type.getDimensions();
        this.position = Vec3.ZERO;
        this.blockPosition = BlockPos.ZERO;
        this.chunkPosition = ChunkPos.ZERO;
        this.packetCoordinates = Vec3.ZERO;
        // Spigot start
        if (world != null) {
            this.defaultActivationState = org.spigotmc.ActivationRange.initializeEntityActivationState(this, world.spigotConfig);
        } else {
            this.defaultActivationState = false;
        }
        // Spigot end
        this.entityData = new SynchedEntityData(this);
        this.entityData.define(Entity.DATA_SHARED_FLAGS_ID, (byte) 0);
        this.entityData.define(Entity.DATA_AIR_SUPPLY_ID, this.getMaxAirSupply());
        this.entityData.define(Entity.DATA_CUSTOM_NAME_VISIBLE, false);
        this.entityData.define(Entity.DATA_CUSTOM_NAME, Optional.empty());
        this.entityData.define(Entity.DATA_SILENT, false);
        this.entityData.define(Entity.DATA_NO_GRAVITY, false);
        this.entityData.define(Entity.DATA_POSE, net.minecraft.world.entity.Pose.STANDING);
        this.entityData.define(Entity.DATA_TICKS_FROZEN, 0);
        this.defineSynchedData();
        this.getEntityData().registrationLocked = true; // Spigot
        this.setPos(0.0D, 0.0D, 0.0D);
        this.eyeHeight = this.getEyeHeight(net.minecraft.world.entity.Pose.STANDING, this.dimensions);
    }

    public boolean isColliding(BlockPos pos, BlockState state) {
        VoxelShape voxelshape = state.getCollisionShape(this.level, pos, CollisionContext.of(this));
        VoxelShape voxelshape1 = voxelshape.move((double) pos.getX(), (double) pos.getY(), (double) pos.getZ());

        return Shapes.joinIsNotEmpty(voxelshape1, Shapes.create(this.getBoundingBox()), BooleanOp.AND);
    }

    public int getTeamColor() {
        Team scoreboardteambase = this.getTeam();

        return scoreboardteambase != null && scoreboardteambase.getColor().getColor() != null ? scoreboardteambase.getColor().getColor() : 16777215;
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

    public boolean equals(Object object) {
        return object instanceof Entity ? ((Entity) object).id == this.id : false;
    }

    public int hashCode() {
        return this.id;
    }

    public void remove(Entity.RemovalReason reason) {
        this.setRemoved(reason);
        if (reason == Entity.RemovalReason.KILLED) {
            this.gameEvent(GameEvent.ENTITY_KILLED);
        }

    }

    public void onClientRemoval() {}

    public void setPose(net.minecraft.world.entity.Pose pose) {
        // CraftBukkit start
        if (pose == this.getPose()) {
            return;
        }
        this.level.getCraftServer().getPluginManager().callEvent(new EntityPoseChangeEvent(this.getBukkitEntity(), Pose.values()[pose.ordinal()]));
        // CraftBukkit end
        this.entityData.set(Entity.DATA_POSE, pose);
    }

    public net.minecraft.world.entity.Pose getPose() {
        return (net.minecraft.world.entity.Pose) this.entityData.get(Entity.DATA_POSE);
    }

    public boolean closerThan(Entity other, double radius) {
        double d1 = other.position.x - this.position.x;
        double d2 = other.position.y - this.position.y;
        double d3 = other.position.z - this.position.z;

        return d1 * d1 + d2 * d2 + d3 * d3 < radius * radius;
    }

    public void setRot(float yaw, float pitch) {
        // CraftBukkit start - yaw was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(yaw)) {
            yaw = 0;
        }

        if (yaw == Float.POSITIVE_INFINITY || yaw == Float.NEGATIVE_INFINITY) {
            if (this instanceof ServerPlayer) {
                this.level.getCraftServer().getLogger().warning(this.getScoreboardName() + " was caught trying to crash the server with an invalid yaw");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite yaw (Hacking?)");
            }
            yaw = 0;
        }

        // pitch was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(pitch)) {
            pitch = 0;
        }

        if (pitch == Float.POSITIVE_INFINITY || pitch == Float.NEGATIVE_INFINITY) {
            if (this instanceof ServerPlayer) {
                this.level.getCraftServer().getLogger().warning(this.getScoreboardName() + " was caught trying to crash the server with an invalid pitch");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite pitch (Hacking?)");
            }
            pitch = 0;
        }
        // CraftBukkit end

        this.setYRot(yaw % 360.0F);
        this.setXRot(pitch % 360.0F);
    }

    public final void setPos(Vec3 pos) {
        this.setPos(pos.x(), pos.y(), pos.z());
    }

    public void setPos(double x, double y, double z) {
        this.setPosRaw(x, y, z, true); // Paper - force bounding box update
        // this.setBoundingBox(this.makeBoundingBox()); // Paper - move into setPositionRaw
    }

    protected AABB makeBoundingBox() {
        return this.dimensions.makeBoundingBox(this.position);
    }

    protected void reapplyPosition() {
        this.setPos(this.position.x, this.position.y, this.position.z);
    }

    public void turn(double cursorDeltaX, double cursorDeltaY) {
        float f = (float) cursorDeltaY * 0.15F;
        float f1 = (float) cursorDeltaX * 0.15F;

        this.setXRot(this.getXRot() + f);
        this.setYRot(this.getYRot() + f1);
        this.setXRot(Mth.clamp(this.getXRot(), -90.0F, 90.0F));
        this.xRotO += f;
        this.yRotO += f1;
        this.xRotO = Mth.clamp(this.xRotO, -90.0F, 90.0F);
        if (this.vehicle != null) {
            this.vehicle.onPassengerTurned(this);
        }

    }

    public void tick() {
        this.baseTick();
    }

    // CraftBukkit start
    public void postTick() {
        // No clean way to break out of ticking once the entity has been copied to a new world, so instead we move the portalling later in the tick cycle
        if (!(this instanceof ServerPlayer) && this.isAlive()) { // Paper - don't attempt to teleport dead entities
            this.handleNetherPortal();
        }
    }
    // CraftBukkit end

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
        if (this instanceof ServerPlayer) this.handleNetherPortal(); // CraftBukkit - // Moved up to postTick
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

            if (this.getTicksFrozen() > 0 && !freezeLocked) { // Paper - Freeze Tick Lock API
                this.setTicksFrozen(0);
                this.level.levelEvent((Player) null, 1009, this.blockPosition, 1);
            }
        }

        if (this.isInLava()) {
            this.lavaHurt();
            this.fallDistance *= 0.5F;
            // CraftBukkit start
        } else {
            this.lastLavaContact = null;
            // CraftBukkit end
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
        // Paper start - Configurable nether ceiling damage
        if (this.getY() < (double) (this.level.getMinBuildHeight() - 64) || (this.level.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER
            && level.paperConfig.doNetherTopVoidDamage()
            && this.getY() >= this.level.paperConfig.netherVoidTopDamageHeight)) {
            // Paper end
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
            // CraftBukkit start - Fallen in lava TODO: this event spams!
            if (this instanceof net.minecraft.world.entity.LivingEntity && this.remainingFireTicks <= 0) {
                // not on fire yet
                org.bukkit.block.Block damager = (this.lastLavaContact == null) ? null : org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock.at(level, lastLavaContact);
                org.bukkit.entity.Entity damagee = this.getBukkitEntity();
                EntityCombustEvent combustEvent = new org.bukkit.event.entity.EntityCombustByBlockEvent(damager, damagee, 15);
                this.level.getCraftServer().getPluginManager().callEvent(combustEvent);

                if (!combustEvent.isCancelled()) {
                    this.setSecondsOnFire(combustEvent.getDuration(), false);
                }
            } else {
                // This will be called every single tick the entity is in lava, so don't throw an event
                this.setSecondsOnFire(15, false);
            }
            CraftEventFactory.blockDamage = (this.lastLavaContact) == null ? null : org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock.at(level, lastLavaContact);
            if (this.hurt(DamageSource.LAVA, 4.0F)) {
                this.playSound(SoundEvents.GENERIC_BURN, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);
            }
            CraftEventFactory.blockDamage = null;
            // CraftBukkit end - we also don't throw an event unless the object in lava is living, to save on some event calls

        }
    }

    public void setSecondsOnFire(int seconds) {
        // CraftBukkit start
        this.setSecondsOnFire(seconds, true);
    }

    public void setSecondsOnFire(int i, boolean callEvent) {
        if (callEvent) {
            EntityCombustEvent event = new EntityCombustEvent(this.getBukkitEntity(), i);
            this.level.getCraftServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            i = event.getDuration();
        }
        // CraftBukkit end
        int j = i * 20;

        if (this instanceof net.minecraft.world.entity.LivingEntity) {
            j = ProtectionEnchantment.getFireAfterDampener((net.minecraft.world.entity.LivingEntity) this, j);
        }

        if (this.remainingFireTicks < j) {
            this.setRemainingFireTicks(j);
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

    // Paper start - detailed watchdog information
    public final Object posLock = new Object(); // Paper - log detailed entity tick information

    private Vec3 moveVector;
    private double moveStartX;
    private double moveStartY;
    private double moveStartZ;

    public final Vec3 getMoveVector() {
        return this.moveVector;
    }

    public final double getMoveStartX() {
        return this.moveStartX;
    }

    public final double getMoveStartY() {
        return this.moveStartY;
    }

    public final double getMoveStartZ() {
        return this.moveStartZ;
    }
    // Paper end - detailed watchdog information

    public void move(MoverType movementType, Vec3 movement) {
        // Paper start - detailed watchdog information
        io.papermc.paper.util.TickThread.ensureTickThread("Cannot move an entity off-main");
        synchronized (this.posLock) {
            this.moveStartX = this.getX();
            this.moveStartY = this.getY();
            this.moveStartZ = this.getZ();
            this.moveVector = movement;
        }
        try {
        // Paper end - detailed watchdog information
        if (this.noPhysics) {
            this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        } else {
            this.wasOnFire = this.isOnFire();
            if (movementType == MoverType.PISTON) {
                this.activatedTick = Math.max(this.activatedTick, MinecraftServer.currentTick + 20); // Paper
                this.activatedImmunityTick = Math.max(this.activatedImmunityTick, MinecraftServer.currentTick + 20);   // Paper
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
            // Paper start - ignore movement changes while inactive.
            if (isTemporarilyActive && !(this instanceof ItemEntity || this instanceof net.minecraft.world.entity.vehicle.AbstractMinecart) && movement == getDeltaMovement() && movementType == MoverType.SELF) {
                setDeltaMovement(Vec3.ZERO);
                this.level.getProfiler().pop();
                return;
            }
            // Paper end

            movement = this.maybeBackOffFromEdge(movement, movementType);
            Vec3 vec3d1 = this.collide(movement);
            double d0 = vec3d1.lengthSqr();

            if (d0 > 1.0E-7D) {
                if (this.fallDistance != 0.0F && d0 >= 1.0D) {
                    BlockHitResult movingobjectpositionblock = this.level.clip(new ClipContext(this.position(), this.position().add(vec3d1), ClipContext.Block.FALLDAMAGE_RESETTING, ClipContext.Fluid.WATER, this));

                    if (movingobjectpositionblock.getType() != HitResult.Type.MISS) {
                        this.resetFallDistance();
                    }
                }

                this.setPos(this.getX() + vec3d1.x, this.getY() + vec3d1.y, this.getZ() + vec3d1.z);
            }

            this.level.getProfiler().pop();
            this.level.getProfiler().push("rest");
            boolean flag = !Mth.equal(movement.x, vec3d1.x);
            boolean flag1 = !Mth.equal(movement.z, vec3d1.z);

            this.horizontalCollision = flag || flag1;
            this.verticalCollision = movement.y != vec3d1.y;
            this.verticalCollisionBelow = this.verticalCollision && movement.y < 0.0D;
            if (this.horizontalCollision) {
                this.minorHorizontalCollision = this.isHorizontalCollisionMinor(vec3d1);
            } else {
                this.minorHorizontalCollision = false;
            }

            this.onGround = this.verticalCollision && movement.y < 0.0D;
            BlockPos blockposition = this.getOnPos();
            BlockState iblockdata = this.level.getBlockState(blockposition);

            this.checkFallDamage(vec3d1.y, this.onGround, iblockdata, blockposition);
            if (this.isRemoved()) {
                this.level.getProfiler().pop();
            } else {
                if (this.horizontalCollision) {
                    Vec3 vec3d2 = this.getDeltaMovement();

                    this.setDeltaMovement(flag ? 0.0D : vec3d2.x, vec3d2.y, flag1 ? 0.0D : vec3d2.z);
                }

                Block block = iblockdata.getBlock();

                if (movement.y != vec3d1.y) {
                    block.updateEntityAfterFallOn(this.level, this);
                }

                // CraftBukkit start
                if (this.horizontalCollision && this.getBukkitEntity() instanceof Vehicle) {
                    Vehicle vehicle = (Vehicle) this.getBukkitEntity();
                    org.bukkit.block.Block bl = this.level.getWorld().getBlockAt(Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()));

                    if (movement.x > vec3d1.x) {
                        bl = bl.getRelative(BlockFace.EAST);
                    } else if (movement.x < vec3d1.x) {
                        bl = bl.getRelative(BlockFace.WEST);
                    } else if (movement.z > vec3d1.z) {
                        bl = bl.getRelative(BlockFace.SOUTH);
                    } else if (movement.z < vec3d1.z) {
                        bl = bl.getRelative(BlockFace.NORTH);
                    }

                    if (!bl.getType().isAir()) {
                        VehicleBlockCollisionEvent event = new VehicleBlockCollisionEvent(vehicle, bl);
                        this.level.getCraftServer().getPluginManager().callEvent(event);
                    }
                }
                // CraftBukkit end

                if (this.onGround && !this.isSteppingCarefully()) {
                    block.stepOn(this.level, blockposition, iblockdata, this);
                }

                Entity.MovementEmission entity_movementemission = this.getMovementEmission();

                if (entity_movementemission.emitsAnything() && !this.isPassenger()) {
                    double d1 = vec3d1.x;
                    double d2 = vec3d1.y;
                    double d3 = vec3d1.z;

                    this.flyDist += (float) (vec3d1.length() * 0.6D);
                    if (!iblockdata.is(BlockTags.CLIMBABLE) && !iblockdata.is(Blocks.POWDER_SNOW)) {
                        d2 = 0.0D;
                    }

                    this.walkDist += (float) vec3d1.horizontalDistance() * 0.6F;
                    this.moveDist += (float) Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3) * 0.6F;
                    if (this.moveDist > this.nextStep && !iblockdata.isAir()) {
                        this.nextStep = this.nextStep();
                        if (this.isInWater()) {
                            if (entity_movementemission.emitsSounds()) {
                                Entity entity = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;
                                float f = entity == this ? 0.35F : 0.4F;
                                Vec3 vec3d3 = entity.getDeltaMovement();
                                float f1 = Math.min(1.0F, (float) Math.sqrt(vec3d3.x * vec3d3.x * 0.20000000298023224D + vec3d3.y * vec3d3.y + vec3d3.z * vec3d3.z * 0.20000000298023224D) * f);

                                this.playSwimSound(f1);
                            }

                            if (entity_movementemission.emitsEvents()) {
                                this.gameEvent(GameEvent.SWIM);
                            }
                        } else {
                            if (entity_movementemission.emitsSounds()) {
                                this.playAmethystStepSound(iblockdata);
                                this.playStepSound(blockposition, iblockdata);
                            }

                            if (entity_movementemission.emitsEvents() && !iblockdata.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS)) {
                                this.gameEvent(GameEvent.STEP);
                            }
                        }
                    } else if (iblockdata.isAir()) {
                        this.processFlappingMovement();
                    }
                }

                this.tryCheckInsideBlocks();
                float f2 = this.getBlockSpeedFactor();

                this.setDeltaMovement(this.getDeltaMovement().multiply((double) f2, 1.0D, (double) f2));
                // Paper start - remove expensive streams from here
                boolean noneMatch = true;
                AABB fireSearchBox = this.getBoundingBox().deflate(1.0E-6D);
                {
                    int minX = Mth.floor(fireSearchBox.minX);
                    int minY = Mth.floor(fireSearchBox.minY);
                    int minZ = Mth.floor(fireSearchBox.minZ);
                    int maxX = Mth.floor(fireSearchBox.maxX);
                    int maxY = Mth.floor(fireSearchBox.maxY);
                    int maxZ = Mth.floor(fireSearchBox.maxZ);
                    fire_search_loop:
                    for (int fz = minZ; fz <= maxZ; ++fz) {
                        for (int fx = minX; fx <= maxX; ++fx) {
                            for (int fy = minY; fy <= maxY; ++fy) {
                                net.minecraft.world.level.chunk.LevelChunk chunk = (net.minecraft.world.level.chunk.LevelChunk)this.level.getChunkIfLoadedImmediately(fx >> 4, fz >> 4);
                                if (chunk == null) {
                                    // Vanilla rets an empty stream if all the chunks are not loaded, so noneMatch will be true
                                    // even if we're in lava/fire
                                    noneMatch = true;
                                    break fire_search_loop;
                                }
                                if (!noneMatch) {
                                    // don't do get type, we already know we're in fire - we just need to check the chunks
                                    // loaded state
                                    continue;
                                }

                                BlockState type = chunk.getBlockStateFinal(fx, fy, fz);
                                if (type.is(BlockTags.FIRE) || type.is(Blocks.LAVA)) {
                                    noneMatch = false;
                                    // can't break, we need to retain vanilla behavior by ensuring ALL chunks are loaded
                                }
                            }
                        }
                    }
                }
                if (noneMatch) {
                    // Paper end - remove expensive streams from here
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
        // Paper start - detailed watchdog information
        } finally {
            synchronized (this.posLock) { // Paper
                this.moveVector = null;
            } // Paper
        }
        // Paper end - detailed watchdog information
    }

    protected boolean isHorizontalCollisionMinor(Vec3 adjustedMovement) {
        return false;
    }

    protected void tryCheckInsideBlocks() {
        try {
            this.checkInsideBlocks();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Checking entity block collision");
            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Entity being checked for collision");

            this.fillCrashReportCategory(crashreportsystemdetails);
            throw new ReportedException(crashreport);
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
        int j = Mth.floor(this.position.y - 0.20000000298023224D);
        int k = Mth.floor(this.position.z);
        BlockPos blockposition = new BlockPos(i, j, k);

        if (this.level.getBlockState(blockposition).isAir()) {
            BlockPos blockposition1 = blockposition.below();
            BlockState iblockdata = this.level.getBlockState(blockposition1);

            if (iblockdata.is(BlockTags.FENCES) || iblockdata.is(BlockTags.WALLS) || iblockdata.getBlock() instanceof FenceGateBlock) {
                return blockposition1;
            }
        }

        return blockposition;
    }

    protected float getBlockJumpFactor() {
        float f = this.level.getBlockState(this.blockPosition()).getBlock().getJumpFactor();
        float f1 = this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();

        return (double) f == 1.0D ? f1 : f;
    }

    protected float getBlockSpeedFactor() {
        BlockState iblockdata = this.level.getBlockState(this.blockPosition());
        float f = iblockdata.getBlock().getSpeedFactor();

        return !iblockdata.is(Blocks.WATER) && !iblockdata.is(Blocks.BUBBLE_COLUMN) ? ((double) f == 1.0D ? this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getSpeedFactor() : f) : f;
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
            long i = this.level.getGameTime();

            if (i != this.pistonDeltasGameTime) {
                Arrays.fill(this.pistonDeltas, 0.0D);
                this.pistonDeltasGameTime = i;
            }

            double d0;

            if (movement.x != 0.0D) {
                d0 = this.applyPistonMovementRestriction(Direction.Axis.X, movement.x);
                return Math.abs(d0) <= 9.999999747378752E-6D ? Vec3.ZERO : new Vec3(d0, 0.0D, 0.0D);
            } else if (movement.y != 0.0D) {
                d0 = this.applyPistonMovementRestriction(Direction.Axis.Y, movement.y);
                return Math.abs(d0) <= 9.999999747378752E-6D ? Vec3.ZERO : new Vec3(0.0D, d0, 0.0D);
            } else if (movement.z != 0.0D) {
                d0 = this.applyPistonMovementRestriction(Direction.Axis.Z, movement.z);
                return Math.abs(d0) <= 9.999999747378752E-6D ? Vec3.ZERO : new Vec3(0.0D, 0.0D, d0);
            } else {
                return Vec3.ZERO;
            }
        }
    }

    private double applyPistonMovementRestriction(Direction.Axis axis, double offsetFactor) {
        int i = axis.ordinal();
        double d1 = Mth.clamp(offsetFactor + this.pistonDeltas[i], -0.51D, 0.51D);

        offsetFactor = d1 - this.pistonDeltas[i];
        this.pistonDeltas[i] = d1;
        return offsetFactor;
    }

    private Vec3 collide(Vec3 movement) {
        // Paper start - optimise collisions
        // This is a copy of vanilla's except that it uses strictly AABB math
        if (movement.x == 0.0 && movement.y == 0.0 && movement.z == 0.0) {
            return movement;
        }

        final Level world = this.level;
        final AABB currBoundingBox = this.getBoundingBox();

        if (io.papermc.paper.util.CollisionUtil.isEmpty(currBoundingBox)) {
            return movement;
        }

        final List<AABB> potentialCollisions = io.papermc.paper.util.CachedLists.getTempCollisionList();
        try {
            final double stepHeight = (double)this.maxUpStep;
            final AABB collisionBox;

            if (movement.x == 0.0 && movement.z == 0.0 && movement.y != 0.0) {
                if (movement.y > 0.0) {
                    collisionBox = io.papermc.paper.util.CollisionUtil.cutUpwards(currBoundingBox, movement.y);
                } else {
                    collisionBox = io.papermc.paper.util.CollisionUtil.cutDownwards(currBoundingBox, movement.y);
                }
            } else {
                if (stepHeight > 0.0 && (this.onGround || (movement.y < 0.0)) && (movement.x != 0.0 || movement.z != 0.0)) {
                    // don't bother getting the collisions if we don't need them.
                    if (movement.y <= 0.0) {
                        collisionBox = io.papermc.paper.util.CollisionUtil.expandUpwards(currBoundingBox.expandTowards(movement.x, movement.y, movement.z), stepHeight);
                    } else {
                        collisionBox = currBoundingBox.expandTowards(movement.x, Math.max(stepHeight, movement.y), movement.z);
                    }
                } else {
                    collisionBox = currBoundingBox.expandTowards(movement.x, movement.y, movement.z);
                }
            }

            io.papermc.paper.util.CollisionUtil.getCollisions(world, this, collisionBox, potentialCollisions, false, true,
                false, false, null, null);

            if (io.papermc.paper.util.CollisionUtil.isCollidingWithBorderEdge(world.getWorldBorder(), collisionBox)) {
                io.papermc.paper.util.CollisionUtil.addBoxesToIfIntersects(world.getWorldBorder().getCollisionShape(), collisionBox, potentialCollisions);
            }

            final Vec3 limitedMoveVector = io.papermc.paper.util.CollisionUtil.performCollisions(movement, currBoundingBox, potentialCollisions);

            if (stepHeight > 0.0
                && (this.onGround || (limitedMoveVector.y != movement.y && movement.y < 0.0))
                && (limitedMoveVector.x != movement.x || limitedMoveVector.z != movement.z)) {
                Vec3 vec3d2 = io.papermc.paper.util.CollisionUtil.performCollisions(new Vec3(movement.x, stepHeight, movement.z), currBoundingBox, potentialCollisions);
                final Vec3 vec3d3 = io.papermc.paper.util.CollisionUtil.performCollisions(new Vec3(0.0, stepHeight, 0.0), currBoundingBox.expandTowards(movement.x, 0.0, movement.z), potentialCollisions);

                if (vec3d3.y < stepHeight) {
                    final Vec3 vec3d4 = io.papermc.paper.util.CollisionUtil.performCollisions(new Vec3(movement.x, 0.0D, movement.z), currBoundingBox.move(vec3d3), potentialCollisions).add(vec3d3);

                    if (vec3d4.horizontalDistanceSqr() > vec3d2.horizontalDistanceSqr()) {
                        vec3d2 = vec3d4;
                    }
                }

                if (vec3d2.horizontalDistanceSqr() > limitedMoveVector.horizontalDistanceSqr()) {
                    return vec3d2.add(io.papermc.paper.util.CollisionUtil.performCollisions(new Vec3(0.0D, -vec3d2.y + movement.y, 0.0D), currBoundingBox.move(vec3d2), potentialCollisions));
                }

                return limitedMoveVector;
            } else {
                return limitedMoveVector;
            }
        } finally {
            io.papermc.paper.util.CachedLists.returnTempCollisionList(potentialCollisions);
        }
        // Paper end - optimise collisions
    }

    public static Vec3 collideBoundingBox(@Nullable Entity entity, Vec3 movement, AABB entityBoundingBox, Level world, List<VoxelShape> collisions) {
        Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 1);

        if (!collisions.isEmpty()) {
            builder.addAll(collisions);
        }

        WorldBorder worldborder = world.getWorldBorder();
        boolean flag = entity != null && worldborder.isInsideCloseToBorder(entity, entityBoundingBox.expandTowards(movement));

        if (flag) {
            builder.add(worldborder.getCollisionShape());
        }

        builder.addAll(world.getBlockCollisions(entity, entityBoundingBox.expandTowards(movement)));
        return Entity.collideWithShapes(movement, entityBoundingBox, builder.build());
    }

    private static Vec3 collideWithShapes(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions) {
        if (collisions.isEmpty()) {
            return movement;
        } else {
            double d0 = movement.x;
            double d1 = movement.y;
            double d2 = movement.z;

            if (d1 != 0.0D) {
                d1 = Shapes.collide(Direction.Axis.Y, entityBoundingBox, collisions, d1);
                if (d1 != 0.0D) {
                    entityBoundingBox = entityBoundingBox.move(0.0D, d1, 0.0D);
                }
            }

            boolean flag = Math.abs(d0) < Math.abs(d2);

            if (flag && d2 != 0.0D) {
                d2 = Shapes.collide(Direction.Axis.Z, entityBoundingBox, collisions, d2);
                if (d2 != 0.0D) {
                    entityBoundingBox = entityBoundingBox.move(0.0D, 0.0D, d2);
                }
            }

            if (d0 != 0.0D) {
                d0 = Shapes.collide(Direction.Axis.X, entityBoundingBox, collisions, d0);
                if (!flag && d0 != 0.0D) {
                    entityBoundingBox = entityBoundingBox.move(d0, 0.0D, 0.0D);
                }
            }

            if (!flag && d2 != 0.0D) {
                d2 = Shapes.collide(Direction.Axis.Z, entityBoundingBox, collisions, d2);
            }

            return new Vec3(d0, d1, d2);
        }
    }

    protected float nextStep() {
        return (float) ((int) this.moveDist + 1);
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
        AABB axisalignedbb = this.getBoundingBox();
        BlockPos blockposition = new BlockPos(axisalignedbb.minX + 0.001D, axisalignedbb.minY + 0.001D, axisalignedbb.minZ + 0.001D);
        BlockPos blockposition1 = new BlockPos(axisalignedbb.maxX - 0.001D, axisalignedbb.maxY - 0.001D, axisalignedbb.maxZ - 0.001D);

        if (this.level.hasChunksAt(blockposition, blockposition1)) {
            BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();

            for (int i = blockposition.getX(); i <= blockposition1.getX(); ++i) {
                for (int j = blockposition.getY(); j <= blockposition1.getY(); ++j) {
                    for (int k = blockposition.getZ(); k <= blockposition1.getZ(); ++k) {
                        blockposition_mutableblockposition.set(i, j, k);
                        BlockState iblockdata = this.level.getBlockState(blockposition_mutableblockposition);

                        try {
                            iblockdata.entityInside(this.level, blockposition_mutableblockposition, this);
                            this.onInsideBlock(iblockdata);
                        } catch (Throwable throwable) {
                            CrashReport crashreport = CrashReport.forThrowable(throwable, "Colliding entity with block");
                            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Block being collided with");

                            CrashReportCategory.populateBlockDetails(crashreportsystemdetails, this.level, blockposition_mutableblockposition, iblockdata);
                            throw new ReportedException(crashreport);
                        }
                    }
                }
            }
        }

    }

    protected void onInsideBlock(BlockState state) {}

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
            BlockState iblockdata1 = this.level.getBlockState(pos.above());
            SoundType soundeffecttype = iblockdata1.is(BlockTags.INSIDE_STEP_SOUND_BLOCKS) ? iblockdata1.getSoundType() : state.getSoundType();

            this.playSound(soundeffecttype.getStepSound(), soundeffecttype.getVolume() * 0.15F, soundeffecttype.getPitch());
        }
    }

    private void playAmethystStepSound(BlockState state) {
        if (state.is(BlockTags.CRYSTAL_SOUND_BLOCKS) && this.tickCount >= this.lastCrystalSoundPlayTick + 20) {
            this.crystalSoundIntensity *= (float) Math.pow(0.997D, (double) (this.tickCount - this.lastCrystalSoundPlayTick));
            this.crystalSoundIntensity = Math.min(1.0F, this.crystalSoundIntensity + 0.07F);
            float f = 0.5F + this.crystalSoundIntensity * this.random.nextFloat() * 1.2F;
            float f1 = 0.1F + this.crystalSoundIntensity * 1.2F;

            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, f1, f);
            this.lastCrystalSoundPlayTick = this.tickCount;
        }

    }

    protected void playSwimSound(float volume) {
        this.playSound(this.getSwimSound(), volume, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    }

    protected void onFlap() {}

    protected boolean isFlapping() {
        return false;
    }

    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!this.isSilent()) {
            this.level.playSound((Player) null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch);
        }

    }

    public boolean isSilent() {
        return (Boolean) this.entityData.get(Entity.DATA_SILENT);
    }

    public void setSilent(boolean silent) {
        this.entityData.set(Entity.DATA_SILENT, silent);
    }

    public boolean isNoGravity() {
        return (Boolean) this.entityData.get(Entity.DATA_NO_GRAVITY);
    }

    public void setNoGravity(boolean noGravity) {
        this.entityData.set(Entity.DATA_NO_GRAVITY, noGravity);
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
            this.fallDistance -= (float) heightDifference;
        }

    }

    public boolean fireImmune() {
        return this.getType().fireImmune();
    }

    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (this.isVehicle()) {
            Iterator iterator = this.getPassengers().iterator();

            while (iterator.hasNext()) {
                Entity entity = (Entity) iterator.next();

                entity.causeFallDamage(fallDistance, damageMultiplier, damageSource);
            }
        }

        return false;
    }

    public boolean isInWater() {
        return this.wasTouchingWater;
    }

    public boolean isInRain() {
        BlockPos blockposition = this.blockPosition();

        return this.level.isRainingAt(blockposition) || this.level.isRainingAt(new BlockPos((double) blockposition.getX(), this.getBoundingBox().maxY, (double) blockposition.getZ()));
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
        double d0 = this.level.dimensionType().ultraWarm() ? 0.007D : 0.0023333333333333335D;
        boolean flag = this.updateFluidHeightAndDoFluidPushing(FluidTags.LAVA, d0);

        return this.isInWater() || flag;
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
        double d0 = this.getEyeY() - 0.1111111119389534D;
        Entity entity = this.getVehicle();

        if (entity instanceof Boat) {
            Boat entityboat = (Boat) entity;

            if (!entityboat.isUnderWater() && entityboat.getBoundingBox().maxY >= d0 && entityboat.getBoundingBox().minY <= d0) {
                return;
            }
        }

        BlockPos blockposition = new BlockPos(this.getX(), d0, this.getZ());
        FluidState fluid = this.level.getFluidState(blockposition);
        double d1 = (double) ((float) blockposition.getY() + fluid.getHeight(this.level, blockposition));

        if (d1 > d0) {
            Stream stream = fluid.getTags();
            Set set = this.fluidOnEyes;

            Objects.requireNonNull(this.fluidOnEyes);
            stream.forEach(set::add);
        }

    }

    protected void doWaterSplashEffect() {
        Entity entity = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;
        float f = entity == this ? 0.2F : 0.9F;
        Vec3 vec3d = entity.getDeltaMovement();
        float f1 = Math.min(1.0F, (float) Math.sqrt(vec3d.x * vec3d.x * 0.20000000298023224D + vec3d.y * vec3d.y + vec3d.z * vec3d.z * 0.20000000298023224D) * f);

        if (f1 < 0.25F) {
            this.playSound(this.getSwimSplashSound(), f1, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        } else {
            this.playSound(this.getSwimHighSpeedSplashSound(), f1, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        }

        float f2 = (float) Mth.floor(this.getY());

        double d0;
        double d1;
        int i;

        for (i = 0; (float) i < 1.0F + this.dimensions.width * 20.0F; ++i) {
            d0 = (this.random.nextDouble() * 2.0D - 1.0D) * (double) this.dimensions.width;
            d1 = (this.random.nextDouble() * 2.0D - 1.0D) * (double) this.dimensions.width;
            this.level.addParticle(ParticleTypes.BUBBLE, this.getX() + d0, (double) (f2 + 1.0F), this.getZ() + d1, vec3d.x, vec3d.y - this.random.nextDouble() * 0.20000000298023224D, vec3d.z);
        }

        for (i = 0; (float) i < 1.0F + this.dimensions.width * 20.0F; ++i) {
            d0 = (this.random.nextDouble() * 2.0D - 1.0D) * (double) this.dimensions.width;
            d1 = (this.random.nextDouble() * 2.0D - 1.0D) * (double) this.dimensions.width;
            this.level.addParticle(ParticleTypes.SPLASH, this.getX() + d0, (double) (f2 + 1.0F), this.getZ() + d1, vec3d.x, vec3d.y, vec3d.z);
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
        int j = Mth.floor(this.getY() - 0.20000000298023224D);
        int k = Mth.floor(this.getZ());
        BlockPos blockposition = new BlockPos(i, j, k);
        BlockState iblockdata = this.level.getBlockState(blockposition);

        if (iblockdata.getRenderShape() != RenderShape.INVISIBLE) {
            Vec3 vec3d = this.getDeltaMovement();

            this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, iblockdata), this.getX() + (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width, this.getY() + 0.1D, this.getZ() + (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width, vec3d.x * -4.0D, 1.5D, vec3d.z * -4.0D);
        }

    }

    public boolean isEyeInFluid(TagKey<Fluid> fluidTag) {
        return this.fluidOnEyes.contains(fluidTag);
    }

    public boolean isInLava() {
        return !this.firstTick && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0D;
    }

    public void moveRelative(float speed, Vec3 movementInput) {
        Vec3 vec3d1 = Entity.getInputVector(movementInput, speed, this.getYRot());

        this.setDeltaMovement(this.getDeltaMovement().add(vec3d1));
    }

    private static Vec3 getInputVector(Vec3 movementInput, float speed, float yaw) {
        double d0 = movementInput.lengthSqr();

        if (d0 < 1.0E-7D) {
            return Vec3.ZERO;
        } else {
            Vec3 vec3d1 = (d0 > 1.0D ? movementInput.normalize() : movementInput).scale((double) speed);
            float f2 = Mth.sin(yaw * 0.017453292F);
            float f3 = Mth.cos(yaw * 0.017453292F);

            return new Vec3(vec3d1.x * (double) f3 - vec3d1.z * (double) f2, vec3d1.y, vec3d1.z * (double) f3 + vec3d1.x * (double) f2);
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
        this.setYHeadRot(yaw); // Paper - Update head rotation
    }

    public void absMoveTo(double x, double y, double z) {
        double d3 = Mth.clamp(x, -3.0E7D, 3.0E7D);
        double d4 = Mth.clamp(z, -3.0E7D, 3.0E7D);

        this.xo = d3;
        this.yo = y;
        this.zo = d4;
        this.setPos(d3, y, d4);
        if (this.valid) this.level.getChunk((int) Math.floor(this.getX()) >> 4, (int) Math.floor(this.getZ()) >> 4); // CraftBukkit
    }

    public void moveTo(Vec3 pos) {
        this.moveTo(pos.x, pos.y, pos.z);
    }

    public void moveTo(double x, double y, double z) {
        this.moveTo(x, y, z, this.getYRot(), this.getXRot());
    }

    public void moveTo(BlockPos pos, float yaw, float pitch) {
        this.moveTo((double) pos.getX() + 0.5D, (double) pos.getY(), (double) pos.getZ() + 0.5D, yaw, pitch);
    }

    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        // Paper - cancel entity velocity if teleported
        if (!preserveMotion) {
            this.deltaMovement = Vec3.ZERO;
        } else {
            this.preserveMotion = false;
        }
        // Paper end
        this.setPosRaw(x, y, z);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setOldPosAndRot();
        this.reapplyPosition();
        this.setYHeadRot(yaw); // Paper - Update head rotation
    }

    public final void setOldPosAndRot() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();

        this.xo = d0;
        this.yo = d1;
        this.zo = d2;
        this.xOld = d0;
        this.yOld = d1;
        this.zOld = d2;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public float distanceTo(Entity entity) {
        float f = (float) (this.getX() - entity.getX());
        float f1 = (float) (this.getY() - entity.getY());
        float f2 = (float) (this.getZ() - entity.getZ());

        return Mth.sqrt(f * f + f1 * f1 + f2 * f2);
    }

    public double distanceToSqr(double x, double y, double z) {
        double d3 = this.getX() - x;
        double d4 = this.getY() - y;
        double d5 = this.getZ() - z;

        return d3 * d3 + d4 * d4 + d5 * d5;
    }

    public double distanceToSqr(Entity entity) {
        return this.distanceToSqr(entity.position());
    }

    public double distanceToSqr(Vec3 vector) {
        double d0 = this.getX() - vector.x;
        double d1 = this.getY() - vector.y;
        double d2 = this.getZ() - vector.z;

        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public void playerTouch(Player player) {}

    public void push(Entity entity) {
        if (!this.isPassengerOfSameVehicle(entity)) {
            if (!entity.noPhysics && !this.noPhysics) {
                if (this.level.paperConfig.onlyPlayersCollide && !(entity instanceof ServerPlayer || this instanceof ServerPlayer)) return; // Paper
                double d0 = entity.getX() - this.getX();
                double d1 = entity.getZ() - this.getZ();
                double d2 = Mth.absMax(d0, d1);

                if (d2 >= 0.009999999776482582D) {
                    d2 = Math.sqrt(d2);
                    d0 /= d2;
                    d1 /= d2;
                    double d3 = 1.0D / d2;

                    if (d3 > 1.0D) {
                        d3 = 1.0D;
                    }

                    d0 *= d3;
                    d1 *= d3;
                    d0 *= 0.05000000074505806D;
                    d1 *= 0.05000000074505806D;
                    if (!this.isVehicle()) {
                        this.push(-d0, 0.0D, -d1);
                    }

                    if (!entity.isVehicle()) {
                        entity.push(d0, 0.0D, d1);
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
        float f2 = pitch * 0.017453292F;
        float f3 = -yaw * 0.017453292F;
        float f4 = Mth.cos(f3);
        float f5 = Mth.sin(f3);
        float f6 = Mth.cos(f2);
        float f7 = Mth.sin(f2);

        return new Vec3((double) (f5 * f6), (double) (-f7), (double) (f4 * f6));
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
        double d0 = Mth.lerp((double) tickDelta, this.xo, this.getX());
        double d1 = Mth.lerp((double) tickDelta, this.yo, this.getY()) + (double) this.getEyeHeight();
        double d2 = Mth.lerp((double) tickDelta, this.zo, this.getZ());

        return new Vec3(d0, d1, d2);
    }

    public Vec3 getLightProbePosition(float tickDelta) {
        return this.getEyePosition(tickDelta);
    }

    public final Vec3 getPosition(float delta) {
        double d0 = Mth.lerp((double) delta, this.xo, this.getX());
        double d1 = Mth.lerp((double) delta, this.yo, this.getY());
        double d2 = Mth.lerp((double) delta, this.zo, this.getZ());

        return new Vec3(d0, d1, d2);
    }

    public HitResult pick(double maxDistance, float tickDelta, boolean includeFluids) {
        Vec3 vec3d = this.getEyePosition(tickDelta);
        Vec3 vec3d1 = this.getViewVector(tickDelta);
        Vec3 vec3d2 = vec3d.add(vec3d1.x * maxDistance, vec3d1.y * maxDistance, vec3d1.z * maxDistance);

        return this.level.clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.OUTLINE, includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
    }

    public boolean isPickable() {
        return false;
    }

    public boolean isPushable() {
        // Paper start
        return isCollidable(false);
    }

    public boolean isCollidable(boolean ignoreClimbing) {
        // Paper end
        return false;
    }

    // CraftBukkit start - collidable API
    public boolean canCollideWithBukkit(Entity entity) {
        return this.isPushable();
    }
    // CraftBukkit end

    public void awardKillScore(Entity entityKilled, int score, DamageSource damageSource) {
        if (entityKilled instanceof ServerPlayer) {
            CriteriaTriggers.ENTITY_KILLED_PLAYER.trigger((ServerPlayer) entityKilled, this, damageSource);
        }

    }

    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        double d3 = this.getX() - cameraX;
        double d4 = this.getY() - cameraY;
        double d5 = this.getZ() - cameraZ;
        double d6 = d3 * d3 + d4 * d4 + d5 * d5;

        return this.shouldRenderAtSqrDistance(d6);
    }

    public boolean shouldRenderAtSqrDistance(double distance) {
        double d1 = this.getBoundingBox().getSize();

        if (Double.isNaN(d1)) {
            d1 = 1.0D;
        }

        d1 *= 64.0D * Entity.viewScale;
        return distance < d1 * d1;
    }

    public boolean saveAsPassenger(CompoundTag nbt) {
        if (this.removalReason != null && !this.removalReason.shouldSave()) {
            return false;
        } else {
            String s = this.getEncodeId();

            if (!this.persist || s == null) { // CraftBukkit - persist flag
                return false;
            } else {
                nbt.putString("id", s);
                this.saveWithoutId(nbt);
                return true;
            }
        }
    }

    // Paper start - Entity serialization api
    public boolean serializeEntity(CompoundTag compound) {
        List<Entity> pass = new java.util.ArrayList<>(this.getPassengers());
        this.passengers = ImmutableList.of();
        boolean result = save(compound);
        this.passengers = ImmutableList.copyOf(pass);
        return result;
    }
    // Paper end
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

            Vec3 vec3d = this.getDeltaMovement();

            nbt.put("Motion", this.newDoubleList(vec3d.x, vec3d.y, vec3d.z));

            // CraftBukkit start - Checking for NaN pitch/yaw and resetting to zero
            // TODO: make sure this is the best way to address this.
            if (Float.isNaN(this.yRot)) {
                this.yRot = 0;
            }

            if (Float.isNaN(this.xRot)) {
                this.xRot = 0;
            }
            // CraftBukkit end

            nbt.put("Rotation", this.newFloatList(this.getYRot(), this.getXRot()));
            nbt.putFloat("FallDistance", this.fallDistance);
            nbt.putShort("Fire", (short) this.remainingFireTicks);
            nbt.putShort("Air", (short) this.getAirSupply());
            nbt.putBoolean("OnGround", this.onGround);
            nbt.putBoolean("Invulnerable", this.invulnerable);
            nbt.putInt("PortalCooldown", this.portalCooldown);
            nbt.putUUID("UUID", this.getUUID());
            // CraftBukkit start
            // PAIL: Check above UUID reads 1.8 properly, ie: UUIDMost / UUIDLeast
            nbt.putLong("WorldUUIDLeast", ((ServerLevel) this.level).getWorld().getUID().getLeastSignificantBits());
            nbt.putLong("WorldUUIDMost", ((ServerLevel) this.level).getWorld().getUID().getMostSignificantBits());
            nbt.putInt("Bukkit.updateLevel", CURRENT_LEVEL);
            if (!this.persist) {
                nbt.putBoolean("Bukkit.persist", this.persist);
            }
            if (this.persistentInvisibility) {
                nbt.putBoolean("Bukkit.invisible", this.persistentInvisibility);
            }
            // SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
            if (this.maxAirTicks != this.getDefaultMaxAirSupply()) {
                nbt.putInt("Bukkit.MaxAirSupply", this.getMaxAirSupply());
            }
            nbt.putInt("Spigot.ticksLived", this.tickCount);
            // CraftBukkit end
            Component ichatbasecomponent = this.getCustomName();

            if (ichatbasecomponent != null) {
                nbt.putString("CustomName", Component.Serializer.toJson(ichatbasecomponent));
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

            ListTag nbttaglist;
            Iterator iterator;

            if (!this.tags.isEmpty()) {
                nbttaglist = new ListTag();
                iterator = this.tags.iterator();

                while (iterator.hasNext()) {
                    String s = (String) iterator.next();

                    nbttaglist.add(StringTag.valueOf(s));
                }

                nbt.put("Tags", nbttaglist);
            }

            this.addAdditionalSaveData(nbt);
            if (this.isVehicle()) {
                nbttaglist = new ListTag();
                iterator = this.getPassengers().iterator();

                while (iterator.hasNext()) {
                    Entity entity = (Entity) iterator.next();
                    CompoundTag nbttagcompound1 = new CompoundTag();

                    if (entity.saveAsPassenger(nbttagcompound1)) {
                        nbttaglist.add(nbttagcompound1);
                    }
                }

                if (!nbttaglist.isEmpty()) {
                    nbt.put("Passengers", nbttaglist);
                }
            }

            // CraftBukkit start - stores eventually existing bukkit values
            if (this.bukkitEntity != null) {
                this.bukkitEntity.storeBukkitValues(nbt);
            }
            // CraftBukkit end
            // Paper start - Save the entity's origin location
            if (this.origin != null) {
                UUID originWorld = this.originWorld != null ? this.originWorld : this.level != null ? this.level.getWorld().getUID() : null;
                if (originWorld != null) {
                    nbt.putUUID("Paper.OriginWorld", originWorld);
                }
                nbt.put("Paper.Origin", this.newDoubleList(origin.getX(), origin.getY(), origin.getZ()));
            }
            if (spawnReason != null) {
                nbt.putString("Paper.SpawnReason", spawnReason.name());
            }
            // Save entity's from mob spawner status
            if (spawnedViaMobSpawner) {
                nbt.putBoolean("Paper.FromMobSpawner", true);
            }
            if (fromNetherPortal) {
                nbt.putBoolean("Paper.FromNetherPortal", true);
            }
            if (freezeLocked) {
                nbt.putBoolean("Paper.FreezeLock", true);
            }
            // Paper end
            return nbt;
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Saving entity NBT");
            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Entity being saved");

            this.fillCrashReportCategory(crashreportsystemdetails);
            throw new ReportedException(crashreport);
        }
    }

    public void load(CompoundTag nbt) {
        try {
            ListTag nbttaglist = nbt.getList("Pos", 6);
            ListTag nbttaglist1 = nbt.getList("Motion", 6);
            ListTag nbttaglist2 = nbt.getList("Rotation", 5);
            double d0 = nbttaglist1.getDouble(0);
            double d1 = nbttaglist1.getDouble(1);
            double d2 = nbttaglist1.getDouble(2);

            this.setDeltaMovement(Math.abs(d0) > 10.0D ? 0.0D : d0, Math.abs(d1) > 10.0D ? 0.0D : d1, Math.abs(d2) > 10.0D ? 0.0D : d2);
            this.setPosRaw(nbttaglist.getDouble(0), Mth.clamp(nbttaglist.getDouble(1), -2.0E7D, 2.0E7D), nbttaglist.getDouble(2));
            this.setYRot(nbttaglist2.getFloat(0));
            this.setXRot(nbttaglist2.getFloat(1));
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
                if (Double.isFinite((double) this.getYRot()) && Double.isFinite((double) this.getXRot())) {
                    this.reapplyPosition();
                    this.setRot(this.getYRot(), this.getXRot());
                    if (nbt.contains("CustomName", 8)) {
                        String s = nbt.getString("CustomName");

                        try {
                            this.setCustomName(Component.Serializer.fromJson(s));
                        } catch (Exception exception) {
                            Entity.LOGGER.warn("Failed to parse entity custom name {}", s, exception);
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
                        ListTag nbttaglist3 = nbt.getList("Tags", 8);
                        int i = Math.min(nbttaglist3.size(), 1024);

                        for (int j = 0; j < i; ++j) {
                            this.tags.add(nbttaglist3.getString(j));
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

            // CraftBukkit start
            if (this instanceof net.minecraft.world.entity.LivingEntity) {
                net.minecraft.world.entity.LivingEntity entity = (net.minecraft.world.entity.LivingEntity) this;

                this.tickCount = nbt.getInt("Spigot.ticksLived");

                // Reset the persistence for tamed animals
                if (entity instanceof TamableAnimal && !Entity.isLevelAtLeast(nbt, 2) && !nbt.getBoolean("PersistenceRequired")) {
                    Mob entityinsentient = (Mob) entity;
                    entityinsentient.setPersistenceRequired(!entityinsentient.removeWhenFarAway(0));
                }
            }
            this.persist = !nbt.contains("Bukkit.persist") || nbt.getBoolean("Bukkit.persist");
            // SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
            if (nbt.contains("Bukkit.MaxAirSupply")) {
                this.maxAirTicks = nbt.getInt("Bukkit.MaxAirSupply");
            }
            // CraftBukkit end

            // CraftBukkit start - Reset world
            if (this instanceof ServerPlayer) {
                Server server = Bukkit.getServer();
                org.bukkit.World bworld = null;

                // TODO: Remove World related checks, replaced with WorldUID
                String worldName = nbt.getString("world");

                if (nbt.contains("WorldUUIDMost") && nbt.contains("WorldUUIDLeast")) {
                    UUID uid = new UUID(nbt.getLong("WorldUUIDMost"), nbt.getLong("WorldUUIDLeast"));
                    bworld = server.getWorld(uid);
                } else {
                    bworld = server.getWorld(worldName);
                }

                // Paper start - Move player to spawn point if spawn in unloaded world
//                if (bworld == null) {
//                    bworld = ((org.bukkit.craftbukkit.v1_18_R2.CraftServer) server).getServer().getWorldServer(World.OVERWORLD).getWorld();
//                }
                // Paper end - Move player to spawn point if spawn in unloaded world

                ((ServerPlayer) this).setLevel(bworld == null ? null : ((CraftWorld) bworld).getHandle());
            }
            this.getBukkitEntity().readBukkitValues(nbt);
            if (nbt.contains("Bukkit.invisible")) {
                boolean bukkitInvisible = nbt.getBoolean("Bukkit.invisible");
                this.setInvisible(bukkitInvisible);
                this.persistentInvisibility = bukkitInvisible;
            }
            // CraftBukkit end

            // Paper start - Restore the entity's origin location
            ListTag originTag = nbt.getList("Paper.Origin", 6);
            if (!originTag.isEmpty()) {
                UUID originWorld = null;
                if (nbt.contains("Paper.OriginWorld")) {
                    originWorld = nbt.getUUID("Paper.OriginWorld");
                } else if (this.level != null) {
                    originWorld = this.level.getWorld().getUID();
                }
                this.originWorld = originWorld;
                origin = new org.bukkit.util.Vector(originTag.getDouble(0), originTag.getDouble(1), originTag.getDouble(2));
            }

            spawnedViaMobSpawner = nbt.getBoolean("Paper.FromMobSpawner"); // Restore entity's from mob spawner status
            fromNetherPortal = nbt.getBoolean("Paper.FromNetherPortal");
            if (nbt.contains("Paper.SpawnReason")) {
                String spawnReasonName = nbt.getString("Paper.SpawnReason");
                try {
                    spawnReason = org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.valueOf(spawnReasonName);
                } catch (Exception ignored) {
                    LOGGER.error("Unknown SpawnReason " + spawnReasonName + " for " + this);
                }
            }
            if (spawnReason == null) {
                if (spawnedViaMobSpawner) {
                    spawnReason = org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER;
                } else if (this instanceof Mob && (this instanceof Animal || this instanceof AbstractFish) && !((Mob) this).removeWhenFarAway(0.0)) {
                    if (!nbt.getBoolean("PersistenceRequired")) {
                        spawnReason = org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL;
                    }
                }
            }
            if (spawnReason == null) {
                spawnReason = org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DEFAULT;
            }
            if (nbt.contains("Paper.FreezeLock")) {
                freezeLocked = nbt.getBoolean("Paper.FreezeLock");
            }
            // Paper end

        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Loading entity NBT");
            CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Entity being loaded");

            this.fillCrashReportCategory(crashreportsystemdetails);
            throw new ReportedException(crashreport);
        }
    }

    protected boolean repositionEntityAfterLoad() {
        return true;
    }

    @Nullable
    public final String getEncodeId() {
        EntityType<?> entitytypes = this.getType();
        ResourceLocation minecraftkey = EntityType.getKey(entitytypes);

        return entitytypes.canSerialize() && minecraftkey != null ? minecraftkey.toString() : null;
    }

    protected abstract void readAdditionalSaveData(CompoundTag nbt);

    protected abstract void addAdditionalSaveData(CompoundTag nbt);

    protected ListTag newDoubleList(double... values) {
        ListTag nbttaglist = new ListTag();
        double[] adouble1 = values;
        int i = values.length;

        for (int j = 0; j < i; ++j) {
            double d0 = adouble1[j];

            nbttaglist.add(DoubleTag.valueOf(d0));
        }

        return nbttaglist;
    }

    protected ListTag newFloatList(float... values) {
        ListTag nbttaglist = new ListTag();
        float[] afloat1 = values;
        int i = values.length;

        for (int j = 0; j < i; ++j) {
            float f = afloat1[j];

            nbttaglist.add(FloatTag.valueOf(f));
        }

        return nbttaglist;
    }

    @Nullable
    public ItemEntity spawnAtLocation(ItemLike item) {
        return this.spawnAtLocation(item, 0);
    }

    @Nullable
    public ItemEntity spawnAtLocation(ItemLike item, int yOffset) {
        return this.spawnAtLocation(new ItemStack(item), (float) yOffset);
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
            // CraftBukkit start - Capture drops for death event
            if (this instanceof net.minecraft.world.entity.LivingEntity && !((net.minecraft.world.entity.LivingEntity) this).forceDrops) {
                ((net.minecraft.world.entity.LivingEntity) this).drops.add(org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack.asCraftMirror(stack)); // Paper - mirror so we can destroy it later
                return null;
            }
            // CraftBukkit end
            ItemEntity entityitem = new ItemEntity(this.level, this.getX(), this.getY() + (double) yOffset, this.getZ(), stack.copy()); // Paper - clone so we can destroy original
            stack.setCount(0); // Paper - destroy this item - if this ever leaks due to game bugs, ensure it doesn't dupe

            entityitem.setDefaultPickUpDelay();
            // CraftBukkit start
            EntityDropItemEvent event = new EntityDropItemEvent(this.getBukkitEntity(), (org.bukkit.entity.Item) entityitem.getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return null;
            }
            // CraftBukkit end
            this.level.addFreshEntity(entityitem);
            return entityitem;
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
            AABB axisalignedbb = AABB.ofSize(this.getEyePosition(), (double) f, 1.0E-6D, (double) f);

            BlockPos.MutableBlockPos blockposition = new BlockPos.MutableBlockPos();
            int minX = Mth.floor(axisalignedbb.minX);
            int minY = Mth.floor(axisalignedbb.minY);
            int minZ = Mth.floor(axisalignedbb.minZ);
            int maxX = Mth.floor(axisalignedbb.maxX);
            int maxY = Mth.floor(axisalignedbb.maxY);
            int maxZ = Mth.floor(axisalignedbb.maxZ);
            for (int fz = minZ; fz <= maxZ; ++fz) {
                for (int fx = minX; fx <= maxX; ++fx) {
                    for (int fy = minY; fy <= maxY; ++fy) {
                        net.minecraft.world.level.chunk.LevelChunk chunk = (net.minecraft.world.level.chunk.LevelChunk)this.level.getChunkIfLoadedImmediately(fx >> 4, fz >> 4);
                        if (chunk == null) {
                            continue;
                        }

                        BlockState iblockdata = chunk.getBlockStateFinal(fx, fy, fz);
                        blockposition.set(fx, fy, fz);
                        if (!iblockdata.isAir() && iblockdata.isSuffocating(this.level, blockposition) && Shapes.joinIsNotEmpty(iblockdata.getCollisionShape(this.level, blockposition).move((double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ()), Shapes.create(axisalignedbb), BooleanOp.AND)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    public boolean canCollideWith(Entity other) { // Paper - diff on change, hard colliding entities override this - TODO CHECK ON UPDATE - AbstractMinecart/Boat override
        return other.canBeCollidedWith() && !this.isPassengerOfSameVehicle(other);
    }

    public boolean canBeCollidedWith() { // Paper - diff on change, hard colliding entities override this TODO CHECK ON UPDATE - Boat/Shulker override
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
            double d0 = this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset();

            positionUpdater.accept(passenger, this.getX(), d0, this.getZ());
        }
    }

    public void onPassengerTurned(Entity passenger) {}

    public double getMyRidingOffset() {
        return 0.0D;
    }

    public double getPassengersRidingOffset() {
        return (double) this.dimensions.height * 0.75D;
    }

    public boolean startRiding(Entity entity) {
        return this.startRiding(entity, false);
    }

    public boolean showVehicleHealth() {
        return this instanceof net.minecraft.world.entity.LivingEntity;
    }

    public boolean startRiding(Entity entity, boolean force) {
        if (entity == this.vehicle) {
            return false;
        } else {
            for (Entity entity1 = entity; entity1.vehicle != null; entity1 = entity1.vehicle) {
                if (entity1.vehicle == this) {
                    return false;
                }
            }

            if (!force && (!this.canRide(entity) || !entity.canAddPassenger(this))) {
                return false;
            } else {
                if (this.isPassenger()) {
                    this.stopRiding();
                }

                this.setPose(net.minecraft.world.entity.Pose.STANDING);
                this.vehicle = entity;
                if (!this.vehicle.addPassenger(this)) this.vehicle = null; // CraftBukkit
                entity.getIndirectPassengersStream().filter((entity2) -> {
                    return entity2 instanceof ServerPlayer;
                }).forEach((entity2) -> {
                    CriteriaTriggers.START_RIDING_TRIGGER.trigger((ServerPlayer) entity2);
                });
                return true;
            }
        }
    }

    protected boolean canRide(Entity entity) {
        return !this.isShiftKeyDown() && this.boardingCooldown <= 0;
    }

    protected boolean canEnterPose(net.minecraft.world.entity.Pose pose) {
        return this.level.noCollision(this, this.getBoundingBoxForPose(pose).deflate(1.0E-7D));
    }

    public void ejectPassengers() {
        for (int i = this.passengers.size() - 1; i >= 0; --i) {
            ((Entity) this.passengers.get(i)).stopRiding();
        }

    }

    public void removeVehicle() {
        // Paper start
        stopRiding(false);
    }
    public void stopRiding(boolean suppressCancellation) {
        // Paper end
        if (this.vehicle != null) {
            Entity entity = this.vehicle;

            this.vehicle = null;
            if (!entity.removePassenger(this, suppressCancellation)) this.vehicle = entity; // CraftBukkit // Paper
        }

    }

    public void stopRiding() {
        this.removeVehicle();
    }

    protected boolean addPassenger(Entity entity) { // CraftBukkit
        // Paper start
        if (entity.level != this.level) {
            throw new IllegalArgumentException("Entity passenger world must match");
        }
        // Paper end
        if (entity == this) throw new IllegalArgumentException("Entities cannot become a passenger of themselves"); // Paper - issue 572
        if (entity.getVehicle() != this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        } else {
            // CraftBukkit start
            com.google.common.base.Preconditions.checkState(!entity.passengers.contains(this), "Circular entity riding! %s %s", this, entity);

            CraftEntity craft = (CraftEntity) entity.getBukkitEntity().getVehicle();
            Entity orig = craft == null ? null : craft.getHandle();
            if (this.getBukkitEntity() instanceof Vehicle && entity.getBukkitEntity() instanceof LivingEntity) {
                VehicleEnterEvent event = new VehicleEnterEvent(
                        (Vehicle) this.getBukkitEntity(),
                         entity.getBukkitEntity()
                );
                // Suppress during worldgen
                if (this.valid) {
                    Bukkit.getPluginManager().callEvent(event);
                }
                CraftEntity craftn = (CraftEntity) entity.getBukkitEntity().getVehicle();
                Entity n = craftn == null ? null : craftn.getHandle();
                if (event.isCancelled() || n != orig) {
                    return false;
                }
            }
            // CraftBukkit end
            // Spigot start
            org.spigotmc.event.entity.EntityMountEvent event = new org.spigotmc.event.entity.EntityMountEvent(entity.getBukkitEntity(), this.getBukkitEntity());
            // Suppress during worldgen
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                return false;
            }
            // Spigot end
            if (this.passengers.isEmpty()) {
                this.passengers = ImmutableList.of(entity);
            } else {
                List<Entity> list = Lists.newArrayList(this.passengers);

                if (!this.level.isClientSide && entity instanceof Player && !(this.getControllingPassenger() instanceof Player)) {
                    list.add(0, entity);
                } else {
                    list.add(entity);
                }

                this.passengers = ImmutableList.copyOf(list);
            }

        }
        return true; // CraftBukkit
    }

    // Paper start
    protected boolean removePassenger(Entity entity) { return removePassenger(entity, false);}
    protected boolean removePassenger(Entity entity, boolean suppressCancellation) { // CraftBukkit
        // Paper end
        if (entity.getVehicle() == this) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        } else {
            // CraftBukkit start
            CraftEntity craft = (CraftEntity) entity.getBukkitEntity().getVehicle();
            Entity orig = craft == null ? null : craft.getHandle();
            if (this.getBukkitEntity() instanceof Vehicle && entity.getBukkitEntity() instanceof LivingEntity) {
                VehicleExitEvent event = new VehicleExitEvent(
                        (Vehicle) this.getBukkitEntity(),
                        (LivingEntity) entity.getBukkitEntity(), !suppressCancellation // Paper
                );
                // Suppress during worldgen
                if (this.valid) {
                    Bukkit.getPluginManager().callEvent(event);
                }
                CraftEntity craftn = (CraftEntity) entity.getBukkitEntity().getVehicle();
                Entity n = craftn == null ? null : craftn.getHandle();
                if (event.isCancelled() || n != orig) {
                    return false;
                }
            }
            // CraftBukkit end
            // Spigot start
            org.spigotmc.event.entity.EntityDismountEvent event = new org.spigotmc.event.entity.EntityDismountEvent(entity.getBukkitEntity(), this.getBukkitEntity(), !suppressCancellation); // Paper
            // Suppress during worldgen
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                return false;
            }
            // Spigot end
            if (this.passengers.size() == 1 && this.passengers.get(0) == entity) {
                this.passengers = ImmutableList.of();
            } else {
                this.passengers = (ImmutableList) this.passengers.stream().filter((entity1) -> {
                    return entity1 != entity;
                }).collect(ImmutableList.toImmutableList());
            }

            entity.boardingCooldown = 60;
        }
        return true; // CraftBukkit
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
            Player entityhuman = (Player) this;
            boolean flag = entityhuman.getOffhandItem().is(item) && !entityhuman.getMainHandItem().is(item);
            HumanoidArm enummainhand = flag ? entityhuman.getMainArm().getOpposite() : entityhuman.getMainArm();

            return this.calculateViewVector(0.0F, this.getYRot() + (float) (enummainhand == HumanoidArm.RIGHT ? 80 : -80)).scale(0.5D);
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
            ServerLevel worldserver = (ServerLevel) this.level;

            if (this.isInsidePortal) {
                MinecraftServer minecraftserver = worldserver.getServer();
                ResourceKey<Level> resourcekey = this.level.getTypeKey() == LevelStem.NETHER ? Level.OVERWORLD : Level.NETHER; // CraftBukkit
                ServerLevel worldserver1 = minecraftserver.getLevel(resourcekey);

                if (true && !this.isPassenger() && this.portalTime++ >= i) { // CraftBukkit
                    this.level.getProfiler().push("portal");
                    this.portalTime = i;
                    this.setPortalCooldown();
                    // CraftBukkit start
                    if (this instanceof ServerPlayer) {
                        ((ServerPlayer) this).changeDimension(worldserver1, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
                    } else {
                        this.changeDimension(worldserver1);
                    }
                    // CraftBukkit end
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
            this.tickEndPortal(); // Paper - make end portalling safe
        }
    }

    public int getDimensionChangingDelay() {
        return 300;
    }

    public void lerpMotion(double x, double y, double z) {
        this.setDeltaMovement(x, y, z);
    }

    public void handleEntityEvent(byte status) {
        switch (status) {
            case 53:
                HoneyBlock.showSlideParticles(this);
            default:
        }
    }

    public void animateHurt() {}

    public Iterable<ItemStack> getHandSlots() {
        return Entity.EMPTY_LIST;
    }

    public Iterable<ItemStack> getArmorSlots() {
        return Entity.EMPTY_LIST;
    }

    public Iterable<ItemStack> getAllSlots() {
        return Iterables.concat(this.getHandSlots(), this.getArmorSlots());
    }

    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    public boolean isOnFire() {
        boolean flag = this.level != null && this.level.isClientSide;

        return !this.fireImmune() && (this.remainingFireTicks > 0 || flag && this.getSharedFlag(0));
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
        return this.getPose() == net.minecraft.world.entity.Pose.CROUCHING;
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
        return this.getPose() == net.minecraft.world.entity.Pose.SWIMMING;
    }

    public boolean isVisuallyCrawling() {
        return this.isVisuallySwimming() && !this.isInWater();
    }

    public void setSwimming(boolean swimming) {
        // CraftBukkit start
        if (this.valid && this.isSwimming() != swimming && this instanceof net.minecraft.world.entity.LivingEntity) {
            if (CraftEventFactory.callToggleSwimEvent((net.minecraft.world.entity.LivingEntity) this, swimming).isCancelled()) {
                return;
            }
        }
        // CraftBukkit end
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
            Team scoreboardteambase = this.getTeam();

            return scoreboardteambase != null && player != null && player.getTeam() == scoreboardteambase && scoreboardteambase.canSeeFriendlyInvisibles() ? false : this.isInvisible();
        }
    }

    @Nullable
    public GameEventListenerRegistrar getGameEventListenerRegistrar() {
        return null;
    }

    @Nullable
    public Team getTeam() {
        if (!this.level.paperConfig.nonPlayerEntitiesOnScoreboards && !(this instanceof Player)) { return null; } // Paper
        return this.level.getScoreboard().getPlayersTeam(this.getScoreboardName());
    }

    public boolean isAlliedTo(Entity other) {
        return this.isAlliedTo(other.getTeam());
    }

    public boolean isAlliedTo(Team team) {
        return this.getTeam() != null ? this.getTeam().isAlliedTo(team) : false;
    }

    // CraftBukkit - start
    public void setInvisible(boolean invisible) {
        if (!this.persistentInvisibility) { // Prevent Minecraft from removing our invisibility flag
            this.setSharedFlag(5, invisible);
        }
        // CraftBukkit - end
    }

    public boolean getSharedFlag(int index) {
        return ((Byte) this.entityData.get(Entity.DATA_SHARED_FLAGS_ID) & 1 << index) != 0;
    }

    public void setSharedFlag(int index, boolean value) {
        byte b0 = (Byte) this.entityData.get(Entity.DATA_SHARED_FLAGS_ID);

        if (value) {
            this.entityData.set(Entity.DATA_SHARED_FLAGS_ID, (byte) (b0 | 1 << index));
        } else {
            this.entityData.set(Entity.DATA_SHARED_FLAGS_ID, (byte) (b0 & ~(1 << index)));
        }

    }

    public int getMaxAirSupply() {
        return this.maxAirTicks; // CraftBukkit - SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
    }

    public int getAirSupply() {
        return (Integer) this.entityData.get(Entity.DATA_AIR_SUPPLY_ID);
    }

    public void setAirSupply(int air) {
        // CraftBukkit start
        EntityAirChangeEvent event = new EntityAirChangeEvent(this.getBukkitEntity(), air);
        // Suppress during worldgen
        if (this.valid) {
            event.getEntity().getServer().getPluginManager().callEvent(event);
        }
        if (event.isCancelled() && this.getAirSupply() != air) {
            this.entityData.markDirty(Entity.DATA_AIR_SUPPLY_ID);
            return;
        }
        this.entityData.set(Entity.DATA_AIR_SUPPLY_ID, event.getAmount());
        // CraftBukkit end
    }

    public int getTicksFrozen() {
        return (Integer) this.entityData.get(Entity.DATA_TICKS_FROZEN);
    }

    public void setTicksFrozen(int frozenTicks) {
        this.entityData.set(Entity.DATA_TICKS_FROZEN, frozenTicks);
    }

    public float getPercentFrozen() {
        int i = this.getTicksRequiredToFreeze();

        return (float) Math.min(this.getTicksFrozen(), i) / (float) i;
    }

    public boolean isFullyFrozen() {
        return this.getTicksFrozen() >= this.getTicksRequiredToFreeze();
    }

    public int getTicksRequiredToFreeze() {
        return 140;
    }

    public void thunderHit(ServerLevel world, LightningBolt lightning) {
        this.setRemainingFireTicks(this.remainingFireTicks + 1);
        // CraftBukkit start
        final org.bukkit.entity.Entity thisBukkitEntity = this.getBukkitEntity();
        final org.bukkit.entity.Entity stormBukkitEntity = lightning.getBukkitEntity();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        // CraftBukkit end

        if (this.remainingFireTicks == 0) {
            // CraftBukkit start - Call a combust event when lightning strikes
            EntityCombustByEntityEvent entityCombustEvent = new EntityCombustByEntityEvent(stormBukkitEntity, thisBukkitEntity, 8);
            pluginManager.callEvent(entityCombustEvent);
            if (!entityCombustEvent.isCancelled()) {
                this.setSecondsOnFire(entityCombustEvent.getDuration(), false);
            }
            // CraftBukkit end
        }

        // CraftBukkit start
        if (thisBukkitEntity instanceof Hanging) {
            HangingBreakByEntityEvent hangingEvent = new HangingBreakByEntityEvent((Hanging) thisBukkitEntity, stormBukkitEntity);
            pluginManager.callEvent(hangingEvent);

            if (hangingEvent.isCancelled()) {
                return;
            }
        }

        if (this.fireImmune()) {
            return;
        }
        CraftEventFactory.entityDamage = lightning;
        if (!this.hurt(DamageSource.LIGHTNING_BOLT, 5.0F)) {
            CraftEventFactory.entityDamage = null;
            return;
        }
        // CraftBukkit end
    }

    public void onAboveBubbleCol(boolean drag) {
        Vec3 vec3d = this.getDeltaMovement();
        double d0;

        if (drag) {
            d0 = Math.max(-0.9D, vec3d.y - 0.03D);
        } else {
            d0 = Math.min(1.8D, vec3d.y + 0.1D);
        }

        this.setDeltaMovement(vec3d.x, d0, vec3d.z);
    }

    public void onInsideBubbleColumn(boolean drag) {
        Vec3 vec3d = this.getDeltaMovement();
        double d0;

        if (drag) {
            d0 = Math.max(-0.3D, vec3d.y - 0.03D);
        } else {
            d0 = Math.min(0.7D, vec3d.y + 0.06D);
        }

        this.setDeltaMovement(vec3d.x, d0, vec3d.z);
        this.resetFallDistance();
    }

    public void killed(ServerLevel world, net.minecraft.world.entity.LivingEntity other) {}

    public void resetFallDistance() {
        this.fallDistance = 0.0F;
    }

    protected void moveTowardsClosestSpace(double x, double y, double z) {
        BlockPos blockposition = new BlockPos(x, y, z);
        Vec3 vec3d = new Vec3(x - (double) blockposition.getX(), y - (double) blockposition.getY(), z - (double) blockposition.getZ());
        BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
        Direction enumdirection = Direction.UP;
        double d3 = Double.MAX_VALUE;
        Direction[] aenumdirection = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};
        int i = aenumdirection.length;

        for (int j = 0; j < i; ++j) {
            Direction enumdirection1 = aenumdirection[j];

            blockposition_mutableblockposition.setWithOffset(blockposition, enumdirection1);
            if (!this.level.getBlockState(blockposition_mutableblockposition).isCollisionShapeFullBlock(this.level, blockposition_mutableblockposition)) {
                double d4 = vec3d.get(enumdirection1.getAxis());
                double d5 = enumdirection1.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0D - d4 : d4;

                if (d5 < d3) {
                    d3 = d5;
                    enumdirection = enumdirection1;
                }
            }
        }

        float f = this.random.nextFloat() * 0.2F + 0.1F;
        float f1 = (float) enumdirection.getAxisDirection().getStep();
        Vec3 vec3d1 = this.getDeltaMovement().scale(0.75D);

        if (enumdirection.getAxis() == Direction.Axis.X) {
            this.setDeltaMovement((double) (f1 * f), vec3d1.y, vec3d1.z);
        } else if (enumdirection.getAxis() == Direction.Axis.Y) {
            this.setDeltaMovement(vec3d1.x, (double) (f1 * f), vec3d1.z);
        } else if (enumdirection.getAxis() == Direction.Axis.Z) {
            this.setDeltaMovement(vec3d1.x, vec3d1.y, (double) (f1 * f));
        }

    }

    public void makeStuckInBlock(BlockState state, Vec3 multiplier) {
        this.resetFallDistance();
        this.stuckSpeedMultiplier = multiplier;
    }

    private static Component removeAction(Component textComponent) {
        MutableComponent ichatmutablecomponent = textComponent.plainCopy().setStyle(textComponent.getStyle().withClickEvent((ClickEvent) null));
        Iterator iterator = textComponent.getSiblings().iterator();

        while (iterator.hasNext()) {
            Component ichatbasecomponent1 = (Component) iterator.next();

            ichatmutablecomponent.append(Entity.removeAction(ichatbasecomponent1));
        }

        return ichatmutablecomponent;
    }

    @Override
    public Component getName() {
        Component ichatbasecomponent = this.getCustomName();

        return ichatbasecomponent != null ? Entity.removeAction(ichatbasecomponent) : this.getTypeName();
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

    public void setYHeadRot(float headYaw) {}

    public void setYBodyRot(float bodyYaw) {}

    public boolean isAttackable() {
        return true;
    }

    public boolean skipAttackInteraction(Entity attacker) {
        return false;
    }

    public String toString() {
        String s = this.level == null ? "~NULL~" : this.level.toString();

        return this.removalReason != null ? String.format(Locale.ROOT, "%s['%s'/%d, uuid='%s', l='%s', x=%.2f, y=%.2f, z=%.2f, cpos=%s, tl=%d, v=%b, removed=%s]", this.getClass().getSimpleName(), this.getName().getString(), this.id, this.uuid, s, this.getX(), this.getY(), this.getZ(), this.chunkPosition(), this.tickCount, this.valid, this.removalReason) : String.format(Locale.ROOT, "%s['%s'/%d, uuid='%s', l='%s', x=%.2f, y=%.2f, z=%.2f, cpos=%s, tl=%d, v=%b]", this.getClass().getSimpleName(), this.getName().getString(), this.id, this.uuid, s, this.getX(), this.getY(), this.getZ(), this.chunkPosition(), this.tickCount, this.valid);
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
        // Paper start
        CraftEntity bukkitEntity = original.bukkitEntity;
        if (bukkitEntity != null) {
            bukkitEntity.setHandle(this);
            this.bukkitEntity = bukkitEntity;
        }
        // Paper end
        CompoundTag nbttagcompound = original.saveWithoutId(new CompoundTag());

        nbttagcompound.remove("Dimension");
        this.load(nbttagcompound);
        this.portalCooldown = original.portalCooldown;
        this.portalEntrancePos = original.portalEntrancePos;
    }

    @Nullable
    public Entity changeDimension(ServerLevel destination) {
        // CraftBukkit start
        return this.teleportTo(destination, null);
    }

    @Nullable
    public Entity teleportTo(ServerLevel worldserver, BlockPos location) {
        // CraftBukkit end
        // Paper start - fix bad state entities causing dupes
        if (!isAlive() || !valid) {
            LOGGER.warn("Illegal Entity Teleport " + this + " to " + worldserver + ":" + location, new Throwable());
            return null;
        }
        // Paper end
        if (this.level instanceof ServerLevel && !this.isRemoved()) {
            this.level.getProfiler().push("changeDimension");
            // CraftBukkit start
            // this.decouple();
            if (worldserver == null) {
                return null;
            }
            // CraftBukkit end
            this.level.getProfiler().push("reposition");
            PortalInfo shapedetectorshape = (location == null) ? this.findDimensionEntryPoint(worldserver) : new PortalInfo(new Vec3(location.getX(), location.getY(), location.getZ()), Vec3.ZERO, this.yRot, this.xRot, worldserver, null); // CraftBukkit

            if (shapedetectorshape == null) {
                return null;
            } else {
                // CraftBukkit start
                worldserver = shapedetectorshape.world;
                // Paper start - Call EntityPortalExitEvent
                CraftEntity bukkitEntity = this.getBukkitEntity();
                Vec3 position = shapedetectorshape.pos;
                float yaw = shapedetectorshape.yRot;
                float pitch = bukkitEntity.getLocation().getPitch(); // Keep entity pitch as per moveTo line below
                Vec3 velocity = shapedetectorshape.speed;
                org.bukkit.event.entity.EntityPortalExitEvent event = new org.bukkit.event.entity.EntityPortalExitEvent(bukkitEntity,
                    bukkitEntity.getLocation(), new Location(worldserver.getWorld(), position.x, position.y, position.z, yaw, pitch),
                    bukkitEntity.getVelocity(), org.bukkit.craftbukkit.v1_18_R2.util.CraftVector.toBukkit(shapedetectorshape.speed));
                if (event.callEvent() && event.getTo() != null && this.isAlive()) {
                    worldserver = ((CraftWorld) event.getTo().getWorld()).getHandle();
                    position = new Vec3(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
                    yaw = event.getTo().getYaw();
                    pitch = event.getTo().getPitch();
                    velocity = org.bukkit.craftbukkit.v1_18_R2.util.CraftVector.toNMS(event.getAfter());
                }
                // Paper end
                if (worldserver == this.level) {
                    // SPIGOT-6782: Just move the entity if a plugin changed the world to the one the entity is already in
                    this.moveTo(shapedetectorshape.pos.x, shapedetectorshape.pos.y, shapedetectorshape.pos.z, shapedetectorshape.yRot, shapedetectorshape.xRot);
                    this.setDeltaMovement(shapedetectorshape.speed);
                    return this;
                }
                this.unRide();
                // CraftBukkit end

                this.level.getProfiler().popPush("reloading");
                // Paper start - Change lead drop timing to prevent dupe
                if (this instanceof Mob) {
                    ((Mob) this).dropLeash(true, true); // Paper drop lead
                }
                // Paper end
                Entity entity = this.getType().create(worldserver);

                if (entity != null) {
                    entity.restoreFrom(this);
                    entity.moveTo(position.x, position.y, position.z, yaw, pitch); // Paper - use EntityPortalExitEvent values
                    entity.setDeltaMovement(velocity); // Paper - use EntityPortalExitEvent values
                    worldserver.addDuringTeleport(entity);
                    if (worldserver.getTypeKey() == LevelStem.END) { // CraftBukkit
                        ServerLevel.makeObsidianPlatform(worldserver, this); // CraftBukkit
                    }
                    // // CraftBukkit start - Forward the CraftEntity to the new entity // Paper - moved to Entity#restoreFrom
                    // this.getBukkitEntity().setHandle(entity);
                    // entity.bukkitEntity = this.getBukkitEntity();
                    // // CraftBukkit end
                }

                this.removeAfterChangingDimensions();
                this.level.getProfiler().pop();
                ((ServerLevel) this.level).resetEmptyTime();
                worldserver.resetEmptyTime();
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
        // CraftBukkit start
        if (destination == null) {
            return null;
        }
        boolean flag = this.level.getTypeKey() == LevelStem.END && destination.getTypeKey() == LevelStem.OVERWORLD; // fromEndToOverworld
        boolean flag1 = destination.getTypeKey() == LevelStem.END; // targetIsEnd
        // CraftBukkit end

        if (!flag && !flag1) {
            boolean flag2 = destination.getTypeKey() == LevelStem.NETHER; // CraftBukkit

            if (this.level.getTypeKey() != LevelStem.NETHER && !flag2) { // CraftBukkit
                return null;
            } else {
                WorldBorder worldborder = destination.getWorldBorder();
                double d0 = DimensionType.getTeleportationScale(this.level.dimensionType(), destination.dimensionType());
                BlockPos blockposition = worldborder.clampToBounds(this.getX() * d0, this.getY(), this.getZ() * d0);
                // CraftBukkit start
                // Paper start
                int portalSearchRadius = destination.paperConfig.portalSearchRadius;
                if (level.paperConfig.portalSearchVanillaDimensionScaling && flag2) { // == THE_NETHER
                    portalSearchRadius = (int) (portalSearchRadius / destination.dimensionType().coordinateScale());
                }
                // Paper end
                CraftPortalEvent event = this.callPortalEvent(this, destination, blockposition, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL, portalSearchRadius, destination.paperConfig.portalCreateRadius); // Paper start - configurable portal radius
                if (event == null) {
                    return null;
                }
                final ServerLevel worldserverFinal = destination = ((CraftWorld) event.getTo().getWorld()).getHandle();
                worldborder = worldserverFinal.getWorldBorder();
                blockposition = worldborder.clampToBounds(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());

                return (PortalInfo) this.getExitPortal(destination, blockposition, flag2, worldborder, event.getSearchRadius(), event.getCanCreatePortal(), event.getCreationRadius()).map((blockutil_rectangle) -> {
                    // CraftBukkit end
                    BlockState iblockdata = this.level.getBlockState(this.portalEntrancePos);
                    Direction.Axis enumdirection_enumaxis;
                    Vec3 vec3d;

                    if (iblockdata.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                        enumdirection_enumaxis = (Direction.Axis) iblockdata.getValue(BlockStateProperties.HORIZONTAL_AXIS);
                        BlockUtil.FoundRectangle blockutil_rectangle1 = BlockUtil.getLargestRectangleAround(this.portalEntrancePos, enumdirection_enumaxis, 21, Direction.Axis.Y, 21, (blockposition1) -> {
                            return this.level.getBlockState(blockposition1) == iblockdata;
                        });

                        vec3d = this.getRelativePortalPosition(enumdirection_enumaxis, blockutil_rectangle1);
                    } else {
                        enumdirection_enumaxis = Direction.Axis.X;
                        vec3d = new Vec3(0.5D, 0.0D, 0.0D);
                    }

                    return PortalShape.createPortalInfo(worldserverFinal, blockutil_rectangle, enumdirection_enumaxis, vec3d, this.getDimensions(this.getPose()), this.getDeltaMovement(), this.getYRot(), this.getXRot(), event); // CraftBukkit
                }).orElse(null); // CraftBuukkit - decompile error
            }
        } else {
            BlockPos blockposition1;

            if (flag1) {
                blockposition1 = ServerLevel.END_SPAWN_POINT;
            } else {
                // Paper start - Ensure spawn chunk is always loaded before calculating Y coordinate
                destination.getChunkAt(destination.getSharedSpawnPos());
                // Paper end
                blockposition1 = destination.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, destination.getSharedSpawnPos());
            }
            // CraftBukkit start
            CraftPortalEvent event = this.callPortalEvent(this, destination, blockposition1, PlayerTeleportEvent.TeleportCause.END_PORTAL, 0, 0);
            if (event == null) {
                return null;
            }
            blockposition1 = new BlockPos(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());

            return new PortalInfo(new Vec3((double) blockposition1.getX() + 0.5D, (double) blockposition1.getY(), (double) blockposition1.getZ() + 0.5D), this.getDeltaMovement(), this.getYRot(), this.getXRot(), ((CraftWorld) event.getTo().getWorld()).getHandle(), event);
            // CraftBukkit end
        }
    }

    protected Vec3 getRelativePortalPosition(Direction.Axis portalAxis, BlockUtil.FoundRectangle portalRect) {
        return PortalShape.getRelativePosition(portalRect, portalAxis, this.position(), this.getDimensions(this.getPose()));
    }

    // CraftBukkit start
    protected CraftPortalEvent callPortalEvent(Entity entity, ServerLevel exitWorldServer, BlockPos exitPosition, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
        Location enter = bukkitEntity.getLocation();
        Location exit = new Location(exitWorldServer.getWorld(), exitPosition.getX(), exitPosition.getY(), exitPosition.getZ());

        EntityPortalEvent event = new EntityPortalEvent(bukkitEntity, enter, exit, searchRadius);
        event.getEntity().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null || !entity.isAlive()) {
            return null;
        }
        return new CraftPortalEvent(event);
    }

    protected Optional<BlockUtil.FoundRectangle> getExitPortal(ServerLevel worldserver, BlockPos blockposition, boolean flag, WorldBorder worldborder, int searchRadius, boolean canCreatePortal, int createRadius) {
        return worldserver.getPortalForcer().findPortalAround(blockposition, worldborder, searchRadius);
        // CraftBukkit end
    }

    public boolean canChangeDimensions() {
        return isAlive() && valid; // Paper
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
            ResourceLocation minecraftkey = EntityType.getKey(this.getType());

            return minecraftkey + " (" + this.getClass().getCanonicalName() + ")";
        });
        section.setDetail("Entity ID", (Object) this.id);
        section.setDetail("Entity Name", () -> {
            return this.getName().getString();
        });
        section.setDetail("Entity's Exact location", (Object) String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
        section.setDetail("Entity's Block location", (Object) CrashReportCategory.formatLocation(this.level, Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ())));
        Vec3 vec3d = this.getDeltaMovement();

        section.setDetail("Entity's Momentum", (Object) String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec3d.x, vec3d.y, vec3d.z));
        section.setDetail("Entity's Passengers", () -> {
            return this.getPassengers().toString();
        });
        section.setDetail("Entity's Vehicle", () -> {
            return String.valueOf(this.getVehicle());
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
        return Entity.viewScale;
    }

    public static void setViewScale(double value) {
        Entity.viewScale = value;
    }

    @Override
    public Component getDisplayName() {
        return PlayerTeam.formatNameForTeam(this.getTeam(), this.getName()).withStyle((chatmodifier) -> {
            return chatmodifier.withHoverEvent(this.createHoverEvent()).withInsertion(this.getStringUUID());
        });
    }

    public void setCustomName(@Nullable Component name) {
        this.entityData.set(Entity.DATA_CUSTOM_NAME, Optional.ofNullable(name));
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return (Component) ((Optional) this.entityData.get(Entity.DATA_CUSTOM_NAME)).orElse((Object) null);
    }

    @Override
    public boolean hasCustomName() {
        return ((Optional) this.entityData.get(Entity.DATA_CUSTOM_NAME)).isPresent();
    }

    public void setCustomNameVisible(boolean visible) {
        this.entityData.set(Entity.DATA_CUSTOM_NAME_VISIBLE, visible);
    }

    public boolean isCustomNameVisible() {
        return (Boolean) this.entityData.get(Entity.DATA_CUSTOM_NAME_VISIBLE);
    }

    public final void teleportToWithTicket(double destX, double destY, double destZ) {
        if (this.level instanceof ServerLevel) {
            ChunkPos chunkcoordintpair = new ChunkPos(new BlockPos(destX, destY, destZ));

            ((ServerLevel) this.level).getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkcoordintpair, 0, this.getId());
            this.level.getChunk(chunkcoordintpair.x, chunkcoordintpair.z);
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
                UnmodifiableIterator unmodifiableiterator = entity.passengers.iterator();

                while (unmodifiableiterator.hasNext()) {
                    Entity entity1 = (Entity) unmodifiableiterator.next();

                    entity.positionRider(entity1, Entity::moveTo);
                }

            });
        }
    }

    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (Entity.DATA_POSE.equals(data)) {
            this.refreshDimensions();
        }

    }

    public void refreshDimensions() {
        EntityDimensions entitysize = this.dimensions;
        net.minecraft.world.entity.Pose entitypose = this.getPose();
        EntityDimensions entitysize1 = this.getDimensions(entitypose);

        this.dimensions = entitysize1;
        this.eyeHeight = this.getEyeHeight(entitypose, entitysize1);
        this.reapplyPosition();
        boolean flag = (double) entitysize1.width <= 4.0D && (double) entitysize1.height <= 4.0D;

        if (!this.level.isClientSide && !this.firstTick && !this.noPhysics && flag && (entitysize1.width > entitysize.width || entitysize1.height > entitysize.height) && !(this instanceof Player)) {
            Vec3 vec3d = this.position().add(0.0D, (double) entitysize.height / 2.0D, 0.0D);
            double d0 = (double) Math.max(0.0F, entitysize1.width - entitysize.width) + 1.0E-6D;
            double d1 = (double) Math.max(0.0F, entitysize1.height - entitysize.height) + 1.0E-6D;
            VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec3d, d0, d1, d0));

            this.level.findFreePosition(this, voxelshape, vec3d, (double) entitysize1.width, (double) entitysize1.height, (double) entitysize1.width).ifPresent((vec3d1) -> {
                this.setPos(vec3d1.add(0.0D, (double) (-entitysize1.height) / 2.0D, 0.0D));
            });
        }

    }

    public Direction getDirection() {
        return Direction.fromYRot((double) this.getYRot());
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

    protected AABB getBoundingBoxForPose(net.minecraft.world.entity.Pose pos) {
        EntityDimensions entitysize = this.getDimensions(pos);
        float f = entitysize.width / 2.0F;
        Vec3 vec3d = new Vec3(this.getX() - (double) f, this.getY(), this.getZ() - (double) f);
        Vec3 vec3d1 = new Vec3(this.getX() + (double) f, this.getY() + (double) entitysize.height, this.getZ() + (double) f);

        return new AABB(vec3d, vec3d1);
    }

    public final void setBoundingBox(AABB boundingBox) {
        // CraftBukkit start - block invalid bounding boxes
        double minX = boundingBox.minX,
                minY = boundingBox.minY,
                minZ = boundingBox.minZ,
                maxX = boundingBox.maxX,
                maxY = boundingBox.maxY,
                maxZ = boundingBox.maxZ;
        double len = boundingBox.maxX - boundingBox.minX;
        if (len < 0) maxX = minX;
        if (len > 64) maxX = minX + 64.0;

        len = boundingBox.maxY - boundingBox.minY;
        if (len < 0) maxY = minY;
        if (len > 64) maxY = minY + 64.0;

        len = boundingBox.maxZ - boundingBox.minZ;
        if (len < 0) maxZ = minZ;
        if (len > 64) maxZ = minZ + 64.0;
        this.bb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        // CraftBukkit end
    }

    protected float getEyeHeight(net.minecraft.world.entity.Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.85F;
    }

    public float getEyeHeight(net.minecraft.world.entity.Pose pose) {
        return this.getEyeHeight(pose, this.getDimensions(pose));
    }

    public final float getEyeHeight() {
        return this.eyeHeight;
    }

    public Vec3 getLeashOffset() {
        return new Vec3(0.0D, (double) this.getEyeHeight(), (double) (this.getBbWidth() * 0.4F));
    }

    public SlotAccess getSlot(int mappedIndex) {
        return SlotAccess.NULL;
    }

    @Override
    public void sendMessage(Component message, UUID sender) {}

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

    public void doEnchantDamageEffects(net.minecraft.world.entity.LivingEntity attacker, Entity target) {
        if (target instanceof net.minecraft.world.entity.LivingEntity) {
            EnchantmentHelper.doPostHurtEffects((net.minecraft.world.entity.LivingEntity) target, attacker);
        }

        EnchantmentHelper.doPostDamageEffects(attacker, target);
    }

    public void startSeenByPlayer(ServerPlayer player) {}

    public void stopSeenByPlayer(ServerPlayer player) {}

    public float rotate(Rotation rotation) {
        float f = Mth.wrapDegrees(this.getYRot());

        switch (rotation) {
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

        switch (mirror) {
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
        return this.passengers.isEmpty() ? null : (Entity) this.passengers.get(0);
    }

    public boolean hasPassenger(Entity passenger) {
        return this.passengers.contains(passenger);
    }

    public boolean hasPassenger(Predicate<Entity> predicate) {
        UnmodifiableIterator unmodifiableiterator = this.passengers.iterator();

        Entity entity;

        do {
            if (!unmodifiableiterator.hasNext()) {
                return false;
            }

            entity = (Entity) unmodifiableiterator.next();
        } while (!predicate.test(entity));

        return true;
    }

    private Stream<Entity> getIndirectPassengersStream() {
        if (this.passengers.isEmpty()) { return Stream.of(); } // Paper
        return this.passengers.stream().flatMap(Entity::getSelfAndPassengers);
    }

    @Override
    public Stream<Entity> getSelfAndPassengers() {
        if (this.passengers.isEmpty()) { return Stream.of(this); } // Paper
        return Stream.concat(Stream.of(this), this.getIndirectPassengersStream());
    }

    @Override
    public Stream<Entity> getPassengersAndSelf() {
        if (this.passengers.isEmpty()) { return Stream.of(this); } // Paper
        return Stream.concat(this.passengers.stream().flatMap(Entity::getPassengersAndSelf), Stream.of(this));
    }

    public Iterable<Entity> getIndirectPassengers() {
        // Paper start - rewrite this method
        if (this.passengers.isEmpty()) { return ImmutableList.of(); }
        ImmutableList.Builder<Entity> indirectPassengers = ImmutableList.builder();
        for (Entity passenger : this.passengers) {
            indirectPassengers.add(passenger);
            indirectPassengers.addAll(passenger.getIndirectPassengers());
        }
        return indirectPassengers.build();
    }
    private Iterable<Entity> getIndirectPassengers_old() {
        // Paper end
        return () -> {
            return this.getIndirectPassengersStream().iterator();
        };
    }

    public boolean hasExactlyOnePlayerPassenger() {
        if (this.passengers.isEmpty()) { return false; } // Paper
        return this.getIndirectPassengersStream().filter((entity) -> {
            return entity instanceof Player;
        }).count() == 1L;
    }

    public Entity getRootVehicle() {
        Entity entity;

        for (entity = this; entity.isPassenger(); entity = entity.getVehicle()) {
            ;
        }

        return entity;
    }

    public boolean isPassengerOfSameVehicle(Entity entity) {
        return this.getRootVehicle() == entity.getRootVehicle();
    }

    public boolean hasIndirectPassenger(Entity passenger) {
        return this.getIndirectPassengersStream().anyMatch((entity1) -> {
            return entity1 == passenger;
        });
    }

    public boolean isControlledByLocalInstance() {
        Entity entity = this.getControllingPassenger();

        return entity instanceof Player ? ((Player) entity).isLocalPlayer() : !this.level.isClientSide;
    }

    protected static Vec3 getCollisionHorizontalEscapeVector(double vehicleWidth, double passengerWidth, float passengerYaw) {
        double d2 = (vehicleWidth + passengerWidth + 9.999999747378752E-6D) / 2.0D;
        float f1 = -Mth.sin(passengerYaw * 0.017453292F);
        float f2 = Mth.cos(passengerYaw * 0.017453292F);
        float f3 = Math.max(Math.abs(f1), Math.abs(f2));

        return new Vec3((double) f1 * d2 / (double) f3, 0.0D, (double) f2 * d2 / (double) f3);
    }

    public Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
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
        return new CommandSourceStack(this, this.position(), this.getRotationVector(), this.level instanceof ServerLevel ? (ServerLevel) this.level : null, this.getPermissionLevel(), this.getName().getString(), this.getDisplayName(), this.level.getServer(), this);
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
        Vec3 vec3d1 = anchorPoint.apply(this);
        double d0 = target.x - vec3d1.x;
        double d1 = target.y - vec3d1.y;
        double d2 = target.z - vec3d1.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        this.setXRot(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 57.2957763671875D))));
        this.setYRot(Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875D) - 90.0F));
        this.setYHeadRot(this.getYRot());
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> tag, double speed) {
        if (this.touchingUnloadedChunk()) {
            return false;
        } else {
            AABB axisalignedbb = this.getBoundingBox().deflate(0.001D);
            int i = Mth.floor(axisalignedbb.minX);
            int j = Mth.ceil(axisalignedbb.maxX);
            int k = Mth.floor(axisalignedbb.minY);
            int l = Mth.ceil(axisalignedbb.maxY);
            int i1 = Mth.floor(axisalignedbb.minZ);
            int j1 = Mth.ceil(axisalignedbb.maxZ);
            double d1 = 0.0D;
            boolean flag = this.isPushedByFluid();
            boolean flag1 = false;
            Vec3 vec3d = Vec3.ZERO;
            int k1 = 0;
            BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();

            for (int l1 = i; l1 < j; ++l1) {
                for (int i2 = k; i2 < l; ++i2) {
                    for (int j2 = i1; j2 < j1; ++j2) {
                        blockposition_mutableblockposition.set(l1, i2, j2);
                        FluidState fluid = this.level.getFluidState(blockposition_mutableblockposition);

                        if (fluid.is(tag)) {
                            double d2 = (double) ((float) i2 + fluid.getHeight(this.level, blockposition_mutableblockposition));

                            if (d2 >= axisalignedbb.minY) {
                                flag1 = true;
                                d1 = Math.max(d2 - axisalignedbb.minY, d1);
                                if (flag) {
                                    Vec3 vec3d1 = fluid.getFlow(this.level, blockposition_mutableblockposition);

                                    if (d1 < 0.4D) {
                                        vec3d1 = vec3d1.scale(d1);
                                    }

                                    vec3d = vec3d.add(vec3d1);
                                    ++k1;
                                }
                                // CraftBukkit start - store last lava contact location
                                if (tag == FluidTags.LAVA) {
                                    this.lastLavaContact = blockposition_mutableblockposition.immutable();
                                }
                                // CraftBukkit end
                            }
                        }
                    }
                }
            }

            if (vec3d.length() > 0.0D) {
                if (k1 > 0) {
                    vec3d = vec3d.scale(1.0D / (double) k1);
                }

                if (!(this instanceof Player)) {
                    vec3d = vec3d.normalize();
                }

                Vec3 vec3d2 = this.getDeltaMovement();

                vec3d = vec3d.scale(speed * 1.0D);
                double d3 = 0.003D;

                if (Math.abs(vec3d2.x) < 0.003D && Math.abs(vec3d2.z) < 0.003D && vec3d.length() < 0.0045000000000000005D) {
                    vec3d = vec3d.normalize().scale(0.0045000000000000005D);
                }

                this.setDeltaMovement(this.getDeltaMovement().add(vec3d));
            }

            this.fluidHeight.put(tag, d1);
            return flag1;
        }
    }

    public boolean touchingUnloadedChunk() {
        AABB axisalignedbb = this.getBoundingBox().inflate(1.0D);
        int i = Mth.floor(axisalignedbb.minX);
        int j = Mth.ceil(axisalignedbb.maxX);
        int k = Mth.floor(axisalignedbb.minZ);
        int l = Mth.ceil(axisalignedbb.maxZ);

        return !this.level.hasChunksAt(i, k, j, l);
    }

    public double getFluidHeight(TagKey<Fluid> fluid) {
        return this.fluidHeight.getDouble(fluid);
    }

    public double getFluidJumpThreshold() {
        return (double) this.getEyeHeight() < 0.4D ? 0.0D : 0.4D;
    }

    public final float getBbWidth() {
        return this.dimensions.width;
    }

    public final float getBbHeight() {
        return this.dimensions.height;
    }

    public abstract Packet<?> getAddEntityPacket();

    public EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
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
        synchronized (this.posLock) { // Paper
        this.deltaMovement = velocity;
        } // Paper
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
        return this.position.x + (double) this.getBbWidth() * widthScale;
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
        return this.position.y + (double) this.getBbHeight() * heightScale;
    }

    public double getRandomY() {
        return this.getY(this.random.nextDouble());
    }

    public double getEyeY() {
        return this.position.y + (double) this.eyeHeight;
    }

    public final int getBlockZ() {
        return this.blockPosition.getZ();
    }

    public final double getZ() {
        return this.position.z;
    }

    public double getZ(double widthScale) {
        return this.position.z + (double) this.getBbWidth() * widthScale;
    }

    public double getRandomZ(double widthScale) {
        return this.getZ((2.0D * this.random.nextDouble() - 1.0D) * widthScale);
    }

    // Paper start - block invalid positions
    public static boolean checkPosition(Entity entity, double newX, double newY, double newZ) {
        if (Double.isFinite(newX) && Double.isFinite(newY) && Double.isFinite(newZ)) {
            return true;
        }

        String entityInfo = null;
        try {
            entityInfo = entity.toString();
        } catch (Exception ex) {
            entityInfo = "[Entity info unavailable] ";
        }
        LOGGER.error("New entity position is invalid! Tried to set invalid position (" + newX + "," + newY + "," + newZ + ") for entity " + entity.getClass().getName() + " located at " + entity.position + ", entity info: " + entityInfo, new Throwable());
        return false;
    }
    // Paper end - block invalid positions

    public final void setPosRaw(double x, double y, double z) {
        // Paper start
        this.setPosRaw(x, y, z, false);
    }
    public final void setPosRaw(double x, double y, double z, boolean forceBoundingBoxUpdate) {
        // Paper start - block invalid positions
        if (!checkPosition(this, x, y, z)) {
            return;
        }
        // Paper end - block invalid positions
        // Paper end
        // Paper start - fix MC-4
        if (this instanceof ItemEntity) {
            if (com.destroystokyo.paper.PaperConfig.fixEntityPositionDesync) {
                // encode/decode from PacketPlayOutEntity
                x = Mth.lfloor(x * 4096.0D) * (1 / 4096.0D);
                y = Mth.lfloor(y * 4096.0D) * (1 / 4096.0D);
                z = Mth.lfloor(z * 4096.0D) * (1 / 4096.0D);
            }
        }
        // Paper end - fix MC-4
        if (this.position.x != x || this.position.y != y || this.position.z != z) {
            synchronized (this.posLock) { // Paper
            this.position = new Vec3(x, y, z);
            } // Paper
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
            GameEventListenerRegistrar gameeventlistenerregistrar = this.getGameEventListenerRegistrar();

            if (gameeventlistenerregistrar != null) {
                gameeventlistenerregistrar.onListenerMove(this.level);
            }
        }

        // Paper start - never allow AABB to become desynced from position
        // hanging has its own special logic
        if (!(this instanceof net.minecraft.world.entity.decoration.HangingEntity) && (forceBoundingBoxUpdate || this.position.x != x || this.position.y != y || this.position.z != z)) {
            this.setBoundingBox(this.makeBoundingBox());
        }
        // Paper end
    }

    public void checkDespawn() {}

    public Vec3 getRopeHoldPosition(float delta) {
        return this.getPosition(delta).add(0.0D, (double) this.eyeHeight * 0.7D, 0.0D);
    }

    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        int i = packet.getId();
        double d0 = packet.getX();
        double d1 = packet.getY();
        double d2 = packet.getZ();

        this.setPacketCoordinates(d0, d1, d2);
        this.moveTo(d0, d1, d2);
        this.setXRot((float) (packet.getxRot() * 360) / 256.0F);
        this.setYRot((float) (packet.getyRot() * 360) / 256.0F);
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
        return this.removalReason != null && !this.removalReason.shouldSave() ? false : (this.isPassenger() ? false : !this.isVehicle() || !this.hasExactlyOnePlayerPassenger());
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

    public static enum RemovalReason {

        KILLED(true, false), DISCARDED(true, false), UNLOADED_TO_CHUNK(false, true), UNLOADED_WITH_PLAYER(false, false), CHANGED_DIMENSION(false, false);

        private final boolean destroy;
        private final boolean save;

        private RemovalReason(boolean flag, boolean flag1) {
            this.destroy = flag;
            this.save = flag1;
        }

        public boolean shouldDestroy() {
            return this.destroy;
        }

        public boolean shouldSave() {
            return this.save;
        }
    }

    public static enum MovementEmission {

        NONE(false, false), SOUNDS(true, false), EVENTS(false, true), ALL(true, true);

        final boolean sounds;
        final boolean events;

        private MovementEmission(boolean flag, boolean flag1) {
            this.sounds = flag;
            this.events = flag1;
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

    @FunctionalInterface
    public interface MoveFunction {

        void accept(Entity entity, double x, double y, double z);
    }

    // Paper start
    public static int nextEntityId() {
        return ENTITY_COUNTER.incrementAndGet();
    }

    public boolean isTicking() {
        return ((ServerChunkCache) level.getChunkSource()).isPositionTicking(this);
    }
    // Paper end
}
