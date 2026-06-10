package com.atakmap.android.isproutes.plugin;

import android.content.Context;

import java.io.File;

public class PluginNativeLoader {

    private static String nativeLibraryDir = null;

    synchronized static public void init(final Context context) {
        if (nativeLibraryDir == null) {
            try {
                nativeLibraryDir = context.getPackageManager()
                        .getApplicationInfo(context.getPackageName(),
                                0).nativeLibraryDir;
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Native library loading will fail; unable to read nativeLibraryDir");
            }
        }
    }

    public static void loadLibrary(final String name) {
        if (nativeLibraryDir == null)
            throw new IllegalArgumentException("NativeLoader not initialized");

        final String lib = nativeLibraryDir + File.separator
                + System.mapLibraryName(name);
        if (new File(lib).exists()) {
            System.load(lib);
        }
    }
}
