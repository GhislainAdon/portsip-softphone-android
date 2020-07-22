/* Copyright (C) 2010-2011, Mamadou Diop.
*  Copyright (C) 2011, Doubango Telecom.
*
* Contact: Mamadou Diop <diopmamadou(at)doubango(dot)org>
*	
* This file is part of imsdroid Project (http://code.google.com/p/imsdroid)
*
* imsdroid is free software: you can redistribute it and/or modify it under the terms of 
* the GNU General Public License as published by the Free Software Foundation, either version 3 
* of the License, or (at your option) any later version.
*	
* imsdroid is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU General Public License for more details.
*	
* You should have received a copy of the GNU General Public License along 
* with this program; if not, write to the Free Software Foundation, Inc., 
* 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package com.portgo.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;

import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.portgo.R;
import com.portgo.manager.ConfigurationManager;
import com.portgo.util.SelectableObject;
import com.portgo.view.RecordView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

//
public class PortActivityRecordPlayer extends PortGoBaseActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener, MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,MediaPlayer.OnCompletionListener,
        SeekBar.OnSeekBarChangeListener {
    ArrayList<String> recordFiles = new ArrayList<>();

    public MediaPlayer mediaPlayer = new  MediaPlayer();
    private SeekBar mSeekBar =null;
    String fileName = null;
    int index = 0;
    Calendar calendar=null;
    TextView currentTime =null;
    TextView totalTime =null;
    Handler handler = new Handler();
    static final String PLAY_SET="play_set";
    static final String PLAY_ITEM= "item" ;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordplayer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        calendar= Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
        String dirName = getString(R.string.prefrence_record_filepath_default);
        String defaultdir = getExternalFilesDir(dirName).getAbsolutePath();
        findViewById(R.id.play_pre).setOnClickListener(this);
        findViewById(R.id.play_pause).setOnClickListener(this);
        findViewById(R.id.play_next).setOnClickListener(this);

        currentTime = (TextView) findViewById(R.id.record_play_current_time);
        totalTime= (TextView) findViewById(R.id.record_play_total_time);

        mSeekBar = (SeekBar) findViewById(R.id.play_seek);

        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);

        if(toolbar!=null) {
            toolbar.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            toolbar.setTitleTextAppearance(this,R.style.ToolBarWhiteTextAppearance);
            toolbar.setNavigationIcon(R.drawable.record_back_ico);
            toolbar.setTitleMarginStart(0);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recordFiles = (ArrayList<String>) getIntent().getStringArrayListExtra(PLAY_SET);
        index = getIntent().getIntExtra(PLAY_ITEM,0);
        if(recordFiles.size()>index&&index>=0){
            fileName = recordFiles.get(index);
            playMediaFile(mediaPlayer,fileName);
            refreshPlayController();
        }else {
            this.finish();
            Toast.makeText(this,R.string.record_cannot_find,Toast.LENGTH_LONG).show();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_pre:
                if(waveView!=null) {
                    waveView.cancel();
                }
                onPreClick();

                break;
            case R.id.play_pause:
                onPlayClick();

                break;
            case R.id.play_next:
                if(waveView!=null) {
                    waveView.cancel();
                }
                handler.removeCallbacks(progressRefresher);
                onNextClick();
                break;
        }
        refreshPlayController();
        return;
    }

    void onNextClick(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        if(index<recordFiles.size()){
            String file = recordFiles.get(++index);
            playMediaFile(mediaPlayer,file);
        }

    }

    void refreshPlayController(){
        View play_pre = findViewById(R.id.play_pre);
        ImageView play_pause = (ImageView) findViewById(R.id.play_pause);
        View play_next = findViewById(R.id.play_next);
        play_pre.setEnabled(true);
        play_pause.setEnabled(true);
        play_next.setEnabled(true);

        if(recordFiles.size()==0){
            play_pre.setEnabled(false);
            play_pause.setEnabled(false);
            play_next.setEnabled(false);
        }else {
            if (index == 0) {
                play_pre.setEnabled(false);
            }
            if (index == recordFiles.size() - 1) {
                play_next.setEnabled(false);
            }

            if (mediaPlayer.isPlaying()) {
                play_pause.setImageResource(R.drawable.record_pause_ico);
            } else {
                play_pause.setImageResource(R.drawable.record_play_ico);
            }
        }
    }
    void onPreClick(){
        if(mediaPlayer.isPlaying()){
            handler.removeCallbacks(progressRefresher);
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        if(index>0){
            String file = recordFiles.get(--index);
            playMediaFile(mediaPlayer,file);
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
        mediaPlayer= null;
    }

    boolean playMediaFile(MediaPlayer mp, String filepath){
        if(mp!=null&&!TextUtils.isEmpty(filepath)){
            try {
                if(!TextUtils.isEmpty(filepath)) {
                    Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
                    String[] path = filepath.split(File.separator);
                    toolbar.setTitle(path[path.length-1]);
                }

                File file = new File(filepath);
                FileInputStream fis = new FileInputStream(file);
                mp.setDataSource(fis.getFD());
                mp.prepare();
                mSeekBar.setMax(mp.getDuration());
                mSeekBar.setOnSeekBarChangeListener(this);
                mp.start();

            } catch (IOException e) {
                mp.reset();
                Toast.makeText(this,R.string.can_not_openfile, Toast.LENGTH_SHORT).show();
                if(waveView!=null){
                    waveView.cancel();
                }
                return false;
            }
        }
        return true;
    }



    void onPlayClick(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            if(waveView!=null) {
                waveView.cancel();
            }
            if(handler!=null){
                handler.removeCallbacks(progressRefresher);
            }
        }else{
            mediaPlayer.start();
            if(waveView!=null&&mediaPlayer.isPlaying()) {
                waveView.start();
                waveView.setVolume(100);
                if(handler!=null){
                    handler.postDelayed(progressRefresher,100);
                }
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                setResult(Activity.RESULT_CANCELED);
                this.finish();
                break;
            case R.id.menu_finish:
                this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
    }



    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    boolean inseek = false;
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

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        handler.removeCallbacks(progressRefresher);
        return false;
    }
    private static final float VISUALIZER_HEIGHT_DIP = 160f;
    RecordView waveView= null;
    @Override
    public void onPrepared(MediaPlayer player) {

        if(handler!=null){
            handler.postDelayed(progressRefresher,100);
        }

        if(waveView==null) {
            waveView = new RecordView(this);
            waveView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            ((LinearLayout) findViewById(R.id.record_play_wave)).addView(waveView);

        }
        waveView.start();
        waveView.setVolume(100);

        final int maxCR = Visualizer.getMaxCaptureRate();

        calendar.setTimeInMillis(0);
        String strTime = ""+ DateFormat.format("HH:mm:ss",calendar);
        currentTime.setText(strTime);

        calendar.setTimeInMillis(player.getDuration());
        strTime = ""+ DateFormat.format("HH:mm:ss",calendar);
        totalTime.setText(strTime);


        handler.postDelayed(progressRefresher,100);

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(waveView!=null) {
            waveView.cancel();
        }
        mediaPlayer.seekTo(0);
        refreshPlayController();

        handler.removeCallbacks(progressRefresher);
        mSeekBar.setProgress(0);
        currentTime.setText("00:00:00");
    }

    Runnable progressRefresher = new Runnable(){

        @Override
        public void run() {
            handler.postDelayed(this,100);
            if(inseek==true) {
                return;
            }else {
                if(mediaPlayer!=null&&mediaPlayer.isPlaying()) {
                    int position  = mediaPlayer.getCurrentPosition();
                    calendar.setTimeInMillis(position);
                    String strTime = ""+ DateFormat.format("HH:mm:ss",calendar);
                    currentTime.setText(strTime);
                    mSeekBar.setProgress(position);
                }
            }
        }
    };
}
