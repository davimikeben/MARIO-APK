package com.davimikeben.marioapk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Botão flutuante discreto que abre/fecha um menu dentro da partida.
 * Não reinicializa o jogo ao abrir. Opções reportadas via Listener.
 */
public class FloatingMenuView extends View {
    public interface Listener {
        void onMenuOption(String option);
    }

    private final Paint bgPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint panelPaint = new Paint();
    private final RectF buttonRect = new RectF();
    private final RectF panelRect = new RectF();

    private float cx, cy, radius;
    private boolean menuOpen = false;
    private Listener listener;
    private boolean dragging = false;
    private float dragStartX, dragStartY;

    private static final String[] OPTIONS = {
        "🎮 Modificar controles","↔️ Mover botões","📏 Redimensionar botões",
        "🔧 Transparência","🎨 Gráficos","⚡ Desempenho","🔄 Reiniciar jogo",
        "💾 Salvar configurações","🏠 Voltar ao menu principal"
    };
    private static final String[] CODES = {
        "controls","move_buttons","resize_buttons","transparency","graphics",
        "performance","restart","save_settings","main_menu"
    };

    public FloatingMenuView(Context context) { this(context, null); }
    public FloatingMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bgPaint.setAntiAlias(true); bgPaint.setColor(0xFF3F51B5);
        textPaint.setAntiAlias(true); textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        panelPaint.setAntiAlias(true); panelPaint.setColor(0xDD161B22);
        setWillNotDraw(false);
    }

    public void setListener(Listener l) { this.listener = l; }
    public void setOpen(boolean open) { this.menuOpen = open; invalidate(); }
    public boolean isOpen() { return menuOpen; }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cx = w - 60f; cy = h - 120f; radius = 32f;
        updateButtonRect(); updatePanelRect(w, h);
    }

    private void updateButtonRect() { buttonRect.set(cx - radius, cy - radius, cx + radius, cy + radius); }
    private void updatePanelRect(int w, int h) {
        float pw = w * 0.72f, ph = h * 0.78f;
        panelRect.set((w - pw)/2f, (h - ph)/2f, (w + pw)/2f, (h + ph)/2f);
    }

    public void setPosition(float x, float y) { cx = x; cy = y; updateButtonRect(); invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(cx, cy, radius, bgPaint);
        textPaint.setTextSize(radius * 0.9f);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        canvas.drawText("⋮", cx, cy + (fm.descent - fm.ascent)/2f - fm.descent, textPaint);
        if (!menuOpen) return;
        canvas.drawRect(panelRect, panelPaint);
        textPaint.setTextSize(radius * 1.1f);
        canvas.drawText("Menu do Jogo", panelRect.centerX(), panelRect.top + 60f, textPaint);
        textPaint.setTextSize(radius * 0.75f);
        float itemH = panelRect.height() / (OPTIONS.length + 1);
        for (int i = 0; i < OPTIONS.length; i++) {
            float iy = panelRect.top + itemH * (i + 1);
            canvas.drawText(OPTIONS[i], panelRect.centerX(), iy, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX(), y = event.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            if (buttonRect.contains(x, y)) { dragging = true; dragStartX = x - cx; dragStartY = y - cy; return true; }
            if (menuOpen && !panelRect.contains(x, y)) { menuOpen = false; invalidate(); return true; }
            if (menuOpen && panelRect.contains(x, y)) {
                int idx = optionAt(x, y);
                if (idx >= 0) { menuOpen = false; invalidate(); if (listener != null) listener.onMenuOption(CODES[idx]); }
                return true;
            }
            return false;
        }
        if (action == MotionEvent.ACTION_MOVE && dragging) {
            cx = Math.max(radius, Math.min(getWidth() - radius, x - dragStartX));
            cy = Math.max(radius, Math.min(getHeight() - radius, y - dragStartY));
            updateButtonRect(); invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (dragging) {
                dragging = false;
                float dx = x - (cx + dragStartX), dy = y - (cy + dragStartY);
                if (dx*dx + dy*dy < 400f) { menuOpen = !menuOpen; invalidate(); }
                return true;
            }
            return false;
        }
        return false;
    }

    private int optionAt(float x, float y) {
        if (!panelRect.contains(x, y)) return -1;
        float itemH = panelRect.height() / (OPTIONS.length + 1);
        for (int i = 0; i < OPTIONS.length; i++) {
            float iy = panelRect.top + itemH * (i + 1);
            if (y >= iy - itemH/2f && y <= iy + itemH/2f) return i;
        }
        return -1;
    }
}
