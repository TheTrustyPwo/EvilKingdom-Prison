package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;

public class MessageArgument implements ArgumentType<MessageArgument.Message> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");

    public static MessageArgument message() {
        return new MessageArgument();
    }

    public static Component getMessage(CommandContext<CommandSourceStack> command, String name) throws CommandSyntaxException {
        return command.getArgument(name, MessageArgument.Message.class).toComponent(command.getSource(), command.getSource().hasPermission(2));
    }

    public MessageArgument.Message parse(StringReader stringReader) throws CommandSyntaxException {
        return MessageArgument.Message.parseText(stringReader, true);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Message {
        private final String text;
        private final MessageArgument.Part[] parts;

        public Message(String contents, MessageArgument.Part[] selectors) {
            this.text = contents;
            this.parts = selectors;
        }

        public String getText() {
            return this.text;
        }

        public MessageArgument.Part[] getParts() {
            return this.parts;
        }

        public Component toComponent(CommandSourceStack source, boolean canUseSelectors) throws CommandSyntaxException {
            if (this.parts.length != 0 && canUseSelectors) {
                MutableComponent mutableComponent = new TextComponent(this.text.substring(0, this.parts[0].getStart()));
                int i = this.parts[0].getStart();

                for(MessageArgument.Part part : this.parts) {
                    Component component = part.toComponent(source);
                    if (i < part.getStart()) {
                        mutableComponent.append(this.text.substring(i, part.getStart()));
                    }

                    if (component != null) {
                        mutableComponent.append(component);
                    }

                    i = part.getEnd();
                }

                if (i < this.text.length()) {
                    mutableComponent.append(this.text.substring(i));
                }

                return mutableComponent;
            } else {
                return new TextComponent(this.text);
            }
        }

        public static MessageArgument.Message parseText(StringReader reader, boolean canUseSelectors) throws CommandSyntaxException {
            String string = reader.getString().substring(reader.getCursor(), reader.getTotalLength());
            if (!canUseSelectors) {
                reader.setCursor(reader.getTotalLength());
                return new MessageArgument.Message(string, new MessageArgument.Part[0]);
            } else {
                List<MessageArgument.Part> list = Lists.newArrayList();
                int i = reader.getCursor();

                while(true) {
                    int j;
                    EntitySelector entitySelector;
                    while(true) {
                        if (!reader.canRead()) {
                            return new MessageArgument.Message(string, list.toArray(new MessageArgument.Part[0]));
                        }

                        if (reader.peek() == '@') {
                            j = reader.getCursor();

                            try {
                                EntitySelectorParser entitySelectorParser = new EntitySelectorParser(reader);
                                entitySelector = entitySelectorParser.parse();
                                break;
                            } catch (CommandSyntaxException var8) {
                                if (var8.getType() != EntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE && var8.getType() != EntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
                                    throw var8;
                                }

                                reader.setCursor(j + 1);
                            }
                        } else {
                            reader.skip();
                        }
                    }

                    list.add(new MessageArgument.Part(j - i, reader.getCursor() - i, entitySelector));
                }
            }
        }
    }

    public static class Part {
        private final int start;
        private final int end;
        private final EntitySelector selector;

        public Part(int start, int end, EntitySelector selector) {
            this.start = start;
            this.end = end;
            this.selector = selector;
        }

        public int getStart() {
            return this.start;
        }

        public int getEnd() {
            return this.end;
        }

        public EntitySelector getSelector() {
            return this.selector;
        }

        @Nullable
        public Component toComponent(CommandSourceStack source) throws CommandSyntaxException {
            return EntitySelector.joinNames(this.selector.findEntities(source));
        }
    }
}
