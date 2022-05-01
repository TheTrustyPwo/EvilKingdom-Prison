package net.minecraft.network.chat;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class KeybindComponent extends BaseComponent {
    private static Function<String, Supplier<Component>> keyResolver = (key) -> {
        return () -> {
            return new TextComponent(key);
        };
    };
    private final String name;
    private Supplier<Component> nameResolver;

    public KeybindComponent(String key) {
        this.name = key;
    }

    public static void setKeyResolver(Function<String, Supplier<Component>> translator) {
        keyResolver = translator;
    }

    private Component getNestedComponent() {
        if (this.nameResolver == null) {
            this.nameResolver = keyResolver.apply(this.name);
        }

        return this.nameResolver.get();
    }

    @Override
    public <T> Optional<T> visitSelf(FormattedText.ContentConsumer<T> visitor) {
        return this.getNestedComponent().visit(visitor);
    }

    @Override
    public <T> Optional<T> visitSelf(FormattedText.StyledContentConsumer<T> visitor, Style style) {
        return this.getNestedComponent().visit(visitor, style);
    }

    @Override
    public KeybindComponent plainCopy() {
        return new KeybindComponent(this.name);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof KeybindComponent)) {
            return false;
        } else {
            KeybindComponent keybindComponent = (KeybindComponent)object;
            return this.name.equals(keybindComponent.name) && super.equals(object);
        }
    }

    @Override
    public String toString() {
        return "KeybindComponent{keybind='" + this.name + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
    }

    public String getName() {
        return this.name;
    }
}
