package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.util.datafix.PackedBitStorage;
import org.slf4j.Logger;

public class ChunkPalettedStorageFix extends DataFix {
    private static final int NORTH_WEST_MASK = 128;
    private static final int WEST_MASK = 64;
    private static final int SOUTH_WEST_MASK = 32;
    private static final int SOUTH_MASK = 16;
    private static final int SOUTH_EAST_MASK = 8;
    private static final int EAST_MASK = 4;
    private static final int NORTH_EAST_MASK = 2;
    private static final int NORTH_MASK = 1;
    static final Logger LOGGER = LogUtils.getLogger();
    static final BitSet VIRTUAL = new BitSet(256);
    static final BitSet FIX = new BitSet(256);
    static final Dynamic<?> PUMPKIN = BlockStateData.parse("{Name:'minecraft:pumpkin'}");
    static final Dynamic<?> SNOWY_PODZOL = BlockStateData.parse("{Name:'minecraft:podzol',Properties:{snowy:'true'}}");
    static final Dynamic<?> SNOWY_GRASS = BlockStateData.parse("{Name:'minecraft:grass_block',Properties:{snowy:'true'}}");
    static final Dynamic<?> SNOWY_MYCELIUM = BlockStateData.parse("{Name:'minecraft:mycelium',Properties:{snowy:'true'}}");
    static final Dynamic<?> UPPER_SUNFLOWER = BlockStateData.parse("{Name:'minecraft:sunflower',Properties:{half:'upper'}}");
    static final Dynamic<?> UPPER_LILAC = BlockStateData.parse("{Name:'minecraft:lilac',Properties:{half:'upper'}}");
    static final Dynamic<?> UPPER_TALL_GRASS = BlockStateData.parse("{Name:'minecraft:tall_grass',Properties:{half:'upper'}}");
    static final Dynamic<?> UPPER_LARGE_FERN = BlockStateData.parse("{Name:'minecraft:large_fern',Properties:{half:'upper'}}");
    static final Dynamic<?> UPPER_ROSE_BUSH = BlockStateData.parse("{Name:'minecraft:rose_bush',Properties:{half:'upper'}}");
    static final Dynamic<?> UPPER_PEONY = BlockStateData.parse("{Name:'minecraft:peony',Properties:{half:'upper'}}");
    static final Map<String, Dynamic<?>> FLOWER_POT_MAP = DataFixUtils.make(Maps.newHashMap(), (map) -> {
        map.put("minecraft:air0", BlockStateData.parse("{Name:'minecraft:flower_pot'}"));
        map.put("minecraft:red_flower0", BlockStateData.parse("{Name:'minecraft:potted_poppy'}"));
        map.put("minecraft:red_flower1", BlockStateData.parse("{Name:'minecraft:potted_blue_orchid'}"));
        map.put("minecraft:red_flower2", BlockStateData.parse("{Name:'minecraft:potted_allium'}"));
        map.put("minecraft:red_flower3", BlockStateData.parse("{Name:'minecraft:potted_azure_bluet'}"));
        map.put("minecraft:red_flower4", BlockStateData.parse("{Name:'minecraft:potted_red_tulip'}"));
        map.put("minecraft:red_flower5", BlockStateData.parse("{Name:'minecraft:potted_orange_tulip'}"));
        map.put("minecraft:red_flower6", BlockStateData.parse("{Name:'minecraft:potted_white_tulip'}"));
        map.put("minecraft:red_flower7", BlockStateData.parse("{Name:'minecraft:potted_pink_tulip'}"));
        map.put("minecraft:red_flower8", BlockStateData.parse("{Name:'minecraft:potted_oxeye_daisy'}"));
        map.put("minecraft:yellow_flower0", BlockStateData.parse("{Name:'minecraft:potted_dandelion'}"));
        map.put("minecraft:sapling0", BlockStateData.parse("{Name:'minecraft:potted_oak_sapling'}"));
        map.put("minecraft:sapling1", BlockStateData.parse("{Name:'minecraft:potted_spruce_sapling'}"));
        map.put("minecraft:sapling2", BlockStateData.parse("{Name:'minecraft:potted_birch_sapling'}"));
        map.put("minecraft:sapling3", BlockStateData.parse("{Name:'minecraft:potted_jungle_sapling'}"));
        map.put("minecraft:sapling4", BlockStateData.parse("{Name:'minecraft:potted_acacia_sapling'}"));
        map.put("minecraft:sapling5", BlockStateData.parse("{Name:'minecraft:potted_dark_oak_sapling'}"));
        map.put("minecraft:red_mushroom0", BlockStateData.parse("{Name:'minecraft:potted_red_mushroom'}"));
        map.put("minecraft:brown_mushroom0", BlockStateData.parse("{Name:'minecraft:potted_brown_mushroom'}"));
        map.put("minecraft:deadbush0", BlockStateData.parse("{Name:'minecraft:potted_dead_bush'}"));
        map.put("minecraft:tallgrass2", BlockStateData.parse("{Name:'minecraft:potted_fern'}"));
        map.put("minecraft:cactus0", BlockStateData.getTag(2240));
    });
    static final Map<String, Dynamic<?>> SKULL_MAP = DataFixUtils.make(Maps.newHashMap(), (map) -> {
        mapSkull(map, 0, "skeleton", "skull");
        mapSkull(map, 1, "wither_skeleton", "skull");
        mapSkull(map, 2, "zombie", "head");
        mapSkull(map, 3, "player", "head");
        mapSkull(map, 4, "creeper", "head");
        mapSkull(map, 5, "dragon", "head");
    });
    static final Map<String, Dynamic<?>> DOOR_MAP = DataFixUtils.make(Maps.newHashMap(), (map) -> {
        mapDoor(map, "oak_door", 1024);
        mapDoor(map, "iron_door", 1136);
        mapDoor(map, "spruce_door", 3088);
        mapDoor(map, "birch_door", 3104);
        mapDoor(map, "jungle_door", 3120);
        mapDoor(map, "acacia_door", 3136);
        mapDoor(map, "dark_oak_door", 3152);
    });
    static final Map<String, Dynamic<?>> NOTE_BLOCK_MAP = DataFixUtils.make(Maps.newHashMap(), (map) -> {
        for(int i = 0; i < 26; ++i) {
            map.put("true" + i, BlockStateData.parse("{Name:'minecraft:note_block',Properties:{powered:'true',note:'" + i + "'}}"));
            map.put("false" + i, BlockStateData.parse("{Name:'minecraft:note_block',Properties:{powered:'false',note:'" + i + "'}}"));
        }

    });
    private static final Int2ObjectMap<String> DYE_COLOR_MAP = DataFixUtils.make(new Int2ObjectOpenHashMap<>(), (map) -> {
        map.put(0, "white");
        map.put(1, "orange");
        map.put(2, "magenta");
        map.put(3, "light_blue");
        map.put(4, "yellow");
        map.put(5, "lime");
        map.put(6, "pink");
        map.put(7, "gray");
        map.put(8, "light_gray");
        map.put(9, "cyan");
        map.put(10, "purple");
        map.put(11, "blue");
        map.put(12, "brown");
        map.put(13, "green");
        map.put(14, "red");
        map.put(15, "black");
    });
    static final Map<String, Dynamic<?>> BED_BLOCK_MAP = DataFixUtils.make(Maps.newHashMap(), (map) -> {
        for(Entry<String> entry : DYE_COLOR_MAP.int2ObjectEntrySet()) {
            if (!Objects.equals(entry.getValue(), "red")) {
                addBeds(map, entry.getIntKey(), entry.getValue());
            }
        }

    });
    static final Map<String, Dynamic<?>> BANNER_BLOCK_MAP = DataFixUtils.make(Maps.newHashMap(), (map) -> {
        for(Entry<String> entry : DYE_COLOR_MAP.int2ObjectEntrySet()) {
            if (!Objects.equals(entry.getValue(), "white")) {
                addBanners(map, 15 - entry.getIntKey(), entry.getValue());
            }
        }

    });
    static final Dynamic<?> AIR;
    private static final int SIZE = 4096;

