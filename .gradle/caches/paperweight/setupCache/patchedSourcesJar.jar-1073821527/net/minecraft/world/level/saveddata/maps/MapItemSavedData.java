package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class MapItemSavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAP_SIZE = 128;
    private static final int HALF_MAP_SIZE = 64;
    public static final int MAX_SCALE = 4;
    public static final int TRACKED_DECORATION_LIMIT = 256;
    public int x;
    public int z;
    public ResourceKey<Level> dimension;
    public boolean trackingPosition;
    public boolean unlimitedTracking;
    public byte scale;
    public byte[] colors = new byte[16384];
    public boolean locked;
    public final List<MapItemSavedData.HoldingPlayer> carriedBy = Lists.newArrayList();
    public final Map<Player, MapItemSavedData.HoldingPlayer> carriedByPlayers = Maps.newHashMap();
    private final Map<String, MapBanner> bannerMarkers = Maps.newHashMap();
    public final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
    private final Map<String, MapFrame> frameMarkers = Maps.newHashMap();
    private int trackedDecorationCount;

    private MapItemSavedData(int centerX, int centerZ, byte scale, boolean showIcons, boolean unlimitedTracking, boolean locked, ResourceKey<Level> dimension) {
        this.scale = scale;
        this.x = centerX;
        this.z = centerZ;
        this.dimension = dimension;
        this.trackingPosition = showIcons;
        this.unlimitedTracking = unlimitedTracking;
        this.locked = locked;
        this.setDirty();
    }

    public static MapItemSavedData createFresh(double centerX, double centerZ, byte scale, boolean showIcons, boolean unlimitedTracking, ResourceKey<Level> dimension) {
        int i = 128 * (1 << scale);
        int j = Mth.floor((centerX + 64.0D) / (double)i);
        int k = Mth.floor((centerZ + 64.0D) / (double)i);
        int l = j * i + i / 2 - 64;
        int m = k * i + i / 2 - 64;
        return new MapItemSavedData(l, m, scale, showIcons, unlimitedTracking, false, dimension);
    }

    public static MapItemSavedData createForClient(byte scale, boolean showIcons, ResourceKey<Level> dimension) {
        return new MapItemSavedData(0, 0, scale, false, false, showIcons, dimension);
    }

    public static MapItemSavedData load(CompoundTag nbt) {
        ResourceKey<Level> resourceKey = DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, nbt.get("dimension"))).resultOrPartial(LOGGER::error).orElseThrow(() -> {
            return new IllegalArgumentException("Invalid map dimension: " + nbt.get("dimension"));
        });
        int i = nbt.getInt("xCenter");
        int j = nbt.getInt("zCenter");
        byte b = (byte)Mth.clamp((int)nbt.getByte("scale"), (int)0, (int)4);
        boolean bl = !nbt.contains("trackingPosition", 1) || nbt.getBoolean("trackingPosition");
        boolean bl2 = nbt.getBoolean("unlimitedTracking");
        boolean bl3 = nbt.getBoolean("locked");
        MapItemSavedData mapItemSavedData = new MapItemSavedData(i, j, b, bl, bl2, bl3, resourceKey);
        byte[] bs = nbt.getByteArray("colors");
        if (bs.length == 16384) {
            mapItemSavedData.colors = bs;
        }

        ListTag listTag = nbt.getList("banners", 10);

        for(int k = 0; k < listTag.size(); ++k) {
            MapBanner mapBanner = MapBanner.load(listTag.getCompound(k));
            mapItemSavedData.bannerMarkers.put(mapBanner.getId(), mapBanner);
            mapItemSavedData.addDecoration(mapBanner.getDecoration(), (LevelAccessor)null, mapBanner.getId(), (double)mapBanner.getPos().getX(), (double)mapBanner.getPos().getZ(), 180.0D, mapBanner.getName());
        }

        ListTag listTag2 = nbt.getList("frames", 10);

        for(int l = 0; l < listTag2.size(); ++l) {
            MapFrame mapFrame = MapFrame.load(listTag2.getCompound(l));
            mapItemSavedData.frameMarkers.put(mapFrame.getId(), mapFrame);
            mapItemSavedData.addDecoration(MapDecoration.Type.FRAME, (LevelAccessor)null, "frame-" + mapFrame.getEntityId(), (double)mapFrame.getPos().getX(), (double)mapFrame.getPos().getZ(), (double)mapFrame.getRotation(), (Component)null);
        }

        return mapItemSavedData;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ResourceLocation.CODEC.encodeStart(NbtOps.INSTANCE, this.dimension.location()).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            nbt.put("dimension", tag);
        });
        nbt.putInt("xCenter", this.x);
        nbt.putInt("zCenter", this.z);
        nbt.putByte("scale", this.scale);
        nbt.putByteArray("colors", this.colors);
        nbt.putBoolean("trackingPosition", this.trackingPosition);
        nbt.putBoolean("unlimitedTracking", this.unlimitedTracking);
        nbt.putBoolean("locked", this.locked);
        ListTag listTag = new ListTag();

        for(MapBanner mapBanner : this.bannerMarkers.values()) {
            listTag.add(mapBanner.save());
        }

        nbt.put("banners", listTag);
        ListTag listTag2 = new ListTag();

        for(MapFrame mapFrame : this.frameMarkers.values()) {
            listTag2.add(mapFrame.save());
        }

        nbt.put("frames", listTag2);
        return nbt;
    }

    public MapItemSavedData locked() {
        MapItemSavedData mapItemSavedData = new MapItemSavedData(this.x, this.z, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension);
        mapItemSavedData.bannerMarkers.putAll(this.bannerMarkers);
        mapItemSavedData.decorations.putAll(this.decorations);
        mapItemSavedData.trackedDecorationCount = this.trackedDecorationCount;
        System.arraycopy(this.colors, 0, mapItemSavedData.colors, 0, this.colors.length);
        mapItemSavedData.setDirty();
        return mapItemSavedData;
    }

    public MapItemSavedData scaled(int zoomOutScale) {
        return createFresh((double)this.x, (double)this.z, (byte)Mth.clamp(this.scale + zoomOutScale, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension);
    }

    public void tickCarriedBy(Player player, ItemStack stack) {
        if (!this.carriedByPlayers.containsKey(player)) {
            MapItemSavedData.HoldingPlayer holdingPlayer = new MapItemSavedData.HoldingPlayer(player);
            this.carriedByPlayers.put(player, holdingPlayer);
            this.carriedBy.add(holdingPlayer);
        }

        if (!player.getInventory().contains(stack)) {
            this.removeDecoration(player.getName().getString());
        }

        for(int i = 0; i < this.carriedBy.size(); ++i) {
            MapItemSavedData.HoldingPlayer holdingPlayer2 = this.carriedBy.get(i);
            String string = holdingPlayer2.player.getName().getString();
            if (!holdingPlayer2.player.isRemoved() && (holdingPlayer2.player.getInventory().contains(stack) || stack.isFramed())) {
                if (!stack.isFramed() && holdingPlayer2.player.level.dimension() == this.dimension && this.trackingPosition) {
                    this.addDecoration(MapDecoration.Type.PLAYER, holdingPlayer2.player.level, string, holdingPlayer2.player.getX(), holdingPlayer2.player.getZ(), (double)holdingPlayer2.player.getYRot(), (Component)null);
                }
            } else {
                this.carriedByPlayers.remove(holdingPlayer2.player);
                this.carriedBy.remove(holdingPlayer2);
                this.removeDecoration(string);
            }
        }

        if (stack.isFramed() && this.trackingPosition) {
            ItemFrame itemFrame = stack.getFrame();
            BlockPos blockPos = itemFrame.getPos();
            MapFrame mapFrame = this.frameMarkers.get(MapFrame.frameId(blockPos));
            if (mapFrame != null && itemFrame.getId() != mapFrame.getEntityId() && this.frameMarkers.containsKey(mapFrame.getId())) {
                this.removeDecoration("frame-" + mapFrame.getEntityId());
            }

            MapFrame mapFrame2 = new MapFrame(blockPos, itemFrame.getDirection().get2DDataValue() * 90, itemFrame.getId());
            this.addDecoration(MapDecoration.Type.FRAME, player.level, "frame-" + itemFrame.getId(), (double)blockPos.getX(), (double)blockPos.getZ(), (double)(itemFrame.getDirection().get2DDataValue() * 90), (Component)null);
            this.frameMarkers.put(mapFrame2.getId(), mapFrame2);
        }

        CompoundTag compoundTag = stack.getTag();
        if (compoundTag != null && compoundTag.contains("Decorations", 9)) {
            ListTag listTag = compoundTag.getList("Decorations", 10);

            for(int j = 0; j < listTag.size(); ++j) {
                CompoundTag compoundTag2 = listTag.getCompound(j);
                if (!this.decorations.containsKey(compoundTag2.getString("id"))) {
                    this.addDecoration(MapDecoration.Type.byIcon(compoundTag2.getByte("type")), player.level, compoundTag2.getString("id"), compoundTag2.getDouble("x"), compoundTag2.getDouble("z"), compoundTag2.getDouble("rot"), (Component)null);
                }
            }
        }

    }

    private void removeDecoration(String id) {
        MapDecoration mapDecoration = this.decorations.remove(id);
        if (mapDecoration != null && mapDecoration.getType().shouldTrackCount()) {
            --this.trackedDecorationCount;
        }

        this.setDecorationsDirty();
    }

    public static void addTargetDecoration(ItemStack stack, BlockPos pos, String id, MapDecoration.Type type) {
        ListTag listTag;
        if (stack.hasTag() && stack.getTag().contains("Decorations", 9)) {
            listTag = stack.getTag().getList("Decorations", 10);
        } else {
            listTag = new ListTag();
            stack.addTagElement("Decorations", listTag);
        }

        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putByte("type", type.getIcon());
        compoundTag.putString("id", id);
        compoundTag.putDouble("x", (double)pos.getX());
        compoundTag.putDouble("z", (double)pos.getZ());
        compoundTag.putDouble("rot", 180.0D);
        listTag.add(compoundTag);
        if (type.hasMapColor()) {
            CompoundTag compoundTag2 = stack.getOrCreateTagElement("display");
            compoundTag2.putInt("MapColor", type.getMapColor());
        }

    }

    private void addDecoration(MapDecoration.Type type, @Nullable LevelAccessor world, String key, double x, double z, double rotation, @Nullable Component text) {
        int i = 1 << this.scale;
        float f = (float)(x - (double)this.x) / (float)i;
        float g = (float)(z - (double)this.z) / (float)i;
        byte b = (byte)((int)((double)(f * 2.0F) + 0.5D));
        byte c = (byte)((int)((double)(g * 2.0F) + 0.5D));
        int j = 63;
        byte d;
        if (f >= -63.0F && g >= -63.0F && f <= 63.0F && g <= 63.0F) {
            rotation += rotation < 0.0D ? -8.0D : 8.0D;
            d = (byte)((int)(rotation * 16.0D / 360.0D));
            if (this.dimension == Level.NETHER && world != null) {
                int k = (int)(world.getLevelData().getDayTime() / 10L);
                d = (byte)(k * k * 34187121 + k * 121 >> 15 & 15);
            }
        } else {
            if (type != MapDecoration.Type.PLAYER) {
                this.removeDecoration(key);
                return;
            }

            int l = 320;
            if (Math.abs(f) < 320.0F && Math.abs(g) < 320.0F) {
                type = MapDecoration.Type.PLAYER_OFF_MAP;
            } else {
                if (!this.unlimitedTracking) {
                    this.removeDecoration(key);
                    return;
                }

                type = MapDecoration.Type.PLAYER_OFF_LIMITS;
            }

            d = 0;
            if (f <= -63.0F) {
                b = -128;
            }

            if (g <= -63.0F) {
                c = -128;
            }

            if (f >= 63.0F) {
                b = 127;
            }

            if (g >= 63.0F) {
                c = 127;
            }
        }

        MapDecoration mapDecoration = new MapDecoration(type, b, c, d, text);
        MapDecoration mapDecoration2 = this.decorations.put(key, mapDecoration);
        if (!mapDecoration.equals(mapDecoration2)) {
            if (mapDecoration2 != null && mapDecoration2.getType().shouldTrackCount()) {
                --this.trackedDecorationCount;
            }

            if (type.shouldTrackCount()) {
                ++this.trackedDecorationCount;
            }

            this.setDecorationsDirty();
        }

    }

    @Nullable
    public Packet<?> getUpdatePacket(int id, Player player) {
        MapItemSavedData.HoldingPlayer holdingPlayer = this.carriedByPlayers.get(player);
        return holdingPlayer == null ? null : holdingPlayer.nextUpdatePacket(id);
    }

    public void setColorsDirty(int x, int z) {
        this.setDirty();

        for(MapItemSavedData.HoldingPlayer holdingPlayer : this.carriedBy) {
            holdingPlayer.markColorsDirty(x, z);
        }

    }

    public void setDecorationsDirty() {
        this.setDirty();
        this.carriedBy.forEach(MapItemSavedData.HoldingPlayer::markDecorationsDirty);
    }

    public MapItemSavedData.HoldingPlayer getHoldingPlayer(Player player) {
        MapItemSavedData.HoldingPlayer holdingPlayer = this.carriedByPlayers.get(player);
        if (holdingPlayer == null) {
            holdingPlayer = new MapItemSavedData.HoldingPlayer(player);
            this.carriedByPlayers.put(player, holdingPlayer);
            this.carriedBy.add(holdingPlayer);
        }

        return holdingPlayer;
    }

    public boolean toggleBanner(LevelAccessor world, BlockPos pos) {
        double d = (double)pos.getX() + 0.5D;
        double e = (double)pos.getZ() + 0.5D;
        int i = 1 << this.scale;
        double f = (d - (double)this.x) / (double)i;
        double g = (e - (double)this.z) / (double)i;
        int j = 63;
        if (f >= -63.0D && g >= -63.0D && f <= 63.0D && g <= 63.0D) {
            MapBanner mapBanner = MapBanner.fromWorld(world, pos);
            if (mapBanner == null) {
                return false;
            }

            if (this.bannerMarkers.remove(mapBanner.getId(), mapBanner)) {
                this.removeDecoration(mapBanner.getId());
                return true;
            }

            if (!this.isTrackedCountOverLimit(256)) {
                this.bannerMarkers.put(mapBanner.getId(), mapBanner);
                this.addDecoration(mapBanner.getDecoration(), world, mapBanner.getId(), d, e, 180.0D, mapBanner.getName());
                return true;
            }
        }

        return false;
    }

    public void checkBanners(BlockGetter world, int x, int z) {
        Iterator<MapBanner> iterator = this.bannerMarkers.values().iterator();

        while(iterator.hasNext()) {
            MapBanner mapBanner = iterator.next();
            if (mapBanner.getPos().getX() == x && mapBanner.getPos().getZ() == z) {
                MapBanner mapBanner2 = MapBanner.fromWorld(world, mapBanner.getPos());
                if (!mapBanner.equals(mapBanner2)) {
                    iterator.remove();
                    this.removeDecoration(mapBanner.getId());
                }
            }
        }

    }

    public Collection<MapBanner> getBanners() {
        return this.bannerMarkers.values();
    }

    public void removedFromFrame(BlockPos pos, int id) {
        this.removeDecoration("frame-" + id);
        this.frameMarkers.remove(MapFrame.frameId(pos));
    }

    public boolean updateColor(int x, int z, byte color) {
        byte b = this.colors[x + z * 128];
        if (b != color) {
            this.setColor(x, z, color);
            return true;
        } else {
            return false;
        }
    }

    public void setColor(int x, int z, byte color) {
        this.colors[x + z * 128] = color;
        this.setColorsDirty(x, z);
    }

    public boolean isExplorationMap() {
        for(MapDecoration mapDecoration : this.decorations.values()) {
            if (mapDecoration.getType() == MapDecoration.Type.MANSION || mapDecoration.getType() == MapDecoration.Type.MONUMENT) {
                return true;
            }
        }

        return false;
    }

    public void addClientSideDecorations(List<MapDecoration> icons) {
        this.decorations.clear();
        this.trackedDecorationCount = 0;

        for(int i = 0; i < icons.size(); ++i) {
            MapDecoration mapDecoration = icons.get(i);
            this.decorations.put("icon-" + i, mapDecoration);
            if (mapDecoration.getType().shouldTrackCount()) {
                ++this.trackedDecorationCount;
            }
        }

    }

    public Iterable<MapDecoration> getDecorations() {
        return this.decorations.values();
    }

    public boolean isTrackedCountOverLimit(int i) {
        return this.trackedDecorationCount >= i;
    }

    public class HoldingPlayer {
        public final Player player;
        private boolean dirtyData = true;
        private int minDirtyX;
        private int minDirtyY;
        private int maxDirtyX = 127;
        private int maxDirtyY = 127;
        private boolean dirtyDecorations = true;
        private int tick;
        public int step;

        HoldingPlayer(Player player) {
            this.player = player;
        }

        private MapItemSavedData.MapPatch createPatch() {
            int i = this.minDirtyX;
            int j = this.minDirtyY;
            int k = this.maxDirtyX + 1 - this.minDirtyX;
            int l = this.maxDirtyY + 1 - this.minDirtyY;
            byte[] bs = new byte[k * l];

            for(int m = 0; m < k; ++m) {
                for(int n = 0; n < l; ++n) {
                    bs[m + n * k] = MapItemSavedData.this.colors[i + m + (j + n) * 128];
                }
            }

            return new MapItemSavedData.MapPatch(i, j, k, l, bs);
        }

        @Nullable
        Packet<?> nextUpdatePacket(int mapId) {
            MapItemSavedData.MapPatch mapPatch;
            if (this.dirtyData) {
                this.dirtyData = false;
                mapPatch = this.createPatch();
            } else {
                mapPatch = null;
            }

            Collection<MapDecoration> collection;
            if (this.dirtyDecorations && this.tick++ % 5 == 0) {
                this.dirtyDecorations = false;
                collection = MapItemSavedData.this.decorations.values();
            } else {
                collection = null;
            }

            return collection == null && mapPatch == null ? null : new ClientboundMapItemDataPacket(mapId, MapItemSavedData.this.scale, MapItemSavedData.this.locked, collection, mapPatch);
        }

        void markColorsDirty(int startX, int startZ) {
            if (this.dirtyData) {
                this.minDirtyX = Math.min(this.minDirtyX, startX);
                this.minDirtyY = Math.min(this.minDirtyY, startZ);
                this.maxDirtyX = Math.max(this.maxDirtyX, startX);
                this.maxDirtyY = Math.max(this.maxDirtyY, startZ);
            } else {
                this.dirtyData = true;
                this.minDirtyX = startX;
                this.minDirtyY = startZ;
                this.maxDirtyX = startX;
                this.maxDirtyY = startZ;
            }

        }

        private void markDecorationsDirty() {
            this.dirtyDecorations = true;
        }
    }

    public static class MapPatch {
        public final int startX;
        public final int startY;
        public final int width;
        public final int height;
        public final byte[] mapColors;

        public MapPatch(int startX, int startZ, int width, int height, byte[] colors) {
            this.startX = startX;
            this.startY = startZ;
            this.width = width;
            this.height = height;
            this.mapColors = colors;
        }

        public void applyToMap(MapItemSavedData mapState) {
            for(int i = 0; i < this.width; ++i) {
                for(int j = 0; j < this.height; ++j) {
                    mapState.setColor(this.startX + i, this.startY + j, this.mapColors[i + j * this.width]);
                }
            }

        }
    }
}
