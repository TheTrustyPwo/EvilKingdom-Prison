package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class EntitySummonArgument implements ArgumentType<ResourceLocation> {
    private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:pig", "cow");
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_ENTITY = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("entity.notFound", id);
    });

    public static EntitySummonArgument id() {
        return new EntitySummonArgument();
    }

    public static ResourceLocation getSummonableEntity(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return verifyCanSummon(context.getArgument(name, ResourceLocation.class));
    }

    private static ResourceLocation verifyCanSummon(ResourceLocation id) throws CommandSyntaxException {
        Registry.ENTITY_TYPE.getOptional(id).filter(EntityType::canSummon).orElseThrow(() -> {
            return ERROR_UNKNOWN_ENTITY.create(id);
        });
        return id;
    }

    public ResourceLocation parse(StringReader stringReader) throws CommandSyntaxException {
        return verifyCanSummon(ResourceLocation.read(stringReader));
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
