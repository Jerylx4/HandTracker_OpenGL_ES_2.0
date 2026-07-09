package com.prograavanzada.test;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class MyGLSurfaceView extends GLSurfaceView {
    private MyGLRenderer renderer;

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyGLSurfaceView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        // Eliminamos setZOrderOnTop y setFormat
        renderer = new MyGLRenderer();
        setRenderer(renderer);
    }

    public MyGLRenderer getRenderer() {
        return renderer;
    }
}