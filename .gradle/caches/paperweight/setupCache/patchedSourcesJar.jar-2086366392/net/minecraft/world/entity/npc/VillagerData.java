package net.minecraft.world.entity.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;

public class VillagerData {
    public static final int MIN_VILLAGER_LEVEL = 1;
    public static final int MAX_VILLAGER_LEVEL = 5;
    private static final int[] NEXT_LEVEL_XP_THRESHOLDS = new int[]{0, 10, 70, 150, 250};
    public static final Codec<VillagerData> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Registry.VILLAGER_TYPE.byNameCodec().fieldOf("type").orElseGet(() -> {
            return VillagerType.PLAINS;
        }).forGetter((villagerData) -> {
            return villagerData.type;
        }), Registry.VILLAGER_PROFESSION.byNameCodec().fieldOf("profession").orElseGet(() -> {
            return VillagerProfession.NONE;
        }).forGetter((villagerData) -> {
            return villagerData.profession;
        }), Codec.INT.fieldOf("level").orElse(1).forGetter((villagerData) -> {
            return villagerData.level;
        })).apply(instance, VillagerData::new);
    });
    private final VillagerType type;
    private final VillagerProfession profession;
    private final int level;

    public VillagerData(VillagerType type, VillagerProfession profession, int level) {
        this.type = type;
        this.profession = profession;
        this.level = Math.max(1, level);
    }

    public VillagerType getType() {
        return this.type;
    }

    public VillagerProfession getProfession() {
        return this.profession;
    }

    public int getLevel() {
        return this.level;
    }

    public VillagerData setType(VillagerType type) {
        return new VillagerData(type, this.profession, this.level);
    }

    public VillagerData setProfession(VillagerProfession profession) {
        return new VillagerData(this.type, profession, this.level);
    }

    public VillagerData setLevel(int level) {
        return new VillagerData(this.type, this.profession, level);
    }

    public static int getMinXpPerLevel(int level) {
        return canLevelUp(level) ? NEXT_LEVEL_XP_THRESHOLDS[level - 1] : 0;
    }

    public static int getMaxXpPerLevel(int level) {
        return canLevelUp(level) ? NEXT_LEVEL_XP_THRESHOLDS[level] : 0;
    }

    public static boolean canLevelUp(int level) {
        return level >= 1 && level < 5;
    }
}
