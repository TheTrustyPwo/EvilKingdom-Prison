package net.minecraft.server.packs.repository;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public interface PackSource {
    PackSource DEFAULT = passThrough();
    PackSource BUILT_IN = decorating("pack.source.builtin");
    PackSource WORLD = decorating("pack.source.world");
    PackSource SERVER = decorating("pack.source.server");

    Component decorate(Component packName);

    static PackSource passThrough() {
        return (name) -> {
            return name;
        };
    }

    static PackSource decorating(String source) {
        Component component = new TranslatableComponent(source);
        return (name) -> {
            return (new TranslatableComponent("pack.nameAndSource", name, component)).withStyle(ChatFormatting.GRAY);
        };
    }
}
