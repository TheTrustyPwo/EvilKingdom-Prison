package net.minecraft.world.entity.animal;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.event.entity.SheepRegrowWoolEvent;
import org.bukkit.inventory.InventoryView;
// CraftBukkit end

public class Sheep extends Animal implements Shearable {

    private static final int EAT_ANIMATION_TICKS = 40;
    private static final EntityDataAccessor<Byte> DATA_WOOL_ID = SynchedEntityData.defineId(Sheep.class, EntityDataSerializers.BYTE);
    private static final Map<DyeColor, ItemLike> ITEM_BY_DYE = (Map) Util.make(Maps.newEnumMap(DyeColor.class), (enummap) -> {
        enummap.put(DyeColor.WHITE, Blocks.WHITE_WOOL);
        enummap.put(DyeColor.ORANGE, Blocks.ORANGE_WOOL);
        enummap.put(DyeColor.MAGENTA, Blocks.MAGENTA_WOOL);
        enummap.put(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL);
        enummap.put(DyeColor.YELLOW, Blocks.YELLOW_WOOL);
        enummap.put(DyeColor.LIME, Blocks.LIME_WOOL);
        enummap.put(DyeColor.PINK, Blocks.PINK_WOOL);
        enummap.put(DyeColor.GRAY, Blocks.GRAY_WOOL);
        enummap.put(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WOOL);
        enummap.put(DyeColor.CYAN, Blocks.CYAN_WOOL);
        enummap.put(DyeColor.PURPLE, Blocks.PURPLE_WOOL);
        enummap.put(DyeColor.BLUE, Blocks.BLUE_WOOL);
        enummap.put(DyeColor.BROWN, Blocks.BROWN_WOOL);
        enummap.put(DyeColor.GREEN, Blocks.GREEN_WOOL);
        enummap.put(DyeColor.RED, Blocks.RED_WOOL);
        enummap.put(DyeColor.BLACK, Blocks.BLACK_WOOL);
    });
    private static final Map<DyeColor, float[]> COLORARRAY_BY_COLOR = Maps.newEnumMap((Map) Arrays.stream(DyeColor.values()).collect(Collectors.toMap((enumcolor) -> {
        return enumcolor;
    }, Sheep::createSheepColor)));
    private int eatAnimationTick;
    private EatBlockGoal eatBlockGoal;

    private static float[] createSheepColor(DyeColor color) {
        if (color == DyeColor.WHITE) {
            return new float[]{0.9019608F, 0.9019608F, 0.9019608F};
        } else {
            float[] afloat = color.getTextureDiffuseColors();
            float f = 0.75F;

            return new float[]{afloat[0] * 0.75F, afloat[1] * 0.75F, afloat[2] * 0.75F};
        }
    }

    public static float[] getColorArray(DyeColor dyeColor) {
        return (float[]) Sheep.COLORARRAY_BY_COLOR.get(dyeColor);
    }

    public Sheep(EntityType<? extends Sheep> type, Level world) {
        super(type, world);
    }

    @Override
    protected void registerGoals() {
        this.eatBlockGoal = new EatBlockGoal(this);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1D, Ingredient.of(Items.WHEAT), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(5, this.eatBlockGoal);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        this.eatAnimationTick = this.eatBlockGoal.getEatAnimationTick();
        super.customServerAiStep();
    }

    @Override
    public void aiStep() {
        if (this.level.isClientSide) {
            this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
        }

        super.aiStep();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.23000000417232513D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(Sheep.DATA_WOOL_ID, (byte) 0);
    }

