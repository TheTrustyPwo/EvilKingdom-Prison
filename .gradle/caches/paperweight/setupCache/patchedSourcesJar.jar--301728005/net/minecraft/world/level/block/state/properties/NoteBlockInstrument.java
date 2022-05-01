package net.minecraft.world.level.block.state.properties;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public enum NoteBlockInstrument implements StringRepresentable {
    HARP("harp", SoundEvents.NOTE_BLOCK_HARP),
    BASEDRUM("basedrum", SoundEvents.NOTE_BLOCK_BASEDRUM),
    SNARE("snare", SoundEvents.NOTE_BLOCK_SNARE),
    HAT("hat", SoundEvents.NOTE_BLOCK_HAT),
    BASS("bass", SoundEvents.NOTE_BLOCK_BASS),
    FLUTE("flute", SoundEvents.NOTE_BLOCK_FLUTE),
    BELL("bell", SoundEvents.NOTE_BLOCK_BELL),
    GUITAR("guitar", SoundEvents.NOTE_BLOCK_GUITAR),
    CHIME("chime", SoundEvents.NOTE_BLOCK_CHIME),
    XYLOPHONE("xylophone", SoundEvents.NOTE_BLOCK_XYLOPHONE),
    IRON_XYLOPHONE("iron_xylophone", SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE),
    COW_BELL("cow_bell", SoundEvents.NOTE_BLOCK_COW_BELL),
    DIDGERIDOO("didgeridoo", SoundEvents.NOTE_BLOCK_DIDGERIDOO),
    BIT("bit", SoundEvents.NOTE_BLOCK_BIT),
    BANJO("banjo", SoundEvents.NOTE_BLOCK_BANJO),
    PLING("pling", SoundEvents.NOTE_BLOCK_PLING);

    private final String name;
    private final SoundEvent soundEvent;

    private NoteBlockInstrument(String name, SoundEvent sound) {
        this.name = name;
        this.soundEvent = sound;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public SoundEvent getSoundEvent() {
        return this.soundEvent;
    }

    public static NoteBlockInstrument byState(BlockState state) {
        if (state.is(Blocks.CLAY)) {
            return FLUTE;
        } else if (state.is(Blocks.GOLD_BLOCK)) {
            return BELL;
        } else if (state.is(BlockTags.WOOL)) {
            return GUITAR;
        } else if (state.is(Blocks.PACKED_ICE)) {
            return CHIME;
        } else if (state.is(Blocks.BONE_BLOCK)) {
            return XYLOPHONE;
        } else if (state.is(Blocks.IRON_BLOCK)) {
            return IRON_XYLOPHONE;
        } else if (state.is(Blocks.SOUL_SAND)) {
            return COW_BELL;
        } else if (state.is(Blocks.PUMPKIN)) {
            return DIDGERIDOO;
        } else if (state.is(Blocks.EMERALD_BLOCK)) {
            return BIT;
        } else if (state.is(Blocks.HAY_BLOCK)) {
            return BANJO;
        } else if (state.is(Blocks.GLOWSTONE)) {
            return PLING;
        } else {
            Material material = state.getMaterial();
            if (material == Material.STONE) {
                return BASEDRUM;
            } else if (material == Material.SAND) {
                return SNARE;
            } else if (material == Material.GLASS) {
                return HAT;
            } else {
                return material != Material.WOOD && material != Material.NETHER_WOOD ? HARP : BASS;
            }
        }
    }
}
