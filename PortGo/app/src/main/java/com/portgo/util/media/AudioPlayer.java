package com.portgo.util.media;

import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;


//
public class AudioPlayer {
    private static MediaPlayer mMediaPlayer;
    private static boolean isPause = false;//

    private MediaPlayer.OnCompletionListener mOnCompletionListener;

    public void playSound(String filePath) {

        if(mMediaPlayer == null){

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new OnErrorListener() {

                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mMediaPlayer.reset();
                    return false;
                }
            });
        }else{
            mMediaPlayer.reset();
        }

        try {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(filePath);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    long playItemid = -1;
    public boolean playSound(String filePath,long id,MediaPlayer.OnCompletionListener onCompletionListener) {

        if(mMediaPlayer == null){
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new OnErrorListener() {

                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mMediaPlayer.reset();
                    return false;
                }
            });
        }else{
            if(mMediaPlayer.isPlaying()){
                mMediaPlayer.reset();
                if(id == playItemid) {
                    playItemid = -1;
                    return false;
                }
            }else{
                playItemid = id;
                mMediaPlayer.reset();
            }
        }

        this.mOnCompletionListener = onCompletionListener;
        try {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(filePath);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void pause(){

        if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
            isPause = true;
        }

    }

    public void reset(){

        if(mMediaPlayer != null && isPause){

            mMediaPlayer.start();
            isPause = false;
        }
    }

    public void release(){
        playItemid = -1;
        if(mMediaPlayer != null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

}