    @Override
    public ResourceLocation getDefaultLootTable() {
        if (this.isSheared()) {
            return this.getType().getDefaultLootTable();
        } else {
            switch (this.getColor()) {
                case WHITE:
                default:
                    return BuiltInLootTables.SHEEP_WHITE;
                case ORANGE:
                    return BuiltInLootTables.SHEEP_ORANGE;
                case MAGENTA:
                    return BuiltInLootTables.SHEEP_MAGENTA;
                case LIGHT_BLUE:
                    return BuiltInLootTables.SHEEP_LIGHT_BLUE;
                case YELLOW:
                    return BuiltInLootTables.SHEEP_YELLOW;
                case LIME:
                    return BuiltInLootTables.SHEEP_LIME;
                case PINK:
                    return BuiltInLootTables.SHEEP_PINK;
                case GRAY:
                    return BuiltInLootTables.SHEEP_GRAY;
                case LIGHT_GRAY:
                    return BuiltInLootTables.SHEEP_LIGHT_GRAY;
                case CYAN:
                    return BuiltInLootTables.SHEEP_CYAN;
                case PURPLE:
                    return BuiltInLootTables.SHEEP_PURPLE;
                case BLUE:
                    return BuiltInLootTables.SHEEP_BLUE;
                case BROWN:
                    return BuiltInLootTables.SHEEP_BROWN;
                case GREEN:
                    return BuiltInLootTables.SHEEP_GREEN;
                case RED:
                    return BuiltInLootTables.SHEEP_RED;
                case BLACK:
                    return BuiltInLootTables.SHEEP_BLACK;
            }
        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 10) {
            this.eatAnimationTick = 40;
        } else {
            super.handleEntityEvent(status);
        }

    }

    public float getHeadEatPositionScale(float delta) {
        return this.eatAnimationTick <= 0 ? 0.0F : (this.eatAnimationTick >= 4 && this.eatAnimationTick <= 36 ? 1.0F : (this.eatAnimationTick < 4 ? ((float) this.eatAnimationTick - delta) / 4.0F : -((float) (this.eatAnimationTick - 40) - delta) / 4.0F));
    }

    public float getHeadEatAngleScale(float delta) {
        if (this.eatAnimationTick > 4 && this.eatAnimationTick <= 36) {
            float f1 = ((float) (this.eatAnimationTick - 4) - delta) / 32.0F;

            return 0.62831855F + 0.21991149F * Mth.sin(f1 * 28.7F);
        } else {
            return this.eatAnimationTick > 0 ? 0.62831855F : this.getXRot() * 0.017453292F;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (itemstack.is(Items.SHEARS)) {
            if (!this.level.isClientSide && this.readyForShearing()) {
                // CraftBukkit start
                if (!CraftEventFactory.handlePlayerShearEntityEvent(player, this, itemstack, hand)) {
                    return InteractionResult.PASS;
                }
                // CraftBukkit end
                this.shear(SoundSource.PLAYERS);
                this.gameEvent(GameEvent.SHEAR, (Entity) player);
                itemstack.hurtAndBreak(1, player, (entityhuman1) -> {
                    entityhuman1.broadcastBreakEvent(hand);
                });
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.CONSUME;
            }
        } else {
            return super.mobInteract(player, hand);
        }
    }

    @Override
    public void shear(SoundSource shearedSoundCategory) {
        this.level.playSound((Player) null, (Entity) this, SoundEvents.SHEEP_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
        this.setSheared(true);
        int i = 1 + this.random.nextInt(3);

        for (int j = 0; j < i; ++j) {
            this.forceDrops = true; // CraftBukkit
            ItemEntity entityitem = this.spawnAtLocation((ItemLike) Sheep.ITEM_BY_DYE.get(this.getColor()), 1);
            this.forceDrops = false; // CraftBukkit

            if (entityitem != null) {
                entityitem.setDeltaMovement(entityitem.getDeltaMovement().add((double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double) (this.random.nextFloat() * 0.05F), (double) ((this.random.nextFloat() - this.random.nextFloat()) * 0.1F)));
            }
        }

    }

    @Override
    public boolean readyForShearing() {
        return this.isAlive() && !this.isSheared() && !this.isBaby();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("Sheared", this.isSheared());
        nbt.putByte("Color", (byte) this.getColor().getId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setSheared(nbt.getBoolean("Sheared"));
        this.setColor(DyeColor.byId(nbt.getByte("Color")));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SHEEP_STEP, 0.15F, 1.0F);
    }

    public DyeColor getColor() {
        return DyeColor.byId((Byte) this.entityData.get(Sheep.DATA_WOOL_ID) & 15);
    }

    public void setColor(DyeColor color) {
        byte b0 = (Byte) this.entityData.get(Sheep.DATA_WOOL_ID);

        this.entityData.set(Sheep.DATA_WOOL_ID, (byte) (b0 & 240 | color.getId() & 15));
    }

    public boolean isSheared() {
        return ((Byte) this.entityData.get(Sheep.DATA_WOOL_ID) & 16) != 0;
    }

    public void setSheared(boolean sheared) {
        byte b0 = (Byte) this.entityData.get(Sheep.DATA_WOOL_ID);

        if (sheared) {
            this.entityData.set(Sheep.DATA_WOOL_ID, (byte) (b0 | 16));
        } else {
            this.entityData.set(Sheep.DATA_WOOL_ID, (byte) (b0 & -17));
        }

    }

    public static DyeColor getRandomSheepColor(Random random) {
        int i = random.nextInt(100);

        return i < 5 ? DyeColor.BLACK : (i < 10 ? DyeColor.GRAY : (i < 15 ? DyeColor.LIGHT_GRAY : (i < 18 ? DyeColor.BROWN : (random.nextInt(500) == 0 ? DyeColor.PINK : DyeColor.WHITE))));
    }

    @Override
    public Sheep getBreedOffspring(ServerLevel world, AgeableMob entity) {
        Sheep entitysheep = (Sheep) entity;
        Sheep entitysheep1 = (Sheep) EntityType.SHEEP.create(world);

        entitysheep1.setColor(this.getOffspringColor(this, entitysheep));
        return entitysheep1;
    }

    @Override
    public void ate() {
        // CraftBukkit start
        SheepRegrowWoolEvent event = new SheepRegrowWoolEvent((org.bukkit.entity.Sheep) this.getBukkitEntity());
        this.level.getCraftServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) return;
        // CraftBukkit end
        this.setSheared(false);
        if (this.isBaby()) {
            this.ageUp(60);
        }

    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, @Nullable CompoundTag entityNbt) {
        this.setColor(Sheep.getRandomSheepColor(world.getRandom()));
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityNbt);
    }

