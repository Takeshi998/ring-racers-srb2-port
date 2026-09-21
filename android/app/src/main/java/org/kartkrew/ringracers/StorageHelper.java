package org.kartkrew.ringracers;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Centralizes the hybrid shared-storage strategy.
 *
 * <p>Preferred: shared {@code /sdcard/RingRacers} so the folder is visible in
 * the file manager and mods can be copied by USB. The engine appends
 * {@code RingRacers} itself (DEFAULTDIR on Android), so callers pass the
 * storage <i>root</i> as {@code -home}.</p>
 *
 * <p>Fallback (no permission / not writable): app-specific external dir, then
 * internal files dir. Those never crash the engine but are not user-visible.</p>
 */
final class StorageHelper {
    static final String GAME_DIR_NAME = "RingRacers";
    static final String ADDONS_DIR_NAME = "addons";
    static final String DOWNLOADS_DIR_NAME = "downloads";
    private static final String PREFS_NAME = "ringracers_storage";
    private static final String KEY_CUSTOM_ROOT = "custom_root";

    private StorageHelper() {
    }

    /** User-picked storage root (folder picker), or null. */
    static File getCustomRoot(Context context) {
        String path = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_ROOT, null);
        if (path == null || path.isEmpty()) {
            return null;
        }
        File dir = new File(path);
        return (dir.isDirectory() && dir.canWrite()) ? dir : null;
    }

    static void setCustomRoot(Context context, File root) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM_ROOT, root.getAbsolutePath()).apply();
    }

    static void clearCustomRoot(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_CUSTOM_ROOT).apply();
    }

    private static final String KEY_LAUNCHED = "has_launched";

    static boolean hasLaunched(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LAUNCHED, false);
    }

    static void setLaunched(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LAUNCHED, true).apply();
    }

    static boolean hasSharedAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        File root = Environment.getExternalStorageDirectory();
        return root != null && root.canWrite();
    }

    /** Best storage root to pass as {@code -home}. Never null. */
    static File resolveStorageRoot(Context context) {
        File custom = getCustomRoot(context);
        if (custom != null) {
            return custom;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                File shared = Environment.getExternalStorageDirectory();
                if (shared != null && shared.canWrite()) {
                    return shared;
                }
            }
        } else {
            File shared = Environment.getExternalStorageDirectory();
            if (shared != null && shared.canWrite()) {
                return shared;
            }
        }
        File appExternal = context.getExternalFilesDir(null);
        if (appExternal != null && appExternal.canWrite()) {
            return appExternal;
        }
        return context.getFilesDir();
    }

    static File getGameDir(File storageRoot) {
        // When storageRoot is the shared sdcard, game dir is /sdcard/RingRacers.
        // When it is already app-specific, avoid nesting RingRacers/RingRacers.
        if (storageRoot.getAbsolutePath().endsWith(GAME_DIR_NAME)) {
            return storageRoot;
        }
        return new File(storageRoot, GAME_DIR_NAME);
    }

    /** Creates RingRacers/addons/downloads. Throws with a user-readable message. */
    static File ensureTree(File storageRoot) throws IOException {
        File gameDir = getGameDir(storageRoot);
        File addonsDir = new File(gameDir, ADDONS_DIR_NAME);
        File downloadsDir = new File(addonsDir, DOWNLOADS_DIR_NAME);
        if (!gameDir.mkdirs() && !gameDir.isDirectory()) {
            throw new IOException("No se pudo crear " + gameDir + ". Revisa el permiso de almacenamiento.");
        }
        if (!addonsDir.mkdirs() && !addonsDir.isDirectory()) {
            throw new IOException("No se pudo crear " + addonsDir);
        }
        if (!downloadsDir.mkdirs() && !downloadsDir.isDirectory()) {
            throw new IOException("No se pudo crear " + downloadsDir);
        }
        // Write test: the engine aborts with I_Error if srb2home is not writable.
        // Fail here instead, with a message shown on the Retry screen.
        File probe = new File(gameDir, ".write-test");
        try {
            try (FileOutputStream out = new FileOutputStream(probe)) {
                out.write("ok".getBytes(StandardCharsets.UTF_8));
            }
            if (!probe.delete()) {
                probe.deleteOnExit();
            }
        } catch (IOException e) {
            throw new IOException("Sin escritura en " + gameDir + ". Concede acceso a archivos e reintenta.", e);
        }
        // Nudge file managers / MTP into showing the new folders.
        try {
            new File(gameDir, ".nomedia").delete();
        } catch (SecurityException ignored) {
        }
        return storageRoot;
    }

    static File ensureTree(Context context) throws IOException {
        return ensureTree(resolveStorageRoot(context));
    }
}
