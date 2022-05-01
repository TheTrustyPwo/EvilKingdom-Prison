package net.minecraft.world.item;

// Paper start
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.event.player.PlayerNameEntityEvent;
// Paper end
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class NameTagItem extends Item {
    public NameTagItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if (stack.hasCustomHoverName() && !(entity instanceof Player)) {
            if (!user.level.isClientSide && entity.isAlive()) {
                // Paper start
                PlayerNameEntityEvent event = new PlayerNameEntityEvent(((net.minecraft.server.level.ServerPlayer) user).getBukkitEntity(), entity.getBukkitLivingEntity(), PaperAdventure.asAdventure(stack.getHoverName()), true);
                if (!event.callEvent()) return InteractionResult.PASS;
                LivingEntity newEntityLiving = ((org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity) event.getEntity()).getHandle();
                newEntityLiving.setCustomName(event.getName() != null ? PaperAdventure.asVanilla(event.getName()) : null);
                if (event.isPersistent() && newEntityLiving instanceof Mob) {
                    ((Mob) newEntityLiving).setPersistenceRequired();
                // Paper end
                }

                stack.shrink(1);
            }

            return InteractionResult.sidedSuccess(user.level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }
}
