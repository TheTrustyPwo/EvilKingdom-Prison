package net.minecraft.server.packs;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ResourceLocationException;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.slf4j.Logger;

public class FolderPackResources extends AbstractPackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ON_WINDOWS = Util.getPlatform() == Util.OS.WINDOWS;
    private static final CharMatcher BACKSLASH_MATCHER = CharMatcher.is('\\');

    public FolderPackResources(File base) {
        super(base);
    }

    public static boolean validatePath(File file, String filename) throws IOException {
        String string = file.getCanonicalPath();
        if (ON_WINDOWS) {
            string = BACKSLASH_MATCHER.replaceFrom(string, '/');
        }

        return string.endsWith(filename);
    }

    @Override
    protected InputStream getResource(String name) throws IOException {
        File file = this.getFile(name);
        if (file == null) {
            throw new ResourcePackFileNotFoundException(this.file, name);
        } else {
            return new FileInputStream(file);
        }
    }

    @Override
    protected boolean hasResource(String name) {
        return this.getFile(name) != null;
    }

    @Nullable
    private File getFile(String name) {
        try {
            File file = new File(this.file, name);
            if (file.isFile() && validatePath(file, name)) {
                return file;
            }
        } catch (IOException var3) {
        }

        return null;
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> set = Sets.newHashSet();
        File file = new File(this.file, type.getDirectory());
        File[] files = file.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
        if (files != null) {
            for(File file2 : files) {
                String string = getRelativePath(file, file2);
                if (string.equals(string.toLowerCase(Locale.ROOT))) {
                    set.add(string.substring(0, string.length() - 1));
                } else {
                    this.logWarning(string);
                }
            }
        }

        return set;
    }

    @Override
    public void close() {
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType type, String namespace, String prefix, int maxDepth, Predicate<String> pathFilter) {
        File file = new File(this.file, type.getDirectory());
        List<ResourceLocation> list = Lists.newArrayList();
        this.listResources(new File(new File(file, namespace), prefix), maxDepth, namespace, list, prefix + "/", pathFilter);
        return list;
    }

    private void listResources(File file, int maxDepth, String namespace, List<ResourceLocation> found, String prefix, Predicate<String> pathFilter) {
        File[] files = file.listFiles();
        if (files != null) {
            for(File file2 : files) {
                if (file2.isDirectory()) {
                    if (maxDepth > 0) {
                        this.listResources(file2, maxDepth - 1, namespace, found, prefix + file2.getName() + "/", pathFilter);
                    }
                } else if (!file2.getName().endsWith(".mcmeta") && pathFilter.test(file2.getName())) {
                    try {
                        found.add(new ResourceLocation(namespace, prefix + file2.getName()));
                    } catch (ResourceLocationException var13) {
                        LOGGER.error(var13.getMessage());
                    }
                }
            }
        }

    }
}
