package com.example.firstapplication;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    private ObjLoader objLoader;

    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    private int program;
    private int positionHandle;
    private int colorHandle;
    private int normalHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;

    private final int mBytesPerFloat = 4;
    private final int mPositionDataSize = 3;
    private final int mColorDataSize = 4;
    private final int mNormalDataSize = 3;

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer = createColorBuffer();
    private FloatBuffer normalBuffer;
    private FloatBuffer textureBuffer;

    public GLRenderer(Context context) {
        this.context = context;
        this.objLoader = new ObjLoader(context, "laurel.obj");

        setupBuffers();

//        logVertexBuffer("vertexBuffer", vertexBuffer);
//        logVertexBuffer("normalBuffer", normalBuffer);
//        logVertexBuffer("textureBuffer", textureBuffer);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background clear color to gray
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Position the eye behind the origin
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 1.5f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        String vertexShaderCode =
                "uniform mat4 uMVPMatrix;           \n"     // A constant representing the combined model/view/projection matrix
                + "attribute vec4 aPosition;        \n"		// Per-vertex position information we will pass in
                + "attribute vec4 aColor;           \n"		// Per-vertex color information we will pass in
                + "attribute vec3 aNormal;          \n"		// Per-vertex normal information we will pass in

                + "varying vec4 vColor;             \n"		// This will be passed into the fragment shader

                + "void main() {                    \n"		// The entry point for our vertex shader
                + "   vColor = aColor;              \n"		// Pass the color through to the fragment shader
                + "   vec3 lightDirection = normalize(vec3(0.0, 0.0, 1.0));     \n"
                + "   float lightIntensity = dot(aNormal, lightDirection);      \n"

                + "   gl_Position = uMVPMatrix      \n" 	// gl_Position is a special variable used to store the final position
                + "               * aPosition;      \n"     // Multiply the vertex by the matrix to get the final point in
                + "}                                \n";    // normalized screen coordinates

        String fragmentShaderCode =
                "precision mediump float;           \n"     // Set the default precision to medium
                + "varying vec4 vColor;             \n"		// This is the color from the vertex shader interpolated across the
                                                            // triangle per fragment
                + "void main() {                    \n"		// The entry point for our fragment shader
                + "   gl_FragColor = vColor;        \n"		// Pass the color directly through the pipeline
                + "}                                \n";

        // Load in the vertex shader.
        int vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);

        // Load in the fragment shader shader
        int fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // Create a program object and store the handle to it
        program = GLES20.glCreateProgram();

        if (program != 0) {
            // Bind the vertex shader to the program
            GLES20.glAttachShader(program, vertexShaderHandle);

            // Bind the fragment shader to the program
            GLES20.glAttachShader(program, fragmentShaderHandle);

            // Bind attributes
            GLES20.glBindAttribLocation(program, 0, "aPosition");
            GLES20.glBindAttribLocation(program, 1, "aColor");
            GLES20.glBindAttribLocation(program, 2, "aNormal");

            // Link the two shaders together into a program
            GLES20.glLinkProgram(program);

            // Get the link status
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }

        if (program == 0) {
            throw new RuntimeException("Error creating program.");
        }

        // Set program handles. These will later be used to pass in values to the program
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        colorHandle = GLES20.glGetAttribLocation(program, "aColor");
        normalHandle = GLES20.glGetAttribLocation(program, "aNormal");
//        textureCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(program);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 10.0f;

        Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.scaleM(modelMatrix,0,0.1f, 0.1f,0.1f);

        for (int i = 0; i < vertexBuffer.capacity(); i = i + 9) {
            vertexBuffer.position(i);
            vertexBuffer.limit(i + 9);
            colorBuffer.position(0);
            normalBuffer.position(i);
            normalBuffer.limit(i + 9);
            drawTriangle(vertexBuffer, colorBuffer, normalBuffer);
        }

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }

    private void setupBuffers() {
        // Allocate buffers for model data
        vertexBuffer = ByteBuffer.allocateDirect(objLoader.positions.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(objLoader.positions).position(0);

        normalBuffer = ByteBuffer.allocateDirect(objLoader.normals.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalBuffer.put(objLoader.normals).position(0);

        textureBuffer = ByteBuffer.allocateDirect(objLoader.textureCoordinates.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(objLoader.textureCoordinates).position(0);
    }

    private int loadShader(int type, String shaderCode) {
        int shaderHandle = GLES20.glCreateShader(type);

        if (shaderHandle != 0) {
            // Pass in the shader source
            GLES20.glShaderSource(shaderHandle, shaderCode);

            // Compile the shader
            GLES20.glCompileShader(shaderHandle);

            // Get the compilation status
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0) {
            throw new RuntimeException("Error creating fragment shader.");
        }

        return shaderHandle;
    }

    private void drawTriangle(FloatBuffer vertex, FloatBuffer color, FloatBuffer normal) {
        // Pass in the position information
        GLES20.glVertexAttribPointer(positionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, mPositionDataSize * mBytesPerFloat, vertex);
        GLES20.glEnableVertexAttribArray(positionHandle);

        // Pass in the color information
        GLES20.glVertexAttribPointer(colorHandle, mColorDataSize, GLES20.GL_FLOAT, false, mColorDataSize * mBytesPerFloat, color);
        GLES20.glEnableVertexAttribArray(colorHandle);

        // Pass in the normal information
        GLES20.glVertexAttribPointer(normalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, mNormalDataSize * mBytesPerFloat, normal);
        GLES20.glEnableVertexAttribArray(normalHandle);

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
    }

    // Method to log the content of the FloatBuffer
    private void logVertexBuffer(String bufferName, FloatBuffer buffer) {
        // Rewind the buffer to make sure we log all content
        buffer.rewind();

        StringBuilder sb = new StringBuilder();
        sb.append("Buffer " + bufferName + ": ");

        while (buffer.hasRemaining()) {
            sb.append(buffer.get()).append(" ");
        }

        // Log the content of the buffer
        Log.d("GLRenderer", sb.toString());
    }

    private FloatBuffer createColorBuffer() {
        // Số phần tử của color buffer
        int numElements = 3;

        // Màu vàng với định dạng RGBA (R=1.0, G=1.0, B=0.0, A=1.0)
        float[] yellowColor = {1.0f, 1.0f, 0.0f, 1.0f};

        // Tạo một FloatBuffer với dung lượng numElements * 4 (vì mỗi màu có 4 giá trị float)
        FloatBuffer colorBuffer = ByteBuffer.allocateDirect(numElements * 4 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        // Điền màu vàng vào buffer
        for (int i = 0; i < numElements; i++) {
            colorBuffer.put(yellowColor);
        }

        // Đặt lại vị trí của buffer về đầu
        colorBuffer.position(0);

        return colorBuffer;
    }
}
