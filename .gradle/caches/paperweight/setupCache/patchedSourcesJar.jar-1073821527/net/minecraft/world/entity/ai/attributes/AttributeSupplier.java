package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;

public class AttributeSupplier {
    public final Map<Attribute, AttributeInstance> instances;

    public AttributeSupplier(Map<Attribute, AttributeInstance> instances) {
        this.instances = ImmutableMap.copyOf(instances);
    }

    private AttributeInstance getAttributeInstance(Attribute attribute) {
        AttributeInstance attributeInstance = this.instances.get(attribute);
        if (attributeInstance == null) {
            throw new IllegalArgumentException("Can't find attribute " + Registry.ATTRIBUTE.getKey(attribute));
        } else {
            return attributeInstance;
        }
    }

    public double getValue(Attribute attribute) {
        return this.getAttributeInstance(attribute).getValue();
    }

    public double getBaseValue(Attribute attribute) {
        return this.getAttributeInstance(attribute).getBaseValue();
    }

    public double getModifierValue(Attribute attribute, UUID uuid) {
        AttributeModifier attributeModifier = this.getAttributeInstance(attribute).getModifier(uuid);
        if (attributeModifier == null) {
            throw new IllegalArgumentException("Can't find modifier " + uuid + " on attribute " + Registry.ATTRIBUTE.getKey(attribute));
        } else {
            return attributeModifier.getAmount();
        }
    }

    @Nullable
    public AttributeInstance createInstance(Consumer<AttributeInstance> updateCallback, Attribute attribute) {
        AttributeInstance attributeInstance = this.instances.get(attribute);
        if (attributeInstance == null) {
            return null;
        } else {
            AttributeInstance attributeInstance2 = new AttributeInstance(attribute, updateCallback);
            attributeInstance2.replaceFrom(attributeInstance);
            return attributeInstance2;
        }
    }

    public static AttributeSupplier.Builder builder() {
        return new AttributeSupplier.Builder();
    }

    public boolean hasAttribute(Attribute type) {
        return this.instances.containsKey(type);
    }

    public boolean hasModifier(Attribute type, UUID uuid) {
        AttributeInstance attributeInstance = this.instances.get(type);
        return attributeInstance != null && attributeInstance.getModifier(uuid) != null;
    }

    public static class Builder {
        private final Map<Attribute, AttributeInstance> builder = Maps.newHashMap();
        private boolean instanceFrozen;

        private AttributeInstance create(Attribute attribute) {
            AttributeInstance attributeInstance = new AttributeInstance(attribute, (attributex) -> {
                if (this.instanceFrozen) {
                    throw new UnsupportedOperationException("Tried to change value for default attribute instance: " + Registry.ATTRIBUTE.getKey(attribute));
                }
            });
            this.builder.put(attribute, attributeInstance);
            return attributeInstance;
        }

        public AttributeSupplier.Builder add(Attribute attribute) {
            this.create(attribute);
            return this;
        }

        public AttributeSupplier.Builder add(Attribute attribute, double baseValue) {
            AttributeInstance attributeInstance = this.create(attribute);
            attributeInstance.setBaseValue(baseValue);
            return this;
        }

        public AttributeSupplier build() {
            this.instanceFrozen = true;
            return new AttributeSupplier(this.builder);
        }
    }
}
