package net.minecraft.world.effect;

import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.LivingEntity;

public final class MobEffectUtil {
    public static String formatDuration(MobEffectInstance effect, float multiplier) {
        if (effect.isNoCounter()) {
            return "**:**";
        } else {
            int i = Mth.floor((float)effect.getDuration() * multiplier);
            return StringUtil.formatTickDuration(i);
        }
    }

    public static boolean hasDigSpeed(LivingEntity entity) {
        return entity.hasEffect(MobEffects.DIG_SPEED) || entity.hasEffect(MobEffects.CONDUIT_POWER);
    }

    public static int getDigSpeedAmplification(LivingEntity entity) {
        int i = 0;
        int j = 0;
        if (entity.hasEffect(MobEffects.DIG_SPEED)) {
            i = entity.getEffect(MobEffects.DIG_SPEED).getAmplifier();
        }

        if (entity.hasEffect(MobEffects.CONDUIT_POWER)) {
            j = entity.getEffect(MobEffects.CONDUIT_POWER).getAmplifier();
        }

        return Math.max(i, j);
    }

    public static boolean hasWaterBreathing(LivingEntity entity) {
        return entity.hasEffect(MobEffects.WATER_BREATHING) || entity.hasEffect(MobEffects.CONDUIT_POWER);
    }
}
