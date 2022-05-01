package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.PredicateManager;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ResourceLocationArgument implements ArgumentType<ResourceLocation> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ADVANCEMENT = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("advancement.advancementNotFound", id);
    });
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_RECIPE = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("recipe.notFound", id);
    });
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PREDICATE = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("predicate.unknown", id);
    });
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM_MODIFIER = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("item_modifier.unknown", id);
    });

    public static ResourceLocationArgument id() {
        return new ResourceLocationArgument();
    }

    public static Advancement getAdvancement(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
        ResourceLocation resourceLocation = getId(context, argumentName);
        Advancement advancement = context.getSource().getServer().getAdvancements().getAdvancement(resourceLocation);
        if (advancement == null) {
            throw ERROR_UNKNOWN_ADVANCEMENT.create(resourceLocation);
        } else {
            return advancement;
        }
    }

    public static Recipe<?> getRecipe(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
        RecipeManager recipeManager = context.getSource().getServer().getRecipeManager();
        ResourceLocation resourceLocation = getId(context, argumentName);
        return recipeManager.byKey(resourceLocation).orElseThrow(() -> {
            return ERROR_UNKNOWN_RECIPE.create(resourceLocation);
        });
    }

    public static LootItemCondition getPredicate(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
        ResourceLocation resourceLocation = getId(context, argumentName);
        PredicateManager predicateManager = context.getSource().getServer().getPredicateManager();
        LootItemCondition lootItemCondition = predicateManager.get(resourceLocation);
        if (lootItemCondition == null) {
            throw ERROR_UNKNOWN_PREDICATE.create(resourceLocation);
        } else {
            return lootItemCondition;
        }
    }

    public static LootItemFunction getItemModifier(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
        ResourceLocation resourceLocation = getId(context, argumentName);
        ItemModifierManager itemModifierManager = context.getSource().getServer().getItemModifierManager();
        LootItemFunction lootItemFunction = itemModifierManager.get(resourceLocation);
        if (lootItemFunction == null) {
            throw ERROR_UNKNOWN_ITEM_MODIFIER.create(resourceLocation);
        } else {
            return lootItemFunction;
        }
    }

    public static ResourceLocation getId(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, ResourceLocation.class);
    }

    public ResourceLocation parse(StringReader stringReader) throws CommandSyntaxException {
        return ResourceLocation.read(stringReader);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
