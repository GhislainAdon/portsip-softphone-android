package com.portgo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.portgo.BuildConfig;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zhaocheng on 2016/11/3.
 */

public class RecordView extends View {

    private static final int DEFAULT_MIN_WIDTH = 500;
    public final static int MODEL_PLAY = 2;
    public final static int MODEL_RECORD = 1;

    private Context mContext;
    private Paint mPaint;
    private final String TAG = "RecordView";
    private double r;

    private long lastTime = 0;
    private int lineSpeed = 100;
    private float translateX = 0;

    final int DEFAULT_VOLUME = 10;
    private int model = MODEL_RECORD;

    private float amplitude = 1;
    private float volume = DEFAULT_VOLUME;
    private int fineness = 10;
    private float targetVolume = 0.1f;
    private float maxVolume = 100;
    private boolean isSet = false;
    private int sensibility = 4;
    private boolean canSetVolume = true;

    private boolean start = false;
    Handler mHandler = new Handler();
    Runnable refresher = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(this,20);
            RecordView.this.invalidate();
        }
    };

    private ArrayList<Path> paths;
    private String unit;

    public RecordView(Context context) {
        this(context,null);
    }

    public RecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initAtts();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);//
        mPaint.setStyle(Paint.Style.STROKE);
    }

    private void initAtts(){

        paths = new ArrayList<>(20);
        for (int i = 0; i <20; i++) {
            paths.add(new Path());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec));
    }
    private int measure(int origin) {
        int result = DEFAULT_MIN_WIDTH;
        int specMode = MeasureSpec.getMode(origin);
        int specSize = MeasureSpec.getSize(origin);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(model == MODEL_RECORD){
            drawVoiceLine(canvas);
        }else{
            drawVoiceLine2(canvas);
        }

    }

    private void drawVoiceLine(Canvas canvas) {

        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(2);
        canvas.save();
        int moveY = getHeight()/2; //baseline
        if(start){
            lineChange();
            for (int i = 0; i < paths.size(); i++) {
                paths.get(i).reset();
                paths.get(i).moveTo(getWidth()*5/6, moveY);
            }

            for (float j = getWidth()*5/6 - 1; j >= getWidth()/6; j -= fineness) {
                float i = j-getWidth()/6;
                amplitude = 5 * volume *i / getWidth() - 5 * volume * i / getWidth() * i/getWidth()*6/4;
                for (int n = 1; n <= paths.size(); n++) {
                    float sin = amplitude * (float) Math.sin((i - Math.pow(1.22, n)) * Math.PI / 180 - translateX);
                    paths.get(n - 1).lineTo(j, (2 * n * sin / paths.size() - 15 * sin / paths.size() + moveY));
                }
            }
            for (int n = 0; n < paths.size(); n++) {
                if (n == paths.size() - 1) {
                    mPaint.setAlpha(255);
                } else {
                    mPaint.setAlpha(n * 130 / paths.size());
                }
                if (mPaint.getAlpha() > 0) {
                    canvas.drawPath(paths.get(n), mPaint);
                }
            }
        }else {
            canvas.drawLine(getWidth()/6,moveY,getWidth()*5/6,moveY,mPaint);
        }
        canvas.restore();
    }

    private void drawVoiceLine2(Canvas canvas) {

        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
        canvas.save();
        int moveY = getHeight() / 2;
        int pandY = getWidth()/12;
        if(start) {
            lineChange();
            for (int i = 0; i < paths.size(); i++) {
                paths.get(i).reset();
                paths.get(i).moveTo(getWidth() - pandY, getHeight() / 2);
            }
            for (float j = getWidth() * 11 / 12 - 1; j >= getWidth() / 12; j -= fineness) {
                float i = j - getWidth() / 12;
                amplitude = 4 * volume * i / getWidth() - 4 * volume * i / getWidth() * i / getWidth() * 12 / 10;
                for (int n = 1; n <= paths.size(); n++) {
                    float sin = amplitude * (float) Math.sin((i - Math.pow(1.22, n)) * Math.PI / 180 - translateX);
                    paths.get(n - 1).lineTo(j, (2 * n * sin / paths.size() - 15 * sin / paths.size() + moveY));
                }
            }
        }

        canvas.restore();
    }

    public void start(){

        canSetVolume = true;
        start =true;
        mHandler.postDelayed(refresher,20);
    }

    private void lineChange() {
        if (lastTime == 0) {
            lastTime = System.currentTimeMillis();
            translateX += 5;
        } else {
            if (System.currentTimeMillis() - lastTime > lineSpeed) {
                lastTime = System.currentTimeMillis();
                translateX += 5;
            } else {
                return;
            }
        }
        if (volume < targetVolume*getHeight()) {
            volume += getHeight() / 10;
        }
    }

    public void setVolume(int volume) {
        if(volume >100)
            volume = volume/100;
        volume = volume*2/5;
        if(!canSetVolume)
            return;
        if (volume > maxVolume * sensibility / 30) {
            isSet = true;
            this.targetVolume =  volume / 3 / maxVolume;
        }
    }


    public void cancel(){
        canSetVolume = false;
        lastTime  =0;
        start =false;
        mHandler.removeCallbacks(refresher);
        volume = DEFAULT_VOLUME;
        targetVolume = 1;
        postInvalidate();
    }
}
