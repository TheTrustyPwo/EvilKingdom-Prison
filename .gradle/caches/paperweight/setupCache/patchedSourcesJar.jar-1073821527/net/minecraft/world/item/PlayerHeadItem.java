package net.minecraft.world.item;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.apache.commons.lang3.StringUtils;

public class PlayerHeadItem extends StandingAndWallBlockItem {
    public static final String TAG_SKULL_OWNER = "SkullOwner";

    public PlayerHeadItem(Block standingBlock, Block wallBlock, Item.Properties settings) {
        super(standingBlock, wallBlock, settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.is(Items.PLAYER_HEAD) && stack.hasTag()) {
            String string = null;
            CompoundTag compoundTag = stack.getTag();
            if (compoundTag.contains("SkullOwner", 8)) {
                string = compoundTag.getString("SkullOwner");
            } else if (compoundTag.contains("SkullOwner", 10)) {
                CompoundTag compoundTag2 = compoundTag.getCompound("SkullOwner");
                if (compoundTag2.contains("Name", 8)) {
                    string = compoundTag2.getString("Name");
                }
            }

            if (string != null) {
                return new TranslatableComponent(this.getDescriptionId() + ".named", string);
            }
        }

        return super.getName(stack);
    }

    @Override
    public void verifyTagAfterLoad(CompoundTag nbt) {
        super.verifyTagAfterLoad(nbt);
        if (nbt.contains("SkullOwner", 8) && !StringUtils.isBlank(nbt.getString("SkullOwner"))) {
            GameProfile gameProfile = new GameProfile((UUID)null, nbt.getString("SkullOwner"));
            SkullBlockEntity.updateGameprofile(gameProfile, (profile) -> {
                nbt.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), profile));
            });
        }

    }
}
