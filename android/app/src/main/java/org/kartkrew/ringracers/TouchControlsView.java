package org.kartkrew.ringracers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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
    private static final int OUTLINE_COLOR = Color.argb(185, 255, 255, 255);
    private static final int LABEL_COLOR = Color.WHITE;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowPath = new Path();
    private final List<ActionButton> actionButtons = new ArrayList<>();
    private final Map<Integer, Set<Integer>> pointerKeys = new HashMap<>();
    private final Map<Integer, Integer> keyReferences = new HashMap<>();
    private final Set<Integer> keyboardPointers = new HashSet<>();

    private float dpadCenterX;
    private float dpadCenterY;
    private float dpadRadius;
    private ActionButton pauseButton;
    private ActionButton keyboardButton;

    TouchControlsView(Context context) {
        super(context);
        setFocusable(false);
        setClickable(true);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(dp(2));
        outlinePaint.setColor(OUTLINE_COLOR);
        textPaint.setColor(LABEL_COLOR);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        float screenHeight = height;

        dpadCenterX = width * 0.16f;
        dpadCenterY = height * 0.72f;
        dpadRadius = Math.min(screenHeight * 0.245f, width * 0.14f);

        actionButtons.clear();
        actionButtons.add(button("GO", "A", KeyEvent.KEYCODE_A, 0.88f, 0.76f, 0.105f, 45, 159, 93));
        actionButtons.add(button("DRIFT", "R", KeyEvent.KEYCODE_S, 0.72f, 0.80f, 0.082f, 28, 145, 170));
        actionButtons.add(button("ITEM", "L", KeyEvent.KEYCODE_SPACE, 0.79f, 0.53f, 0.078f, 224, 169, 47));
        actionButtons.add(button("BRAKE", "X", KeyEvent.KEYCODE_D, 0.945f, 0.57f, 0.076f, 215, 72, 65));
        actionButtons.add(button("SPIN", "C", KeyEvent.KEYCODE_Q, 0.64f, 0.60f, 0.070f, 62, 101, 181));
        actionButtons.add(button("LOOK", "B", KeyEvent.KEYCODE_SHIFT_LEFT, 0.58f, 0.82f, 0.062f, 94, 102, 110));
        actionButtons.add(button("BAIL", "Y", KeyEvent.KEYCODE_V, 0.86f, 0.32f, 0.065f, 202, 76, 111));
        actionButtons.add(button("VOTE", "Z", KeyEvent.KEYCODE_Z, 0.96f, 0.31f, 0.052f, 105, 111, 118));

        pauseButton = new ActionButton("", "", KeyEvent.KEYCODE_ESCAPE, width * 0.54f, height * 0.085f,
            screenHeight * 0.052f, Color.rgb(53, 58, 64));
        keyboardButton = new ActionButton("", "", 0, width * 0.46f, height * 0.085f,
            screenHeight * 0.052f, Color.rgb(53, 58, 64));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDpad(canvas);
        for (ActionButton button : actionButtons) {
            drawActionButton(canvas, button, ButtonKind.NORMAL);
        }
        drawActionButton(canvas, pauseButton, ButtonKind.PAUSE);
        drawActionButton(canvas, keyboardButton, ButtonKind.KEYBOARD);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

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

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    void releaseAll() {
        keyboardPointers.clear();
        List<Integer> pressedKeys = new ArrayList<>(keyReferences.keySet());
        pointerKeys.clear();
        keyReferences.clear();
        for (int keyCode : pressedKeys) {
            SDLActivity.onNativeKeyUp(keyCode);
        }
        invalidate();
    }

    private ActionButton button(String label, String code, int keyCode, float horizontal, float vertical,
                                float radiusScale, int red, int green, int blue) {
        return new ActionButton(label, code, keyCode, getWidth() * horizontal, getHeight() * vertical,
            getHeight() * radiusScale, Color.rgb(red, green, blue));
    }

    private void updatePointer(int pointerId, float pointerX, float pointerY) {
        boolean inKeyboard = keyboardButton != null && keyboardButton.contains(pointerX, pointerY);
        if (inKeyboard) {
            if (!keyboardPointers.contains(pointerId)) {
                keyboardPointers.add(pointerId);
                toggleKeyboard();
            }
        } else {
            keyboardPointers.remove(pointerId);
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
        Set<Integer> previousKeys = pointerKeys.remove(pointerId);
        if (previousKeys != null) {
            for (int keyCode : previousKeys) {
                releaseKey(keyCode);
            }
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

    private Set<Integer> keysAt(float pointerX, float pointerY) {
        if (keyboardButton != null && keyboardButton.contains(pointerX, pointerY)) {
            return Collections.emptySet();
        }
        for (ActionButton button : actionButtons) {
            if (button.contains(pointerX, pointerY)) {
                return Collections.singleton(button.keyCode);
            }
        }
        if (pauseButton != null && pauseButton.contains(pointerX, pointerY)) {
            return Collections.singleton(pauseButton.keyCode);
        }

        float deltaX = pointerX - dpadCenterX;
        float deltaY = pointerY - dpadCenterY;
        float distanceSquared = deltaX * deltaX + deltaY * deltaY;
        float touchRadius = dpadRadius * 1.22f;
        if (distanceSquared > touchRadius * touchRadius) {
            return Collections.emptySet();
        }

        Set<Integer> keys = new HashSet<>();
        float horizontal = deltaX / dpadRadius;
        float vertical = deltaY / dpadRadius;
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

    private void drawDpad(Canvas canvas) {
        boolean leftPressed = isPressed(KeyEvent.KEYCODE_DPAD_LEFT);
        boolean rightPressed = isPressed(KeyEvent.KEYCODE_DPAD_RIGHT);
        boolean upPressed = isPressed(KeyEvent.KEYCODE_DPAD_UP);
        boolean downPressed = isPressed(KeyEvent.KEYCODE_DPAD_DOWN);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(92, 28, 31, 35));
        canvas.drawCircle(dpadCenterX, dpadCenterY, dpadRadius, fillPaint);
        canvas.drawCircle(dpadCenterX, dpadCenterY, dpadRadius, outlinePaint);

        float arrowDistance = dpadRadius * 0.55f;
        float arrowSize = dpadRadius * 0.20f;
        drawArrow(canvas, dpadCenterX - arrowDistance, dpadCenterY, arrowSize, 180f, leftPressed);
        drawArrow(canvas, dpadCenterX + arrowDistance, dpadCenterY, arrowSize, 0f, rightPressed);
        drawArrow(canvas, dpadCenterX, dpadCenterY - arrowDistance, arrowSize, -90f, upPressed);
        drawArrow(canvas, dpadCenterX, dpadCenterY + arrowDistance, arrowSize, 90f, downPressed);

        fillPaint.setColor(Color.argb(130, 255, 255, 255));
        canvas.drawCircle(dpadCenterX, dpadCenterY, dpadRadius * 0.12f, fillPaint);
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

    private enum ButtonKind {
        NORMAL,
        PAUSE,
        KEYBOARD
    }

    private void drawActionButton(Canvas canvas, ActionButton button, ButtonKind kind) {
        if (button == null) {
            return;
        }
        boolean pressed = (kind == ButtonKind.KEYBOARD) ? !keyboardPointers.isEmpty() : isPressed(button.keyCode);
        int alpha = pressed ? 230 : 140;
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(alpha, Color.red(button.color), Color.green(button.color), Color.blue(button.color)));
        canvas.drawCircle(button.centerX, button.centerY, button.radius, fillPaint);
        canvas.drawCircle(button.centerX, button.centerY, button.radius, outlinePaint);

        if (kind == ButtonKind.PAUSE) {
            fillPaint.setColor(LABEL_COLOR);
            float barWidth = button.radius * 0.18f;
            float barHeight = button.radius * 0.70f;
            float gap = button.radius * 0.18f;
            canvas.drawRoundRect(new RectF(
                button.centerX - gap - barWidth,
                button.centerY - barHeight / 2f,
                button.centerX - gap,
                button.centerY + barHeight / 2f
            ), barWidth * 0.25f, barWidth * 0.25f, fillPaint);
            canvas.drawRoundRect(new RectF(
                button.centerX + gap,
                button.centerY - barHeight / 2f,
                button.centerX + gap + barWidth,
                button.centerY + barHeight / 2f
            ), barWidth * 0.25f, barWidth * 0.25f, fillPaint);
            return;
        }

        if (kind == ButtonKind.KEYBOARD) {
            drawKeyboardIcon(canvas, button.centerX, button.centerY, button.radius);
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

    private void drawKeyboardIcon(Canvas canvas, float cx, float cy, float radius) {
        float kw = radius * 1.18f;
        float kh = radius * 0.78f;
        RectF kbRect = new RectF(cx - kw / 2f, cy - kh / 2f, cx + kw / 2f, cy + kh / 2f);

        // Keyboard body outline
        outlinePaint.setStrokeWidth(dp(1.5f));
        canvas.drawRoundRect(kbRect, dp(3f), dp(3f), outlinePaint);
        outlinePaint.setStrokeWidth(dp(2f)); // restore standard outline width

        // Key dots / segments inside
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(LABEL_COLOR);

        float dotRadius = dp(1.2f);
        // Row 1 (top keys)
        float row1Y = cy - kh * 0.22f;
        float[] row1XOffsets = {-kw * 0.30f, -kw * 0.10f, kw * 0.10f, kw * 0.30f};
        for (float xOffset : row1XOffsets) {
            canvas.drawCircle(cx + xOffset, row1Y, dotRadius, fillPaint);
        }

        // Row 2 (middle keys)
        float row2Y = cy + kh * 0.04f;
        float[] row2XOffsets = {-kw * 0.22f, 0f, kw * 0.22f};
        for (float xOffset : row2XOffsets) {
            canvas.drawCircle(cx + xOffset, row2Y, dotRadius, fillPaint);
        }

        // Row 3 (space bar)
        float row3Y = cy + kh * 0.28f;
        float spaceHalfW = kw * 0.26f;
        float spaceHalfH = dp(1.1f);
        canvas.drawRoundRect(new RectF(cx - spaceHalfW, row3Y - spaceHalfH, cx + spaceHalfW, row3Y + spaceHalfH),
            dp(1f), dp(1f), fillPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static final class ActionButton {
        final String label;
        final String code;
        final int keyCode;
        final float centerX;
        final float centerY;
        final float radius;
        final int color;

        ActionButton(String label, String code, int keyCode, float centerX, float centerY, float radius, int color) {
            this.label = label;
            this.code = code;
            this.keyCode = keyCode;
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
            this.color = color;
        }

        boolean contains(float pointerX, float pointerY) {
            float deltaX = pointerX - centerX;
            float deltaY = pointerY - centerY;
            return deltaX * deltaX + deltaY * deltaY <= radius * radius;
        }
    }
}
