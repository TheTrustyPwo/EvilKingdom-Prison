package net.minecraft.advancements;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public enum FrameType {
    TASK("task", 0, ChatFormatting.GREEN),
    CHALLENGE("challenge", 26, ChatFormatting.DARK_PURPLE),
    GOAL("goal", 52, ChatFormatting.GREEN);

    private final String name;
    private final int texture;
    private final ChatFormatting chatColor;
    private final Component displayName;

    private FrameType(String id, int texV, ChatFormatting titleFormat) {
        this.name = id;
        this.texture = texV;
        this.chatColor = titleFormat;
        this.displayName = new TranslatableComponent("advancements.toast." + id);
    }

    public String getName() {
        return this.name;
    }

    public int getTexture() {
        return this.texture;
    }

    public static FrameType byName(String name) {
        for(FrameType frameType : values()) {
            if (frameType.name.equals(name)) {
                return frameType;
            }
        }

        throw new IllegalArgumentException("Unknown frame type '" + name + "'");
    }

    public ChatFormatting getChatColor() {
        return this.chatColor;
    }

    public Component getDisplayName() {
        return this.displayName;
    }
}
