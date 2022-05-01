package net.minecraft.world.entity.item;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.ExplosionPrimeEvent; // CraftBukkit

public class PrimedTnt extends Entity {

    private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(PrimedTnt.class, EntityDataSerializers.INT);
    private static final int DEFAULT_FUSE_TIME = 80;
    @Nullable
    public LivingEntity owner;
    public float yield = 4; // CraftBukkit - add field
    public boolean isIncendiary = false; // CraftBukkit - add field

    public PrimedTnt(EntityType<? extends PrimedTnt> type, Level world) {
        super(type, world);
        this.blocksBuilding = true;
    }

    public PrimedTnt(Level world, double x, double y, double z, @Nullable LivingEntity igniter) {
        this(EntityType.TNT, world);
        this.setPos(x, y, z);
        double d3 = world.random.nextDouble() * 6.2831854820251465D;

        this.setDeltaMovement(-Math.sin(d3) * 0.02D, 0.20000000298023224D, -Math.cos(d3) * 0.02D);
        this.setFuse(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = igniter;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PrimedTnt.DATA_FUSE_ID, 80);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public void tick() {
        if (level.spigotConfig.currentPrimedTnt++ > level.spigotConfig.maxTntTicksPerTick) { return; } // Spigot
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        // Paper start - Configurable TNT entity height nerf
        if (this.level.paperConfig.entityTNTHeightNerf != 0 && this.getY() > this.level.paperConfig.entityTNTHeightNerf) {
            this.discard();
            return;
        }
        // Paper end
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
        if (this.onGround) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
        }

        int i = this.getFuse() - 1;

        this.setFuse(i);
        if (i <= 0) {
            // CraftBukkit start - Need to reverse the order of the explosion and the entity death so we have a location for the event
            // this.discard();
            if (!this.level.isClientSide) {
                this.explode();
            }
            this.discard();
            // CraftBukkit end
        } else {
            this.updateInWaterStateAndDoFluidPushing();
            if (this.level.isClientSide) {
                this.level.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }

        // Paper start - Optional prevent TNT from moving in water
        if (!this.isRemoved() && this.wasTouchingWater && this.level.paperConfig.preventTntFromMovingInWater) {
            /*
             * Author: Jedediah Smith <jedediah@silencegreys.com>
             */
            // Send position and velocity updates to nearby players on every tick while the TNT is in water.
            // This does pretty well at keeping their clients in sync with the server.
            net.minecraft.server.level.ChunkMap.TrackedEntity ete = ((net.minecraft.server.level.ServerLevel)this.level).getChunkSource().chunkMap.entityMap.get(this.getId());
            if (ete != null) {
                net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket velocityPacket = new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(this);
                net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket positionPacket = new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(this);

                ete.seenBy.stream()
                    .filter(viewer -> (viewer.getPlayer().getX() - this.getX()) * (viewer.getPlayer().getY() - this.getY()) * (viewer.getPlayer().getZ() - this.getZ()) < 16 * 16)
                    .forEach(viewer -> {
                        viewer.send(velocityPacket);
                        viewer.send(positionPacket);
                    });
            }
        }
        // Paper end
    }

    private void explode() {
        // CraftBukkit start
        // float f = 4.0F;

        ExplosionPrimeEvent event = new ExplosionPrimeEvent((org.bukkit.entity.Explosive) this.getBukkitEntity());
        this.level.getCraftServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            this.level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(), event.getRadius(), event.getFire(), Explosion.BlockInteraction.BREAK);
        }
        // CraftBukkit end
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putShort("Fuse", (short) this.getFuse());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.setFuse(nbt.getShort("Fuse"));
        // Paper start - Try and load origin location from the old NBT tags for backwards compatibility
        if (nbt.contains("SourceLoc_x")) {
            int srcX = nbt.getInt("SourceLoc_x");
            int srcY = nbt.getInt("SourceLoc_y");
            int srcZ = nbt.getInt("SourceLoc_z");
            this.setOrigin(new org.bukkit.Location(level.getWorld(), srcX, srcY, srcZ));
        }
        // Paper end
    }

    @Nullable
    public LivingEntity getOwner() {
        return this.owner;
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.15F;
    }

    public void setFuse(int fuse) {
        this.entityData.set(PrimedTnt.DATA_FUSE_ID, fuse);
    }

    public int getFuse() {
        return (Integer) this.entityData.get(PrimedTnt.DATA_FUSE_ID);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    // Paper start - Optional prevent TNT from moving in water
    @Override
    public boolean isPushedByFluid() {
        return !level.paperConfig.preventTntFromMovingInWater && super.isPushedByFluid();
    }
    // Paper end
}
