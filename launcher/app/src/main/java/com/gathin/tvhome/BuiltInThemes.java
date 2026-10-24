package com.gathin.tvhome;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Themes picked from Launcher settings. The APK carries copies of some folders in themes/ (assets/themes, copied at
 * build time), and applying one writes the same files tools/theme.sh pushes: config.json, wallpaper.jpg and a new
 * theme_revision, so Config clears the on-TV choices exactly as it does after a push. "original" removes the theme,
 * like {@code tools/theme.sh --reset}.
 */
final class BuiltInThemes {
    private static final String TAG = "TvHome.Themes";
    static final String ORIGINAL = "original";
    /** Theme folder names, in the order Launcher settings shows them. */
    static final String[] NAMES = {ORIGINAL, "aurora", "six-columns"};

    private BuiltInThemes() { }

    /** Writes the theme into the folder Config reads and records it as applied. False if a file could not be written. */
    static boolean apply(Context ctx, String name) {
        File dir = Config.dir(ctx);
        if (dir == null || (!dir.isDirectory() && !dir.mkdirs())) return false;
        File config = new File(dir, Config.FILE_NAME);
        File wallpaper = new File(dir, Config.WALLPAPER_NAME);
        try {
            if (ORIGINAL.equals(name)) {
                delete(config);
                delete(wallpaper);
            } else {
                AssetManager assets = ctx.getAssets();
                copy(assets, "themes/" + name + "/" + Config.FILE_NAME, config);
                if (!copyIfPresent(assets, "themes/" + name + "/" + Config.WALLPAPER_NAME, wallpaper)) delete(wallpaper);
            }
            try (OutputStream out = new FileOutputStream(new File(dir, Config.THEME_REVISION_NAME))) {
                out.write(UUID.randomUUID().toString().replace("-", "").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            }
        } catch (IOException e) {
            Log.w(TAG, "could not apply " + name + ": " + e.getMessage());
            return false;
        }
        // Loading notices the new revision and clears the on-TV choices; then remember which theme this was.
        Config.load(ctx);
        ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).edit()
                .putString(Config.APPLIED_THEME_KEY, name).commit();
        return true;
    }

    private static void delete(File file) throws IOException {
        if (file.exists() && !file.delete()) throw new IOException("could not delete " + file);
    }

    private static boolean copyIfPresent(AssetManager assets, String path, File to) throws IOException {
        try {
            copy(assets, path, to);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    /** Copies through a temporary file, so Home never reads half a config.json or wallpaper. */
    private static void copy(AssetManager assets, String path, File to) throws IOException {
        File tmp = new File(to.getPath() + ".tmp");
        try (InputStream in = assets.open(path); OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        if (!tmp.renameTo(to)) {
            tmp.delete();
            throw new IOException("could not write " + to);
        }
    }
}
