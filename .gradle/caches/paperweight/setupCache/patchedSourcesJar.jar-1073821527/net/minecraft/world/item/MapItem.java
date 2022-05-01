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

public class MapItem extends ComplexItem {
    public static final int IMAGE_WIDTH = 128;
    public static final int IMAGE_HEIGHT = 128;
    private static final int DEFAULT_MAP_COLOR = -12173266;
    private static final String TAG_MAP = "map";

    public MapItem(Item.Properties settings) {
        super(settings);
    }

    public static ItemStack create(Level world, int x, int z, byte scale, boolean showIcons, boolean unlimitedTracking) {
        ItemStack itemStack = new ItemStack(Items.FILLED_MAP);
        createAndStoreSavedData(itemStack, world, x, z, scale, showIcons, unlimitedTracking, world.dimension());
        return itemStack;
    }

    @Nullable
    public static MapItemSavedData getSavedData(@Nullable Integer id, Level world) {
        return id == null ? null : world.getMapData(makeKey(id));
    }

    @Nullable
    public static MapItemSavedData getSavedData(ItemStack map, Level world) {
        Integer integer = getMapId(map);
        return getSavedData(integer, world);
    }

    @Nullable
    public static Integer getMapId(ItemStack stack) {
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.contains("map", 99) ? compoundTag.getInt("map") : null;
    }

    public static int createNewSavedData(Level world, int x, int z, int scale, boolean showIcons, boolean unlimitedTracking, ResourceKey<Level> dimension) {
        MapItemSavedData mapItemSavedData = MapItemSavedData.createFresh((double)x, (double)z, (byte)scale, showIcons, unlimitedTracking, dimension);
        int i = world.getFreeMapId();
        world.setMapData(makeKey(i), mapItemSavedData);
        return i;
    }

    private static void storeMapData(ItemStack stack, int id) {
        stack.getOrCreateTag().putInt("map", id);
    }

    private static void createAndStoreSavedData(ItemStack stack, Level world, int x, int z, int scale, boolean showIcons, boolean unlimitedTracking, ResourceKey<Level> dimension) {
        int i = createNewSavedData(world, x, z, scale, showIcons, unlimitedTracking, dimension);
        storeMapData(stack, i);
    }

    public static String makeKey(int mapId) {
        return "map_" + mapId;
    }

