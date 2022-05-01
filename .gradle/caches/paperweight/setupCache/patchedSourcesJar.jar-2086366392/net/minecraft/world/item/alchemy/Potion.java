package net.minecraft.world.item.alchemy;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

public class Potion {
    @Nullable
    private final String name;
    private final ImmutableList<MobEffectInstance> effects;

    public static Potion byName(String id) {
        return Registry.POTION.get(ResourceLocation.tryParse(id));
    }

    public Potion(MobEffectInstance... effects) {
        this((String)null, effects);
    }

    public Potion(@Nullable String baseName, MobEffectInstance... effects) {
        this.name = baseName;
        this.effects = ImmutableList.copyOf(effects);
    }

    public String getName(String prefix) {
        return prefix + (this.name == null ? Registry.POTION.getKey(this).getPath() : this.name);
    }

    public List<MobEffectInstance> getEffects() {
        return this.effects;
    }

    public boolean hasInstantEffects() {
        if (!this.effects.isEmpty()) {
            for(MobEffectInstance mobEffectInstance : this.effects) {
                if (mobEffectInstance.getEffect().isInstantenous()) {
                    return true;
                }
            }
        }

        return false;
    }
}
