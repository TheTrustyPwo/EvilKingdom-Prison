package net.minecraft.world.entity.animal.horse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class SkeletonTrapGoal extends Goal {
    private final SkeletonHorse horse;

    public SkeletonTrapGoal(SkeletonHorse skeletonHorse) {
        this.horse = skeletonHorse;
    }

    @Override
    public boolean canUse() {
        return this.horse.level.hasNearbyAlivePlayer(this.horse.getX(), this.horse.getY(), this.horse.getZ(), 10.0D);
    }

    @Override
    public void tick() {
        ServerLevel serverLevel = (ServerLevel)this.horse.level;
        DifficultyInstance difficultyInstance = serverLevel.getCurrentDifficultyAt(this.horse.blockPosition());
        this.horse.setTrap(false);
        this.horse.setTamed(true);
        this.horse.setAge(0);
        LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
        lightningBolt.moveTo(this.horse.getX(), this.horse.getY(), this.horse.getZ());
        lightningBolt.setVisualOnly(true);
        serverLevel.addFreshEntity(lightningBolt);
        Skeleton skeleton = this.createSkeleton(difficultyInstance, this.horse);
        skeleton.startRiding(this.horse);
        serverLevel.addFreshEntityWithPassengers(skeleton);

        for(int i = 0; i < 3; ++i) {
            AbstractHorse abstractHorse = this.createHorse(difficultyInstance);
            Skeleton skeleton2 = this.createSkeleton(difficultyInstance, abstractHorse);
            skeleton2.startRiding(abstractHorse);
            abstractHorse.push(this.horse.getRandom().nextGaussian() * 0.5D, 0.0D, this.horse.getRandom().nextGaussian() * 0.5D);
            serverLevel.addFreshEntityWithPassengers(abstractHorse);
        }

    }

    private AbstractHorse createHorse(DifficultyInstance localDifficulty) {
        SkeletonHorse skeletonHorse = EntityType.SKELETON_HORSE.create(this.horse.level);
        skeletonHorse.finalizeSpawn((ServerLevel)this.horse.level, localDifficulty, MobSpawnType.TRIGGERED, (SpawnGroupData)null, (CompoundTag)null);
        skeletonHorse.setPos(this.horse.getX(), this.horse.getY(), this.horse.getZ());
        skeletonHorse.invulnerableTime = 60;
        skeletonHorse.setPersistenceRequired();
        skeletonHorse.setTamed(true);
        skeletonHorse.setAge(0);
        return skeletonHorse;
    }

    private Skeleton createSkeleton(DifficultyInstance localDifficulty, AbstractHorse vehicle) {
        Skeleton skeleton = EntityType.SKELETON.create(vehicle.level);
        skeleton.finalizeSpawn((ServerLevel)vehicle.level, localDifficulty, MobSpawnType.TRIGGERED, (SpawnGroupData)null, (CompoundTag)null);
        skeleton.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
        skeleton.invulnerableTime = 60;
        skeleton.setPersistenceRequired();
        if (skeleton.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        }

        skeleton.setItemSlot(EquipmentSlot.MAINHAND, EnchantmentHelper.enchantItem(skeleton.getRandom(), this.disenchant(skeleton.getMainHandItem()), (int)(5.0F + localDifficulty.getSpecialMultiplier() * (float)skeleton.getRandom().nextInt(18)), false));
        skeleton.setItemSlot(EquipmentSlot.HEAD, EnchantmentHelper.enchantItem(skeleton.getRandom(), this.disenchant(skeleton.getItemBySlot(EquipmentSlot.HEAD)), (int)(5.0F + localDifficulty.getSpecialMultiplier() * (float)skeleton.getRandom().nextInt(18)), false));
        return skeleton;
    }

    private ItemStack disenchant(ItemStack stack) {
        stack.removeTagKey("Enchantments");
        return stack;
    }
}
