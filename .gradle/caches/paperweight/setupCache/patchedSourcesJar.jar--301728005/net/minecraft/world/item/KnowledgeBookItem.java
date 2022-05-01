package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class KnowledgeBookItem extends Item {
    private static final String RECIPE_TAG = "Recipes";
    private static final Logger LOGGER = LogUtils.getLogger();

    public KnowledgeBookItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        CompoundTag compoundTag = itemStack.getTag();
        if (!user.getAbilities().instabuild) {
            user.setItemInHand(hand, ItemStack.EMPTY);
        }

        if (compoundTag != null && compoundTag.contains("Recipes", 9)) {
            if (!world.isClientSide) {
                ListTag listTag = compoundTag.getList("Recipes", 8);
                List<Recipe<?>> list = Lists.newArrayList();
                RecipeManager recipeManager = world.getServer().getRecipeManager();

                for(int i = 0; i < listTag.size(); ++i) {
                    String string = listTag.getString(i);
                    Optional<? extends Recipe<?>> optional = recipeManager.byKey(new ResourceLocation(string));
                    if (!optional.isPresent()) {
                        LOGGER.error("Invalid recipe: {}", (Object)string);
                        return InteractionResultHolder.fail(itemStack);
                    }

                    list.add(optional.get());
                }

                user.awardRecipes(list);
                user.awardStat(Stats.ITEM_USED.get(this));
            }

            return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
        } else {
            LOGGER.error("Tag not valid: {}", (Object)compoundTag);
            return InteractionResultHolder.fail(itemStack);
        }
    }
}
