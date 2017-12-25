package com.test.airhockey;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.test.airhockey.util.LoggerConfig;
import com.test.airhockey.util.MatrixHelper;
import com.test.airhockey.util.ShaderHelper;
import com.test.airhockey.util.TextResourceReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;


/**
 * Created by IGG on 2017/12/21.
 */

public class AirHockeyRenderer implements GLSurfaceView.Renderer {

    private FloatBuffer vertexData;
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COMPONENT_COUNT = 4;
    private static final int COLOR_COMPONENT_COUNT = 3;

    private static final int STRIDE =
            (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private Context context;
    private int program;

    private static final String U_COLOR = "u_Color";
    private static final String A_COLOR = "a_Color";
    private static final String A_POSITION = "a_Position";
    private static final String U_MATRIX = "u_Matrix";

    private int uColorLocation;
    private int aPositionLocation;
    private int aColorLocation;
    private int uMatrixLocation;

    private float[] projectionMatrix = new float[16];
    private float[] modelMatrix = new float[16];

    public AirHockeyRenderer(Context context) {
        // This constructor shouldn't be called -- only kept for showing
        // evolution of the code in the chapter.
        vertexData = null;
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        //
        // Vertex data is stored in the following manner:
        //
        // The first two numbers are part of the position: X, Y
        // The next three numbers are part of the color: R, G, B
        //
        float[] tableVerticesWithTriangles = {
                // Order of coordinates: X, Y, Z, W, R, G, B

                // Triangle Fan
                0f,    0f, 0f, 1.5f,   1f,   1f,   1f,
                -0.5f, -0.8f, 0f,   1f, 0.7f, 0.7f, 0.7f,
                0.5f, -0.8f, 0f,   1f, 0.7f, 0.7f, 0.7f,
                0.5f,  0.8f, 0f,   2f, 0.7f, 0.7f, 0.7f,
                -0.5f,  0.8f, 0f,   2f, 0.7f, 0.7f, 0.7f,
                -0.5f, -0.8f, 0f,   1f, 0.7f, 0.7f, 0.7f,

                // Line 1
                -0.5f, 0f, 0f, 1.5f, 1f, 0f, 0f,
                0.5f, 0f, 0f, 1.5f, 1f, 0f, 0f,

                // Mallets
                0f, -0.4f, 0f, 1.25f, 0f, 0f, 1f,
                0f,  0.4f, 0f, 1.75f, 1f, 0f, 0f
        };

        vertexData = ByteBuffer
                .allocateDirect(tableVerticesWithTriangles.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        vertexData.put(tableVerticesWithTriangles);


        String vertexShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.simple_vertex_shader);
        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.simple_fragment_shader);

        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);


        program = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program);
        }

        GLES20.glUseProgram(program);

        uMatrixLocation = GLES20.glGetUniformLocation(program, U_MATRIX);

       // uColorLocation = GLES20.glGetUniformLocation(program, U_COLOR);
        aColorLocation = GLES20.glGetAttribLocation(program, A_COLOR);
        aPositionLocation = GLES20.glGetAttribLocation(program, A_POSITION);


        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_POSITION_LOCATION.
        vertexData.position(0);
        GLES20.glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GLES20.GL_FLOAT,
                false, STRIDE, vertexData);
        GLES20.glEnableVertexAttribArray(aPositionLocation);

        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_COLOR_LOCATION.
        vertexData.position(POSITION_COMPONENT_COUNT);
        GLES20.glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT,  GLES20.GL_FLOAT,
                false, STRIDE, vertexData);

        GLES20.glEnableVertexAttribArray(aColorLocation);

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        /*
        final float aspectRatio = width > height ?
                (float) width / (float) height :
                (float) height / (float) width;

        if (width > height) {
            // Landscape
            orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);
        } else {
            // Portrait or square
            orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
        }
        */



        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width
                / (float) height, 1f, 10f);

        setIdentityM(modelMatrix, 0);

        translateM(modelMatrix, 0, 0f, 0f, -2.5f);
        rotateM(modelMatrix, 0, -60f, 1f, 0f, 0f);

        final float[] temp = new float[16];
        multiplyMM(temp, 0, projectionMatrix, 0, modelMatrix, 0);
        System.arraycopy(temp, 0, projectionMatrix, 0, temp.length);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


        // Assign the matrix
        glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);

        // Draw the table.
       // GLES20.glUniform4f(uColorLocation, 1.0f, 0f, 1.0f, 1.0f);
      //  GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 6, 6);

        // Draw the table.
       // GLES20.glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 6);



        // Draw the center dividing line.
       // GLES20.glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 6, 2);

        // Draw the first mallet blue.
      //  GLES20.glUniform4f(uColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 8, 1);

        // Draw the second mallet red.
      //  GLES20.glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 9, 1);
    }
}
