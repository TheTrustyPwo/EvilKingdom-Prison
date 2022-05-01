package net.minecraft.world.level.dimension.end;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;

public enum DragonRespawnAnimation {
    START {
        @Override
        public void tick(ServerLevel world, EndDragonFight fight, List<EndCrystal> crystals, int tick, BlockPos pos) {
            BlockPos blockPos = new BlockPos(0, 128, 0);

            for(EndCrystal endCrystal : crystals) {
                endCrystal.setBeamTarget(blockPos);
            }

            fight.setRespawnStage(PREPARING_TO_SUMMON_PILLARS);
        }
    },
    PREPARING_TO_SUMMON_PILLARS {
        @Override
        public void tick(ServerLevel world, EndDragonFight fight, List<EndCrystal> crystals, int tick, BlockPos pos) {
            if (tick < 100) {
                if (tick == 0 || tick == 50 || tick == 51 || tick == 52 || tick >= 95) {
                    world.levelEvent(3001, new BlockPos(0, 128, 0), 0);
                }
            } else {
                fight.setRespawnStage(SUMMONING_PILLARS);
            }

        }
    },
    SUMMONING_PILLARS {
        @Override
        public void tick(ServerLevel world, EndDragonFight fight, List<EndCrystal> crystals, int tick, BlockPos pos) {
            int i = 40;
            boolean bl = tick % 40 == 0;
            boolean bl2 = tick % 40 == 39;
            if (bl || bl2) {
                List<SpikeFeature.EndSpike> list = SpikeFeature.getSpikesForLevel(world);
                int j = tick / 40;
                if (j < list.size()) {
                    SpikeFeature.EndSpike endSpike = list.get(j);
                    if (bl) {
                        for(EndCrystal endCrystal : crystals) {
                            endCrystal.setBeamTarget(new BlockPos(endSpike.getCenterX(), endSpike.getHeight() + 1, endSpike.getCenterZ()));
                        }
                    } else {
                        int k = 10;

                        for(BlockPos blockPos : BlockPos.betweenClosed(new BlockPos(endSpike.getCenterX() - 10, endSpike.getHeight() - 10, endSpike.getCenterZ() - 10), new BlockPos(endSpike.getCenterX() + 10, endSpike.getHeight() + 10, endSpike.getCenterZ() + 10))) {
                            world.removeBlock(blockPos, false);
                        }

                        world.explode((Entity)null, (double)((float)endSpike.getCenterX() + 0.5F), (double)endSpike.getHeight(), (double)((float)endSpike.getCenterZ() + 0.5F), 5.0F, Explosion.BlockInteraction.DESTROY);
                        SpikeConfiguration spikeConfiguration = new SpikeConfiguration(true, ImmutableList.of(endSpike), new BlockPos(0, 128, 0));
                        Feature.END_SPIKE.place(spikeConfiguration, world, world.getChunkSource().getGenerator(), new Random(), new BlockPos(endSpike.getCenterX(), 45, endSpike.getCenterZ()));
                    }
                } else if (bl) {
                    fight.setRespawnStage(SUMMONING_DRAGON);
                }
            }

        }
    },
    SUMMONING_DRAGON {
        @Override
        public void tick(ServerLevel world, EndDragonFight fight, List<EndCrystal> crystals, int tick, BlockPos pos) {
            if (tick >= 100) {
                fight.setRespawnStage(END);
                fight.resetSpikeCrystals();

                for(EndCrystal endCrystal : crystals) {
                    endCrystal.setBeamTarget((BlockPos)null);
                    world.explode(endCrystal, endCrystal.getX(), endCrystal.getY(), endCrystal.getZ(), 6.0F, Explosion.BlockInteraction.NONE);
                    endCrystal.discard();
                }
            } else if (tick >= 80) {
                world.levelEvent(3001, new BlockPos(0, 128, 0), 0);
            } else if (tick == 0) {
                for(EndCrystal endCrystal2 : crystals) {
                    endCrystal2.setBeamTarget(new BlockPos(0, 128, 0));
                }
            } else if (tick < 5) {
                world.levelEvent(3001, new BlockPos(0, 128, 0), 0);
            }

        }
    },
    END {
        @Override
        public void tick(ServerLevel world, EndDragonFight fight, List<EndCrystal> crystals, int tick, BlockPos pos) {
        }
    };

    public abstract void tick(ServerLevel world, EndDragonFight fight, List<EndCrystal> crystals, int tick, BlockPos pos);
}
