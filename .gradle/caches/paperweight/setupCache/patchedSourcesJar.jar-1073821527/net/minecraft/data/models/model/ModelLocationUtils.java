package net.minecraft.data.models.model;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelLocationUtils {
    /** @deprecated */
    @Deprecated
    public static ResourceLocation decorateBlockModelLocation(String name) {
        return new ResourceLocation("minecraft", "block/" + name);
    }

    public static ResourceLocation decorateItemModelLocation(String name) {
        return new ResourceLocation("minecraft", "item/" + name);
    }

    public static ResourceLocation getModelLocation(Block block, String suffix) {
        ResourceLocation resourceLocation = Registry.BLOCK.getKey(block);
        return new ResourceLocation(resourceLocation.getNamespace(), "block/" + resourceLocation.getPath() + suffix);
    }

    public static ResourceLocation getModelLocation(Block block) {
        ResourceLocation resourceLocation = Registry.BLOCK.getKey(block);
        return new ResourceLocation(resourceLocation.getNamespace(), "block/" + resourceLocation.getPath());
    }

    public static ResourceLocation getModelLocation(Item item) {
        ResourceLocation resourceLocation = Registry.ITEM.getKey(item);
        return new ResourceLocation(resourceLocation.getNamespace(), "item/" + resourceLocation.getPath());
    }

    public static ResourceLocation getModelLocation(Item item, String suffix) {
        ResourceLocation resourceLocation = Registry.ITEM.getKey(item);
        return new ResourceLocation(resourceLocation.getNamespace(), "item/" + resourceLocation.getPath() + suffix);
    }
}
