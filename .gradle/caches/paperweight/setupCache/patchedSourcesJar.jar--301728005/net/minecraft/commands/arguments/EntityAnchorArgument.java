package net.minecraft.commands.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityAnchorArgument implements ArgumentType<EntityAnchorArgument.Anchor> {
    private static final Collection<String> EXAMPLES = Arrays.asList("eyes", "feet");
    private static final DynamicCommandExceptionType ERROR_INVALID = new DynamicCommandExceptionType((name) -> {
        return new TranslatableComponent("argument.anchor.invalid", name);
    });

    public static EntityAnchorArgument.Anchor getAnchor(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, EntityAnchorArgument.Anchor.class);
    }

    public static EntityAnchorArgument anchor() {
        return new EntityAnchorArgument();
    }

    public EntityAnchorArgument.Anchor parse(StringReader stringReader) throws CommandSyntaxException {
        int i = stringReader.getCursor();
        String string = stringReader.readUnquotedString();
        EntityAnchorArgument.Anchor anchor = EntityAnchorArgument.Anchor.getByName(string);
        if (anchor == null) {
            stringReader.setCursor(i);
            throw ERROR_INVALID.createWithContext(stringReader, string);
        } else {
            return anchor;
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return SharedSuggestionProvider.suggest(EntityAnchorArgument.Anchor.BY_NAME.keySet(), suggestionsBuilder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static enum Anchor {
        FEET("feet", (pos, entity) -> {
            return pos;
        }),
        EYES("eyes", (pos, entity) -> {
            return new Vec3(pos.x, pos.y + (double)entity.getEyeHeight(), pos.z);
        });

        static final Map<String, EntityAnchorArgument.Anchor> BY_NAME = Util.make(Maps.newHashMap(), (map) -> {
            for(EntityAnchorArgument.Anchor anchor : values()) {
                map.put(anchor.name, anchor);
            }

        });
        private final String name;
        private final BiFunction<Vec3, Entity, Vec3> transform;

        private Anchor(String id, BiFunction<Vec3, Entity, Vec3> offset) {
            this.name = id;
            this.transform = offset;
        }

        @Nullable
        public static EntityAnchorArgument.Anchor getByName(String id) {
            return BY_NAME.get(id);
        }

        public Vec3 apply(Entity entity) {
            return this.transform.apply(entity.position(), entity);
        }

        public Vec3 apply(CommandSourceStack source) {
            Entity entity = source.getEntity();
            return entity == null ? source.getPosition() : this.transform.apply(source.getPosition(), entity);
        }
    }
}
