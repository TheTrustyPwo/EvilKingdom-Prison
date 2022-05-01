package net.minecraft.world.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;

public class AreaEffectCloud extends Entity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TIME_BETWEEN_APPLICATIONS = 5;
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ParticleOptions> DATA_PARTICLE = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.PARTICLE);
    private static final float MAX_RADIUS = 32.0F;
    private Potion potion = Potions.EMPTY;
    public List<MobEffectInstance> effects = Lists.newArrayList();
    private final Map<Entity, Integer> victims = Maps.newHashMap();
    private int duration = 600;
    public int waitTime = 20;
    public int reapplicationDelay = 20;
    private boolean fixedColor;
    public int durationOnUse;
    public float radiusOnUse;
    public float radiusPerTick;
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;

    public AreaEffectCloud(EntityType<? extends AreaEffectCloud> type, Level world) {
        super(type, world);
        this.noPhysics = true;
        this.setRadius(3.0F);
    }

    public AreaEffectCloud(Level world, double x, double y, double z) {
        this(EntityType.AREA_EFFECT_CLOUD, world);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(DATA_COLOR, 0);
        this.getEntityData().define(DATA_RADIUS, 0.5F);
        this.getEntityData().define(DATA_WAITING, false);
        this.getEntityData().define(DATA_PARTICLE, ParticleTypes.ENTITY_EFFECT);
    }

    public void setRadius(float radius) {
        if (!this.level.isClientSide) {
            this.getEntityData().set(DATA_RADIUS, Mth.clamp(radius, 0.0F, 32.0F));
        }

    }

    @Override
    public void refreshDimensions() {
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        super.refreshDimensions();
        this.setPos(d, e, f);
    }

    public float getRadius() {
        return this.getEntityData().get(DATA_RADIUS);
    }

    public void setPotion(Potion potion) {
        this.potion = potion;
        if (!this.fixedColor) {
            this.updateColor();
        }

    }

    private void updateColor() {
        if (this.potion == Potions.EMPTY && this.effects.isEmpty()) {
            this.getEntityData().set(DATA_COLOR, 0);
        } else {
            this.getEntityData().set(DATA_COLOR, PotionUtils.getColor(PotionUtils.getAllEffects(this.potion, this.effects)));
        }

    }

    public void addEffect(MobEffectInstance effect) {
        this.effects.add(effect);
        if (!this.fixedColor) {
            this.updateColor();
        }

    }

    public int getColor() {
        return this.getEntityData().get(DATA_COLOR);
    }

    public void setFixedColor(int rgb) {
        this.fixedColor = true;
        this.getEntityData().set(DATA_COLOR, rgb);
    }

    public ParticleOptions getParticle() {
        return this.getEntityData().get(DATA_PARTICLE);
    }

    public void setParticle(ParticleOptions particle) {
        this.getEntityData().set(DATA_PARTICLE, particle);
    }

    protected void setWaiting(boolean waiting) {
        this.getEntityData().set(DATA_WAITING, waiting);
    }

    public boolean isWaiting() {
        return this.getEntityData().get(DATA_WAITING);
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public void tick() {
        super.tick();
        boolean bl = this.isWaiting();
        float f = this.getRadius();
        if (this.level.isClientSide) {
            if (bl && this.random.nextBoolean()) {
                return;
            }

            ParticleOptions particleOptions = this.getParticle();
            int i;
            float g;
            if (bl) {
                i = 2;
                g = 0.2F;
            } else {
                i = Mth.ceil((float)Math.PI * f * f);
                g = f;
            }

            for(int k = 0; k < i; ++k) {
                float l = this.random.nextFloat() * ((float)Math.PI * 2F);
                float m = Mth.sqrt(this.random.nextFloat()) * g;
                double d = this.getX() + (double)(Mth.cos(l) * m);
                double e = this.getY();
                double n = this.getZ() + (double)(Mth.sin(l) * m);
                double s;
                double t;
                double u;
                if (particleOptions.getType() != ParticleTypes.ENTITY_EFFECT) {
                    if (bl) {
                        s = 0.0D;
                        t = 0.0D;
                        u = 0.0D;
                    } else {
                        s = (0.5D - this.random.nextDouble()) * 0.15D;
                        t = (double)0.01F;
                        u = (0.5D - this.random.nextDouble()) * 0.15D;
                    }
                } else {
                    int o = bl && this.random.nextBoolean() ? 16777215 : this.getColor();
                    s = (double)((float)(o >> 16 & 255) / 255.0F);
                    t = (double)((float)(o >> 8 & 255) / 255.0F);
                    u = (double)((float)(o & 255) / 255.0F);
                }

                this.level.addAlwaysVisibleParticle(particleOptions, d, e, n, s, t, u);
            }
        } else {
            if (this.tickCount >= this.waitTime + this.duration) {
                this.discard();
                return;
            }

            boolean bl2 = this.tickCount < this.waitTime;
            if (bl != bl2) {
                this.setWaiting(bl2);
            }

            if (bl2) {
                return;
            }

            if (this.radiusPerTick != 0.0F) {
                f += this.radiusPerTick;
                if (f < 0.5F) {
                    this.discard();
                    return;
                }

                this.setRadius(f);
            }

            if (this.tickCount % 5 == 0) {
                this.victims.entrySet().removeIf((entry) -> {
                    return this.tickCount >= entry.getValue();
                });
                List<MobEffectInstance> list = Lists.newArrayList();

                for(MobEffectInstance mobEffectInstance : this.potion.getEffects()) {
                    list.add(new MobEffectInstance(mobEffectInstance.getEffect(), mobEffectInstance.getDuration() / 4, mobEffectInstance.getAmplifier(), mobEffectInstance.isAmbient(), mobEffectInstance.isVisible()));
                }

                list.addAll(this.effects);
                if (list.isEmpty()) {
                    this.victims.clear();
                } else {
                    List<LivingEntity> list2 = this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
                    if (!list2.isEmpty()) {
                        for(LivingEntity livingEntity : list2) {
                            if (!this.victims.containsKey(livingEntity) && livingEntity.isAffectedByPotions()) {
                                double y = livingEntity.getX() - this.getX();
                                double z = livingEntity.getZ() - this.getZ();
                                double aa = y * y + z * z;
                                if (aa <= (double)(f * f)) {
                                    this.victims.put(livingEntity, this.tickCount + this.reapplicationDelay);

                                    for(MobEffectInstance mobEffectInstance2 : list) {
                                        if (mobEffectInstance2.getEffect().isInstantenous()) {
                                            mobEffectInstance2.getEffect().applyInstantenousEffect(this, this.getOwner(), livingEntity, mobEffectInstance2.getAmplifier(), 0.5D);
                                        } else {
                                            livingEntity.addEffect(new MobEffectInstance(mobEffectInstance2), this);
                                        }
                                    }

                                    if (this.radiusOnUse != 0.0F) {
                                        f += this.radiusOnUse;
                                        if (f < 0.5F) {
                                            this.discard();
                                            return;
                                        }

                                        this.setRadius(f);
                                    }

                                    if (this.durationOnUse != 0) {
                                        this.duration += this.durationOnUse;
                                        if (this.duration <= 0) {
                                            this.discard();
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    public float getRadiusOnUse() {
        return this.radiusOnUse;
    }

    public void setRadiusOnUse(float radiusOnUse) {
        this.radiusOnUse = radiusOnUse;
    }

    public float getRadiusPerTick() {
        return this.radiusPerTick;
    }

    public void setRadiusPerTick(float radiusGrowth) {
        this.radiusPerTick = radiusGrowth;
    }

    public int getDurationOnUse() {
        return this.durationOnUse;
    }

    public void setDurationOnUse(int durationOnUse) {
        this.durationOnUse = durationOnUse;
    }

    public int getWaitTime() {
        return this.waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner == null ? null : owner.getUUID();
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level instanceof ServerLevel) {
            Entity entity = ((ServerLevel)this.level).getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity)entity;
            }
        }

        return this.owner;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.tickCount = nbt.getInt("Age");
        this.duration = nbt.getInt("Duration");
        this.waitTime = nbt.getInt("WaitTime");
        this.reapplicationDelay = nbt.getInt("ReapplicationDelay");
        this.durationOnUse = nbt.getInt("DurationOnUse");
        this.radiusOnUse = nbt.getFloat("RadiusOnUse");
        this.radiusPerTick = nbt.getFloat("RadiusPerTick");
        this.setRadius(nbt.getFloat("Radius"));
        if (nbt.hasUUID("Owner")) {
            this.ownerUUID = nbt.getUUID("Owner");
        }

        if (nbt.contains("Particle", 8)) {
            try {
                this.setParticle(ParticleArgument.readParticle(new StringReader(nbt.getString("Particle"))));
            } catch (CommandSyntaxException var5) {
                LOGGER.warn("Couldn't load custom particle {}", nbt.getString("Particle"), var5);
            }
        }

        if (nbt.contains("Color", 99)) {
            this.setFixedColor(nbt.getInt("Color"));
        }

        if (nbt.contains("Potion", 8)) {
            this.setPotion(PotionUtils.getPotion(nbt));
        }

        if (nbt.contains("Effects", 9)) {
            ListTag listTag = nbt.getList("Effects", 10);
            this.effects.clear();

            for(int i = 0; i < listTag.size(); ++i) {
                MobEffectInstance mobEffectInstance = MobEffectInstance.load(listTag.getCompound(i));
                if (mobEffectInstance != null) {
                    this.addEffect(mobEffectInstance);
                }
            }
        }

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("Age", this.tickCount);
        nbt.putInt("Duration", this.duration);
        nbt.putInt("WaitTime", this.waitTime);
        nbt.putInt("ReapplicationDelay", this.reapplicationDelay);
        nbt.putInt("DurationOnUse", this.durationOnUse);
        nbt.putFloat("RadiusOnUse", this.radiusOnUse);
        nbt.putFloat("RadiusPerTick", this.radiusPerTick);
        nbt.putFloat("Radius", this.getRadius());
        nbt.putString("Particle", this.getParticle().writeToString());
        if (this.ownerUUID != null) {
            nbt.putUUID("Owner", this.ownerUUID);
        }

        if (this.fixedColor) {
            nbt.putInt("Color", this.getColor());
        }

        if (this.potion != Potions.EMPTY) {
            nbt.putString("Potion", Registry.POTION.getKey(this.potion).toString());
        }

        if (!this.effects.isEmpty()) {
            ListTag listTag = new ListTag();

            for(MobEffectInstance mobEffectInstance : this.effects) {
                listTag.add(mobEffectInstance.save(new CompoundTag()));
            }

            nbt.put("Effects", listTag);
        }

    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (DATA_RADIUS.equals(data)) {
            this.refreshDimensions();
        }

        super.onSyncedDataUpdated(data);
    }

    public Potion getPotion() {
        return this.potion;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(this.getRadius() * 2.0F, 0.5F);
    }
}
