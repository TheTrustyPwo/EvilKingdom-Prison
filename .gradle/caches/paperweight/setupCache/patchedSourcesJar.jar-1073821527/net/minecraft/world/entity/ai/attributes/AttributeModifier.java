package net.minecraft.world.entity.ai.attributes;

import com.mojang.logging.LogUtils;
import io.netty.util.internal.ThreadLocalRandom;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public class AttributeModifier {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final double amount;
    private final AttributeModifier.Operation operation;
    private final Supplier<String> nameGetter;
    private final UUID id;

    public AttributeModifier(String name, double value, AttributeModifier.Operation operation) {
        this(Mth.createInsecureUUID(ThreadLocalRandom.current()), () -> {
            return name;
        }, value, operation);
    }

    public AttributeModifier(UUID uuid, String name, double value, AttributeModifier.Operation operation) {
        this(uuid, () -> {
            return name;
        }, value, operation);
    }

    public AttributeModifier(UUID uuid, Supplier<String> nameGetter, double value, AttributeModifier.Operation operation) {
        this.id = uuid;
        this.nameGetter = nameGetter;
        this.amount = value;
        this.operation = operation;
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.nameGetter.get();
    }

    public AttributeModifier.Operation getOperation() {
        return this.operation;
    }

    public double getAmount() {
        return this.amount;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            AttributeModifier attributeModifier = (AttributeModifier)object;
            return Objects.equals(this.id, attributeModifier.id);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "AttributeModifier{amount=" + this.amount + ", operation=" + this.operation + ", name='" + (String)this.nameGetter.get() + "', id=" + this.id + "}";
    }

    public CompoundTag save() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("Name", this.getName());
        compoundTag.putDouble("Amount", this.amount);
        compoundTag.putInt("Operation", this.operation.toValue());
        compoundTag.putUUID("UUID", this.id);
        return compoundTag;
    }

    @Nullable
    public static AttributeModifier load(CompoundTag nbt) {
        try {
            UUID uUID = nbt.getUUID("UUID");
            AttributeModifier.Operation operation = AttributeModifier.Operation.fromValue(nbt.getInt("Operation"));
            return new AttributeModifier(uUID, nbt.getString("Name"), nbt.getDouble("Amount"), operation);
        } catch (Exception var3) {
            LOGGER.warn("Unable to create attribute: {}", (Object)var3.getMessage());
            return null;
        }
    }

    public static enum Operation {
        ADDITION(0),
        MULTIPLY_BASE(1),
        MULTIPLY_TOTAL(2);

        private static final AttributeModifier.Operation[] OPERATIONS = new AttributeModifier.Operation[]{ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL};
        private final int value;

        private Operation(int id) {
            this.value = id;
        }

        public int toValue() {
            return this.value;
        }

        public static AttributeModifier.Operation fromValue(int id) {
            if (id >= 0 && id < OPERATIONS.length) {
                return OPERATIONS[id];
            } else {
                throw new IllegalArgumentException("No operation with value " + id);
            }
        }
    }
}
