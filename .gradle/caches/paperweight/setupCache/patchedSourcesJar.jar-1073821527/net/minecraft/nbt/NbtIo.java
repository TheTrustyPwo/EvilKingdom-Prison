package net.minecraft.nbt;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.util.FastBufferedInputStream;

public class NbtIo {
    public static CompoundTag readCompressed(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);

        CompoundTag var2;
        try {
            var2 = readCompressed(inputStream);
        } catch (Throwable var5) {
            try {
                inputStream.close();
            } catch (Throwable var4) {
                var5.addSuppressed(var4);
            }

            throw var5;
        }

        inputStream.close();
        return var2;
    }

    private static DataInputStream createDecompressorStream(InputStream stream) throws IOException {
        return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(stream)));
    }

    public static CompoundTag readCompressed(InputStream stream) throws IOException {
        DataInputStream dataInputStream = createDecompressorStream(stream);

        CompoundTag var2;
        try {
            var2 = read(dataInputStream, NbtAccounter.UNLIMITED);
        } catch (Throwable var5) {
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (Throwable var4) {
                    var5.addSuppressed(var4);
                }
            }

            throw var5;
        }

        if (dataInputStream != null) {
            dataInputStream.close();
        }

        return var2;
    }

    public static void parseCompressed(File file, StreamTagVisitor scanner) throws IOException {
        InputStream inputStream = new FileInputStream(file);

        try {
            parseCompressed(inputStream, scanner);
        } catch (Throwable var6) {
            try {
                inputStream.close();
            } catch (Throwable var5) {
                var6.addSuppressed(var5);
            }

            throw var6;
        }

        inputStream.close();
    }

    public static void parseCompressed(InputStream stream, StreamTagVisitor scanner) throws IOException {
        DataInputStream dataInputStream = createDecompressorStream(stream);

        try {
            parse(dataInputStream, scanner);
        } catch (Throwable var6) {
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }
            }

            throw var6;
        }

        if (dataInputStream != null) {
            dataInputStream.close();
        }

    }

    public static void writeCompressed(CompoundTag compound, File file) throws IOException {
        OutputStream outputStream = new FileOutputStream(file);

        try {
            writeCompressed(compound, outputStream);
        } catch (Throwable var6) {
            try {
                outputStream.close();
            } catch (Throwable var5) {
                var6.addSuppressed(var5);
            }

            throw var6;
        }

        outputStream.close();
    }

    public static void writeCompressed(CompoundTag compound, OutputStream stream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(stream)));

        try {
            write(compound, dataOutputStream);
        } catch (Throwable var6) {
            try {
                dataOutputStream.close();
            } catch (Throwable var5) {
                var6.addSuppressed(var5);
            }

            throw var6;
        }

        dataOutputStream.close();
    }

    public static void write(CompoundTag compound, File file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        try {
            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

            try {
                write(compound, dataOutputStream);
            } catch (Throwable var8) {
                try {
                    dataOutputStream.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }

                throw var8;
            }

            dataOutputStream.close();
        } catch (Throwable var9) {
            try {
                fileOutputStream.close();
            } catch (Throwable var6) {
                var9.addSuppressed(var6);
            }

            throw var9;
        }

        fileOutputStream.close();
    }

    @Nullable
    public static CompoundTag read(File file) throws IOException {
        if (!file.exists()) {
            return null;
        } else {
            FileInputStream fileInputStream = new FileInputStream(file);

            CompoundTag var3;
            try {
                DataInputStream dataInputStream = new DataInputStream(fileInputStream);

                try {
                    var3 = read(dataInputStream, NbtAccounter.UNLIMITED);
                } catch (Throwable var7) {
                    try {
                        dataInputStream.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }

                    throw var7;
                }

                dataInputStream.close();
            } catch (Throwable var8) {
                try {
                    fileInputStream.close();
                } catch (Throwable var5) {
                    var8.addSuppressed(var5);
                }

                throw var8;
            }

            fileInputStream.close();
            return var3;
        }
    }

    public static CompoundTag read(DataInput input) throws IOException {
        return read(input, NbtAccounter.UNLIMITED);
    }

    public static CompoundTag read(DataInput input, NbtAccounter tracker) throws IOException {
        Tag tag = readUnnamedTag(input, 0, tracker);
        if (tag instanceof CompoundTag) {
            return (CompoundTag)tag;
        } else {
            throw new IOException("Root tag must be a named compound tag");
        }
    }

    public static void write(CompoundTag compound, DataOutput output) throws IOException {
        writeUnnamedTag(compound, output);
    }

    public static void parse(DataInput input, StreamTagVisitor scanner) throws IOException {
        TagType<?> tagType = TagTypes.getType(input.readByte());
        if (tagType == EndTag.TYPE) {
            if (scanner.visitRootEntry(EndTag.TYPE) == StreamTagVisitor.ValueResult.CONTINUE) {
                scanner.visitEnd();
            }

        } else {
            switch(scanner.visitRootEntry(tagType)) {
            case HALT:
            default:
                break;
            case BREAK:
                StringTag.skipString(input);
                tagType.skip(input);
                break;
            case CONTINUE:
                StringTag.skipString(input);
                tagType.parse(input, scanner);
            }

        }
    }

    public static void writeUnnamedTag(Tag element, DataOutput output) throws IOException {
        output.writeByte(element.getId());
        if (element.getId() != 0) {
            output.writeUTF("");
            element.write(output);
        }
    }

    private static Tag readUnnamedTag(DataInput input, int depth, NbtAccounter tracker) throws IOException {
        byte b = input.readByte();
        if (b == 0) {
            return EndTag.INSTANCE;
        } else {
            StringTag.skipString(input);

            try {
                return TagTypes.getType(b).load(input, depth, tracker);
            } catch (IOException var7) {
                CrashReport crashReport = CrashReport.forThrowable(var7, "Loading NBT data");
                CrashReportCategory crashReportCategory = crashReport.addCategory("NBT Tag");
                crashReportCategory.setDetail("Tag type", b);
                throw new ReportedException(crashReport);
            }
        }
    }
}
