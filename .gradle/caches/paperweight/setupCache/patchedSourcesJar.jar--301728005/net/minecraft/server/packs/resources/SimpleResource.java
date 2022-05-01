package net.minecraft.server.packs.resources;

import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;

public class SimpleResource implements Resource {
    private final String sourceName;
    private final ResourceLocation location;
    private final InputStream resourceStream;
    private final InputStream metadataStream;
    private boolean triedMetadata;
    private JsonObject metadata;

    public SimpleResource(String packName, ResourceLocation id, InputStream inputStream, @Nullable InputStream metaInputStream) {
        this.sourceName = packName;
        this.location = id;
        this.resourceStream = inputStream;
        this.metadataStream = metaInputStream;
    }

    @Override
    public ResourceLocation getLocation() {
        return this.location;
    }

    @Override
    public InputStream getInputStream() {
        return this.resourceStream;
    }

    @Override
    public boolean hasMetadata() {
        return this.metadataStream != null;
    }

    @Nullable
    @Override
    public <T> T getMetadata(MetadataSectionSerializer<T> metaReader) {
        if (!this.hasMetadata()) {
            return (T)null;
        } else {
            if (this.metadata == null && !this.triedMetadata) {
                this.triedMetadata = true;
                BufferedReader bufferedReader = null;

                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(this.metadataStream, StandardCharsets.UTF_8));
                    this.metadata = GsonHelper.parse(bufferedReader);
                } finally {
                    IOUtils.closeQuietly((Reader)bufferedReader);
                }
            }

            if (this.metadata == null) {
                return (T)null;
            } else {
                String string = metaReader.getMetadataSectionName();
                return (T)(this.metadata.has(string) ? metaReader.fromJson(GsonHelper.getAsJsonObject(this.metadata, string)) : null);
            }
        }
    }

    @Override
    public String getSourceName() {
        return this.sourceName;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof SimpleResource)) {
            return false;
        } else {
            SimpleResource simpleResource = (SimpleResource)object;
            if (this.location != null) {
                if (!this.location.equals(simpleResource.location)) {
                    return false;
                }
            } else if (simpleResource.location != null) {
                return false;
            }

            if (this.sourceName != null) {
                if (!this.sourceName.equals(simpleResource.sourceName)) {
                    return false;
                }
            } else if (simpleResource.sourceName != null) {
                return false;
            }

            return true;
        }
    }

    @Override
    public int hashCode() {
        int i = this.sourceName != null ? this.sourceName.hashCode() : 0;
        return 31 * i + (this.location != null ? this.location.hashCode() : 0);
    }

    @Override
    public void close() throws IOException {
        this.resourceStream.close();
        if (this.metadataStream != null) {
            this.metadataStream.close();
        }

    }
}
