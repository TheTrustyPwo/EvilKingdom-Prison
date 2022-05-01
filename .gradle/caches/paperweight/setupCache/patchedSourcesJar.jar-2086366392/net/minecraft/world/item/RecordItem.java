package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;

public class RecordItem extends Item {

    private static final Map<SoundEvent, RecordItem> BY_NAME = Maps.newHashMap();
    private final int analogOutput;
    private final SoundEvent sound;

    protected RecordItem(int comparatorOutput, SoundEvent sound, Item.Properties settings) {
        super(settings);
        this.analogOutput = comparatorOutput;
        this.sound = sound;
        RecordItem.BY_NAME.put(this.sound, this);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos blockposition = context.getClickedPos();
        BlockState iblockdata = world.getBlockState(blockposition);

        if (iblockdata.is(Blocks.JUKEBOX) && !(Boolean) iblockdata.getValue(JukeboxBlock.HAS_RECORD)) {
            ItemStack itemstack = context.getItemInHand();

            if (!world.isClientSide) {
                if (true) return InteractionResult.SUCCESS; // CraftBukkit - handled in ItemStack
                ((JukeboxBlock) Blocks.JUKEBOX).setRecord(world, blockposition, iblockdata, itemstack);
                world.levelEvent((Player) null, 1010, blockposition, Item.getId(this));
                itemstack.shrink(1);
                Player entityhuman = context.getPlayer();

                if (entityhuman != null) {
                    entityhuman.awardStat(Stats.PLAY_RECORD);
                }
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    public int getAnalogOutput() {
        return this.analogOutput;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        tooltip.add(this.getDisplayName().withStyle(ChatFormatting.GRAY));
    }

    public MutableComponent getDisplayName() {
        return new TranslatableComponent(this.getDescriptionId() + ".desc");
    }

    @Nullable
    public static RecordItem getBySound(SoundEvent sound) {
        return (RecordItem) RecordItem.BY_NAME.get(sound);
    }

    public SoundEvent getSound() {
        return this.sound;
    }
}
