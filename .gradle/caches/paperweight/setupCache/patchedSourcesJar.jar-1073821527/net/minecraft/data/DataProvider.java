package net.minecraft.data;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public interface DataProvider {
    HashFunction SHA1 = Hashing.sha1();

    void run(HashCache cache) throws IOException;

    String getName();

    static void save(Gson gson, HashCache cache, JsonElement output, Path path) throws IOException {
        String string = gson.toJson(output);
        String string2 = SHA1.hashUnencodedChars(string).toString();
        if (!Objects.equals(cache.getHash(path), string2) || !Files.exists(path)) {
            Files.createDirectories(path.getParent());
            BufferedWriter bufferedWriter = Files.newBufferedWriter(path);

            try {
                bufferedWriter.write(string);
            } catch (Throwable var10) {
                if (bufferedWriter != null) {
                    try {
                        bufferedWriter.close();
                    } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                    }
                }

                throw var10;
            }

            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }

        cache.putNew(path, string2);
    }
}
