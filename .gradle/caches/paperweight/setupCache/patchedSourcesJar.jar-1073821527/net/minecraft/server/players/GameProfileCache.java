package net.minecraft.server.players;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class GameProfileCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int GAMEPROFILES_MRU_LIMIT = 1000;
    private static final int GAMEPROFILES_EXPIRATION_MONTHS = 1;
    private static boolean usesAuthentication;
    private final Map<String, GameProfileCache.GameProfileInfo> profilesByName = Maps.newConcurrentMap();
    private final Map<UUID, GameProfileCache.GameProfileInfo> profilesByUUID = Maps.newConcurrentMap();
    private final Map<String, CompletableFuture<Optional<GameProfile>>> requests = Maps.newConcurrentMap();
    private final GameProfileRepository profileRepository;
    private final Gson gson = (new GsonBuilder()).create();
    private final File file;
    private final AtomicLong operationCount = new AtomicLong();
    @Nullable
    private Executor executor;

    public GameProfileCache(GameProfileRepository profileRepository, File cacheFile) {
        this.profileRepository = profileRepository;
        this.file = cacheFile;
        Lists.reverse(this.load()).forEach(this::safeAdd);
    }

    private void safeAdd(GameProfileCache.GameProfileInfo entry) {
        GameProfile gameProfile = entry.getProfile();
        entry.setLastAccess(this.getNextOperation());
        String string = gameProfile.getName();
        if (string != null) {
            this.profilesByName.put(string.toLowerCase(Locale.ROOT), entry);
        }

        UUID uUID = gameProfile.getId();
        if (uUID != null) {
            this.profilesByUUID.put(uUID, entry);
        }

    }

    private static Optional<GameProfile> lookupGameProfile(GameProfileRepository repository, String name) {
        final AtomicReference<GameProfile> atomicReference = new AtomicReference<>();
        ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
            public void onProfileLookupSucceeded(GameProfile gameProfile) {
                atomicReference.set(gameProfile);
            }

            public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
                atomicReference.set((GameProfile)null);
            }
        };
        repository.findProfilesByNames(new String[]{name}, Agent.MINECRAFT, profileLookupCallback);
        GameProfile gameProfile = atomicReference.get();
        if (!usesAuthentication() && gameProfile == null) {
            UUID uUID = Player.createPlayerUUID(new GameProfile((UUID)null, name));
            return Optional.of(new GameProfile(uUID, name));
        } else {
            return Optional.ofNullable(gameProfile);
        }
    }

    public static void setUsesAuthentication(boolean value) {
        usesAuthentication = value;
    }

    private static boolean usesAuthentication() {
        return usesAuthentication;
    }

    public void add(GameProfile profile) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(2, 1);
        Date date = calendar.getTime();
        GameProfileCache.GameProfileInfo gameProfileInfo = new GameProfileCache.GameProfileInfo(profile, date);
        this.safeAdd(gameProfileInfo);
        this.save();
    }

    private long getNextOperation() {
        return this.operationCount.incrementAndGet();
    }

    public Optional<GameProfile> get(String name) {
        String string = name.toLowerCase(Locale.ROOT);
        GameProfileCache.GameProfileInfo gameProfileInfo = this.profilesByName.get(string);
        boolean bl = false;
        if (gameProfileInfo != null && (new Date()).getTime() >= gameProfileInfo.expirationDate.getTime()) {
            this.profilesByUUID.remove(gameProfileInfo.getProfile().getId());
            this.profilesByName.remove(gameProfileInfo.getProfile().getName().toLowerCase(Locale.ROOT));
            bl = true;
            gameProfileInfo = null;
        }

        Optional<GameProfile> optional;
        if (gameProfileInfo != null) {
            gameProfileInfo.setLastAccess(this.getNextOperation());
            optional = Optional.of(gameProfileInfo.getProfile());
        } else {
            optional = lookupGameProfile(this.profileRepository, string);
            if (optional.isPresent()) {
                this.add(optional.get());
                bl = false;
            }
        }

        if (bl) {
            this.save();
        }

        return optional;
    }

    public void getAsync(String username, Consumer<Optional<GameProfile>> consumer) {
        if (this.executor == null) {
            throw new IllegalStateException("No executor");
        } else {
            CompletableFuture<Optional<GameProfile>> completableFuture = this.requests.get(username);
            if (completableFuture != null) {
                this.requests.put(username, completableFuture.whenCompleteAsync((profile, throwable) -> {
                    consumer.accept(profile);
                }, this.executor));
            } else {
                this.requests.put(username, CompletableFuture.supplyAsync(() -> {
                    return this.get(username);
                }, Util.backgroundExecutor()).whenCompleteAsync((profile, throwable) -> {
                    this.requests.remove(username);
                }, this.executor).whenCompleteAsync((profile, throwable) -> {
                    consumer.accept(profile);
                }, this.executor));
            }

        }
    }

    public Optional<GameProfile> get(UUID uuid) {
        GameProfileCache.GameProfileInfo gameProfileInfo = this.profilesByUUID.get(uuid);
        if (gameProfileInfo == null) {
            return Optional.empty();
        } else {
            gameProfileInfo.setLastAccess(this.getNextOperation());
            return Optional.of(gameProfileInfo.getProfile());
        }
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void clearExecutor() {
        this.executor = null;
    }

    private static DateFormat createDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    }

    public List<GameProfileCache.GameProfileInfo> load() {
        List<GameProfileCache.GameProfileInfo> list = Lists.newArrayList();

        try {
            Reader reader = Files.newReader(this.file, StandardCharsets.UTF_8);

            Object var9;
            label61: {
                try {
                    JsonArray jsonArray = this.gson.fromJson(reader, JsonArray.class);
                    if (jsonArray == null) {
                        var9 = list;
                        break label61;
                    }

                    DateFormat dateFormat = createDateFormat();
                    jsonArray.forEach((json) -> {
                        readGameProfile(json, dateFormat).ifPresent(list::add);
                    });
                } catch (Throwable var6) {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Throwable var5) {
                            var6.addSuppressed(var5);
                        }
                    }

                    throw var6;
                }

                if (reader != null) {
                    reader.close();
                }

                return list;
            }

            if (reader != null) {
                reader.close();
            }

            return (List<GameProfileCache.GameProfileInfo>)var9;
        } catch (FileNotFoundException var7) {
        } catch (JsonParseException | IOException var8) {
            LOGGER.warn("Failed to load profile cache {}", this.file, var8);
        }

        return list;
    }

    public void save() {
        JsonArray jsonArray = new JsonArray();
        DateFormat dateFormat = createDateFormat();
        this.getTopMRUProfiles(1000).forEach((entry) -> {
            jsonArray.add(writeGameProfile(entry, dateFormat));
        });
        String string = this.gson.toJson((JsonElement)jsonArray);

        try {
            Writer writer = Files.newWriter(this.file, StandardCharsets.UTF_8);

            try {
                writer.write(string);
            } catch (Throwable var8) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                    }
                }

                throw var8;
            }

            if (writer != null) {
                writer.close();
            }
        } catch (IOException var9) {
        }

    }

    private Stream<GameProfileCache.GameProfileInfo> getTopMRUProfiles(int limit) {
        return ImmutableList.copyOf(this.profilesByUUID.values()).stream().sorted(Comparator.comparing(GameProfileCache.GameProfileInfo::getLastAccess).reversed()).limit((long)limit);
    }

    private static JsonElement writeGameProfile(GameProfileCache.GameProfileInfo entry, DateFormat dateFormat) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", entry.getProfile().getName());
        UUID uUID = entry.getProfile().getId();
        jsonObject.addProperty("uuid", uUID == null ? "" : uUID.toString());
        jsonObject.addProperty("expiresOn", dateFormat.format(entry.getExpirationDate()));
        return jsonObject;
    }

    private static Optional<GameProfileCache.GameProfileInfo> readGameProfile(JsonElement json, DateFormat dateFormat) {
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonElement jsonElement = jsonObject.get("name");
            JsonElement jsonElement2 = jsonObject.get("uuid");
            JsonElement jsonElement3 = jsonObject.get("expiresOn");
            if (jsonElement != null && jsonElement2 != null) {
                String string = jsonElement2.getAsString();
                String string2 = jsonElement.getAsString();
                Date date = null;
                if (jsonElement3 != null) {
                    try {
                        date = dateFormat.parse(jsonElement3.getAsString());
                    } catch (ParseException var12) {
                    }
                }

                if (string2 != null && string != null && date != null) {
                    UUID uUID;
                    try {
                        uUID = UUID.fromString(string);
                    } catch (Throwable var11) {
                        return Optional.empty();
                    }

                    return Optional.of(new GameProfileCache.GameProfileInfo(new GameProfile(uUID, string2), date));
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    static class GameProfileInfo {
        private final GameProfile profile;
        final Date expirationDate;
        private volatile long lastAccess;

        GameProfileInfo(GameProfile profile, Date expirationDate) {
            this.profile = profile;
            this.expirationDate = expirationDate;
        }

        public GameProfile getProfile() {
            return this.profile;
        }

        public Date getExpirationDate() {
            return this.expirationDate;
        }

        public void setLastAccess(long lastAccessed) {
            this.lastAccess = lastAccessed;
        }

        public long getLastAccess() {
            return this.lastAccess;
        }
    }
}
