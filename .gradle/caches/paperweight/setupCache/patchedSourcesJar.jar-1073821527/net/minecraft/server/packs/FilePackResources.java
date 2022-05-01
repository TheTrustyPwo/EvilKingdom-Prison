package net.minecraft.server.packs;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;

public class FilePackResources extends AbstractPackResources {
    public static final Splitter SPLITTER = Splitter.on('/').omitEmptyStrings().limit(3);
    @Nullable
    private ZipFile zipFile;

    public FilePackResources(File base) {
        super(base);
    }

    private ZipFile getOrCreateZipFile() throws IOException {
        if (this.zipFile == null) {
            this.zipFile = new ZipFile(this.file);
        }

        return this.zipFile;
    }

    @Override
    protected InputStream getResource(String name) throws IOException {
        ZipFile zipFile = this.getOrCreateZipFile();
        ZipEntry zipEntry = zipFile.getEntry(name);
        if (zipEntry == null) {
            throw new ResourcePackFileNotFoundException(this.file, name);
        } else {
            return zipFile.getInputStream(zipEntry);
        }
    }

    @Override
    public boolean hasResource(String name) {
        try {
            return this.getOrCreateZipFile().getEntry(name) != null;
        } catch (IOException var3) {
            return false;
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        ZipFile zipFile;
        try {
            zipFile = this.getOrCreateZipFile();
        } catch (IOException var9) {
            return Collections.emptySet();
        }

        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        Set<String> set = Sets.newHashSet();

        while(enumeration.hasMoreElements()) {
            ZipEntry zipEntry = enumeration.nextElement();
            String string = zipEntry.getName();
            if (string.startsWith(type.getDirectory() + "/")) {
                List<String> list = Lists.newArrayList(SPLITTER.split(string));
                if (list.size() > 1) {
                    String string2 = list.get(1);
                    if (string2.equals(string2.toLowerCase(Locale.ROOT))) {
                        set.add(string2);
                    } else {
                        this.logWarning(string2);
                    }
                }
            }
        }

        return set;
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    @Override
    public void close() {
        if (this.zipFile != null) {
            IOUtils.closeQuietly((Closeable)this.zipFile);
            this.zipFile = null;
        }

    }

    @Override
    public Collection<ResourceLocation> getResources(PackType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter) {
        ZipFile zipFile;
        try {
            zipFile = this.getOrCreateZipFile();
        } catch (IOException var15) {
            return Collections.emptySet();
        }

        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        List<ResourceLocation> list = Lists.newArrayList();
        String string = type.getDirectory() + "/" + namespace + "/";
        String string2 = string + prefix + "/";

        while(enumeration.hasMoreElements()) {
            ZipEntry zipEntry = enumeration.nextElement();
            if (!zipEntry.isDirectory()) {
                String string3 = zipEntry.getName();
                if (!string3.endsWith(".mcmeta") && string3.startsWith(string2)) {
                    String string4 = string3.substring(string.length());
                    String[] strings = string4.split("/");
                    if (strings.length >= maxDepth + 1 && pathFilter.test(strings[strings.length - 1])) {
                        list.add(new ResourceLocation(namespace, string4));
                    }
                }
            }
        }

        return list;
    }
}
