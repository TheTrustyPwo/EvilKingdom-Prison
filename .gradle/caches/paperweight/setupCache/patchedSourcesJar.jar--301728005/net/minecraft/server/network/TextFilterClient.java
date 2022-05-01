package net.minecraft.server.network;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.thread.ProcessorMailbox;
import org.slf4j.Logger;

public class TextFilterClient implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
    private static final ThreadFactory THREAD_FACTORY = (runnable) -> {
        Thread thread = new Thread(runnable);
        thread.setName("Chat-Filter-Worker-" + WORKER_COUNT.getAndIncrement());
        return thread;
    };
    private final URL chatEndpoint;
    final URL joinEndpoint;
    final URL leaveEndpoint;
    private final String authKey;
    private final int ruleId;
    private final String serverId;
    private final String roomId;
    final TextFilterClient.IgnoreStrategy chatIgnoreStrategy;
    final ExecutorService workerPool;

    private TextFilterClient(URL chatEndpoint, URL joinEndpoint, URL leaveEndpoint, String apiKey, int ruleId, String serverId, String roomId, TextFilterClient.IgnoreStrategy ignorer, int parallelism) {
        this.authKey = apiKey;
        this.ruleId = ruleId;
        this.serverId = serverId;
        this.roomId = roomId;
        this.chatIgnoreStrategy = ignorer;
        this.chatEndpoint = chatEndpoint;
        this.joinEndpoint = joinEndpoint;
        this.leaveEndpoint = leaveEndpoint;
        this.workerPool = Executors.newFixedThreadPool(parallelism, THREAD_FACTORY);
    }

    private static URL getEndpoint(URI root, @Nullable JsonObject endpoints, String key, String fallback) throws MalformedURLException {
        String string = endpoints != null ? GsonHelper.getAsString(endpoints, key, fallback) : fallback;
        return root.resolve("/" + string).toURL();
    }

    @Nullable
    public static TextFilterClient createFromConfig(String config) {
        if (Strings.isNullOrEmpty(config)) {
            return null;
        } else {
            try {
                JsonObject jsonObject = GsonHelper.parse(config);
                URI uRI = new URI(GsonHelper.getAsString(jsonObject, "apiServer"));
                String string = GsonHelper.getAsString(jsonObject, "apiKey");
                if (string.isEmpty()) {
                    throw new IllegalArgumentException("Missing API key");
                } else {
                    int i = GsonHelper.getAsInt(jsonObject, "ruleId", 1);
                    String string2 = GsonHelper.getAsString(jsonObject, "serverId", "");
                    String string3 = GsonHelper.getAsString(jsonObject, "roomId", "Java:Chat");
                    int j = GsonHelper.getAsInt(jsonObject, "hashesToDrop", -1);
                    int k = GsonHelper.getAsInt(jsonObject, "maxConcurrentRequests", 7);
                    JsonObject jsonObject2 = GsonHelper.getAsJsonObject(jsonObject, "endpoints", (JsonObject)null);
                    URL uRL = getEndpoint(uRI, jsonObject2, "chat", "v1/chat");
                    URL uRL2 = getEndpoint(uRI, jsonObject2, "join", "v1/join");
                    URL uRL3 = getEndpoint(uRI, jsonObject2, "leave", "v1/leave");
                    TextFilterClient.IgnoreStrategy ignoreStrategy = TextFilterClient.IgnoreStrategy.select(j);
                    return new TextFilterClient(uRL, uRL2, uRL3, Base64.getEncoder().encodeToString(string.getBytes(StandardCharsets.US_ASCII)), i, string2, string3, ignoreStrategy, k);
                }
            } catch (Exception var14) {
                LOGGER.warn("Failed to parse chat filter config {}", config, var14);
                return null;
            }
        }
    }

    void processJoinOrLeave(GameProfile gameProfile, URL endpoint, Executor executor) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("server", this.serverId);
        jsonObject.addProperty("room", this.roomId);
        jsonObject.addProperty("user_id", gameProfile.getId().toString());
        jsonObject.addProperty("user_display_name", gameProfile.getName());
        executor.execute(() -> {
            try {
                this.processRequest(jsonObject, endpoint);
            } catch (Exception var5) {
                LOGGER.warn("Failed to send join/leave packet to {} for player {}", endpoint, gameProfile, var5);
            }

        });
    }

    CompletableFuture<TextFilter.FilteredText> requestMessageProcessing(GameProfile gameProfile, String message, TextFilterClient.IgnoreStrategy ignorer, Executor executor) {
        if (message.isEmpty()) {
            return CompletableFuture.completedFuture(TextFilter.FilteredText.EMPTY);
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("rule", this.ruleId);
            jsonObject.addProperty("server", this.serverId);
            jsonObject.addProperty("room", this.roomId);
            jsonObject.addProperty("player", gameProfile.getId().toString());
            jsonObject.addProperty("player_display_name", gameProfile.getName());
            jsonObject.addProperty("text", message);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    JsonObject jsonObject2 = this.processRequestResponse(jsonObject, this.chatEndpoint);
                    boolean bl = GsonHelper.getAsBoolean(jsonObject2, "response", false);
                    if (bl) {
                        return TextFilter.FilteredText.passThrough(message);
                    } else {
                        String string2 = GsonHelper.getAsString(jsonObject2, "hashed", (String)null);
                        if (string2 == null) {
                            return TextFilter.FilteredText.fullyFiltered(message);
                        } else {
                            int i = GsonHelper.getAsJsonArray(jsonObject2, "hashes").size();
                            return ignorer.shouldIgnore(string2, i) ? TextFilter.FilteredText.fullyFiltered(message) : new TextFilter.FilteredText(message, string2);
                        }
                    }
                } catch (Exception var8) {
                    LOGGER.warn("Failed to validate message '{}'", message, var8);
                    return TextFilter.FilteredText.fullyFiltered(message);
                }
            }, executor);
        }
    }

    @Override
    public void close() {
        this.workerPool.shutdownNow();
    }

    private void drainStream(InputStream inputStream) throws IOException {
        byte[] bs = new byte[1024];

        while(inputStream.read(bs) != -1) {
        }

    }

    private JsonObject processRequestResponse(JsonObject payload, URL endpoint) throws IOException {
        HttpURLConnection httpURLConnection = this.makeRequest(payload, endpoint);
        InputStream inputStream = httpURLConnection.getInputStream();

        JsonObject var13;
        label89: {
            try {
                if (httpURLConnection.getResponseCode() == 204) {
                    var13 = new JsonObject();
                    break label89;
                }

                try {
                    var13 = Streams.parse(new JsonReader(new InputStreamReader(inputStream))).getAsJsonObject();
                } finally {
                    this.drainStream(inputStream);
                }
            } catch (Throwable var12) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var10) {
                        var12.addSuppressed(var10);
                    }
                }

                throw var12;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return var13;
        }

        if (inputStream != null) {
            inputStream.close();
        }

        return var13;
    }

    private void processRequest(JsonObject payload, URL endpoint) throws IOException {
        HttpURLConnection httpURLConnection = this.makeRequest(payload, endpoint);
        InputStream inputStream = httpURLConnection.getInputStream();

        try {
            this.drainStream(inputStream);
        } catch (Throwable var8) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }
            }

            throw var8;
        }

        if (inputStream != null) {
            inputStream.close();
        }

    }

    private HttpURLConnection makeRequest(JsonObject payload, URL endpoint) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection)endpoint.openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(2000);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setDoInput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestProperty("Authorization", "Basic " + this.authKey);
        httpURLConnection.setRequestProperty("User-Agent", "Minecraft server" + SharedConstants.getCurrentVersion().getName());
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpURLConnection.getOutputStream(), StandardCharsets.UTF_8);

        try {
            JsonWriter jsonWriter = new JsonWriter(outputStreamWriter);

            try {
                Streams.write(payload, jsonWriter);
            } catch (Throwable var10) {
                try {
                    jsonWriter.close();
                } catch (Throwable var9) {
                    var10.addSuppressed(var9);
                }

                throw var10;
            }

            jsonWriter.close();
        } catch (Throwable var11) {
            try {
                outputStreamWriter.close();
            } catch (Throwable var8) {
                var11.addSuppressed(var8);
            }

            throw var11;
        }

        outputStreamWriter.close();
        int i = httpURLConnection.getResponseCode();
        if (i >= 200 && i < 300) {
            return httpURLConnection;
        } else {
            throw new TextFilterClient.RequestFailedException(i + " " + httpURLConnection.getResponseMessage());
        }
    }

    public TextFilter createContext(GameProfile gameProfile) {
        return new TextFilterClient.PlayerContext(gameProfile);
    }

    @FunctionalInterface
    public interface IgnoreStrategy {
        TextFilterClient.IgnoreStrategy NEVER_IGNORE = (hashes, hashesSize) -> {
            return false;
        };
        TextFilterClient.IgnoreStrategy IGNORE_FULLY_FILTERED = (hashes, hashesSize) -> {
            return hashes.length() == hashesSize;
        };

        static TextFilterClient.IgnoreStrategy ignoreOverThreshold(int hashesToDrop) {
            return (hashes, hashesSize) -> {
                return hashesSize >= hashesToDrop;
            };
        }

        static TextFilterClient.IgnoreStrategy select(int hashesToDrop) {
            switch(hashesToDrop) {
            case -1:
                return NEVER_IGNORE;
            case 0:
                return IGNORE_FULLY_FILTERED;
            default:
                return ignoreOverThreshold(hashesToDrop);
            }
        }

        boolean shouldIgnore(String hashes, int hashesSize);
    }

    class PlayerContext implements TextFilter {
        private final GameProfile profile;
        private final Executor streamExecutor;

        PlayerContext(GameProfile gameProfile) {
            this.profile = gameProfile;
            ProcessorMailbox<Runnable> processorMailbox = ProcessorMailbox.create(TextFilterClient.this.workerPool, "chat stream for " + gameProfile.getName());
            this.streamExecutor = processorMailbox::tell;
        }

        @Override
        public void join() {
            TextFilterClient.this.processJoinOrLeave(this.profile, TextFilterClient.this.joinEndpoint, this.streamExecutor);
        }

        @Override
        public void leave() {
            TextFilterClient.this.processJoinOrLeave(this.profile, TextFilterClient.this.leaveEndpoint, this.streamExecutor);
        }

        @Override
        public CompletableFuture<List<TextFilter.FilteredText>> processMessageBundle(List<String> texts) {
            List<CompletableFuture<TextFilter.FilteredText>> list = texts.stream().map((text) -> {
                return TextFilterClient.this.requestMessageProcessing(this.profile, text, TextFilterClient.this.chatIgnoreStrategy, this.streamExecutor);
            }).collect(ImmutableList.toImmutableList());
            return Util.sequenceFailFast(list).exceptionally((throwable) -> {
                return ImmutableList.of();
            });
        }

        @Override
        public CompletableFuture<TextFilter.FilteredText> processStreamMessage(String text) {
            return TextFilterClient.this.requestMessageProcessing(this.profile, text, TextFilterClient.this.chatIgnoreStrategy, this.streamExecutor);
        }
    }

    public static class RequestFailedException extends RuntimeException {
        RequestFailedException(String message) {
            super(message);
        }
    }
}
