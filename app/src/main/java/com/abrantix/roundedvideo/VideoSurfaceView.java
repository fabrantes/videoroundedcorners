/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abrantix.roundedvideo;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class has been adapted from
 *
 * https://code.google.com/p/android-source-browsing/source/browse/tests/tests/media/src/
 * android/media/cts/VideoSurfaceView.java?repo=platform--cts
 *
 * {@link android.opengl.GLSurfaceView} subclass that displays video on the screen with the ability
 * to round its corners.
 *
 * It creates a GL texture and from that generates a {@link android.graphics.SurfaceTexture} and an
 * associated {@link android.view.Surface} which it binds to a given
 * {@link android.media.MediaPlayer}. The {@link android.media.MediaPlayer} will draw the decoded
 * video frames directly on to the GL texture which afterwards is rendered on the screen mapped to
 * a given (rounded corner) geometry.
 *
 * To set adjust the rounded corners use {@link #setCornerRadius(float, float, float, float)}.
 *
 */
public class VideoSurfaceView extends GLSurfaceView {
    private static final String TAG = "VideoSurfaceView";

    VideoRenderer mRenderer;
    MediaPlayer mMediaPlayer = null;

    public VideoSurfaceView(Context context) {
        super(context);
        init(new VideoRenderer(this));
    }

    public VideoSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(new VideoRenderer(this));
    }

    VideoSurfaceView(Context context, @NonNull VideoRenderer videoRender) {
        super(context);
        init(videoRender);
    }

    private void init(@NonNull VideoRenderer videoRender) {
        setEGLContextClientVersion(2);
        setTranslucent(true);
        mRenderer = videoRender;
        setRenderer(mRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    private void setTranslucent(boolean translucent) {
        if (translucent) {
            setZOrderOnTop(true);
            setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            this.getHolder().setFormat(PixelFormat.RGBA_8888);
        } else {
            this.getHolder().setFormat(PixelFormat.RGB_565);
        }
    }

    public void setCornerRadius(float radius) {
        setCornerRadius(radius, radius, radius, radius);
    }

    public void setCornerRadius(float topLeft, float topRight, float bottomRight,
                                float bottomLeft) {
        mRenderer.setCornerRadius(topLeft, topRight, bottomRight, bottomLeft);
    }

    // TODO
    public void setVideoAspectRatio(float aspectRatio) {
        mRenderer.setVideoAspectRatio(aspectRatio);
    }

    @Override
    public void onResume() {
        queueEvent(new Runnable(){
            public void run() {
                mRenderer.setMediaPlayer(mMediaPlayer);
            }});

        super.onResume();
    }

    public void setMediaPlayer(@Nullable MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
        if (mRenderer != null) {
            mRenderer.setMediaPlayer(mediaPlayer);
        }
    }

    private static class VideoRenderer
            implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
        private static String TAG = "VideoRender";

        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

        private final String mVertexShader =
                "uniform mat4 uMVPMatrix;\n" +
                        "uniform mat4 uSTMatrix;\n" +
                        "attribute vec4 aPosition;\n" +
                        "attribute vec4 aTextureCoord;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "void main() {\n" +
                        "  gl_Position = uMVPMatrix * aPosition;\n" +
                        "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                        "}\n";

        private final String mFragmentShader =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                        "}\n";

        private float[] mMVPMatrix = new float[16];
        private float[] mSTMatrix = new float[16];

        private int mProgram;
        private int mTextureID;
        private int muMVPMatrixHandle;
        private int muSTMatrixHandle;
        private int maPositionHandle;
        private int maTextureHandle;

        private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

        private final GLSurfaceView mGLSurfaceView;
        private MediaPlayer mMediaPlayer;
        private SurfaceTexture mSurfaceTexture;
        private boolean mUpdateSurface = false;

        private float[] mTriangleVerticesData;
        private FloatBuffer mTriangleVertices;
        private RectF mRoundRadius = new RectF();
        private GLRoundedGeometry mRoundedGeometry;
        private final Point mViewPortSize = new Point();
        private final RectF mViewPortGLBounds;

        public VideoRenderer(@NonNull GLSurfaceView view) {
            this(view, new GLRoundedGeometry(), new RectF(-1, 1, 1, -1));
        }

        public VideoRenderer(@NonNull GLSurfaceView view,
                             @NonNull GLRoundedGeometry roundedGeometry,
                             @NonNull RectF viewPortGLBounds) {
            mGLSurfaceView = view;
            mRoundedGeometry = roundedGeometry;
            mViewPortGLBounds = viewPortGLBounds;
            mViewPortSize.set(1, 1); // init this with a non-zero size

            Matrix.setIdentityM(mSTMatrix, 0);
        }

        public void setCornerRadius(float topLeft, float topRight, float bottomRight,
                                    float bottomLeft) {
            mRoundRadius.left = topLeft;
            mRoundRadius.top = topRight;
            mRoundRadius.right = bottomRight;
            mRoundRadius.bottom = bottomLeft;
            if (mViewPortSize.x > 1) {
                updateVertexData();
            }
        }

        private void updateVertexData() {
            mTriangleVerticesData = mRoundedGeometry.generateVertexData(mRoundRadius,
                    mViewPortGLBounds, mViewPortSize);
            if (mTriangleVertices != null) {
                mTriangleVertices.clear();
            } else {
                mTriangleVertices = ByteBuffer.allocateDirect(
                        mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
            }
            mTriangleVertices.put(mTriangleVerticesData).position(0);
        }

        public void setMediaPlayer(MediaPlayer player) {
            mMediaPlayer = player;
            if (mSurfaceTexture != null) {
                Surface surface = new Surface(mSurfaceTexture);
                mMediaPlayer.setSurface(surface);
                surface.release();

                try {
                    mMediaPlayer.prepare();
                } catch (IOException t) {
                    Log.e(TAG, "media player prepare failed");
                }
            }
        }

        public void onDrawFrame(GL10 glUnused) {
            synchronized(this) {
                if (mUpdateSurface) {
                    mSurfaceTexture.updateTexImage();
                    mSurfaceTexture.getTransformMatrix(mSTMatrix);
                    mUpdateSurface = false;
                }
            }

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray maTextureHandle");

            Matrix.setIdentityM(mMVPMatrix, 0);
            Matrix.scaleM(mMVPMatrix, 0, 1f, 1f, 1f);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mTriangleVerticesData.length / 5);
            checkGlError("glDrawArrays");
            GLES20.glFinish();
        }

        public void onSurfaceChanged(GL10 glUnused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            mViewPortSize.set(width, height);
            updateVertexData();
        }

        public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
            mProgram = createProgram(mVertexShader, mFragmentShader);
            if (mProgram == 0) {
                return;
            }
            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkGlError("glGetAttribLocation aPosition");
            if (maPositionHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aPosition");
            }
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            checkGlError("glGetAttribLocation aTextureCoord");
            if (maTextureHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }

            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            checkGlError("glGetUniformLocation uMVPMatrix");
            if (muMVPMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uMVPMatrix");
            }

            muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
            checkGlError("glGetUniformLocation uSTMatrix");
            if (muSTMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMatrix");
            }

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);

            mTextureID = textures[0];
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
            checkGlError("glBindTexture mTextureID");

            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);

            /*
             * Create the SurfaceTexture that will feed this textureID,
             * and pass it to the MediaPlayer
             */
            mSurfaceTexture = new SurfaceTexture(mTextureID);
            mSurfaceTexture.setOnFrameAvailableListener(this);

            if (mMediaPlayer != null) {
                Surface surface = new Surface(mSurfaceTexture);
                mMediaPlayer.setSurface(surface);
                surface.release();
                try {
                    mMediaPlayer.prepare();
                } catch (IOException t) {
                    Log.e(TAG, "media player prepare failed");
                }
            }

            synchronized(this) {
                mUpdateSurface = false;
            }
        }

        synchronized public void onFrameAvailable(SurfaceTexture surface) {
            mUpdateSurface = true;
            mGLSurfaceView.requestRender();
        }

        private int loadShader(int shaderType, String source) {
            int shader = GLES20.glCreateShader(shaderType);
            if (shader != 0) {
                GLES20.glShaderSource(shader, source);
                GLES20.glCompileShader(shader);
                int[] compiled = new int[1];
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
                if (compiled[0] == 0) {
                    Log.e(TAG, "Could not compile shader " + shaderType + ":");
                    Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                    GLES20.glDeleteShader(shader);
                    shader = 0;
                }
            }
            return shader;
        }

        private int createProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }

            int program = GLES20.glCreateProgram();
            if (program != 0) {
                GLES20.glAttachShader(program, vertexShader);
                checkGlError("glAttachShader");
                GLES20.glAttachShader(program, pixelShader);
                checkGlError("glAttachShader");
                GLES20.glLinkProgram(program);
                int[] linkStatus = new int[1];
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e(TAG, "Could not link program: ");
                    Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                    GLES20.glDeleteProgram(program);
                    program = 0;
                }
            }
            return program;
        }

        private void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }

        public void setVideoAspectRatio(float aspectRatio) {
            // TODO
        }
    }

}