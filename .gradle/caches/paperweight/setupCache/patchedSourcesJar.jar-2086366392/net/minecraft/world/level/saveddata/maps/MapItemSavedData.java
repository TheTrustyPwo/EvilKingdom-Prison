package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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

// CraftBukkit start
import io.papermc.paper.adventure.PaperAdventure; // Paper
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.map.CraftMapView;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage;
// CraftBukkit end

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
    private org.bukkit.craftbukkit.v1_18_R2.map.RenderData vanillaRender = new org.bukkit.craftbukkit.v1_18_R2.map.RenderData(); // Paper

    // CraftBukkit start
    public final CraftMapView mapView;
    private CraftServer server;
    private UUID uniqueId = null;
    public String id;
    // CraftBukkit end

    private MapItemSavedData(int centerX, int centerZ, byte scale, boolean showIcons, boolean unlimitedTracking, boolean locked, ResourceKey<Level> dimension) {
        this.scale = scale;
        this.x = centerX;
        this.z = centerZ;
        this.dimension = dimension;
        this.trackingPosition = showIcons;
        this.unlimitedTracking = unlimitedTracking;
        this.locked = locked;
        this.setDirty();
        // CraftBukkit start
        this.mapView = new CraftMapView(this);
        this.server = (CraftServer) org.bukkit.Bukkit.getServer();
        this.vanillaRender.buffer = colors; // Paper
        // CraftBukkit end
    }

    public static MapItemSavedData createFresh(double centerX, double centerZ, byte scale, boolean showIcons, boolean unlimitedTracking, ResourceKey<Level> dimension) {
        int i = 128 * (1 << scale);
        int j = Mth.floor((centerX + 64.0D) / (double) i);
        int k = Mth.floor((centerZ + 64.0D) / (double) i);
        int l = j * i + i / 2 - 64;
        int i1 = k * i + i / 2 - 64;

        return new MapItemSavedData(l, i1, scale, showIcons, unlimitedTracking, false, dimension);
    }

    public static MapItemSavedData createForClient(byte scale, boolean showIcons, ResourceKey<Level> dimension) {
        return new MapItemSavedData(0, 0, scale, false, false, showIcons, dimension);
    }

    public static MapItemSavedData load(CompoundTag nbt) {
        // Paper start - fix "Not a string" spam
        Tag dimension = nbt.get("dimension");
        if (dimension instanceof NumericTag && ((NumericTag) dimension).getAsInt() >= CraftWorld.CUSTOM_DIMENSION_OFFSET) {
            long least = nbt.getLong("UUIDLeast");
            long most = nbt.getLong("UUIDMost");

            if (least != 0L && most != 0L) {
                UUID uuid = new UUID(most, least);
                CraftWorld world = (CraftWorld) Bukkit.getWorld(uuid);
                if (world != null) {
                    dimension = StringTag.valueOf("minecraft:" + world.getName().toLowerCase(java.util.Locale.ENGLISH));
                } else {
                    dimension = StringTag.valueOf("bukkit:_invalidworld_");
                }
            } else {
                dimension = StringTag.valueOf("bukkit:_invalidworld_");
            }
        }
        DataResult<ResourceKey<Level>> dataresult = DimensionType.parseLegacy(new Dynamic(NbtOps.INSTANCE, dimension)); // CraftBukkit - decompile error
        // Paper end - fix "Not a string" spam
        Logger logger = MapItemSavedData.LOGGER;

        Objects.requireNonNull(logger);
        // CraftBukkit start
        ResourceKey<Level> resourcekey = dataresult.resultOrPartial(logger::error).orElseGet(() -> {
            long least = nbt.getLong("UUIDLeast");
            long most = nbt.getLong("UUIDMost");

            if (least != 0L && most != 0L) {
                UUID uniqueId = new UUID(most, least);

                CraftWorld world = (CraftWorld) Bukkit.getWorld(uniqueId);
                // Check if the stored world details are correct.
                if (world == null) {
                    /* All Maps which do not have their valid world loaded are set to a dimension which hopefully won't be reached.
                       This is to prevent them being corrupted with the wrong map data. */
                    // PAIL: Use Vanilla exception handling for now
                } else {
                    return world.getHandle().dimension();
                }
            }
            throw new IllegalArgumentException("Invalid map dimension: " + nbt.get("dimension"));
            // CraftBukkit end
        });
        int i = nbt.getInt("xCenter");
        int j = nbt.getInt("zCenter");
        byte b0 = (byte) Mth.clamp((int) nbt.getByte("scale"), (int) 0, (int) 4);
        boolean flag = !nbt.contains("trackingPosition", 1) || nbt.getBoolean("trackingPosition");
        boolean flag1 = nbt.getBoolean("unlimitedTracking");
        boolean flag2 = nbt.getBoolean("locked");
        MapItemSavedData worldmap = new MapItemSavedData(i, j, b0, flag, flag1, flag2, resourcekey);
        byte[] abyte = nbt.getByteArray("colors");

        if (abyte.length == 16384) {
            worldmap.colors = abyte;
        }
        worldmap.vanillaRender.buffer = abyte; // Paper

        ListTag nbttaglist = nbt.getList("banners", 10);

        for (int k = 0; k < nbttaglist.size(); ++k) {
            MapBanner mapiconbanner = MapBanner.load(nbttaglist.getCompound(k));

            worldmap.bannerMarkers.put(mapiconbanner.getId(), mapiconbanner);
            worldmap.addDecoration(mapiconbanner.getDecoration(), (LevelAccessor) null, mapiconbanner.getId(), (double) mapiconbanner.getPos().getX(), (double) mapiconbanner.getPos().getZ(), 180.0D, mapiconbanner.getName());
        }

        ListTag nbttaglist1 = nbt.getList("frames", 10);

        for (int l = 0; l < nbttaglist1.size(); ++l) {
            MapFrame worldmapframe = MapFrame.load(nbttaglist1.getCompound(l));

            worldmap.frameMarkers.put(worldmapframe.getId(), worldmapframe);
            worldmap.addDecoration(MapDecoration.Type.FRAME, (LevelAccessor) null, "frame-" + worldmapframe.getEntityId(), (double) worldmapframe.getPos().getX(), (double) worldmapframe.getPos().getZ(), (double) worldmapframe.getRotation(), (Component) null);
        }

        return worldmap;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        DataResult<Tag> dataresult = ResourceLocation.CODEC.encodeStart(NbtOps.INSTANCE, this.dimension.location()); // CraftBukkit - decompile error
        Logger logger = MapItemSavedData.LOGGER;

        Objects.requireNonNull(logger);
        dataresult.resultOrPartial(logger::error).ifPresent((nbtbase) -> {
            nbt.put("dimension", nbtbase);
        });
        // CraftBukkit start
        if (true) {
            if (this.uniqueId == null) {
                for (org.bukkit.World world : this.server.getWorlds()) {
                    CraftWorld cWorld = (CraftWorld) world;
                    if (cWorld.getHandle().dimension() == this.dimension) {
                        this.uniqueId = cWorld.getUID();
                        break;
                    }
                }
            }
            /* Perform a second check to see if a matching world was found, this is a necessary
               change incase Maps are forcefully unlinked from a World and lack a UID.*/
            if (this.uniqueId != null) {
                nbt.putLong("UUIDLeast", this.uniqueId.getLeastSignificantBits());
                nbt.putLong("UUIDMost", this.uniqueId.getMostSignificantBits());
            }
        }
        // CraftBukkit end
        nbt.putInt("xCenter", this.x);
        nbt.putInt("zCenter", this.z);
        nbt.putByte("scale", this.scale);
        nbt.putByteArray("colors", this.colors);
        nbt.putBoolean("trackingPosition", this.trackingPosition);
        nbt.putBoolean("unlimitedTracking", this.unlimitedTracking);
        nbt.putBoolean("locked", this.locked);
        ListTag nbttaglist = new ListTag();
        Iterator iterator = this.bannerMarkers.values().iterator();

        while (iterator.hasNext()) {
            MapBanner mapiconbanner = (MapBanner) iterator.next();

            nbttaglist.add(mapiconbanner.save());
        }

        nbt.put("banners", nbttaglist);
        ListTag nbttaglist1 = new ListTag();
        Iterator iterator1 = this.frameMarkers.values().iterator();

        while (iterator1.hasNext()) {
            MapFrame worldmapframe = (MapFrame) iterator1.next();

            nbttaglist1.add(worldmapframe.save());
        }

        nbt.put("frames", nbttaglist1);
        return nbt;
    }

    public MapItemSavedData locked() {
        MapItemSavedData worldmap = new MapItemSavedData(this.x, this.z, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension);

        worldmap.bannerMarkers.putAll(this.bannerMarkers);
        worldmap.decorations.putAll(this.decorations);
        worldmap.trackedDecorationCount = this.trackedDecorationCount;
        System.arraycopy(this.colors, 0, worldmap.colors, 0, this.colors.length);
        worldmap.setDirty();
        return worldmap;
    }

    public MapItemSavedData scaled(int zoomOutScale) {
        return MapItemSavedData.createFresh((double) this.x, (double) this.z, (byte) Mth.clamp(this.scale + zoomOutScale, (int) 0, (int) 4), this.trackingPosition, this.unlimitedTracking, this.dimension);
    }

    public void tickCarriedBy(Player player, ItemStack stack) {
        if (!this.carriedByPlayers.containsKey(player)) {
            MapItemSavedData.HoldingPlayer worldmap_worldmaphumantracker = new MapItemSavedData.HoldingPlayer(player);

            this.carriedByPlayers.put(player, worldmap_worldmaphumantracker);
            this.carriedBy.add(worldmap_worldmaphumantracker);
        }

        if (!player.getInventory().contains(stack)) {
            this.removeDecoration(player.getName().getString());
        }

        for (int i = 0; i < this.carriedBy.size(); ++i) {
            MapItemSavedData.HoldingPlayer worldmap_worldmaphumantracker1 = (MapItemSavedData.HoldingPlayer) this.carriedBy.get(i);
            String s = worldmap_worldmaphumantracker1.player.getName().getString();

            if (!worldmap_worldmaphumantracker1.player.isRemoved() && (worldmap_worldmaphumantracker1.player.getInventory().contains(stack) || stack.isFramed())) {
                if (!stack.isFramed() && worldmap_worldmaphumantracker1.player.level.dimension() == this.dimension && this.trackingPosition) {
                    this.addDecoration(MapDecoration.Type.PLAYER, worldmap_worldmaphumantracker1.player.level, s, worldmap_worldmaphumantracker1.player.getX(), worldmap_worldmaphumantracker1.player.getZ(), (double) worldmap_worldmaphumantracker1.player.getYRot(), (Component) null);
                }
            } else {
                this.carriedByPlayers.remove(worldmap_worldmaphumantracker1.player);
                this.carriedBy.remove(worldmap_worldmaphumantracker1);
                this.removeDecoration(s);
            }
        }

        if (stack.isFramed() && this.trackingPosition) {
            ItemFrame entityitemframe = stack.getFrame();
            BlockPos blockposition = entityitemframe.getPos();
            MapFrame worldmapframe = (MapFrame) this.frameMarkers.get(MapFrame.frameId(blockposition));

            if (worldmapframe != null && entityitemframe.getId() != worldmapframe.getEntityId() && this.frameMarkers.containsKey(worldmapframe.getId())) {
                this.removeDecoration("frame-" + worldmapframe.getEntityId());
            }

            MapFrame worldmapframe1 = new MapFrame(blockposition, entityitemframe.getDirection().get2DDataValue() * 90, entityitemframe.getId());

            // Paper start
            if (this.decorations.size() < player.level.paperConfig.mapItemFrameCursorLimit) {
            this.addDecoration(MapDecoration.Type.FRAME, player.level, "frame-" + entityitemframe.getId(), (double) blockposition.getX(), (double) blockposition.getZ(), (double) (entityitemframe.getDirection().get2DDataValue() * 90), (Component) null);
            this.frameMarkers.put(worldmapframe1.getId(), worldmapframe1);
            }
            // Paper end
        }

        CompoundTag nbttagcompound = stack.getTag();

        if (nbttagcompound != null && nbttagcompound.contains("Decorations", 9)) {
            ListTag nbttaglist = nbttagcompound.getList("Decorations", 10);

            for (int j = 0; j < nbttaglist.size(); ++j) {
                CompoundTag nbttagcompound1 = nbttaglist.getCompound(j);

                if (!this.decorations.containsKey(nbttagcompound1.getString("id"))) {
                    this.addDecoration(MapDecoration.Type.byIcon(nbttagcompound1.getByte("type")), player.level, nbttagcompound1.getString("id"), nbttagcompound1.getDouble("x"), nbttagcompound1.getDouble("z"), nbttagcompound1.getDouble("rot"), (Component) null);
                }
            }
        }

    }

    private void removeDecoration(String id) {
        MapDecoration mapicon = (MapDecoration) this.decorations.remove(id);

        if (mapicon != null && mapicon.getType().shouldTrackCount()) {
            --this.trackedDecorationCount;
        }

        this.setDecorationsDirty();
    }

    public static void addTargetDecoration(ItemStack stack, BlockPos pos, String id, MapDecoration.Type type) {
        ListTag nbttaglist;

        if (stack.hasTag() && stack.getTag().contains("Decorations", 9)) {
            nbttaglist = stack.getTag().getList("Decorations", 10);
        } else {
            nbttaglist = new ListTag();
            stack.addTagElement("Decorations", nbttaglist);
        }

        CompoundTag nbttagcompound = new CompoundTag();

        nbttagcompound.putByte("type", type.getIcon());
        nbttagcompound.putString("id", id);
        nbttagcompound.putDouble("x", (double) pos.getX());
        nbttagcompound.putDouble("z", (double) pos.getZ());
        nbttagcompound.putDouble("rot", 180.0D);
        nbttaglist.add(nbttagcompound);
        if (type.hasMapColor()) {
            CompoundTag nbttagcompound1 = stack.getOrCreateTagElement("display");

            nbttagcompound1.putInt("MapColor", type.getMapColor());
        }

    }

    private void addDecoration(MapDecoration.Type type, @Nullable LevelAccessor world, String key, double x, double z, double rotation, @Nullable Component text) {
        int i = 1 << this.scale;
        float f = (float) (x - (double) this.x) / (float) i;
        float f1 = (float) (z - (double) this.z) / (float) i;
        byte b0 = (byte) ((int) ((double) (f * 2.0F) + 0.5D));
        byte b1 = (byte) ((int) ((double) (f1 * 2.0F) + 0.5D));
        boolean flag = true;
        byte b2;

        if (f >= -63.0F && f1 >= -63.0F && f <= 63.0F && f1 <= 63.0F) {
            rotation += rotation < 0.0D ? -8.0D : 8.0D;
            b2 = (byte) ((int) (rotation * 16.0D / 360.0D));
            if (this.dimension == Level.NETHER && world != null) {
                int j = (int) (world.getLevelData().getDayTime() / 10L);

                b2 = (byte) (j * j * 34187121 + j * 121 >> 15 & 15);
            }
        } else {
            if (type != MapDecoration.Type.PLAYER) {
                this.removeDecoration(key);
                return;
            }

            boolean flag1 = true;

            if (Math.abs(f) < 320.0F && Math.abs(f1) < 320.0F) {
                type = MapDecoration.Type.PLAYER_OFF_MAP;
            } else {
                if (!this.unlimitedTracking) {
                    this.removeDecoration(key);
                    return;
                }

                type = MapDecoration.Type.PLAYER_OFF_LIMITS;
            }

            b2 = 0;
            if (f <= -63.0F) {
                b0 = -128;
            }

            if (f1 <= -63.0F) {
                b1 = -128;
            }

            if (f >= 63.0F) {
                b0 = 127;
            }

            if (f1 >= 63.0F) {
                b1 = 127;
            }
        }

        MapDecoration mapicon = new MapDecoration(type, b0, b1, b2, text);
        MapDecoration mapicon1 = (MapDecoration) this.decorations.put(key, mapicon);

        if (!mapicon.equals(mapicon1)) {
            if (mapicon1 != null && mapicon1.getType().shouldTrackCount()) {
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
        MapItemSavedData.HoldingPlayer worldmap_worldmaphumantracker = (MapItemSavedData.HoldingPlayer) this.carriedByPlayers.get(player);

        return worldmap_worldmaphumantracker == null ? null : worldmap_worldmaphumantracker.nextUpdatePacket(id);
    }

    public void setColorsDirty(int x, int z) {
        this.setDirty();
        Iterator iterator = this.carriedBy.iterator();

        while (iterator.hasNext()) {
            MapItemSavedData.HoldingPlayer worldmap_worldmaphumantracker = (MapItemSavedData.HoldingPlayer) iterator.next();

            worldmap_worldmaphumantracker.markColorsDirty(x, z);
        }

    }

    public void setDecorationsDirty() {
        this.setDirty();
        this.carriedBy.forEach(MapItemSavedData.HoldingPlayer::markDecorationsDirty);
    }

    public MapItemSavedData.HoldingPlayer getHoldingPlayer(Player player) {
        MapItemSavedData.HoldingPlayer worldmap_worldmaphumantracker = (MapItemSavedData.HoldingPlayer) this.carriedByPlayers.get(player);

        if (worldmap_worldmaphumantracker == null) {
            worldmap_worldmaphumantracker = new MapItemSavedData.HoldingPlayer(player);
            this.carriedByPlayers.put(player, worldmap_worldmaphumantracker);
            this.carriedBy.add(worldmap_worldmaphumantracker);
        }

        return worldmap_worldmaphumantracker;
    }

    public boolean toggleBanner(LevelAccessor world, BlockPos pos) {
        double d0 = (double) pos.getX() + 0.5D;
        double d1 = (double) pos.getZ() + 0.5D;
        int i = 1 << this.scale;
        double d2 = (d0 - (double) this.x) / (double) i;
        double d3 = (d1 - (double) this.z) / (double) i;
        boolean flag = true;

        if (d2 >= -63.0D && d3 >= -63.0D && d2 <= 63.0D && d3 <= 63.0D) {
            MapBanner mapiconbanner = MapBanner.fromWorld(world, pos);

            if (mapiconbanner == null) {
                return false;
            }

            if (this.bannerMarkers.remove(mapiconbanner.getId(), mapiconbanner)) {
                this.removeDecoration(mapiconbanner.getId());
                return true;
            }

            if (!this.isTrackedCountOverLimit(256)) {
                this.bannerMarkers.put(mapiconbanner.getId(), mapiconbanner);
                this.addDecoration(mapiconbanner.getDecoration(), world, mapiconbanner.getId(), d0, d1, 180.0D, mapiconbanner.getName());
                return true;
            }
        }

        return false;
    }

    public void checkBanners(BlockGetter world, int x, int z) {
        Iterator iterator = this.bannerMarkers.values().iterator();

        while (iterator.hasNext()) {
            MapBanner mapiconbanner = (MapBanner) iterator.next();

            if (mapiconbanner.getPos().getX() == x && mapiconbanner.getPos().getZ() == z) {
                MapBanner mapiconbanner1 = MapBanner.fromWorld(world, mapiconbanner.getPos());

                if (!mapiconbanner.equals(mapiconbanner1)) {
                    iterator.remove();
                    this.removeDecoration(mapiconbanner.getId());
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
        byte b1 = this.colors[x + z * 128];

        if (b1 != color) {
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
        Iterator iterator = this.decorations.values().iterator();

        MapDecoration mapicon;

        do {
            if (!iterator.hasNext()) {
                return false;
            }

            mapicon = (MapDecoration) iterator.next();
        } while (mapicon.getType() != MapDecoration.Type.MANSION && mapicon.getType() != MapDecoration.Type.MONUMENT);

        return true;
    }

    public void addClientSideDecorations(List<MapDecoration> icons) {
        this.decorations.clear();
        this.trackedDecorationCount = 0;

        for (int i = 0; i < icons.size(); ++i) {
            MapDecoration mapicon = (MapDecoration) icons.get(i);

            this.decorations.put("icon-" + i, mapicon);
            if (mapicon.getType().shouldTrackCount()) {
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

        // Paper start
        private void addSeenPlayers(java.util.Collection<MapDecoration> icons) {
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) this.player.getBukkitEntity();
            MapItemSavedData.this.decorations.forEach((name, mapIcon) -> {
                // If this cursor is for a player check visibility with vanish system
                org.bukkit.entity.Player other = org.bukkit.Bukkit.getPlayerExact(name); // Spigot
                if (other == null || player.canSee(other)) {
                    icons.add(mapIcon);
                }
            });
        }
        private boolean shouldUseVanillaMap() {
            return mapView.getRenderers().size() == 1 && mapView.getRenderers().get(0).getClass() == org.bukkit.craftbukkit.v1_18_R2.map.CraftMapRenderer.class;
        }
        // Paper end
        public final Player player;
        private boolean dirtyData = true;
        private int minDirtyX;
        private int minDirtyY;
        private int maxDirtyX = 127;
        private int maxDirtyY = 127;
        private boolean dirtyDecorations = true;
        private int tick;
        public int step;

        HoldingPlayer(Player entityhuman) {
            this.player = entityhuman;
        }

        private MapItemSavedData.MapPatch createPatch(byte[] buffer) { // CraftBukkit
            int i = this.minDirtyX;
            int j = this.minDirtyY;
            int k = this.maxDirtyX + 1 - this.minDirtyX;
            int l = this.maxDirtyY + 1 - this.minDirtyY;
            byte[] abyte = new byte[k * l];

            for (int i1 = 0; i1 < k; ++i1) {
                for (int j1 = 0; j1 < l; ++j1) {
                    abyte[i1 + j1 * k] = buffer[i + i1 + (j + j1) * 128]; // CraftBukkit
                }
            }

            return new MapItemSavedData.MapPatch(i, j, k, l, abyte);
        }

        @Nullable
        Packet<?> nextUpdatePacket(int mapId) {
            MapItemSavedData.MapPatch worldmap_b;
            if (!this.dirtyData && this.tick % 5 != 0) { this.tick++; return null; } // Paper - this won't end up sending, so don't render it!
            boolean vanillaMaps = shouldUseVanillaMap(); // Paper
            org.bukkit.craftbukkit.v1_18_R2.map.RenderData render = !vanillaMaps ? MapItemSavedData.this.mapView.render((org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer) this.player.getBukkitEntity()) : MapItemSavedData.this.vanillaRender; // CraftBukkit // Paper

            if (this.dirtyData) {
                this.dirtyData = false;
                worldmap_b = this.createPatch(render.buffer); // CraftBukkit
            } else {
                worldmap_b = null;
            }

            Collection collection;

            if ((true || this.dirtyDecorations) && this.tick++ % 5 == 0) { // CraftBukkit - custom maps don't update this yet
                this.dirtyDecorations = false;
                // CraftBukkit start
                java.util.Collection<MapDecoration> icons = new java.util.ArrayList<MapDecoration>();

                if (vanillaMaps) addSeenPlayers(icons); // Paper

                for (org.bukkit.map.MapCursor cursor : render.cursors) {
                    if (cursor.isVisible()) {
                        icons.add(new MapDecoration(MapDecoration.Type.byIcon(cursor.getRawType()), cursor.getX(), cursor.getY(), cursor.getDirection(), PaperAdventure.asVanilla(cursor.caption()))); // Paper - Adventure
                    }
                }
                collection = icons;
                // CraftBukkit end
            } else {
                collection = null;
            }

            return collection == null && worldmap_b == null ? null : new ClientboundMapItemDataPacket(mapId, MapItemSavedData.this.scale, MapItemSavedData.this.locked, collection, worldmap_b);
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
            for (int i = 0; i < this.width; ++i) {
                for (int j = 0; j < this.height; ++j) {
                    mapState.setColor(this.startX + i, this.startY + j, this.mapColors[i + j * this.width]);
                }
            }

        }
    }
}
