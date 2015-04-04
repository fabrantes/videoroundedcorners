package com.abrantix.roundedvideo.example;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.abrantix.roundedvideo.R;
import com.abrantix.roundedvideo.VideoSurfaceView;

import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    private VideoSurfaceView[] mVideoSurfaceView = new VideoSurfaceView[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int radius = getResources()
                .getDimensionPixelOffset(R.dimen.corner_radius_video);

        final String[] dataSources = new String[] {
                "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4",
                "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4",
                "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"
        };

        mVideoSurfaceView[0] = (VideoSurfaceView) findViewById(R.id.video_surface_view1);
        mVideoSurfaceView[1] = (VideoSurfaceView) findViewById(R.id.video_surface_view2);
        mVideoSurfaceView[2] = (VideoSurfaceView) findViewById(R.id.video_surface_view3);

        mVideoSurfaceView[0].setCornerRadius(radius);
        mVideoSurfaceView[1].setCornerRadius(radius);
        mVideoSurfaceView[2].setCornerRadius(radius);

        for (int i = 0; i < mVideoSurfaceView.length; i++) {
            final MediaPlayer mediaPlayer = new MediaPlayer();
            final VideoSurfaceView surfaceView = mVideoSurfaceView[i];
            final String dataSource = dataSources[i];
            try {
                mediaPlayer.setDataSource(dataSource);
                // the video view will take care of calling prepare and attaching the surface once
                // it becomes available
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mediaPlayer.start();
                        surfaceView.setVideoAspectRatio((float) mediaPlayer.getVideoWidth() /
                                (float) mediaPlayer.getVideoHeight());
                    }
                });
                surfaceView.setMediaPlayer(mediaPlayer);
            } catch (IOException e) {
                e.printStackTrace();
                mediaPlayer.release();
            }
        }

        // Draw a smooth background gradient that is always changing
        getWindow().getDecorView().setBackgroundDrawable(new WickedGradientDrawable());

        // Animate the top surface up and down so we're sure animations work
        mVideoSurfaceView[0].animate()
                .translationY(600f)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) { }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        final float targetY = mVideoSurfaceView[0].getTranslationY() == 0 ?
                                600f : 0;
                        mVideoSurfaceView[0].animate()
                                .translationY(targetY)
                                .setDuration(1999)
                                .setListener(this)
                                .start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) { }

                    @Override
                    public void onAnimationRepeat(Animator animation) { }
                })
                .start();
    }
}
