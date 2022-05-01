package net.minecraft.world.effect;

import com.google.common.collect.ComparisonChain;
import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

public class MobEffectInstance implements Comparable<MobEffectInstance> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MobEffect effect;
    private int duration;
    private int amplifier;
    private boolean ambient;
    private boolean noCounter;
    private boolean visible;
    private boolean showIcon;
    @Nullable
    private MobEffectInstance hiddenEffect;

    public MobEffectInstance(MobEffect type) {
        this(type, 0, 0);
    }

    public MobEffectInstance(MobEffect type, int duration) {
        this(type, duration, 0);
    }

    public MobEffectInstance(MobEffect type, int duration, int amplifier) {
        this(type, duration, amplifier, false, true);
    }

    public MobEffectInstance(MobEffect type, int duration, int amplifier, boolean ambient, boolean visible) {
        this(type, duration, amplifier, ambient, visible, visible);
    }

    public MobEffectInstance(MobEffect type, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon) {
        this(type, duration, amplifier, ambient, showParticles, showIcon, (MobEffectInstance)null);
    }

    public MobEffectInstance(MobEffect type, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon, @Nullable MobEffectInstance hiddenEffect) {
        this.effect = type;
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.visible = showParticles;
        this.showIcon = showIcon;
        this.hiddenEffect = hiddenEffect;
    }

    public MobEffectInstance(MobEffectInstance mobEffectInstance) {
        this.effect = mobEffectInstance.effect;
        this.setDetailsFrom(mobEffectInstance);
    }

    void setDetailsFrom(MobEffectInstance that) {
        this.duration = that.duration;
        this.amplifier = that.amplifier;
        this.ambient = that.ambient;
        this.visible = that.visible;
        this.showIcon = that.showIcon;
    }

    public boolean update(MobEffectInstance that) {
        if (this.effect != that.effect) {
            LOGGER.warn("This method should only be called for matching effects!");
        }

        boolean bl = false;
        if (that.amplifier > this.amplifier) {
            if (that.duration < this.duration) {
                MobEffectInstance mobEffectInstance = this.hiddenEffect;
                this.hiddenEffect = new MobEffectInstance(this);
                this.hiddenEffect.hiddenEffect = mobEffectInstance;
            }

            this.amplifier = that.amplifier;
            this.duration = that.duration;
            bl = true;
        } else if (that.duration > this.duration) {
            if (that.amplifier == this.amplifier) {
                this.duration = that.duration;
                bl = true;
            } else if (this.hiddenEffect == null) {
                this.hiddenEffect = new MobEffectInstance(that);
            } else {
                this.hiddenEffect.update(that);
            }
        }

        if (!that.ambient && this.ambient || bl) {
            this.ambient = that.ambient;
            bl = true;
        }

        if (that.visible != this.visible) {
            this.visible = that.visible;
            bl = true;
        }

        if (that.showIcon != this.showIcon) {
            this.showIcon = that.showIcon;
            bl = true;
        }

        return bl;
    }

    public MobEffect getEffect() {
        return this.effect;
    }

    public int getDuration() {
        return this.duration;
    }

    public int getAmplifier() {
        return this.amplifier;
    }

    public boolean isAmbient() {
        return this.ambient;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean showIcon() {
        return this.showIcon;
    }

    public boolean tick(LivingEntity entity, Runnable overwriteCallback) {
        if (this.duration > 0) {
            if (this.effect.isDurationEffectTick(this.duration, this.amplifier)) {
                this.applyEffect(entity);
            }

            this.tickDownDuration();
            if (this.duration == 0 && this.hiddenEffect != null) {
                this.setDetailsFrom(this.hiddenEffect);
                this.hiddenEffect = this.hiddenEffect.hiddenEffect;
                overwriteCallback.run();
            }
        }

        return this.duration > 0;
    }

    private int tickDownDuration() {
        if (this.hiddenEffect != null) {
            this.hiddenEffect.tickDownDuration();
        }

        return --this.duration;
    }

    public void applyEffect(LivingEntity entity) {
        if (this.duration > 0) {
            this.effect.applyEffectTick(entity, this.amplifier);
        }

    }

    public String getDescriptionId() {
        return this.effect.getDescriptionId();
    }

    @Override
    public String toString() {
        String string;
        if (this.amplifier > 0) {
            string = this.getDescriptionId() + " x " + (this.amplifier + 1) + ", Duration: " + this.duration;
        } else {
            string = this.getDescriptionId() + ", Duration: " + this.duration;
        }

        if (!this.visible) {
            string = string + ", Particles: false";
        }

        if (!this.showIcon) {
            string = string + ", Show Icon: false";
        }

        return string;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof MobEffectInstance)) {
            return false;
        } else {
            MobEffectInstance mobEffectInstance = (MobEffectInstance)object;
            return this.duration == mobEffectInstance.duration && this.amplifier == mobEffectInstance.amplifier && this.ambient == mobEffectInstance.ambient && this.effect.equals(mobEffectInstance.effect);
        }
    }

    @Override
    public int hashCode() {
        int i = this.effect.hashCode();
        i = 31 * i + this.duration;
        i = 31 * i + this.amplifier;
        return 31 * i + (this.ambient ? 1 : 0);
    }

    public CompoundTag save(CompoundTag nbt) {
        nbt.putByte("Id", (byte)MobEffect.getId(this.getEffect()));
        this.writeDetailsTo(nbt);
        return nbt;
    }

    private void writeDetailsTo(CompoundTag nbt) {
        nbt.putByte("Amplifier", (byte)this.getAmplifier());
        nbt.putInt("Duration", this.getDuration());
        nbt.putBoolean("Ambient", this.isAmbient());
        nbt.putBoolean("ShowParticles", this.isVisible());
        nbt.putBoolean("ShowIcon", this.showIcon());
        if (this.hiddenEffect != null) {
            CompoundTag compoundTag = new CompoundTag();
            this.hiddenEffect.save(compoundTag);
            nbt.put("HiddenEffect", compoundTag);
        }

    }

    @Nullable
    public static MobEffectInstance load(CompoundTag nbt) {
        int i = nbt.getByte("Id");
        MobEffect mobEffect = MobEffect.byId(i);
        return mobEffect == null ? null : loadSpecifiedEffect(mobEffect, nbt);
    }

    private static MobEffectInstance loadSpecifiedEffect(MobEffect type, CompoundTag nbt) {
        int i = Byte.toUnsignedInt(nbt.getByte("Amplifier")); // Paper - correctly load amplifiers > 127
        int j = nbt.getInt("Duration");
        boolean bl = nbt.getBoolean("Ambient");
        boolean bl2 = true;
        if (nbt.contains("ShowParticles", 1)) {
            bl2 = nbt.getBoolean("ShowParticles");
        }

        boolean bl3 = bl2;
        if (nbt.contains("ShowIcon", 1)) {
            bl3 = nbt.getBoolean("ShowIcon");
        }

        MobEffectInstance mobEffectInstance = null;
        if (nbt.contains("HiddenEffect", 10)) {
            mobEffectInstance = loadSpecifiedEffect(type, nbt.getCompound("HiddenEffect"));
        }

        return new MobEffectInstance(type, j, i < 0 ? 0 : i, bl, bl2, bl3, mobEffectInstance);
    }

    public void setNoCounter(boolean permanent) {
        this.noCounter = permanent;
    }

    public boolean isNoCounter() {
        return this.noCounter;
    }

    @Override
    public int compareTo(MobEffectInstance mobEffectInstance) {
        int i = 32147;
        return (this.getDuration() <= 32147 || mobEffectInstance.getDuration() <= 32147) && (!this.isAmbient() || !mobEffectInstance.isAmbient()) ? ComparisonChain.start().compare(this.isAmbient(), mobEffectInstance.isAmbient()).compare(this.getDuration(), mobEffectInstance.getDuration()).compare(this.getEffect().getColor(), mobEffectInstance.getEffect().getColor()).result() : ComparisonChain.start().compare(this.isAmbient(), mobEffectInstance.isAmbient()).compare(this.getEffect().getColor(), mobEffectInstance.getEffect().getColor()).result();
    }
}
