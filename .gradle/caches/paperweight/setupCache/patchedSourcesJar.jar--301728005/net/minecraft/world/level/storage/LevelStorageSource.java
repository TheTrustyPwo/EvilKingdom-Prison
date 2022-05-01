package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.nbt.visitors.SkipFields;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.DirectoryLock;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.slf4j.Logger;

public class LevelStorageSource {

    static final Logger LOGGER = LogUtils.getLogger();
    static final DateTimeFormatter FORMATTER = (new DateTimeFormatterBuilder()).appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral('_').appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral('-').appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral('-').appendValue(ChronoField.SECOND_OF_MINUTE, 2).toFormatter();
    private static final String ICON_FILENAME = "icon.png";
    private static final ImmutableList<String> OLD_SETTINGS_KEYS = ImmutableList.of("RandomSeed", "generatorName", "generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest");
    private static final String TAG_DATA = "Data";
    public final Path baseDir;
    private final Path backupDir;
    final DataFixer fixerUpper;

    public LevelStorageSource(Path savesDirectory, Path backupsDirectory, DataFixer dataFixer) {
        this.fixerUpper = dataFixer;

        try {
            Files.createDirectories(Files.exists(savesDirectory, new LinkOption[0]) ? savesDirectory.toRealPath() : savesDirectory);
        } catch (IOException ioexception) {
            throw new RuntimeException(ioexception);
        }

        this.baseDir = savesDirectory;
        this.backupDir = backupsDirectory;
    }

    public static LevelStorageSource createDefault(Path path) {
        return new LevelStorageSource(path, path.resolve("../backups"), DataFixers.getDataFixer());
    }

    private static <T> Pair<WorldGenSettings, Lifecycle> readWorldGenSettings(Dynamic<T> levelData, DataFixer dataFixer, int version) {
        Dynamic<T> dynamic1 = levelData.get("WorldGenSettings").orElseEmptyMap();
        UnmodifiableIterator unmodifiableiterator = LevelStorageSource.OLD_SETTINGS_KEYS.iterator();

        while (unmodifiableiterator.hasNext()) {
            String s = (String) unmodifiableiterator.next();
            Optional<? extends Dynamic<?>> optional = levelData.get(s).result();

            if (optional.isPresent()) {
                dynamic1 = dynamic1.set(s, (Dynamic) optional.get());
            }
        }

        Dynamic<T> dynamic2 = dataFixer.update(References.WORLD_GEN_SETTINGS, dynamic1, version, SharedConstants.getCurrentVersion().getWorldVersion());
        DataResult<WorldGenSettings> dataresult = WorldGenSettings.CODEC.parse(dynamic2);
        Logger logger = LevelStorageSource.LOGGER;

        Objects.requireNonNull(logger);
        return Pair.of((WorldGenSettings) dataresult.resultOrPartial(Util.prefix("WorldGenSettings: ", logger::error)).orElseGet(() -> {
            RegistryAccess iregistrycustom = RegistryAccess.readFromDisk(dynamic2);

            return WorldGenSettings.makeDefault(iregistrycustom);
        }), dataresult.lifecycle());
    }

    private static DataPackConfig readDataPackConfig(Dynamic<?> dynamic) {
        DataResult<DataPackConfig> dataresult = DataPackConfig.CODEC.parse(dynamic); // CraftBukkit - decompile error
        Logger logger = LevelStorageSource.LOGGER;

        Objects.requireNonNull(logger);
        return (DataPackConfig) dataresult.resultOrPartial(logger::error).orElse(DataPackConfig.DEFAULT);
    }

    public String getName() {
        return "Anvil";
    }

