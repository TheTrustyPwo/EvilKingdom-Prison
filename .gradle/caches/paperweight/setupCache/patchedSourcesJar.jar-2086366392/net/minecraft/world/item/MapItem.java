package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
// CraftBukkit start
import org.bukkit.Bukkit;
import org.bukkit.event.server.MapInitializeEvent;
// CraftBukkit end

public class MapItem extends ComplexItem {

    public static final int IMAGE_WIDTH = 128;
    public static final int IMAGE_HEIGHT = 128;
    private static final int DEFAULT_MAP_COLOR = -12173266;
    private static final String TAG_MAP = "map";

    public MapItem(Item.Properties settings) {
        super(settings);
    }

    public static ItemStack create(Level world, int x, int z, byte scale, boolean showIcons, boolean unlimitedTracking) {
        ItemStack itemstack = new ItemStack(Items.FILLED_MAP);

        MapItem.createAndStoreSavedData(itemstack, world, x, z, scale, showIcons, unlimitedTracking, world.dimension());
        return itemstack;
    }

    @Nullable
    public static MapItemSavedData getSavedData(@Nullable Integer id, Level world) {
        return id == null ? null : world.getMapData(MapItem.makeKey(id));
    }

    @Nullable
    public static MapItemSavedData getSavedData(ItemStack map, Level world) {
        Integer integer = MapItem.getMapId(map);

        return MapItem.getSavedData(integer, world);
    }

    @Nullable
    public static Integer getMapId(ItemStack stack) {
        CompoundTag nbttagcompound = stack.getTag();

        return nbttagcompound != null && nbttagcompound.contains("map", 99) ? nbttagcompound.getInt("map") : null; // CraftBukkit - make new maps for no tag // Paper - don't return invalid ID
    }

    public static int createNewSavedData(Level world, int x, int z, int scale, boolean showIcons, boolean unlimitedTracking, ResourceKey<Level> dimension) {
        MapItemSavedData worldmap = MapItemSavedData.createFresh((double) x, (double) z, (byte) scale, showIcons, unlimitedTracking, dimension);
        int l = world.getFreeMapId();

        world.setMapData(MapItem.makeKey(l), worldmap);
        // CraftBukkit start
        MapInitializeEvent event = new MapInitializeEvent(worldmap.mapView);
        Bukkit.getServer().getPluginManager().callEvent(event);
        // CraftBukkit end
        return l;
    }

    private static void storeMapData(ItemStack stack, int id) {
        stack.getOrCreateTag().putInt("map", id);
    }

    private static void createAndStoreSavedData(ItemStack stack, Level world, int x, int z, int scale, boolean showIcons, boolean unlimitedTracking, ResourceKey<Level> dimension) {
        int l = MapItem.createNewSavedData(world, x, z, scale, showIcons, unlimitedTracking, dimension);

        MapItem.storeMapData(stack, l);
    }

    public static String makeKey(int mapId) {
        return "map_" + mapId;
    }

