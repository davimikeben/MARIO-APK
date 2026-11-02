package com.davimikeben.marioapk;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Teclado virtual completo, multitouch, para o jogo.
 * Comandos do Nintendo 64 (Super Mario 64):
 *   Direcional (⬆️⬇️⬅️➡️), A, B, Start/Pausa, Z, R, C-buttons (câmera).
 * Cada tecla tem nome, descrição, tamanho, opacidade e posição ajustáveis.
 */
public class VirtualKeyboardView extends View {
    public interface Listener {
        void onKeyPressed(String id, int keyCode);
        void onKeyReleased(String id, int keyCode);
        void onAnalog(float x, float y); // -1..1
    }

    private final List<KeyButton> keys = new ArrayList<>();
    private Listener listener;
    private float scale = 1f;
    private int alpha = 200;
    private boolean enabled = true;

    public static final int KEY_UP = 1;
    public static final int KEY_DOWN = 2;
    public static final int KEY_LEFT = 3;
    public static final int KEY_RIGHT = 4;
    public static final int KEY_A = 5;
    public static final int KEY_B = 6;
    public static final int KEY_START = 7;
    public static final int KEY_Z = 8;
    public static final int KEY_R = 9;
    public static final int KEY_C_UP = 10;
    public static final int KEY_C_DOWN = 11;
    public static final int KEY_C_LEFT = 12;
    public static final int KEY_C_RIGHT = 13;

    public VirtualKeyboardView(Context context) { this(context, null); }
    public VirtualKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDefaultLayout();
        setWillNotDraw(false);
    }

    private void setupDefaultLayout() {
        addKey("UP", "⬆️", "Direção cima", KeyButton.Type.DPAD, KEY_UP);
        addKey("DOWN", "⬇️", "Direção baixo", KeyButton.Type.DPAD, KEY_DOWN);
        addKey("LEFT", "⬅️", "Direção esquerda", KeyButton.Type.DPAD, KEY_LEFT);
        addKey("RIGHT", "➡️", "Direção direita", KeyButton.Type.DPAD, KEY_RIGHT);
        addKey("A", "🅰️", "Botão A — pular / ação", KeyButton.Type.ACTION, KEY_A);
        addKey("B", "🅱️", "Botão B — atacar / ação", KeyButton.Type.ACTION, KEY_B);
        addKey("START", "⏸️", "Pausar / menu do jogo", KeyButton.Type.SYSTEM, KEY_START);
        addKey("Z", "Z", "Agachar / mirar", KeyButton.Type.ACTION, KEY_Z);
        addKey("R", "R", "Ombro direito", KeyButton.Type.ACTION, KEY_R);
        addKey("C_UP", "⬆️ C", "Câmera cima", KeyButton.Type.ACTION, KEY_C_UP);
        addKey("C_DOWN", "⬇️ C", "Câmera baixo", KeyButton.Type.ACTION, KEY_C_DOWN);
        addKey("C_LEFT", "⬅️ C", "Câmera esquerda", KeyButton.Type.ACTION, KEY_C_LEFT);
        addKey("C_RIGHT", "➡️ C", "Câmera direita", KeyButton.Type.ACTION, KEY_C_RIGHT);
    }

    public void setListener(Listener l) { this.listener = l; }
    public void setScale(float scale) { this.scale = Math.max(0.5f, Math.min(2f, scale)); requestLayout(); }
    public void setAlpha(int alpha) { this.alpha = Math.max(0, Math.min(255, alpha)); invalidate(); }
    public void setEnabled(boolean enabled) { this.enabled = enabled; invalidate(); }

    public List<KeyButton> getKeys() { return keys; }
    public KeyButton findKey(String id) {
        for (KeyButton k : keys) if (k.id.equals(id)) return k;
        return null;
    }

    public void addKey(String id, String label, String desc, KeyButton.Type type, int keyCode) {
        keys.add(new KeyButton(id, label, desc, type, keyCode));
    }
}

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyLayout(w, h);
    }

    private void applyLayout(int w, int h) {
        if (w == 0 || h == 0) return;
        float k = Math.min(w, h) * 0.14f * scale;
        float pad = k * 0.25f;

        KeyButton up = findKey("UP"), down = findKey("DOWN"), left = findKey("LEFT"), right = findKey("RIGHT");
        KeyButton A = findKey("A"), B = findKey("B"), start = findKey("START");
        KeyButton Z = findKey("Z"), R = findKey("R");
        KeyButton cu = findKey("C_UP"), cd = findKey("C_DOWN"), cl = findKey("C_LEFT"), cr = findKey("C_RIGHT");

        float dcx = w * 0.16f; float dcy = h * 0.62f;
        if (up != null) up.setBounds(dcx, dcy - k - pad, k, k);
        if (down != null) down.setBounds(dcx, dcy + k + pad, k, k);
        if (left != null) left.setBounds(dcx - k - pad, dcy, k, k);
        if (right != null) right.setBounds(dcx + k + pad, dcy, k, k);

        float ax = w * 0.82f; float ay = h * 0.62f;
        if (A != null) A.setBounds(ax, ay - k - pad, k, k);
        if (B != null) B.setBounds(ax + k + pad, ay, k, k);

        if (start != null) start.setBounds(w * 0.88f, h * 0.18f, k * 0.9f, k * 0.9f);
        if (Z != null) Z.setBounds(w * 0.16f, h * 0.18f, k * 0.85f, k * 0.85f);
        if (R != null) R.setBounds(w * 0.88f, h * 0.30f, k * 0.85f, k * 0.85f);

        float cx = w * 0.30f; float cy = h * 0.88f;
        if (cu != null) cu.setBounds(cx, cy - k - pad, k * 0.85f, k * 0.85f);
        if (cd != null) cd.setBounds(cx, cy + k + pad, k * 0.85f, k * 0.85f);
        if (cl != null) cl.setBounds(cx - k - pad, cy, k * 0.85f, k * 0.85f);
        if (cr != null) cr.setBounds(cx + k + pad, cy, k * 0.85f, k * 0.85f);

        for (KeyButton kb : keys) kb.setScale(scale);
        for (KeyButton kb : keys) kb.setAlpha(alpha);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!enabled) return;
        for (KeyButton k : keys) k.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enabled) return false;
        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            for (int i = 0; i < pointerCount; i++) {
                KeyButton k = hitTest(event.getX(i), event.getY(i));
                if (k != null && k.getActivePointerId() == -1) {
                    k.press(event.getX(i), event.getY(i), event.getPointerId(i));
                    if (listener != null) listener.onKeyPressed(k.id, k.keyCode);
                }
            }
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < pointerCount; i++) {
                int pid = event.getPointerId(i);
                KeyButton k = findByPointer(pid);
                if (k != null && !k.contains(event.getX(i), event.getY(i))) {
                    k.release();
                    if (listener != null) listener.onKeyReleased(k.id, k.keyCode);
                }
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            for (int i = 0; i < pointerCount; i++) {
                int pid = event.getPointerId(i);
                KeyButton k = findByPointer(pid);
                if (k != null) {
                    k.release();
                    if (listener != null) listener.onKeyReleased(k.id, k.keyCode);
                }
            }
            return true;
        }
        return false;
    }

    private KeyButton hitTest(float px, float py) {
        for (int i = keys.size() - 1; i >= 0; i--) {
            KeyButton k = keys.get(i);
            if (k.contains(px, py)) return k;
        }
        return null;
    }

    private KeyButton findByPointer(int pid) {
        for (KeyButton k : keys) if (k.getActivePointerId() == pid) return k;
        return null;
    }
}

