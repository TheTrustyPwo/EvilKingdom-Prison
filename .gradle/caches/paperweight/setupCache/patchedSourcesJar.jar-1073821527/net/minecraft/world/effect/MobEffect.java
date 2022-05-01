package net.minecraft.world.effect;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public class MobEffect {
    private final Map<Attribute, AttributeModifier> attributeModifiers = Maps.newHashMap();
    private final MobEffectCategory category;
    private final int color;
    @Nullable
    private String descriptionId;

    @Nullable
    public static MobEffect byId(int rawId) {
        return Registry.MOB_EFFECT.byId(rawId);
    }

    public static int getId(MobEffect type) {
        return Registry.MOB_EFFECT.getId(type);
    }

    protected MobEffect(MobEffectCategory category, int color) {
        this.category = category;
        this.color = color;
    }

    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (this == MobEffects.REGENERATION) {
            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(1.0F);
            }
        } else if (this == MobEffects.POISON) {
            if (entity.getHealth() > 1.0F) {
                entity.hurt(DamageSource.MAGIC, 1.0F);
            }
        } else if (this == MobEffects.WITHER) {
            entity.hurt(DamageSource.WITHER, 1.0F);
        } else if (this == MobEffects.HUNGER && entity instanceof Player) {
            ((Player)entity).causeFoodExhaustion(0.005F * (float)(amplifier + 1));
        } else if (this == MobEffects.SATURATION && entity instanceof Player) {
            if (!entity.level.isClientSide) {
                ((Player)entity).getFoodData().eat(amplifier + 1, 1.0F);
            }
        } else if ((this != MobEffects.HEAL || entity.isInvertedHealAndHarm()) && (this != MobEffects.HARM || !entity.isInvertedHealAndHarm())) {
            if (this == MobEffects.HARM && !entity.isInvertedHealAndHarm() || this == MobEffects.HEAL && entity.isInvertedHealAndHarm()) {
                entity.hurt(DamageSource.MAGIC, (float)(6 << amplifier));
            }
        } else {
            entity.heal((float)Math.max(4 << amplifier, 0));
        }

    }

    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity attacker, LivingEntity target, int amplifier, double proximity) {
        if ((this != MobEffects.HEAL || target.isInvertedHealAndHarm()) && (this != MobEffects.HARM || !target.isInvertedHealAndHarm())) {
            if (this == MobEffects.HARM && !target.isInvertedHealAndHarm() || this == MobEffects.HEAL && target.isInvertedHealAndHarm()) {
                int j = (int)(proximity * (double)(6 << amplifier) + 0.5D);
                if (source == null) {
                    target.hurt(DamageSource.MAGIC, (float)j);
                } else {
                    target.hurt(DamageSource.indirectMagic(source, attacker), (float)j);
                }
            } else {
                this.applyEffectTick(target, amplifier);
            }
        } else {
            int i = (int)(proximity * (double)(4 << amplifier) + 0.5D);
            target.heal((float)i);
        }

    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        if (this == MobEffects.REGENERATION) {
            int i = 50 >> amplifier;
            if (i > 0) {
                return duration % i == 0;
            } else {
                return true;
            }
        } else if (this == MobEffects.POISON) {
            int j = 25 >> amplifier;
            if (j > 0) {
                return duration % j == 0;
            } else {
                return true;
            }
        } else if (this == MobEffects.WITHER) {
            int k = 40 >> amplifier;
            if (k > 0) {
                return duration % k == 0;
            } else {
                return true;
            }
        } else {
            return this == MobEffects.HUNGER;
        }
    }

    public boolean isInstantenous() {
        return false;
    }

    protected String getOrCreateDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("effect", Registry.MOB_EFFECT.getKey(this));
        }

        return this.descriptionId;
    }

    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    public Component getDisplayName() {
        return new TranslatableComponent(this.getDescriptionId());
    }

    public MobEffectCategory getCategory() {
        return this.category;
    }

    public int getColor() {
        return this.color;
    }

    public MobEffect addAttributeModifier(Attribute attribute, String uuid, double amount, AttributeModifier.Operation operation) {
        AttributeModifier attributeModifier = new AttributeModifier(UUID.fromString(uuid), this::getDescriptionId, amount, operation);
        this.attributeModifiers.put(attribute, attributeModifier);
        return this;
    }

    public Map<Attribute, AttributeModifier> getAttributeModifiers() {
        return this.attributeModifiers;
    }

    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        for(Entry<Attribute, AttributeModifier> entry : this.attributeModifiers.entrySet()) {
            AttributeInstance attributeInstance = attributes.getInstance(entry.getKey());
            if (attributeInstance != null) {
                attributeInstance.removeModifier(entry.getValue());
            }
        }

    }

    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        for(Entry<Attribute, AttributeModifier> entry : this.attributeModifiers.entrySet()) {
            AttributeInstance attributeInstance = attributes.getInstance(entry.getKey());
            if (attributeInstance != null) {
                AttributeModifier attributeModifier = entry.getValue();
                attributeInstance.removeModifier(attributeModifier);
                attributeInstance.addPermanentModifier(new AttributeModifier(attributeModifier.getId(), this.getDescriptionId() + " " + amplifier, this.getAttributeModifierValue(amplifier, attributeModifier), attributeModifier.getOperation()));
            }
        }

    }

    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return modifier.getAmount() * (double)(amplifier + 1);
    }

    public boolean isBeneficial() {
        return this.category == MobEffectCategory.BENEFICIAL;
    }
}
