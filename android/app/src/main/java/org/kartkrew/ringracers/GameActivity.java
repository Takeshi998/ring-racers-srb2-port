package org.kartkrew.ringracers;

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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TouchControlsView touchControls;

    @Override
    protected String[] getLibraries() {
        return new String[]{"main"};
    }

    @Override
    protected String[] getArguments() {
        File gameDirectory = new File(getFilesDir(), "game");
        File userRoot = new File(getFilesDir(), "user");
        new File(userRoot, ".ringracers").mkdirs();
        new File(userRoot, ".ringracers/addons").mkdirs();
        new File(userRoot, "addons").mkdirs();
        return new String[]{
            "-waddir", gameDirectory.getAbsolutePath(),
            "-home", userRoot.getAbsolutePath()
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
