package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class ResourceKeyArgument<T> implements ArgumentType<ResourceKey<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ATTRIBUTE = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("attribute.unknown", id);
    });
    private static final DynamicCommandExceptionType ERROR_INVALID_FEATURE = new DynamicCommandExceptionType((id) -> {
        return new TranslatableComponent("commands.placefeature.invalid", id);
    });
    final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceKeyArgument(ResourceKey<? extends Registry<T>> registryRef) {
        this.registryKey = registryRef;
    }

    public static <T> ResourceKeyArgument<T> key(ResourceKey<? extends Registry<T>> registryRef) {
        return new ResourceKeyArgument<>(registryRef);
    }

    private static <T> ResourceKey<T> getRegistryType(CommandContext<CommandSourceStack> context, String name, ResourceKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException) throws CommandSyntaxException {
        ResourceKey<?> resourceKey = context.getArgument(name, ResourceKey.class);
        Optional<ResourceKey<T>> optional = resourceKey.cast(registryRef);
        return optional.orElseThrow(() -> {
            return invalidException.create(resourceKey);
        });
    }

    private static <T> Registry<T> getRegistry(CommandContext<CommandSourceStack> context, ResourceKey<? extends Registry<T>> registryRef) {
        return context.getSource().getServer().registryAccess().registryOrThrow(registryRef);
    }

    public static Attribute getAttribute(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ResourceKey<Attribute> resourceKey = getRegistryType(context, name, Registry.ATTRIBUTE_REGISTRY, ERROR_UNKNOWN_ATTRIBUTE);
        return getRegistry(context, Registry.ATTRIBUTE_REGISTRY).getOptional(resourceKey).orElseThrow(() -> {
            return ERROR_UNKNOWN_ATTRIBUTE.create(resourceKey.location());
        });
    }

    public static Holder<ConfiguredFeature<?, ?>> getConfiguredFeature(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ResourceKey<ConfiguredFeature<?, ?>> resourceKey = getRegistryType(context, name, Registry.CONFIGURED_FEATURE_REGISTRY, ERROR_INVALID_FEATURE);
        return getRegistry(context, Registry.CONFIGURED_FEATURE_REGISTRY).getHolder(resourceKey).orElseThrow(() -> {
            return ERROR_INVALID_FEATURE.create(resourceKey.location());
        });
    }

    public ResourceKey<T> parse(StringReader stringReader) throws CommandSyntaxException {
        ResourceLocation resourceLocation = ResourceLocation.read(stringReader);
        return ResourceKey.create(this.registryKey, resourceLocation);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        Object var4 = commandContext.getSource();
        if (var4 instanceof SharedSuggestionProvider) {
            SharedSuggestionProvider sharedSuggestionProvider = (SharedSuggestionProvider)var4;
            return sharedSuggestionProvider.suggestRegistryElements(this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS, suggestionsBuilder, commandContext);
        } else {
            return suggestionsBuilder.buildFuture();
        }
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Serializer implements ArgumentSerializer<ResourceKeyArgument<?>> {
        @Override
        public void serializeToNetwork(ResourceKeyArgument<?> type, FriendlyByteBuf buf) {
            buf.writeResourceLocation(type.registryKey.location());
        }

        @Override
        public ResourceKeyArgument<?> deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
            ResourceLocation resourceLocation = friendlyByteBuf.readResourceLocation();
            return new ResourceKeyArgument(ResourceKey.createRegistryKey(resourceLocation));
        }

        @Override
        public void serializeToJson(ResourceKeyArgument<?> type, JsonObject json) {
            json.addProperty("registry", type.registryKey.location().toString());
        }
    }
}
