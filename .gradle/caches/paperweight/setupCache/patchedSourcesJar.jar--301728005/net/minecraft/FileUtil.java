package net.minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;

public class FileUtil {
    private static final Pattern COPY_COUNTER_PATTERN = Pattern.compile("(<name>.*) \\((<count>\\d*)\\)", 66);
    private static final int MAX_FILE_NAME = 255;
    private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", 2);

    public static String findAvailableName(Path path, String name, String extension) throws IOException {
        for(char c : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
            name = name.replace(c, '_');
        }

        name = name.replaceAll("[./\"]", "_");
        if (RESERVED_WINDOWS_FILENAMES.matcher(name).matches()) {
            name = "_" + name + "_";
        }

        Matcher matcher = COPY_COUNTER_PATTERN.matcher(name);
        int i = 0;
        if (matcher.matches()) {
            name = matcher.group("name");
            i = Integer.parseInt(matcher.group("count"));
        }

        if (name.length() > 255 - extension.length()) {
            name = name.substring(0, 255 - extension.length());
        }

        while(true) {
            String string = name;
            if (i != 0) {
                String string2 = " (" + i + ")";
                int j = 255 - string2.length();
                if (name.length() > j) {
                    string = name.substring(0, j);
                }

                string = string + string2;
            }

            string = string + extension;
            Path path2 = path.resolve(string);

            try {
                Path path3 = Files.createDirectory(path2);
                Files.deleteIfExists(path3);
                return path.relativize(path3).toString();
            } catch (FileAlreadyExistsException var8) {
                ++i;
            }
        }
    }

    public static boolean isPathNormalized(Path path) {
        Path path2 = path.normalize();
        return path2.equals(path);
    }

    public static boolean isPathPortable(Path path) {
        for(Path path2 : path) {
            if (RESERVED_WINDOWS_FILENAMES.matcher(path2.toString()).matches()) {
                return false;
            }
        }

        return true;
    }

    public static Path createPathToResource(Path path, String resourceName, String extension) {
        String string = resourceName + extension;
        Path path2 = Paths.get(string);
        if (path2.endsWith(extension)) {
            throw new InvalidPathException(string, "empty resource name");
        } else {
            return path.resolve(path2);
        }
    }

    public static String getFullResourcePath(String path) {
        return FilenameUtils.getFullPath(path).replace(File.separator, "/");
    }

    public static String normalizeResourcePath(String path) {
        return FilenameUtils.normalize(path).replace(File.separator, "/");
    }
}
