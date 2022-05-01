package net.minecraft.world.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.slf4j.Logger;

public final class ItemStack {
    public static final Codec<ItemStack> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Registry.ITEM.byNameCodec().fieldOf("id").forGetter((stack) -> {
            return stack.item;
        }), Codec.INT.fieldOf("Count").forGetter((stack) -> {
            return stack.count;
        }), CompoundTag.CODEC.optionalFieldOf("tag").forGetter((stack) -> {
            return Optional.ofNullable(stack.tag);
        })).apply(instance, ItemStack::new);
    });
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ItemStack EMPTY = new ItemStack((Item)null);
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = Util.make(new DecimalFormat("#.##"), (decimalFormat) -> {
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    });
    public static final String TAG_ENCH = "Enchantments";
    public static final String TAG_DISPLAY = "display";
    public static final String TAG_DISPLAY_NAME = "Name";
    public static final String TAG_LORE = "Lore";
    public static final String TAG_DAMAGE = "Damage";
    public static final String TAG_COLOR = "color";
    private static final String TAG_UNBREAKABLE = "Unbreakable";
    private static final String TAG_REPAIR_COST = "RepairCost";
    private static final String TAG_CAN_DESTROY_BLOCK_LIST = "CanDestroy";
    private static final String TAG_CAN_PLACE_ON_BLOCK_LIST = "CanPlaceOn";
    private static final String TAG_HIDE_FLAGS = "HideFlags";
    private static final int DONT_HIDE_TOOLTIP = 0;
    private static final Style LORE_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true);
    private int count;
    private int popTime;
    /** @deprecated */
    @Deprecated
    private Item item;
    @Nullable
    public CompoundTag tag;
    private boolean emptyCacheFlag;
    @Nullable
    private Entity entityRepresentation;
    @Nullable
    private AdventureModeCheck adventureBreakCheck;
    @Nullable
    private AdventureModeCheck adventurePlaceCheck;

    public Optional<TooltipComponent> getTooltipImage() {
        return this.getItem().getTooltipImage(this);
    }

    public ItemStack(ItemLike item) {
        this(item, 1);
    }

    public ItemStack(Holder<Item> entry) {
        this(entry.value(), 1);
    }

    private ItemStack(ItemLike item, int count, Optional<CompoundTag> nbt) {
        this(item, count);
        nbt.ifPresent(this::setTag);
    }

    public ItemStack(ItemLike item, int count) {
        this.item = item == null ? null : item.asItem();
        this.count = count;
        if (this.item != null && this.item.canBeDepleted()) {
            this.setDamageValue(this.getDamageValue());
        }

        this.updateEmptyCacheFlag();
    }

    private void updateEmptyCacheFlag() {
        this.emptyCacheFlag = false;
        this.emptyCacheFlag = this.isEmpty();
    }

    private ItemStack(CompoundTag nbt) {
        this.item = Registry.ITEM.get(new ResourceLocation(nbt.getString("id")));
        this.count = nbt.getByte("Count");
        if (nbt.contains("tag", 10)) {
            this.tag = nbt.getCompound("tag");
            this.getItem().verifyTagAfterLoad(this.tag);
        }

        if (this.getItem().canBeDepleted()) {
            this.setDamageValue(this.getDamageValue());
        }

        this.updateEmptyCacheFlag();
    }

    public static ItemStack of(CompoundTag nbt) {
        try {
            return new ItemStack(nbt);
        } catch (RuntimeException var2) {
            LOGGER.debug("Tried to load invalid item: {}", nbt, var2);
            return EMPTY;
        }
    }

    public boolean isEmpty() {
        if (this == EMPTY) {
            return true;
        } else if (this.getItem() != null && !this.is(Items.AIR)) {
            return this.count <= 0;
        } else {
            return true;
        }
    }

    public ItemStack split(int amount) {
        int i = Math.min(amount, this.count);
        ItemStack itemStack = this.copy();
        itemStack.setCount(i);
        this.shrink(i);
        return itemStack;
    }

    public Item getItem() {
        return this.emptyCacheFlag ? Items.AIR : this.item;
    }

    public boolean is(TagKey<Item> tag) {
        return this.getItem().builtInRegistryHolder().is(tag);
    }

    public boolean is(Item item) {
        return this.getItem() == item;
    }

    public Stream<TagKey<Item>> getTags() {
        return this.getItem().builtInRegistryHolder().tags();
    }

    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos blockPos = context.getClickedPos();
        BlockInWorld blockInWorld = new BlockInWorld(context.getLevel(), blockPos, false);
        if (player != null && !player.getAbilities().mayBuild && !this.hasAdventureModePlaceTagForBlock(context.getLevel().registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY), blockInWorld)) {
            return InteractionResult.PASS;
        } else {
            Item item = this.getItem();
            InteractionResult interactionResult = item.useOn(context);
            if (player != null && interactionResult.shouldAwardStats()) {
                player.awardStat(Stats.ITEM_USED.get(item));
            }

            return interactionResult;
        }
    }

    public float getDestroySpeed(BlockState state) {
        return this.getItem().getDestroySpeed(this, state);
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        return this.getItem().use(world, user, hand);
    }

    public ItemStack finishUsingItem(Level world, LivingEntity user) {
        return this.getItem().finishUsingItem(this, world, user);
    }

    public CompoundTag save(CompoundTag nbt) {
        ResourceLocation resourceLocation = Registry.ITEM.getKey(this.getItem());
        nbt.putString("id", resourceLocation == null ? "minecraft:air" : resourceLocation.toString());
        nbt.putByte("Count", (byte)this.count);
        if (this.tag != null) {
            nbt.put("tag", this.tag.copy());
        }

        return nbt;
    }

    public int getMaxStackSize() {
        return this.getItem().getMaxStackSize();
    }

    public boolean isStackable() {
        return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
    }

    public boolean isDamageableItem() {
        if (!this.emptyCacheFlag && this.getItem().getMaxDamage() > 0) {
            CompoundTag compoundTag = this.getTag();
            return compoundTag == null || !compoundTag.getBoolean("Unbreakable");
        } else {
            return false;
        }
    }

    public boolean isDamaged() {
        return this.isDamageableItem() && this.getDamageValue() > 0;
    }

    public int getDamageValue() {
        return this.tag == null ? 0 : this.tag.getInt("Damage");
    }

    public void setDamageValue(int damage) {
        this.getOrCreateTag().putInt("Damage", Math.max(0, damage));
    }

    public int getMaxDamage() {
        return this.getItem().getMaxDamage();
    }

    public boolean hurt(int amount, Random random, @Nullable ServerPlayer player) {
        if (!this.isDamageableItem()) {
            return false;
        } else {
            if (amount > 0) {
                int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, this);
                int j = 0;

                for(int k = 0; i > 0 && k < amount; ++k) {
                    if (DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(this, i, random)) {
                        ++j;
                    }
                }

                amount -= j;
                if (amount <= 0) {
                    return false;
                }
            }

            if (player != null && amount != 0) {
                CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, this, this.getDamageValue() + amount);
            }

            int l = this.getDamageValue() + amount;
            this.setDamageValue(l);
            return l >= this.getMaxDamage();
        }
    }

    public <T extends LivingEntity> void hurtAndBreak(int amount, T entity, Consumer<T> breakCallback) {
        if (!entity.level.isClientSide && (!(entity instanceof Player) || !((Player)entity).getAbilities().instabuild)) {
            if (this.isDamageableItem()) {
                if (this.hurt(amount, entity.getRandom(), entity instanceof ServerPlayer ? (ServerPlayer)entity : null)) {
                    breakCallback.accept(entity);
                    Item item = this.getItem();
                    this.shrink(1);
                    if (entity instanceof Player) {
                        ((Player)entity).awardStat(Stats.ITEM_BROKEN.get(item));
                    }

                    this.setDamageValue(0);
                }

            }
        }
    }

    public boolean isBarVisible() {
        return this.item.isBarVisible(this);
    }

    public int getBarWidth() {
        return this.item.getBarWidth(this);
    }

    public int getBarColor() {
        return this.item.getBarColor(this);
    }

    public boolean overrideStackedOnOther(Slot slot, ClickAction clickType, Player player) {
        return this.getItem().overrideStackedOnOther(this, slot, clickType, player);
    }

    public boolean overrideOtherStackedOnMe(ItemStack stack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference) {
        return this.getItem().overrideOtherStackedOnMe(this, stack, slot, clickType, player, cursorStackReference);
    }

    public void hurtEnemy(LivingEntity target, Player attacker) {
        Item item = this.getItem();
        if (item.hurtEnemy(this, target, attacker)) {
            attacker.awardStat(Stats.ITEM_USED.get(item));
        }

    }

    public void mineBlock(Level world, BlockState state, BlockPos pos, Player miner) {
        Item item = this.getItem();
        if (item.mineBlock(this, world, state, pos, miner)) {
            miner.awardStat(Stats.ITEM_USED.get(item));
        }

    }

    public boolean isCorrectToolForDrops(BlockState state) {
        return this.getItem().isCorrectToolForDrops(state);
    }

    public InteractionResult interactLivingEntity(Player user, LivingEntity entity, InteractionHand hand) {
        return this.getItem().interactLivingEntity(this, user, entity, hand);
    }

    public ItemStack copy() {
        if (this.isEmpty()) {
            return EMPTY;
        } else {
            ItemStack itemStack = new ItemStack(this.getItem(), this.count);
            itemStack.setPopTime(this.getPopTime());
            if (this.tag != null) {
                itemStack.tag = this.tag.copy();
            }

            return itemStack;
        }
    }

    public static boolean tagMatches(ItemStack left, ItemStack right) {
        if (left.isEmpty() && right.isEmpty()) {
            return true;
        } else if (!left.isEmpty() && !right.isEmpty()) {
            if (left.tag == null && right.tag != null) {
                return false;
            } else {
                return left.tag == null || left.tag.equals(right.tag);
            }
        } else {
            return false;
        }
    }

    public static boolean matches(ItemStack left, ItemStack right) {
        if (left.isEmpty() && right.isEmpty()) {
            return true;
        } else {
            return !left.isEmpty() && !right.isEmpty() ? left.matches(right) : false;
        }
    }

    private boolean matches(ItemStack stack) {
        if (this.count != stack.count) {
            return false;
        } else if (!this.is(stack.getItem())) {
            return false;
        } else if (this.tag == null && stack.tag != null) {
            return false;
        } else {
            return this.tag == null || this.tag.equals(stack.tag);
        }
    }

    public static boolean isSame(ItemStack left, ItemStack right) {
        if (left == right) {
            return true;
        } else {
            return !left.isEmpty() && !right.isEmpty() ? left.sameItem(right) : false;
        }
    }

    public static boolean isSameIgnoreDurability(ItemStack left, ItemStack right) {
        if (left == right) {
            return true;
        } else {
            return !left.isEmpty() && !right.isEmpty() ? left.sameItemStackIgnoreDurability(right) : false;
        }
    }

    public boolean sameItem(ItemStack stack) {
        return !stack.isEmpty() && this.is(stack.getItem());
    }

    public boolean sameItemStackIgnoreDurability(ItemStack stack) {
        if (!this.isDamageableItem()) {
            return this.sameItem(stack);
        } else {
            return !stack.isEmpty() && this.is(stack.getItem());
        }
    }

    public static boolean isSameItemSameTags(ItemStack stack, ItemStack otherStack) {
        return stack.is(otherStack.getItem()) && tagMatches(stack, otherStack);
    }

    public String getDescriptionId() {
        return this.getItem().getDescriptionId(this);
    }

    @Override
    public String toString() {
        return this.count + " " + this.getItem();
    }

    public void inventoryTick(Level world, Entity entity, int slot, boolean selected) {
        if (this.popTime > 0) {
            --this.popTime;
        }

        if (this.getItem() != null) {
            this.getItem().inventoryTick(this, world, entity, slot, selected);
        }

    }

    public void onCraftedBy(Level world, Player player, int amount) {
        player.awardStat(Stats.ITEM_CRAFTED.get(this.getItem()), amount);
        this.getItem().onCraftedBy(this, world, player);
    }

    public int getUseDuration() {
        return this.getItem().getUseDuration(this);
    }

    public UseAnim getUseAnimation() {
        return this.getItem().getUseAnimation(this);
    }

    public void releaseUsing(Level world, LivingEntity user, int remainingUseTicks) {
        this.getItem().releaseUsing(this, world, user, remainingUseTicks);
    }

    public boolean useOnRelease() {
        return this.getItem().useOnRelease(this);
    }

    public boolean hasTag() {
        return !this.emptyCacheFlag && this.tag != null && !this.tag.isEmpty();
    }

    @Nullable
    public CompoundTag getTag() {
        return this.tag;
    }

    public CompoundTag getOrCreateTag() {
        if (this.tag == null) {
            this.setTag(new CompoundTag());
        }

        return this.tag;
    }

    public CompoundTag getOrCreateTagElement(String key) {
        if (this.tag != null && this.tag.contains(key, 10)) {
            return this.tag.getCompound(key);
        } else {
            CompoundTag compoundTag = new CompoundTag();
            this.addTagElement(key, compoundTag);
            return compoundTag;
        }
    }

    @Nullable
    public CompoundTag getTagElement(String key) {
        return this.tag != null && this.tag.contains(key, 10) ? this.tag.getCompound(key) : null;
    }

    public void removeTagKey(String key) {
        if (this.tag != null && this.tag.contains(key)) {
            this.tag.remove(key);
            if (this.tag.isEmpty()) {
                this.tag = null;
            }
        }

    }

    public ListTag getEnchantmentTags() {
        return this.tag != null ? this.tag.getList("Enchantments", 10) : new ListTag();
    }

    public void setTag(@Nullable CompoundTag nbt) {
        this.tag = nbt;
        if (this.getItem().canBeDepleted()) {
            this.setDamageValue(this.getDamageValue());
        }

        if (nbt != null) {
            this.getItem().verifyTagAfterLoad(nbt);
        }

    }

    public Component getHoverName() {
        CompoundTag compoundTag = this.getTagElement("display");
        if (compoundTag != null && compoundTag.contains("Name", 8)) {
            try {
                Component component = Component.Serializer.fromJson(compoundTag.getString("Name"));
                if (component != null) {
                    return component;
                }

                compoundTag.remove("Name");
            } catch (Exception var3) {
                compoundTag.remove("Name");
            }
        }

        return this.getItem().getName(this);
    }

    public ItemStack setHoverName(@Nullable Component name) {
        CompoundTag compoundTag = this.getOrCreateTagElement("display");
        if (name != null) {
            compoundTag.putString("Name", Component.Serializer.toJson(name));
        } else {
            compoundTag.remove("Name");
        }

        return this;
    }

    public void resetHoverName() {
        CompoundTag compoundTag = this.getTagElement("display");
        if (compoundTag != null) {
            compoundTag.remove("Name");
            if (compoundTag.isEmpty()) {
                this.removeTagKey("display");
            }
        }

        if (this.tag != null && this.tag.isEmpty()) {
            this.tag = null;
        }

    }

    public boolean hasCustomHoverName() {
        CompoundTag compoundTag = this.getTagElement("display");
        return compoundTag != null && compoundTag.contains("Name", 8);
    }

    public List<Component> getTooltipLines(@Nullable Player player, TooltipFlag context) {
        List<Component> list = Lists.newArrayList();
        MutableComponent mutableComponent = (new TextComponent("")).append(this.getHoverName()).withStyle(this.getRarity().color);
        if (this.hasCustomHoverName()) {
            mutableComponent.withStyle(ChatFormatting.ITALIC);
        }

        list.add(mutableComponent);
        if (!context.isAdvanced() && !this.hasCustomHoverName() && this.is(Items.FILLED_MAP)) {
            Integer integer = MapItem.getMapId(this);
            if (integer != null) {
                list.add((new TextComponent("#" + integer)).withStyle(ChatFormatting.GRAY));
            }
        }

        int i = this.getHideFlags();
        if (shouldShowInTooltip(i, ItemStack.TooltipPart.ADDITIONAL)) {
            this.getItem().appendHoverText(this, player == null ? null : player.level, list, context);
        }

        if (this.hasTag()) {
            if (shouldShowInTooltip(i, ItemStack.TooltipPart.ENCHANTMENTS)) {
                appendEnchantmentNames(list, this.getEnchantmentTags());
            }

            if (this.tag.contains("display", 10)) {
                CompoundTag compoundTag = this.tag.getCompound("display");
                if (shouldShowInTooltip(i, ItemStack.TooltipPart.DYE) && compoundTag.contains("color", 99)) {
                    if (context.isAdvanced()) {
                        list.add((new TranslatableComponent("item.color", String.format("#%06X", compoundTag.getInt("color")))).withStyle(ChatFormatting.GRAY));
                    } else {
                        list.add((new TranslatableComponent("item.dyed")).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
                    }
                }

                if (compoundTag.getTagType("Lore") == 9) {
                    ListTag listTag = compoundTag.getList("Lore", 8);

                    for(int j = 0; j < listTag.size(); ++j) {
                        String string = listTag.getString(j);

                        try {
                            MutableComponent mutableComponent2 = Component.Serializer.fromJson(string);
                            if (mutableComponent2 != null) {
                                list.add(ComponentUtils.mergeStyles(mutableComponent2, LORE_STYLE));
                            }
                        } catch (Exception var19) {
                            compoundTag.remove("Lore");
                        }
                    }
                }
            }
        }

        if (shouldShowInTooltip(i, ItemStack.TooltipPart.MODIFIERS)) {
            for(EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                Multimap<Attribute, AttributeModifier> multimap = this.getAttributeModifiers(equipmentSlot);
                if (!multimap.isEmpty()) {
                    list.add(TextComponent.EMPTY);
                    list.add((new TranslatableComponent("item.modifiers." + equipmentSlot.getName())).withStyle(ChatFormatting.GRAY));

                    for(Entry<Attribute, AttributeModifier> entry : multimap.entries()) {
                        AttributeModifier attributeModifier = entry.getValue();
                        double d = attributeModifier.getAmount();
                        boolean bl = false;
                        if (player != null) {
                            if (attributeModifier.getId() == Item.BASE_ATTACK_DAMAGE_UUID) {
                                d += player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                                d += (double)EnchantmentHelper.getDamageBonus(this, MobType.UNDEFINED);
                                bl = true;
                            } else if (attributeModifier.getId() == Item.BASE_ATTACK_SPEED_UUID) {
                                d += player.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                                bl = true;
                            }
                        }

                        double f;
                        if (attributeModifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributeModifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
                            if (entry.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                                f = d * 10.0D;
                            } else {
                                f = d;
                            }
                        } else {
                            f = d * 100.0D;
                        }

                        if (bl) {
                            list.add((new TextComponent(" ")).append(new TranslatableComponent("attribute.modifier.equals." + attributeModifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(f), new TranslatableComponent(entry.getKey().getDescriptionId()))).withStyle(ChatFormatting.DARK_GREEN));
                        } else if (d > 0.0D) {
                            list.add((new TranslatableComponent("attribute.modifier.plus." + attributeModifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(f), new TranslatableComponent(entry.getKey().getDescriptionId()))).withStyle(ChatFormatting.BLUE));
                        } else if (d < 0.0D) {
                            f *= -1.0D;
                            list.add((new TranslatableComponent("attribute.modifier.take." + attributeModifier.getOperation().toValue(), ATTRIBUTE_MODIFIER_FORMAT.format(f), new TranslatableComponent(entry.getKey().getDescriptionId()))).withStyle(ChatFormatting.RED));
                        }
                    }
                }
            }
        }

        if (this.hasTag()) {
            if (shouldShowInTooltip(i, ItemStack.TooltipPart.UNBREAKABLE) && this.tag.getBoolean("Unbreakable")) {
                list.add((new TranslatableComponent("item.unbreakable")).withStyle(ChatFormatting.BLUE));
            }

            if (shouldShowInTooltip(i, ItemStack.TooltipPart.CAN_DESTROY) && this.tag.contains("CanDestroy", 9)) {
                ListTag listTag2 = this.tag.getList("CanDestroy", 8);
                if (!listTag2.isEmpty()) {
                    list.add(TextComponent.EMPTY);
                    list.add((new TranslatableComponent("item.canBreak")).withStyle(ChatFormatting.GRAY));

                    for(int k = 0; k < listTag2.size(); ++k) {
                        list.addAll(expandBlockState(listTag2.getString(k)));
                    }
                }
            }

            if (shouldShowInTooltip(i, ItemStack.TooltipPart.CAN_PLACE) && this.tag.contains("CanPlaceOn", 9)) {
                ListTag listTag3 = this.tag.getList("CanPlaceOn", 8);
                if (!listTag3.isEmpty()) {
                    list.add(TextComponent.EMPTY);
                    list.add((new TranslatableComponent("item.canPlace")).withStyle(ChatFormatting.GRAY));

                    for(int l = 0; l < listTag3.size(); ++l) {
                        list.addAll(expandBlockState(listTag3.getString(l)));
                    }
                }
            }
        }

        if (context.isAdvanced()) {
            if (this.isDamaged()) {
                list.add(new TranslatableComponent("item.durability", this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()));
            }

            list.add((new TextComponent(Registry.ITEM.getKey(this.getItem()).toString())).withStyle(ChatFormatting.DARK_GRAY));
            if (this.hasTag()) {
                list.add((new TranslatableComponent("item.nbt_tags", this.tag.getAllKeys().size())).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        return list;
    }

    private static boolean shouldShowInTooltip(int flags, ItemStack.TooltipPart tooltipSection) {
        return (flags & tooltipSection.getMask()) == 0;
    }

    private int getHideFlags() {
        return this.hasTag() && this.tag.contains("HideFlags", 99) ? this.tag.getInt("HideFlags") : 0;
    }

    public void hideTooltipPart(ItemStack.TooltipPart tooltipSection) {
        CompoundTag compoundTag = this.getOrCreateTag();
        compoundTag.putInt("HideFlags", compoundTag.getInt("HideFlags") | tooltipSection.getMask());
    }

    public static void appendEnchantmentNames(List<Component> tooltip, ListTag enchantments) {
        for(int i = 0; i < enchantments.size(); ++i) {
            CompoundTag compoundTag = enchantments.getCompound(i);
            Registry.ENCHANTMENT.getOptional(EnchantmentHelper.getEnchantmentId(compoundTag)).ifPresent((e) -> {
                tooltip.add(e.getFullname(EnchantmentHelper.getEnchantmentLevel(compoundTag)));
            });
        }

    }

    private static Collection<Component> expandBlockState(String tag) {
        try {
            BlockStateParser blockStateParser = (new BlockStateParser(new StringReader(tag), true)).parse(true);
            BlockState blockState = blockStateParser.getState();
            TagKey<Block> tagKey = blockStateParser.getTag();
            boolean bl = blockState != null;
            boolean bl2 = tagKey != null;
            if (bl) {
                return Lists.newArrayList(blockState.getBlock().getName().withStyle(ChatFormatting.DARK_GRAY));
            }

            if (bl2) {
                List<Component> list = Streams.stream(Registry.BLOCK.getTagOrEmpty(tagKey)).map((entry) -> {
                    return entry.value().getName();
                }).map((text) -> {
                    return text.withStyle(ChatFormatting.DARK_GRAY);
                }).collect(Collectors.toList());
                if (!list.isEmpty()) {
                    return list;
                }
            }
        } catch (CommandSyntaxException var7) {
        }

        return Lists.newArrayList((new TextComponent("missingno")).withStyle(ChatFormatting.DARK_GRAY));
    }

    public boolean hasFoil() {
        return this.getItem().isFoil(this);
    }

    public Rarity getRarity() {
        return this.getItem().getRarity(this);
    }

    public boolean isEnchantable() {
        if (!this.getItem().isEnchantable(this)) {
            return false;
        } else {
            return !this.isEnchanted();
        }
    }

    public void enchant(Enchantment enchantment, int level) {
        this.getOrCreateTag();
        if (!this.tag.contains("Enchantments", 9)) {
            this.tag.put("Enchantments", new ListTag());
        }

        ListTag listTag = this.tag.getList("Enchantments", 10);
        listTag.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(enchantment), (byte)level));
    }

    public boolean isEnchanted() {
        if (this.tag != null && this.tag.contains("Enchantments", 9)) {
            return !this.tag.getList("Enchantments", 10).isEmpty();
        } else {
            return false;
        }
    }

    public void addTagElement(String key, Tag element) {
        this.getOrCreateTag().put(key, element);
    }

    public boolean isFramed() {
        return this.entityRepresentation instanceof ItemFrame;
    }

    public void setEntityRepresentation(@Nullable Entity holder) {
        this.entityRepresentation = holder;
    }

    @Nullable
    public ItemFrame getFrame() {
        return this.entityRepresentation instanceof ItemFrame ? (ItemFrame)this.getEntityRepresentation() : null;
    }

    @Nullable
    public Entity getEntityRepresentation() {
        return !this.emptyCacheFlag ? this.entityRepresentation : null;
    }

    public int getBaseRepairCost() {
        return this.hasTag() && this.tag.contains("RepairCost", 3) ? this.tag.getInt("RepairCost") : 0;
    }

    public void setRepairCost(int repairCost) {
        this.getOrCreateTag().putInt("RepairCost", repairCost);
    }

    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> multimap;
        if (this.hasTag() && this.tag.contains("AttributeModifiers", 9)) {
            multimap = HashMultimap.create();
            ListTag listTag = this.tag.getList("AttributeModifiers", 10);

            for(int i = 0; i < listTag.size(); ++i) {
                CompoundTag compoundTag = listTag.getCompound(i);
                if (!compoundTag.contains("Slot", 8) || compoundTag.getString("Slot").equals(slot.getName())) {
                    Optional<Attribute> optional = Registry.ATTRIBUTE.getOptional(ResourceLocation.tryParse(compoundTag.getString("AttributeName")));
                    if (optional.isPresent()) {
                        AttributeModifier attributeModifier = AttributeModifier.load(compoundTag);
                        if (attributeModifier != null && attributeModifier.getId().getLeastSignificantBits() != 0L && attributeModifier.getId().getMostSignificantBits() != 0L) {
                            multimap.put(optional.get(), attributeModifier);
                        }
                    }
                }
            }
        } else {
            multimap = this.getItem().getDefaultAttributeModifiers(slot);
        }

        return multimap;
    }

    public void addAttributeModifier(Attribute attribute, AttributeModifier modifier, @Nullable EquipmentSlot slot) {
        this.getOrCreateTag();
        if (!this.tag.contains("AttributeModifiers", 9)) {
            this.tag.put("AttributeModifiers", new ListTag());
        }

        ListTag listTag = this.tag.getList("AttributeModifiers", 10);
        CompoundTag compoundTag = modifier.save();
        compoundTag.putString("AttributeName", Registry.ATTRIBUTE.getKey(attribute).toString());
        if (slot != null) {
            compoundTag.putString("Slot", slot.getName());
        }

        listTag.add(compoundTag);
    }

    public Component getDisplayName() {
        MutableComponent mutableComponent = (new TextComponent("")).append(this.getHoverName());
        if (this.hasCustomHoverName()) {
            mutableComponent.withStyle(ChatFormatting.ITALIC);
        }

        MutableComponent mutableComponent2 = ComponentUtils.wrapInSquareBrackets(mutableComponent);
        if (!this.emptyCacheFlag) {
            mutableComponent2.withStyle(this.getRarity().color).withStyle((style) -> {
                return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(this)));
            });
        }

        return mutableComponent2;
    }

    public boolean hasAdventureModePlaceTagForBlock(Registry<Block> blockRegistry, BlockInWorld pos) {
        if (this.adventurePlaceCheck == null) {
            this.adventurePlaceCheck = new AdventureModeCheck("CanPlaceOn");
        }

        return this.adventurePlaceCheck.test(this, blockRegistry, pos);
    }

    public boolean hasAdventureModeBreakTagForBlock(Registry<Block> blockRegistry, BlockInWorld pos) {
        if (this.adventureBreakCheck == null) {
            this.adventureBreakCheck = new AdventureModeCheck("CanDestroy");
        }

        return this.adventureBreakCheck.test(this, blockRegistry, pos);
    }

    public int getPopTime() {
        return this.popTime;
    }

    public void setPopTime(int bobbingAnimationTime) {
        this.popTime = bobbingAnimationTime;
    }

    public int getCount() {
        return this.emptyCacheFlag ? 0 : this.count;
    }

    public void setCount(int count) {
        this.count = count;
        this.updateEmptyCacheFlag();
    }

    public void grow(int amount) {
        this.setCount(this.count + amount);
    }

    public void shrink(int amount) {
        this.grow(-amount);
    }

    public void onUseTick(Level world, LivingEntity user, int remainingUseTicks) {
        this.getItem().onUseTick(world, user, this, remainingUseTicks);
    }

    public void onDestroyed(ItemEntity entity) {
        this.getItem().onDestroyed(entity);
    }

    public boolean isEdible() {
        return this.getItem().isEdible();
    }

    public SoundEvent getDrinkingSound() {
        return this.getItem().getDrinkingSound();
    }

    public SoundEvent getEatingSound() {
        return this.getItem().getEatingSound();
    }

    @Nullable
    public SoundEvent getEquipSound() {
        return this.getItem().getEquipSound();
    }

    public static enum TooltipPart {
        ENCHANTMENTS,
        MODIFIERS,
        UNBREAKABLE,
        CAN_DESTROY,
        CAN_PLACE,
        ADDITIONAL,
        DYE;

        private final int mask = 1 << this.ordinal();

        public int getMask() {
            return this.mask;
        }
    }
}