    public ChunkPalettedStorageFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    private static void mapSkull(Map<String, Dynamic<?>> out, int i, String mob, String block) {
        out.put(i + "north", BlockStateData.parse("{Name:'minecraft:" + mob + "_wall_" + block + "',Properties:{facing:'north'}}"));
        out.put(i + "east", BlockStateData.parse("{Name:'minecraft:" + mob + "_wall_" + block + "',Properties:{facing:'east'}}"));
        out.put(i + "south", BlockStateData.parse("{Name:'minecraft:" + mob + "_wall_" + block + "',Properties:{facing:'south'}}"));
        out.put(i + "west", BlockStateData.parse("{Name:'minecraft:" + mob + "_wall_" + block + "',Properties:{facing:'west'}}"));

        for(int j = 0; j < 16; ++j) {
            out.put("" + i + j, BlockStateData.parse("{Name:'minecraft:" + mob + "_" + block + "',Properties:{rotation:'" + j + "'}}"));
        }

    }

    private static void mapDoor(Map<String, Dynamic<?>> out, String name, int i) {
        out.put("minecraft:" + name + "eastlowerleftfalsefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'false'}}"));
        out.put("minecraft:" + name + "eastlowerleftfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "eastlowerlefttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "eastlowerlefttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "eastlowerrightfalsefalse", BlockStateData.getTag(i));
        out.put("minecraft:" + name + "eastlowerrightfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "eastlowerrighttruefalse", BlockStateData.getTag(i + 4));
        out.put("minecraft:" + name + "eastlowerrighttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "eastupperleftfalsefalse", BlockStateData.getTag(i + 8));
        out.put("minecraft:" + name + "eastupperleftfalsetrue", BlockStateData.getTag(i + 10));
        out.put("minecraft:" + name + "eastupperlefttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "eastupperlefttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "eastupperrightfalsefalse", BlockStateData.getTag(i + 9));
        out.put("minecraft:" + name + "eastupperrightfalsetrue", BlockStateData.getTag(i + 11));
        out.put("minecraft:" + name + "eastupperrighttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "eastupperrighttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "northlowerleftfalsefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'false'}}"));
        out.put("minecraft:" + name + "northlowerleftfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "northlowerlefttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "northlowerlefttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "northlowerrightfalsefalse", BlockStateData.getTag(i + 3));
        out.put("minecraft:" + name + "northlowerrightfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "northlowerrighttruefalse", BlockStateData.getTag(i + 7));
        out.put("minecraft:" + name + "northlowerrighttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "northupperleftfalsefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'false'}}"));
        out.put("minecraft:" + name + "northupperleftfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "northupperlefttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "northupperlefttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "northupperrightfalsefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'false'}}"));
        out.put("minecraft:" + name + "northupperrightfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "northupperrighttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "northupperrighttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "southlowerleftfalsefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'false'}}"));
        out.put("minecraft:" + name + "southlowerleftfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "southlowerlefttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "southlowerlefttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "southlowerrightfalsefalse", BlockStateData.getTag(i + 1));
        out.put("minecraft:" + name + "southlowerrightfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "southlowerrighttruefalse", BlockStateData.getTag(i + 5));
        out.put("minecraft:" + name + "southlowerrighttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "southupperleftfalsefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'false'}}"));
        out.put("minecraft:" + name + "southupperleftfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "southupperlefttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "southupperlefttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "southupperrightfalsefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'false'}}"));
        out.put("minecraft:" + name + "southupperrightfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "southupperrighttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "southupperrighttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "westlowerleftfalsefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'false'}}"));
        out.put("minecraft:" + name + "westlowerleftfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "westlowerlefttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "westlowerlefttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "westlowerrightfalsefalse", BlockStateData.getTag(i + 2));
        out.put("minecraft:" + name + "westlowerrightfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "westlowerrighttruefalse", BlockStateData.getTag(i + 6));
        out.put("minecraft:" + name + "westlowerrighttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "westupperleftfalsefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'false'}}"));
        out.put("minecraft:" + name + "westupperleftfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "westupperlefttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "westupperlefttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'true'}}"));
        out.put("minecraft:" + name + "westupperrightfalsefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'false'}}"));
        out.put("minecraft:" + name + "westupperrightfalsetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'true'}}"));
        out.put("minecraft:" + name + "westupperrighttruefalse", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'false'}}"));
        out.put("minecraft:" + name + "westupperrighttruetrue", BlockStateData.parse("{Name:'minecraft:" + name + "',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'true'}}"));
    }

    private static void addBeds(Map<String, Dynamic<?>> out, int i, String color) {
        out.put("southfalsefoot" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'south',occupied:'false',part:'foot'}}"));
        out.put("westfalsefoot" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'west',occupied:'false',part:'foot'}}"));
        out.put("northfalsefoot" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'north',occupied:'false',part:'foot'}}"));
        out.put("eastfalsefoot" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'east',occupied:'false',part:'foot'}}"));
        out.put("southfalsehead" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'south',occupied:'false',part:'head'}}"));
        out.put("westfalsehead" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'west',occupied:'false',part:'head'}}"));
        out.put("northfalsehead" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'north',occupied:'false',part:'head'}}"));
        out.put("eastfalsehead" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'east',occupied:'false',part:'head'}}"));
        out.put("southtruehead" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'south',occupied:'true',part:'head'}}"));
        out.put("westtruehead" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'west',occupied:'true',part:'head'}}"));
        out.put("northtruehead" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'north',occupied:'true',part:'head'}}"));
        out.put("easttruehead" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_bed',Properties:{facing:'east',occupied:'true',part:'head'}}"));
    }

    private static void addBanners(Map<String, Dynamic<?>> out, int i, String color) {
        for(int j = 0; j < 16; ++j) {
            out.put(j + "_" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_banner',Properties:{rotation:'" + j + "'}}"));
        }

        out.put("north_" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_wall_banner',Properties:{facing:'north'}}"));
        out.put("south_" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_wall_banner',Properties:{facing:'south'}}"));
        out.put("west_" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_wall_banner',Properties:{facing:'west'}}"));
        out.put("east_" + i, BlockStateData.parse("{Name:'minecraft:" + color + "_wall_banner',Properties:{facing:'east'}}"));
    }

    public static String getName(Dynamic<?> dynamic) {
        return dynamic.get("Name").asString("");
    }

    public static String getProperty(Dynamic<?> dynamic, String string) {
        return dynamic.get("Properties").get(string).asString("");
    }

    public static int idFor(CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> crudeIncrementalIntIdentityHashBiMap, Dynamic<?> dynamic) {
        int i = crudeIncrementalIntIdentityHashBiMap.getId(dynamic);
        if (i == -1) {
            i = crudeIncrementalIntIdentityHashBiMap.add(dynamic);
        }

        return i;
    }

    private Dynamic<?> fix(Dynamic<?> dynamic) {
        Optional<? extends Dynamic<?>> optional = dynamic.get("Level").result();
        return optional.isPresent() && optional.get().get("Sections").asStreamOpt().result().isPresent() ? dynamic.set("Level", (new ChunkPalettedStorageFix.UpgradeChunk(optional.get())).write()) : dynamic;
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.CHUNK);
        Type<?> type2 = this.getOutputSchema().getType(References.CHUNK);
        return this.writeFixAndRead("ChunkPalettedStorageFix", type, type2, this::fix);
    }

    public static int getSideMask(boolean west, boolean east, boolean north, boolean south) {
        int i = 0;
        if (north) {
            if (east) {
                i |= 2;
            } else if (west) {
                i |= 128;
            } else {
                i |= 1;
            }
        } else if (south) {
            if (west) {
                i |= 32;
            } else if (east) {
                i |= 8;
            } else {
                i |= 16;
            }
        } else if (east) {
            i |= 4;
        } else if (west) {
            i |= 64;
        }

        return i;
    }

    static {
        FIX.set(2);
        FIX.set(3);
        FIX.set(110);
        FIX.set(140);
        FIX.set(144);
        FIX.set(25);
        FIX.set(86);
        FIX.set(26);
        FIX.set(176);
        FIX.set(177);
        FIX.set(175);
        FIX.set(64);
        FIX.set(71);
        FIX.set(193);
        FIX.set(194);
        FIX.set(195);
        FIX.set(196);
        FIX.set(197);
        VIRTUAL.set(54);
        VIRTUAL.set(146);
        VIRTUAL.set(25);
        VIRTUAL.set(26);
        VIRTUAL.set(51);
        VIRTUAL.set(53);
        VIRTUAL.set(67);
        VIRTUAL.set(108);
        VIRTUAL.set(109);
        VIRTUAL.set(114);
        VIRTUAL.set(128);
        VIRTUAL.set(134);
        VIRTUAL.set(135);
        VIRTUAL.set(136);
        VIRTUAL.set(156);
        VIRTUAL.set(163);
        VIRTUAL.set(164);
        VIRTUAL.set(180);
        VIRTUAL.set(203);
        VIRTUAL.set(55);
        VIRTUAL.set(85);
        VIRTUAL.set(113);
        VIRTUAL.set(188);
        VIRTUAL.set(189);
        VIRTUAL.set(190);
        VIRTUAL.set(191);
        VIRTUAL.set(192);
        VIRTUAL.set(93);
        VIRTUAL.set(94);
        VIRTUAL.set(101);
        VIRTUAL.set(102);
        VIRTUAL.set(160);
        VIRTUAL.set(106);
        VIRTUAL.set(107);
        VIRTUAL.set(183);
        VIRTUAL.set(184);
        VIRTUAL.set(185);
        VIRTUAL.set(186);
        VIRTUAL.set(187);
        VIRTUAL.set(132);
        VIRTUAL.set(139);
        VIRTUAL.set(199);
        AIR = BlockStateData.getTag(0);
    }

    static class DataLayer {
        private static final int SIZE = 2048;
        private static final int NIBBLE_SIZE = 4;
        private final byte[] data;

        public DataLayer() {
            this.data = new byte[2048];
        }

        public DataLayer(byte[] bs) {
            this.data = bs;
            if (bs.length != 2048) {
                throw new IllegalArgumentException("ChunkNibbleArrays should be 2048 bytes not: " + bs.length);
            }
        }

        public int get(int x, int y, int i) {
            int j = this.getPosition(y << 8 | i << 4 | x);
            return this.isFirst(y << 8 | i << 4 | x) ? this.data[j] & 15 : this.data[j] >> 4 & 15;
        }

        private boolean isFirst(int index) {
            return (index & 1) == 0;
        }

        private int getPosition(int index) {
            return index >> 1;
        }
    }

    public static enum Direction {
        DOWN(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.Y),
        UP(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.Y),
        NORTH(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.Z),
        SOUTH(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.Z),
        WEST(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.X),
        EAST(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.X);

        private final ChunkPalettedStorageFix.Direction.Axis axis;
        private final ChunkPalettedStorageFix.Direction.AxisDirection axisDirection;

        private Direction(ChunkPalettedStorageFix.Direction.AxisDirection direction, ChunkPalettedStorageFix.Direction.Axis axis) {
            this.axis = axis;
            this.axisDirection = direction;
        }

        public ChunkPalettedStorageFix.Direction.AxisDirection getAxisDirection() {
            return this.axisDirection;
        }

        public ChunkPalettedStorageFix.Direction.Axis getAxis() {
            return this.axis;
        }

        public static enum Axis {
            X,
            Y,
            Z;
        }

        public static enum AxisDirection {
            POSITIVE(1),
            NEGATIVE(-1);

            private final int step;

            private AxisDirection(int j) {
                this.step = j;
            }

            public int getStep() {
                return this.step;
            }
        }
    }

    static class Section {
        private final CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> palette = CrudeIncrementalIntIdentityHashBiMap.create(32);
        private final List<Dynamic<?>> listTag;
        private final Dynamic<?> section;
        private final boolean hasData;
        final Int2ObjectMap<IntList> toFix = new Int2ObjectLinkedOpenHashMap<>();
        final IntList update = new IntArrayList();
        public final int y;
        private final Set<Dynamic<?>> seen = Sets.newIdentityHashSet();
        private final int[] buffer = new int[4096];

        public Section(Dynamic<?> dynamic) {
            this.listTag = Lists.newArrayList();
            this.section = dynamic;
            this.y = dynamic.get("Y").asInt(0);
            this.hasData = dynamic.get("Blocks").result().isPresent();
        }

        public Dynamic<?> getBlock(int index) {
            if (index >= 0 && index <= 4095) {
                Dynamic<?> dynamic = this.palette.byId(this.buffer[index]);
                return dynamic == null ? ChunkPalettedStorageFix.AIR : dynamic;
            } else {
                return ChunkPalettedStorageFix.AIR;
            }
        }

        public void setBlock(int pos, Dynamic<?> dynamic) {
            if (this.seen.add(dynamic)) {
                this.listTag.add("%%FILTER_ME%%".equals(ChunkPalettedStorageFix.getName(dynamic)) ? ChunkPalettedStorageFix.AIR : dynamic);
            }

            this.buffer[pos] = ChunkPalettedStorageFix.idFor(this.palette, dynamic);
        }

        public int upgrade(int sidesToUpgrade) {
            if (!this.hasData) {
                return sidesToUpgrade;
            } else {
                ByteBuffer byteBuffer = this.section.get("Blocks").asByteBufferOpt().result().get();
                ChunkPalettedStorageFix.DataLayer dataLayer = this.section.get("Data").asByteBufferOpt().map((byteBufferx) -> {
                    return new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(byteBufferx));
                }).result().orElseGet(ChunkPalettedStorageFix.DataLayer::new);
                ChunkPalettedStorageFix.DataLayer dataLayer2 = this.section.get("Add").asByteBufferOpt().map((byteBufferx) -> {
                    return new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(byteBufferx));
                }).result().orElseGet(ChunkPalettedStorageFix.DataLayer::new);
                this.seen.add(ChunkPalettedStorageFix.AIR);
                ChunkPalettedStorageFix.idFor(this.palette, ChunkPalettedStorageFix.AIR);
                this.listTag.add(ChunkPalettedStorageFix.AIR);

                for(int i = 0; i < 4096; ++i) {
                    int j = i & 15;
                    int k = i >> 8 & 15;
                    int l = i >> 4 & 15;
                    int m = dataLayer2.get(j, k, l) << 12 | (byteBuffer.get(i) & 255) << 4 | dataLayer.get(j, k, l);
                    if (ChunkPalettedStorageFix.FIX.get(m >> 4)) {
                        this.addFix(m >> 4, i);
                    }

                    if (ChunkPalettedStorageFix.VIRTUAL.get(m >> 4)) {
                        int n = ChunkPalettedStorageFix.getSideMask(j == 0, j == 15, l == 0, l == 15);
                        if (n == 0) {
                            this.update.add(i);
                        } else {
                            sidesToUpgrade |= n;
                        }
                    }

                    this.setBlock(i, BlockStateData.getTag(m));
                }

                return sidesToUpgrade;
            }
        }

        private void addFix(int section, int index) {
            IntList intList = this.toFix.get(section);
            if (intList == null) {
                intList = new IntArrayList();
                this.toFix.put(section, intList);
            }

            intList.add(index);
        }

        public Dynamic<?> write() {
            Dynamic<?> dynamic = this.section;
            if (!this.hasData) {
                return dynamic;
            } else {
                dynamic = dynamic.set("Palette", dynamic.createList(this.listTag.stream()));
                int i = Math.max(4, DataFixUtils.ceillog2(this.seen.size()));
                PackedBitStorage packedBitStorage = new PackedBitStorage(i, 4096);

                for(int j = 0; j < this.buffer.length; ++j) {
                    packedBitStorage.set(j, this.buffer[j]);
                }

                dynamic = dynamic.set("BlockStates", dynamic.createLongList(Arrays.stream(packedBitStorage.getRaw())));
                dynamic = dynamic.remove("Blocks");
                dynamic = dynamic.remove("Data");
                return dynamic.remove("Add");
            }
        }
    }

    static final class UpgradeChunk {
        private int sides;
        private final ChunkPalettedStorageFix.Section[] sections = new ChunkPalettedStorageFix.Section[16];
        private final Dynamic<?> level;
        private final int x;
        private final int z;
        private final Int2ObjectMap<Dynamic<?>> blockEntities = new Int2ObjectLinkedOpenHashMap<>(16);

        public UpgradeChunk(Dynamic<?> dynamic) {
            this.level = dynamic;
            this.x = dynamic.get("xPos").asInt(0) << 4;
            this.z = dynamic.get("zPos").asInt(0) << 4;
            dynamic.get("TileEntities").asStreamOpt().result().ifPresent((stream) -> {
                stream.forEach((dynamic) -> {
                    int i = dynamic.get("x").asInt(0) - this.x & 15;
                    int j = dynamic.get("y").asInt(0);
                    int k = dynamic.get("z").asInt(0) - this.z & 15;
                    int l = j << 8 | k << 4 | i;
                    if (this.blockEntities.put(l, dynamic) != null) {
                        ChunkPalettedStorageFix.LOGGER.warn("In chunk: {}x{} found a duplicate block entity at position: [{}, {}, {}]", this.x, this.z, i, j, k);
                    }

                });
            });
            boolean bl = dynamic.get("convertedFromAlphaFormat").asBoolean(false);
            dynamic.get("Sections").asStreamOpt().result().ifPresent((stream) -> {
                stream.forEach((dynamic) -> {
                    ChunkPalettedStorageFix.Section section = new ChunkPalettedStorageFix.Section(dynamic);
                    this.sides = section.upgrade(this.sides);
                    this.sections[section.y] = section;
                });
            });

            for(ChunkPalettedStorageFix.Section section : this.sections) {
                if (section != null) {
                    for(java.util.Map.Entry<Integer, IntList> entry : section.toFix.entrySet()) {
                        int i = section.y << 12;
                        switch(entry.getKey()) {
                        case 2:
                            for(int j : entry.getValue()) {
                                j |= i;
                                Dynamic<?> dynamic2 = this.getBlock(j);
                                if ("minecraft:grass_block".equals(ChunkPalettedStorageFix.getName(dynamic2))) {
                                    String string = ChunkPalettedStorageFix.getName(this.getBlock(relative(j, ChunkPalettedStorageFix.Direction.UP)));
                                    if ("minecraft:snow".equals(string) || "minecraft:snow_layer".equals(string)) {
                                        this.setBlock(j, ChunkPalettedStorageFix.SNOWY_GRASS);
                                    }
                                }
                            }
                            break;
                        case 3:
                            for(int k : entry.getValue()) {
                                k |= i;
                                Dynamic<?> dynamic3 = this.getBlock(k);
                                if ("minecraft:podzol".equals(ChunkPalettedStorageFix.getName(dynamic3))) {
                                    String string2 = ChunkPalettedStorageFix.getName(this.getBlock(relative(k, ChunkPalettedStorageFix.Direction.UP)));
                                    if ("minecraft:snow".equals(string2) || "minecraft:snow_layer".equals(string2)) {
                                        this.setBlock(k, ChunkPalettedStorageFix.SNOWY_PODZOL);
                                    }
                                }
                            }
                            break;
                        case 25:
                            for(int m : entry.getValue()) {
                                m |= i;
                                Dynamic<?> dynamic5 = this.removeBlockEntity(m);
                                if (dynamic5 != null) {
                                    String string4 = Boolean.toString(dynamic5.get("powered").asBoolean(false)) + (byte)Math.min(Math.max(dynamic5.get("note").asInt(0), 0), 24);
                                    this.setBlock(m, ChunkPalettedStorageFix.NOTE_BLOCK_MAP.getOrDefault(string4, ChunkPalettedStorageFix.NOTE_BLOCK_MAP.get("false0")));
                                }
                            }
                            break;
                        case 26:
                            for(int n : entry.getValue()) {
                                n |= i;
                                Dynamic<?> dynamic6 = this.getBlockEntity(n);
                                Dynamic<?> dynamic7 = this.getBlock(n);
                                if (dynamic6 != null) {
                                    int o = dynamic6.get("color").asInt(0);
                                    if (o != 14 && o >= 0 && o < 16) {
                                        String string5 = ChunkPalettedStorageFix.getProperty(dynamic7, "facing") + ChunkPalettedStorageFix.getProperty(dynamic7, "occupied") + ChunkPalettedStorageFix.getProperty(dynamic7, "part") + o;
                                        if (ChunkPalettedStorageFix.BED_BLOCK_MAP.containsKey(string5)) {
                                            this.setBlock(n, ChunkPalettedStorageFix.BED_BLOCK_MAP.get(string5));
                                        }
                                    }
                                }
                            }
                            break;
                        case 64:
                        case 71:
                        case 193:
                        case 194:
                        case 195:
                        case 196:
                        case 197:
                            for(int u : entry.getValue()) {
                                u |= i;
                                Dynamic<?> dynamic13 = this.getBlock(u);
                                if (ChunkPalettedStorageFix.getName(dynamic13).endsWith("_door")) {
                                    Dynamic<?> dynamic14 = this.getBlock(u);
                                    if ("lower".equals(ChunkPalettedStorageFix.getProperty(dynamic14, "half"))) {
                                        int v = relative(u, ChunkPalettedStorageFix.Direction.UP);
                                        Dynamic<?> dynamic15 = this.getBlock(v);
                                        String string13 = ChunkPalettedStorageFix.getName(dynamic14);
                                        if (string13.equals(ChunkPalettedStorageFix.getName(dynamic15))) {
                                            String string14 = ChunkPalettedStorageFix.getProperty(dynamic14, "facing");
                                            String string15 = ChunkPalettedStorageFix.getProperty(dynamic14, "open");
                                            String string16 = bl ? "left" : ChunkPalettedStorageFix.getProperty(dynamic15, "hinge");
                                            String string17 = bl ? "false" : ChunkPalettedStorageFix.getProperty(dynamic15, "powered");
                                            this.setBlock(u, ChunkPalettedStorageFix.DOOR_MAP.get(string13 + string14 + "lower" + string16 + string15 + string17));
                                            this.setBlock(v, ChunkPalettedStorageFix.DOOR_MAP.get(string13 + string14 + "upper" + string16 + string15 + string17));
                                        }
                                    }
                                }
                            }
                            break;
                        case 86:
                            for(int r : entry.getValue()) {
                                r |= i;
                                Dynamic<?> dynamic10 = this.getBlock(r);
                                if ("minecraft:carved_pumpkin".equals(ChunkPalettedStorageFix.getName(dynamic10))) {
                                    String string7 = ChunkPalettedStorageFix.getName(this.getBlock(relative(r, ChunkPalettedStorageFix.Direction.DOWN)));
                                    if ("minecraft:grass_block".equals(string7) || "minecraft:dirt".equals(string7)) {
                                        this.setBlock(r, ChunkPalettedStorageFix.PUMPKIN);
                                    }
                                }
                            }
                            break;
                        case 110:
                            for(int l : entry.getValue()) {
                                l |= i;
                                Dynamic<?> dynamic4 = this.getBlock(l);
                                if ("minecraft:mycelium".equals(ChunkPalettedStorageFix.getName(dynamic4))) {
                                    String string3 = ChunkPalettedStorageFix.getName(this.getBlock(relative(l, ChunkPalettedStorageFix.Direction.UP)));
                                    if ("minecraft:snow".equals(string3) || "minecraft:snow_layer".equals(string3)) {
                                        this.setBlock(l, ChunkPalettedStorageFix.SNOWY_MYCELIUM);
                                    }
                                }
                            }
                            break;
                        case 140:
                            for(int s : entry.getValue()) {
                                s |= i;
                                Dynamic<?> dynamic11 = this.removeBlockEntity(s);
                                if (dynamic11 != null) {
                                    String string8 = dynamic11.get("Item").asString("") + dynamic11.get("Data").asInt(0);
                                    this.setBlock(s, ChunkPalettedStorageFix.FLOWER_POT_MAP.getOrDefault(string8, ChunkPalettedStorageFix.FLOWER_POT_MAP.get("minecraft:air0")));
                                }
                            }
                            break;
                        case 144:
                            for(int t : entry.getValue()) {
                                t |= i;
                                Dynamic<?> dynamic12 = this.getBlockEntity(t);
                                if (dynamic12 != null) {
                                    String string9 = String.valueOf(dynamic12.get("SkullType").asInt(0));
                                    String string10 = ChunkPalettedStorageFix.getProperty(this.getBlock(t), "facing");
                                    String string12;
                                    if (!"up".equals(string10) && !"down".equals(string10)) {
                                        string12 = string9 + string10;
                                    } else {
                                        string12 = string9 + String.valueOf(dynamic12.get("Rot").asInt(0));
                                    }

                                    dynamic12.remove("SkullType");
                                    dynamic12.remove("facing");
                                    dynamic12.remove("Rot");
                                    this.setBlock(t, ChunkPalettedStorageFix.SKULL_MAP.getOrDefault(string12, ChunkPalettedStorageFix.SKULL_MAP.get("0north")));
                                }
                            }
                            break;
                        case 175:
                            for(int w : entry.getValue()) {
                                w |= i;
                                Dynamic<?> dynamic16 = this.getBlock(w);
                                if ("upper".equals(ChunkPalettedStorageFix.getProperty(dynamic16, "half"))) {
                                    Dynamic<?> dynamic17 = this.getBlock(relative(w, ChunkPalettedStorageFix.Direction.DOWN));
                                    String string18 = ChunkPalettedStorageFix.getName(dynamic17);
                                    if ("minecraft:sunflower".equals(string18)) {
                                        this.setBlock(w, ChunkPalettedStorageFix.UPPER_SUNFLOWER);
                                    } else if ("minecraft:lilac".equals(string18)) {
                                        this.setBlock(w, ChunkPalettedStorageFix.UPPER_LILAC);
                                    } else if ("minecraft:tall_grass".equals(string18)) {
                                        this.setBlock(w, ChunkPalettedStorageFix.UPPER_TALL_GRASS);
                                    } else if ("minecraft:large_fern".equals(string18)) {
                                        this.setBlock(w, ChunkPalettedStorageFix.UPPER_LARGE_FERN);
                                    } else if ("minecraft:rose_bush".equals(string18)) {
                                        this.setBlock(w, ChunkPalettedStorageFix.UPPER_ROSE_BUSH);
                                    } else if ("minecraft:peony".equals(string18)) {
                                        this.setBlock(w, ChunkPalettedStorageFix.UPPER_PEONY);
                                    }
                                }
                            }
                            break;
                        case 176:
                        case 177:
                            for(int p : entry.getValue()) {
                                p |= i;
                                Dynamic<?> dynamic8 = this.getBlockEntity(p);
                                Dynamic<?> dynamic9 = this.getBlock(p);
                                if (dynamic8 != null) {
                                    int q = dynamic8.get("Base").asInt(0);
                                    if (q != 15 && q >= 0 && q < 16) {
                                        String string6 = ChunkPalettedStorageFix.getProperty(dynamic9, entry.getKey() == 176 ? "rotation" : "facing") + "_" + q;
                                        if (ChunkPalettedStorageFix.BANNER_BLOCK_MAP.containsKey(string6)) {
                                            this.setBlock(p, ChunkPalettedStorageFix.BANNER_BLOCK_MAP.get(string6));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

        @Nullable
        private Dynamic<?> getBlockEntity(int i) {
            return this.blockEntities.get(i);
        }

        @Nullable
        private Dynamic<?> removeBlockEntity(int i) {
            return this.blockEntities.remove(i);
        }

        public static int relative(int i, ChunkPalettedStorageFix.Direction direction) {
            switch(direction.getAxis()) {
            case X:
                int j = (i & 15) + direction.getAxisDirection().getStep();
                return j >= 0 && j <= 15 ? i & -16 | j : -1;
            case Y:
                int k = (i >> 8) + direction.getAxisDirection().getStep();
                return k >= 0 && k <= 255 ? i & 255 | k << 8 : -1;
            case Z:
                int l = (i >> 4 & 15) + direction.getAxisDirection().getStep();
                return l >= 0 && l <= 15 ? i & -241 | l << 4 : -1;
            default:
                return -1;
            }
        }

        private void setBlock(int i, Dynamic<?> dynamic) {
            if (i >= 0 && i <= 65535) {
                ChunkPalettedStorageFix.Section section = this.getSection(i);
                if (section != null) {
                    section.setBlock(i & 4095, dynamic);
                }
            }
        }

        @Nullable
        private ChunkPalettedStorageFix.Section getSection(int i) {
            int j = i >> 12;
            return j < this.sections.length ? this.sections[j] : null;
        }

        public Dynamic<?> getBlock(int i) {
            if (i >= 0 && i <= 65535) {
                ChunkPalettedStorageFix.Section section = this.getSection(i);
                return section == null ? ChunkPalettedStorageFix.AIR : section.getBlock(i & 4095);
            } else {
                return ChunkPalettedStorageFix.AIR;
            }
        }

        public Dynamic<?> write() {
            Dynamic<?> dynamic = this.level;
            if (this.blockEntities.isEmpty()) {
                dynamic = dynamic.remove("TileEntities");
            } else {
                dynamic = dynamic.set("TileEntities", dynamic.createList(this.blockEntities.values().stream()));
            }

            Dynamic<?> dynamic2 = dynamic.emptyMap();
            List<Dynamic<?>> list = Lists.newArrayList();

            for(ChunkPalettedStorageFix.Section section : this.sections) {
                if (section != null) {
                    list.add(section.write());
                    dynamic2 = dynamic2.set(String.valueOf(section.y), dynamic2.createIntList(Arrays.stream(section.update.toIntArray())));
                }
            }

            Dynamic<?> dynamic3 = dynamic.emptyMap();
            dynamic3 = dynamic3.set("Sides", dynamic3.createByte((byte)this.sides));
            dynamic3 = dynamic3.set("Indices", dynamic2);
            return dynamic.set("UpgradeData", dynamic3).set("Sections", dynamic3.createList(list.stream()));
        }
    }
}
