package com.example.firstapplication;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class GLView extends GLSurfaceView {
    private final GLRenderer renderer;

    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Check if the system supports OpenGL ES 2.0
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context
            setEGLContextClientVersion(2);

            // Set the renderer to our demo renderer, defined below
            renderer = new GLRenderer(context);
            setRenderer(renderer);

            // Only render if the view changes
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2
            throw new UnsupportedOperationException("OpenGL ES 2.0 not supported on this device.");
        }
    }
}
