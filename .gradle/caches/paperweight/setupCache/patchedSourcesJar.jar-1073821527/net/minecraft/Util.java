package net.minecraft;

import com.google.common.base.Ticker;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.Hash.Strategy;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class Util {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_MAX_THREADS = 255;
    private static final String MAX_THREADS_SYSTEM_PROPERTY = "max.bg.threads";
    private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
    private static final ExecutorService BOOTSTRAP_EXECUTOR = makeExecutor("Bootstrap");
    private static final ExecutorService BACKGROUND_EXECUTOR = makeExecutor("Main");
    private static final ExecutorService IO_POOL = makeIoExecutor();
    public static LongSupplier timeSource = System::nanoTime;
    public static final Ticker TICKER = new Ticker() {
        @Override
        public long read() {
            return Util.timeSource.getAsLong();
        }
    };
    public static final UUID NIL_UUID = new UUID(0L, 0L);
    public static final FileSystemProvider ZIP_FILE_SYSTEM_PROVIDER = FileSystemProvider.installedProviders().stream().filter((fileSystemProvider) -> {
        return fileSystemProvider.getScheme().equalsIgnoreCase("jar");
    }).findFirst().orElseThrow(() -> {
        return new IllegalStateException("No jar file system provider found");
    });
    private static Consumer<String> thePauser = (message) -> {
    };

    public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
        return Collectors.toMap(Entry::getKey, Entry::getValue);
    }

    public static <T extends Comparable<T>> String getPropertyName(Property<T> property, Object value) {
        return property.getName((T)(value));
    }

    public static String makeDescriptionId(String type, @Nullable ResourceLocation id) {
        return id == null ? type + ".unregistered_sadface" : type + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public static long getMillis() {
        return getNanos() / 1000000L;
    }

    public static long getNanos() {
        return timeSource.getAsLong();
    }

    public static long getEpochMillis() {
        return Instant.now().toEpochMilli();
    }

    private static ExecutorService makeExecutor(String name) {
        int i = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, getMaxThreads());
        ExecutorService executorService;
        if (i <= 0) {
            executorService = MoreExecutors.newDirectExecutorService();
        } else {
            executorService = new ForkJoinPool(i, (forkJoinPool) -> {
                ForkJoinWorkerThread forkJoinWorkerThread = new ForkJoinWorkerThread(forkJoinPool) {
                    @Override
                    protected void onTermination(Throwable throwable) {
                        if (throwable != null) {
                            Util.LOGGER.warn("{} died", this.getName(), throwable);
                        } else {
                            Util.LOGGER.debug("{} shutdown", (Object)this.getName());
                        }

                        super.onTermination(throwable);
                    }
                };
                forkJoinWorkerThread.setName("Worker-" + name + "-" + WORKER_COUNT.getAndIncrement());
                return forkJoinWorkerThread;
            }, Util::onThreadException, true);
        }

        return executorService;
    }

    private static int getMaxThreads() {
        String string = System.getProperty("max.bg.threads");
        if (string != null) {
            try {
                int i = Integer.parseInt(string);
                if (i >= 1 && i <= 255) {
                    return i;
                }

                LOGGER.error("Wrong {} property value '{}'. Should be an integer value between 1 and {}.", "max.bg.threads", string, 255);
            } catch (NumberFormatException var2) {
                LOGGER.error("Could not parse {} property value '{}'. Should be an integer value between 1 and {}.", "max.bg.threads", string, 255);
            }
        }

        return 255;
    }

    public static ExecutorService bootstrapExecutor() {
        return BOOTSTRAP_EXECUTOR;
    }

    public static ExecutorService backgroundExecutor() {
        return BACKGROUND_EXECUTOR;
    }

    public static ExecutorService ioPool() {
        return IO_POOL;
    }

    public static void shutdownExecutors() {
        shutdownExecutor(BACKGROUND_EXECUTOR);
        shutdownExecutor(IO_POOL);
    }

    private static void shutdownExecutor(ExecutorService service) {
        service.shutdown();

        boolean bl;
        try {
            bl = service.awaitTermination(3L, TimeUnit.SECONDS);
        } catch (InterruptedException var3) {
            bl = false;
        }

        if (!bl) {
            service.shutdownNow();
        }

    }

    private static ExecutorService makeIoExecutor() {
        return Executors.newCachedThreadPool((runnable) -> {
            Thread thread = new Thread(runnable);
            thread.setName("IO-Worker-" + WORKER_COUNT.getAndIncrement());
            thread.setUncaughtExceptionHandler(Util::onThreadException);
            return thread;
        });
    }

    public static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        completableFuture.completeExceptionally(throwable);
        return completableFuture;
    }

    public static void throwAsRuntime(Throwable t) {
        throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
    }

    public static void onThreadException(Thread thread, Throwable t) {
        pauseInIde(t);
        if (t instanceof CompletionException) {
            t = t.getCause();
        }

        if (t instanceof ReportedException) {
            Bootstrap.realStdoutPrintln(((ReportedException)t).getReport().getFriendlyReport());
            System.exit(-1);
        }

        LOGGER.error(String.format("Caught exception in thread %s", thread), t);
    }

    @Nullable
    public static Type<?> fetchChoiceType(TypeReference typeReference, String id) {
        return !SharedConstants.CHECK_DATA_FIXER_SCHEMA ? null : doFetchChoiceType(typeReference, id);
    }

    @Nullable
    private static Type<?> doFetchChoiceType(TypeReference typeReference, String id) {
        Type<?> type = null;

        try {
            type = DataFixers.getDataFixer().getSchema(DataFixUtils.makeKey(SharedConstants.getCurrentVersion().getWorldVersion())).getChoiceType(typeReference, id);
        } catch (IllegalArgumentException var4) {
            LOGGER.error("No data fixer registered for {}", (Object)id);
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                throw var4;
            }
        }

        return type;
    }

    public static Runnable wrapThreadWithTaskName(String activeThreadName, Runnable task) {
        return SharedConstants.IS_RUNNING_IN_IDE ? () -> {
            Thread thread = Thread.currentThread();
            String string2 = thread.getName();
            thread.setName(activeThreadName);

            try {
                task.run();
            } finally {
                thread.setName(string2);
            }

        } : task;
    }

    public static <V> Supplier<V> wrapThreadWithTaskName(String activeThreadName, Supplier<V> supplier) {
        return SharedConstants.IS_RUNNING_IN_IDE ? () -> {
            Thread thread = Thread.currentThread();
            String string2 = thread.getName();
            thread.setName(activeThreadName);

            Object var4;
            try {
                var4 = supplier.get();
            } finally {
                thread.setName(string2);
            }

            return (V)var4;
        } : supplier;
    }

    public static Util.OS getPlatform() {
        String string = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (string.contains("win")) {
            return Util.OS.WINDOWS;
        } else if (string.contains("mac")) {
            return Util.OS.OSX;
        } else if (string.contains("solaris")) {
            return Util.OS.SOLARIS;
        } else if (string.contains("sunos")) {
            return Util.OS.SOLARIS;
        } else if (string.contains("linux")) {
            return Util.OS.LINUX;
        } else {
            return string.contains("unix") ? Util.OS.LINUX : Util.OS.UNKNOWN;
        }
    }

    public static Stream<String> getVmArguments() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMXBean.getInputArguments().stream().filter((runtimeArg) -> {
            return runtimeArg.startsWith("-X");
        });
    }

    public static <T> T lastOf(List<T> list) {
        return list.get(list.size() - 1);
    }

    public static <T> T findNextInIterable(Iterable<T> iterable, @Nullable T object) {
        Iterator<T> iterator = iterable.iterator();
        T object2 = iterator.next();
        if (object != null) {
            T object3 = object2;

            while(object3 != object) {
                if (iterator.hasNext()) {
                    object3 = iterator.next();
                }
            }

            if (iterator.hasNext()) {
                return iterator.next();
            }
        }

        return object2;
    }

    public static <T> T findPreviousInIterable(Iterable<T> iterable, @Nullable T object) {
        Iterator<T> iterator = iterable.iterator();

        T object2;
        T object3;
        for(object2 = null; iterator.hasNext(); object2 = object3) {
            object3 = iterator.next();
            if (object3 == object) {
                if (object2 == null) {
                    object2 = (T)(iterator.hasNext() ? Iterators.getLast(iterator) : object);
                }
                break;
            }
        }

        return object2;
    }

    public static <T> T make(Supplier<T> factory) {
        return factory.get();
    }

    public static <T> T make(T object, Consumer<T> initializer) {
        initializer.accept(object);
        return object;
    }

    public static <K> Strategy<K> identityStrategy() {
        return Util.IdentityStrategy.INSTANCE;
    }

    public static <V> CompletableFuture<List<V>> sequence(List<? extends CompletableFuture<V>> futures) {
        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        } else if (futures.size() == 1) {
            return futures.get(0).thenApply(List::of);
        } else {
            CompletableFuture<Void> completableFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            return completableFuture.thenApply((void_) -> {
                return futures.stream().map(CompletableFuture::join).toList();
            });
        }
    }

    public static <V> CompletableFuture<List<V>> sequenceFailFast(List<? extends CompletableFuture<? extends V>> futures) {
        List<V> list = Lists.newArrayListWithCapacity(futures.size());
        CompletableFuture<?>[] completableFutures = new CompletableFuture[futures.size()];
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        futures.forEach((future) -> {
            int i = list.size();
            list.add((V)null);
            completableFutures[i] = future.whenComplete((object, throwable) -> {
                if (throwable != null) {
                    completableFuture.completeExceptionally(throwable);
                } else {
                    list.set(i, object);
                }

            });
        });
        return CompletableFuture.allOf(completableFutures).applyToEither(completableFuture, (void_) -> {
            return list;
        });
    }

    public static <T> Optional<T> ifElse(Optional<T> optional, Consumer<T> presentAction, Runnable elseAction) {
        if (optional.isPresent()) {
            presentAction.accept(optional.get());
        } else {
            elseAction.run();
        }

        return optional;
    }

    public static Runnable name(Runnable runnable, Supplier<String> messageSupplier) {
        return runnable;
    }

    public static void logAndPauseIfInIde(String message) {
        LOGGER.error(message);
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            doPause(message);
        }

    }

    public static void logAndPauseIfInIde(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            doPause(message);
        }

    }

    public static <T extends Throwable> T pauseInIde(T t) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            LOGGER.error("Trying to throw a fatal exception, pausing in IDE", t);
            doPause(t.getMessage());
        }

        return t;
    }

    public static void setPause(Consumer<String> missingBreakpointHandler) {
        thePauser = missingBreakpointHandler;
    }

    private static void doPause(String message) {
        Instant instant = Instant.now();
        LOGGER.warn("Did you remember to set a breakpoint here?");
        boolean bl = Duration.between(instant, Instant.now()).toMillis() > 500L;
        if (!bl) {
            thePauser.accept(message);
        }

    }

    public static String describeError(Throwable t) {
        if (t.getCause() != null) {
            return describeError(t.getCause());
        } else {
            return t.getMessage() != null ? t.getMessage() : t.toString();
        }
    }

    public static <T> T getRandom(T[] array, Random random) {
        return array[random.nextInt(array.length)];
    }

    public static int getRandom(int[] array, Random random) {
        return array[random.nextInt(array.length)];
    }

    public static <T> T getRandom(List<T> list, Random random) {
        return list.get(random.nextInt(list.size()));
    }

    public static <T> Optional<T> getRandomSafe(List<T> list, Random random) {
        return list.isEmpty() ? Optional.empty() : Optional.of(getRandom(list, random));
    }

    private static BooleanSupplier createRenamer(Path src, Path dest) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                try {
                    Files.move(src, dest);
                    return true;
                } catch (IOException var2) {
                    Util.LOGGER.error("Failed to rename", (Throwable)var2);
                    return false;
                }
            }

            @Override
            public String toString() {
                return "rename " + src + " to " + dest;
            }
        };
    }

    private static BooleanSupplier createDeleter(Path path) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                try {
                    Files.deleteIfExists(path);
                    return true;
                } catch (IOException var2) {
                    Util.LOGGER.warn("Failed to delete", (Throwable)var2);
                    return false;
                }
            }

            @Override
            public String toString() {
                return "delete old " + path;
            }
        };
    }

    private static BooleanSupplier createFileDeletedCheck(Path path) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return !Files.exists(path);
            }

            @Override
            public String toString() {
                return "verify that " + path + " is deleted";
            }
        };
    }

    private static BooleanSupplier createFileCreatedCheck(Path path) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return Files.isRegularFile(path);
            }

            @Override
            public String toString() {
                return "verify that " + path + " is present";
            }
        };
    }

    private static boolean executeInSequence(BooleanSupplier... tasks) {
        for(BooleanSupplier booleanSupplier : tasks) {
            if (!booleanSupplier.getAsBoolean()) {
                LOGGER.warn("Failed to execute {}", (Object)booleanSupplier);
                return false;
            }
        }

        return true;
    }

    private static boolean runWithRetries(int retries, String taskName, BooleanSupplier... tasks) {
        for(int i = 0; i < retries; ++i) {
            if (executeInSequence(tasks)) {
                return true;
            }

            LOGGER.error("Failed to {}, retrying {}/{}", taskName, i, retries);
        }

        LOGGER.error("Failed to {}, aborting, progress might be lost", (Object)taskName);
        return false;
    }

    public static void safeReplaceFile(File current, File newFile, File backup) {
        safeReplaceFile(current.toPath(), newFile.toPath(), backup.toPath());
    }

    public static void safeReplaceFile(Path current, Path newPath, Path backup) {
        safeReplaceOrMoveFile(current, newPath, backup, false);
    }

    public static void safeReplaceOrMoveFile(File current, File newPath, File backup, boolean noRestoreOnFail) {
        safeReplaceOrMoveFile(current.toPath(), newPath.toPath(), backup.toPath(), noRestoreOnFail);
    }

    public static void safeReplaceOrMoveFile(Path current, Path newPath, Path backup, boolean noRestoreOnFail) {
        int i = 10;
        if (!Files.exists(current) || runWithRetries(10, "create backup " + backup, createDeleter(backup), createRenamer(current, backup), createFileCreatedCheck(backup))) {
            if (runWithRetries(10, "remove old " + current, createDeleter(current), createFileDeletedCheck(current))) {
                if (!runWithRetries(10, "replace " + current + " with " + newPath, createRenamer(newPath, current), createFileCreatedCheck(current)) && !noRestoreOnFail) {
                    runWithRetries(10, "restore " + current + " from " + backup, createRenamer(backup, current), createFileCreatedCheck(current));
                }

            }
        }
    }

    public static int offsetByCodepoints(String string, int cursor, int delta) {
        int i = string.length();
        if (delta >= 0) {
            for(int j = 0; cursor < i && j < delta; ++j) {
                if (Character.isHighSurrogate(string.charAt(cursor++)) && cursor < i && Character.isLowSurrogate(string.charAt(cursor))) {
                    ++cursor;
                }
            }
        } else {
            for(int k = delta; cursor > 0 && k < 0; ++k) {
                --cursor;
                if (Character.isLowSurrogate(string.charAt(cursor)) && cursor > 0 && Character.isHighSurrogate(string.charAt(cursor - 1))) {
                    --cursor;
                }
            }
        }

        return cursor;
    }

    public static Consumer<String> prefix(String prefix, Consumer<String> consumer) {
        return (string) -> {
            consumer.accept(prefix + string);
        };
    }

    public static DataResult<int[]> fixedSize(IntStream stream, int length) {
        int[] is = stream.limit((long)(length + 1)).toArray();
        if (is.length != length) {
            String string = "Input is not a list of " + length + " ints";
            return is.length >= length ? DataResult.error(string, Arrays.copyOf(is, length)) : DataResult.error(string);
        } else {
            return DataResult.success(is);
        }
    }

    public static <T> DataResult<List<T>> fixedSize(List<T> list, int length) {
        if (list.size() != length) {
            String string = "Input is not a list of " + length + " elements";
            return list.size() >= length ? DataResult.error(string, list.subList(0, length)) : DataResult.error(string);
        } else {
            return DataResult.success(list);
        }
    }

    public static void startTimerHackThread() {
        Thread thread = new Thread("Timer hack thread") {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(2147483647L);
                    } catch (InterruptedException var2) {
                        Util.LOGGER.warn("Timer hack thread interrupted, that really should not happen");
                        return;
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    public static void copyBetweenDirs(Path src, Path dest, Path toCopy) throws IOException {
        Path path = src.relativize(toCopy);
        Path path2 = dest.resolve(path);
        Files.copy(toCopy, path2);
    }

    public static String sanitizeName(String string, CharPredicate predicate) {
        return string.toLowerCase(Locale.ROOT).chars().mapToObj((charCode) -> {
            return predicate.test((char)charCode) ? Character.toString((char)charCode) : "_";
        }).collect(Collectors.joining());
    }

    public static <T, R> Function<T, R> memoize(Function<T, R> function) {
        return new Function<T, R>() {
            private final Map<T, R> cache = Maps.newHashMap();

            @Override
            public R apply(T object) {
                return this.cache.computeIfAbsent(object, function);
            }

            @Override
            public String toString() {
                return "memoize/1[function=" + function + ", size=" + this.cache.size() + "]";
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R> memoize(BiFunction<T, U, R> biFunction) {
        return new BiFunction<T, U, R>() {
            private final Map<Pair<T, U>, R> cache = Maps.newHashMap();

            @Override
            public R apply(T object, U object2) {
                return this.cache.computeIfAbsent(Pair.of(object, object2), (pair) -> {
                    return biFunction.apply(pair.getFirst(), pair.getSecond());
                });
            }

            @Override
            public String toString() {
                return "memoize/2[function=" + biFunction + ", size=" + this.cache.size() + "]";
            }
        };
    }

    static enum IdentityStrategy implements Strategy<Object> {
        INSTANCE;

        @Override
        public int hashCode(Object object) {
            return System.identityHashCode(object);
        }

        @Override
        public boolean equals(Object object, Object object2) {
            return object == object2;
        }
    }

    public static enum OS {
        LINUX("linux"),
        SOLARIS("solaris"),
        WINDOWS("windows") {
            @Override
            protected String[] getOpenUrlArguments(URL url) {
                return new String[]{"rundll32", "url.dll,FileProtocolHandler", url.toString()};
            }
        },
        OSX("mac") {
            @Override
            protected String[] getOpenUrlArguments(URL url) {
                return new String[]{"open", url.toString()};
            }
        },
        UNKNOWN("unknown");

        private final String telemetryName;

        OS(String name) {
            this.telemetryName = name;
        }

        public void openUrl(URL url) {
            try {
                Process process = AccessController.doPrivileged((PrivilegedExceptionAction<Process>)(() -> {
                    return Runtime.getRuntime().exec(this.getOpenUrlArguments(url));
                }));

                for(String string : IOUtils.readLines(process.getErrorStream())) {
                    Util.LOGGER.error(string);
                }

                process.getInputStream().close();
                process.getErrorStream().close();
                process.getOutputStream().close();
            } catch (IOException | PrivilegedActionException var5) {
                Util.LOGGER.error("Couldn't open url '{}'", url, var5);
            }

        }

        public void openUri(URI uri) {
            try {
                this.openUrl(uri.toURL());
            } catch (MalformedURLException var3) {
                Util.LOGGER.error("Couldn't open uri '{}'", uri, var3);
            }

        }

        public void openFile(File file) {
            try {
                this.openUrl(file.toURI().toURL());
            } catch (MalformedURLException var3) {
                Util.LOGGER.error("Couldn't open file '{}'", file, var3);
            }

        }

        protected String[] getOpenUrlArguments(URL url) {
            String string = url.toString();
            if ("file".equals(url.getProtocol())) {
                string = string.replace("file:", "file://");
            }

            return new String[]{"xdg-open", string};
        }

        public void openUri(String uri) {
            try {
                this.openUrl((new URI(uri)).toURL());
            } catch (MalformedURLException | IllegalArgumentException | URISyntaxException var3) {
                Util.LOGGER.error("Couldn't open uri '{}'", uri, var3);
            }

        }

        public String telemetryName() {
            return this.telemetryName;
        }
    }
}
