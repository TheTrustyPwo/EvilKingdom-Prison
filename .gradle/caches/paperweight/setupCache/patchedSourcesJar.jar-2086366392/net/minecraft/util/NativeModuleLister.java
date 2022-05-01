package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Version;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.Tlhelp32.MODULEENTRY32W;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import net.minecraft.CrashReportCategory;
import org.slf4j.Logger;

public class NativeModuleLister {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int LANG_MASK = 65535;
    private static final int DEFAULT_LANG = 1033;
    private static final int CODEPAGE_MASK = -65536;
    private static final int DEFAULT_CODEPAGE = 78643200;

    public static List<NativeModuleLister.NativeModuleInfo> listModules() {
        if (!Platform.isWindows()) {
            return ImmutableList.of();
        } else {
            int i = Kernel32.INSTANCE.GetCurrentProcessId();
            Builder<NativeModuleLister.NativeModuleInfo> builder = ImmutableList.builder();

            for(MODULEENTRY32W mODULEENTRY32W : Kernel32Util.getModules(i)) {
                String string = mODULEENTRY32W.szModule();
                Optional<NativeModuleLister.NativeModuleVersion> optional = tryGetVersion(mODULEENTRY32W.szExePath());
                builder.add(new NativeModuleLister.NativeModuleInfo(string, optional));
            }

            return builder.build();
        }
    }

    private static Optional<NativeModuleLister.NativeModuleVersion> tryGetVersion(String path) {
        try {
            IntByReference intByReference = new IntByReference();
            int i = Version.INSTANCE.GetFileVersionInfoSize(path, intByReference);
            if (i == 0) {
                int j = Native.getLastError();
                if (j != 1813 && j != 1812) {
                    throw new Win32Exception(j);
                } else {
                    return Optional.empty();
                }
            } else {
                Pointer pointer = new Memory((long)i);
                if (!Version.INSTANCE.GetFileVersionInfo(path, 0, i, pointer)) {
                    throw new Win32Exception(Native.getLastError());
                } else {
                    IntByReference intByReference2 = new IntByReference();
                    Pointer pointer2 = queryVersionValue(pointer, "\\VarFileInfo\\Translation", intByReference2);
                    int[] is = pointer2.getIntArray(0L, intByReference2.getValue() / 4);
                    OptionalInt optionalInt = findLangAndCodepage(is);
                    if (!optionalInt.isPresent()) {
                        return Optional.empty();
                    } else {
                        int k = optionalInt.getAsInt();
                        int l = k & '\uffff';
                        int m = (k & -65536) >> 16;
                        String string = queryVersionString(pointer, langTableKey("FileDescription", l, m), intByReference2);
                        String string2 = queryVersionString(pointer, langTableKey("CompanyName", l, m), intByReference2);
                        String string3 = queryVersionString(pointer, langTableKey("FileVersion", l, m), intByReference2);
                        return Optional.of(new NativeModuleLister.NativeModuleVersion(string, string3, string2));
                    }
                }
            }
        } catch (Exception var14) {
            LOGGER.info("Failed to find module info for {}", path, var14);
            return Optional.empty();
        }
    }

    private static String langTableKey(String key, int languageId, int codePage) {
        return String.format("\\StringFileInfo\\%04x%04x\\%s", languageId, codePage, key);
    }

    private static OptionalInt findLangAndCodepage(int[] indices) {
        OptionalInt optionalInt = OptionalInt.empty();

        for(int i : indices) {
            if ((i & -65536) == 78643200 && (i & '\uffff') == 1033) {
                return OptionalInt.of(i);
            }

            optionalInt = OptionalInt.of(i);
        }

        return optionalInt;
    }

    private static Pointer queryVersionValue(Pointer pointer, String path, IntByReference lengthPointer) {
        PointerByReference pointerByReference = new PointerByReference();
        if (!Version.INSTANCE.VerQueryValue(pointer, path, pointerByReference, lengthPointer)) {
            throw new UnsupportedOperationException("Can't get version value " + path);
        } else {
            return pointerByReference.getValue();
        }
    }

    private static String queryVersionString(Pointer pointer, String path, IntByReference lengthPointer) {
        try {
            Pointer pointer2 = queryVersionValue(pointer, path, lengthPointer);
            byte[] bs = pointer2.getByteArray(0L, (lengthPointer.getValue() - 1) * 2);
            return new String(bs, StandardCharsets.UTF_16LE);
        } catch (Exception var5) {
            return "";
        }
    }

    public static void addCrashSection(CrashReportCategory section) {
        section.setDetail("Modules", () -> {
            return listModules().stream().sorted(Comparator.comparing((module) -> {
                return module.name;
            })).map((moduleName) -> {
                return "\n\t\t" + moduleName;
            }).collect(Collectors.joining());
        });
    }

    public static class NativeModuleInfo {
        public final String name;
        public final Optional<NativeModuleLister.NativeModuleVersion> version;

        public NativeModuleInfo(String path, Optional<NativeModuleLister.NativeModuleVersion> info) {
            this.name = path;
            this.version = info;
        }

        @Override
        public String toString() {
            return this.version.map((info) -> {
                return this.name + ":" + info;
            }).orElse(this.name);
        }
    }

    public static class NativeModuleVersion {
        public final String description;
        public final String version;
        public final String company;

        public NativeModuleVersion(String fileDescription, String fileVersion, String companyName) {
            this.description = fileDescription;
            this.version = fileVersion;
            this.company = companyName;
        }

        @Override
        public String toString() {
            return this.description + ":" + this.version + ":" + this.company;
        }
    }
}
