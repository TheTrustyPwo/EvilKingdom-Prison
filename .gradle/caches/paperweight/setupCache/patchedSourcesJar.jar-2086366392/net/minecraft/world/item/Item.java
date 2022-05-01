package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class Item implements ItemLike {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<Block, Item> BY_BLOCK = Maps.newHashMap();
    protected static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    protected static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    public static final int MAX_STACK_SIZE = 64;
    public static final int EAT_DURATION = 32;
    public static final int MAX_BAR_WIDTH = 13;
    private final Holder.Reference<Item> builtInRegistryHolder = Registry.ITEM.createIntrusiveHolder(this);
    @Nullable
    protected final CreativeModeTab category;
    public final Rarity rarity;
    private final int maxStackSize;
    private final int maxDamage;
    private final boolean isFireResistant;
    @Nullable
    private final Item craftingRemainingItem;
    @Nullable
    private String descriptionId;
    @Nullable
    private final FoodProperties foodProperties;

    public static int getId(Item item) {
        return item == null ? 0 : Registry.ITEM.getId(item);
    }

    public static Item byId(int id) {
        return Registry.ITEM.byId(id);
    }

    /** @deprecated */
    @Deprecated
    public static Item byBlock(Block block) {
        return BY_BLOCK.getOrDefault(block, Items.AIR);
    }

    public Item(Item.Properties settings) {
        this.category = settings.category;
        this.rarity = settings.rarity;
        this.craftingRemainingItem = settings.craftingRemainingItem;
        this.maxDamage = settings.maxDamage;
        this.maxStackSize = settings.maxStackSize;
        this.foodProperties = settings.foodProperties;
        this.isFireResistant = settings.isFireResistant;
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            String string = this.getClass().getSimpleName();
            if (!string.endsWith("Item")) {
                LOGGER.error("Item classes should end with Item and {} doesn't.", (Object)string);
            }
        }

    }

    /** @deprecated */
    @Deprecated
    public Holder.Reference<Item> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
    }

    public void onDestroyed(ItemEntity entity) {
    }

    public void verifyTagAfterLoad(CompoundTag nbt) {
    }

    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player miner) {
        return true;
    }

    @Override
    public Item asItem() {
        return this;
    }

    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 1.0F;
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (this.isEdible()) {
            ItemStack itemStack = user.getItemInHand(hand);
            if (user.canEat(this.getFoodProperties().canAlwaysEat())) {
                user.startUsingItem(hand);
                return InteractionResultHolder.consume(itemStack);
            } else {
                return InteractionResultHolder.fail(itemStack);
            }
        } else {
            return InteractionResultHolder.pass(user.getItemInHand(hand));
        }
    }

    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        return this.isEdible() ? user.eat(world, stack) : stack;
    }

    public final int getMaxStackSize() {
        return this.maxStackSize;
    }

    public final int getMaxDamage() {
        return this.maxDamage;
    }

    public boolean canBeDepleted() {
        return this.maxDamage > 0;
    }

    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - (float)stack.getDamageValue() * 13.0F / (float)this.maxDamage);
    }

    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, ((float)this.maxDamage - (float)stack.getDamageValue()) / (float)this.maxDamage);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickType, Player player) {
        return false;
    }

    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference) {
        return false;
    }

    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return false;
    }

    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        return false;
    }

    public boolean isCorrectToolForDrops(BlockState state) {
        return false;
    }

    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    public Component getDescription() {
        return new TranslatableComponent(this.getDescriptionId());
    }

    @Override
    public String toString() {
        return Registry.ITEM.getKey(this).getPath();
    }

    protected String getOrCreateDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("item", Registry.ITEM.getKey(this));
        }

        return this.descriptionId;
    }

    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    public String getDescriptionId(ItemStack stack) {
        return this.getDescriptionId();
    }

    public boolean shouldOverrideMultiplayerNbt() {
        return true;
    }

    @Nullable
    public final Item getCraftingRemainingItem() {
        return this.craftingRemainingItem;
    }

    public boolean hasCraftingRemainingItem() {
        return this.craftingRemainingItem != null;
    }

    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
    }

    public void onCraftedBy(ItemStack stack, Level world, Player player) {
    }

    public boolean isComplex() {
        return false;
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return stack.getItem().isEdible() ? UseAnim.EAT : UseAnim.NONE;
    }

    public int getUseDuration(ItemStack stack) {
        if (stack.getItem().isEdible()) {
            return this.getFoodProperties().isFastFood() ? 16 : 32;
        } else {
            return 0;
        }
    }

    public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
    }

    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    }

    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.empty();
    }

    public Component getName(ItemStack stack) {
        return new TranslatableComponent(this.getDescriptionId(stack));
    }

    public boolean isFoil(ItemStack stack) {
        return stack.isEnchanted();
    }

    public Rarity getRarity(ItemStack stack) {
        if (!stack.isEnchanted()) {
            return this.rarity;
        } else {
            switch(this.rarity) {
            case COMMON:
            case UNCOMMON:
                return Rarity.RARE;
            case RARE:
                return Rarity.EPIC;
            case EPIC:
            default:
                return this.rarity;
            }
        }
    }

    public boolean isEnchantable(ItemStack stack) {
        return this.getMaxStackSize() == 1 && this.canBeDepleted();
    }

    protected static BlockHitResult getPlayerPOVHitResult(Level world, Player player, ClipContext.Fluid fluidHandling) {
        float f = player.getXRot();
        float g = player.getYRot();
        Vec3 vec3 = player.getEyePosition();
        float h = Mth.cos(-g * ((float)Math.PI / 180F) - (float)Math.PI);
        float i = Mth.sin(-g * ((float)Math.PI / 180F) - (float)Math.PI);
        float j = -Mth.cos(-f * ((float)Math.PI / 180F));
        float k = Mth.sin(-f * ((float)Math.PI / 180F));
        float l = i * j;
        float n = h * j;
        double d = 5.0D;
        Vec3 vec32 = vec3.add((double)l * 5.0D, (double)k * 5.0D, (double)n * 5.0D);
        return world.clip(new ClipContext(vec3, vec32, ClipContext.Block.OUTLINE, fluidHandling, player));
    }

    public int getEnchantmentValue() {
        return 0;
    }

    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(group)) {
            stacks.add(new ItemStack(this));
        }

    }

    protected boolean allowdedIn(CreativeModeTab group) {
        CreativeModeTab creativeModeTab = this.getItemCategory();
        return creativeModeTab != null && (group == CreativeModeTab.TAB_SEARCH || group == creativeModeTab);
    }

    @Nullable
    public final CreativeModeTab getItemCategory() {
        return this.category;
    }

    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return false;
    }

    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return ImmutableMultimap.of();
    }

    public boolean useOnRelease(ItemStack stack) {
        return false;
    }

    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    public boolean isEdible() {
        return this.foodProperties != null;
    }

    @Nullable
    public FoodProperties getFoodProperties() {
        return this.foodProperties;
    }

    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_EAT;
    }

    public boolean isFireResistant() {
        return this.isFireResistant;
    }

    public boolean canBeHurtBy(DamageSource source) {
        return !this.isFireResistant || !source.isFire();
    }

    @Nullable
    public SoundEvent getEquipSound() {
        return null;
    }

    public boolean canFitInsideContainerItems() {
        return true;
    }

    public static class Properties {
        int maxStackSize = 64;
        int maxDamage;
        @Nullable
        Item craftingRemainingItem;
        @Nullable
        CreativeModeTab category;
        Rarity rarity = Rarity.COMMON;
        @Nullable
        FoodProperties foodProperties;
        boolean isFireResistant;

        public Item.Properties food(FoodProperties foodComponent) {
            this.foodProperties = foodComponent;
            return this;
        }

        public Item.Properties stacksTo(int maxCount) {
            if (this.maxDamage > 0) {
                throw new RuntimeException("Unable to have damage AND stack.");
            } else {
                this.maxStackSize = maxCount;
                return this;
            }
        }

        public Item.Properties defaultDurability(int maxDamage) {
            return this.maxDamage == 0 ? this.durability(maxDamage) : this;
        }

        public Item.Properties durability(int maxDamage) {
            this.maxDamage = maxDamage;
            this.maxStackSize = 1;
            return this;
        }

        public Item.Properties craftRemainder(Item recipeRemainder) {
            this.craftingRemainingItem = recipeRemainder;
            return this;
        }

        public Item.Properties tab(CreativeModeTab group) {
            this.category = group;
            return this;
        }

        public Item.Properties rarity(Rarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public Item.Properties fireResistant() {
            this.isFireResistant = true;
            return this;
        }
    }
}
