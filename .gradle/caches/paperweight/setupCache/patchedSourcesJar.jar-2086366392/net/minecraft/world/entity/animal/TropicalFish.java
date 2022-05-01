package net.minecraft.world.entity.animal;

import java.util.Locale;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

public class TropicalFish extends AbstractSchoolingFish {
    public static final String BUCKET_VARIANT_TAG = "BucketVariantTag";
    private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(TropicalFish.class, EntityDataSerializers.INT);
    public static final int BASE_SMALL = 0;
    public static final int BASE_LARGE = 1;
    private static final int BASES = 2;
    private static final ResourceLocation[] BASE_TEXTURE_LOCATIONS = new ResourceLocation[]{new ResourceLocation("textures/entity/fish/tropical_a.png"), new ResourceLocation("textures/entity/fish/tropical_b.png")};
    private static final ResourceLocation[] PATTERN_A_TEXTURE_LOCATIONS = new ResourceLocation[]{new ResourceLocation("textures/entity/fish/tropical_a_pattern_1.png"), new ResourceLocation("textures/entity/fish/tropical_a_pattern_2.png"), new ResourceLocation("textures/entity/fish/tropical_a_pattern_3.png"), new ResourceLocation("textures/entity/fish/tropical_a_pattern_4.png"), new ResourceLocation("textures/entity/fish/tropical_a_pattern_5.png"), new ResourceLocation("textures/entity/fish/tropical_a_pattern_6.png")};
    private static final ResourceLocation[] PATTERN_B_TEXTURE_LOCATIONS = new ResourceLocation[]{new ResourceLocation("textures/entity/fish/tropical_b_pattern_1.png"), new ResourceLocation("textures/entity/fish/tropical_b_pattern_2.png"), new ResourceLocation("textures/entity/fish/tropical_b_pattern_3.png"), new ResourceLocation("textures/entity/fish/tropical_b_pattern_4.png"), new ResourceLocation("textures/entity/fish/tropical_b_pattern_5.png"), new ResourceLocation("textures/entity/fish/tropical_b_pattern_6.png")};
    private static final int PATTERNS = 6;
    private static final int COLORS = 15;
    public static final int[] COMMON_VARIANTS = new int[]{calculateVariant(TropicalFish.Pattern.STRIPEY, DyeColor.ORANGE, DyeColor.GRAY), calculateVariant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.GRAY), calculateVariant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.BLUE), calculateVariant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.GRAY), calculateVariant(TropicalFish.Pattern.SUNSTREAK, DyeColor.BLUE, DyeColor.GRAY), calculateVariant(TropicalFish.Pattern.KOB, DyeColor.ORANGE, DyeColor.WHITE), calculateVariant(TropicalFish.Pattern.SPOTTY, DyeColor.PINK, DyeColor.LIGHT_BLUE), calculateVariant(TropicalFish.Pattern.BLOCKFISH, DyeColor.PURPLE, DyeColor.YELLOW), calculateVariant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.RED), calculateVariant(TropicalFish.Pattern.SPOTTY, DyeColor.WHITE, DyeColor.YELLOW), calculateVariant(TropicalFish.Pattern.GLITTER, DyeColor.WHITE, DyeColor.GRAY), calculateVariant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.ORANGE), calculateVariant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.PINK), calculateVariant(TropicalFish.Pattern.BRINELY, DyeColor.LIME, DyeColor.LIGHT_BLUE), calculateVariant(TropicalFish.Pattern.BETTY, DyeColor.RED, DyeColor.WHITE), calculateVariant(TropicalFish.Pattern.SNOOPER, DyeColor.GRAY, DyeColor.RED), calculateVariant(TropicalFish.Pattern.BLOCKFISH, DyeColor.RED, DyeColor.WHITE), calculateVariant(TropicalFish.Pattern.FLOPPER, DyeColor.WHITE, DyeColor.YELLOW), calculateVariant(TropicalFish.Pattern.KOB, DyeColor.RED, DyeColor.WHITE), calculateVariant(TropicalFish.Pattern.SUNSTREAK, DyeColor.GRAY, DyeColor.WHITE), calculateVariant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.YELLOW), calculateVariant(TropicalFish.Pattern.FLOPPER, DyeColor.YELLOW, DyeColor.YELLOW)};
    private boolean isSchool = true;

    private static int calculateVariant(TropicalFish.Pattern variety, DyeColor baseColor, DyeColor patternColor) {
        return variety.getBase() & 255 | (variety.getIndex() & 255) << 8 | (baseColor.getId() & 255) << 16 | (patternColor.getId() & 255) << 24;
    }

    public TropicalFish(EntityType<? extends TropicalFish> type, Level world) {
        super(type, world);
    }

    public static String getPredefinedName(int variant) {
        return "entity.minecraft.tropical_fish.predefined." + variant;
    }

    public static DyeColor getBaseColor(int variant) {
        return DyeColor.byId(getBaseColorIdx(variant));
    }

    public static DyeColor getPatternColor(int variant) {
        return DyeColor.byId(getPatternColorIdx(variant));
    }

    public static String getFishTypeName(int variant) {
        int i = getBaseVariant(variant);
        int j = getPatternVariant(variant);
        return "entity.minecraft.tropical_fish.type." + TropicalFish.Pattern.getPatternName(i, j);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ID_TYPE_VARIANT, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setVariant(nbt.getInt("Variant"));
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, variant);
    }

    @Override
    public boolean isMaxGroupSizeReached(int count) {
        return !this.isSchool;
    }

    public int getVariant() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    @Override
    public void saveToBucketTag(ItemStack stack) {
        super.saveToBucketTag(stack);
        CompoundTag compoundTag = stack.getOrCreateTag();
        compoundTag.putInt("BucketVariantTag", this.getVariant());
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.TROPICAL_FISH_BUCKET);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    private static int getBaseColorIdx(int variant) {
        return (variant & 16711680) >> 16;
    }

    public float[] getBaseColor() {
        return DyeColor.byId(getBaseColorIdx(this.getVariant())).getTextureDiffuseColors();
    }

    private static int getPatternColorIdx(int variant) {
        return (variant & -16777216) >> 24;
    }

    public float[] getPatternColor() {
        return DyeColor.byId(getPatternColorIdx(this.getVariant())).getTextureDiffuseColors();
    }

    public static int getBaseVariant(int variant) {
        return Math.min(variant & 255, 1);
    }

    public int getBaseVariant() {
        return getBaseVariant(this.getVariant());
    }

    private static int getPatternVariant(int variant) {
        return Math.min((variant & '\uff00') >> 8, 5);
    }

    public ResourceLocation getPatternTextureLocation() {
        return getBaseVariant(this.getVariant()) == 0 ? PATTERN_A_TEXTURE_LOCATIONS[getPatternVariant(this.getVariant())] : PATTERN_B_TEXTURE_LOCATIONS[getPatternVariant(this.getVariant())];
    }

    public ResourceLocation getBaseTextureLocation() {
        return BASE_TEXTURE_LOCATIONS[getBaseVariant(this.getVariant())];
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        entityData = super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityNbt);
        if (spawnReason == MobSpawnType.BUCKET && entityNbt != null && entityNbt.contains("BucketVariantTag", 3)) {
            this.setVariant(entityNbt.getInt("BucketVariantTag"));
            return entityData;
        } else {
            int i;
            int j;
            int k;
            int l;
            if (entityData instanceof TropicalFish.TropicalFishGroupData) {
                TropicalFish.TropicalFishGroupData tropicalFishGroupData = (TropicalFish.TropicalFishGroupData)entityData;
                i = tropicalFishGroupData.base;
                j = tropicalFishGroupData.pattern;
                k = tropicalFishGroupData.baseColor;
                l = tropicalFishGroupData.patternColor;
            } else if ((double)this.random.nextFloat() < 0.9D) {
                int m = Util.getRandom(COMMON_VARIANTS, this.random);
                i = m & 255;
                j = (m & '\uff00') >> 8;
                k = (m & 16711680) >> 16;
                l = (m & -16777216) >> 24;
                entityData = new TropicalFish.TropicalFishGroupData(this, i, j, k, l);
            } else {
                this.isSchool = false;
                i = this.random.nextInt(2);
                j = this.random.nextInt(6);
                k = this.random.nextInt(15);
                l = this.random.nextInt(15);
            }

            this.setVariant(i | j << 8 | k << 16 | l << 24);
            return entityData;
        }
    }

    public static boolean checkTropicalFishSpawnRules(EntityType<TropicalFish> type, LevelAccessor world, MobSpawnType reason, BlockPos pos, Random random) {
        return world.getFluidState(pos.below()).is(FluidTags.WATER) && world.getBlockState(pos.above()).is(Blocks.WATER) && (world.getBiome(pos).is(Biomes.LUSH_CAVES) || WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, world, reason, pos, random));
    }

    static enum Pattern {
        KOB(0, 0),
        SUNSTREAK(0, 1),
        SNOOPER(0, 2),
        DASHER(0, 3),
        BRINELY(0, 4),
        SPOTTY(0, 5),
        FLOPPER(1, 0),
        STRIPEY(1, 1),
        GLITTER(1, 2),
        BLOCKFISH(1, 3),
        BETTY(1, 4),
        CLAYFISH(1, 5);

        private final int base;
        private final int index;
        private static final TropicalFish.Pattern[] VALUES = values();

        private Pattern(int shape, int pattern) {
            this.base = shape;
            this.index = pattern;
        }

        public int getBase() {
            return this.base;
        }

        public int getIndex() {
            return this.index;
        }

        public static String getPatternName(int shape, int pattern) {
            return VALUES[pattern + 6 * shape].getName();
        }

        public String getName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    static class TropicalFishGroupData extends AbstractSchoolingFish.SchoolSpawnGroupData {
        final int base;
        final int pattern;
        final int baseColor;
        final int patternColor;

        TropicalFishGroupData(TropicalFish leader, int shape, int pattern, int baseColor, int patternColor) {
            super(leader);
            this.base = shape;
            this.pattern = pattern;
            this.baseColor = baseColor;
            this.patternColor = patternColor;
        }
    }
}
