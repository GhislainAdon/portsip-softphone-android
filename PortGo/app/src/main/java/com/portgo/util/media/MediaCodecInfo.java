package com.portgo.util.media;

import android.media.MediaExtractor;
import android.media.MediaPlayer;
import android.text.format.DateFormat;

import com.portgo.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by cj on 2017/7/11.
 */

public class MediaCodecInfo {
    public String path;
    public MediaExtractor extractor;
    public int cutPoint;
    public int cutDuration;
    public int duration;

    static  public  int getFilePlayTime(File file) {
        int duration = 0;
        MediaPlayer player = new MediaPlayer();
        try {
            if(file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                player.setDataSource(fis.getFD());
                player.prepare();
                duration =  player.getDuration();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        player.release();
        return duration;
    }

}