    public List<LevelSummary> getLevelList() throws LevelStorageException {
        if (!Files.isDirectory(this.baseDir, new LinkOption[0])) {
            throw new LevelStorageException((new TranslatableComponent("selectWorld.load_folder_access")).getString());
        } else {
            List<LevelSummary> list = Lists.newArrayList();
            File[] afile = this.baseDir.toFile().listFiles();
            File[] afile1 = afile;
            int i = afile.length;

            for (int j = 0; j < i; ++j) {
                File file = afile1[j];

                if (file.isDirectory()) {
                    boolean flag;

                    try {
                        flag = DirectoryLock.isLocked(file.toPath());
                    } catch (Exception exception) {
                        LevelStorageSource.LOGGER.warn("Failed to read {} lock", file, exception);
                        continue;
                    }

                    try {
                        LevelSummary worldinfo = (LevelSummary) this.readLevelData(file, this.levelSummaryReader(file, flag));

                        if (worldinfo != null) {
                            list.add(worldinfo);
                        }
                    } catch (OutOfMemoryError outofmemoryerror) {
                        MemoryReserve.release();
                        System.gc();
                        LevelStorageSource.LOGGER.error(LogUtils.FATAL_MARKER, "Ran out of memory trying to read summary of {}", file);
                        throw outofmemoryerror;
                    } catch (StackOverflowError stackoverflowerror) {
                        LevelStorageSource.LOGGER.error(LogUtils.FATAL_MARKER, "Ran out of stack trying to read summary of {}. Assuming corruption; attempting to restore from from level.dat_old.", file);
                        File file1 = new File(file, "level.dat");
                        File file2 = new File(file, "level.dat_old");
                        LocalDateTime localdatetime = LocalDateTime.now();
                        File file3 = new File(file, "level.dat_corrupted_" + localdatetime.format(LevelStorageSource.FORMATTER));

                        Util.safeReplaceOrMoveFile(file1, file2, file3, true);
                        throw stackoverflowerror;
                    }
                }
            }

            return list;
        }
    }

    private int getStorageVersion() {
        return 19133;
    }

    @Nullable
    <T> T readLevelData(File file, BiFunction<File, DataFixer, T> levelDataParser) {
        if (!file.exists()) {
            return null;
        } else {
            File file1 = new File(file, "level.dat");

            if (file1.exists()) {
                T t0 = levelDataParser.apply(file1, this.fixerUpper);

                if (t0 != null) {
                    return t0;
                }
            }

            file1 = new File(file, "level.dat_old");
            return file1.exists() ? levelDataParser.apply(file1, this.fixerUpper) : null;
        }
    }

    @Nullable
    private static DataPackConfig getDataPacks(File file, DataFixer dataFixer) {
        try {
            Tag nbtbase = LevelStorageSource.readLightweightData(file);

            if (nbtbase instanceof CompoundTag) {
                CompoundTag nbttagcompound = (CompoundTag) nbtbase;
                CompoundTag nbttagcompound1 = nbttagcompound.getCompound("Data");
                int i = nbttagcompound1.contains("DataVersion", 99) ? nbttagcompound1.getInt("DataVersion") : -1;
                Dynamic<Tag> dynamic = dataFixer.update(DataFixTypes.LEVEL.getType(), new Dynamic(NbtOps.INSTANCE, nbttagcompound1), i, SharedConstants.getCurrentVersion().getWorldVersion());

                return (DataPackConfig) dynamic.get("DataPacks").result().map(LevelStorageSource::readDataPackConfig).orElse(DataPackConfig.DEFAULT);
            }
        } catch (Exception exception) {
            LevelStorageSource.LOGGER.error("Exception reading {}", file, exception);
        }

        return null;
    }