    public void update(Level world, Entity entity, MapItemSavedData state) {
        if (world.dimension() == state.dimension && entity instanceof Player) {
            int i = 1 << state.scale;
            int j = state.x;
            int k = state.z;
            int l = Mth.floor(entity.getX() - (double) j) / i + 64;
            int i1 = Mth.floor(entity.getZ() - (double) k) / i + 64;
            int j1 = 128 / i;

            if (world.dimensionType().hasCeiling()) {
                j1 /= 2;
            }

            MapItemSavedData.HoldingPlayer worldmap_worldmaphumantracker = state.getHoldingPlayer((Player) entity);

            ++worldmap_worldmaphumantracker.step;
            boolean flag = false;

            for (int k1 = l - j1 + 1; k1 < l + j1; ++k1) {
                if ((k1 & 15) == (worldmap_worldmaphumantracker.step & 15) || flag) {
                    flag = false;
                    double d0 = 0.0D;

                    for (int l1 = i1 - j1 - 1; l1 < i1 + j1; ++l1) {
                        if (k1 >= 0 && l1 >= -1 && k1 < 128 && l1 < 128) {
                            int i2 = k1 - l;
                            int j2 = l1 - i1;
                            boolean flag1 = i2 * i2 + j2 * j2 > (j1 - 2) * (j1 - 2);
                            int k2 = (j / i + k1 - 64) * i;
                            int l2 = (k / i + l1 - 64) * i;
                            Multiset<MaterialColor> multiset = LinkedHashMultiset.create();
                            LevelChunk chunk = world.getChunkIfLoaded(new BlockPos(k2, 0, l2)); // Paper - Maps shouldn't load chunks

                            if (chunk != null && !chunk.isEmpty()) { // Paper - Maps shouldn't load chunks
                                ChunkPos chunkcoordintpair = chunk.getPos();
                                int i3 = k2 & 15;
                                int j3 = l2 & 15;
                                int k3 = 0;
                                double d1 = 0.0D;

                                if (world.dimensionType().hasCeiling()) {
                                    int l3 = k2 + l2 * 231871;

                                    l3 = l3 * l3 * 31287121 + l3 * 11;
                                    if ((l3 >> 20 & 1) == 0) {
                                        multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(world, BlockPos.ZERO), 10);
                                    } else {
                                        multiset.add(Blocks.STONE.defaultBlockState().getMapColor(world, BlockPos.ZERO), 100);
                                    }

                                    d1 = 100.0D;
                                } else {
                                    BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
                                    BlockPos.MutableBlockPos blockposition_mutableblockposition1 = new BlockPos.MutableBlockPos();

                                    for (int i4 = 0; i4 < i; ++i4) {
                                        for (int j4 = 0; j4 < i; ++j4) {
                                            int k4 = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, i4 + i3, j4 + j3) + 1;
                                            BlockState iblockdata;

                                            if (k4 > world.getMinBuildHeight() + 1) {
                                                do {
                                                    --k4;
                                                    blockposition_mutableblockposition.set(chunkcoordintpair.getMinBlockX() + i4 + i3, k4, chunkcoordintpair.getMinBlockZ() + j4 + j3);
                                                    iblockdata = chunk.getBlockState(blockposition_mutableblockposition);
                                                } while (iblockdata.getMapColor(world, blockposition_mutableblockposition) == MaterialColor.NONE && k4 > world.getMinBuildHeight());

                                                if (k4 > world.getMinBuildHeight() && !iblockdata.getFluidState().isEmpty()) {
                                                    int l4 = k4 - 1;

                                                    blockposition_mutableblockposition1.set(blockposition_mutableblockposition);

                                                    BlockState iblockdata1;

                                                    do {
                                                        blockposition_mutableblockposition1.setY(l4--);
                                                        iblockdata1 = chunk.getBlockState(blockposition_mutableblockposition1);
                                                        ++k3;
                                                    } while (l4 > world.getMinBuildHeight() && !iblockdata1.getFluidState().isEmpty());

                                                    iblockdata = this.getCorrectStateForFluidBlock(world, iblockdata, blockposition_mutableblockposition);
                                                }
                                            } else {
                                                iblockdata = Blocks.BEDROCK.defaultBlockState();
                                            }

                                            state.checkBanners(world, chunkcoordintpair.getMinBlockX() + i4 + i3, chunkcoordintpair.getMinBlockZ() + j4 + j3);
                                            d1 += (double) k4 / (double) (i * i);
                                            multiset.add(iblockdata.getMapColor(world, blockposition_mutableblockposition));
                                        }
                                    }
                                }

                                k3 /= i * i;
                                MaterialColor materialmapcolor = (MaterialColor) Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MaterialColor.NONE);
                                double d2;
                                MaterialColor.Brightness materialmapcolor_a;

                                if (materialmapcolor == MaterialColor.WATER) {
                                    d2 = (double) k3 * 0.1D + (double) (k1 + l1 & 1) * 0.2D;
                                    if (d2 < 0.5D) {
                                        materialmapcolor_a = MaterialColor.Brightness.HIGH;
                                    } else if (d2 > 0.9D) {
                                        materialmapcolor_a = MaterialColor.Brightness.LOW;
                                    } else {
                                        materialmapcolor_a = MaterialColor.Brightness.NORMAL;
                                    }
                                } else {
                                    d2 = (d1 - d0) * 4.0D / (double) (i + 4) + ((double) (k1 + l1 & 1) - 0.5D) * 0.4D;
                                    if (d2 > 0.6D) {
                                        materialmapcolor_a = MaterialColor.Brightness.HIGH;
                                    } else if (d2 < -0.6D) {
                                        materialmapcolor_a = MaterialColor.Brightness.LOW;
                                    } else {
                                        materialmapcolor_a = MaterialColor.Brightness.NORMAL;
                                    }
                                }

                                d0 = d1;
                                if (l1 >= 0 && i2 * i2 + j2 * j2 < j1 * j1 && (!flag1 || (k1 + l1 & 1) != 0)) {
                                    flag |= state.updateColor(k1, l1, materialmapcolor.getPackedId(materialmapcolor_a));
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private BlockState getCorrectStateForFluidBlock(Level world, BlockState state, BlockPos pos) {
        FluidState fluid = state.getFluidState();

        return !fluid.isEmpty() && !state.isFaceSturdy(world, pos, Direction.UP) ? fluid.createLegacyBlock() : state;
    }

    private static boolean isBiomeWatery(boolean[] biomes, int x, int z) {
        return biomes[z * 128 + x];
    }

    public static void renderBiomePreviewMap(ServerLevel world, ItemStack map) {
        MapItemSavedData worldmap = MapItem.getSavedData(map, world);

        if (worldmap != null) {
            if (world.dimension() == worldmap.dimension) {
                int i = 1 << worldmap.scale;
                int j = worldmap.x;
                int k = worldmap.z;
                boolean[] aboolean = new boolean[16384];
                int l = j / i - 64;
                int i1 = k / i - 64;

                int j1;
                int k1;

                for (j1 = 0; j1 < 128; ++j1) {
                    for (k1 = 0; k1 < 128; ++k1) {
                        Biome.BiomeCategory biomebase_geography = Biome.getBiomeCategory(world.getUncachedNoiseBiome((l + k1) * i, 0, (i1 + j1) * i)); // Paper

                        aboolean[j1 * 128 + k1] = biomebase_geography == Biome.BiomeCategory.OCEAN || biomebase_geography == Biome.BiomeCategory.RIVER || biomebase_geography == Biome.BiomeCategory.SWAMP;
                    }
                }

                for (j1 = 1; j1 < 127; ++j1) {
                    for (k1 = 1; k1 < 127; ++k1) {
                        int l1 = 0;

                        for (int i2 = -1; i2 < 2; ++i2) {
                            for (int j2 = -1; j2 < 2; ++j2) {
                                if ((i2 != 0 || j2 != 0) && MapItem.isBiomeWatery(aboolean, j1 + i2, k1 + j2)) {
                                    ++l1;
                                }
                            }
                        }

                        MaterialColor.Brightness materialmapcolor_a = MaterialColor.Brightness.LOWEST;
                        MaterialColor materialmapcolor = MaterialColor.NONE;

                        if (MapItem.isBiomeWatery(aboolean, j1, k1)) {
                            materialmapcolor = MaterialColor.COLOR_ORANGE;
                            if (l1 > 7 && k1 % 2 == 0) {
                                switch ((j1 + (int) (Mth.sin((float) k1 + 0.0F) * 7.0F)) / 8 % 5) {
                                    case 0:
                                    case 4:
                                        materialmapcolor_a = MaterialColor.Brightness.LOW;
                                        break;
                                    case 1:
                                    case 3:
                                        materialmapcolor_a = MaterialColor.Brightness.NORMAL;
                                        break;
                                    case 2:
                                        materialmapcolor_a = MaterialColor.Brightness.HIGH;
                                }
                            } else if (l1 > 7) {
                                materialmapcolor = MaterialColor.NONE;
                            } else if (l1 > 5) {
                                materialmapcolor_a = MaterialColor.Brightness.NORMAL;
                            } else if (l1 > 3) {
                                materialmapcolor_a = MaterialColor.Brightness.LOW;
                            } else if (l1 > 1) {
                                materialmapcolor_a = MaterialColor.Brightness.LOW;
                            }
                        } else if (l1 > 0) {
                            materialmapcolor = MaterialColor.COLOR_BROWN;
                            if (l1 > 3) {
                                materialmapcolor_a = MaterialColor.Brightness.NORMAL;
                            } else {
                                materialmapcolor_a = MaterialColor.Brightness.LOWEST;
                            }
                        }

                        if (materialmapcolor != MaterialColor.NONE) {
                            worldmap.setColor(j1, k1, materialmapcolor.getPackedId(materialmapcolor_a));
                        }
                    }
                }

            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (!world.isClientSide) {
            MapItemSavedData worldmap = MapItem.getSavedData(stack, world);

            if (worldmap != null) {
                if (entity instanceof Player) {
                    Player entityhuman = (Player) entity;

                    worldmap.tickCarriedBy(entityhuman, stack);
                }

                if (!worldmap.locked && (selected || entity instanceof Player && ((Player) entity).getOffhandItem() == stack)) {
                    this.update(world, entity, worldmap);
                }

            }
        }
    }

    @Nullable
    @Override
    public Packet<?> getUpdatePacket(ItemStack stack, Level world, Player player) {
        Integer integer = MapItem.getMapId(stack);
        MapItemSavedData worldmap = MapItem.getSavedData(integer, world);

        return worldmap != null ? worldmap.getUpdatePacket(integer, player) : null;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
        CompoundTag nbttagcompound = stack.getTag();

        if (nbttagcompound != null && nbttagcompound.contains("map_scale_direction", 99)) {
            MapItem.scaleMap(stack, world, nbttagcompound.getInt("map_scale_direction"));
            nbttagcompound.remove("map_scale_direction");
        } else if (nbttagcompound != null && nbttagcompound.contains("map_to_lock", 1) && nbttagcompound.getBoolean("map_to_lock")) {
            MapItem.lockMap(world, stack);
            nbttagcompound.remove("map_to_lock");
        }

    }

    private static void scaleMap(ItemStack map, Level world, int amount) {
        MapItemSavedData worldmap = MapItem.getSavedData(map, world);

        if (worldmap != null) {
            int j = world.getFreeMapId();

            world.setMapData(MapItem.makeKey(j), worldmap.scaled(amount));
            MapItem.storeMapData(map, j);
        }

    }

    public static void lockMap(Level world, ItemStack stack) {
        MapItemSavedData worldmap = MapItem.getSavedData(stack, world);

        if (worldmap != null) {
            int i = world.getFreeMapId();
            String s = MapItem.makeKey(i);
            MapItemSavedData worldmap1 = worldmap.locked();

            world.setMapData(s, worldmap1);
            MapItem.storeMapData(stack, i);
        }

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        Integer integer = MapItem.getMapId(stack);
        MapItemSavedData worldmap = world == null ? null : MapItem.getSavedData(integer, world);

        if (worldmap != null && worldmap.locked) {
            tooltip.add((new TranslatableComponent("filled_map.locked", new Object[]{integer})).withStyle(ChatFormatting.GRAY));
        }

        if (context.isAdvanced()) {
            if (worldmap != null) {
                tooltip.add((new TranslatableComponent("filled_map.id", new Object[]{integer})).withStyle(ChatFormatting.GRAY));
                tooltip.add((new TranslatableComponent("filled_map.scale", new Object[]{1 << worldmap.scale})).withStyle(ChatFormatting.GRAY));
                tooltip.add((new TranslatableComponent("filled_map.level", new Object[]{worldmap.scale, 4})).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add((new TranslatableComponent("filled_map.unknown")).withStyle(ChatFormatting.GRAY));
            }
        }

    }

    public static int getColor(ItemStack stack) {
        CompoundTag nbttagcompound = stack.getTagElement("display");

        if (nbttagcompound != null && nbttagcompound.contains("MapColor", 99)) {
            int i = nbttagcompound.getInt("MapColor");

            return -16777216 | i & 16777215;
        } else {
            return -12173266;
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState iblockdata = context.getLevel().getBlockState(context.getClickedPos());

        if (iblockdata.is(BlockTags.BANNERS)) {
            if (!context.getLevel().isClientSide) {
                MapItemSavedData worldmap = MapItem.getSavedData(context.getItemInHand(), context.getLevel());

                if (worldmap != null && !worldmap.toggleBanner(context.getLevel(), context.getClickedPos())) {
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        } else {
            return super.useOn(context);
        }
    }
}
