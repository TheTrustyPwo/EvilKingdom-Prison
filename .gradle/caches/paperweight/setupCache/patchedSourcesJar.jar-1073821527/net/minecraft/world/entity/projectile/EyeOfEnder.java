package net.minecraft.world.entity.projectile;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EyeOfEnder extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(EyeOfEnder.class, EntityDataSerializers.ITEM_STACK);
    public double tx;
    public double ty;
    public double tz;
    public int life;
    public boolean surviveAfterDeath;

    public EyeOfEnder(EntityType<? extends EyeOfEnder> type, Level world) {
        super(type, world);
    }

    public EyeOfEnder(Level world, double x, double y, double z) {
        this(EntityType.EYE_OF_ENDER, world);
        this.setPos(x, y, z);
    }

    public void setItem(ItemStack stack) {
        if (!stack.is(Items.ENDER_EYE) || stack.hasTag()) {
            this.getEntityData().set(DATA_ITEM_STACK, Util.make(stack.copy(), (stackx) -> {
                stackx.setCount(1);
            }));
        }

    }

    private ItemStack getItemRaw() {
        return this.getEntityData().get(DATA_ITEM_STACK);
    }

    @Override
    public ItemStack getItem() {
        ItemStack itemStack = this.getItemRaw();
        return itemStack.isEmpty() ? new ItemStack(Items.ENDER_EYE) : itemStack;
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(DATA_ITEM_STACK, ItemStack.EMPTY);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize() * 4.0D;
        if (Double.isNaN(d)) {
            d = 4.0D;
        }

        d *= 64.0D;
        return distance < d * d;
    }

    public void signalTo(BlockPos pos) {
        double d = (double)pos.getX();
        int i = pos.getY();
        double e = (double)pos.getZ();
        double f = d - this.getX();
        double g = e - this.getZ();
        double h = Math.sqrt(f * f + g * g);
        if (h > 12.0D) {
            this.tx = this.getX() + f / h * 12.0D;
            this.tz = this.getZ() + g / h * 12.0D;
            this.ty = this.getY() + 8.0D;
        } else {
            this.tx = d;
            this.ty = (double)i;
            this.tz = e;
        }

        this.life = 0;
        this.surviveAfterDeath = this.random.nextInt(5) > 0;
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.setDeltaMovement(x, y, z);
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            double d = Math.sqrt(x * x + z * z);
            this.setYRot((float)(Mth.atan2(x, z) * (double)(180F / (float)Math.PI)));
            this.setXRot((float)(Mth.atan2(y, d) * (double)(180F / (float)Math.PI)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }

    }

    @Override
    public void tick() {
        super.tick();
        Vec3 vec3 = this.getDeltaMovement();
        double d = this.getX() + vec3.x;
        double e = this.getY() + vec3.y;
        double f = this.getZ() + vec3.z;
        double g = vec3.horizontalDistance();
        this.setXRot(Projectile.lerpRotation(this.xRotO, (float)(Mth.atan2(vec3.y, g) * (double)(180F / (float)Math.PI))));
        this.setYRot(Projectile.lerpRotation(this.yRotO, (float)(Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI))));
        if (!this.level.isClientSide) {
            double h = this.tx - d;
            double i = this.tz - f;
            float j = (float)Math.sqrt(h * h + i * i);
            float k = (float)Mth.atan2(i, h);
            double l = Mth.lerp(0.0025D, g, (double)j);
            double m = vec3.y;
            if (j < 1.0F) {
                l *= 0.8D;
                m *= 0.8D;
            }

            int n = this.getY() < this.ty ? 1 : -1;
            vec3 = new Vec3(Math.cos((double)k) * l, m + ((double)n - m) * (double)0.015F, Math.sin((double)k) * l);
            this.setDeltaMovement(vec3);
        }

        float o = 0.25F;
        if (this.isInWater()) {
            for(int p = 0; p < 4; ++p) {
                this.level.addParticle(ParticleTypes.BUBBLE, d - vec3.x * 0.25D, e - vec3.y * 0.25D, f - vec3.z * 0.25D, vec3.x, vec3.y, vec3.z);
            }
        } else {
            this.level.addParticle(ParticleTypes.PORTAL, d - vec3.x * 0.25D + this.random.nextDouble() * 0.6D - 0.3D, e - vec3.y * 0.25D - 0.5D, f - vec3.z * 0.25D + this.random.nextDouble() * 0.6D - 0.3D, vec3.x, vec3.y, vec3.z);
        }

        if (!this.level.isClientSide) {
            this.setPos(d, e, f);
            ++this.life;
            if (this.life > 80 && !this.level.isClientSide) {
                this.playSound(SoundEvents.ENDER_EYE_DEATH, 1.0F, 1.0F);
                this.discard();
                if (this.surviveAfterDeath) {
                    this.level.addFreshEntity(new ItemEntity(this.level, this.getX(), this.getY(), this.getZ(), this.getItem()));
                } else {
                    this.level.levelEvent(2003, this.blockPosition(), 0);
                }
            }
        } else {
            this.setPosRaw(d, e, f);
        }

    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        ItemStack itemStack = this.getItemRaw();
        if (!itemStack.isEmpty()) {
            nbt.put("Item", itemStack.save(new CompoundTag()));
        }

    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        ItemStack itemStack = ItemStack.of(nbt.getCompound("Item"));
        this.setItem(itemStack);
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
