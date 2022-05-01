package net.minecraft.world.entity.npc;

import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;

public class WanderingTraderSpawner implements CustomSpawner {

    private static final int DEFAULT_TICK_DELAY = 1200;
    public static final int DEFAULT_SPAWN_DELAY = 24000;
    private static final int MIN_SPAWN_CHANCE = 25;
    private static final int MAX_SPAWN_CHANCE = 75;
    private static final int SPAWN_CHANCE_INCREASE = 25;
    private static final int SPAWN_ONE_IN_X_CHANCE = 10;
    private static final int NUMBER_OF_SPAWN_ATTEMPTS = 10;
    private final Random random = new Random();
    private final ServerLevelData serverLevelData;
    private int tickDelay;
    private int spawnDelay;
    private int spawnChance;

    public WanderingTraderSpawner(ServerLevelData properties) {
        this.serverLevelData = properties;
        // Paper start
        this.tickDelay = Integer.MIN_VALUE;
        //this.spawnDelay = properties.getWanderingTraderSpawnDelay(); // Paper - This value is read from the world file only for the first spawn, after which vanilla uses a hardcoded value
        //this.spawnChance = properties.getWanderingTraderSpawnChance(); // Paper - This value is read from the world file only for the first spawn, after which vanilla uses a hardcoded value
        //if (this.spawnDelay == 0 && this.spawnChance == 0) {
        //    this.spawnDelay = 24000;
        //    properties.setWanderingTraderSpawnDelay(this.spawnDelay);
        //    this.spawnChance = 25;
        //    properties.setWanderingTraderSpawnChance(this.spawnChance);
        //}
        // Paper end

    }

    @Override
    public int tick(ServerLevel world, boolean spawnMonsters, boolean spawnAnimals) {
        // Paper start
        if (this.tickDelay == Integer.MIN_VALUE) {
            this.tickDelay = world.paperConfig.wanderingTraderSpawnMinuteTicks;
            this.spawnDelay = world.paperConfig.wanderingTraderSpawnDayTicks;
            this.spawnChance = world.paperConfig.wanderingTraderSpawnChanceMin;
        }
        if (!world.getGameRules().getBoolean(GameRules.RULE_DO_TRADER_SPAWNING)) {
            return 0;
        } else if (this.tickDelay - 1 > 0) {
            this.tickDelay = this.tickDelay - 1;
            return 0;
        } else {
            this.tickDelay = world.paperConfig.wanderingTraderSpawnMinuteTicks;
            this.spawnDelay = this.spawnDelay - world.paperConfig.wanderingTraderSpawnMinuteTicks;
            //this.serverLevelData.setWanderingTraderSpawnDelay(this.spawnDelay); // Paper - We don't need to save this value to disk if it gets set back to a hardcoded value anyways
            if (this.spawnDelay > 0) {
                return 0;
            } else {
                this.spawnDelay = world.paperConfig.wanderingTraderSpawnDayTicks;
                if (!world.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
                    return 0;
                } else {
                    int i = this.spawnChance;

                    this.spawnChance = Mth.clamp(i + world.paperConfig.wanderingTraderSpawnChanceFailureIncrement, world.paperConfig.wanderingTraderSpawnChanceMin, world.paperConfig.wanderingTraderSpawnChanceMax);
                    //this.serverLevelData.setWanderingTraderSpawnChance(this.spawnChance); // Paper - We don't need to save this value to disk if it gets set back to a hardcoded value anyways
                    if (this.random.nextInt(100) > i) {
                        return 0;
                    } else if (this.spawn(world)) {
                        this.spawnChance = world.paperConfig.wanderingTraderSpawnChanceMin;
                        // Paper end
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }
        }
    }

    private boolean spawn(ServerLevel world) {
        ServerPlayer entityplayer = world.getRandomPlayer();

        if (entityplayer == null) {
            return true;
        } else if (this.random.nextInt(10) != 0) {
            return false;
        } else {
            BlockPos blockposition = entityplayer.blockPosition();
            boolean flag = true;
            PoiManager villageplace = world.getPoiManager();
            Optional<BlockPos> optional = villageplace.find(PoiType.MEETING.getPredicate(), (blockposition1) -> {
                return true;
            }, blockposition, 48, PoiManager.Occupancy.ANY);
            BlockPos blockposition1 = (BlockPos) optional.orElse(blockposition);
            BlockPos blockposition2 = this.findSpawnPositionNear(world, blockposition1, 48);

            if (blockposition2 != null && this.hasEnoughSpace(world, blockposition2)) {
                if (world.getBiome(blockposition2).is(Biomes.THE_VOID)) {
                    return false;
                }

                WanderingTrader entityvillagertrader = (WanderingTrader) EntityType.WANDERING_TRADER.spawn(world, (CompoundTag) null, (Component) null, (Player) null, blockposition2, MobSpawnType.EVENT, false, false, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL, trader -> trader.setDespawnDelay(48000)); // CraftBukkit // Paper - set despawnTimer before spawn events called

                if (entityvillagertrader != null) {
                    for (int i = 0; i < 2; ++i) {
                        this.tryToSpawnLlamaFor(world, entityvillagertrader, 4);
                    }

                    this.serverLevelData.setWanderingTraderId(entityvillagertrader.getUUID());
                    // entityvillagertrader.setDespawnDelay(48000); // CraftBukkit - moved to EntityVillagerTrader constructor. This lets the value be modified by plugins on CreatureSpawnEvent
                    entityvillagertrader.setWanderTarget(blockposition1);
                    entityvillagertrader.restrictTo(blockposition1, 16);
                    return true;
                }
            }

            return false;
        }
    }

    private void tryToSpawnLlamaFor(ServerLevel world, WanderingTrader wanderingTrader, int range) {
        BlockPos blockposition = this.findSpawnPositionNear(world, wanderingTrader.blockPosition(), range);

        if (blockposition != null) {
            TraderLlama entityllamatrader = (TraderLlama) EntityType.TRADER_LLAMA.spawn(world, (CompoundTag) null, (Component) null, (Player) null, blockposition, MobSpawnType.EVENT, false, false, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL); // CraftBukkit

            if (entityllamatrader != null) {
                entityllamatrader.setLeashedTo(wanderingTrader, true);
            }
        }
    }

    @Nullable
    private BlockPos findSpawnPositionNear(LevelReader world, BlockPos pos, int range) {
        BlockPos blockposition1 = null;

        for (int j = 0; j < 10; ++j) {
            int k = pos.getX() + this.random.nextInt(range * 2) - range;
            int l = pos.getZ() + this.random.nextInt(range * 2) - range;
            int i1 = world.getHeight(Heightmap.Types.WORLD_SURFACE, k, l);
            BlockPos blockposition2 = new BlockPos(k, i1, l);

            if (NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, world, blockposition2, EntityType.WANDERING_TRADER)) {
                blockposition1 = blockposition2;
                break;
            }
        }

        return blockposition1;
    }

    private boolean hasEnoughSpace(BlockGetter world, BlockPos pos) {
        Iterator iterator = BlockPos.betweenClosed(pos, pos.offset(1, 2, 1)).iterator();

        BlockPos blockposition1;

        do {
            if (!iterator.hasNext()) {
                return true;
            }

            blockposition1 = (BlockPos) iterator.next();
        } while (world.getBlockState(blockposition1).getCollisionShape(world, blockposition1).isEmpty());

        return false;
    }
}
