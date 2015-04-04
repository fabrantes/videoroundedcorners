package com.abrantix.roundedvideo.example;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.util.Random;

/**
 * Created by fabrantes on 02/04/15.
 */
public class WickedGradientDrawable extends Drawable {
    private int mTopColor;
    private int mBottomColor;
    private int mITopColor = Color.WHITE;
    private int mIBottomColor = Color.WHITE;
    private int mTTopColor = Color.GREEN;
    private int mTBottomColor = Color.MAGENTA;
    private Interpolator mInterpolator = new LinearInterpolator();
    private int mCounter = 0;
    private int mDuration = 1999;
    private long mLastTimestamp = SystemClock.elapsedRealtime();
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);


    @Override
    public void draw(Canvas canvas) {
        final long dt = SystemClock.elapsedRealtime() - mLastTimestamp;
        mLastTimestamp = SystemClock.elapsedRealtime();
        mCounter = Math.min(mDuration, mCounter + (int) dt);
        final float interpolation = mCounter / (float) mDuration;

        mTopColor = Color.argb(0xff,
                (int) (Color.red(mITopColor) + (Color.red(mTTopColor) - Color.red(mITopColor)) *
                        mInterpolator.getInterpolation(interpolation)),
                (int) (Color.green(mITopColor) + (Color.green(mTTopColor) - Color.green(mITopColor))
                        * mInterpolator.getInterpolation(interpolation)),
                (int) (Color.blue(mITopColor) + (Color.blue(mTTopColor) - Color.blue(mITopColor)) *
                        mInterpolator.getInterpolation(interpolation)));
        mBottomColor = Color.argb(0xff,
                (int) (Color.red(mIBottomColor) + (Color.red(mTBottomColor) - Color.red(mIBottomColor)) *
                        mInterpolator.getInterpolation(interpolation)),
                (int) (Color.green(mIBottomColor) + (Color.green(mTBottomColor) - Color.green(mIBottomColor))
                        * mInterpolator.getInterpolation(interpolation)),
                (int) (Color.blue(mIBottomColor) + (Color.blue(mTBottomColor) - Color.blue(mIBottomColor)) *
                        mInterpolator.getInterpolation(interpolation)));

        final LinearGradient gradient = new LinearGradient(0, 0, 0, canvas.getHeight(), mTopColor,
                mBottomColor, Shader.TileMode.CLAMP);
        mPaint.setShader(gradient);
        canvas.drawRect(getBounds(), mPaint);

        if (mCounter >= mDuration) {
            mCounter = 0;
            mIBottomColor = mTBottomColor;
            mITopColor = mTTopColor;
            mTBottomColor = Color.argb(0xff,
                    new Random().nextInt(0xff),
                    new Random().nextInt(0xff),
                    new Random().nextInt(0xff));
            mTTopColor = Color.argb(0xff,
                    new Random().nextInt(0xff),
                    new Random().nextInt(0xff),
                    new Random().nextInt(0xff));
        }

        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) { }

    @Override
    public void setColorFilter(ColorFilter cf) { }

    @Override
    public int getOpacity() {
        return 0xff;
    }
}
