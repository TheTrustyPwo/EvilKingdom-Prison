package net.minecraft.world;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public abstract class BossEvent {
    private final UUID id;
    public Component name;
    protected float progress;
    public BossEvent.BossBarColor color;
    public BossEvent.BossBarOverlay overlay;
    protected boolean darkenScreen;
    protected boolean playBossMusic;
    protected boolean createWorldFog;

    public BossEvent(UUID uuid, Component name, BossEvent.BossBarColor color, BossEvent.BossBarOverlay style) {
        this.id = uuid;
        this.name = name;
        this.color = color;
        this.overlay = style;
        this.progress = 1.0F;
    }

    public UUID getId() {
        return this.id;
    }

    public Component getName() {
        return this.name;
    }

    public void setName(Component name) {
        this.name = name;
    }

    public float getProgress() {
        return this.progress;
    }

    public void setProgress(float percent) {
        this.progress = percent;
    }

    public BossEvent.BossBarColor getColor() {
        return this.color;
    }

    public void setColor(BossEvent.BossBarColor color) {
        this.color = color;
    }

    public BossEvent.BossBarOverlay getOverlay() {
        return this.overlay;
    }

    public void setOverlay(BossEvent.BossBarOverlay style) {
        this.overlay = style;
    }

    public boolean shouldDarkenScreen() {
        return this.darkenScreen;
    }

    public BossEvent setDarkenScreen(boolean darkenSky) {
        this.darkenScreen = darkenSky;
        return this;
    }

    public boolean shouldPlayBossMusic() {
        return this.playBossMusic;
    }

    public BossEvent setPlayBossMusic(boolean dragonMusic) {
        this.playBossMusic = dragonMusic;
        return this;
    }

    public BossEvent setCreateWorldFog(boolean thickenFog) {
        this.createWorldFog = thickenFog;
        return this;
    }

    public boolean shouldCreateWorldFog() {
        return this.createWorldFog;
    }

    public static enum BossBarColor {
        PINK("pink", ChatFormatting.RED),
        BLUE("blue", ChatFormatting.BLUE),
        RED("red", ChatFormatting.DARK_RED),
        GREEN("green", ChatFormatting.GREEN),
        YELLOW("yellow", ChatFormatting.YELLOW),
        PURPLE("purple", ChatFormatting.DARK_BLUE),
        WHITE("white", ChatFormatting.WHITE);

        private final String name;
        private final ChatFormatting formatting;

        private BossBarColor(String name, ChatFormatting format) {
            this.name = name;
            this.formatting = format;
        }

        public ChatFormatting getFormatting() {
            return this.formatting;
        }

        public String getName() {
            return this.name;
        }

        public static BossEvent.BossBarColor byName(String name) {
            for(BossEvent.BossBarColor bossBarColor : values()) {
                if (bossBarColor.name.equals(name)) {
                    return bossBarColor;
                }
            }

            return WHITE;
        }
    }

    public static enum BossBarOverlay {
        PROGRESS("progress"),
        NOTCHED_6("notched_6"),
        NOTCHED_10("notched_10"),
        NOTCHED_12("notched_12"),
        NOTCHED_20("notched_20");

        private final String name;

        private BossBarOverlay(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static BossEvent.BossBarOverlay byName(String name) {
            for(BossEvent.BossBarOverlay bossBarOverlay : values()) {
                if (bossBarOverlay.name.equals(name)) {
                    return bossBarOverlay;
                }
            }

            return PROGRESS;
        }
    }
}
