package net.minecraft.server.packs.repository;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;

public enum PackCompatibility {
    TOO_OLD("old"),
    TOO_NEW("new"),
    COMPATIBLE("compatible");

    private final Component description;
    private final Component confirmation;

    private PackCompatibility(String translationSuffix) {
        this.description = (new TranslatableComponent("pack.incompatible." + translationSuffix)).withStyle(ChatFormatting.GRAY);
        this.confirmation = new TranslatableComponent("pack.incompatible.confirm." + translationSuffix);
    }

    public boolean isCompatible() {
        return this == COMPATIBLE;
    }

    public static PackCompatibility forFormat(int packVersion, PackType type) {
        int i = type.getVersion(SharedConstants.getCurrentVersion());
        if (packVersion < i) {
            return TOO_OLD;
        } else {
            return packVersion > i ? TOO_NEW : COMPATIBLE;
        }
    }

    public static PackCompatibility forMetadata(PackMetadataSection metadata, PackType type) {
        return forFormat(metadata.getPackFormat(), type);
    }

    public Component getDescription() {
        return this.description;
    }

    public Component getConfirmation() {
        return this.confirmation;
    }
}
