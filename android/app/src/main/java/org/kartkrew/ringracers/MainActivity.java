package org.kartkrew.ringracers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_STORAGE_LEGACY = 1001;
    private static final int REQUEST_MANAGE_STORAGE = 1002;
    private static final int REQUEST_OPEN_TREE = 1003;
    private final ExecutorService extractor = Executors.newSingleThreadExecutor();
    private ProgressBar progress;
    private TextView status;
    private TextView pathView;
    private Button retry;
    private Button permissionButton;
    private Button folderButton;
    private Button playButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(createLoadingView());
        prepareGame();
    }

    @Override
    protected void onDestroy() {
        extractor.shutdownNow();
        super.onDestroy();
    }

    private View createLoadingView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(48), dp(32), dp(48), dp(32));
        root.setBackgroundColor(Color.rgb(13, 15, 18));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        status = new TextView(this);
        status.setText("Preparing game data");
        status.setTextColor(Color.rgb(194, 201, 207));
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.topMargin = dp(16);
        root.addView(status, statusParams);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(false);
        progress.setMax(16);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(244, 81, 77)));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(360), dp(8));
        progressParams.topMargin = dp(18);
        root.addView(progress, progressParams);

        retry = new Button(this);
        retry.setText("Retry");
        retry.setAllCaps(false);
        retry.setVisibility(View.GONE);
        retry.setOnClickListener(view -> prepareGame());
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(dp(120), dp(48));
        retryParams.topMargin = dp(20);
        root.addView(retry, retryParams);

        permissionButton = new Button(this);
        permissionButton.setText("Dar acceso a archivos");
        permissionButton.setAllCaps(false);
        permissionButton.setVisibility(View.GONE);
        permissionButton.setOnClickListener(view -> requestSharedAccess());
        LinearLayout.LayoutParams permParams = new LinearLayout.LayoutParams(dp(240), dp(48));
        permParams.topMargin = dp(12);
        root.addView(permissionButton, permParams);

        folderButton = new Button(this);
        folderButton.setText("Elegir carpeta");
        folderButton.setAllCaps(false);
        folderButton.setOnClickListener(view -> pickFolder());
        LinearLayout.LayoutParams folderParams = new LinearLayout.LayoutParams(dp(240), dp(48));
        folderParams.topMargin = dp(12);
        root.addView(folderButton, folderParams);

        playButton = new Button(this);
        playButton.setText("Jugar");
        playButton.setAllCaps(false);
        playButton.setVisibility(View.GONE);
        playButton.setOnClickListener(view -> launchGame());
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(dp(240), dp(56));
        playParams.topMargin = dp(12);
        root.addView(playButton, playParams);

        pathView = new TextView(this);
        pathView.setText("");
        pathView.setTextColor(Color.rgb(140, 150, 160));
        pathView.setTextSize(12);
        pathView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        pathParams.topMargin = dp(12);
        root.addView(pathView, pathParams);

        return root;
    }

    private void requestSharedAccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
            } else {
                requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_STORAGE_LEGACY);
            }
        } catch (Exception e) {
            status.setText("No se pudo abrir el ajuste: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_STORAGE) {
            prepareGame();
        } else if (requestCode == REQUEST_OPEN_TREE && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            onFolderPicked(data);
        }
    }

    /** Lets the user pick the storage root (default: /sdcard). Engine appends RingRacers. */
    private void pickFolder() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_OPEN_TREE);
        } catch (Exception e) {
            status.setText("Sin selector de carpetas: " + e.getMessage());
        }
    }

    private void onFolderPicked(Intent data) {
        android.net.Uri tree = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(tree,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception ignored) {
        }
        File realPath = resolvePrimaryPath(tree);
        if (realPath != null) {
            StorageHelper.setCustomRoot(this, realPath);
            try {
                File storageRoot = AssetExtractor.prepareUserHome(this);
                showReady(StorageHelper.getGameDir(storageRoot), StorageHelper.hasSharedAccess());
            } catch (IOException e) {
                status.setText(e.getMessage());
                retry.setVisibility(View.VISIBLE);
            }
        } else {
            status.setText("No se pudo usar esa carpeta (solo almacenamiento principal).");
        }
    }

    /**
     * Resolves a SAF tree URI to a real path, primary volume only
     * ({@code primary:Rel/Path} -> {@code /storage/emulated/0/Rel/Path}).
     * The native engine needs a filesystem path, so SD cards / OTG that the
     * OS does not expose by path cannot be used.
     */
    private File resolvePrimaryPath(android.net.Uri tree) {
        try {
            String docId = android.provider.DocumentsContract.getTreeDocumentId(tree);
            if (docId == null || !docId.startsWith("primary:")) {
                return null;
            }
            String rel = docId.substring("primary:".length());
            File base = Environment.getExternalStorageDirectory();
            if (base == null) {
                return null;
            }
            File dir = rel.isEmpty() ? base : new File(base, rel);
            if (!dir.mkdirs() && !dir.isDirectory()) {
                return null;
            }
            return dir.canWrite() ? dir : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_LEGACY) {
            prepareGame();
        }
    }

    private boolean needsSharedPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return !Environment.isExternalStorageManager();
        }
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED;
    }

    private void prepareGame() {
        retry.setVisibility(View.GONE);
        permissionButton.setVisibility(View.GONE);
        playButton.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        status.setText("Preparing game data");

        extractor.execute(() -> {
            try {
                AssetExtractor.prepare(this, (completed, total, name) -> runOnUiThread(() -> {
                    progress.setMax(total);
                    progress.setProgress(completed);
                    status.setText(name);
                }));
                File storageRoot = AssetExtractor.prepareUserHome(this);
                File gameDir = StorageHelper.getGameDir(storageRoot);
                final boolean shared = StorageHelper.hasSharedAccess();
                runOnUiThread(() -> showReady(gameDir, shared));
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    status.setText(exception.getMessage());
                    progress.setVisibility(View.GONE);
                    retry.setVisibility(View.VISIBLE);
                    if (needsSharedPermission()) {
                        permissionButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    /** Ready screen: user picks folder (optional) and taps Jugar. No auto-launch. */
    private void showReady(File gameDir, boolean shared) {
        progress.setVisibility(View.GONE);
        status.setText(shared ? "Listo: " + gameDir.getAbsolutePath()
            : "Listo (privado): " + gameDir.getAbsolutePath());
        pathView.setText("Datos: " + gameDir.getAbsolutePath()
            + "\nAddons: " + new File(gameDir, "addons").getAbsolutePath());
        playButton.setVisibility(View.VISIBLE);
    }

    private void launchGame() {
        if (!isFinishing() && !isDestroyed()) {
            startActivity(new Intent(this, GameActivity.class));
            finish();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
