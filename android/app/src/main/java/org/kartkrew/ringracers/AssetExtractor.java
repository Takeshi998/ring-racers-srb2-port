package org.kartkrew.ringracers;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

final class AssetExtractor {
    interface ProgressListener {
        void onProgress(int completed, int total, String name);
    }

    private static final String ASSET_VERSION = "2.4-android-1";
    private static final List<String> GAME_ASSETS = Arrays.asList(
        "bios.pk3",
        "gamecontrollerdb.txt",
        "data/altmusic.pk3",
        "data/chars.pk3",
        "data/followers.pk3",
        "data/gfx.pk3",
        "data/maps.pk3",
        "data/music.pk3",
        "data/scripts.pk3",
        "data/shaders.pk3",
        "data/sounds.pk3",
        "data/staffghosts.pk3",
        "data/textures_general.pk3",
        "data/textures_originalzones.pk3",
        "data/textures_segazones.pk3",
        "data/unlocks.pk3"
    );

    private AssetExtractor() {
    }

    static File prepare(Context context, ProgressListener listener) throws IOException {
        File gameDirectory = new File(context.getFilesDir(), "game");
        File marker = new File(gameDirectory, ".asset-version");

        if (isCurrent(marker, gameDirectory)) {
            listener.onProgress(GAME_ASSETS.size(), GAME_ASSETS.size(), "Ready");
            return gameDirectory;
        }

        deleteRecursively(gameDirectory);
        if (!gameDirectory.mkdirs() && !gameDirectory.isDirectory()) {
            throw new IOException("Could not create " + gameDirectory);
        }

        AssetManager assets = context.getAssets();
        // No-assets builds: users copy the 16 files over USB. Accept two layouts:
        // shared RingRacers/game/<rel> (preferred) or PC-style RingRacers/<rel>.
        File sharedDir = StorageHelper.getGameDir(StorageHelper.resolveStorageRoot(context));
        File sharedGame = new File(sharedDir, "game");
        int completed = 0;
        try {
            for (String relativePath : GAME_ASSETS) {
                listener.onProgress(completed, GAME_ASSETS.size(), relativePath);
                File destination = new File(gameDirectory, relativePath);
                if (assetExists(assets, "game/" + relativePath)) {
                    copyAsset(assets, "game/" + relativePath, destination);
                } else {
                    File staged = new File(sharedGame, relativePath);
                    if (!staged.isFile()) {
                        staged = new File(sharedDir, relativePath);
                    }
                    if (!staged.isFile()) {
                        throw new IOException("NEEDFILE:" + relativePath + "|"
                            + new File(sharedGame, "").getAbsolutePath());
                    }
                    copyStaged(staged, destination);
                }
                completed++;
            }
            writeText(marker, ASSET_VERSION);
        } catch (IOException exception) {
            deleteRecursively(gameDirectory);
            throw exception;
        }

        listener.onProgress(completed, GAME_ASSETS.size(), "Ready");
        return gameDirectory;
    }

    static File prepareUserHome(Context context) throws IOException {
        File storageRoot = StorageHelper.resolveStorageRoot(context);
        return StorageHelper.ensureTree(storageRoot);
    }

    private static boolean isCurrent(File marker, File gameDirectory) {
        if (!marker.isFile()) {
            return false;
        }
        try {
            byte[] bytes = new byte[(int) marker.length()];
            try (FileInputStream input = new FileInputStream(marker)) {
                int offset = 0;
                while (offset < bytes.length) {
                    int read = input.read(bytes, offset, bytes.length - offset);
                    if (read < 0) {
                        return false;
                    }
                    offset += read;
                }
            }
            if (!ASSET_VERSION.equals(new String(bytes, StandardCharsets.UTF_8))) {
                return false;
            }
        } catch (IOException exception) {
            return false;
        }

        for (String relativePath : GAME_ASSETS) {
            File file = new File(gameDirectory, relativePath);
            if (!file.isFile() || file.length() == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean assetExists(AssetManager assets, String assetPath) {
        try {
            assets.open(assetPath, AssetManager.ACCESS_STREAMING).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void copyStaged(File source, File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Could not create " + parent);
        }

        File temporary = new File(destination.getPath() + ".tmp");
        byte[] buffer = new byte[1024 * 1024];
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(source));
             BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(temporary), buffer.length)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }

        if (!temporary.renameTo(destination)) {
            temporary.delete();
            throw new IOException("Could not finish extracting " + destination.getName());
        }
    }

    private static void copyAsset(AssetManager assets, String assetPath, File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Could not create " + parent);
        }

        File temporary = new File(destination.getPath() + ".tmp");
        byte[] buffer = new byte[1024 * 1024];
        try (BufferedInputStream input = new BufferedInputStream(assets.open(assetPath, AssetManager.ACCESS_STREAMING));
             BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(temporary), buffer.length)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }

        if (!temporary.renameTo(destination)) {
            temporary.delete();
            throw new IOException("Could not finish extracting " + destination.getName());
        }
    }

    private static void writeText(File file, String text) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void deleteRecursively(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Could not delete stale file " + file);
        }
    }
}