    static BiFunction<File, DataFixer, PrimaryLevelData> getLevelData(DynamicOps<Tag> ops, DataPackConfig dataPackSettings, Lifecycle lifecycle) {
        return (file, datafixer) -> {
            try {
                CompoundTag nbttagcompound = NbtIo.readCompressed(file);
                CompoundTag nbttagcompound1 = nbttagcompound.getCompound("Data");
                CompoundTag nbttagcompound2 = nbttagcompound1.contains("Player", 10) ? nbttagcompound1.getCompound("Player") : null;

                nbttagcompound1.remove("Player");
                int i = nbttagcompound1.contains("DataVersion", 99) ? nbttagcompound1.getInt("DataVersion") : -1;
                Dynamic<Tag> dynamic = datafixer.update(DataFixTypes.LEVEL.getType(), new Dynamic(ops, nbttagcompound1), i, SharedConstants.getCurrentVersion().getWorldVersion());
                Pair<WorldGenSettings, Lifecycle> pair = LevelStorageSource.readWorldGenSettings(dynamic, datafixer, i);
                LevelVersion levelversion = LevelVersion.parse(dynamic);
                LevelSettings worldsettings = LevelSettings.parse(dynamic, dataPackSettings);
                Lifecycle lifecycle1 = ((Lifecycle) pair.getSecond()).add(lifecycle);

                // CraftBukkit start - Add PDC to world
                PrimaryLevelData worldDataServer = PrimaryLevelData.parse(dynamic, datafixer, i, nbttagcompound2, worldsettings, levelversion, (WorldGenSettings) pair.getFirst(), lifecycle1);
                worldDataServer.pdc = nbttagcompound1.get("BukkitValues");
                return worldDataServer;
                // CraftBukkit end
            } catch (Exception exception) {
                LevelStorageSource.LOGGER.error("Exception reading {}", file, exception);
                return null;
            }
        };
    }

    BiFunction<File, DataFixer, LevelSummary> levelSummaryReader(File file, boolean locked) {
        return (file1, datafixer) -> {
            try {
                Tag nbtbase = LevelStorageSource.readLightweightData(file1);

                if (nbtbase instanceof CompoundTag) {
                    CompoundTag nbttagcompound = (CompoundTag) nbtbase;
                    CompoundTag nbttagcompound1 = nbttagcompound.getCompound("Data");
                    int i = nbttagcompound1.contains("DataVersion", 99) ? nbttagcompound1.getInt("DataVersion") : -1;
                    Dynamic<Tag> dynamic = datafixer.update(DataFixTypes.LEVEL.getType(), new Dynamic(NbtOps.INSTANCE, nbttagcompound1), i, SharedConstants.getCurrentVersion().getWorldVersion());
                    LevelVersion levelversion = LevelVersion.parse(dynamic);
                    int j = levelversion.levelDataVersion();

                    if (j == 19132 || j == 19133) {
                        boolean flag1 = j != this.getStorageVersion();
                        File file2 = new File(file, "icon.png");
                        DataPackConfig datapackconfiguration = (DataPackConfig) dynamic.get("DataPacks").result().map(LevelStorageSource::readDataPackConfig).orElse(DataPackConfig.DEFAULT);
                        LevelSettings worldsettings = LevelSettings.parse(dynamic, datapackconfiguration);

                        return new LevelSummary(worldsettings, levelversion, file.getName(), flag1, locked, file2);
                    }
                } else {
                    LevelStorageSource.LOGGER.warn("Invalid root tag in {}", file1);
                }

                return null;
            } catch (Exception exception) {
                LevelStorageSource.LOGGER.error("Exception reading {}", file1, exception);
                return null;
            }
        };
    }

    @Nullable
    private static Tag readLightweightData(File file) throws IOException {
        SkipFields skipfields = new SkipFields(new FieldSelector[]{new FieldSelector("Data", CompoundTag.TYPE, "Player"), new FieldSelector("Data", CompoundTag.TYPE, "WorldGenSettings")});

        NbtIo.parseCompressed(file, skipfields);
        return skipfields.getResult();
    }

    public boolean isNewLevelIdAcceptable(String name) {
        try {
            Path path = this.baseDir.resolve(name);

            Files.createDirectory(path);
            Files.deleteIfExists(path);
            return true;
        } catch (IOException ioexception) {
            return false;
        }
    }

    public boolean levelExists(String name) {
        return Files.isDirectory(this.baseDir.resolve(name), new LinkOption[0]);
    }

    public Path getBaseDir() {
        return this.baseDir;
    }

    public Path getBackupPath() {
        return this.backupDir;
    }

