package net.minecraft.world.entity;

public enum EquipmentSlot {
    MAINHAND(EquipmentSlot.Type.HAND, 0, 0, "mainhand"),
    OFFHAND(EquipmentSlot.Type.HAND, 1, 5, "offhand"),
    FEET(EquipmentSlot.Type.ARMOR, 0, 1, "feet"),
    LEGS(EquipmentSlot.Type.ARMOR, 1, 2, "legs"),
    CHEST(EquipmentSlot.Type.ARMOR, 2, 3, "chest"),
    HEAD(EquipmentSlot.Type.ARMOR, 3, 4, "head");

    private final EquipmentSlot.Type type;
    private final int index;
    private final int filterFlag;
    private final String name;

    private EquipmentSlot(EquipmentSlot.Type type, int entityId, int armorStandId, String name) {
        this.type = type;
        this.index = entityId;
        this.filterFlag = armorStandId;
        this.name = name;
    }

    public EquipmentSlot.Type getType() {
        return this.type;
    }

    public int getIndex() {
        return this.index;
    }

    public int getIndex(int offset) {
        return offset + this.index;
    }

    public int getFilterFlag() {
        return this.filterFlag;
    }

    public String getName() {
        return this.name;
    }

    public static EquipmentSlot byName(String name) {
        for(EquipmentSlot equipmentSlot : values()) {
            if (equipmentSlot.getName().equals(name)) {
                return equipmentSlot;
            }
        }

        throw new IllegalArgumentException("Invalid slot '" + name + "'");
    }

    public static EquipmentSlot byTypeAndIndex(EquipmentSlot.Type type, int index) {
        for(EquipmentSlot equipmentSlot : values()) {
            if (equipmentSlot.getType() == type && equipmentSlot.getIndex() == index) {
                return equipmentSlot;
            }
        }

        throw new IllegalArgumentException("Invalid slot '" + type + "': " + index);
    }

    public static enum Type {
        HAND,
        ARMOR;
    }
}
