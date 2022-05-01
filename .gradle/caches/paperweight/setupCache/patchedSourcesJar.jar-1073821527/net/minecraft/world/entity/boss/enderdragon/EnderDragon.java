package net.minecraft.world.entity.boss.enderdragon;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class EnderDragon extends Mob implements Enemy {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(EnderDragon.class, EntityDataSerializers.INT);
    private static final TargetingConditions CRYSTAL_DESTROY_TARGETING = TargetingConditions.forCombat().range(64.0D);
    private static final int GROWL_INTERVAL_MIN = 200;
    private static final int GROWL_INTERVAL_MAX = 400;
    private static final float SITTING_ALLOWED_DAMAGE_PERCENTAGE = 0.25F;
    private static final String DRAGON_DEATH_TIME_KEY = "DragonDeathTime";
    private static final String DRAGON_PHASE_KEY = "DragonPhase";
    public final double[][] positions = new double[64][3];
    public int posPointer = -1;
    public final EnderDragonPart[] subEntities;
    public final EnderDragonPart head;
    private final EnderDragonPart neck;
    private final EnderDragonPart body;
    private final EnderDragonPart tail1;
    private final EnderDragonPart tail2;
    private final EnderDragonPart tail3;
    private final EnderDragonPart wing1;
    private final EnderDragonPart wing2;
    public float oFlapTime;
    public float flapTime;
    public boolean inWall;
    public int dragonDeathTime;
    public float yRotA;
    @Nullable
    public EndCrystal nearestCrystal;
    @Nullable
    private final EndDragonFight dragonFight;
    private final EnderDragonPhaseManager phaseManager;
    private int growlTime = 100;
    private float sittingDamageReceived;
    private final Node[] nodes = new Node[24];
    private final int[] nodeAdjacency = new int[24];
    private final BinaryHeap openSet = new BinaryHeap();

    public EnderDragon(EntityType<? extends EnderDragon> entityType, Level world) {
        super(EntityType.ENDER_DRAGON, world);
        this.head = new EnderDragonPart(this, "head", 1.0F, 1.0F);
        this.neck = new EnderDragonPart(this, "neck", 3.0F, 3.0F);
        this.body = new EnderDragonPart(this, "body", 5.0F, 3.0F);
        this.tail1 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
        this.tail2 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
        this.tail3 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
        this.wing1 = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
        this.wing2 = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
        this.subEntities = new EnderDragonPart[]{this.head, this.neck, this.body, this.tail1, this.tail2, this.tail3, this.wing1, this.wing2};
        this.setHealth(this.getMaxHealth());
        this.noPhysics = true;
        this.noCulling = true;
        if (world instanceof ServerLevel) {
            this.dragonFight = ((ServerLevel)world).dragonFight();
        } else {
            this.dragonFight = null;
        }

        this.phaseManager = new EnderDragonPhaseManager(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 200.0D);
    }

    @Override
    public boolean isFlapping() {
        float f = Mth.cos(this.flapTime * ((float)Math.PI * 2F));
        float g = Mth.cos(this.oFlapTime * ((float)Math.PI * 2F));
        return g <= -0.3F && f >= -0.3F;
    }

    @Override
    public void onFlap() {
        if (this.level.isClientSide && !this.isSilent()) {
            this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, this.getSoundSource(), 5.0F, 0.8F + this.random.nextFloat() * 0.3F, false);
        }

    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(DATA_PHASE, EnderDragonPhase.HOVERING.getId());
    }

    public double[] getLatencyPos(int segmentNumber, float tickDelta) {
        if (this.isDeadOrDying()) {
            tickDelta = 0.0F;
        }

        tickDelta = 1.0F - tickDelta;
        int i = this.posPointer - segmentNumber & 63;
        int j = this.posPointer - segmentNumber - 1 & 63;
        double[] ds = new double[3];
        double d = this.positions[i][0];
        double e = Mth.wrapDegrees(this.positions[j][0] - d);
        ds[0] = d + e * (double)tickDelta;
        d = this.positions[i][1];
        e = this.positions[j][1] - d;
        ds[1] = d + e * (double)tickDelta;
        ds[2] = Mth.lerp((double)tickDelta, this.positions[i][2], this.positions[j][2]);
        return ds;
    }

    @Override
    public void aiStep() {
        this.processFlappingMovement();
        if (this.level.isClientSide) {
            this.setHealth(this.getHealth());
            if (!this.isSilent() && !this.phaseManager.getCurrentPhase().isSitting() && --this.growlTime < 0) {
                this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, this.getSoundSource(), 2.5F, 0.8F + this.random.nextFloat() * 0.3F, false);
                this.growlTime = 200 + this.random.nextInt(200);
            }
        }

        this.oFlapTime = this.flapTime;
        if (this.isDeadOrDying()) {
            float f = (this.random.nextFloat() - 0.5F) * 8.0F;
            float g = (this.random.nextFloat() - 0.5F) * 4.0F;
            float h = (this.random.nextFloat() - 0.5F) * 8.0F;
            this.level.addParticle(ParticleTypes.EXPLOSION, this.getX() + (double)f, this.getY() + 2.0D + (double)g, this.getZ() + (double)h, 0.0D, 0.0D, 0.0D);
        } else {
            this.checkCrystals();
            Vec3 vec3 = this.getDeltaMovement();
            float i = 0.2F / ((float)vec3.horizontalDistance() * 10.0F + 1.0F);
            i *= (float)Math.pow(2.0D, vec3.y);
            if (this.phaseManager.getCurrentPhase().isSitting()) {
                this.flapTime += 0.1F;
            } else if (this.inWall) {
                this.flapTime += i * 0.5F;
            } else {
                this.flapTime += i;
            }

            this.setYRot(Mth.wrapDegrees(this.getYRot()));
            if (this.isNoAi()) {
                this.flapTime = 0.5F;
            } else {
                if (this.posPointer < 0) {
                    for(int j = 0; j < this.positions.length; ++j) {
                        this.positions[j][0] = (double)this.getYRot();
                        this.positions[j][1] = this.getY();
                    }
                }

                if (++this.posPointer == this.positions.length) {
                    this.posPointer = 0;
                }

                this.positions[this.posPointer][0] = (double)this.getYRot();
                this.positions[this.posPointer][1] = this.getY();
                if (this.level.isClientSide) {
                    if (this.lerpSteps > 0) {
                        double d = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
                        double e = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
                        double k = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
                        double l = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
                        this.setYRot(this.getYRot() + (float)l / (float)this.lerpSteps);
                        this.setXRot(this.getXRot() + (float)(this.lerpXRot - (double)this.getXRot()) / (float)this.lerpSteps);
                        --this.lerpSteps;
                        this.setPos(d, e, k);
                        this.setRot(this.getYRot(), this.getXRot());
                    }

                    this.phaseManager.getCurrentPhase().doClientTick();
                } else {
                    DragonPhaseInstance dragonPhaseInstance = this.phaseManager.getCurrentPhase();
                    dragonPhaseInstance.doServerTick();
                    if (this.phaseManager.getCurrentPhase() != dragonPhaseInstance) {
                        dragonPhaseInstance = this.phaseManager.getCurrentPhase();
                        dragonPhaseInstance.doServerTick();
                    }

                    Vec3 vec32 = dragonPhaseInstance.getFlyTargetLocation();
                    if (vec32 != null) {
                        double m = vec32.x - this.getX();
                        double n = vec32.y - this.getY();
                        double o = vec32.z - this.getZ();
                        double p = m * m + n * n + o * o;
                        float q = dragonPhaseInstance.getFlySpeed();
                        double r = Math.sqrt(m * m + o * o);
                        if (r > 0.0D) {
                            n = Mth.clamp(n / r, (double)(-q), (double)q);
                        }

                        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, n * 0.01D, 0.0D));
                        this.setYRot(Mth.wrapDegrees(this.getYRot()));
                        Vec3 vec33 = vec32.subtract(this.getX(), this.getY(), this.getZ()).normalize();
                        Vec3 vec34 = (new Vec3((double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), this.getDeltaMovement().y, (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))))).normalize();
                        float s = Math.max(((float)vec34.dot(vec33) + 0.5F) / 1.5F, 0.0F);
                        if (Math.abs(m) > (double)1.0E-5F || Math.abs(o) > (double)1.0E-5F) {
                            float t = Mth.clamp(Mth.wrapDegrees(180.0F - (float)Mth.atan2(m, o) * (180F / (float)Math.PI) - this.getYRot()), -50.0F, 50.0F);
                            this.yRotA *= 0.8F;
                            this.yRotA += t * dragonPhaseInstance.getTurnSpeed();
                            this.setYRot(this.getYRot() + this.yRotA * 0.1F);
                        }

                        float u = (float)(2.0D / (p + 1.0D));
                        float v = 0.06F;
                        this.moveRelative(0.06F * (s * u + (1.0F - u)), new Vec3(0.0D, 0.0D, -1.0D));
                        if (this.inWall) {
                            this.move(MoverType.SELF, this.getDeltaMovement().scale((double)0.8F));
                        } else {
                            this.move(MoverType.SELF, this.getDeltaMovement());
                        }

                        Vec3 vec35 = this.getDeltaMovement().normalize();
                        double w = 0.8D + 0.15D * (vec35.dot(vec34) + 1.0D) / 2.0D;
                        this.setDeltaMovement(this.getDeltaMovement().multiply(w, (double)0.91F, w));
                    }
                }

                this.yBodyRot = this.getYRot();
                Vec3[] vec3s = new Vec3[this.subEntities.length];

                for(int x = 0; x < this.subEntities.length; ++x) {
                    vec3s[x] = new Vec3(this.subEntities[x].getX(), this.subEntities[x].getY(), this.subEntities[x].getZ());
                }

                float y = (float)(this.getLatencyPos(5, 1.0F)[1] - this.getLatencyPos(10, 1.0F)[1]) * 10.0F * ((float)Math.PI / 180F);
                float z = Mth.cos(y);
                float aa = Mth.sin(y);
                float ab = this.getYRot() * ((float)Math.PI / 180F);
                float ac = Mth.sin(ab);
                float ad = Mth.cos(ab);
                this.tickPart(this.body, (double)(ac * 0.5F), 0.0D, (double)(-ad * 0.5F));
                this.tickPart(this.wing1, (double)(ad * 4.5F), 2.0D, (double)(ac * 4.5F));
                this.tickPart(this.wing2, (double)(ad * -4.5F), 2.0D, (double)(ac * -4.5F));
                if (!this.level.isClientSide && this.hurtTime == 0) {
                    this.knockBack(this.level.getEntities(this, this.wing1.getBoundingBox().inflate(4.0D, 2.0D, 4.0D).move(0.0D, -2.0D, 0.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
                    this.knockBack(this.level.getEntities(this, this.wing2.getBoundingBox().inflate(4.0D, 2.0D, 4.0D).move(0.0D, -2.0D, 0.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
                    this.hurt(this.level.getEntities(this, this.head.getBoundingBox().inflate(1.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
                    this.hurt(this.level.getEntities(this, this.neck.getBoundingBox().inflate(1.0D), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
                }

                float ae = Mth.sin(this.getYRot() * ((float)Math.PI / 180F) - this.yRotA * 0.01F);
                float af = Mth.cos(this.getYRot() * ((float)Math.PI / 180F) - this.yRotA * 0.01F);
                float ag = this.getHeadYOffset();
                this.tickPart(this.head, (double)(ae * 6.5F * z), (double)(ag + aa * 6.5F), (double)(-af * 6.5F * z));
                this.tickPart(this.neck, (double)(ae * 5.5F * z), (double)(ag + aa * 5.5F), (double)(-af * 5.5F * z));
                double[] ds = this.getLatencyPos(5, 1.0F);

                for(int ah = 0; ah < 3; ++ah) {
                    EnderDragonPart enderDragonPart = null;
                    if (ah == 0) {
                        enderDragonPart = this.tail1;
                    }

                    if (ah == 1) {
                        enderDragonPart = this.tail2;
                    }

                    if (ah == 2) {
                        enderDragonPart = this.tail3;
                    }

                    double[] es = this.getLatencyPos(12 + ah * 2, 1.0F);
                    float ai = this.getYRot() * ((float)Math.PI / 180F) + this.rotWrap(es[0] - ds[0]) * ((float)Math.PI / 180F);
                    float aj = Mth.sin(ai);
                    float ak = Mth.cos(ai);
                    float al = 1.5F;
                    float am = (float)(ah + 1) * 2.0F;
                    this.tickPart(enderDragonPart, (double)(-(ac * 1.5F + aj * am) * z), es[1] - ds[1] - (double)((am + 1.5F) * aa) + 1.5D, (double)((ad * 1.5F + ak * am) * z));
                }

                if (!this.level.isClientSide) {
                    this.inWall = this.checkWalls(this.head.getBoundingBox()) | this.checkWalls(this.neck.getBoundingBox()) | this.checkWalls(this.body.getBoundingBox());
                    if (this.dragonFight != null) {
                        this.dragonFight.updateDragon(this);
                    }
                }

                for(int an = 0; an < this.subEntities.length; ++an) {
                    this.subEntities[an].xo = vec3s[an].x;
                    this.subEntities[an].yo = vec3s[an].y;
                    this.subEntities[an].zo = vec3s[an].z;
                    this.subEntities[an].xOld = vec3s[an].x;
                    this.subEntities[an].yOld = vec3s[an].y;
                    this.subEntities[an].zOld = vec3s[an].z;
                }

            }
        }
    }

    private void tickPart(EnderDragonPart enderDragonPart, double dx, double dy, double dz) {
        enderDragonPart.setPos(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
    }

    private float getHeadYOffset() {
        if (this.phaseManager.getCurrentPhase().isSitting()) {
            return -1.0F;
        } else {
            double[] ds = this.getLatencyPos(5, 1.0F);
            double[] es = this.getLatencyPos(0, 1.0F);
            return (float)(ds[1] - es[1]);
        }
    }

    private void checkCrystals() {
        if (this.nearestCrystal != null) {
            if (this.nearestCrystal.isRemoved()) {
                this.nearestCrystal = null;
            } else if (this.tickCount % 10 == 0 && this.getHealth() < this.getMaxHealth()) {
                this.setHealth(this.getHealth() + 1.0F);
            }
        }

        if (this.random.nextInt(10) == 0) {
            List<EndCrystal> list = this.level.getEntitiesOfClass(EndCrystal.class, this.getBoundingBox().inflate(32.0D));
            EndCrystal endCrystal = null;
            double d = Double.MAX_VALUE;

            for(EndCrystal endCrystal2 : list) {
                double e = endCrystal2.distanceToSqr(this);
                if (e < d) {
                    d = e;
                    endCrystal = endCrystal2;
                }
            }

            this.nearestCrystal = endCrystal;
        }

    }

    private void knockBack(List<Entity> entities) {
        double d = (this.body.getBoundingBox().minX + this.body.getBoundingBox().maxX) / 2.0D;
        double e = (this.body.getBoundingBox().minZ + this.body.getBoundingBox().maxZ) / 2.0D;

        for(Entity entity : entities) {
            if (entity instanceof LivingEntity) {
                double f = entity.getX() - d;
                double g = entity.getZ() - e;
                double h = Math.max(f * f + g * g, 0.1D);
                entity.push(f / h * 4.0D, (double)0.2F, g / h * 4.0D);
                if (!this.phaseManager.getCurrentPhase().isSitting() && ((LivingEntity)entity).getLastHurtByMobTimestamp() < entity.tickCount - 2) {
                    entity.hurt(DamageSource.mobAttack(this), 5.0F);
                    this.doEnchantDamageEffects(this, entity);
                }
            }
        }

    }

    private void hurt(List<Entity> entities) {
        for(Entity entity : entities) {
            if (entity instanceof LivingEntity) {
                entity.hurt(DamageSource.mobAttack(this), 10.0F);
                this.doEnchantDamageEffects(this, entity);
            }
        }

    }

    private float rotWrap(double yawDegrees) {
        return (float)Mth.wrapDegrees(yawDegrees);
    }

    private boolean checkWalls(AABB box) {
        int i = Mth.floor(box.minX);
        int j = Mth.floor(box.minY);
        int k = Mth.floor(box.minZ);
        int l = Mth.floor(box.maxX);
        int m = Mth.floor(box.maxY);
        int n = Mth.floor(box.maxZ);
        boolean bl = false;
        boolean bl2 = false;

        for(int o = i; o <= l; ++o) {
            for(int p = j; p <= m; ++p) {
                for(int q = k; q <= n; ++q) {
                    BlockPos blockPos = new BlockPos(o, p, q);
                    BlockState blockState = this.level.getBlockState(blockPos);
                    if (!blockState.isAir() && blockState.getMaterial() != Material.FIRE) {
                        if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && !blockState.is(BlockTags.DRAGON_IMMUNE)) {
                            bl2 = this.level.removeBlock(blockPos, false) || bl2;
                        } else {
                            bl = true;
                        }
                    }
                }
            }
        }

        if (bl2) {
            BlockPos blockPos2 = new BlockPos(i + this.random.nextInt(l - i + 1), j + this.random.nextInt(m - j + 1), k + this.random.nextInt(n - k + 1));
            this.level.levelEvent(2008, blockPos2, 0);
        }

        return bl;
    }

    public boolean hurt(EnderDragonPart part, DamageSource source, float amount) {
        if (this.phaseManager.getCurrentPhase().getPhase() == EnderDragonPhase.DYING) {
            return false;
        } else {
            amount = this.phaseManager.getCurrentPhase().onHurt(source, amount);
            if (part != this.head) {
                amount = amount / 4.0F + Math.min(amount, 1.0F);
            }

            if (amount < 0.01F) {
                return false;
            } else {
                if (source.getEntity() instanceof Player || source.isExplosion()) {
                    float f = this.getHealth();
                    this.reallyHurt(source, amount);
                    if (this.isDeadOrDying() && !this.phaseManager.getCurrentPhase().isSitting()) {
                        this.setHealth(1.0F);
                        this.phaseManager.setPhase(EnderDragonPhase.DYING);
                    }

                    if (this.phaseManager.getCurrentPhase().isSitting()) {
                        this.sittingDamageReceived = this.sittingDamageReceived + f - this.getHealth();
                        if (this.sittingDamageReceived > 0.25F * this.getMaxHealth()) {
                            this.sittingDamageReceived = 0.0F;
                            this.phaseManager.setPhase(EnderDragonPhase.TAKEOFF);
                        }
                    }
                }

                return true;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source instanceof EntityDamageSource && ((EntityDamageSource)source).isThorns() && !this.level.isClientSide) {
            this.hurt(this.body, source, amount);
        }

        return false;
    }

    protected boolean reallyHurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public void kill() {
        this.remove(Entity.RemovalReason.KILLED);
        if (this.dragonFight != null) {
            this.dragonFight.updateDragon(this);
            this.dragonFight.setDragonKilled(this);
        }

    }

    @Override
    protected void tickDeath() {
        if (this.dragonFight != null) {
            this.dragonFight.updateDragon(this);
        }

        ++this.dragonDeathTime;
        if (this.dragonDeathTime >= 180 && this.dragonDeathTime <= 200) {
            float f = (this.random.nextFloat() - 0.5F) * 8.0F;
            float g = (this.random.nextFloat() - 0.5F) * 4.0F;
            float h = (this.random.nextFloat() - 0.5F) * 8.0F;
            this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.getX() + (double)f, this.getY() + 2.0D + (double)g, this.getZ() + (double)h, 0.0D, 0.0D, 0.0D);
        }

        boolean bl = this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        int i = 500;
        if (this.dragonFight != null && !this.dragonFight.hasPreviouslyKilledDragon()) {
            i = 12000;
        }

        if (this.level instanceof ServerLevel) {
            if (this.dragonDeathTime > 150 && this.dragonDeathTime % 5 == 0 && bl) {
                ExperienceOrb.award((ServerLevel)this.level, this.position(), Mth.floor((float)i * 0.08F));
            }

            if (this.dragonDeathTime == 1 && !this.isSilent()) {
                this.level.globalLevelEvent(1028, this.blockPosition(), 0);
            }
        }

        this.move(MoverType.SELF, new Vec3(0.0D, (double)0.1F, 0.0D));
        this.setYRot(this.getYRot() + 20.0F);
        this.yBodyRot = this.getYRot();
        if (this.dragonDeathTime == 200 && this.level instanceof ServerLevel) {
            if (bl) {
                ExperienceOrb.award((ServerLevel)this.level, this.position(), Mth.floor((float)i * 0.2F));
            }

            if (this.dragonFight != null) {
                this.dragonFight.setDragonKilled(this);
            }

            this.remove(Entity.RemovalReason.KILLED);
        }

    }

    public int findClosestNode() {
        if (this.nodes[0] == null) {
            for(int i = 0; i < 24; ++i) {
                int j = 5;
                int l;
                int m;
                if (i < 12) {
                    l = Mth.floor(60.0F * Mth.cos(2.0F * (-(float)Math.PI + 0.2617994F * (float)i)));
                    m = Mth.floor(60.0F * Mth.sin(2.0F * (-(float)Math.PI + 0.2617994F * (float)i)));
                } else if (i < 20) {
                    int k = i - 12;
                    l = Mth.floor(40.0F * Mth.cos(2.0F * (-(float)Math.PI + ((float)Math.PI / 8F) * (float)k)));
                    m = Mth.floor(40.0F * Mth.sin(2.0F * (-(float)Math.PI + ((float)Math.PI / 8F) * (float)k)));
                    j += 10;
                } else {
                    int var7 = i - 20;
                    l = Mth.floor(20.0F * Mth.cos(2.0F * (-(float)Math.PI + ((float)Math.PI / 4F) * (float)var7)));
                    m = Mth.floor(20.0F * Mth.sin(2.0F * (-(float)Math.PI + ((float)Math.PI / 4F) * (float)var7)));
                }

                int r = Math.max(this.level.getSeaLevel() + 10, this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(l, 0, m)).getY() + j);
                this.nodes[i] = new Node(l, r, m);
            }

            this.nodeAdjacency[0] = 6146;
            this.nodeAdjacency[1] = 8197;
            this.nodeAdjacency[2] = 8202;
            this.nodeAdjacency[3] = 16404;
            this.nodeAdjacency[4] = 32808;
            this.nodeAdjacency[5] = 32848;
            this.nodeAdjacency[6] = 65696;
            this.nodeAdjacency[7] = 131392;
            this.nodeAdjacency[8] = 131712;
            this.nodeAdjacency[9] = 263424;
            this.nodeAdjacency[10] = 526848;
            this.nodeAdjacency[11] = 525313;
            this.nodeAdjacency[12] = 1581057;
            this.nodeAdjacency[13] = 3166214;
            this.nodeAdjacency[14] = 2138120;
            this.nodeAdjacency[15] = 6373424;
            this.nodeAdjacency[16] = 4358208;
            this.nodeAdjacency[17] = 12910976;
            this.nodeAdjacency[18] = 9044480;
            this.nodeAdjacency[19] = 9706496;
            this.nodeAdjacency[20] = 15216640;
            this.nodeAdjacency[21] = 13688832;
            this.nodeAdjacency[22] = 11763712;
            this.nodeAdjacency[23] = 8257536;
        }

        return this.findClosestNode(this.getX(), this.getY(), this.getZ());
    }

    public int findClosestNode(double x, double y, double z) {
        float f = 10000.0F;
        int i = 0;
        Node node = new Node(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        int j = 0;
        if (this.dragonFight == null || this.dragonFight.getCrystalsAlive() == 0) {
            j = 12;
        }

        for(int k = j; k < 24; ++k) {
            if (this.nodes[k] != null) {
                float g = this.nodes[k].distanceToSqr(node);
                if (g < f) {
                    f = g;
                    i = k;
                }
            }
        }

        return i;
    }

    @Nullable
    public Path findPath(int from, int to, @Nullable Node pathNode) {
        for(int i = 0; i < 24; ++i) {
            Node node = this.nodes[i];
            node.closed = false;
            node.f = 0.0F;
            node.g = 0.0F;
            node.h = 0.0F;
            node.cameFrom = null;
            node.heapIdx = -1;
        }

        Node node2 = this.nodes[from];
        Node node3 = this.nodes[to];
        node2.g = 0.0F;
        node2.h = node2.distanceTo(node3);
        node2.f = node2.h;
        this.openSet.clear();
        this.openSet.insert(node2);
        Node node4 = node2;
        int j = 0;
        if (this.dragonFight == null || this.dragonFight.getCrystalsAlive() == 0) {
            j = 12;
        }

        while(!this.openSet.isEmpty()) {
            Node node5 = this.openSet.pop();
            if (node5.equals(node3)) {
                if (pathNode != null) {
                    pathNode.cameFrom = node3;
                    node3 = pathNode;
                }

                return this.reconstructPath(node2, node3);
            }

            if (node5.distanceTo(node3) < node4.distanceTo(node3)) {
                node4 = node5;
            }

            node5.closed = true;
            int k = 0;

            for(int l = 0; l < 24; ++l) {
                if (this.nodes[l] == node5) {
                    k = l;
                    break;
                }
            }

            for(int m = j; m < 24; ++m) {
                if ((this.nodeAdjacency[k] & 1 << m) > 0) {
                    Node node6 = this.nodes[m];
                    if (!node6.closed) {
                        float f = node5.g + node5.distanceTo(node6);
                        if (!node6.inOpenSet() || f < node6.g) {
                            node6.cameFrom = node5;
                            node6.g = f;
                            node6.h = node6.distanceTo(node3);
                            if (node6.inOpenSet()) {
                                this.openSet.changeCost(node6, node6.g + node6.h);
                            } else {
                                node6.f = node6.g + node6.h;
                                this.openSet.insert(node6);
                            }
                        }
                    }
                }
            }
        }

        if (node4 == node2) {
            return null;
        } else {
            LOGGER.debug("Failed to find path from {} to {}", from, to);
            if (pathNode != null) {
                pathNode.cameFrom = node4;
                node4 = pathNode;
            }

            return this.reconstructPath(node2, node4);
        }
    }

    private Path reconstructPath(Node unused, Node node) {
        List<Node> list = Lists.newArrayList();
        Node node2 = node;
        list.add(0, node);

        while(node2.cameFrom != null) {
            node2 = node2.cameFrom;
            list.add(0, node2);
        }

        return new Path(list, new BlockPos(node.x, node.y, node.z), true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("DragonPhase", this.phaseManager.getCurrentPhase().getPhase().getId());
        nbt.putInt("DragonDeathTime", this.dragonDeathTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("DragonPhase")) {
            this.phaseManager.setPhase(EnderDragonPhase.getById(nbt.getInt("DragonPhase")));
        }

        if (nbt.contains("DragonDeathTime")) {
            this.dragonDeathTime = nbt.getInt("DragonDeathTime");
        }

    }

    @Override
    public void checkDespawn() {
    }

    public EnderDragonPart[] getSubEntities() {
        return this.subEntities;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    public float getSoundVolume() {
        return 5.0F;
    }

    public float getHeadPartYOffset(int segmentOffset, double[] segment1, double[] segment2) {
        DragonPhaseInstance dragonPhaseInstance = this.phaseManager.getCurrentPhase();
        EnderDragonPhase<? extends DragonPhaseInstance> enderDragonPhase = dragonPhaseInstance.getPhase();
        double f;
        if (enderDragonPhase != EnderDragonPhase.LANDING && enderDragonPhase != EnderDragonPhase.TAKEOFF) {
            if (dragonPhaseInstance.isSitting()) {
                f = (double)segmentOffset;
            } else if (segmentOffset == 6) {
                f = 0.0D;
            } else {
                f = segment2[1] - segment1[1];
            }
        } else {
            BlockPos blockPos = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.END_PODIUM_LOCATION);
            double d = Math.max(Math.sqrt(blockPos.distToCenterSqr(this.position())) / 4.0D, 1.0D);
            f = (double)segmentOffset / d;
        }

        return (float)f;
    }

    public Vec3 getHeadLookVector(float tickDelta) {
        DragonPhaseInstance dragonPhaseInstance = this.phaseManager.getCurrentPhase();
        EnderDragonPhase<? extends DragonPhaseInstance> enderDragonPhase = dragonPhaseInstance.getPhase();
        Vec3 vec32;
        if (enderDragonPhase != EnderDragonPhase.LANDING && enderDragonPhase != EnderDragonPhase.TAKEOFF) {
            if (dragonPhaseInstance.isSitting()) {
                float j = this.getXRot();
                float k = 1.5F;
                this.setXRot(-45.0F);
                vec32 = this.getViewVector(tickDelta);
                this.setXRot(j);
            } else {
                vec32 = this.getViewVector(tickDelta);
            }
        } else {
            BlockPos blockPos = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.END_PODIUM_LOCATION);
            float f = Math.max((float)Math.sqrt(blockPos.distToCenterSqr(this.position())) / 4.0F, 1.0F);
            float g = 6.0F / f;
            float h = this.getXRot();
            float i = 1.5F;
            this.setXRot(-g * 1.5F * 5.0F);
            vec32 = this.getViewVector(tickDelta);
            this.setXRot(h);
        }

        return vec32;
    }

    public void onCrystalDestroyed(EndCrystal crystal, BlockPos pos, DamageSource source) {
        Player player;
        if (source.getEntity() instanceof Player) {
            player = (Player)source.getEntity();
        } else {
            player = this.level.getNearestPlayer(CRYSTAL_DESTROY_TARGETING, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        }

        if (crystal == this.nearestCrystal) {
            this.hurt(this.head, DamageSource.explosion(player), 10.0F);
        }

        this.phaseManager.getCurrentPhase().onCrystalDestroyed(crystal, pos, source, player);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (DATA_PHASE.equals(data) && this.level.isClientSide) {
            this.phaseManager.setPhase(EnderDragonPhase.getById(this.getEntityData().get(DATA_PHASE)));
        }

        super.onSyncedDataUpdated(data);
    }

    public EnderDragonPhaseManager getPhaseManager() {
        return this.phaseManager;
    }

    @Nullable
    public EndDragonFight getDragonFight() {
        return this.dragonFight;
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        return false;
    }

    @Override
    protected boolean canRide(Entity entity) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void recreateFromPacket(ClientboundAddMobPacket packet) {
        super.recreateFromPacket(packet);
        EnderDragonPart[] enderDragonParts = this.getSubEntities();

        for(int i = 0; i < enderDragonParts.length; ++i) {
            enderDragonParts[i].setId(i + packet.getId());
        }

    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target.canBeSeenAsEnemy();
    }
}