    // CraftBukkit start
    public LevelStorageSource.LevelStorageAccess createAccess(String s, ResourceKey<LevelStem> dimensionType) throws IOException {
        return new LevelStorageSource.LevelStorageAccess(s, dimensionType);
    }

    public static Path getStorageFolder(Path path, ResourceKey<LevelStem> dimensionType) {
        if (dimensionType == LevelStem.OVERWORLD) {
            return path;
        } else if (dimensionType == LevelStem.NETHER) {
            return path.resolve("DIM-1");
        } else if (dimensionType == LevelStem.END) {
            return path.resolve("DIM1");
        } else {
            return path.resolve("dimensions").resolve(dimensionType.location().getNamespace()).resolve(dimensionType.location().getPath());
        }
    }
    // CraftBukkit end

    public class LevelStorageAccess implements AutoCloseable {

        final DirectoryLock lock;
        public final Path levelPath;
        private final String levelId;
        private final Map<LevelResource, Path> resources = Maps.newHashMap();
        // CraftBukkit start
        public final ResourceKey<LevelStem> dimensionType;

        public LevelStorageAccess(String s, ResourceKey<LevelStem> dimensionType) throws IOException {
            this.dimensionType = dimensionType;
            // CraftBukkit end
            this.levelId = s;
            this.levelPath = LevelStorageSource.this.baseDir.resolve(s);
            this.lock = DirectoryLock.create(this.levelPath);
        }

        public String getLevelId() {
            return this.levelId;
        }

        public Path getLevelPath(LevelResource savePath) {
            return (Path) this.resources.computeIfAbsent(savePath, (savedfile1) -> {
                return this.levelPath.resolve(savedfile1.getId());
            });
        }

        public Path getDimensionPath(ResourceKey<Level> key) {
            return LevelStorageSource.getStorageFolder(this.levelPath, this.dimensionType); // CraftBukkit
        }

        private void checkLock() {
            if (!this.lock.isValid()) {
                throw new IllegalStateException("Lock is no longer valid");
            }
        }

        public PlayerDataStorage createPlayerStorage() {
            this.checkLock();
            return new PlayerDataStorage(this, LevelStorageSource.this.fixerUpper);
        }

        @Nullable
        public LevelSummary getSummary() {
            this.checkLock();
            return (LevelSummary) LevelStorageSource.this.readLevelData(this.levelPath.toFile(), LevelStorageSource.this.levelSummaryReader(this.levelPath.toFile(), false));
        }

        @Nullable
        public WorldData getDataTag(DynamicOps<Tag> ops, DataPackConfig dataPackSettings, Lifecycle lifecycle) {
            this.checkLock();
            return (WorldData) LevelStorageSource.this.readLevelData(this.levelPath.toFile(), LevelStorageSource.getLevelData(ops, dataPackSettings, lifecycle));
        }

        @Nullable
        public DataPackConfig getDataPacks() {
            this.checkLock();
            return (DataPackConfig) LevelStorageSource.this.readLevelData(this.levelPath.toFile(), LevelStorageSource::getDataPacks);
        }

        public void saveDataTag(RegistryAccess registryManager, WorldData saveProperties) {
            this.saveDataTag(registryManager, saveProperties, (CompoundTag) null);
        }

        public void saveDataTag(RegistryAccess registryManager, WorldData saveProperties, @Nullable CompoundTag nbt) {
            File file = this.levelPath.toFile();
            CompoundTag nbttagcompound1 = saveProperties.createTag(registryManager, nbt);
            CompoundTag nbttagcompound2 = new CompoundTag();

            nbttagcompound2.put("Data", nbttagcompound1);

            try {
                File file1 = File.createTempFile("level", ".dat", file);

                NbtIo.writeCompressed(nbttagcompound2, file1);
                File file2 = new File(file, "level.dat_old");
                File file3 = new File(file, "level.dat");

                Util.safeReplaceFile(file3, file1, file2);
            } catch (Exception exception) {
                LevelStorageSource.LOGGER.error("Failed to save level {}", file, exception);
            }

        }

