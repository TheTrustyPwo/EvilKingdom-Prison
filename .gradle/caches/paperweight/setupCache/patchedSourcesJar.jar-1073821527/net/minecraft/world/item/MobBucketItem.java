package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;

public class MobBucketItem extends BucketItem {
    private final EntityType<?> type;
    private final SoundEvent emptySound;

    public MobBucketItem(EntityType<?> type, Fluid fluid, SoundEvent emptyingSound, Item.Properties settings) {
        super(fluid, settings);
        this.type = type;
        this.emptySound = emptyingSound;
    }

    @Override
    public void checkExtraContent(@Nullable Player player, Level world, ItemStack stack, BlockPos pos) {
        if (world instanceof ServerLevel) {
            this.spawn((ServerLevel)world, stack, pos);
            world.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }

    }

    @Override
    protected void playEmptySound(@Nullable Player player, LevelAccessor world, BlockPos pos) {
        world.playSound(player, pos, this.emptySound, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private void spawn(ServerLevel world, ItemStack stack, BlockPos pos) {
        Entity entity = this.type.spawn(world, stack, (Player)null, pos, MobSpawnType.BUCKET, true, false);
        if (entity instanceof Bucketable) {
            Bucketable bucketable = (Bucketable)entity;
            bucketable.loadFromBucketTag(stack.getOrCreateTag());
            bucketable.setFromBucket(true);
        }

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        if (this.type == EntityType.TROPICAL_FISH) {
            CompoundTag compoundTag = stack.getTag();
            if (compoundTag != null && compoundTag.contains("BucketVariantTag", 3)) {
                int i = compoundTag.getInt("BucketVariantTag");
                ChatFormatting[] chatFormattings = new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY};
                String string = "color.minecraft." + TropicalFish.getBaseColor(i);
                String string2 = "color.minecraft." + TropicalFish.getPatternColor(i);

                for(int j = 0; j < TropicalFish.COMMON_VARIANTS.length; ++j) {
                    if (i == TropicalFish.COMMON_VARIANTS[j]) {
                        tooltip.add((new TranslatableComponent(TropicalFish.getPredefinedName(j))).withStyle(chatFormattings));
                        return;
                    }
                }

                tooltip.add((new TranslatableComponent(TropicalFish.getFishTypeName(i))).withStyle(chatFormattings));
                MutableComponent mutableComponent = new TranslatableComponent(string);
                if (!string.equals(string2)) {
                    mutableComponent.append(", ").append(new TranslatableComponent(string2));
                }

                mutableComponent.withStyle(chatFormattings);
                tooltip.add(mutableComponent);
            }
        }

    }
}
