package com.prograavanzada.test;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Cube {

    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private ShortBuffer indexBuffer;

    private int mProgram, positionHandle, colorHandle, mMVPMatrixHandle, normalHandle;

    private final int COORDS_PER_VERTEX = 7;

    // X, Y, Z, R, G, B, A
    private final float[] cubeCoords = {

            // =========================
            // FRENTE -> BLANCO
            // =========================
            -0.5f,  0.5f,  0.5f, 1, 1, 1, 1,
            -0.5f, -0.5f,  0.5f, 1, 1, 1, 1,
            0.5f, -0.5f,  0.5f, 1, 1, 1, 1,
            0.5f,  0.5f,  0.5f, 1, 1, 1, 1,

            // =========================
            // ATRAS -> BLANCO
            // =========================
            -0.5f,  0.5f, -0.5f, 1, 1, 1, 1,
            -0.5f, -0.5f, -0.5f, 1, 1, 1, 1,
            0.5f, -0.5f, -0.5f, 1, 1, 1, 1,
            0.5f,  0.5f, -0.5f, 1, 1, 1, 1,

            // =========================
            // IZQUIERDA -> BLANCO
            // =========================
            -0.5f,  0.5f, -0.5f, 1, 1, 1, 1,
            -0.5f, -0.5f, -0.5f, 1, 1, 1, 1,
            -0.5f, -0.5f,  0.5f, 1, 1, 1, 1,
            -0.5f,  0.5f,  0.5f, 1, 1, 1, 1,

            // =========================
            // DERECHA -> BLANCO
            // =========================
            0.5f,  0.5f, -0.5f, 1, 1, 1, 1,
            0.5f, -0.5f, -0.5f, 1, 1, 1, 1,
            0.5f, -0.5f,  0.5f, 1, 1, 1, 1,
            0.5f,  0.5f,  0.5f, 1, 1, 1, 1,

            // =========================
            // ARRIBA -> BLANCO
            // =========================
            -0.5f,  0.5f, -0.5f, 1, 1, 1, 1,
            -0.5f,  0.5f,  0.5f, 1, 1, 1, 1,
            0.5f,  0.5f,  0.5f, 1, 1, 1, 1,
            0.5f,  0.5f, -0.5f, 1, 1, 1, 1,

            // =========================
            // ABAJO -> BLANCO
            // =========================
            -0.5f, -0.5f, -0.5f, 1, 1, 1, 1,
            -0.5f, -0.5f,  0.5f, 1, 1, 1, 1,
            0.5f, -0.5f,  0.5f, 1, 1, 1, 1,
            0.5f, -0.5f, -0.5f, 1, 1, 1, 1
    };

    // INDICES
    private final short[] indices = {

            // frente
            0, 1, 2,
            0, 2, 3,

            // atrás
            4, 5, 6,
            4, 6, 7,

            // izquierda
            8, 9, 10,
            8, 10, 11,

            // derecha
            12, 13, 14,
            12, 14, 15,

            // arriba
            16, 17, 18,
            16, 18, 19,

            // abajo
            20, 21, 22,
            20, 22, 23
    };


    static float[] normals = {
            0,0,1,0,0,1,0,0,1,0,0,1,
            0,0,-1,0,0,-1,0,0,-1,0,0,-1,
            -1,0,0,-1,0,0,-1,0,0,-1,0,0,
            1,0,0,1,0,0,1,0,0,1,0,0,
            0,1,0,0,1,0,0,1,0,0,1,0,
            0,-1,0,0,-1,0,0,-1,0,0,-1,0
    };

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "uniform mat4 uMVMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec3 vNormal;" +
                    "varying vec3 aNormal;" +
                    "varying vec3 aPosition;" +
                    "attribute vec4 aColor;" +
                    "varying vec4 vColor;" +
                    "void main(){" +
                    "   aPosition = vec3(uMVMatrix * vPosition);" +
                    "   aNormal = normalize(mat3(uMVMatrix) * vNormal);" +
                    "   gl_Position = uMVPMatrix * vPosition;" +
                    "   vColor = aColor;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 vColor;" +
                    "uniform vec3 lightPosition;" +
                    "uniform vec3 viewPosition;" +
                    "uniform float shininess;" +
                    "varying vec3 aPosition;" +
                    "varying vec3 aNormal;" +
                    "void main(){" +
                    "   // 1. Luz Ambiental (Mantiene el cubo blanco siempre visible)\n" +
                    "   float ambientStrength = 0.5;" +
                    "   vec3 ambient = ambientStrength * vColor.rgb;" +
                    "   " +
                    "   // 2. Luz Difusa (Le da volumen 3D)\n" +
                    "   vec3 N = normalize(aNormal);" +
                    "   vec3 L = normalize(lightPosition - aPosition);" +
                    "   float diff = max(dot(N, L), 0.0);" +
                    "   vec3 diffuse = diff * vColor.rgb;" +
                    "   " +
                    "   // 3. Luz Especular (El brillo)\n" +
                    "   vec3 V = normalize(viewPosition - aPosition);" +
                    "   vec3 R = reflect(-L, N);" +
                    "   float spec = pow(max(dot(V, R), 0.0), shininess);" +
                    "   vec3 specular = vec3(1.0, 1.0, 1.0) * spec;" +
                    "   " +
                    "   // Combinación final\n" +
                    "   vec3 result = ambient + diffuse + specular;" +
                    "   gl_FragColor = vec4(result, 1.0);" + // 1.0 de opacidad
                    "}";

    public Cube() {
        ByteBuffer bb = ByteBuffer.allocateDirect(cubeCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(cubeCoords);
        vertexBuffer.position(0);

        ByteBuffer nb = ByteBuffer.allocateDirect(normals.length*4);
        nb.order(ByteOrder.nativeOrder());
        normalBuffer = nb.asFloatBuffer();
        normalBuffer.put(normals);
        normalBuffer.position(0);

        ByteBuffer ib = ByteBuffer.allocateDirect(indices.length * 2);
        ib.order(ByteOrder.nativeOrder());
        indexBuffer = ib.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        int vertexShader =
                MyGLRenderer.loadShader(
                        GLES20.GL_VERTEX_SHADER,
                        vertexShaderCode
                );

        int fragmentShader =
                MyGLRenderer.loadShader(
                        GLES20.GL_FRAGMENT_SHADER,
                        fragmentShaderCode
                );

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    public void draw(float[] mvpMatrix, float[] mvMatrix) {

        GLES20.glUseProgram(mProgram);

        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, COORDS_PER_VERTEX * 4, vertexBuffer);

        normalHandle = GLES20.glGetAttribLocation(mProgram, "vNormal");
        GLES20.glEnableVertexAttribArray(normalHandle);
        GLES20.glVertexAttribPointer(
                normalHandle, 3,
                GLES20.GL_FLOAT, false,
                3*4, normalBuffer
        );

        colorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");
        GLES20.glEnableVertexAttribArray(colorHandle);
        vertexBuffer.position(3);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, COORDS_PER_VERTEX * 4, vertexBuffer);
        vertexBuffer.position(0);

        //Clase anterior inicio
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        int mvMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVMatrix");
        GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvMatrix,0);

        int lightHandle = GLES20.glGetUniformLocation(mProgram, "lightPosition");
        GLES20.glUniform3f(lightHandle, 0.0f, 0.0f, 3.0f);

        int viewHandle = GLES20.glGetUniformLocation(mProgram, "viewPosition"); // Posición del observador en espacio ojo (en espacio ojo el observador está en el origen)
        GLES20.glUniform3f(viewHandle, 0.0f, 0.0f, 0.0f);

        int shinHandle = GLES20.glGetUniformLocation(mProgram, "shininess");
        GLES20.glUniform1f(shinHandle, 64.0f); // Valor razonable de brillo

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);


        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }
}