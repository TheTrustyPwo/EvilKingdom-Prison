package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class DimensionDataStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    public final Map<String, SavedData> cache = Maps.newHashMap();
    private final DataFixer fixerUpper;
    private final File dataFolder;

    public DimensionDataStorage(File directory, DataFixer dataFixer) {
        this.fixerUpper = dataFixer;
        this.dataFolder = directory;
    }

    private File getDataFile(String id) {
        return new File(this.dataFolder, id + ".dat");
    }

    public <T extends SavedData> T computeIfAbsent(Function<CompoundTag, T> readFunction, Supplier<T> supplier, String id) {
        T savedData = this.get(readFunction, id);
        if (savedData != null) {
            return savedData;
        } else {
            T savedData2 = supplier.get();
            this.set(id, savedData2);
            return savedData2;
        }
    }

    @Nullable
    public <T extends SavedData> T get(Function<CompoundTag, T> readFunction, String id) {
        SavedData savedData = this.cache.get(id);
        if (savedData == null && !this.cache.containsKey(id)) {
            savedData = this.readSavedData(readFunction, id);
            this.cache.put(id, savedData);
        }

        return (T)savedData;
    }

    @Nullable
    private <T extends SavedData> T readSavedData(Function<CompoundTag, T> readFunction, String id) {
        try {
            File file = this.getDataFile(id);
            if (file.exists()) {
                CompoundTag compoundTag = this.readTagFromDisk(id, SharedConstants.getCurrentVersion().getWorldVersion());
                return readFunction.apply(compoundTag.getCompound("data"));
            }
        } catch (Exception var5) {
            LOGGER.error("Error loading saved data: {}", id, var5);
        }

        return (T)null;
    }

    public void set(String id, SavedData state) {
        this.cache.put(id, state);
    }

    public CompoundTag readTagFromDisk(String id, int dataVersion) throws IOException {
        File file = this.getDataFile(id);
        FileInputStream fileInputStream = new FileInputStream(file);

        CompoundTag var8;
        try {
            PushbackInputStream pushbackInputStream = new PushbackInputStream(fileInputStream, 2);

            try {
                CompoundTag compoundTag;
                if (this.isGzip(pushbackInputStream)) {
                    compoundTag = NbtIo.readCompressed(pushbackInputStream);
                } else {
                    DataInputStream dataInputStream = new DataInputStream(pushbackInputStream);

                    try {
                        compoundTag = NbtIo.read(dataInputStream);
                    } catch (Throwable var13) {
                        try {
                            dataInputStream.close();
                        } catch (Throwable var12) {
                            var13.addSuppressed(var12);
                        }

                        throw var13;
                    }

                    dataInputStream.close();
                }

                int i = compoundTag.contains("DataVersion", 99) ? compoundTag.getInt("DataVersion") : 1343;
                var8 = NbtUtils.update(this.fixerUpper, DataFixTypes.SAVED_DATA, compoundTag, i, dataVersion);
            } catch (Throwable var14) {
                try {
                    pushbackInputStream.close();
                } catch (Throwable var11) {
                    var14.addSuppressed(var11);
                }

                throw var14;
            }

            pushbackInputStream.close();
        } catch (Throwable var15) {
            com.destroystokyo.paper.exception.ServerInternalException.reportInternalException(var15); // Paper
            try {
                fileInputStream.close();
            } catch (Throwable var10) {
                var15.addSuppressed(var10);
            }

            throw var15;
        }

        fileInputStream.close();
        return var8;
    }

    private boolean isGzip(PushbackInputStream pushbackInputStream) throws IOException {
        byte[] bs = new byte[2];
        boolean bl = false;
        int i = pushbackInputStream.read(bs, 0, 2);
        if (i == 2) {
            int j = (bs[1] & 255) << 8 | bs[0] & 255;
            if (j == 35615) {
                bl = true;
            }
        }

        if (i != 0) {
            pushbackInputStream.unread(bs, 0, i);
        }

        return bl;
    }

    public void save() {
        this.cache.forEach((id, state) -> {
            if (state != null) {
                state.save(this.getDataFile(id));
            }

        });
    }
}
