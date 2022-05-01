package net.minecraft.server.level;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ServerEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TOLERANCE_LEVEL_ROTATION = 1;
    private final ServerLevel level;
    private final Entity entity;
    private final int updateInterval;
    private final boolean trackDelta;
    private final Consumer<Packet<?>> broadcast;
    private long xp;
    private long yp;
    private long zp;
    private int yRotp;
    private int xRotp;
    private int yHeadRotp;
    private Vec3 ap = Vec3.ZERO;
    private int tickCount;
    private int teleportDelay;
    private List<Entity> lastPassengers = Collections.emptyList();
    private boolean wasRiding;
    private boolean wasOnGround;

    public ServerEntity(ServerLevel world, Entity entity, int tickInterval, boolean alwaysUpdateVelocity, Consumer<Packet<?>> receiver) {
        this.level = world;
        this.broadcast = receiver;
        this.entity = entity;
        this.updateInterval = tickInterval;
        this.trackDelta = alwaysUpdateVelocity;
        this.updateSentPos();
        this.yRotp = Mth.floor(entity.getYRot() * 256.0F / 360.0F);
        this.xRotp = Mth.floor(entity.getXRot() * 256.0F / 360.0F);
        this.yHeadRotp = Mth.floor(entity.getYHeadRot() * 256.0F / 360.0F);
        this.wasOnGround = entity.isOnGround();
    }

    public void sendChanges() {
        List<Entity> list = this.entity.getPassengers();
        if (!list.equals(this.lastPassengers)) {
            this.lastPassengers = list;
            this.broadcast.accept(new ClientboundSetPassengersPacket(this.entity));
        }

        if (this.entity instanceof ItemFrame && this.tickCount % 10 == 0) {
            ItemFrame itemFrame = (ItemFrame)this.entity;
            ItemStack itemStack = itemFrame.getItem();
            if (itemStack.getItem() instanceof MapItem) {
                Integer integer = MapItem.getMapId(itemStack);
                MapItemSavedData mapItemSavedData = MapItem.getSavedData(integer, this.level);
                if (mapItemSavedData != null) {
                    for(ServerPlayer serverPlayer : this.level.players()) {
                        mapItemSavedData.tickCarriedBy(serverPlayer, itemStack);
                        Packet<?> packet = mapItemSavedData.getUpdatePacket(integer, serverPlayer);
                        if (packet != null) {
                            serverPlayer.connection.send(packet);
                        }
                    }
                }
            }

            this.sendDirtyEntityData();
        }

        if (this.tickCount % this.updateInterval == 0 || this.entity.hasImpulse || this.entity.getEntityData().isDirty()) {
            if (this.entity.isPassenger()) {
                int i = Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
                int j = Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
                boolean bl = Math.abs(i - this.yRotp) >= 1 || Math.abs(j - this.xRotp) >= 1;
                if (bl) {
                    this.broadcast.accept(new ClientboundMoveEntityPacket.Rot(this.entity.getId(), (byte)i, (byte)j, this.entity.isOnGround()));
                    this.yRotp = i;
                    this.xRotp = j;
                }

                this.updateSentPos();
                this.sendDirtyEntityData();
                this.wasRiding = true;
            } else {
                ++this.teleportDelay;
                int k = Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
                int l = Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
                Vec3 vec3 = this.entity.position().subtract(ClientboundMoveEntityPacket.packetToEntity(this.xp, this.yp, this.zp));
                boolean bl2 = vec3.lengthSqr() >= (double)7.6293945E-6F;
                Packet<?> packet2 = null;
                boolean bl3 = bl2 || this.tickCount % 60 == 0;
                boolean bl4 = Math.abs(k - this.yRotp) >= 1 || Math.abs(l - this.xRotp) >= 1;
                if (this.tickCount > 0 || this.entity instanceof AbstractArrow) {
                    long m = ClientboundMoveEntityPacket.entityToPacket(vec3.x);
                    long n = ClientboundMoveEntityPacket.entityToPacket(vec3.y);
                    long o = ClientboundMoveEntityPacket.entityToPacket(vec3.z);
                    boolean bl5 = m < -32768L || m > 32767L || n < -32768L || n > 32767L || o < -32768L || o > 32767L;
                    if (!bl5 && this.teleportDelay <= 400 && !this.wasRiding && this.wasOnGround == this.entity.isOnGround()) {
                        if ((!bl3 || !bl4) && !(this.entity instanceof AbstractArrow)) {
                            if (bl3) {
                                packet2 = new ClientboundMoveEntityPacket.Pos(this.entity.getId(), (short)((int)m), (short)((int)n), (short)((int)o), this.entity.isOnGround());
                            } else if (bl4) {
                                packet2 = new ClientboundMoveEntityPacket.Rot(this.entity.getId(), (byte)k, (byte)l, this.entity.isOnGround());
                            }
                        } else {
                            packet2 = new ClientboundMoveEntityPacket.PosRot(this.entity.getId(), (short)((int)m), (short)((int)n), (short)((int)o), (byte)k, (byte)l, this.entity.isOnGround());
                        }
                    } else {
                        this.wasOnGround = this.entity.isOnGround();
                        this.teleportDelay = 0;
                        packet2 = new ClientboundTeleportEntityPacket(this.entity);
                    }
                }

                if ((this.trackDelta || this.entity.hasImpulse || this.entity instanceof LivingEntity && ((LivingEntity)this.entity).isFallFlying()) && this.tickCount > 0) {
                    Vec3 vec32 = this.entity.getDeltaMovement();
                    double d = vec32.distanceToSqr(this.ap);
                    if (d > 1.0E-7D || d > 0.0D && vec32.lengthSqr() == 0.0D) {
                        this.ap = vec32;
                        this.broadcast.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.ap));
                    }
                }

                if (packet2 != null) {
                    this.broadcast.accept(packet2);
                }

                this.sendDirtyEntityData();
                if (bl3) {
                    this.updateSentPos();
                }

                if (bl4) {
                    this.yRotp = k;
                    this.xRotp = l;
                }

                this.wasRiding = false;
            }

            int p = Mth.floor(this.entity.getYHeadRot() * 256.0F / 360.0F);
            if (Math.abs(p - this.yHeadRotp) >= 1) {
                this.broadcast.accept(new ClientboundRotateHeadPacket(this.entity, (byte)p));
                this.yHeadRotp = p;
            }

            this.entity.hasImpulse = false;
        }

        ++this.tickCount;
        if (this.entity.hurtMarked) {
            this.broadcastAndSend(new ClientboundSetEntityMotionPacket(this.entity));
            this.entity.hurtMarked = false;
        }

    }

    public void removePairing(ServerPlayer player) {
        this.entity.stopSeenByPlayer(player);
        player.connection.send(new ClientboundRemoveEntitiesPacket(this.entity.getId()));
    }

    public void addPairing(ServerPlayer player) {
        this.sendPairingData(player.connection::send);
        this.entity.startSeenByPlayer(player);
    }

    public void sendPairingData(Consumer<Packet<?>> sender) {
        if (this.entity.isRemoved()) {
            LOGGER.warn("Fetching packet for removed entity {}", (Object)this.entity);
        }

        Packet<?> packet = this.entity.getAddEntityPacket();
        this.yHeadRotp = Mth.floor(this.entity.getYHeadRot() * 256.0F / 360.0F);
        sender.accept(packet);
        if (!this.entity.getEntityData().isEmpty()) {
            sender.accept(new ClientboundSetEntityDataPacket(this.entity.getId(), this.entity.getEntityData(), true));
        }

        boolean bl = this.trackDelta;
        if (this.entity instanceof LivingEntity) {
            Collection<AttributeInstance> collection = ((LivingEntity)this.entity).getAttributes().getSyncableAttributes();
            if (!collection.isEmpty()) {
                sender.accept(new ClientboundUpdateAttributesPacket(this.entity.getId(), collection));
            }

            if (((LivingEntity)this.entity).isFallFlying()) {
                bl = true;
            }
        }

        this.ap = this.entity.getDeltaMovement();
        if (bl && !(packet instanceof ClientboundAddMobPacket)) {
            sender.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.ap));
        }

        if (this.entity instanceof LivingEntity) {
            List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayList();

            for(EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                ItemStack itemStack = ((LivingEntity)this.entity).getItemBySlot(equipmentSlot);
                if (!itemStack.isEmpty()) {
                    list.add(Pair.of(equipmentSlot, itemStack.copy()));
                }
            }

            if (!list.isEmpty()) {
                sender.accept(new ClientboundSetEquipmentPacket(this.entity.getId(), list));
            }
        }

        if (this.entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)this.entity;

            for(MobEffectInstance mobEffectInstance : livingEntity.getActiveEffects()) {
                sender.accept(new ClientboundUpdateMobEffectPacket(this.entity.getId(), mobEffectInstance));
            }
        }

        if (!this.entity.getPassengers().isEmpty()) {
            sender.accept(new ClientboundSetPassengersPacket(this.entity));
        }

        if (this.entity.isPassenger()) {
            sender.accept(new ClientboundSetPassengersPacket(this.entity.getVehicle()));
        }

        if (this.entity instanceof Mob) {
            Mob mob = (Mob)this.entity;
            if (mob.isLeashed()) {
                sender.accept(new ClientboundSetEntityLinkPacket(mob, mob.getLeashHolder()));
            }
        }

    }

    private void sendDirtyEntityData() {
        SynchedEntityData synchedEntityData = this.entity.getEntityData();
        if (synchedEntityData.isDirty()) {
            this.broadcastAndSend(new ClientboundSetEntityDataPacket(this.entity.getId(), synchedEntityData, false));
        }

        if (this.entity instanceof LivingEntity) {
            Set<AttributeInstance> set = ((LivingEntity)this.entity).getAttributes().getDirtyAttributes();
            if (!set.isEmpty()) {
                this.broadcastAndSend(new ClientboundUpdateAttributesPacket(this.entity.getId(), set));
            }

            set.clear();
        }

    }

    private void updateSentPos() {
        this.xp = ClientboundMoveEntityPacket.entityToPacket(this.entity.getX());
        this.yp = ClientboundMoveEntityPacket.entityToPacket(this.entity.getY());
        this.zp = ClientboundMoveEntityPacket.entityToPacket(this.entity.getZ());
    }

    public Vec3 sentPos() {
        return ClientboundMoveEntityPacket.packetToEntity(this.xp, this.yp, this.zp);
    }

    private void broadcastAndSend(Packet<?> packet) {
        this.broadcast.accept(packet);
        if (this.entity instanceof ServerPlayer) {
            ((ServerPlayer)this.entity).connection.send(packet);
        }

    }
}
