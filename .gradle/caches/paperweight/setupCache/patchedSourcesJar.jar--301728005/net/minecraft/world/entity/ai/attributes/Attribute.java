package net.minecraft.world.entity.ai.attributes;

public class Attribute {
    public static final int MAX_NAME_LENGTH = 64;
    private final double defaultValue;
    private boolean syncable;
    private final String descriptionId;

    protected Attribute(String translationKey, double fallback) {
        this.defaultValue = fallback;
        this.descriptionId = translationKey;
    }

    public double getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isClientSyncable() {
        return this.syncable;
    }

    public Attribute setSyncable(boolean tracked) {
        this.syncable = tracked;
        return this;
    }

    public double sanitizeValue(double value) {
        return value;
    }

    public String getDescriptionId() {
        return this.descriptionId;
    }
}
