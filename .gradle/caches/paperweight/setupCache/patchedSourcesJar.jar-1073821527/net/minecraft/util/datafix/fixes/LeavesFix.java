package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.util.datafix.PackedBitStorage;

public class LeavesFix extends DataFix {
    private static final int NORTH_WEST_MASK = 128;
    private static final int WEST_MASK = 64;
    private static final int SOUTH_WEST_MASK = 32;
    private static final int SOUTH_MASK = 16;
    private static final int SOUTH_EAST_MASK = 8;
    private static final int EAST_MASK = 4;
    private static final int NORTH_EAST_MASK = 2;
    private static final int NORTH_MASK = 1;
    private static final int[][] DIRECTIONS = new int[][]{{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};
    private static final int DECAY_DISTANCE = 7;
    private static final int SIZE_BITS = 12;
    private static final int SIZE = 4096;
    static final Object2IntMap<String> LEAVES = DataFixUtils.make(new Object2IntOpenHashMap<>(), (map) -> {
        map.put("minecraft:acacia_leaves", 0);
        map.put("minecraft:birch_leaves", 1);
        map.put("minecraft:dark_oak_leaves", 2);
        map.put("minecraft:jungle_leaves", 3);
        map.put("minecraft:oak_leaves", 4);
        map.put("minecraft:spruce_leaves", 5);
    });
    static final Set<String> LOGS = ImmutableSet.of("minecraft:acacia_bark", "minecraft:birch_bark", "minecraft:dark_oak_bark", "minecraft:jungle_bark", "minecraft:oak_bark", "minecraft:spruce_bark", "minecraft:acacia_log", "minecraft:birch_log", "minecraft:dark_oak_log", "minecraft:jungle_log", "minecraft:oak_log", "minecraft:spruce_log", "minecraft:stripped_acacia_log", "minecraft:stripped_birch_log", "minecraft:stripped_dark_oak_log", "minecraft:stripped_jungle_log", "minecraft:stripped_oak_log", "minecraft:stripped_spruce_log");

    public LeavesFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.CHUNK);
        OpticFinder<?> opticFinder = type.findField("Level");
        OpticFinder<?> opticFinder2 = opticFinder.type().findField("Sections");
        Type<?> type2 = opticFinder2.type();
        if (!(type2 instanceof ListType)) {
            throw new IllegalStateException("Expecting sections to be a list.");
        } else {
            Type<?> type3 = ((ListType)type2).getElement();
            OpticFinder<?> opticFinder3 = DSL.typeFinder(type3);
            return this.fixTypeEverywhereTyped("Leaves fix", type, (typed) -> {
                return typed.updateTyped(opticFinder, (typedx) -> {
                    int[] is = new int[]{0};
                    Typed<?> typed2 = typedx.updateTyped(opticFinder2, (typed) -> {
                        Int2ObjectMap<LeavesFix.LeavesSection> int2ObjectMap = new Int2ObjectOpenHashMap<>(typed.getAllTyped(opticFinder3).stream().map((typedx) -> {
                            return new LeavesFix.LeavesSection(typedx, this.getInputSchema());
                        }).collect(Collectors.toMap(LeavesFix.Section::getIndex, (leavesSection) -> {
                            return leavesSection;
                        })));
                        if (int2ObjectMap.values().stream().allMatch(LeavesFix.Section::isSkippable)) {
                            return typed;
                        } else {
                            List<IntSet> list = Lists.newArrayList();

                            for(int i = 0; i < 7; ++i) {
                                list.add(new IntOpenHashSet());
                            }

                            for(LeavesFix.LeavesSection leavesSection : int2ObjectMap.values()) {
                                if (!leavesSection.isSkippable()) {
                                    for(int j = 0; j < 4096; ++j) {
                                        int k = leavesSection.getBlock(j);
                                        if (leavesSection.isLog(k)) {
                                            list.get(0).add(leavesSection.getIndex() << 12 | j);
                                        } else if (leavesSection.isLeaf(k)) {
                                            int l = this.getX(j);
                                            int m = this.getZ(j);
                                            is[0] |= getSideMask(l == 0, l == 15, m == 0, m == 15);
                                        }
                                    }
                                }
                            }

                            for(int n = 1; n < 7; ++n) {
                                IntSet intSet = list.get(n - 1);
                                IntSet intSet2 = list.get(n);
                                IntIterator intIterator = intSet.iterator();

                                while(intIterator.hasNext()) {
                                    int o = intIterator.nextInt();
                                    int p = this.getX(o);
                                    int q = this.getY(o);
                                    int r = this.getZ(o);

                                    for(int[] js : DIRECTIONS) {
                                        int s = p + js[0];
                                        int t = q + js[1];
                                        int u = r + js[2];
                                        if (s >= 0 && s <= 15 && u >= 0 && u <= 15 && t >= 0 && t <= 255) {
                                            LeavesFix.LeavesSection leavesSection2 = int2ObjectMap.get(t >> 4);
                                            if (leavesSection2 != null && !leavesSection2.isSkippable()) {
                                                int v = getIndex(s, t & 15, u);
                                                int w = leavesSection2.getBlock(v);
                                                if (leavesSection2.isLeaf(w)) {
                                                    int x = leavesSection2.getDistance(w);
                                                    if (x > n) {
                                                        leavesSection2.setDistance(v, w, n);
                                                        intSet2.add(getIndex(s, t, u));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            return typed.updateTyped(opticFinder3, (typedx) -> {
                                return int2ObjectMap.get(typedx.get(DSL.remainderFinder()).get("Y").asInt(0)).write(typedx);
                            });
                        }
                    });
                    if (is[0] != 0) {
                        typed2 = typed2.update(DSL.remainderFinder(), (dynamic) -> {
                            Dynamic<?> dynamic2 = DataFixUtils.orElse(dynamic.get("UpgradeData").result(), dynamic.emptyMap());
                            return dynamic.set("UpgradeData", dynamic2.set("Sides", dynamic.createByte((byte)(dynamic2.get("Sides").asByte((byte)0) | is[0]))));
                        });
                    }

                    return typed2;
                });
            });
        }
    }

    public static int getIndex(int i, int j, int k) {
        return j << 8 | k << 4 | i;
    }

    private int getX(int i) {
        return i & 15;
    }

    private int getY(int i) {
        return i >> 8 & 255;
    }

    private int getZ(int i) {
        return i >> 4 & 15;
    }

    public static int getSideMask(boolean bl, boolean bl2, boolean bl3, boolean bl4) {
        int i = 0;
        if (bl3) {
            if (bl2) {
                i |= 2;
            } else if (bl) {
                i |= 128;
            } else {
                i |= 1;
            }
        } else if (bl4) {
            if (bl) {
                i |= 32;
            } else if (bl2) {
                i |= 8;
            } else {
                i |= 16;
            }
        } else if (bl2) {
            i |= 4;
        } else if (bl) {
            i |= 64;
        }

        return i;
    }

    public static final class LeavesSection extends LeavesFix.Section {
        private static final String PERSISTENT = "persistent";
        private static final String DECAYABLE = "decayable";
        private static final String DISTANCE = "distance";
        @Nullable
        private IntSet leaveIds;
        @Nullable
        private IntSet logIds;
        @Nullable
        private Int2IntMap stateToIdMap;

        public LeavesSection(Typed<?> typed, Schema schema) {
            super(typed, schema);
        }

        @Override
        protected boolean skippable() {
            this.leaveIds = new IntOpenHashSet();
            this.logIds = new IntOpenHashSet();
            this.stateToIdMap = new Int2IntOpenHashMap();

            for(int i = 0; i < this.palette.size(); ++i) {
                Dynamic<?> dynamic = this.palette.get(i);
                String string = dynamic.get("Name").asString("");
                if (LeavesFix.LEAVES.containsKey(string)) {
                    boolean bl = Objects.equals(dynamic.get("Properties").get("decayable").asString(""), "false");
                    this.leaveIds.add(i);
                    this.stateToIdMap.put(this.getStateId(string, bl, 7), i);
                    this.palette.set(i, this.makeLeafTag(dynamic, string, bl, 7));
                }

                if (LeavesFix.LOGS.contains(string)) {
                    this.logIds.add(i);
                }
            }

            return this.leaveIds.isEmpty() && this.logIds.isEmpty();
        }

        private Dynamic<?> makeLeafTag(Dynamic<?> dynamic, String string, boolean bl, int i) {
            Dynamic<?> dynamic2 = dynamic.emptyMap();
            dynamic2 = dynamic2.set("persistent", dynamic2.createString(bl ? "true" : "false"));
            dynamic2 = dynamic2.set("distance", dynamic2.createString(Integer.toString(i)));
            Dynamic<?> dynamic3 = dynamic.emptyMap();
            dynamic3 = dynamic3.set("Properties", dynamic2);
            return dynamic3.set("Name", dynamic3.createString(string));
        }

        public boolean isLog(int i) {
            return this.logIds.contains(i);
        }

        public boolean isLeaf(int i) {
            return this.leaveIds.contains(i);
        }

        int getDistance(int i) {
            return this.isLog(i) ? 0 : Integer.parseInt(this.palette.get(i).get("Properties").get("distance").asString(""));
        }

        void setDistance(int i, int j, int k) {
            Dynamic<?> dynamic = this.palette.get(j);
            String string = dynamic.get("Name").asString("");
            boolean bl = Objects.equals(dynamic.get("Properties").get("persistent").asString(""), "true");
            int l = this.getStateId(string, bl, k);
            if (!this.stateToIdMap.containsKey(l)) {
                int m = this.palette.size();
                this.leaveIds.add(m);
                this.stateToIdMap.put(l, m);
                this.palette.add(this.makeLeafTag(dynamic, string, bl, k));
            }

            int n = this.stateToIdMap.get(l);
            if (1 << this.storage.getBits() <= n) {
                PackedBitStorage packedBitStorage = new PackedBitStorage(this.storage.getBits() + 1, 4096);

                for(int o = 0; o < 4096; ++o) {
                    packedBitStorage.set(o, this.storage.get(o));
                }

                this.storage = packedBitStorage;
            }

            this.storage.set(i, n);
        }
    }

    public abstract static class Section {
        protected static final String BLOCK_STATES_TAG = "BlockStates";
        protected static final String NAME_TAG = "Name";
        protected static final String PROPERTIES_TAG = "Properties";
        private final Type<Pair<String, Dynamic<?>>> blockStateType = DSL.named(References.BLOCK_STATE.typeName(), DSL.remainderType());
        protected final OpticFinder<List<Pair<String, Dynamic<?>>>> paletteFinder = DSL.fieldFinder("Palette", DSL.list(this.blockStateType));
        protected final List<Dynamic<?>> palette;
        protected final int index;
        @Nullable
        protected PackedBitStorage storage;

        public Section(Typed<?> typed, Schema schema) {
            if (!Objects.equals(schema.getType(References.BLOCK_STATE), this.blockStateType)) {
                throw new IllegalStateException("Block state type is not what was expected.");
            } else {
                Optional<List<Pair<String, Dynamic<?>>>> optional = typed.getOptional(this.paletteFinder);
                this.palette = optional.map((list) -> {
                    return list.stream().map(Pair::getSecond).collect(Collectors.toList());
                }).orElse(ImmutableList.of());
                Dynamic<?> dynamic = typed.get(DSL.remainderFinder());
                this.index = dynamic.get("Y").asInt(0);
                this.readStorage(dynamic);
            }
        }

        protected void readStorage(Dynamic<?> dynamic) {
            if (this.skippable()) {
                this.storage = null;
            } else {
                long[] ls = dynamic.get("BlockStates").asLongStream().toArray();
                int i = Math.max(4, DataFixUtils.ceillog2(this.palette.size()));
                this.storage = new PackedBitStorage(i, 4096, ls);
            }

        }

        public Typed<?> write(Typed<?> typed) {
            return this.isSkippable() ? typed : typed.update(DSL.remainderFinder(), (dynamic) -> {
                return dynamic.set("BlockStates", dynamic.createLongList(Arrays.stream(this.storage.getRaw())));
            }).set(this.paletteFinder, this.palette.stream().map((dynamic) -> {
                return Pair.of(References.BLOCK_STATE.typeName(), dynamic);
            }).collect(Collectors.toList()));
        }

        public boolean isSkippable() {
            return this.storage == null;
        }

        public int getBlock(int i) {
            return this.storage.get(i);
        }

        protected int getStateId(String leafBlockName, boolean persistent, int i) {
            return LeavesFix.LEAVES.get(leafBlockName) << 5 | (persistent ? 16 : 0) | i;
        }

        int getIndex() {
            return this.index;
        }

        protected abstract boolean skippable();
    }
}
