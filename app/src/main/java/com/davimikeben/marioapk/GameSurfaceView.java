package com.davimikeben.marioapk;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * SurfaceView dedicada à renderização OpenGL ES do jogo.
 * Estende GLSurfaceView para gerenciar o ciclo de vida do EGL e o renderer.
 */
public class GameSurfaceView extends GLSurfaceView {
    public GameSurfaceView(Context context) { super(context); init(); }
    public GameSurfaceView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer(renderer);
    }

    /**
     * Aceita o GameRenderer (que implementa GLSurfaceView.Renderer).
     */
    public void setRenderer(GameRenderer renderer) {
        super.setRenderer(renderer);
    }
}
