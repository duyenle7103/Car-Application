package com.example.firstapplication;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    private ObjLoader objLoader;
    private static final String TAG = "GLRenderer";

    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private float[] lightModelMatrix = new float[16];

    private int program;
    private int pointProgramHandle;
    private int positionHandle;
    private int colorHandle;
    private int normalHandle;
    private int intersectionCoordHandle;
    private int lightPosHandle;
    private int mvMatrixHandle;
    private int mvpMatrixHandle;

    private final int mBytesPerFloat = 4;
    private final int mPositionDataSize = 3;
    private final int mColorDataSize = 4;
    private final int mNormalDataSize = 3;

    private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
    private final float[] mLightPosInWorldSpace = new float[4];
    private final float[] mLightPosInEyeSpace = new float[4];

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
        // Set the background clear color to black
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
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
        final float upY = 10.0f;
        final float upZ = 0.0f;

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShaderCode = getVertexShader();
        final String fragmentShaderCode = getFragmentShader();

        final int vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        final int fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[] {"aPosition",  "aColor", "aNormal"});

        final String pointVertexShader = getPointVertexShader();
        final String pointFragmentShader = getPointFragmentShader();

        final int pointVertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        pointProgramHandle = createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle, new String[] {"aPosition"});
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
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

        // Tell OpenGL to use this program when rendering
        GLES20.glUseProgram(program);

        // Set program handles for drawing
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        mvMatrixHandle = GLES20.glGetUniformLocation(program, "uMVMatrix");
//        lightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos");
//        intersectionCoordHandle = GLES20.glGetUniformLocation(program, "aIntersectionCoord");
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        colorHandle = GLES20.glGetAttribLocation(program, "aColor");
        normalHandle = GLES20.glGetAttribLocation(program, "aNormal");

//        textureCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");

        // Calculate position of the light. Rotate and then push into the distance
//        Matrix.setIdentityM(lightModelMatrix, 0);
//        Matrix.translateM(lightModelMatrix, 0, 1.0f, 1.0f, -2.0f);
//
//        Matrix.multiplyMV(mLightPosInWorldSpace, 0, lightModelMatrix, 0, mLightPosInModelSpace, 0);
//        Matrix.multiplyMV(mLightPosInEyeSpace, 0, viewMatrix, 0, mLightPosInWorldSpace, 0);

        // Draw triangle
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

