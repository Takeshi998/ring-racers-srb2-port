package org.kartkrew.ringracers;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import org.libsdl.app.SDLActivity;

import java.io.File;

public final class GameActivity extends SDLActivity {
    private static final int REQUEST_CONTROL_LAYOUT = 2001;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TouchControlsView touchControls;

    @Override
    protected String[] getLibraries() {
        return new String[]{"main"};
    }

    @Override
    protected String[] getArguments() {
        File gameDirectory = new File(getFilesDir(), "game");

        // Hybrid shared storage: prefer /sdcard so RingRacers/addons is visible
        // in the file manager (needs All-files access on Android 11+).
        // Falls back to app-specific dirs instead of crashing the engine.
        File storageRoot = StorageHelper.resolveStorageRoot(this);
        try {
            StorageHelper.ensureTree(storageRoot);
        } catch (java.io.IOException e) {
            File fallback = getExternalFilesDir(null);
            if (fallback == null) {
                fallback = getFilesDir();
            }
            storageRoot = fallback;
            try {
                StorageHelper.ensureTree(storageRoot);
            } catch (java.io.IOException ignored) {
            }
        }

        return new String[]{
            "-waddir", gameDirectory.getAbsolutePath(),
            "-home", storageRoot.getAbsolutePath()
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enableImmersiveMode();

        if (!mBrokenLibraries && mLayout != null) {
            touchControls = new TouchControlsView(this);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            );
            mLayout.addView(touchControls, params);
        }
    }

    @Override
    protected void onPause() {
        if (touchControls != null) {
            touchControls.releaseAll();
        }
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveMode();
        } else if (touchControls != null) {
            touchControls.releaseAll();
        }
    }

    @Override
    public void onBackPressed() {
        if (mBrokenLibraries) {
            super.onBackPressed();
            return;
        }
        if (touchControls != null && touchControls.isEditMode()) {
            touchControls.exitEditMode(true);
            return;
        }
        if (SDLActivity.isScreenKeyboardShown()) {
            hideKeyboard();
            return;
        }
        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE);
        handler.postDelayed(() -> SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ESCAPE), 40L);
    }

    public void toggleKeyboard() {
        handler.post(() -> {
            if (SDLActivity.isScreenKeyboardShown()) {
                hideKeyboard();
            } else {
                showKeyboard();
            }
        });
    }

    public void showKeyboard() {
        handler.post(() -> SDLActivity.showTextInput(0, 0, 0, 0));
    }

    public void hideKeyboard() {
        handler.post(() -> SDLActivity.sendMessage(3, 0));
    }

    /** Opens a JSON control layout via the system picker.
     * Uses star-slash-star MIME: several file explorers don't handle application/json. */
    public void pickControlLayout() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            startActivityForResult(intent, REQUEST_CONTROL_LAYOUT);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTROL_LAYOUT && resultCode == RESULT_OK
                && data != null && data.getData() != null && touchControls != null) {
            int applied = touchControls.importProfile(data.getData());
            android.widget.Toast.makeText(this,
                applied >= 0 ? "Botones aplicados: " + applied : "Archivo inválido",
                android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void enableImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }
}
