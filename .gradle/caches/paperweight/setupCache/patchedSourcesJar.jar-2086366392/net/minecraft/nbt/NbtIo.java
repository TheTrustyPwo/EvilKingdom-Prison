// mc-dev import
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

    public NbtIo() {}

    public static CompoundTag readCompressed(File file) throws IOException {
        FileInputStream fileinputstream = new FileInputStream(file);

        CompoundTag nbttagcompound;

        try {
            nbttagcompound = NbtIo.readCompressed((InputStream) fileinputstream);
        } catch (Throwable throwable) {
            try {
                fileinputstream.close();
            } catch (Throwable throwable1) {
                throwable.addSuppressed(throwable1);
            }

            throw throwable;
        }

        fileinputstream.close();
        return nbttagcompound;
    }

    private static DataInputStream createDecompressorStream(InputStream stream) throws IOException {
        return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(stream)));
    }

    public static CompoundTag readCompressed(InputStream stream) throws IOException {
        DataInputStream datainputstream = NbtIo.createDecompressorStream(stream);

        CompoundTag nbttagcompound;

        try {
            nbttagcompound = NbtIo.read(datainputstream, NbtAccounter.UNLIMITED);
        } catch (Throwable throwable) {
            if (datainputstream != null) {
                try {
                    datainputstream.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            }

            throw throwable;
        }

        if (datainputstream != null) {
            datainputstream.close();
        }

        return nbttagcompound;
    }

    public static void parseCompressed(File file, StreamTagVisitor scanner) throws IOException {
        FileInputStream fileinputstream = new FileInputStream(file);

        try {
            NbtIo.parseCompressed((InputStream) fileinputstream, scanner);
        } catch (Throwable throwable) {
            try {
                fileinputstream.close();
            } catch (Throwable throwable1) {
                throwable.addSuppressed(throwable1);
            }

            throw throwable;
        }

        fileinputstream.close();
    }

    public static void parseCompressed(InputStream stream, StreamTagVisitor scanner) throws IOException {
        DataInputStream datainputstream = NbtIo.createDecompressorStream(stream);

        try {
            NbtIo.parse(datainputstream, scanner);
        } catch (Throwable throwable) {
            if (datainputstream != null) {
                try {
                    datainputstream.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            }

            throw throwable;
        }

        if (datainputstream != null) {
            datainputstream.close();
        }

    }

    public static void writeCompressed(CompoundTag compound, File file) throws IOException {
        FileOutputStream fileoutputstream = new FileOutputStream(file);

        try {
            NbtIo.writeCompressed(compound, (OutputStream) fileoutputstream);
        } catch (Throwable throwable) {
            try {
                fileoutputstream.close();
            } catch (Throwable throwable1) {
                throwable.addSuppressed(throwable1);
            }

            throw throwable;
        }

        fileoutputstream.close();
    }

    public static void writeCompressed(CompoundTag compound, OutputStream stream) throws IOException {
        DataOutputStream dataoutputstream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(stream)));

        try {
            NbtIo.write(compound, (DataOutput) dataoutputstream);
        } catch (Throwable throwable) {
            try {
                dataoutputstream.close();
            } catch (Throwable throwable1) {
                throwable.addSuppressed(throwable1);
            }

            throw throwable;
        }

        dataoutputstream.close();
    }

    public static void write(CompoundTag compound, File file) throws IOException {
        FileOutputStream fileoutputstream = new FileOutputStream(file);

        try {
            DataOutputStream dataoutputstream = new DataOutputStream(fileoutputstream);

            try {
                NbtIo.write(compound, (DataOutput) dataoutputstream);
            } catch (Throwable throwable) {
                try {
                    dataoutputstream.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }

                throw throwable;
            }

            dataoutputstream.close();
        } catch (Throwable throwable2) {
            try {
                fileoutputstream.close();
            } catch (Throwable throwable3) {
                throwable2.addSuppressed(throwable3);
            }

            throw throwable2;
        }

        fileoutputstream.close();
    }

    @Nullable
    public static CompoundTag read(File file) throws IOException {
        if (!file.exists()) {
            return null;
        } else {
            FileInputStream fileinputstream = new FileInputStream(file);

            CompoundTag nbttagcompound;

            try {
                DataInputStream datainputstream = new DataInputStream(fileinputstream);

                try {
                    nbttagcompound = NbtIo.read(datainputstream, NbtAccounter.UNLIMITED);
                } catch (Throwable throwable) {
                    try {
                        datainputstream.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }

                    throw throwable;
                }

                datainputstream.close();
            } catch (Throwable throwable2) {
                try {
                    fileinputstream.close();
                } catch (Throwable throwable3) {
                    throwable2.addSuppressed(throwable3);
                }

                throw throwable2;
            }

            fileinputstream.close();
            return nbttagcompound;
        }
    }

    public static CompoundTag read(DataInput input) throws IOException {
        return NbtIo.read(input, NbtAccounter.UNLIMITED);
    }

    public static CompoundTag read(DataInput input, NbtAccounter tracker) throws IOException {
        // Spigot start
        if ( input instanceof io.netty.buffer.ByteBufInputStream )
        {
            input = new DataInputStream(new org.spigotmc.LimitStream((InputStream) input, tracker));
        }
        // Spigot end
        Tag nbtbase = NbtIo.readUnnamedTag(input, 0, tracker);

        if (nbtbase instanceof CompoundTag) {
            return (CompoundTag) nbtbase;
        } else {
            throw new IOException("Root tag must be a named compound tag");
        }
    }

    public static void write(CompoundTag compound, DataOutput output) throws IOException {
        NbtIo.writeUnnamedTag(compound, output);
    }

    public static void parse(DataInput input, StreamTagVisitor scanner) throws IOException {
        TagType<?> nbttagtype = TagTypes.getType(input.readByte());

        if (nbttagtype == EndTag.TYPE) {
            if (scanner.visitRootEntry(EndTag.TYPE) == StreamTagVisitor.ValueResult.CONTINUE) {
                scanner.visitEnd();
            }

        } else {
            switch (scanner.visitRootEntry(nbttagtype)) {
                case HALT:
                default:
                    break;
                case BREAK:
                    StringTag.skipString(input);
                    nbttagtype.skip(input);
                    break;
                case CONTINUE:
                    StringTag.skipString(input);
                    nbttagtype.parse(input, scanner);
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
        byte b0 = input.readByte();

        if (b0 == 0) {
            return EndTag.INSTANCE;
        } else {
            StringTag.skipString(input);

            try {
                return TagTypes.getType(b0).load(input, depth, tracker);
            } catch (IOException ioexception) {
                CrashReport crashreport = CrashReport.forThrowable(ioexception, "Loading NBT data");
                CrashReportCategory crashreportsystemdetails = crashreport.addCategory("NBT Tag");

                crashreportsystemdetails.setDetail("Tag type", (Object) b0);
                throw new ReportedException(crashreport);
            }
        }
    }
}
