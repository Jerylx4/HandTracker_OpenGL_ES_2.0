package com.prograavanzada.test;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mVPMatrix = new float[16];
    private final float[] mvMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    float angle = 0;

    private Cube cube;
    private float[][] handLandmarks = null;
    private final Object lock = new Object();

    private final int[][] HAND_CONNECTIONS = {
            {0, 1}, {1, 2}, {2, 3}, {3, 4},
            {0, 5}, {5, 6}, {6, 7}, {7, 8},
            {5, 9}, {9, 10}, {10, 11}, {11, 12},
            {9, 13}, {13, 14}, {14, 15}, {15, 16},
            {13, 17}, {0, 17}, {17, 18}, {18, 19}, {19, 20}
    };

    public void updateHandLandmarks(float[][] landmarks) {
        synchronized (lock) {
            this.handLandmarks = landmarks;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        cube = new Cube();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;

        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 1, 50);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setLookAtM(mViewMatrix, 0, 3.0f, 3.0f, 5.0f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        cube.draw(calculateMVP(0f, 0f, 0f, angle, 0f, 1f, 0f, 1f, 1f, 1f), mvMatrix);

        angle += 0.5f;

        float[][] currentLandmarks;
        synchronized (lock) {
            currentLandmarks = this.handLandmarks;
        }

        if (currentLandmarks != null && currentLandmarks.length == 21) {
            float[] colorLinea = {1.0f, 1.0f, 1.0f, 1.0f};
            float grosor = 10.0f;

            for (int[] connection : HAND_CONNECTIONS) {
                int p1 = connection[0];
                int p2 = connection[1];

                float mpX1 = currentLandmarks[p1][0];
                float mpY1 = currentLandmarks[p1][1];
                float mpX2 = currentLandmarks[p2][0];
                float mpY2 = currentLandmarks[p2][1];

                float x1 = 1.0f - (mpY1 * 2.0f);
                float y1 = (mpX1 * 2.0f) - 1.0f;
                float x2 = 1.0f - (mpY2 * 2.0f);
                float y2 = (mpX2 * 2.0f) - 1.0f;

                Line linea = new Line(x1, y1, x2, y2, grosor, colorLinea);
                linea.draw();
            }
        }
    }

    private float[] calculateMVP(float tx, float ty, float tz, float rotAngle, float rx, float ry, float rz, float sx, float sy, float sz) {
        float[] finalMVPMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, tx, ty, tz);

        if (rotAngle != 0f) {
            Matrix.rotateM(modelMatrix, 0, rotAngle, rx, ry, rz);
        }
        if (sx != 1f || sy != 1f || sz != 1f) {
            Matrix.scaleM(modelMatrix, 0, sx, sy, sz);
        }

        Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(finalMVPMatrix, 0, mVPMatrix, 0, modelMatrix, 0);

        return finalMVPMatrix;
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}