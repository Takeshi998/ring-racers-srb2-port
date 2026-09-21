package org.kartkrew.ringracers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TouchControlsView extends View {
    private static final String PREFS_NAME = "ringracers_touch_controls";
    private static final String PREFS_META = "ringracers_touch_controls_meta";
    private static final String KEY_ACTIVE_PROFILE = "active_profile";
    private static final int NUM_PROFILES = 3;
    /** ids opcionales (visibles/ocultables desde el menú lateral) */
    private static final String[] OPTIONAL_IDS = {"chat", "rank", "console", "lua1", "lua2", "lua3", "cruise"};
    private static final String[] OPTIONAL_NAMES = {"CHAT", "RANK", "CON", "LUA 1", "LUA 2", "LUA 3", "CRUISE"};
    private static final int OUTLINE_COLOR = Color.argb(185, 255, 255, 255);
    private static final int LABEL_COLOR = Color.WHITE;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dashedOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bannerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowPath = new Path();

    private final List<TouchElement> allElements = new ArrayList<>();
    private final List<TouchElement> actionButtons = new ArrayList<>();
    private final Map<Integer, Set<Integer>> pointerKeys = new HashMap<>();
    private final Map<Integer, Integer> keyReferences = new HashMap<>();
    private final Set<Integer> keyboardPointers = new HashSet<>();
    private final Set<Integer> chatPointers = new HashSet<>();
    private final Set<Integer> editPointers = new HashSet<>();

    private final Map<Integer, TouchElement> dragPointers = new HashMap<>();
    private final Map<Integer, Float> dragOffsetX = new HashMap<>();
    private final Map<Integer, Float> dragOffsetY = new HashMap<>();
    private final Set<Integer> cruisePointers = new HashSet<>();
    private boolean cruiseActive = false;

    private TouchElement dpadElement;
    private TouchElement pauseButton;
    private TouchElement keyboardButton;
    private TouchElement editButton;
    private TouchElement chatButton;
    private TouchElement cruiseButton;
    private TouchElement resetButton;
    private TouchElement doneButton;
    private TouchElement sizeMinusButton;
    private TouchElement sizePlusButton;
    private TouchElement menuButton;
    private TouchElement selectedElement;

    private boolean editMode = false;
    private int activeProfile = 0;

    TouchControlsView(Context context) {
        super(context);
        setFocusable(false);
        setClickable(true);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(dp(2));
        outlinePaint.setColor(OUTLINE_COLOR);

        dashedOutlinePaint.setStyle(Paint.Style.STROKE);
        dashedOutlinePaint.setStrokeWidth(dp(2));
        dashedOutlinePaint.setColor(Color.argb(220, 255, 215, 0));
        dashedOutlinePaint.setPathEffect(new DashPathEffect(new float[]{dp(6), dp(4)}, 0));

        textPaint.setColor(LABEL_COLOR);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));

        bannerPaint.setStyle(Paint.Style.FILL);

        initElements();
    }

    private void initElements() {
        allElements.clear();
        actionButtons.clear();

        dpadElement = new TouchElement("dpad", "D-PAD", "", 0, 0.16f, 0.72f, 0.245f,
            Color.rgb(28, 31, 35), ElementKind.DPAD);
        allElements.add(dpadElement);

        TouchElement go = new TouchElement("go", "GO", "A", KeyEvent.KEYCODE_A, 0.88f, 0.76f, 0.105f,
            Color.rgb(45, 159, 93), ElementKind.ACTION);
        TouchElement drift = new TouchElement("drift", "DRIFT", "R", KeyEvent.KEYCODE_S, 0.72f, 0.80f, 0.082f,
            Color.rgb(28, 145, 170), ElementKind.ACTION);
        TouchElement item = new TouchElement("item", "ITEM", "L", KeyEvent.KEYCODE_SPACE, 0.79f, 0.53f, 0.078f,
            Color.rgb(224, 169, 47), ElementKind.ACTION);
        TouchElement brake = new TouchElement("brake", "BRAKE", "X", KeyEvent.KEYCODE_D, 0.945f, 0.57f, 0.076f,
            Color.rgb(215, 72, 65), ElementKind.ACTION);
        TouchElement spin = new TouchElement("spin", "SPIN", "C", KeyEvent.KEYCODE_Q, 0.64f, 0.60f, 0.070f,
            Color.rgb(62, 101, 181), ElementKind.ACTION);
        TouchElement look = new TouchElement("look", "LOOK", "B", KeyEvent.KEYCODE_SHIFT_LEFT, 0.58f, 0.82f, 0.062f,
            Color.rgb(94, 102, 110), ElementKind.ACTION);
        TouchElement bail = new TouchElement("bail", "BAIL", "Y", KeyEvent.KEYCODE_V, 0.86f, 0.32f, 0.065f,
            Color.rgb(202, 76, 111), ElementKind.ACTION);
        TouchElement vote = new TouchElement("vote", "VOTE", "Z", KeyEvent.KEYCODE_Z, 0.96f, 0.31f, 0.052f,
            Color.rgb(105, 111, 118), ElementKind.ACTION);

        chatButton = new TouchElement("chat", "CHAT", "T", KeyEvent.KEYCODE_T, 0.76f, 0.33f, 0.065f,
            Color.rgb(142, 68, 173), ElementKind.CHAT);

        // Extra buttons. RANK (TAB) works out of the box (gc_rankings default).
        // CONSOLE (`), LUA 1/2/3 ('1'/'2'/'3') must be bound in-game under
        // Options > Controls if the build has no default for them
        // (gc_console default only exists in DEVELOP builds, gc_lua1-3 have none).
        // SDL maps scancodes 1..9 to '1'..'9' (src/sdl/i_video.cpp) and nothing
        // uses those keys by default, so they are safe slots for mod actions.
        TouchElement rank = new TouchElement("rank", "RANK", "TAB", KeyEvent.KEYCODE_TAB, 0.06f, 0.22f, 0.055f,
            Color.rgb(52, 120, 140), ElementKind.ACTION);
        TouchElement console = new TouchElement("console", "CON", "`", KeyEvent.KEYCODE_GRAVE, 0.145f, 0.22f, 0.055f,
            Color.rgb(60, 60, 66), ElementKind.ACTION);
        TouchElement lua1 = new TouchElement("lua1", "1", "1", KeyEvent.KEYCODE_1, 0.06f, 0.37f, 0.052f,
            Color.rgb(128, 90, 160), ElementKind.ACTION);
        TouchElement lua2 = new TouchElement("lua2", "2", "2", KeyEvent.KEYCODE_2, 0.145f, 0.37f, 0.052f,
            Color.rgb(128, 90, 160), ElementKind.ACTION);
        TouchElement lua3 = new TouchElement("lua3", "3", "3", KeyEvent.KEYCODE_3, 0.102f, 0.50f, 0.052f,
            Color.rgb(128, 90, 160), ElementKind.ACTION);
        // Cruise: latching GO. Single tap toggles a held KEYCODE_A so the kart
        // accelerates without keeping a finger down. Tap again to release.
        cruiseButton = new TouchElement("cruise", "CRUISE", "AUTO", KeyEvent.KEYCODE_A, 0.965f, 0.90f, 0.062f,
            Color.rgb(45, 159, 93), ElementKind.CRUISE);

        actionButtons.add(go);
        actionButtons.add(drift);
        actionButtons.add(item);
        actionButtons.add(brake);
        actionButtons.add(spin);
        actionButtons.add(look);
        actionButtons.add(bail);
        actionButtons.add(vote);
        actionButtons.add(chatButton);
        actionButtons.add(rank);
        actionButtons.add(console);
        actionButtons.add(lua1);
        actionButtons.add(lua2);
        actionButtons.add(lua3);
        actionButtons.add(cruiseButton);
        allElements.addAll(actionButtons);

        editButton = new TouchElement("edit", "EDIT", "", 0, 0.42f, 0.085f, 0.052f,
            Color.rgb(70, 80, 95), ElementKind.EDIT);
        keyboardButton = new TouchElement("keyboard", "", "", 0, 0.50f, 0.085f, 0.052f,
            Color.rgb(53, 58, 64), ElementKind.KEYBOARD);
        pauseButton = new TouchElement("pause", "", "", KeyEvent.KEYCODE_ESCAPE, 0.58f, 0.085f, 0.052f,
            Color.rgb(53, 58, 64), ElementKind.PAUSE);

        allElements.add(editButton);
        allElements.add(keyboardButton);
        allElements.add(pauseButton);

        resetButton = new TouchElement("reset", "RESET", "", 0, 0.35f, 0.085f, 0.055f,
            Color.rgb(192, 57, 43), ElementKind.RESET);
        doneButton = new TouchElement("done", "DONE", "", 0, 0.65f, 0.085f, 0.055f,
            Color.rgb(39, 174, 96), ElementKind.DONE);
        sizeMinusButton = new TouchElement("sizeminus", "A-", "", 0, 0.40f, 0.20f, 0.050f,
            Color.rgb(70, 80, 95), ElementKind.SIZE_MINUS);
        menuButton = new TouchElement("menu", "MENU", "", 0, 0.50f, 0.20f, 0.050f,
            Color.rgb(70, 80, 95), ElementKind.MENU);
        sizePlusButton = new TouchElement("sizeplus", "A+", "", 0, 0.60f, 0.20f, 0.050f,
            Color.rgb(70, 80, 95), ElementKind.SIZE_PLUS);
        activeProfile = getContext().getSharedPreferences(PREFS_META, Context.MODE_PRIVATE)
            .getInt(KEY_ACTIVE_PROFILE, 0);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        loadLayout(width, height);
    }

    private String prefsName() {
        // Perfil 0 usa el archivo original (conserva layouts existentes).
        return activeProfile == 0 ? PREFS_NAME : PREFS_NAME + "_p" + activeProfile;
    }

    private boolean isOptional(String id) {
        for (String optional : OPTIONAL_IDS) {
            if (optional.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private TouchElement findElementById(String id) {
        for (TouchElement element : allElements) {
            if (element.id.equals(id)) {
                return element;
            }
        }
        return null;
    }

    private void loadLayout(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        SharedPreferences prefs = getContext().getSharedPreferences(prefsName(), Context.MODE_PRIVATE);
        for (TouchElement element : allElements) {
            element.normX = prefs.getFloat(element.id + "_normX", element.defaultNormX);
            element.normY = prefs.getFloat(element.id + "_normY", element.defaultNormY);
            element.sizeFactor = prefs.getFloat(element.id + "_size", 1f);
            element.visible = !isOptional(element.id) || prefs.getBoolean(element.id + "_visible", true);
            element.updatePixelCoords(width, height);
        }
        resetButton.updatePixelCoords(width, height);
        doneButton.updatePixelCoords(width, height);
        sizeMinusButton.updatePixelCoords(width, height);
        menuButton.updatePixelCoords(width, height);
        sizePlusButton.updatePixelCoords(width, height);
    }

    private void saveLayout() {
        SharedPreferences prefs = getContext().getSharedPreferences(prefsName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (TouchElement element : allElements) {
            editor.putFloat(element.id + "_normX", element.normX);
            editor.putFloat(element.id + "_normY", element.normY);
            editor.putFloat(element.id + "_size", element.sizeFactor);
            if (isOptional(element.id)) {
                editor.putBoolean(element.id + "_visible", element.visible);
            }
        }
        editor.apply();
    }

    private void resetLayout() {
        SharedPreferences prefs = getContext().getSharedPreferences(prefsName(), Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        for (TouchElement element : allElements) {
            element.resetToDefault();
            element.updatePixelCoords(getWidth(), getHeight());
        }
        invalidate();
    }

    private void switchProfile(int profile) {
        if (profile == activeProfile) {
            return;
        }
        saveLayout();
        activeProfile = profile;
        getContext().getSharedPreferences(PREFS_META, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACTIVE_PROFILE, profile).apply();
        selectedElement = null;
        loadLayout(getWidth(), getHeight());
        invalidate();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void enterEditMode() {
        releaseAll();
        editMode = true;
        if (selectedElement == null) {
            selectedElement = dpadElement;
        }
        dragPointers.clear();
        dragOffsetX.clear();
        dragOffsetY.clear();
        invalidate();
    }

    public void exitEditMode(boolean save) {
        if (save) {
            saveLayout();
        }
        editMode = false;
        dragPointers.clear();
        dragOffsetX.clear();
        dragOffsetY.clear();
        invalidate();
    }

    private void adjustSelectedSize(float factor) {
        if (selectedElement == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        selectedElement.sizeFactor = Math.max(0.5f, Math.min(2.0f, selectedElement.sizeFactor * factor));
        selectedElement.updatePixelCoords(getWidth(), getHeight());
        saveLayout();
        invalidate();
    }

    /** Menú lateral: perfiles + botones extra visibles. */
    private void openSideMenu() {
        Context context = getContext();
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        android.widget.TextView profileTitle = new android.widget.TextView(context);
        profileTitle.setText("Perfil de controles");
        profileTitle.setTextSize(16);
        layout.addView(profileTitle);

        android.widget.RadioGroup profiles = new android.widget.RadioGroup(context);
        profiles.setOrientation(android.widget.RadioGroup.HORIZONTAL);
        for (int i = 0; i < NUM_PROFILES; i++) {
            android.widget.RadioButton option = new android.widget.RadioButton(context);
            option.setText("Perfil " + (i + 1));
            option.setId(1000 + i);
            option.setChecked(i == activeProfile);
            profiles.addView(option);
        }
        layout.addView(profiles);

        android.widget.TextView buttonsTitle = new android.widget.TextView(context);
        buttonsTitle.setText("Botones extra visibles");
        buttonsTitle.setTextSize(16);
        buttonsTitle.setPadding(0, pad / 2, 0, 0);
        layout.addView(buttonsTitle);

        final android.widget.CheckBox[] boxes = new android.widget.CheckBox[OPTIONAL_IDS.length];
        for (int i = 0; i < OPTIONAL_IDS.length; i++) {
            TouchElement element = findElementById(OPTIONAL_IDS[i]);
            android.widget.CheckBox box = new android.widget.CheckBox(context);
            box.setText(OPTIONAL_NAMES[i]);
            box.setChecked(element == null || element.visible);
            boxes[i] = box;
            layout.addView(box);
        }

        new android.app.AlertDialog.Builder(context)
            .setTitle("Controles")
            .setView(layout)
            .setPositiveButton("Aplicar", (dialog, which) -> {
                int checkedProfile = profiles.getCheckedRadioButtonId() - 1000;
                for (int i = 0; i < OPTIONAL_IDS.length; i++) {
                    TouchElement element = findElementById(OPTIONAL_IDS[i]);
                    if (element != null) {
                        element.visible = boxes[i].isChecked();
                    }
                }
                saveLayout();
                if (checkedProfile >= 0 && checkedProfile < NUM_PROFILES
                        && checkedProfile != activeProfile) {
                    // switchProfile guarda el perfil actual y carga el nuevo
                    // (la visibilidad recién marcada ya quedó guardada arriba,
                    //  se reaplica tras el cambio).
                    boolean[] wanted = new boolean[OPTIONAL_IDS.length];
                    for (int i = 0; i < wanted.length; i++) {
                        wanted[i] = boxes[i].isChecked();
                    }
                    switchProfile(checkedProfile);
                    for (int i = 0; i < OPTIONAL_IDS.length; i++) {
                        TouchElement element = findElementById(OPTIONAL_IDS[i]);
                        if (element != null) {
                            element.visible = wanted[i];
                        }
                    }
                    saveLayout();
                }
                invalidate();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dpadElement.visible) {
            drawDpad(canvas);
        }
        for (TouchElement button : actionButtons) {
            if (button.visible) {
                drawActionButton(canvas, button);
            }
        }
        drawActionButton(canvas, editButton);
        drawActionButton(canvas, keyboardButton);
        drawActionButton(canvas, pauseButton);

        if (editMode) {
            drawEditModeOverlay(canvas);
        }
    }

    private void drawEditModeOverlay(Canvas canvas) {
        float bannerWidth = getWidth() * 0.44f;
        float bannerHeight = getHeight() * 0.08f;
        float bannerX = (getWidth() - bannerWidth) * 0.5f;
        float bannerY = dp(8);
        RectF bannerRect = new RectF(bannerX, bannerY, bannerX + bannerWidth, bannerY + bannerHeight);

        bannerPaint.setColor(Color.argb(210, 20, 24, 30));
        canvas.drawRoundRect(bannerRect, dp(8), dp(8), bannerPaint);
        outlinePaint.setColor(Color.argb(200, 255, 215, 0));
        outlinePaint.setStrokeWidth(dp(1.5f));
        canvas.drawRoundRect(bannerRect, dp(8), dp(8), outlinePaint);
        outlinePaint.setColor(OUTLINE_COLOR);
        outlinePaint.setStrokeWidth(dp(2));

        textPaint.setTextSize(Math.max(dp(10), bannerHeight * 0.40f));
        textPaint.setColor(Color.argb(255, 255, 215, 0));
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = bannerRect.centerY() - (metrics.ascent + metrics.descent) * 0.5f;
        canvas.drawText("CUSTOMIZE CONTROLS - DRAG TO MOVE", bannerRect.centerX(), baseline, textPaint);
        textPaint.setColor(LABEL_COLOR);

        drawActionButton(canvas, resetButton);
        drawActionButton(canvas, doneButton);
        drawActionButton(canvas, sizeMinusButton);
        drawActionButton(canvas, menuButton);
        drawActionButton(canvas, sizePlusButton);
    }

    private void drawDpad(Canvas canvas) {
        boolean leftPressed = isPressed(KeyEvent.KEYCODE_DPAD_LEFT);
        boolean rightPressed = isPressed(KeyEvent.KEYCODE_DPAD_RIGHT);
        boolean upPressed = isPressed(KeyEvent.KEYCODE_DPAD_UP);
        boolean downPressed = isPressed(KeyEvent.KEYCODE_DPAD_DOWN);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(92, 28, 31, 35));
        canvas.drawCircle(dpadElement.centerX, dpadElement.centerY, dpadElement.radius, fillPaint);

        if (editMode) {
            canvas.drawCircle(dpadElement.centerX, dpadElement.centerY, dpadElement.radius,
                dpadElement == selectedElement ? dashedOutlinePaint : outlinePaint);
        } else {
            canvas.drawCircle(dpadElement.centerX, dpadElement.centerY, dpadElement.radius, outlinePaint);
        }

        float arrowDistance = dpadElement.radius * 0.55f;
        float arrowSize = dpadElement.radius * 0.20f;
        drawArrow(canvas, dpadElement.centerX - arrowDistance, dpadElement.centerY, arrowSize, 180f, leftPressed);
        drawArrow(canvas, dpadElement.centerX + arrowDistance, dpadElement.centerY, arrowSize, 0f, rightPressed);
        drawArrow(canvas, dpadElement.centerX, dpadElement.centerY - arrowDistance, arrowSize, -90f, upPressed);
        drawArrow(canvas, dpadElement.centerX, dpadElement.centerY + arrowDistance, arrowSize, 90f, downPressed);

        fillPaint.setColor(Color.argb(130, 255, 255, 255));
        canvas.drawCircle(dpadElement.centerX, dpadElement.centerY, dpadElement.radius * 0.12f, fillPaint);
    }

    private void drawArrow(Canvas canvas, float centerX, float centerY, float size, float rotation, boolean pressed) {
        arrowPath.reset();
        arrowPath.moveTo(size, 0f);
        arrowPath.lineTo(-size * 0.55f, -size * 0.72f);
        arrowPath.lineTo(-size * 0.55f, size * 0.72f);
        arrowPath.close();
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(rotation);
        fillPaint.setColor(pressed ? Color.argb(235, 244, 193, 70) : Color.argb(155, 255, 255, 255));
        canvas.drawPath(arrowPath, fillPaint);
        canvas.restore();
    }

    private void drawActionButton(Canvas canvas, TouchElement button) {
        if (button == null) {
            return;
        }
        boolean isDragged = editMode && dragPointers.containsValue(button);
        boolean pressed = false;
        if (!editMode) {
            if (button.kind == ElementKind.KEYBOARD) {
                pressed = !keyboardPointers.isEmpty();
            } else if (button.kind == ElementKind.CHAT) {
                pressed = !chatPointers.isEmpty();
            } else if (button.kind == ElementKind.EDIT) {
                pressed = !editPointers.isEmpty();
            } else if (button.kind == ElementKind.CRUISE) {
                pressed = cruiseActive;
            } else {
                pressed = isPressed(button.keyCode);
            }
        }

        int alpha = pressed ? 230 : (editMode ? 170 : 140);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(alpha, Color.red(button.color), Color.green(button.color), Color.blue(button.color)));
        canvas.drawCircle(button.centerX, button.centerY, button.radius, fillPaint);

        if (editMode && (button.kind != ElementKind.RESET && button.kind != ElementKind.DONE
                && button.kind != ElementKind.SIZE_MINUS && button.kind != ElementKind.SIZE_PLUS
                && button.kind != ElementKind.MENU)) {
            canvas.drawCircle(button.centerX, button.centerY, button.radius,
                button == selectedElement ? dashedOutlinePaint : outlinePaint);
        } else {
            canvas.drawCircle(button.centerX, button.centerY, button.radius, outlinePaint);
        }

        if (button.kind == ElementKind.PAUSE) {
            drawPauseIcon(canvas, button.centerX, button.centerY, button.radius);
            return;
        }

        if (button.kind == ElementKind.KEYBOARD) {
            drawKeyboardIcon(canvas, button.centerX, button.centerY, button.radius);
            return;
        }

        if (button.kind == ElementKind.EDIT) {
            float labelSize = Math.max(dp(9), button.radius * 0.38f);
            textPaint.setTextSize(labelSize);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float baseline = button.centerY - (metrics.ascent + metrics.descent) * 0.5f;
            canvas.drawText("EDIT", button.centerX, baseline, textPaint);
            return;
        }

        if (button.kind == ElementKind.RESET || button.kind == ElementKind.DONE
                || button.kind == ElementKind.SIZE_MINUS || button.kind == ElementKind.SIZE_PLUS
                || button.kind == ElementKind.MENU) {
            float labelSize = Math.max(dp(9), button.radius * 0.36f);
            textPaint.setTextSize(labelSize);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float baseline = button.centerY - (metrics.ascent + metrics.descent) * 0.5f;
            canvas.drawText(button.label, button.centerX, baseline, textPaint);
            return;
        }

        float labelSize = Math.max(dp(9), button.radius * 0.34f);
        textPaint.setTextSize(labelSize);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = button.centerY - (metrics.ascent + metrics.descent) * 0.5f - button.radius * 0.08f;
        canvas.drawText(button.label, button.centerX, baseline, textPaint);

        textPaint.setTextSize(Math.max(dp(7), button.radius * 0.22f));
        textPaint.setColor(Color.argb(205, 255, 255, 255));
        canvas.drawText(button.code, button.centerX, button.centerY + button.radius * 0.48f, textPaint);
        textPaint.setColor(LABEL_COLOR);
    }

    private void drawPauseIcon(Canvas canvas, float cx, float cy, float radius) {
        fillPaint.setColor(LABEL_COLOR);
        float barWidth = radius * 0.18f;
        float barHeight = radius * 0.70f;
        float gap = radius * 0.18f;
        canvas.drawRoundRect(new RectF(
            cx - gap - barWidth,
            cy - barHeight / 2f,
            cx - gap,
            cy + barHeight / 2f
        ), barWidth * 0.25f, barWidth * 0.25f, fillPaint);
        canvas.drawRoundRect(new RectF(
            cx + gap,
            cy - barHeight / 2f,
            cx + gap + barWidth,
            cy + barHeight / 2f
        ), barWidth * 0.25f, barWidth * 0.25f, fillPaint);
    }

    private void drawKeyboardIcon(Canvas canvas, float cx, float cy, float radius) {
        float kw = radius * 1.18f;
        float kh = radius * 0.78f;
        RectF kbRect = new RectF(cx - kw / 2f, cy - kh / 2f, cx + kw / 2f, cy + kh / 2f);

        outlinePaint.setStrokeWidth(dp(1.5f));
        canvas.drawRoundRect(kbRect, dp(3f), dp(3f), outlinePaint);
        outlinePaint.setStrokeWidth(dp(2f));

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(LABEL_COLOR);

        float dotRadius = dp(1.2f);
        float row1Y = cy - kh * 0.22f;
        float[] row1XOffsets = {-kw * 0.30f, -kw * 0.10f, kw * 0.10f, kw * 0.30f};
        for (float xOffset : row1XOffsets) {
            canvas.drawCircle(cx + xOffset, row1Y, dotRadius, fillPaint);
        }

        float row2Y = cy + kh * 0.04f;
        float[] row2XOffsets = {-kw * 0.22f, 0f, kw * 0.22f};
        for (float xOffset : row2XOffsets) {
            canvas.drawCircle(cx + xOffset, row2Y, dotRadius, fillPaint);
        }

        float row3Y = cy + kh * 0.28f;
        float spaceHalfW = kw * 0.26f;
        float spaceHalfH = dp(1.1f);
        canvas.drawRoundRect(new RectF(cx - spaceHalfW, row3Y - spaceHalfH, cx + spaceHalfW, row3Y + spaceHalfH),
            dp(1f), dp(1f), fillPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        if (editMode) {
            return handleEditTouchEvent(event, action, actionIndex);
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                updatePointer(event.getPointerId(actionIndex), event.getX(actionIndex), event.getY(actionIndex));
                break;
            case MotionEvent.ACTION_MOVE:
                for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
                    updatePointer(event.getPointerId(pointerIndex), event.getX(pointerIndex), event.getY(pointerIndex));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                clearPointer(event.getPointerId(actionIndex));
                if (action == MotionEvent.ACTION_UP) {
                    performClick();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                releaseAll();
                break;
            default:
                break;
        }
        return true;
    }

    private boolean handleEditTouchEvent(MotionEvent event, int action, int actionIndex) {
        int pointerId = event.getPointerId(actionIndex);
        float px = event.getX(actionIndex);
        float py = event.getY(actionIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (resetButton.contains(px, py)) {
                    resetLayout();
                    return true;
                }
                if (doneButton.contains(px, py)) {
                    exitEditMode(true);
                    return true;
                }
                if (sizeMinusButton.contains(px, py)) {
                    adjustSelectedSize(1f / 1.15f);
                    return true;
                }
                if (sizePlusButton.contains(px, py)) {
                    adjustSelectedSize(1.15f);
                    return true;
                }
                if (menuButton.contains(px, py)) {
                    openSideMenu();
                    return true;
                }

                TouchElement hit = findElementAt(px, py);
                if (hit != null && hit.kind != ElementKind.RESET && hit.kind != ElementKind.DONE) {
                    selectedElement = hit;
                    dragPointers.put(pointerId, hit);
                    dragOffsetX.put(pointerId, hit.centerX - px);
                    dragOffsetY.put(pointerId, hit.centerY - py);
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    TouchElement dragged = dragPointers.get(pId);
                    if (dragged != null) {
                        float curX = event.getX(i);
                        float curY = event.getY(i);
                        float offX = dragOffsetX.getOrDefault(pId, 0f);
                        float offY = dragOffsetY.getOrDefault(pId, 0f);

                        float newX = curX + offX;
                        float newY = curY + offY;

                        // Clamp to screen boundaries
                        float minX = dragged.radius;
                        float maxX = getWidth() - dragged.radius;
                        float minY = dragged.radius;
                        float maxY = getHeight() - dragged.radius;

                        dragged.centerX = Math.max(minX, Math.min(maxX, newX));
                        dragged.centerY = Math.max(minY, Math.min(maxY, newY));
                        dragged.normX = dragged.centerX / getWidth();
                        dragged.normY = dragged.centerY / getHeight();
                    }
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                dragPointers.remove(pointerId);
                dragOffsetX.remove(pointerId);
                dragOffsetY.remove(pointerId);
                saveLayout();
                if (action == MotionEvent.ACTION_UP) {
                    performClick();
                }
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                dragPointers.clear();
                dragOffsetX.clear();
                dragOffsetY.clear();
                invalidate();
                break;
        }
        return true;
    }

    private TouchElement findElementAt(float px, float py) {
        // Check buttons first (higher priority than dpad area)
        for (TouchElement element : allElements) {
            if (element.visible && element.kind != ElementKind.DPAD && element.contains(px, py)) {
                return element;
            }
        }
        if (dpadElement.visible && dpadElement.contains(px, py)) {
            return dpadElement;
        }
        return null;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    void releaseAll() {
        keyboardPointers.clear();
        chatPointers.clear();
        editPointers.clear();
        cruisePointers.clear();
        if (cruiseActive) {
            cruiseActive = false;
        }
        List<Integer> pressedKeys = new ArrayList<>(keyReferences.keySet());
        pointerKeys.clear();
        keyReferences.clear();
        for (int keyCode : pressedKeys) {
            SDLActivity.onNativeKeyUp(keyCode);
        }
        invalidate();
    }

    private void updatePointer(int pointerId, float pointerX, float pointerY) {
        // Keyboard button check
        boolean inKeyboard = keyboardButton != null && keyboardButton.contains(pointerX, pointerY);
        if (inKeyboard) {
            if (!keyboardPointers.contains(pointerId)) {
                keyboardPointers.add(pointerId);
                toggleKeyboard();
            }
        } else {
            keyboardPointers.remove(pointerId);
        }

        // Edit button check
        boolean inEdit = editButton != null && editButton.contains(pointerX, pointerY);
        if (inEdit) {
            if (!editPointers.contains(pointerId)) {
                editPointers.add(pointerId);
                enterEditMode();
                return;
            }
        } else {
            editPointers.remove(pointerId);
        }

        // Chat (T) button check
        boolean inChat = chatButton != null && chatButton.visible && chatButton.contains(pointerX, pointerY);
        if (inChat) {
            if (!chatPointers.contains(pointerId)) {
                chatPointers.add(pointerId);
                triggerChat();
            }
        } else {
            chatPointers.remove(pointerId);
        }

        // Cruise (latching GO) check: tap toggles, no hold needed.
        boolean inCruise = cruiseButton != null && cruiseButton.visible && cruiseButton.contains(pointerX, pointerY);
        if (inCruise) {
            if (!cruisePointers.contains(pointerId)) {
                cruisePointers.add(pointerId);
                setCruise(!cruiseActive);
            }
        } else {
            cruisePointers.remove(pointerId);
        }

        Set<Integer> nextKeys = keysAt(pointerX, pointerY);
        Set<Integer> previousKeys = pointerKeys.getOrDefault(pointerId, Collections.emptySet());

        for (int keyCode : previousKeys) {
            if (!nextKeys.contains(keyCode)) {
                releaseKey(keyCode);
            }
        }
        for (int keyCode : nextKeys) {
            if (!previousKeys.contains(keyCode)) {
                pressKey(keyCode);
            }
        }

        if (nextKeys.isEmpty()) {
            pointerKeys.remove(pointerId);
        } else {
            pointerKeys.put(pointerId, nextKeys);
        }
        invalidate();
    }

    private void clearPointer(int pointerId) {
        keyboardPointers.remove(pointerId);
        editPointers.remove(pointerId);
        chatPointers.remove(pointerId);
        cruisePointers.remove(pointerId);
        Set<Integer> previousKeys = pointerKeys.remove(pointerId);
        if (previousKeys != null) {
            for (int keyCode : previousKeys) {
                releaseKey(keyCode);
            }
        }
        invalidate();
    }

    private void setCruise(boolean active) {
        if (cruiseActive == active) {
            return;
        }
        cruiseActive = active;
        if (active) {
            pressKey(KeyEvent.KEYCODE_A);
        } else {
            releaseKey(KeyEvent.KEYCODE_A);
        }
        invalidate();
    }

    private void toggleKeyboard() {
        Context context = getContext();
        if (context instanceof GameActivity) {
            ((GameActivity) context).toggleKeyboard();
        } else {
            if (SDLActivity.isScreenKeyboardShown()) {
                SDLActivity.sendMessage(3, 0);
            } else {
                SDLActivity.showTextInput(0, 0, 0, 0);
            }
        }
    }

    private void triggerChat() {
        pressKey(KeyEvent.KEYCODE_T);
        postDelayed(() -> releaseKey(KeyEvent.KEYCODE_T), 40L);
        postDelayed(() -> {
            Context context = getContext();
            if (context instanceof GameActivity) {
                ((GameActivity) context).showKeyboard();
            } else {
                SDLActivity.showTextInput(0, 0, 0, 0);
            }
        }, 80L);
    }

    private Set<Integer> keysAt(float pointerX, float pointerY) {
        if (keyboardButton != null && keyboardButton.contains(pointerX, pointerY)) {
            return Collections.emptySet();
        }
        if (editButton != null && editButton.contains(pointerX, pointerY)) {
            return Collections.emptySet();
        }
        if (chatButton != null && chatButton.visible && chatButton.contains(pointerX, pointerY)) {
            return Collections.emptySet();
        }
        if (cruiseButton != null && cruiseButton.contains(pointerX, pointerY)) {
            return Collections.emptySet();
        }

        for (TouchElement button : actionButtons) {
            if (button.visible && button != chatButton && button.kind != ElementKind.CRUISE && button.contains(pointerX, pointerY)) {
                return Collections.singleton(button.keyCode);
            }
        }
        if (pauseButton != null && pauseButton.contains(pointerX, pointerY)) {
            return Collections.singleton(pauseButton.keyCode);
        }

        float deltaX = pointerX - dpadElement.centerX;
        float deltaY = pointerY - dpadElement.centerY;
        float distanceSquared = deltaX * deltaX + deltaY * deltaY;
        float touchRadius = dpadElement.radius * 1.22f;
        if (distanceSquared > touchRadius * touchRadius) {
            return Collections.emptySet();
        }

        Set<Integer> keys = new HashSet<>();
        float horizontal = deltaX / dpadElement.radius;
        float vertical = deltaY / dpadElement.radius;
        if (horizontal < -0.22f) {
            keys.add(KeyEvent.KEYCODE_DPAD_LEFT);
        } else if (horizontal > 0.22f) {
            keys.add(KeyEvent.KEYCODE_DPAD_RIGHT);
        }
        if (vertical < -0.22f) {
            keys.add(KeyEvent.KEYCODE_DPAD_UP);
        } else if (vertical > 0.22f) {
            keys.add(KeyEvent.KEYCODE_DPAD_DOWN);
        }
        return keys;
    }

    private void pressKey(int keyCode) {
        int references = keyReferences.getOrDefault(keyCode, 0);
        keyReferences.put(keyCode, references + 1);
        if (references == 0) {
            SDLActivity.onNativeKeyDown(keyCode);
        }
    }

    private void releaseKey(int keyCode) {
        int references = keyReferences.getOrDefault(keyCode, 0);
        if (references <= 1) {
            keyReferences.remove(keyCode);
            SDLActivity.onNativeKeyUp(keyCode);
        } else {
            keyReferences.put(keyCode, references - 1);
        }
    }

    private boolean isPressed(int keyCode) {
        return keyReferences.getOrDefault(keyCode, 0) > 0;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private enum ElementKind {
        DPAD,
        ACTION,
        CHAT,
        CRUISE,
        KEYBOARD,
        PAUSE,
        EDIT,
        RESET,
        DONE,
        SIZE_MINUS,
        SIZE_PLUS,
        MENU
    }

    private static final class TouchElement {
        final String id;
        final String label;
        final String code;
        final int keyCode;
        final float defaultNormX;
        final float defaultNormY;
        final float defaultRadiusScale;
        final int color;
        final ElementKind kind;

        float normX;
        float normY;
        float centerX;
        float centerY;
        float radius;
        float sizeFactor;
        boolean visible;

        TouchElement(String id, String label, String code, int keyCode, float defaultNormX,
                     float defaultNormY, float defaultRadiusScale, int color, ElementKind kind) {
            this.id = id;
            this.label = label;
            this.code = code;
            this.keyCode = keyCode;
            this.defaultNormX = defaultNormX;
            this.defaultNormY = defaultNormY;
            this.defaultRadiusScale = defaultRadiusScale;
            this.color = color;
            this.kind = kind;
            this.normX = defaultNormX;
            this.normY = defaultNormY;
            this.sizeFactor = 1f;
            this.visible = true;
        }

        void updatePixelCoords(int width, int height) {
            this.centerX = width * normX;
            this.centerY = height * normY;
            if (kind == ElementKind.DPAD) {
                this.radius = Math.min(height * defaultRadiusScale, width * 0.14f) * sizeFactor;
            } else {
                this.radius = height * defaultRadiusScale * sizeFactor;
            }
        }

        void resetToDefault() {
            this.normX = defaultNormX;
            this.normY = defaultNormY;
            this.sizeFactor = 1f;
            this.visible = true;
        }

        boolean contains(float px, float py) {
            float dx = px - centerX;
            float dy = py - centerY;
            return dx * dx + dy * dy <= radius * radius;
        }
    }
}
