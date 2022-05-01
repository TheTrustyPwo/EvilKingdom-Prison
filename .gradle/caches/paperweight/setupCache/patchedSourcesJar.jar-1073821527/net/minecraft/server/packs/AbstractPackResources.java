package net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public abstract class AbstractPackResources implements PackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final File file;

    public AbstractPackResources(File base) {
        this.file = base;
    }

    private static String getPathFromLocation(PackType type, ResourceLocation id) {
        return String.format("%s/%s/%s", type.getDirectory(), id.getNamespace(), id.getPath());
    }

    protected static String getRelativePath(File base, File target) {
        return base.toURI().relativize(target.toURI()).getPath();
    }

    @Override
    public InputStream getResource(PackType type, ResourceLocation id) throws IOException {
        return this.getResource(getPathFromLocation(type, id));
    }

    @Override
    public boolean hasResource(PackType type, ResourceLocation id) {
        return this.hasResource(getPathFromLocation(type, id));
    }

    protected abstract InputStream getResource(String name) throws IOException;

    @Override
    public InputStream getRootResource(String fileName) throws IOException {
        if (!fileName.contains("/") && !fileName.contains("\\")) {
            return this.getResource(fileName);
        } else {
            throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
        }
    }

    protected abstract boolean hasResource(String name);

    protected void logWarning(String namespace) {
        LOGGER.warn("ResourcePack: ignored non-lowercase namespace: {} in {}", namespace, this.file);
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> metaReader) throws IOException {
        InputStream inputStream = this.getResource("pack.mcmeta");

        Object var3;
        try {
            var3 = getMetadataFromStream(metaReader, inputStream);
        } catch (Throwable var6) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }
            }

            throw var6;
        }

        if (inputStream != null) {
            inputStream.close();
        }

        return (T)var3;
    }

    @Nullable
    public static <T> T getMetadataFromStream(MetadataSectionSerializer<T> metaReader, InputStream inputStream) {
        JsonObject jsonObject;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            try {
                jsonObject = GsonHelper.parse(bufferedReader);
            } catch (Throwable var8) {
                try {
                    bufferedReader.close();
                } catch (Throwable var6) {
                    var8.addSuppressed(var6);
                }

                throw var8;
            }

            bufferedReader.close();
        } catch (Exception var9) {
            LOGGER.error("Couldn't load {} metadata", metaReader.getMetadataSectionName(), var9);
            return (T)null;
        }

        if (!jsonObject.has(metaReader.getMetadataSectionName())) {
            return (T)null;
        } else {
            try {
                return metaReader.fromJson(GsonHelper.getAsJsonObject(jsonObject, metaReader.getMetadataSectionName()));
            } catch (Exception var7) {
                LOGGER.error("Couldn't load {} metadata", metaReader.getMetadataSectionName(), var7);
                return (T)null;
            }
        }
    }

    @Override
    public String getName() {
        return this.file.getName();
    }
}
