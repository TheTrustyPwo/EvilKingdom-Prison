package net.minecraft.world.level.chunk;

interface PaletteResize<T> {
    int onResize(int newBits, T object);
}
