package org.kartkrew.ringracers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService extractor = Executors.newSingleThreadExecutor();
    private ProgressBar progress;
    private TextView status;
    private Button retry;

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

        return root;
    }

    private void prepareGame() {
        retry.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        status.setText("Preparing game data");

        extractor.execute(() -> {
            try {
                AssetExtractor.prepare(this, (completed, total, name) -> runOnUiThread(() -> {
                    progress.setMax(total);
                    progress.setProgress(completed);
                    status.setText(name);
                }));
                AssetExtractor.prepareUserHome(this);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        startActivity(new Intent(this, GameActivity.class));
                        finish();
                    }
                });
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    status.setText(exception.getMessage());
                    progress.setVisibility(View.GONE);
                    retry.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
