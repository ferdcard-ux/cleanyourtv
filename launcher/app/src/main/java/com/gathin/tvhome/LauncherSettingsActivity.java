package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    /** Result extra: open reorder mode on Home once this screen closes. */
    static final String EXTRA_REORDER = "reorder";
    private static final int PICK_WALLPAPER_REQUEST = 7;
    /** Dark backgrounds the text and the dock band are designed for. */
    private static final int[] BACKGROUND_COLORS = {0xFF000000, 0xFF0B1A2A, 0xFF101820, 0xFF0A2226,
            0xFF0E2018, 0xFF1A1030, 0xFF22101A, 0xFF2A1A0C};
    private static final int[] SKY_LEVELS = {0, 30, 60, 100};

    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] themeChoices = new TextView[BuiltInThemes.NAMES.length];
    private final TextView[] backgroundChoices = new TextView[BACKGROUND_COLORS.length];
    private final TextView[] skyChoices = new TextView[SKY_LEVELS.length];
    private TextView wallpaperTheme;
    private TextView wallpaperNone;
    private TextView wallpaperCustom;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView statsChoice;
    private TextView libraryChoice;
    private TextView followThemeChoice;
    private int columns;
    private boolean animations;
    private boolean advancedEffects;
    private int backgroundColor;
    private int skyIntensity;
    private boolean showStats;
    private boolean appLibrary;
    private boolean changed;
    private boolean importing;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        readConfig();

        int[] themeIds = {R.id.themeOriginal, R.id.themeAurora, R.id.themeSixColumns};
        for (int i = 0; i < themeIds.length; i++) {
            final String name = BuiltInThemes.NAMES[i];
            themeChoices[i] = findViewById(themeIds[i]);
            themeChoices[i].setOnClickListener(v -> {
                if (!BuiltInThemes.apply(this, name)) {
                    Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
                    return;
                }
                readConfig();
                changed();
            });
        }

        LinearLayout swatches = findViewById(R.id.backgroundChoices);
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < BACKGROUND_COLORS.length; i++) {
            final int color = BACKGROUND_COLORS[i];
            TextView swatch = new TextView(this, null, 0, R.style.SettingsColumnChoice);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (52 * density), 1f);
            lp.setMarginEnd((int) (10 * density));
            swatch.setLayoutParams(lp);
            swatch.setBackground(swatchBackground(color, density));
            swatch.setContentDescription(String.format("#%06X", color & 0xFFFFFF));
            swatch.setOnClickListener(v -> {
                backgroundColor = color;
                choices.edit().putInt("background_color", color).apply();
                changed();
            });
            swatches.addView(swatch);
            backgroundChoices[i] = swatch;
        }

        wallpaperTheme = findViewById(R.id.wallpaperTheme);
        wallpaperTheme.setOnClickListener(v -> {
            choices.edit().remove("wallpaper_mode").apply();
            changed();
        });
        wallpaperNone = findViewById(R.id.wallpaperNone);
        wallpaperNone.setOnClickListener(v -> {
            choices.edit().putString("wallpaper_mode", "none").apply();
            changed();
        });
        wallpaperCustom = findViewById(R.id.wallpaperCustom);
        wallpaperCustom.setOnClickListener(v -> pickWallpaper());

        int[] skyIds = {R.id.skyOff, R.id.skySoft, R.id.skyMedium, R.id.skyStrong};
        for (int i = 0; i < skyIds.length; i++) {
            final int level = SKY_LEVELS[i];
            skyChoices[i] = findViewById(skyIds[i]);
            skyChoices[i].setOnClickListener(v -> {
                skyIntensity = level;
                choices.edit().putInt("sky_intensity", level).apply();
                changed();
            });
        }

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
            });
        }
        // Keep the numeric choices increasing in the Hebrew reading direction. Reorder the
        // actual views so D-pad focus follows their positions without changing the saved values.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            LinearLayout row = (LinearLayout) columnChoices[0].getParent();
            row.removeAllViews();
            for (int i = columnChoices.length - 1; i >= 0; i--) row.addView(columnChoices[i]);
        }
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        statsChoice = findViewById(R.id.statsChoice);
        statsChoice.setOnClickListener(v -> {
            showStats = !showStats;
            choices.edit().putBoolean("show_stats", showStats).apply();
            changed();
        });
        libraryChoice = findViewById(R.id.libraryChoice);
        libraryChoice.setOnClickListener(v -> {
            appLibrary = !appLibrary;
            choices.edit().putBoolean("app_library", appLibrary).apply();
            changed();
        });
        findViewById(R.id.reorderChoice).setOnClickListener(v -> {
            setResult(changed ? RESULT_OK : RESULT_CANCELED, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!Config.hasOnTvChoices(choices)) return;
            Config.clearOnTvChoices(choices.edit()).commit();
            readConfig();
            changed();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(new Intent(Settings.ACTION_SETTINGS)));
        refresh();
        themeChoices[0].requestFocus();
    }

    /** The values in effect: config.xml, the theme, then the on-TV choices (see Config.load). */
    private void readConfig() {
        Config config = Config.load(this);
        columns = config.columns;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;
        backgroundColor = config.backgroundColor;
        skyIntensity = config.skyIntensity;
        showStats = config.showStats;
        appLibrary = config.appLibrary;
    }

    private void changed() {
        changed = true;
        setResult(RESULT_OK);
        refresh();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
    }

    private void refresh() {
        String applied = choices.getString(Config.APPLIED_THEME_KEY, "");
        for (int i = 0; i < themeChoices.length; i++) {
            themeChoices[i].setSelected(BuiltInThemes.NAMES[i].equals(applied));
        }
        for (int i = 0; i < backgroundChoices.length; i++) {
            backgroundChoices[i].setSelected(backgroundColor == BACKGROUND_COLORS[i]);
        }
        String wallpaperMode = choices.getString("wallpaper_mode", "");
        wallpaperTheme.setSelected(wallpaperMode.isEmpty());
        wallpaperNone.setSelected("none".equals(wallpaperMode));
        wallpaperCustom.setSelected("custom".equals(wallpaperMode));
        for (int i = 0; i < skyChoices.length; i++) {
            skyChoices[i].setSelected(skyIntensity == SKY_LEVELS[i]);
        }
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        statsChoice.setText(getString(R.string.stats_state, getString(showStats ? R.string.on : R.string.off)));
        libraryChoice.setText(getString(R.string.app_library_state, getString(appLibrary ? R.string.on : R.string.off)));
        followThemeChoice.setSelected(!Config.hasOnTvChoices(choices));
    }

    /** The usual chip background with the colour as a rounded square inside it; focus and selection still show around it. */
    private Drawable swatchBackground(int color, float density) {
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(color);
        fill.setCornerRadius(9 * density);
        fill.setStroke((int) Math.max(1, density), 0x55FFFFFF);
        int inset = (int) (7 * density);
        return new LayerDrawable(new Drawable[]{getDrawable(R.drawable.settings_choice_bg),
                new InsetDrawable(fill, inset)});
    }

    // ---- custom wallpaper -------------------------------------------------------------------------

    private void pickWallpaper() {
        if (importing) return;
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(pick, PICK_WALLPAPER_REQUEST);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_WALLPAPER_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        final File target = Config.customWallpaper(this);
        importing = true;
        new Thread(() -> {
            boolean ok = importWallpaper(uri, target, Math.max(dm.widthPixels, dm.heightPixels),
                    Math.min(dm.widthPixels, dm.heightPixels));
            runOnUiThread(() -> {
                importing = false;
                if (isFinishing() || isDestroyed()) return;
                if (!ok) {
                    Toast.makeText(this, R.string.wallpaper_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                choices.edit().putString("wallpaper_mode", "custom").apply();
                changed();
            });
        }, "wallpaper-import").start();
    }

    /**
     * Copies the picture into the private files as a screen-sized JPEG, so Home decodes a small file and the
     * picture stays even if the original is on a USB drive that goes away. False on any problem.
     */
    private boolean importWallpaper(Uri uri, File target, int screenW, int screenH) {
        File tmp = new File(target.getPath() + ".tmp");
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) return false;
                BitmapFactory.decodeStream(in, null, opts);
            }
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return false;
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = WallpaperSize.sampleSize(opts.outWidth, opts.outHeight, screenW, screenH);
            if (!WallpaperSize.accepted(opts.outWidth / opts.inSampleSize, opts.outHeight / opts.inSampleSize)) return false;
            Bitmap bmp;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) return false;
                bmp = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bmp == null) return false;
            int[] scale = WallpaperSize.coverScale(bmp.getWidth(), bmp.getHeight(), screenW, screenH);
            if (scale != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(bmp,
                        Math.max(1, (int) ((long) bmp.getWidth() * scale[1] / scale[0])),
                        Math.max(1, (int) ((long) bmp.getHeight() * scale[1] / scale[0])), true);
                if (scaled != bmp) bmp.recycle();
                bmp = scaled;
            }
            try (OutputStream out = new FileOutputStream(tmp)) {
                if (!bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)) return false;
            } finally {
                bmp.recycle();
            }
            if (!tmp.renameTo(target)) return false;
            return true;
        } catch (Exception | OutOfMemoryError e) {
            return false;
        } finally {
            if (tmp.exists()) tmp.delete();
        }
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
