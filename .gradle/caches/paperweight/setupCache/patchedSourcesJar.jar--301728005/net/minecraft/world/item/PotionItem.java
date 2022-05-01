package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class PotionItem extends Item {

    private static final int DRINK_DURATION = 32;

    public PotionItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public ItemStack getDefaultInstance() {
        return PotionUtils.setPotion(super.getDefaultInstance(), Potions.WATER);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        Player entityhuman = user instanceof Player ? (Player) user : null;

        if (entityhuman instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer) entityhuman, stack);
        }

        List<MobEffectInstance> instantLater = new java.util.ArrayList<>(); // Paper - Fix harming potion dupe
        if (!world.isClientSide) {
            List<MobEffectInstance> list = PotionUtils.getMobEffects(stack);
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                MobEffectInstance mobeffect = (MobEffectInstance) iterator.next();

                if (mobeffect.getEffect().isInstantenous()) {
                    instantLater.add(mobeffect); // Paper - Fix harming potion dupe
                } else {
                    user.addEffect(new MobEffectInstance(mobeffect), org.bukkit.event.entity.EntityPotionEffectEvent.Cause.POTION_DRINK); // CraftBukkit
                }
            }
        }

        if (entityhuman != null) {
            entityhuman.awardStat(Stats.ITEM_USED.get(this));
            if (!entityhuman.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        // Paper start - Fix harming potion dupe
        for (MobEffectInstance mobeffect : instantLater) {
            mobeffect.getEffect().applyInstantenousEffect(entityhuman, entityhuman, user, mobeffect.getAmplifier(), 1.0D);
        }
        // Paper end
        if (entityhuman == null || !entityhuman.getAbilities().instabuild) {
            // Paper start - Fix harming potion dupe
            if (user.getHealth() <= 0 && !user.level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
                user.spawnAtLocation(new ItemStack(Items.GLASS_BOTTLE), 0);
                return ItemStack.EMPTY;
            }
            // Paper end
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (entityhuman != null) {
                entityhuman.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        world.gameEvent(user, GameEvent.DRINKING_FINISH, user.eyeBlockPosition());
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(world, user, hand);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return PotionUtils.getPotion(stack).getName(this.getDescriptionId() + ".effect.");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        PotionUtils.addPotionTooltip(stack, tooltip, 1.0F);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || !PotionUtils.getMobEffects(stack).isEmpty();
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(group)) {
            Iterator iterator = Registry.POTION.iterator();

            while (iterator.hasNext()) {
                Potion potionregistry = (Potion) iterator.next();

                if (potionregistry != Potions.EMPTY) {
                    stacks.add(PotionUtils.setPotion(new ItemStack(this), potionregistry));
                }
            }
        }

    }
}
