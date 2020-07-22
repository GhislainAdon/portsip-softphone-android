package com.portgo.ui;
//

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.app.ActivityCompat;
import android.text.format.DateFormat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.R;


import java.io.File;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

public class PortActivityVideoPlayer extends Activity implements MediaPlayer.OnBufferingUpdateListener,
        SurfaceHolder.Callback, MediaPlayer.OnPreparedListener,View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private SurfaceView mSurfaceview;
    private MediaPlayer mediaPlayer;
    private String path;
    public static final String PLAYER_NAME  = "fdsafasa";
    private TextView player_currenttime,player_totaltime;
    private SeekBar player_seek;
    Calendar calendar=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_video_player);
        String fileName = getIntent().getStringExtra(PLAYER_NAME);

        File file = new File(fileName);

        calendar= Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
        mSurfaceview = (SurfaceView) findViewById(R.id.video_player_surfaceview);
        mSurfaceview.getHolder().addCallback(this);

        player_currenttime =  findViewById(R.id.video_player_current_time);
        player_totaltime =  findViewById(R.id.video_player_total_time);
        player_seek =  findViewById(R.id.video_player_seek);

        findViewById(R.id.video_player_back).setOnClickListener(this);
        player_seek.setOnSeekBarChangeListener(this);
        findViewById(R.id.video_player_surfaceview).setVisibility(View.VISIBLE);
        findViewById(R.id.video_player_tools).setVisibility(View.VISIBLE);
        if(file.exists()) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setLooping(true);
            path = file.getAbsolutePath();

        }else{
            Toast.makeText(this,getText(R.string.unknow_message_format),Toast.LENGTH_SHORT);
            finish();
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            handler.removeCallbacks(progressRefresher);
        }else {
            mediaPlayer.start();
            handler.postDelayed(progressRefresher,300);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.video_player_back:
                this.finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressRefresher);
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
        mediaPlayer.release();
        ActivityCompat.finishAfterTransition(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(mediaPlayer!=null) {
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
            }

            try {
                mediaPlayer.setDataSource(path);
                mediaPlayer.setDisplay(holder);
                mediaPlayer.prepareAsync();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setOnPreparedListener(this);
            } catch (IOException e) {
                e.printStackTrace();
                finish();
            }

            handler.postDelayed(progressRefresher,300);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        int duration = mp.getDuration();
        player_seek.setMax(duration);
        player_seek.setProgress(0);
        mp.start();
        calendar.setTimeInMillis(duration);
        String strTime = ""+ DateFormat.format("HH:mm:ss",calendar);
        player_totaltime.setText(strTime);

        if(handler!=null){
            handler.postDelayed(progressRefresher,100);
        }
    }

    Handler handler = new Handler();
    Runnable progressRefresher = new Runnable(){

        @Override
        public void run() {
            handler.postDelayed(this,300);
            if(inseek==true) {
                return;
            }else {
                if(mediaPlayer!=null&&mediaPlayer.isPlaying()) {
                    int position  = mediaPlayer.getCurrentPosition();
                    calendar.setTimeInMillis(position);
                    String strTime = ""+ DateFormat.format("HH:mm:ss",calendar);
                    player_currenttime.setText(strTime);
                    player_seek.setProgress(position);
                }
            }
        }
    };

    Runnable animationFinish = new Runnable(){

        @Override
        public void run() {
           findViewById(R.id.video_player_surfaceview).setVisibility(View.VISIBLE);
            findViewById(R.id.video_player_tools).setVisibility(View.VISIBLE);

        }
    };
    boolean inseek = false;
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

        inseek = true;

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if(mediaPlayer!=null){
            mediaPlayer.seekTo(seekBar.getProgress());
        }
        inseek = false;
    }
}