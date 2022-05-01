package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class ItemParticleOption implements ParticleOptions {
    public static final ParticleOptions.Deserializer<ItemParticleOption> DESERIALIZER = new ParticleOptions.Deserializer<ItemParticleOption>() {
        @Override
        public ItemParticleOption fromCommand(ParticleType<ItemParticleOption> particleType, StringReader stringReader) throws CommandSyntaxException {
            stringReader.expect(' ');
            ItemParser itemParser = (new ItemParser(stringReader, false)).parse();
            ItemStack itemStack = (new ItemInput(itemParser.getItem(), itemParser.getNbt())).createItemStack(1, false);
            return new ItemParticleOption(particleType, itemStack);
        }

        @Override
        public ItemParticleOption fromNetwork(ParticleType<ItemParticleOption> particleType, FriendlyByteBuf friendlyByteBuf) {
            return new ItemParticleOption(particleType, friendlyByteBuf.readItem());
        }
    };
    private final ParticleType<ItemParticleOption> type;
    private final ItemStack itemStack;

    public static Codec<ItemParticleOption> codec(ParticleType<ItemParticleOption> type) {
        return ItemStack.CODEC.xmap((stack) -> {
            return new ItemParticleOption(type, stack);
        }, (effect) -> {
            return effect.itemStack;
        });
    }

    public ItemParticleOption(ParticleType<ItemParticleOption> type, ItemStack stack) {
        this.type = type;
        this.itemStack = stack;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeItem(this.itemStack);
    }

    @Override
    public String writeToString() {
        return Registry.PARTICLE_TYPE.getKey(this.getType()) + " " + (new ItemInput(this.itemStack.getItem(), this.itemStack.getTag())).serialize();
    }

    @Override
    public ParticleType<ItemParticleOption> getType() {
        return this.type;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }
}
