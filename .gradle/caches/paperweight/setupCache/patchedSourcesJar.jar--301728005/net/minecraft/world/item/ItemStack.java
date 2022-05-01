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
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.datafix.fixes.References;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

// CraftBukkit start
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.world.StructureGrowEvent;
// CraftBukkit end

public final class ItemStack {

    public static final Codec<ItemStack> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Registry.ITEM.byNameCodec().fieldOf("id").forGetter((itemstack) -> {
            return itemstack.item;
        }), Codec.INT.fieldOf("Count").forGetter((itemstack) -> {
            return itemstack.count;
        }), CompoundTag.CODEC.optionalFieldOf("tag").forGetter((itemstack) -> {
            return Optional.ofNullable(itemstack.tag);
        })).apply(instance, ItemStack::new);
    });
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ItemStack EMPTY = new ItemStack((Item) null);
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = (DecimalFormat) Util.make(new DecimalFormat("#.##"), (decimalformat) -> {
        decimalformat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
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

    // Paper start
    private static final java.util.Comparator<? super CompoundTag> enchantSorter = java.util.Comparator.comparing(o -> o.getString("id"));
    private void processEnchantOrder(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains("Enchantments", 9)) {
            return;
        }
        ListTag list = tag.getList("Enchantments", 10);
        if (list.size() < 2) {
            return;
        }
        try {
            //noinspection unchecked
            list.sort((Comparator<? super net.minecraft.nbt.Tag>) enchantSorter); // Paper
        } catch (Exception ignored) {}
    }

    private void processText() {
        CompoundTag display = getTagElement("display");
        if (display != null) {
            if (display.contains("Name", 8)) {
                String json = display.getString("Name");
                if (json != null && json.contains("\u00A7")) {
                    try {
                        display.put("Name", convert(json));
                    } catch (com.google.gson.JsonParseException jsonparseexception) {
                        display.remove("Name");
                    }
                }
            }
            if (display.contains("Lore", 9)) {
                ListTag list = display.getList("Lore", 8);
                for (int index = 0; index < list.size(); index++) {
                    String json = list.getString(index);
                    if (json != null && json.contains("\u00A7")) { // Only try if it has legacy in the unparsed json
                        try {
                            list.set(index, convert(json));
                        } catch (com.google.gson.JsonParseException e) {
                            list.set(index, net.minecraft.nbt.StringTag.valueOf(org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage.toJSON(new TextComponent(""))));
                        }
                    }
                }
            }
        }
    }

    private net.minecraft.nbt.StringTag convert(String json) {
        Component component = Component.Serializer.fromJson(json);
        if (component instanceof TextComponent && component.getContents().contains("\u00A7") && component.getSiblings().isEmpty()) {
            // Only convert if the root component is a single comp with legacy in it, don't convert already normal components
            component = org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage.fromString(component.getContents())[0];
        }
        return net.minecraft.nbt.StringTag.valueOf(org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage.toJSON(component));
    }
    // Paper end

    public ItemStack(ItemLike item) {
        this(item, 1);
    }

    public ItemStack(Holder<Item> entry) {
        this((ItemLike) entry.value(), 1);
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

    // Called to run this stack through the data converter to handle older storage methods and serialized items
    public void convertStack(int version) {
        if (0 < version && version < CraftMagicNumbers.INSTANCE.getDataVersion()) {
            CompoundTag savedStack = new CompoundTag();
            this.save(savedStack);
            savedStack = (CompoundTag) MinecraftServer.getServer().fixerUpper.update(References.ITEM_STACK, new Dynamic(NbtOps.INSTANCE, savedStack), version, CraftMagicNumbers.INSTANCE.getDataVersion()).getValue();
            this.load(savedStack);
        }
    }

    private void updateEmptyCacheFlag() {
        if (this.emptyCacheFlag && this == ItemStack.EMPTY) throw new AssertionError("TRAP"); // CraftBukkit
        this.emptyCacheFlag = false;
        this.emptyCacheFlag = this.isEmpty();
    }

    // CraftBukkit - break into own method
    private void load(CompoundTag nbttagcompound) {
        this.item = (Item) Registry.ITEM.get(new ResourceLocation(nbttagcompound.getString("id")));
        this.count = nbttagcompound.getByte("Count");
        if (nbttagcompound.contains("tag", 10)) {
            // CraftBukkit start - make defensive copy as this data may be coming from the save thread
            this.tag = nbttagcompound.getCompound("tag").copy();
            // CraftBukkit end
            this.processEnchantOrder(this.tag); // Paper
            this.processText(); // Paper
            this.getItem().verifyTagAfterLoad(this.tag);
        }

        if (this.getItem().canBeDepleted()) {
            this.setDamageValue(this.getDamageValue());
        }

    }

    private ItemStack(CompoundTag nbt) {
        this.load(nbt);
        // CraftBukkit end
        this.updateEmptyCacheFlag();
    }

    public static ItemStack of(CompoundTag nbt) {
        try {
            return new ItemStack(nbt);
        } catch (RuntimeException runtimeexception) {
            ItemStack.LOGGER.debug("Tried to load invalid item: {}", nbt, runtimeexception);
            return ItemStack.EMPTY;
        }
    }

    public boolean isEmpty() {
        return this == ItemStack.EMPTY || this.item == null || this.item == Items.AIR || this.count <= 0; // Paper
    }

    public ItemStack split(int amount) {
        int j = Math.min(amount, this.count);
        ItemStack itemstack = this.copy();

        itemstack.setCount(j);
        this.shrink(j);
        return itemstack;
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

    public InteractionResult useOn(UseOnContext itemactioncontext, InteractionHand enumhand) { // CraftBukkit - add hand
        net.minecraft.world.entity.player.Player entityhuman = itemactioncontext.getPlayer();
        BlockPos blockposition = itemactioncontext.getClickedPos();
        BlockInWorld shapedetectorblock = new BlockInWorld(itemactioncontext.getLevel(), blockposition, false);

        if (entityhuman != null && !entityhuman.getAbilities().mayBuild && !this.hasAdventureModePlaceTagForBlock(itemactioncontext.getLevel().registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY), shapedetectorblock)) {
            return InteractionResult.PASS;
        } else {
            // CraftBukkit start - handle all block place event logic here
            CompoundTag oldData = this.getTagClone();
            int oldCount = this.getCount();
            ServerLevel world = (ServerLevel) itemactioncontext.getLevel();

            if (!(this.getItem() instanceof BucketItem/* || this.getItem() instanceof SolidBucketItem*/)) { // if not bucket // Paper - capture block states for snow buckets
                world.captureBlockStates = true;
                // special case bonemeal
                if (this.getItem() == Items.BONE_MEAL) {
                    world.captureTreeGeneration = true;
                }
            }
            Item item = this.getItem();
            InteractionResult enuminteractionresult = item.useOn(itemactioncontext);
            CompoundTag newData = this.getTagClone();
            int newCount = this.getCount();
            this.setCount(oldCount);
            this.setTagClone(oldData);
            world.captureBlockStates = false;
            if (enuminteractionresult.consumesAction() && world.captureTreeGeneration && world.capturedBlockStates.size() > 0) {
                world.captureTreeGeneration = false;
                Location location = new Location(world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ());
                TreeType treeType = SaplingBlock.treeType;
                SaplingBlock.treeType = null;
                List<BlockState> blocks = new java.util.ArrayList<>(world.capturedBlockStates.values());
                world.capturedBlockStates.clear();
                StructureGrowEvent structureEvent = null;
                if (treeType != null) {
                    boolean isBonemeal = this.getItem() == Items.BONE_MEAL;
                    structureEvent = new StructureGrowEvent(location, treeType, isBonemeal, (Player) entityhuman.getBukkitEntity(), blocks);
                    org.bukkit.Bukkit.getPluginManager().callEvent(structureEvent);
                }

                BlockFertilizeEvent fertilizeEvent = new BlockFertilizeEvent(CraftBlock.at(world, blockposition), (Player) entityhuman.getBukkitEntity(), blocks);
                fertilizeEvent.setCancelled(structureEvent != null && structureEvent.isCancelled());
                org.bukkit.Bukkit.getPluginManager().callEvent(fertilizeEvent);

                if (!fertilizeEvent.isCancelled()) {
                    // Change the stack to its new contents if it hasn't been tampered with.
                    if (this.getCount() == oldCount && Objects.equals(this.tag, oldData)) {
                        this.setTag(newData);
                        this.setCount(newCount);
                    }
                    for (BlockState blockstate : blocks) {
                        blockstate.update(true);
                    }
                }

                SignItem.openSign = null; // SPIGOT-6758 - Reset on early return
                return enuminteractionresult;
            }
            world.captureTreeGeneration = false;

            if (entityhuman != null && enuminteractionresult.shouldAwardStats()) {
                org.bukkit.event.block.BlockPlaceEvent placeEvent = null;
                List<BlockState> blocks = new java.util.ArrayList<>(world.capturedBlockStates.values());
                world.capturedBlockStates.clear();
                if (blocks.size() > 1) {
                    placeEvent = org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callBlockMultiPlaceEvent(world, entityhuman, enumhand, blocks, blockposition.getX(), blockposition.getY(), blockposition.getZ());
                } else if (blocks.size() == 1 && item != Items.POWDER_SNOW_BUCKET) { // Paper - don't call event twice for snow buckets
                    placeEvent = org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callBlockPlaceEvent(world, entityhuman, enumhand, blocks.get(0), blockposition.getX(), blockposition.getY(), blockposition.getZ());
                }

                if (placeEvent != null && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
                    enuminteractionresult = InteractionResult.FAIL; // cancel placement
                    // PAIL: Remove this when MC-99075 fixed
                    placeEvent.getPlayer().updateInventory();
                    world.capturedTileEntities.clear(); // Paper - clear out tile entities as chests and such will pop loot
                    // revert back all captured blocks
                    world.preventPoiUpdated = true; // CraftBukkit - SPIGOT-5710
                    for (BlockState blockstate : blocks) {
                        blockstate.update(true, false);
                    }
                    world.preventPoiUpdated = false;

                    // Brute force all possible updates
                    BlockPos placedPos = ((CraftBlock) placeEvent.getBlock()).getPosition();
                    for (Direction dir : Direction.values()) {
                        ((ServerPlayer) entityhuman).connection.send(new ClientboundBlockUpdatePacket(world, placedPos.relative(dir)));
                    }
                    SignItem.openSign = null; // SPIGOT-6758 - Reset on early return
                } else {
                    // Change the stack to its new contents if it hasn't been tampered with.
                    if (this.getCount() == oldCount && Objects.equals(this.tag, oldData)) {
                        this.setTag(newData);
                        this.setCount(newCount);
                    }

                    for (Map.Entry<BlockPos, BlockEntity> e : world.capturedTileEntities.entrySet()) {
                        world.setBlockEntity(e.getValue());
                    }

                    for (BlockState blockstate : blocks) {
                        int updateFlag = ((CraftBlockState) blockstate).getFlag();
                        net.minecraft.world.level.block.state.BlockState oldBlock = ((CraftBlockState) blockstate).getHandle();
                        BlockPos newblockposition = ((CraftBlockState) blockstate).getPosition();
                        net.minecraft.world.level.block.state.BlockState block = world.getBlockState(newblockposition);

                        if (!(block.getBlock() instanceof BaseEntityBlock)) { // Containers get placed automatically
                            block.getBlock().onPlace(block, world, newblockposition, oldBlock, true, itemactioncontext); // Paper - pass itemactioncontext
                        }

                        world.notifyAndUpdatePhysics(newblockposition, null, oldBlock, block, world.getBlockState(newblockposition), updateFlag, 512); // send null chunk as chunk.k() returns false by this point
                    }

                    // Special case juke boxes as they update their tile entity. Copied from ItemRecord.
                    // PAIL: checkme on updates.
                    if (this.item instanceof RecordItem) {
                        ((JukeboxBlock) Blocks.JUKEBOX).setRecord(world, blockposition, world.getBlockState(blockposition), this);
                        world.levelEvent((net.minecraft.world.entity.player.Player) null, 1010, blockposition, Item.getId(this.item));
                        this.shrink(1);
                        entityhuman.awardStat(Stats.PLAY_RECORD);
                    }

                    if (this.item == Items.WITHER_SKELETON_SKULL) { // Special case skulls to allow wither spawns to be cancelled
                        BlockPos bp = blockposition;
                        if (!world.getBlockState(blockposition).getMaterial().isReplaceable()) {
                            if (!world.getBlockState(blockposition).getMaterial().isSolid()) {
                                bp = null;
                            } else {
                                bp = bp.relative(itemactioncontext.getClickedFace());
                            }
                        }
                        if (bp != null) {
                            BlockEntity te = world.getBlockEntity(bp);
                            if (te instanceof SkullBlockEntity) {
                                WitherSkullBlock.checkSpawn(world, bp, (SkullBlockEntity) te);
                            }
                        }
                    }

                    // SPIGOT-4678
                    if (this.item instanceof SignItem && SignItem.openSign != null) {
                        try {
                            entityhuman.openTextEdit((SignBlockEntity) world.getBlockEntity(SignItem.openSign));
                        } finally {
                            SignItem.openSign = null;
                        }
                    }

                    // SPIGOT-1288 - play sound stripped from ItemBlock
                    if (this.item instanceof BlockItem) {
                        SoundType soundeffecttype = ((BlockItem) this.item).getBlock().getSoundType(null);
                        world.playSound(entityhuman, blockposition, soundeffecttype.getPlaceSound(), SoundSource.BLOCKS, (soundeffecttype.getVolume() + 1.0F) / 2.0F, soundeffecttype.getPitch() * 0.8F);
                    }

                    entityhuman.awardStat(Stats.ITEM_USED.get(item));
                }
            }
            world.capturedTileEntities.clear();
            world.capturedBlockStates.clear();
            // CraftBukkit end

            return enuminteractionresult;
        }
    }

    public float getDestroySpeed(net.minecraft.world.level.block.state.BlockState state) {
        return this.getItem().getDestroySpeed(this, state);
    }

    public InteractionResultHolder<ItemStack> use(Level world, net.minecraft.world.entity.player.Player user, InteractionHand hand) {
        return this.getItem().use(world, user, hand);
    }

    public ItemStack finishUsingItem(Level world, LivingEntity user) {
        return this.getItem().finishUsingItem(this, world, user);
    }

    public CompoundTag save(CompoundTag nbt) {
        ResourceLocation minecraftkey = Registry.ITEM.getKey(this.getItem());

        nbt.putString("id", minecraftkey == null ? "minecraft:air" : minecraftkey.toString());
        nbt.putByte("Count", (byte) this.count);
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
            CompoundTag nbttagcompound = this.getTag();

            return nbttagcompound == null || !nbttagcompound.getBoolean("Unbreakable");
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

    public boolean hurt(int amount, Random random, @Nullable LivingEntity player) { // Paper - allow any living entity instead of only ServerPlayers
        if (!this.isDamageableItem()) {
            return false;
        } else {
            int j;

            if (amount > 0) {
                j = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, this);
                int k = 0;

                for (int l = 0; j > 0 && l < amount; ++l) {
                    if (DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(this, j, random)) {
                        ++k;
                    }
                }

                int originalDamage = amount; // Paper
                amount -= k;
                // CraftBukkit start
                if (player instanceof ServerPlayer serverPlayer) { // Paper
                    PlayerItemDamageEvent event = new PlayerItemDamageEvent(serverPlayer.getBukkitEntity(), CraftItemStack.asCraftMirror(this), amount, originalDamage); // Paper
                    event.getPlayer().getServer().getPluginManager().callEvent(event);

                    if (amount != event.getDamage() || event.isCancelled()) {
                        event.getPlayer().updateInventory();
                    }
                    if (event.isCancelled()) {
                        return false;
                    }

                    amount = event.getDamage();
                    // Paper start - EntityDamageItemEvent
                } else if (player != null) {
                    io.papermc.paper.event.entity.EntityDamageItemEvent event = new io.papermc.paper.event.entity.EntityDamageItemEvent(player.getBukkitLivingEntity(), CraftItemStack.asCraftMirror(this), amount);
                    if (!event.callEvent()) {
                        return false;
                    }
                    amount = event.getDamage();
                    // Paper end
                }
                // CraftBukkit end
                if (amount <= 0) {
                    return false;
                }
            }

            if (player instanceof ServerPlayer serverPlayer && amount != 0) { // Paper
                CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(serverPlayer, this, this.getDamageValue() + amount); // Paper
            }

            j = this.getDamageValue() + amount;
            this.setDamageValue(j);
            return j >= this.getMaxDamage();
        }
    }

    public <T extends LivingEntity> void hurtAndBreak(int amount, T entity, Consumer<T> breakCallback) {
        if (!entity.level.isClientSide && (!(entity instanceof net.minecraft.world.entity.player.Player) || !((net.minecraft.world.entity.player.Player) entity).getAbilities().instabuild)) {
            if (this.isDamageableItem()) {
                if (this.hurt(amount, entity.getRandom(), entity /*instanceof ServerPlayer ? (ServerPlayer) entity : null*/)) { // Paper - pass LivingEntity for EntityItemDamageEvent
                    breakCallback.accept(entity);
                    Item item = this.getItem();
                    // CraftBukkit start - Check for item breaking
                    if (this.count == 1 && entity instanceof net.minecraft.world.entity.player.Player) {
                        org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPlayerItemBreakEvent((net.minecraft.world.entity.player.Player) entity, this);
                    }
                    // CraftBukkit end

                    this.shrink(1);
                    if (entity instanceof net.minecraft.world.entity.player.Player) {
                        ((net.minecraft.world.entity.player.Player) entity).awardStat(Stats.ITEM_BROKEN.get(item));
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

    public boolean overrideStackedOnOther(Slot slot, ClickAction clickType, net.minecraft.world.entity.player.Player player) {
        return this.getItem().overrideStackedOnOther(this, slot, clickType, player);
    }

    public boolean overrideOtherStackedOnMe(ItemStack stack, Slot slot, ClickAction clickType, net.minecraft.world.entity.player.Player player, SlotAccess cursorStackReference) {
        return this.getItem().overrideOtherStackedOnMe(this, stack, slot, clickType, player, cursorStackReference);
    }

    public void hurtEnemy(LivingEntity target, net.minecraft.world.entity.player.Player attacker) {
        Item item = this.getItem();

        if (item.hurtEnemy(this, target, attacker)) {
            attacker.awardStat(Stats.ITEM_USED.get(item));
        }

    }

    public void mineBlock(Level world, net.minecraft.world.level.block.state.BlockState state, BlockPos pos, net.minecraft.world.entity.player.Player miner) {
        Item item = this.getItem();

        if (item.mineBlock(this, world, state, pos, miner)) {
            miner.awardStat(Stats.ITEM_USED.get(item));
        }

    }

    public boolean isCorrectToolForDrops(net.minecraft.world.level.block.state.BlockState state) {
        return this.getItem().isCorrectToolForDrops(state);
    }

    public InteractionResult interactLivingEntity(net.minecraft.world.entity.player.Player user, LivingEntity entity, InteractionHand hand) {
        return this.getItem().interactLivingEntity(this, user, entity, hand);
    }

    public ItemStack copy() { return cloneItemStack(false); } // Paper
    public ItemStack cloneItemStack(boolean origItem) { // Paper
        if (!origItem && this.isEmpty()) { // Paper
            return ItemStack.EMPTY;
        } else {
            ItemStack itemstack = new ItemStack(origItem ? this.item : this.getItem(), this.count); // Paper

            itemstack.setPopTime(this.getPopTime());
            if (this.tag != null) {
                itemstack.tag = this.tag.copy();
            }

            return itemstack;
        }
    }

    public static boolean tagMatches(ItemStack left, ItemStack right) {
        return left.isEmpty() && right.isEmpty() ? true : (!left.isEmpty() && !right.isEmpty() ? (left.tag == null && right.tag != null ? false : left.tag == null || left.tag.equals(right.tag)) : false);
    }

    public static boolean matches(ItemStack left, ItemStack right) {
        return left.isEmpty() && right.isEmpty() ? true : (!left.isEmpty() && !right.isEmpty() ? left.matches(right) : false);
    }

    private boolean matches(ItemStack stack) {
        return this.count != stack.count ? false : (!this.is(stack.getItem()) ? false : (this.tag == null && stack.tag != null ? false : this.tag == null || this.tag.equals(stack.tag)));
    }

    public static boolean isSame(ItemStack left, ItemStack right) {
        return left == right ? true : (!left.isEmpty() && !right.isEmpty() ? left.sameItem(right) : false);
    }

    public static boolean isSameIgnoreDurability(ItemStack left, ItemStack right) {
        return left == right ? true : (!left.isEmpty() && !right.isEmpty() ? left.sameItemStackIgnoreDurability(right) : false);
    }

    public boolean sameItem(ItemStack stack) {
        return !stack.isEmpty() && this.is(stack.getItem());
    }

    public boolean sameItemStackIgnoreDurability(ItemStack stack) {
        return !this.isDamageableItem() ? this.sameItem(stack) : !stack.isEmpty() && this.is(stack.getItem());
    }

    public static boolean isSameItemSameTags(ItemStack stack, ItemStack otherStack) {
        return stack.is(otherStack.getItem()) && ItemStack.tagMatches(stack, otherStack);
    }

    public String getDescriptionId() {
        return this.getItem().getDescriptionId(this);
    }

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

    public void onCraftedBy(Level world, net.minecraft.world.entity.player.Player player, int amount) {
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

    // CraftBukkit start
    @Nullable
    private CompoundTag getTagClone() {
        return this.tag == null ? null : this.tag.copy();
    }

    private void setTagClone(@Nullable CompoundTag nbtttagcompound) {
        this.setTag(nbtttagcompound == null ? null : nbtttagcompound.copy());
    }
    // CraftBukkit end

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
            CompoundTag nbttagcompound = new CompoundTag();

            this.addTagElement(key, nbttagcompound);
            return nbttagcompound;
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

    // Paper start - (this is just a good no conflict location)
    public org.bukkit.inventory.ItemStack asBukkitMirror() {
        return CraftItemStack.asCraftMirror(this);
    }
    public org.bukkit.inventory.ItemStack asBukkitCopy() {
        return CraftItemStack.asCraftMirror(this.copy());
    }
    public static ItemStack fromBukkitCopy(org.bukkit.inventory.ItemStack itemstack) {
        return CraftItemStack.asNMSCopy(itemstack);
    }
    private org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack bukkitStack;
    public org.bukkit.inventory.ItemStack getBukkitStack() {
        if (bukkitStack == null || bukkitStack.handle != this) {
            bukkitStack = org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack.asCraftMirror(this);
        }
        return bukkitStack;
    }
    // Paper end

    public void setTag(@Nullable CompoundTag nbt) {
        this.tag = nbt;
        this.processEnchantOrder(this.tag); // Paper
        if (this.getItem().canBeDepleted()) {
            this.setDamageValue(this.getDamageValue());
        }

        if (nbt != null) {
            this.getItem().verifyTagAfterLoad(nbt);
        }

    }

    public Component getHoverName() {
        CompoundTag nbttagcompound = this.getTagElement("display");

        if (nbttagcompound != null && nbttagcompound.contains("Name", 8)) {
            try {
                MutableComponent ichatmutablecomponent = Component.Serializer.fromJson(nbttagcompound.getString("Name"));

                if (ichatmutablecomponent != null) {
                    return ichatmutablecomponent;
                }

                nbttagcompound.remove("Name");
            } catch (Exception exception) {
                nbttagcompound.remove("Name");
            }
        }

        return this.getItem().getName(this);
    }

    public ItemStack setHoverName(@Nullable Component name) {
        CompoundTag nbttagcompound = this.getOrCreateTagElement("display");

        if (name != null) {
            nbttagcompound.putString("Name", Component.Serializer.toJson(name));
        } else {
            nbttagcompound.remove("Name");
        }

        return this;
    }

    public void resetHoverName() {
        CompoundTag nbttagcompound = this.getTagElement("display");

        if (nbttagcompound != null) {
            nbttagcompound.remove("Name");
            if (nbttagcompound.isEmpty()) {
                this.removeTagKey("display");
            }
        }

        if (this.tag != null && this.tag.isEmpty()) {
            this.tag = null;
        }

    }

    public boolean hasCustomHoverName() {
        CompoundTag nbttagcompound = this.getTagElement("display");

        return nbttagcompound != null && nbttagcompound.contains("Name", 8);
    }

    public List<Component> getTooltipLines(@Nullable net.minecraft.world.entity.player.Player player, TooltipFlag context) {
        List<Component> list = Lists.newArrayList();
        MutableComponent ichatmutablecomponent = (new TextComponent("")).append(this.getHoverName()).withStyle(this.getRarity().color);

        if (this.hasCustomHoverName()) {
            ichatmutablecomponent.withStyle(ChatFormatting.ITALIC);
        }

        list.add(ichatmutablecomponent);
        if (!context.isAdvanced() && !this.hasCustomHoverName() && this.is(Items.FILLED_MAP)) {
            Integer integer = MapItem.getMapId(this);

            if (integer != null) {
                list.add((new TextComponent("#" + integer)).withStyle(ChatFormatting.GRAY));
            }
        }

        int i = this.getHideFlags();

        if (ItemStack.shouldShowInTooltip(i, ItemStack.TooltipPart.ADDITIONAL)) {
            this.getItem().appendHoverText(this, player == null ? null : player.level, list, context);
        }

        int j;

        if (this.hasTag()) {
            if (ItemStack.shouldShowInTooltip(i, ItemStack.TooltipPart.ENCHANTMENTS)) {
                ItemStack.appendEnchantmentNames(list, this.getEnchantmentTags());
            }

            if (this.tag.contains("display", 10)) {
                CompoundTag nbttagcompound = this.tag.getCompound("display");

                if (ItemStack.shouldShowInTooltip(i, ItemStack.TooltipPart.DYE) && nbttagcompound.contains("color", 99)) {
                    if (context.isAdvanced()) {
                        list.add((new TranslatableComponent("item.color", new Object[]{String.format("#%06X", nbttagcompound.getInt("color"))})).withStyle(ChatFormatting.GRAY));
                    } else {
                        list.add((new TranslatableComponent("item.dyed")).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
                    }
                }

                if (nbttagcompound.getTagType("Lore") == 9) {
                    ListTag nbttaglist = nbttagcompound.getList("Lore", 8);

                    for (j = 0; j < nbttaglist.size(); ++j) {
                        String s = nbttaglist.getString(j);

                        try {
                            MutableComponent ichatmutablecomponent1 = Component.Serializer.fromJson(s);

                            if (ichatmutablecomponent1 != null) {
                                list.add(ComponentUtils.mergeStyles(ichatmutablecomponent1, ItemStack.LORE_STYLE));
                            }
                        } catch (Exception exception) {
                            nbttagcompound.remove("Lore");
                        }
                    }
                }
            }
        }

        int k;

        if (ItemStack.shouldShowInTooltip(i, ItemStack.TooltipPart.MODIFIERS)) {
            EquipmentSlot[] aenumitemslot = EquipmentSlot.values();

            k = aenumitemslot.length;

            for (j = 0; j < k; ++j) {
                EquipmentSlot enumitemslot = aenumitemslot[j];
                Multimap<Attribute, AttributeModifier> multimap = this.getAttributeModifiers(enumitemslot);

                if (!multimap.isEmpty()) {
                    list.add(TextComponent.EMPTY);
                    list.add((new TranslatableComponent("item.modifiers." + enumitemslot.getName())).withStyle(ChatFormatting.GRAY));
                    Iterator iterator = multimap.entries().iterator();

                    while (iterator.hasNext()) {
                        Entry<Attribute, AttributeModifier> entry = (Entry) iterator.next();
                        AttributeModifier attributemodifier = (AttributeModifier) entry.getValue();
                        double d0 = attributemodifier.getAmount();
                        boolean flag = false;

                        if (player != null) {
                            if (attributemodifier.getId() == Item.BASE_ATTACK_DAMAGE_UUID) {
                                d0 += player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                                d0 += (double) EnchantmentHelper.getDamageBonus(this, MobType.UNDEFINED);
                                flag = true;
                            } else if (attributemodifier.getId() == Item.BASE_ATTACK_SPEED_UUID) {
                                d0 += player.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                                flag = true;
                            }
                        }

                        double d1;

                        if (attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
                            if (((Attribute) entry.getKey()).equals(Attributes.KNOCKBACK_RESISTANCE)) {
                                d1 = d0 * 10.0D;
                            } else {
                                d1 = d0;
                            }
                        } else {
                            d1 = d0 * 100.0D;
                        }

                        if (flag) {
                            list.add((new TextComponent(" ")).append((Component) (new TranslatableComponent("attribute.modifier.equals." + attributemodifier.getOperation().toValue(), new Object[]{ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d1), new TranslatableComponent(((Attribute) entry.getKey()).getDescriptionId())}))).withStyle(ChatFormatting.DARK_GREEN));
                        } else if (d0 > 0.0D) {
                            list.add((new TranslatableComponent("attribute.modifier.plus." + attributemodifier.getOperation().toValue(), new Object[]{ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d1), new TranslatableComponent(((Attribute) entry.getKey()).getDescriptionId())})).withStyle(ChatFormatting.BLUE));
                        } else if (d0 < 0.0D) {
                            d1 *= -1.0D;
                            list.add((new TranslatableComponent("attribute.modifier.take." + attributemodifier.getOperation().toValue(), new Object[]{ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d1), new TranslatableComponent(((Attribute) entry.getKey()).getDescriptionId())})).withStyle(ChatFormatting.RED));
                        }
                    }
                }
            }
        }

        if (this.hasTag()) {
            if (ItemStack.shouldShowInTooltip(i, ItemStack.TooltipPart.UNBREAKABLE) && this.tag.getBoolean("Unbreakable")) {
                list.add((new TranslatableComponent("item.unbreakable")).withStyle(ChatFormatting.BLUE));
            }

            ListTag nbttaglist1;

            if (ItemStack.shouldShowInTooltip(i, ItemStack.TooltipPart.CAN_DESTROY) && this.tag.contains("CanDestroy", 9)) {
                nbttaglist1 = this.tag.getList("CanDestroy", 8);
                if (!nbttaglist1.isEmpty()) {
                    list.add(TextComponent.EMPTY);
                    list.add((new TranslatableComponent("item.canBreak")).withStyle(ChatFormatting.GRAY));

                    for (k = 0; k < nbttaglist1.size(); ++k) {
                        list.addAll(ItemStack.expandBlockState(nbttaglist1.getString(k)));
                    }
                }
            }

            if (ItemStack.shouldShowInTooltip(i, ItemStack.TooltipPart.CAN_PLACE) && this.tag.contains("CanPlaceOn", 9)) {
                nbttaglist1 = this.tag.getList("CanPlaceOn", 8);
                if (!nbttaglist1.isEmpty()) {
                    list.add(TextComponent.EMPTY);
                    list.add((new TranslatableComponent("item.canPlace")).withStyle(ChatFormatting.GRAY));

                    for (k = 0; k < nbttaglist1.size(); ++k) {
                        list.addAll(ItemStack.expandBlockState(nbttaglist1.getString(k)));
                    }
                }
            }
        }

        if (context.isAdvanced()) {
            if (this.isDamaged()) {
                list.add(new TranslatableComponent("item.durability", new Object[]{this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()}));
            }

            list.add((new TextComponent(Registry.ITEM.getKey(this.getItem()).toString())).withStyle(ChatFormatting.DARK_GRAY));
            if (this.hasTag()) {
                list.add((new TranslatableComponent("item.nbt_tags", new Object[]{this.tag.getAllKeys().size()})).withStyle(ChatFormatting.DARK_GRAY));
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
        CompoundTag nbttagcompound = this.getOrCreateTag();

        nbttagcompound.putInt("HideFlags", nbttagcompound.getInt("HideFlags") | tooltipSection.getMask());
    }

    public static void appendEnchantmentNames(List<Component> tooltip, ListTag enchantments) {
        for (int i = 0; i < enchantments.size(); ++i) {
            CompoundTag nbttagcompound = enchantments.getCompound(i);

            Registry.ENCHANTMENT.getOptional(EnchantmentHelper.getEnchantmentId(nbttagcompound)).ifPresent((enchantment) -> {
                tooltip.add(enchantment.getFullname(EnchantmentHelper.getEnchantmentLevel(nbttagcompound)));
            });
        }

    }

    private static Collection<Component> expandBlockState(String tag) {
        try {
            BlockStateParser argumentblock = (new BlockStateParser(new StringReader(tag), true)).parse(true);
            net.minecraft.world.level.block.state.BlockState iblockdata = argumentblock.getState();
            TagKey<Block> tagkey = argumentblock.getTag();
            boolean flag = iblockdata != null;
            boolean flag1 = tagkey != null;

            if (flag) {
                return Lists.newArrayList(new Component[]{iblockdata.getBlock().getName().withStyle(ChatFormatting.DARK_GRAY)});
            }

            if (flag1) {
                List<Component> list = (List) Streams.stream(Registry.BLOCK.getTagOrEmpty(tagkey)).map((holder) -> {
                    return ((Block) holder.value()).getName();
                }).map((ichatmutablecomponent) -> {
                    return ichatmutablecomponent.withStyle(ChatFormatting.DARK_GRAY);
                }).collect(Collectors.toList());

                if (!list.isEmpty()) {
                    return list;
                }
            }
        } catch (CommandSyntaxException commandsyntaxexception) {
            ;
        }

        return Lists.newArrayList(new Component[]{(new TextComponent("missingno")).withStyle(ChatFormatting.DARK_GRAY)});
    }

    public boolean hasFoil() {
        return this.getItem().isFoil(this);
    }

    public Rarity getRarity() {
        return this.getItem().getRarity(this);
    }

    public boolean isEnchantable() {
        return !this.getItem().isEnchantable(this) ? false : !this.isEnchanted();
    }

    public void enchant(Enchantment enchantment, int level) {
        this.getOrCreateTag();
        if (!this.tag.contains("Enchantments", 9)) {
            this.tag.put("Enchantments", new ListTag());
        }

        ListTag nbttaglist = this.tag.getList("Enchantments", 10);

        nbttaglist.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(enchantment), (byte) level));
        processEnchantOrder(this.tag); // Paper
    }

    public boolean isEnchanted() {
        return this.tag != null && this.tag.contains("Enchantments", 9) ? !this.tag.getList("Enchantments", 10).isEmpty() : false;
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
        return this.entityRepresentation instanceof ItemFrame ? (ItemFrame) this.getEntityRepresentation() : null;
    }

    @Nullable
    public Entity getEntityRepresentation() {
        return !this.emptyCacheFlag ? this.entityRepresentation : null;
    }

    public int getBaseRepairCost() {
        return this.hasTag() && this.tag.contains("RepairCost", 3) ? this.tag.getInt("RepairCost") : 0;
    }

    public void setRepairCost(int repairCost) {
        // CraftBukkit start - remove RepairCost tag when 0 (SPIGOT-3945)
        if (repairCost == 0) {
            this.removeTagKey("RepairCost");
            return;
        }
        // CraftBukkit end
        this.getOrCreateTag().putInt("RepairCost", repairCost);
    }

    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        Object object;

        if (this.hasTag() && this.tag.contains("AttributeModifiers", 9)) {
            object = HashMultimap.create();
            ListTag nbttaglist = this.tag.getList("AttributeModifiers", 10);

            for (int i = 0; i < nbttaglist.size(); ++i) {
                CompoundTag nbttagcompound = nbttaglist.getCompound(i);

                if (!nbttagcompound.contains("Slot", 8) || nbttagcompound.getString("Slot").equals(slot.getName())) {
                    Optional<Attribute> optional = Registry.ATTRIBUTE.getOptional(ResourceLocation.tryParse(nbttagcompound.getString("AttributeName")));

                    if (optional.isPresent()) {
                        AttributeModifier attributemodifier = AttributeModifier.load(nbttagcompound);

                        if (attributemodifier != null && attributemodifier.getId().getLeastSignificantBits() != 0L && attributemodifier.getId().getMostSignificantBits() != 0L) {
                            ((Multimap) object).put((Attribute) optional.get(), attributemodifier);
                        }
                    }
                }
            }
        } else {
            object = this.getItem().getDefaultAttributeModifiers(slot);
        }

        return (Multimap) object;
    }

    public void addAttributeModifier(Attribute attribute, AttributeModifier modifier, @Nullable EquipmentSlot slot) {
        this.getOrCreateTag();
        if (!this.tag.contains("AttributeModifiers", 9)) {
            this.tag.put("AttributeModifiers", new ListTag());
        }

        ListTag nbttaglist = this.tag.getList("AttributeModifiers", 10);
        CompoundTag nbttagcompound = modifier.save();

        nbttagcompound.putString("AttributeName", Registry.ATTRIBUTE.getKey(attribute).toString());
        if (slot != null) {
            nbttagcompound.putString("Slot", slot.getName());
        }

        nbttaglist.add(nbttagcompound);
    }

    // CraftBukkit start
    @Deprecated
    public void setItem(Item item) {
        this.bukkitStack = null; // Paper
        this.item = item;
    }
    // CraftBukkit end

    public Component getDisplayName() {
        MutableComponent ichatmutablecomponent = (new TextComponent("")).append(this.getHoverName());

        if (this.hasCustomHoverName()) {
            ichatmutablecomponent.withStyle(ChatFormatting.ITALIC);
        }

        MutableComponent ichatmutablecomponent1 = ComponentUtils.wrapInSquareBrackets(ichatmutablecomponent);

        if (!this.emptyCacheFlag) {
            ichatmutablecomponent1.withStyle(this.getRarity().color).withStyle((chatmodifier) -> {
                return chatmodifier.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(this)));
            });
        }

        return ichatmutablecomponent1;
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

        ENCHANTMENTS, MODIFIERS, UNBREAKABLE, CAN_DESTROY, CAN_PLACE, ADDITIONAL, DYE;

        private final int mask = 1 << this.ordinal();

        private TooltipPart() {}

        public int getMask() {
            return this.mask;
        }
    }
}
