package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.LargeDripstoneConfiguration;
import net.minecraft.world.phys.Vec3;

public class LargeDripstoneFeature extends Feature<LargeDripstoneConfiguration> {
    public LargeDripstoneFeature(Codec<LargeDripstoneConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<LargeDripstoneConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        LargeDripstoneConfiguration largeDripstoneConfiguration = context.config();
        Random random = context.random();
        if (!DripstoneUtils.isEmptyOrWater(worldGenLevel, blockPos)) {
            return false;
        } else {
            Optional<Column> optional = Column.scan(worldGenLevel, blockPos, largeDripstoneConfiguration.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isDripstoneBaseOrLava);
            if (optional.isPresent() && optional.get() instanceof Column.Range) {
                Column.Range range = (Column.Range)optional.get();
                if (range.height() < 4) {
                    return false;
                } else {
                    int i = (int)((float)range.height() * largeDripstoneConfiguration.maxColumnRadiusToCaveHeightRatio);
                    int j = Mth.clamp(i, largeDripstoneConfiguration.columnRadius.getMinValue(), largeDripstoneConfiguration.columnRadius.getMaxValue());
                    int k = Mth.randomBetweenInclusive(random, largeDripstoneConfiguration.columnRadius.getMinValue(), j);
                    LargeDripstoneFeature.LargeDripstone largeDripstone = makeDripstone(blockPos.atY(range.ceiling() - 1), false, random, k, largeDripstoneConfiguration.stalactiteBluntness, largeDripstoneConfiguration.heightScale);
                    LargeDripstoneFeature.LargeDripstone largeDripstone2 = makeDripstone(blockPos.atY(range.floor() + 1), true, random, k, largeDripstoneConfiguration.stalagmiteBluntness, largeDripstoneConfiguration.heightScale);
                    LargeDripstoneFeature.WindOffsetter windOffsetter;
                    if (largeDripstone.isSuitableForWind(largeDripstoneConfiguration) && largeDripstone2.isSuitableForWind(largeDripstoneConfiguration)) {
                        windOffsetter = new LargeDripstoneFeature.WindOffsetter(blockPos.getY(), random, largeDripstoneConfiguration.windSpeed);
                    } else {
                        windOffsetter = LargeDripstoneFeature.WindOffsetter.noWind();
                    }

                    boolean bl = largeDripstone.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(worldGenLevel, windOffsetter);
                    boolean bl2 = largeDripstone2.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(worldGenLevel, windOffsetter);
                    if (bl) {
                        largeDripstone.placeBlocks(worldGenLevel, random, windOffsetter);
                    }

                    if (bl2) {
                        largeDripstone2.placeBlocks(worldGenLevel, random, windOffsetter);
                    }

                    return true;
                }
            } else {
                return false;
            }
        }
    }

    private static LargeDripstoneFeature.LargeDripstone makeDripstone(BlockPos pos, boolean isStalagmite, Random random, int scale, FloatProvider bluntness, FloatProvider heightScale) {
        return new LargeDripstoneFeature.LargeDripstone(pos, isStalagmite, scale, (double)bluntness.sample(random), (double)heightScale.sample(random));
    }

