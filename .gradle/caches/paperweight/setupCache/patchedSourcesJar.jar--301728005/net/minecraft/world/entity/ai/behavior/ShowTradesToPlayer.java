package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

public class ShowTradesToPlayer extends Behavior<Villager> {
    private static final int MAX_LOOK_TIME = 900;
    private static final int STARTING_LOOK_TIME = 40;
    @Nullable
    private ItemStack playerItemStack;
    private final List<ItemStack> displayItems = Lists.newArrayList();
    private int cycleCounter;
    private int displayIndex;
    private int lookTime;

    public ShowTradesToPlayer(int minRunTime, int maxRunTime) {
        super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT), minRunTime, maxRunTime);
    }

    @Override
    public boolean checkExtraStartConditions(ServerLevel world, Villager entity) {
        Brain<?> brain = entity.getBrain();
        if (!brain.getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent()) {
            return false;
        } else {
            LivingEntity livingEntity = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
            return livingEntity.getType() == EntityType.PLAYER && entity.isAlive() && livingEntity.isAlive() && !entity.isBaby() && entity.distanceToSqr(livingEntity) <= 17.0D;
        }
    }

    @Override
    public boolean canStillUse(ServerLevel serverLevel, Villager villager, long l) {
        return this.checkExtraStartConditions(serverLevel, villager) && this.lookTime > 0 && villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
    }

    @Override
    public void start(ServerLevel serverLevel, Villager villager, long l) {
        super.start(serverLevel, villager, l);
        this.lookAtTarget(villager);
        this.cycleCounter = 0;
        this.displayIndex = 0;
        this.lookTime = 40;
    }

    @Override
    public void tick(ServerLevel serverLevel, Villager villager, long l) {
        LivingEntity livingEntity = this.lookAtTarget(villager);
        this.findItemsToDisplay(livingEntity, villager);
        if (!this.displayItems.isEmpty()) {
            this.displayCyclingItems(villager);
        } else {
            clearHeldItem(villager);
            this.lookTime = Math.min(this.lookTime, 40);
        }

        --this.lookTime;
    }

    @Override
    public void stop(ServerLevel serverLevel, Villager villager, long l) {
        super.stop(serverLevel, villager, l);
        villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
        clearHeldItem(villager);
        this.playerItemStack = null;
    }

    private void findItemsToDisplay(LivingEntity customer, Villager villager) {
        boolean bl = false;
        ItemStack itemStack = customer.getMainHandItem();
        if (this.playerItemStack == null || !ItemStack.isSame(this.playerItemStack, itemStack)) {
            this.playerItemStack = itemStack;
            bl = true;
            this.displayItems.clear();
        }

        if (bl && !this.playerItemStack.isEmpty()) {
            this.updateDisplayItems(villager);
            if (!this.displayItems.isEmpty()) {
                this.lookTime = 900;
                this.displayFirstItem(villager);
            }
        }

    }

    private void displayFirstItem(Villager villager) {
        displayAsHeldItem(villager, this.displayItems.get(0));
    }

    private void updateDisplayItems(Villager villager) {
        for(MerchantOffer merchantOffer : villager.getOffers()) {
            if (!merchantOffer.isOutOfStock() && this.playerItemStackMatchesCostOfOffer(merchantOffer)) {
                this.displayItems.add(merchantOffer.getResult());
            }
        }

    }

    private boolean playerItemStackMatchesCostOfOffer(MerchantOffer offer) {
        return ItemStack.isSame(this.playerItemStack, offer.getCostA()) || ItemStack.isSame(this.playerItemStack, offer.getCostB());
    }

    private static void clearHeldItem(Villager villager) {
        villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
    }

    private static void displayAsHeldItem(Villager villager, ItemStack stack) {
        villager.setItemSlot(EquipmentSlot.MAINHAND, stack);
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private LivingEntity lookAtTarget(Villager villager) {
        Brain<?> brain = villager.getBrain();
        LivingEntity livingEntity = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingEntity, true));
        return livingEntity;
    }

    private void displayCyclingItems(Villager villager) {
        if (this.displayItems.size() >= 2 && ++this.cycleCounter >= 40) {
            ++this.displayIndex;
            this.cycleCounter = 0;
            if (this.displayIndex > this.displayItems.size() - 1) {
                this.displayIndex = 0;
            }

            displayAsHeldItem(villager, this.displayItems.get(this.displayIndex));
        }

    }
}
