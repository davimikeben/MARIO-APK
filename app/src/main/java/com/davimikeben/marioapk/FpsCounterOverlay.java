package com.davimikeben.marioapk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Contador de FPS discreto. Por padrão mostra somente o FPS.
 * Quando o menu de monitoramento está aberto, mostra métricas adicionais.
 */
public class FpsCounterOverlay extends View {
    private final Paint paint = new Paint();
    private int frameCount = 0;
    private long lastTime = 0;
    private float fps = 0f;
    private boolean showDetailed = false;
    private String detailLine = "";

    public FpsCounterOverlay(Context context) {
        super(context);
        paint.setColor(Color.GREEN);
        paint.setTextSize(32f);
        paint.setAntiAlias(true);
        paint.setShadowLayer(3f, 2f, 2f, Color.BLACK);
        setWillNotDraw(false);
    }

    public void setShowDetailed(boolean showDetailed) {
        this.showDetailed = showDetailed;
    }

    public void setDetailLine(String detailLine) {
        this.detailLine = detailLine;
    }

    /** Registrar um frame renderizado. Chamado a cada frame. */
    public void tick() {
        long now = System.currentTimeMillis();
        frameCount++;
        if (lastTime == 0) {
            lastTime = now;
        }
        if (now - lastTime >= 1000) {
            fps = frameCount * 1000f / (now - lastTime);
            frameCount = 0;
            lastTime = now;
            postInvalidate();
        } else {
            postInvalidate();
        }
    }

    public float getFps() {
        return fps;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        String text = String.format("FPS %.0f", fps);
        canvas.drawText(text, 20f, 50f, paint);
        if (showDetailed && detailLine != null && !detailLine.isEmpty()) {
            paint.setTextSize(24f);
            canvas.drawText(detailLine, 20f, 84f, paint);
            paint.setTextSize(32f);
        }
    }
}
