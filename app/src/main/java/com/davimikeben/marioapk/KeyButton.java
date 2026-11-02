package com.davimikeben.marioapk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 * Um único botão do teclado virtual.
 * Suporta: toque, multitouch (ponteiro próprio), redimensionamento, opacidade,
 * posição personalizada, nome e descrição.
 */
public class KeyButton {
    public enum Type { DPAD, ACTION, SPECIAL, SYSTEM }

    public final String id;           // identificador do comando (ex: "A", "START", "UP")
    public final String label;        // texto/emoji exibido
    public final String description;  // descrição do comando
    public final Type type;
    public final int keyCode;         // código enviado ao núcleo

    public float x, y;                // posição do centro (em pixels da tela)
    public float w, h;                // tamanho
    public float scale = 1f;          // escala de redimensionamento
    public int alpha = 200;           // opacidade 0-255
    public boolean enabled = true;

    private boolean pressed = false;
    private int activePointerId = -1; // qual dedo está pressionando este botão
    private float pressStartX, pressStartY;

    private final Paint bgPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final RectF rect = new RectF();

    public KeyButton(String id, String label, String description, Type type, int keyCode) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.type = type;
        this.keyCode = keyCode;
        this.w = 72f; this.h = 72f;
        initPaints();
    }

    private void initPaints() {
        bgPaint.setAntiAlias(true);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
    }

    public void setBounds(float cx, float cy, float w, float h) {
        this.x = cx; this.y = cy; this.w = w * scale; this.h = h * scale;
        rect.set(x - this.w/2f, y - this.h/2f, x + this.w/2f, y + this.h/2f);
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.5f, Math.min(2f, scale));
        rect.set(x - this.w/2f, y - this.h/2f, x + this.w/2f, y + this.h/2f);
    }

    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
    }

    public boolean contains(float px, float py) {
        if (!enabled) return false;
        return rect.contains(px, py);
    }

    public void press(float px, float py, int pointerId) {
        this.pressed = true;
        this.activePointerId = pointerId;
        this.pressStartX = px; this.pressStartY = py;
    }

    public void release() {
        this.pressed = false;
        this.activePointerId = -1;
    }

    public boolean isPressed() { return pressed; }
    public int getActivePointerId() { return activePointerId; }

    public void draw(Canvas canvas) {
        if (!enabled) {
            bgPaint.setColor(0xFF444444);
            bgPaint.setAlpha(80);
        } else if (pressed) {
            bgPaint.setColor(0xFF00E5FF);
            bgPaint.setAlpha(alpha);
        } else {
            bgPaint.setColor(0xFF232A36);
            bgPaint.setAlpha(alpha);
        }
        canvas.drawRoundRect(rect, 14f, 14f, bgPaint);

        borderPaint.setColor(pressed ? Color.WHITE : 0xFF555555);
        borderPaint.setAlpha(alpha);
        canvas.drawRoundRect(rect, 14f, 14f, borderPaint);

        textPaint.setColor(pressed ? Color.BLACK : Color.WHITE);
        textPaint.setAlpha(alpha);
        textPaint.setTextSize(Math.min(w, h) * 0.45f);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float cy = y + (fm.descent - fm.ascent)/2f - fm.descent;
        canvas.drawText(label, x, cy, textPaint);
    }

    /** Retorna true se este evento de toque deve ser consumido por este botão. */
    public boolean onTouchEvent(MotionEvent ev) {
        if (!enabled) return false;
        int action = ev.getActionMasked();
        int idx = ev.getActionIndex();
        int pid = ev.getPointerId(idx);
        float px = ev.getX(idx);
        float py = ev.getY(idx);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (contains(px, py) && activePointerId == -1) {
                    press(px, py, pid);
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pressed && activePointerId == pid) {
                    // manter pressionado enquanto o dedo estiver sobre o botão
                    if (!contains(px, py)) {
                        release();
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (pressed && activePointerId == pid) {
                    release();
                    return true; // clique consumido
                }
                break;
        }
        return false;
    }
}
