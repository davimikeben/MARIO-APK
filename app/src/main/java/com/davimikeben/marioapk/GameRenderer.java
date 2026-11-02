package com.davimikeben.marioapk;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renderizador OpenGL ES 2.0. Desenha o frame do núcleo como textura na tela.
 * Otimizado para Mali-G52: filtro NEXT (sem interpolação), sem blending/dithering
 * desnecessários, textura RGBA direta.
 */
public class GameRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "GameRenderer";
    private final PerformanceManager perf;
    private int textureId = -1;
    private int program = -1;
    private int uTexture = -1, aPosition = -1, aTexCoord = -1;
    private int frameWidth = 640, frameHeight = 480;
    private int surfaceWidth = 1, surfaceHeight = 1;
    private volatile byte[] framePixels = null;
    private volatile boolean newFrame = false;

    public GameRenderer(PerformanceManager perf) { this.perf = perf; }

    public void setFrameSize(int w, int h) { this.frameWidth = w; this.frameHeight = h; }

    public void submitFrame(byte[] pixelsRGBA) {
        this.framePixels = pixelsRGBA;
        this.newFrame = true;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "OpenGL ES surface criada. GLES=" + perf.getGlEsVersion());
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DITHER);
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (program == 0) { Log.e(TAG, "Falha ao criar programa GLSL"); return; }
        uTexture = GLES20.glGetUniformLocation(program, "u_texture");
        aPosition = GLES20.glGetAttribLocation(program, "a_position");
        aTexCoord = GLES20.glGetAttribLocation(program, "a_texCoord");
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        textureId = tex[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        surfaceWidth = w; surfaceHeight = h;
        GLES20.glViewport(0, 0, w, h);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (program == 0 || textureId < 0) return;
        GLES20.glUseProgram(program);
        byte[] pixels = null;
        synchronized (this) {
            if (newFrame && framePixels != null) { pixels = framePixels; newFrame = false; }
        }
        if (pixels != null && pixels.length == frameWidth * frameHeight * 4) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    frameWidth, frameHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, java.nio.ByteBuffer.wrap(pixels));
        }
        drawQuad();
    }

    private void drawQuad() {
        float[] v = {-1f,-1f,0f,1f, 1f,-1f,1f,1f, -1f,1f,0f,0f, 1f,1f,1f,0f};
        java.nio.FloatBuffer fb = java.nio.ByteBuffer.allocateDirect(v.length*4)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(v).position(0);
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition,2,GLES20.GL_FLOAT,false,16,fb);
        java.nio.FloatBuffer tb = java.nio.ByteBuffer.allocateDirect(v.length*4)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        tb.put(v).position(0);
        GLES20.glEnableVertexAttribArray(aTexCoord);
        GLES20.glVertexAttribPointer(aTexCoord,2,GLES20.GL_FLOAT,false,16,tb);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aTexCoord);
    }

    private int createProgram(String vs, String fs) {
        int v = loadShader(GLES20.GL_VERTEX_SHADER, vs);
        int f = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
        if (v==0 || f==0) return 0;
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f);
        GLES20.glLinkProgram(p);
        int[] ok=new int[1]; GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);
        return ok[0]==1? p : 0;
    }
    private int loadShader(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok=new int[1]; GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);
        return ok[0]==1? s : 0;
    }
    public void release() {
        if (textureId>=0) GLES20.glDeleteTextures(1,new int[]{textureId},0);
        if (program>0) GLES20.glDeleteProgram(program);
    }

    private static final String VERTEX_SHADER =
        "attribute vec2 a_position; attribute vec2 a_texCoord; varying vec2 v_texCoord; void main(){gl_Position=vec4(a_position,0.0,1.0); v_texCoord=a_texCoord;}";
    private static final String FRAGMENT_SHADER =
        "precision mediump float; uniform sampler2D u_texture; varying vec2 v_texCoord; void main(){gl_FragColor=texture2D(u_texture,v_texCoord);}";
}