        public Optional<Path> getIconFile() {
            return !this.lock.isValid() ? Optional.empty() : Optional.of(this.levelPath.resolve("icon.png"));
        }

        public void deleteLevel() throws IOException {
            this.checkLock();
            final Path path = this.levelPath.resolve("session.lock");

            LevelStorageSource.LOGGER.info("Deleting level {}", this.levelId);
            int i = 1;

            while (i <= 5) {
                LevelStorageSource.LOGGER.info("Attempt {}...", i);

                try {
                    Files.walkFileTree(this.levelPath, new SimpleFileVisitor<Path>() {
                        public FileVisitResult visitFile(Path path1, BasicFileAttributes basicfileattributes) throws IOException {
                            if (!path1.equals(path)) {
                                LevelStorageSource.LOGGER.debug("Deleting {}", path1);
                                Files.delete(path1);
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        public FileVisitResult postVisitDirectory(Path path1, IOException ioexception) throws IOException {
                            if (ioexception != null) {
                                throw ioexception;
                            } else {
                                if (path1.equals(LevelStorageAccess.this.levelPath)) {
                                    LevelStorageAccess.this.lock.close();
                                    Files.deleteIfExists(path);
                                }

                                Files.delete(path1);
                                return FileVisitResult.CONTINUE;
                            }
                        }
                    });
                    break;
                } catch (IOException ioexception) {
                    if (i >= 5) {
                        throw ioexception;
                    }

                    LevelStorageSource.LOGGER.warn("Failed to delete {}", this.levelPath, ioexception);

                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException interruptedexception) {
                        ;
                    }

                    ++i;
                }
            }

        }

        public void renameLevel(String name) throws IOException {
            this.checkLock();
            File file = new File(LevelStorageSource.this.baseDir.toFile(), this.levelId);

            if (file.exists()) {
                File file1 = new File(file, "level.dat");

                if (file1.exists()) {
                    CompoundTag nbttagcompound = NbtIo.readCompressed(file1);
                    CompoundTag nbttagcompound1 = nbttagcompound.getCompound("Data");

                    nbttagcompound1.putString("LevelName", name);
                    NbtIo.writeCompressed(nbttagcompound, file1);
                }

            }
        }

        public long makeWorldBackup() throws IOException {
            this.checkLock();
            String s = LocalDateTime.now().format(LevelStorageSource.FORMATTER);
            String s1 = s + "_" + this.levelId;
            Path path = LevelStorageSource.this.getBackupPath();

            try {
                Files.createDirectories(Files.exists(path, new LinkOption[0]) ? path.toRealPath() : path);
            } catch (IOException ioexception) {
                throw new RuntimeException(ioexception);
            }

            Path path1 = path.resolve(FileUtil.findAvailableName(path, s1, ".zip"));
            final ZipOutputStream zipoutputstream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(path1)));

            try {
                final Path path2 = Paths.get(this.levelId);

                Files.walkFileTree(this.levelPath, new SimpleFileVisitor<Path>() {
                    public FileVisitResult visitFile(Path path3, BasicFileAttributes basicfileattributes) throws IOException {
                        if (path3.endsWith("session.lock")) {
                            return FileVisitResult.CONTINUE;
                        } else {
                            String s2 = path2.resolve(LevelStorageAccess.this.levelPath.relativize(path3)).toString().replace('\\', '/');
                            ZipEntry zipentry = new ZipEntry(s2);

                            zipoutputstream.putNextEntry(zipentry);
                            com.google.common.io.Files.asByteSource(path3.toFile()).copyTo(zipoutputstream);
                            zipoutputstream.closeEntry();
                            return FileVisitResult.CONTINUE;
                        }
                    }
                });
            } catch (Throwable throwable) {
                try {
                    zipoutputstream.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }

                throw throwable;
            }

            zipoutputstream.close();
            return Files.size(path1);
        }

        public void close() throws IOException {
            this.lock.close();
        }
    }
}
