package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BannerBlockEntity extends BlockEntity implements Nameable {
    public static final int MAX_PATTERNS = 6;
    public static final String TAG_PATTERNS = "Patterns";
    public static final String TAG_PATTERN = "Pattern";
    public static final String TAG_COLOR = "Color";
    @Nullable
    private Component name;
    public DyeColor baseColor;
    @Nullable
    public ListTag itemPatterns;
    @Nullable
    private List<Pair<BannerPattern, DyeColor>> patterns;

    public BannerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.BANNER, pos, state);
        this.baseColor = ((AbstractBannerBlock)state.getBlock()).getColor();
    }

    public BannerBlockEntity(BlockPos pos, BlockState state, DyeColor baseColor) {
        this(pos, state);
        this.baseColor = baseColor;
    }

    @Nullable
    public static ListTag getItemPatterns(ItemStack stack) {
        ListTag listTag = null;
        CompoundTag compoundTag = BlockItem.getBlockEntityData(stack);
        if (compoundTag != null && compoundTag.contains("Patterns", 9)) {
            listTag = compoundTag.getList("Patterns", 10).copy();
        }

        return listTag;
    }

    public void fromItem(ItemStack stack, DyeColor baseColor) {
        this.baseColor = baseColor;
        this.fromItem(stack);
    }

    public void fromItem(ItemStack stack) {
        this.itemPatterns = getItemPatterns(stack);
        this.patterns = null;
        this.name = stack.hasCustomHoverName() ? stack.getHoverName() : null;
    }

    @Override
    public Component getName() {
        return (Component)(this.name != null ? this.name : new TranslatableComponent("block.minecraft.banner"));
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    public void setCustomName(Component customName) {
        this.name = customName;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (this.itemPatterns != null) {
            nbt.put("Patterns", this.itemPatterns);
        }

        if (this.name != null) {
            nbt.putString("CustomName", Component.Serializer.toJson(this.name));
        }

    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("CustomName", 8)) {
            this.name = Component.Serializer.fromJson(nbt.getString("CustomName"));
        }

        this.itemPatterns = nbt.getList("Patterns", 10);
        this.patterns = null;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public static int getPatternCount(ItemStack stack) {
        CompoundTag compoundTag = BlockItem.getBlockEntityData(stack);
        return compoundTag != null && compoundTag.contains("Patterns") ? compoundTag.getList("Patterns", 10).size() : 0;
    }

    public List<Pair<BannerPattern, DyeColor>> getPatterns() {
        if (this.patterns == null) {
            this.patterns = createPatterns(this.baseColor, this.itemPatterns);
        }

        return this.patterns;
    }

    public static List<Pair<BannerPattern, DyeColor>> createPatterns(DyeColor baseColor, @Nullable ListTag patternListNbt) {
        List<Pair<BannerPattern, DyeColor>> list = Lists.newArrayList();
        list.add(Pair.of(BannerPattern.BASE, baseColor));
        if (patternListNbt != null) {
            for(int i = 0; i < patternListNbt.size(); ++i) {
                CompoundTag compoundTag = patternListNbt.getCompound(i);
                BannerPattern bannerPattern = BannerPattern.byHash(compoundTag.getString("Pattern"));
                if (bannerPattern != null) {
                    int j = compoundTag.getInt("Color");
                    list.add(Pair.of(bannerPattern, DyeColor.byId(j)));
                }
            }
        }

        return list;
    }

    public static void removeLastPattern(ItemStack stack) {
        CompoundTag compoundTag = BlockItem.getBlockEntityData(stack);
        if (compoundTag != null && compoundTag.contains("Patterns", 9)) {
            ListTag listTag = compoundTag.getList("Patterns", 10);
            if (!listTag.isEmpty()) {
                listTag.remove(listTag.size() - 1);
                if (listTag.isEmpty()) {
                    compoundTag.remove("Patterns");
                }

                BlockItem.setBlockEntityData(stack, BlockEntityType.BANNER, compoundTag);
            }
        }
    }

    public ItemStack getItem() {
        ItemStack itemStack = new ItemStack(BannerBlock.byColor(this.baseColor));
        if (this.itemPatterns != null && !this.itemPatterns.isEmpty()) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.put("Patterns", this.itemPatterns.copy());
            BlockItem.setBlockEntityData(itemStack, this.getType(), compoundTag);
        }

        if (this.name != null) {
            itemStack.setHoverName(this.name);
        }

        return itemStack;
    }

    public DyeColor getBaseColor() {
        return this.baseColor;
    }
}
