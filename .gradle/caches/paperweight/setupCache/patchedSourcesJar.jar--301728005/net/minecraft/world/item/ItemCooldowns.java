package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.Mth;

public class ItemCooldowns {
    public final Map<Item, ItemCooldowns.CooldownInstance> cooldowns = Maps.newHashMap();
    public int tickCount;

    public boolean isOnCooldown(Item item) {
        return this.getCooldownPercent(item, 0.0F) > 0.0F;
    }

    public float getCooldownPercent(Item item, float partialTicks) {
        ItemCooldowns.CooldownInstance cooldownInstance = this.cooldowns.get(item);
        if (cooldownInstance != null) {
            float f = (float)(cooldownInstance.endTime - cooldownInstance.startTime);
            float g = (float)cooldownInstance.endTime - ((float)this.tickCount + partialTicks);
            return Mth.clamp(g / f, 0.0F, 1.0F);
        } else {
            return 0.0F;
        }
    }

    public void tick() {
        ++this.tickCount;
        if (!this.cooldowns.isEmpty()) {
            Iterator<Entry<Item, ItemCooldowns.CooldownInstance>> iterator = this.cooldowns.entrySet().iterator();

            while(iterator.hasNext()) {
                Entry<Item, ItemCooldowns.CooldownInstance> entry = iterator.next();
                if ((entry.getValue()).endTime <= this.tickCount) {
                    iterator.remove();
                    this.onCooldownEnded(entry.getKey());
                }
            }
        }

    }

    public void addCooldown(Item item, int duration) {
        this.cooldowns.put(item, new ItemCooldowns.CooldownInstance(this.tickCount, this.tickCount + duration));
        this.onCooldownStarted(item, duration);
    }

    public void removeCooldown(Item item) {
        this.cooldowns.remove(item);
        this.onCooldownEnded(item);
    }

    protected void onCooldownStarted(Item item, int duration) {
    }

    protected void onCooldownEnded(Item item) {
    }

    public static class CooldownInstance {
        final int startTime;
        public final int endTime;

        CooldownInstance(int startTick, int endTick) {
            this.startTime = startTick;
            this.endTime = endTick;
        }
    }
}