    private void placeDebugMarkers(WorldGenLevel world, BlockPos pos, Column.Range surface, LargeDripstoneFeature.WindOffsetter wind) {
        world.setBlock(wind.offset(pos.atY(surface.ceiling() - 1)), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        world.setBlock(wind.offset(pos.atY(surface.floor() + 1)), Blocks.GOLD_BLOCK.defaultBlockState(), 2);

        for(BlockPos.MutableBlockPos mutableBlockPos = pos.atY(surface.floor() + 2).mutable(); mutableBlockPos.getY() < surface.ceiling() - 1; mutableBlockPos.move(Direction.UP)) {
            BlockPos blockPos = wind.offset(mutableBlockPos);
            if (DripstoneUtils.isEmptyOrWater(world, blockPos) || world.getBlockState(blockPos).is(Blocks.DRIPSTONE_BLOCK)) {
                world.setBlock(blockPos, Blocks.CREEPER_HEAD.defaultBlockState(), 2);
            }
        }

    }

    static final class LargeDripstone {
        private BlockPos root;
        private final boolean pointingUp;
        private int radius;
        private final double bluntness;
        private final double scale;

        LargeDripstone(BlockPos pos, boolean isStalagmite, int scale, double bluntness, double heightScale) {
            this.root = pos;
            this.pointingUp = isStalagmite;
            this.radius = scale;
            this.bluntness = bluntness;
            this.scale = heightScale;
        }

        private int getHeight() {
            return this.getHeightAtRadius(0.0F);
        }

        private int getMinY() {
            return this.pointingUp ? this.root.getY() : this.root.getY() - this.getHeight();
        }

        private int getMaxY() {
            return !this.pointingUp ? this.root.getY() : this.root.getY() + this.getHeight();
        }

        boolean moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(WorldGenLevel world, LargeDripstoneFeature.WindOffsetter wind) {
            while(this.radius > 1) {
                BlockPos.MutableBlockPos mutableBlockPos = this.root.mutable();
                int i = Math.min(10, this.getHeight());

                for(int j = 0; j < i; ++j) {
                    if (world.getBlockState(mutableBlockPos).is(Blocks.LAVA)) {
                        return false;
                    }

                    if (DripstoneUtils.isCircleMostlyEmbeddedInStone(world, wind.offset(mutableBlockPos), this.radius)) {
                        this.root = mutableBlockPos;
                        return true;
                    }

                    mutableBlockPos.move(this.pointingUp ? Direction.DOWN : Direction.UP);
                }

                this.radius /= 2;
            }

            return false;
        }

        private int getHeightAtRadius(float height) {
            return (int)DripstoneUtils.getDripstoneHeight((double)height, (double)this.radius, this.scale, this.bluntness);
        }

        void placeBlocks(WorldGenLevel world, Random random, LargeDripstoneFeature.WindOffsetter wind) {
            for(int i = -this.radius; i <= this.radius; ++i) {
                for(int j = -this.radius; j <= this.radius; ++j) {
                    float f = Mth.sqrt((float)(i * i + j * j));
                    if (!(f > (float)this.radius)) {
                        int k = this.getHeightAtRadius(f);
                        if (k > 0) {
                            if ((double)random.nextFloat() < 0.2D) {
                                k = (int)((float)k * Mth.randomBetween(random, 0.8F, 1.0F));
                            }

                            BlockPos.MutableBlockPos mutableBlockPos = this.root.offset(i, 0, j).mutable();
                            boolean bl = false;
                            int l = this.pointingUp ? world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, mutableBlockPos.getX(), mutableBlockPos.getZ()) : Integer.MAX_VALUE;

                            for(int m = 0; m < k && mutableBlockPos.getY() < l; ++m) {
                                BlockPos blockPos = wind.offset(mutableBlockPos);
                                if (DripstoneUtils.isEmptyOrWaterOrLava(world, blockPos)) {
                                    bl = true;
                                    Block block = Blocks.DRIPSTONE_BLOCK;
                                    world.setBlock(blockPos, block.defaultBlockState(), 2);
                                } else if (bl && world.getBlockState(blockPos).is(BlockTags.BASE_STONE_OVERWORLD)) {
                                    break;
                                }

                                mutableBlockPos.move(this.pointingUp ? Direction.UP : Direction.DOWN);
                            }
                        }
                    }
                }
            }

        }

        boolean isSuitableForWind(LargeDripstoneConfiguration config) {
            return this.radius >= config.minRadiusForWind && this.bluntness >= (double)config.minBluntnessForWind;
        }
    }

    static final class WindOffsetter {
        private final int originY;
        @Nullable
        private final Vec3 windSpeed;

        WindOffsetter(int y, Random random, FloatProvider wind) {
            this.originY = y;
            float f = wind.sample(random);
            float g = Mth.randomBetween(random, 0.0F, (float)Math.PI);
            this.windSpeed = new Vec3((double)(Mth.cos(g) * f), 0.0D, (double)(Mth.sin(g) * f));
        }

        private WindOffsetter() {
            this.originY = 0;
            this.windSpeed = null;
        }

        static LargeDripstoneFeature.WindOffsetter noWind() {
            return new LargeDripstoneFeature.WindOffsetter();
        }

        BlockPos offset(BlockPos pos) {
            if (this.windSpeed == null) {
                return pos;
            } else {
                int i = this.originY - pos.getY();
                Vec3 vec3 = this.windSpeed.scale((double)i);
                return pos.offset(vec3.x, 0.0D, vec3.z);
            }
        }
    }
}
