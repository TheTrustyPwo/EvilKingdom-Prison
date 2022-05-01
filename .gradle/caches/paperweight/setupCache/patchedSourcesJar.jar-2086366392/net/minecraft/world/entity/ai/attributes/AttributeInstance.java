package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class AttributeInstance {
    private final Attribute attribute;
    private final Map<AttributeModifier.Operation, Set<AttributeModifier>> modifiersByOperation = Maps.newEnumMap(AttributeModifier.Operation.class);
    private final Map<UUID, AttributeModifier> modifierById = new Object2ObjectArrayMap<>();
    private final Set<AttributeModifier> permanentModifiers = new ObjectArraySet<>();
    private double baseValue;
    private boolean dirty = true;
    private double cachedValue;
    private final Consumer<AttributeInstance> onDirty;

    public AttributeInstance(Attribute type, Consumer<AttributeInstance> updateCallback) {
        this.attribute = type;
        this.onDirty = updateCallback;
        this.baseValue = type.getDefaultValue();
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public double getBaseValue() {
        return this.baseValue;
    }

    public void setBaseValue(double baseValue) {
        if (baseValue != this.baseValue) {
            this.baseValue = baseValue;
            this.setDirty();
        }
    }

    public Set<AttributeModifier> getModifiers(AttributeModifier.Operation operation) {
        return this.modifiersByOperation.computeIfAbsent(operation, (operationx) -> {
            return Sets.newHashSet();
        });
    }

    public Set<AttributeModifier> getModifiers() {
        return ImmutableSet.copyOf(this.modifierById.values());
    }

    @Nullable
    public AttributeModifier getModifier(UUID uuid) {
        return this.modifierById.get(uuid);
    }

    public boolean hasModifier(AttributeModifier modifier) {
        return this.modifierById.get(modifier.getId()) != null;
    }

    private void addModifier(AttributeModifier modifier) {
        AttributeModifier attributeModifier = this.modifierById.putIfAbsent(modifier.getId(), modifier);
        if (attributeModifier != null) {
            throw new IllegalArgumentException("Modifier is already applied on this attribute!");
        } else {
            this.getModifiers(modifier.getOperation()).add(modifier);
            this.setDirty();
        }
    }

    public void addTransientModifier(AttributeModifier modifier) {
        this.addModifier(modifier);
    }

    public void addPermanentModifier(AttributeModifier modifier) {
        this.addModifier(modifier);
        this.permanentModifiers.add(modifier);
    }

    protected void setDirty() {
        this.dirty = true;
        this.onDirty.accept(this);
    }

    public void removeModifier(AttributeModifier modifier) {
        this.getModifiers(modifier.getOperation()).remove(modifier);
        this.modifierById.remove(modifier.getId());
        this.permanentModifiers.remove(modifier);
        this.setDirty();
    }

    public void removeModifier(UUID uuid) {
        AttributeModifier attributeModifier = this.getModifier(uuid);
        if (attributeModifier != null) {
            this.removeModifier(attributeModifier);
        }

    }

    public boolean removePermanentModifier(UUID uuid) {
        AttributeModifier attributeModifier = this.getModifier(uuid);
        if (attributeModifier != null && this.permanentModifiers.contains(attributeModifier)) {
            this.removeModifier(attributeModifier);
            return true;
        } else {
            return false;
        }
    }

    public void removeModifiers() {
        for(AttributeModifier attributeModifier : this.getModifiers()) {
            this.removeModifier(attributeModifier);
        }

    }

    public double getValue() {
        if (this.dirty) {
            this.cachedValue = this.calculateValue();
            this.dirty = false;
        }

        return this.cachedValue;
    }

    private double calculateValue() {
        double d = this.getBaseValue();

        for(AttributeModifier attributeModifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADDITION)) {
            d += attributeModifier.getAmount();
        }

        double e = d;

        for(AttributeModifier attributeModifier2 : this.getModifiersOrEmpty(AttributeModifier.Operation.MULTIPLY_BASE)) {
            e += d * attributeModifier2.getAmount();
        }

        for(AttributeModifier attributeModifier3 : this.getModifiersOrEmpty(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
            e *= 1.0D + attributeModifier3.getAmount();
        }

        return this.attribute.sanitizeValue(e);
    }

    private Collection<AttributeModifier> getModifiersOrEmpty(AttributeModifier.Operation operation) {
        return this.modifiersByOperation.getOrDefault(operation, Collections.emptySet());
    }

    public void replaceFrom(AttributeInstance other) {
        this.baseValue = other.baseValue;
        this.modifierById.clear();
        this.modifierById.putAll(other.modifierById);
        this.permanentModifiers.clear();
        this.permanentModifiers.addAll(other.permanentModifiers);
        this.modifiersByOperation.clear();
        other.modifiersByOperation.forEach((operation, modifiers) -> {
            this.getModifiers(operation).addAll(modifiers);
        });
        this.setDirty();
    }

    public CompoundTag save() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("Name", Registry.ATTRIBUTE.getKey(this.attribute).toString());
        compoundTag.putDouble("Base", this.baseValue);
        if (!this.permanentModifiers.isEmpty()) {
            ListTag listTag = new ListTag();

            for(AttributeModifier attributeModifier : this.permanentModifiers) {
                listTag.add(attributeModifier.save());
            }

            compoundTag.put("Modifiers", listTag);
        }

        return compoundTag;
    }

    public void load(CompoundTag nbt) {
        this.baseValue = nbt.getDouble("Base");
        if (nbt.contains("Modifiers", 9)) {
            ListTag listTag = nbt.getList("Modifiers", 10);

            for(int i = 0; i < listTag.size(); ++i) {
                AttributeModifier attributeModifier = AttributeModifier.load(listTag.getCompound(i));
                if (attributeModifier != null) {
                    this.modifierById.put(attributeModifier.getId(), attributeModifier);
                    this.getModifiers(attributeModifier.getOperation()).add(attributeModifier);
                    this.permanentModifiers.add(attributeModifier);
                }
            }
        }

        this.setDirty();
    }
}