    public void update(Level world, Entity entity, MapItemSavedData state) {
        if (world.dimension() == state.dimension && entity instanceof Player) {
            int i = 1 << state.scale;
            int j = state.x;
            int k = state.z;
            int l = Mth.floor(entity.getX() - (double)j) / i + 64;
            int m = Mth.floor(entity.getZ() - (double)k) / i + 64;
            int n = 128 / i;
            if (world.dimensionType().hasCeiling()) {
                n /= 2;
            }

            MapItemSavedData.HoldingPlayer holdingPlayer = state.getHoldingPlayer((Player)entity);
            ++holdingPlayer.step;
            boolean bl = false;

            for(int o = l - n + 1; o < l + n; ++o) {
                if ((o & 15) == (holdingPlayer.step & 15) || bl) {
                    bl = false;
                    double d = 0.0D;

                    for(int p = m - n - 1; p < m + n; ++p) {
                        if (o >= 0 && p >= -1 && o < 128 && p < 128) {
                            int q = o - l;
                            int r = p - m;
                            boolean bl2 = q * q + r * r > (n - 2) * (n - 2);
                            int s = (j / i + o - 64) * i;
                            int t = (k / i + p - 64) * i;
                            Multiset<MaterialColor> multiset = LinkedHashMultiset.create();
                            LevelChunk levelChunk = world.getChunkAt(new BlockPos(s, 0, t));
                            if (!levelChunk.isEmpty()) {
                                ChunkPos chunkPos = levelChunk.getPos();
                                int u = s & 15;
                                int v = t & 15;
                                int w = 0;
                                double e = 0.0D;
                                if (world.dimensionType().hasCeiling()) {
                                    int x = s + t * 231871;
                                    x = x * x * 31287121 + x * 11;
                                    if ((x >> 20 & 1) == 0) {
                                        multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(world, BlockPos.ZERO), 10);
                                    } else {
                                        multiset.add(Blocks.STONE.defaultBlockState().getMapColor(world, BlockPos.ZERO), 100);
                                    }

                                    e = 100.0D;
                                } else {
                                    BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
                                    BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();

                                    for(int y = 0; y < i; ++y) {
                                        for(int z = 0; z < i; ++z) {
                                            int aa = levelChunk.getHeight(Heightmap.Types.WORLD_SURFACE, y + u, z + v) + 1;
                                            BlockState blockState3;
                                            if (aa <= world.getMinBuildHeight() + 1) {
                                                blockState3 = Blocks.BEDROCK.defaultBlockState();
                                            } else {
                                                do {
                                                    --aa;
                                                    mutableBlockPos.set(chunkPos.getMinBlockX() + y + u, aa, chunkPos.getMinBlockZ() + z + v);
                                                    blockState3 = levelChunk.getBlockState(mutableBlockPos);
                                                } while(blockState3.getMapColor(world, mutableBlockPos) == MaterialColor.NONE && aa > world.getMinBuildHeight());

                                                if (aa > world.getMinBuildHeight() && !blockState3.getFluidState().isEmpty()) {
                                                    int ab = aa - 1;
                                                    mutableBlockPos2.set(mutableBlockPos);

                                                    BlockState blockState2;
                                                    do {
                                                        mutableBlockPos2.setY(ab--);
                                                        blockState2 = levelChunk.getBlockState(mutableBlockPos2);
                                                        ++w;
                                                    } while(ab > world.getMinBuildHeight() && !blockState2.getFluidState().isEmpty());

                                                    blockState3 = this.getCorrectStateForFluidBlock(world, blockState3, mutableBlockPos);
                                                }
                                            }

                                            state.checkBanners(world, chunkPos.getMinBlockX() + y + u, chunkPos.getMinBlockZ() + z + v);
                                            e += (double)aa / (double)(i * i);
                                            multiset.add(blockState3.getMapColor(world, mutableBlockPos));
                                        }
                                    }
                                }

                                w /= i * i;
                                MaterialColor materialColor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MaterialColor.NONE);
                                MaterialColor.Brightness brightness;
                                if (materialColor == MaterialColor.WATER) {
                                    double f = (double)w * 0.1D + (double)(o + p & 1) * 0.2D;
                                    if (f < 0.5D) {
                                        brightness = MaterialColor.Brightness.HIGH;
                                    } else if (f > 0.9D) {
                                        brightness = MaterialColor.Brightness.LOW;
                                    } else {
                                        brightness = MaterialColor.Brightness.NORMAL;
                                    }
                                } else {
                                    double g = (e - d) * 4.0D / (double)(i + 4) + ((double)(o + p & 1) - 0.5D) * 0.4D;
                                    if (g > 0.6D) {
                                        brightness = MaterialColor.Brightness.HIGH;
                                    } else if (g < -0.6D) {
                                        brightness = MaterialColor.Brightness.LOW;
                                    } else {
                                        brightness = MaterialColor.Brightness.NORMAL;
                                    }
                                }

                                d = e;
                                if (p >= 0 && q * q + r * r < n * n && (!bl2 || (o + p & 1) != 0)) {
                                    bl |= state.updateColor(o, p, materialColor.getPackedId(brightness));
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private BlockState getCorrectStateForFluidBlock(Level world, BlockState state, BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isFaceSturdy(world, pos, Direction.UP) ? fluidState.createLegacyBlock() : state;
    }

    private static boolean isBiomeWatery(boolean[] biomes, int x, int z) {
        return biomes[z * 128 + x];
    }

    public static void renderBiomePreviewMap(ServerLevel world, ItemStack map) {
        MapItemSavedData mapItemSavedData = getSavedData(map, world);
        if (mapItemSavedData != null) {
            if (world.dimension() == mapItemSavedData.dimension) {
                int i = 1 << mapItemSavedData.scale;
                int j = mapItemSavedData.x;
                int k = mapItemSavedData.z;
                boolean[] bls = new boolean[16384];
                int l = j / i - 64;
                int m = k / i - 64;
                BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

                for(int n = 0; n < 128; ++n) {
                    for(int o = 0; o < 128; ++o) {
                        Biome.BiomeCategory biomeCategory = Biome.getBiomeCategory(world.getBiome(mutableBlockPos.set((l + o) * i, 0, (m + n) * i)));
                        bls[n * 128 + o] = biomeCategory == Biome.BiomeCategory.OCEAN || biomeCategory == Biome.BiomeCategory.RIVER || biomeCategory == Biome.BiomeCategory.SWAMP;
                    }
                }

                for(int p = 1; p < 127; ++p) {
                    for(int q = 1; q < 127; ++q) {
                        int r = 0;

                        for(int s = -1; s < 2; ++s) {
                            for(int t = -1; t < 2; ++t) {
                                if ((s != 0 || t != 0) && isBiomeWatery(bls, p + s, q + t)) {
                                    ++r;
                                }
                            }
                        }

                        MaterialColor.Brightness brightness = MaterialColor.Brightness.LOWEST;
                        MaterialColor materialColor = MaterialColor.NONE;
                        if (isBiomeWatery(bls, p, q)) {
                            materialColor = MaterialColor.COLOR_ORANGE;
                            if (r > 7 && q % 2 == 0) {
                                switch((p + (int)(Mth.sin((float)q + 0.0F) * 7.0F)) / 8 % 5) {
                                case 0:
                                case 4:
                                    brightness = MaterialColor.Brightness.LOW;
                                    break;
                                case 1:
                                case 3:
                                    brightness = MaterialColor.Brightness.NORMAL;
                                    break;
                                case 2:
                                    brightness = MaterialColor.Brightness.HIGH;
                                }
                            } else if (r > 7) {
                                materialColor = MaterialColor.NONE;
                            } else if (r > 5) {
                                brightness = MaterialColor.Brightness.NORMAL;
                            } else if (r > 3) {
                                brightness = MaterialColor.Brightness.LOW;
                            } else if (r > 1) {
                                brightness = MaterialColor.Brightness.LOW;
                            }
                        } else if (r > 0) {
                            materialColor = MaterialColor.COLOR_BROWN;
                            if (r > 3) {
                                brightness = MaterialColor.Brightness.NORMAL;
                            } else {
                                brightness = MaterialColor.Brightness.LOWEST;
                            }
                        }

                        if (materialColor != MaterialColor.NONE) {
                            mapItemSavedData.setColor(p, q, materialColor.getPackedId(brightness));
                        }
                    }
                }

            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (!world.isClientSide) {
            MapItemSavedData mapItemSavedData = getSavedData(stack, world);
            if (mapItemSavedData != null) {
                if (entity instanceof Player) {
                    Player player = (Player)entity;
                    mapItemSavedData.tickCarriedBy(player, stack);
                }

                if (!mapItemSavedData.locked && (selected || entity instanceof Player && ((Player)entity).getOffhandItem() == stack)) {
                    this.update(world, entity, mapItemSavedData);
                }

            }
        }
    }

    @Nullable
    @Override
    public Packet<?> getUpdatePacket(ItemStack stack, Level world, Player player) {
        Integer integer = getMapId(stack);
        MapItemSavedData mapItemSavedData = getSavedData(integer, world);
        return mapItemSavedData != null ? mapItemSavedData.getUpdatePacket(integer, player) : null;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
        CompoundTag compoundTag = stack.getTag();
        if (compoundTag != null && compoundTag.contains("map_scale_direction", 99)) {
            scaleMap(stack, world, compoundTag.getInt("map_scale_direction"));
            compoundTag.remove("map_scale_direction");
        } else if (compoundTag != null && compoundTag.contains("map_to_lock", 1) && compoundTag.getBoolean("map_to_lock")) {
            lockMap(world, stack);
            compoundTag.remove("map_to_lock");
        }

    }

    private static void scaleMap(ItemStack map, Level world, int amount) {
        MapItemSavedData mapItemSavedData = getSavedData(map, world);
        if (mapItemSavedData != null) {
            int i = world.getFreeMapId();
            world.setMapData(makeKey(i), mapItemSavedData.scaled(amount));
            storeMapData(map, i);
        }

    }

    public static void lockMap(Level world, ItemStack stack) {
        MapItemSavedData mapItemSavedData = getSavedData(stack, world);
        if (mapItemSavedData != null) {
            int i = world.getFreeMapId();
            String string = makeKey(i);
            MapItemSavedData mapItemSavedData2 = mapItemSavedData.locked();
            world.setMapData(string, mapItemSavedData2);
            storeMapData(stack, i);
        }

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        Integer integer = getMapId(stack);
        MapItemSavedData mapItemSavedData = world == null ? null : getSavedData(integer, world);
        if (mapItemSavedData != null && mapItemSavedData.locked) {
            tooltip.add((new TranslatableComponent("filled_map.locked", integer)).withStyle(ChatFormatting.GRAY));
        }

        if (context.isAdvanced()) {
            if (mapItemSavedData != null) {
                tooltip.add((new TranslatableComponent("filled_map.id", integer)).withStyle(ChatFormatting.GRAY));
                tooltip.add((new TranslatableComponent("filled_map.scale", 1 << mapItemSavedData.scale)).withStyle(ChatFormatting.GRAY));
                tooltip.add((new TranslatableComponent("filled_map.level", mapItemSavedData.scale, 4)).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add((new TranslatableComponent("filled_map.unknown")).withStyle(ChatFormatting.GRAY));
            }
        }

    }

    public static int getColor(ItemStack stack) {
        CompoundTag compoundTag = stack.getTagElement("display");
        if (compoundTag != null && compoundTag.contains("MapColor", 99)) {
            int i = compoundTag.getInt("MapColor");
            return -16777216 | i & 16777215;
        } else {
            return -12173266;
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
        if (blockState.is(BlockTags.BANNERS)) {
            if (!context.getLevel().isClientSide) {
                MapItemSavedData mapItemSavedData = getSavedData(context.getItemInHand(), context.getLevel());
                if (mapItemSavedData != null && !mapItemSavedData.toggleBanner(context.getLevel(), context.getClickedPos())) {
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        } else {
            return super.useOn(context);
        }
    }
}