//        GLES20.glUseProgram(pointProgramHandle);
//        drawLight();

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
//        GLES20.glDisableVertexAttribArray(textureCoordHandle);
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

    private void setupBuffers() {
        // Allocate buffers for model data
        vertexBuffer = ByteBuffer.allocateDirect(objLoader.positions.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(objLoader.positions).position(0);

        colorBuffer = createColorBuffer();

        normalBuffer = ByteBuffer.allocateDirect(objLoader.normals.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalBuffer.put(objLoader.normals).position(0);

        textureBuffer = ByteBuffer.allocateDirect(objLoader.textureCoordinates.length * mBytesPerFloat)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(objLoader.textureCoordinates).position(0);
    }

    protected String getVertexShader() {
        final String vertexShader =
                "uniform mat4 uMVPMatrix;           \n"     // A constant representing the combined model/view/projection matrix
                + "uniform mat4 uMVMatrix;          \n"     // A constant representing the combined model/view matrix

                + "attribute vec4 aPosition;        \n"		// Per-vertex position information we will pass in
                + "attribute vec4 aColor;           \n"		// Per-vertex color information we will pass in
                + "attribute vec3 aNormal;          \n"		// Per-vertex normal information we will pass in

                + "varying vec3 vPosition;          \n"		// This will be passed into the fragment shader
                + "varying vec4 vColor;             \n"		// This will be passed into the fragment shader
                + "varying vec3 vNormal;            \n"		// This will be passed into the fragment shader

                + "void main() {                    \n"		// The entry point for our vertex shader
                + "   vPosition = vec3(uMVMatrix * aPosition);                          \n"     // Transform the vertex into eye space
                + "   vColor = aColor;                                                  \n"     // Pass through the color
                + "   vNormal = vec3(uMVMatrix * vec4(aNormal, 0.0));                   \n"     // Transform the normal's orientation into eye space

                + "   gl_Position = uMVPMatrix      \n" 	// gl_Position is a special variable used to store the final position
                + "               * aPosition;      \n"     // Multiply the vertex by the matrix to get the final point in
                + "}                                \n";    // normalized screen coordinates

        return vertexShader;
    }

    protected String getFragmentShader() {
        final String fragmentShader =
                "precision mediump float;           \n"     // Set the default precision to medium
                + "uniform vec3 uLightPos;          \n"	    // The position of the light in eye space

                + "varying vec3 vPosition;		    \n"		// Interpolated position for this fragment
                + "varying vec4 vColor;             \n"		// This is the color from the vertex shader interpolated across the
                                                            // triangle per fragment
                + "varying vec3 vNormal;            \n"		// Interpolated normal for this fragment

                + "void main() {                    \n"		// The entry point for our fragment shader
//                + "   float distance = length(uLightPos - vPosition);                       \n"
//                + "   vec3 lightVector = normalize(uLightPos - vPosition);                  \n"
//                + "   float diffuse = max(dot(vNormal, lightVector), 0.1);                  \n"
//                + "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));     \n"
//                + "   gl_FragColor = vColor * diffuse;                                      \n"
                + "   gl_FragColor = vColor;        \n"
                + "}                                \n";

        return fragmentShader;
    }

    protected String getPointVertexShader() {
        final String pointVertexShader =
                "uniform mat4 uMVPMatrix;           \n"
                + "attribute vec4 aPosition;        \n"
                + "void main() {                    \n"
                + "   gl_Position = uMVPMatrix      \n"
                + "               * aPosition;      \n"
                + "   gl_PointSize = 5.0;           \n"
                + "}                                \n";

        return pointVertexShader;
    }

    protected String getPointFragmentShader() {
        final String pointFragmentShader =
                "precision mediump float;       \n"
                + "void main() {                \n"
                + "   gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);  \n"
                + "}                            \n";

        return pointFragmentShader;
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

    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            // Bind the vertex shader to the program
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            if (attributes != null) {
                final int size = attributes.length;
                for (int i = 0; i < size; i++) {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }

            // Link the two shaders together into a program
            GLES20.glLinkProgram(programHandle);

            // Get the link status
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }

    private void drawLight() {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(pointProgramHandle, "uMVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(pointProgramHandle, "aPosition");

        // Pass in the position
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, lightModelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }


    private void drawTriangle(FloatBuffer vertex, FloatBuffer color, FloatBuffer normal) {
        logVertexBuffer("vertexBuffer", vertex);
        logVertexBuffer("colorBuffer", color);
        logVertexBuffer("normalBuffer", normal);

        logMatrix(TAG, "modelMatrix", modelMatrix);
        logMatrix(TAG, "viewMatrix", viewMatrix);
        logMatrix(TAG, "projectionMatrix", projectionMatrix);


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

        logMatrix(TAG, "mvMatrix", mvpMatrix);

        GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvpMatrix, 0);

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        logMatrix(TAG, "mvpMatrix", mvpMatrix);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        Log.d(TAG, "mLightPosInEyeSpace: " + mLightPosInEyeSpace[0] + ", " + mLightPosInEyeSpace[1] + ", " + mLightPosInEyeSpace[2]);

        GLES20.glUniform3f(lightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
    }

    private void logVertexBuffer(String bufferName, FloatBuffer buffer) {
        // Rewind the buffer to make sure we log all content
        buffer.rewind();

        StringBuilder sb = new StringBuilder();
        sb.append("Buffer " + bufferName + ": ");

        while (buffer.hasRemaining()) {
            sb.append(buffer.get()).append(" ");
        }

        // Log the content of the buffer
        Log.d(TAG, sb.toString());
    }

    private void logMatrix(String tag, String matrixName, float[] matrix) {
        StringBuilder sb = new StringBuilder();
        sb.append(matrixName).append(":\n");
        for (int i = 0; i < 4; i++) {
            sb.append(matrix[4 * i]).append(", ")
                    .append(matrix[4 * i + 1]).append(", ")
                    .append(matrix[4 * i + 2]).append(", ")
                    .append(matrix[4 * i + 3]).append("\n");
        }
        Log.d(tag, sb.toString());
    }
}
