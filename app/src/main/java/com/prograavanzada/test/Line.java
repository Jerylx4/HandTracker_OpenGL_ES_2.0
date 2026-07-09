package com.prograavanzada.test;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Line {
    private final FloatBuffer vertexBuffer; /*Donde se almacenan las coordenadas de los vertices,
                                              pero cuando estan transformados a buffer*/
    private final int mProgram; //Combinacion del vertex con el fragment, controla como se dibuja el objeto
    private int positionHandle, colorHandle; //Son identificadores la ubicacion de la posicion y la ubicacion del color

    private float color[];
    private float grosor;

    public Line(float x1, float y1, float x2, float y2, float grosor, float[] color){
        this.color = color;
        this.grosor = grosor;
        float[] lineCoord = {
                x1, y1, 0.0f, //v0
                x2, y2, 0.0f //v1
        };

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(lineCoord.length*4); //Reservamos en la memoria nativa
        byteBuffer.order(ByteOrder.nativeOrder()); //Definir el orden de los bytes, se le dice que orden de bytes se va a usar, el orden nativo del dispositivo
        vertexBuffer = byteBuffer.asFloatBuffer();
        vertexBuffer.put(lineCoord);
        vertexBuffer.position(0);

        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode); //Cargar el codigo del shader, se le dice que tipo de shader es y el codigo del shader
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode); //Cargar el codigo del shader, se le dice que tipo de shader es y el codigo del shader

        mProgram = GLES20.glCreateProgram(); //Crear un programa vacio, se le asigna un id para poder referenciarlo posteriormente
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }


    private final String vertexShaderCode =
            "attribute vec4 vPosition;" + //Recibe la posicion del vertice
                    "void main() {" +
                    "  gl_Position = vPosition;" + //Asignar la posicion del vertice a la variable predefinida gl_Position donde va a dibujar en pantalla
                    "}";
    private final String fragmentShaderCode =
            "precision mediump float;" + //Definir la precision de los numeros flotantes
                    "uniform vec4 vColor;" + //Recibe el color del punto
                    "void main() {" +
                    "  gl_FragColor = vColor;" + //Asignar el color del punto a la variable predefinida gl_FragColor
                    "}";

    public void draw(){
        GLES20.glUseProgram(mProgram); //usa el espacio ya definido por el programa
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition"); //Obtener la ubicacion de los vertices en el shader, se le da el id del programa y el nombre de la variable en el shader
        GLES20.glEnableVertexAttribArray(positionHandle);

        //Especificar como se deben leer los vertices del buffer, se le da el id del programa y
        // el nombre de la variable en el shader, el numero de coordenadas por vertice, el tipo de dato,
        // si se normalizan o no, el salto entre cada vertice y el buffer donde se encuentran los vertices
        GLES20.glVertexAttribPointer(positionHandle,
                3,
                GLES20.GL_FLOAT,
                false, //No se normalizan los datos, se le dice que no se van a normalizar porque ya estan en el rango de -1 a 1
                0, //No hay salto entre cada vertice, se le dice que no hay salto porque los vertices estan almacenados de forma contigua en el buffer
                vertexBuffer
        );

        colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor"); //Obtener la ubicacion del color en el shader, se le da el id del programa y el nombre de la variable en el shader
        GLES20.glUniform4fv(colorHandle, 1, color, 0); //Especificar el color, enviamos el color al fragment shader con el id

        GLES20.glLineWidth(grosor); //Establecer el grosor de la linea, se le da el grosor en pixeles

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2); //Dibujar la linea, se le dice que tipo de primitiva es, desde donde empieza y cuantos vertices se van a dibujar
        GLES20.glDisableVertexAttribArray(positionHandle); //Deshabilitar el arreglo de posicion para que no afecte a otros objetos que se dibujen posteriormente, proceso de limpieza

    }

}
