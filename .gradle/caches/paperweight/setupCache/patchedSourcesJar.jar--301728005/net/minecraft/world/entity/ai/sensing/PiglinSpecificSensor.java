package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PiglinSpecificSensor extends Sensor<LivingEntity> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, MemoryModuleType.NEARBY_ADULT_PIGLINS, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, MemoryModuleType.NEAREST_REPELLENT);
    }

    @Override
    protected void doTick(ServerLevel world, LivingEntity entity) {
        Brain<?> brain = entity.getBrain();
        brain.setMemory(MemoryModuleType.NEAREST_REPELLENT, findNearestRepellent(world, entity));
        Optional<Mob> optional = Optional.empty();
        Optional<Hoglin> optional2 = Optional.empty();
        Optional<Hoglin> optional3 = Optional.empty();
        Optional<Piglin> optional4 = Optional.empty();
        Optional<LivingEntity> optional5 = Optional.empty();
        Optional<Player> optional6 = Optional.empty();
        Optional<Player> optional7 = Optional.empty();
        int i = 0;
        List<AbstractPiglin> list = Lists.newArrayList();
        List<AbstractPiglin> list2 = Lists.newArrayList();
        NearestVisibleLivingEntities nearestVisibleLivingEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty());

        for(LivingEntity livingEntity : nearestVisibleLivingEntities.findAll((livingEntityx) -> {
            return true;
        })) {
            if (livingEntity instanceof Hoglin) {
                Hoglin hoglin = (Hoglin)livingEntity;
                if (hoglin.isBaby() && optional3.isEmpty()) {
                    optional3 = Optional.of(hoglin);
                } else if (hoglin.isAdult()) {
                    ++i;
                    if (optional2.isEmpty() && hoglin.canBeHunted()) {
                        optional2 = Optional.of(hoglin);
                    }
                }
            } else if (livingEntity instanceof PiglinBrute) {
                PiglinBrute piglinBrute = (PiglinBrute)livingEntity;
                list.add(piglinBrute);
            } else if (livingEntity instanceof Piglin) {
                Piglin piglin = (Piglin)livingEntity;
                if (piglin.isBaby() && optional4.isEmpty()) {
                    optional4 = Optional.of(piglin);
                } else if (piglin.isAdult()) {
                    list.add(piglin);
                }
            } else if (livingEntity instanceof Player) {
                Player player = (Player)livingEntity;
                if (optional6.isEmpty() && !PiglinAi.isWearingGold(player) && entity.canAttack(livingEntity)) {
                    optional6 = Optional.of(player);
                }

                if (optional7.isEmpty() && !player.isSpectator() && PiglinAi.isPlayerHoldingLovedItem(player)) {
                    optional7 = Optional.of(player);
                }
            } else if (!optional.isEmpty() || !(livingEntity instanceof WitherSkeleton) && !(livingEntity instanceof WitherBoss)) {
                if (optional5.isEmpty() && PiglinAi.isZombified(livingEntity.getType())) {
                    optional5 = Optional.of(livingEntity);
                }
            } else {
                optional = Optional.of((Mob)livingEntity);
            }
        }

        for(LivingEntity livingEntity2 : brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(ImmutableList.of())) {
            if (livingEntity2 instanceof AbstractPiglin) {
                AbstractPiglin abstractPiglin = (AbstractPiglin)livingEntity2;
                if (abstractPiglin.isAdult()) {
                    list2.add(abstractPiglin);
                }
            }
        }

        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, optional);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, optional2);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, optional3);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, optional5);
        brain.setMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, optional6);
        brain.setMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, optional7);
        brain.setMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS, list2);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, list);
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, list.size());
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, i);
    }

    private static Optional<BlockPos> findNearestRepellent(ServerLevel world, LivingEntity entity) {
        return BlockPos.findClosestMatch(entity.blockPosition(), 8, 4, (pos) -> {
            return isValidRepellent(world, pos);
        });
    }

    private static boolean isValidRepellent(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        boolean bl = blockState.is(BlockTags.PIGLIN_REPELLENTS);
        return bl && blockState.is(Blocks.SOUL_CAMPFIRE) ? CampfireBlock.isLitCampfire(blockState) : bl;
    }
}
