package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class ComponentArgument implements ArgumentType<Component> {
    private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "\"\"", "\"{\"text\":\"hello world\"}", "[\"\"]");
    public static final DynamicCommandExceptionType ERROR_INVALID_JSON = new DynamicCommandExceptionType((text) -> {
        return new TranslatableComponent("argument.component.invalid", text);
    });

    private ComponentArgument() {
    }

    public static Component getComponent(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, Component.class);
    }

    public static ComponentArgument textComponent() {
        return new ComponentArgument();
    }

    public Component parse(StringReader stringReader) throws CommandSyntaxException {
        try {
            Component component = Component.Serializer.fromJson(stringReader);
            if (component == null) {
                throw ERROR_INVALID_JSON.createWithContext(stringReader, "empty");
            } else {
                return component;
            }
        } catch (Exception var4) {
            String string = var4.getCause() != null ? var4.getCause().getMessage() : var4.getMessage();
            throw ERROR_INVALID_JSON.createWithContext(stringReader, string);
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
