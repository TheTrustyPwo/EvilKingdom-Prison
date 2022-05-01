package net.minecraft.world.effect;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
// CraftBukkit end

public class MobEffect {

    private final Map<Attribute, AttributeModifier> attributeModifiers = Maps.newHashMap();
    private final MobEffectCategory category;
    private final int color;
    @Nullable
    private String descriptionId;

    @Nullable
    public static MobEffect byId(int rawId) {
        return (MobEffect) Registry.MOB_EFFECT.byId(rawId);
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
                entity.heal(1.0F, RegainReason.MAGIC_REGEN); // CraftBukkit
            }
        } else if (this == MobEffects.POISON) {
            if (entity.getHealth() > 1.0F) {
                entity.hurt(CraftEventFactory.POISON, 1.0F);  // CraftBukkit - DamageSource.MAGIC -> CraftEventFactory.POISON
            }
        } else if (this == MobEffects.WITHER) {
            entity.hurt(DamageSource.WITHER, 1.0F);
        } else if (this == MobEffects.HUNGER && entity instanceof Player) {
            ((Player) entity).causeFoodExhaustion(0.005F * (float) (amplifier + 1), org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.HUNGER_EFFECT); // CraftBukkit - EntityExhaustionEvent
        } else if (this == MobEffects.SATURATION && entity instanceof Player) {
            if (!entity.level.isClientSide) {
                // CraftBukkit start
                Player entityhuman = (Player) entity;
                int oldFoodLevel = entityhuman.getFoodData().foodLevel;

                org.bukkit.event.entity.FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(entityhuman, amplifier + 1 + oldFoodLevel);

                if (!event.isCancelled()) {
                    entityhuman.getFoodData().eat(event.getFoodLevel() - oldFoodLevel, 1.0F);
                }

                ((ServerPlayer) entityhuman).connection.send(new ClientboundSetHealthPacket(((ServerPlayer) entityhuman).getBukkitEntity().getScaledHealth(), entityhuman.getFoodData().foodLevel, entityhuman.getFoodData().saturationLevel));
                // CraftBukkit end
            }
        } else if ((this != MobEffects.HEAL || entity.isInvertedHealAndHarm()) && (this != MobEffects.HARM || !entity.isInvertedHealAndHarm())) {
            if (this == MobEffects.HARM && !entity.isInvertedHealAndHarm() || this == MobEffects.HEAL && entity.isInvertedHealAndHarm()) {
                entity.hurt(DamageSource.MAGIC, (float) (6 << amplifier));
            }
        } else {
            entity.heal((float) Math.max(4 << amplifier, 0), RegainReason.MAGIC); // CraftBukkit
        }

    }

    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity attacker, LivingEntity target, int amplifier, double proximity) {
        int j;

        if ((this != MobEffects.HEAL || target.isInvertedHealAndHarm()) && (this != MobEffects.HARM || !target.isInvertedHealAndHarm())) {
            if ((this != MobEffects.HARM || target.isInvertedHealAndHarm()) && (this != MobEffects.HEAL || !target.isInvertedHealAndHarm())) {
                this.applyEffectTick(target, amplifier);
            } else {
                j = (int) (proximity * (double) (6 << amplifier) + 0.5D);
                if (source == null) {
                    target.hurt(DamageSource.MAGIC, (float) j);
                } else {
                    target.hurt(DamageSource.indirectMagic(source, attacker), (float) j);
                }
            }
        } else {
            j = (int) (proximity * (double) (4 << amplifier) + 0.5D);
            target.heal((float) j, RegainReason.MAGIC); // CraftBukkit
        }

    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        int k;

        if (this == MobEffects.REGENERATION) {
            k = 50 >> amplifier;
            return k > 0 ? duration % k == 0 : true;
        } else if (this == MobEffects.POISON) {
            k = 25 >> amplifier;
            return k > 0 ? duration % k == 0 : true;
        } else if (this == MobEffects.WITHER) {
            k = 40 >> amplifier;
            return k > 0 ? duration % k == 0 : true;
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
        AttributeModifier attributemodifier = new AttributeModifier(UUID.fromString(uuid), this::getDescriptionId, amount, operation);

        this.attributeModifiers.put(attribute, attributemodifier);
        return this;
    }

    public Map<Attribute, AttributeModifier> getAttributeModifiers() {
        return this.attributeModifiers;
    }

    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        Iterator iterator = this.attributeModifiers.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<Attribute, AttributeModifier> entry = (Entry) iterator.next();
            AttributeInstance attributemodifiable = attributes.getInstance((Attribute) entry.getKey());

            if (attributemodifiable != null) {
                attributemodifiable.removeModifier((AttributeModifier) entry.getValue());
            }
        }

    }

    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        Iterator iterator = this.attributeModifiers.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<Attribute, AttributeModifier> entry = (Entry) iterator.next();
            AttributeInstance attributemodifiable = attributes.getInstance((Attribute) entry.getKey());

            if (attributemodifiable != null) {
                AttributeModifier attributemodifier = (AttributeModifier) entry.getValue();

                attributemodifiable.removeModifier(attributemodifier);
                attributemodifiable.addPermanentModifier(new AttributeModifier(attributemodifier.getId(), this.getDescriptionId() + " " + amplifier, this.getAttributeModifierValue(amplifier, attributemodifier), attributemodifier.getOperation()));
            }
        }

    }

    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return modifier.getAmount() * (double) (amplifier + 1);
    }

    public boolean isBeneficial() {
        return this.category == MobEffectCategory.BENEFICIAL;
    }
}