    private DyeColor getOffspringColor(Animal firstParent, Animal secondParent) {
        DyeColor enumcolor = ((Sheep) firstParent).getColor();
        DyeColor enumcolor1 = ((Sheep) secondParent).getColor();
        CraftingContainer inventorycrafting = Sheep.makeContainer(enumcolor, enumcolor1);
        Optional<Item> optional = this.level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, inventorycrafting, this.level).map((recipecrafting) -> { // CraftBukkit - decompile error
            return recipecrafting.assemble(inventorycrafting);
        }).map(ItemStack::getItem);

        Objects.requireNonNull(DyeItem.class);
        optional = optional.filter(DyeItem.class::isInstance);
        Objects.requireNonNull(DyeItem.class);
        return (DyeColor) optional.map(DyeItem.class::cast).map(DyeItem::getDyeColor).orElseGet(() -> {
            return this.level.random.nextBoolean() ? enumcolor : enumcolor1;
        });
    }

    private static CraftingContainer makeContainer(DyeColor firstColor, DyeColor secondColor) {
        CraftingContainer inventorycrafting = new CraftingContainer(new AbstractContainerMenu((MenuType) null, -1) {
            @Override
            public boolean stillValid(Player player) {
                return false;
            }

            // CraftBukkit start
            @Override
            public InventoryView getBukkitView() {
                return null; // TODO: O.O
            }
            // CraftBukkit end
        }, 2, 1);

        inventorycrafting.setItem(0, new ItemStack(DyeItem.byColor(firstColor)));
        inventorycrafting.setItem(1, new ItemStack(DyeItem.byColor(secondColor)));
        inventorycrafting.resultInventory = new ResultContainer(); // CraftBukkit - add result slot for event
        return inventorycrafting;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.95F * dimensions.height;
    }
}
