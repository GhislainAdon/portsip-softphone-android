package com.portgo.view;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;


import java.util.Calendar;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * Created by huacai on 2017/4/26.
 */
public class TextViewClock  extends AppCompatTextView {

     Calendar mCalendar;
    @SuppressWarnings("FieldCanBeLocal") // We must keep a reference to this observer
    private FormatChangeObserver mFormatChangeObserver;

    private Runnable mTicker;
    private Handler mHandler;

    private boolean mTickerStopped = false;

    private String mFormat="HH:mm:ss";

    public TextViewClock(Context context) {
        super(context);
        initClock();
    }

    public TextViewClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        initClock();
    }

    private void initClock() {
        if (mCalendar == null) {
            mCalendar = Calendar.getInstance();
        }

        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);

        setFormat();
    }

    @Override
    protected void onAttachedToWindow() {
        mTickerStopped = false;
        super.onAttachedToWindow();
        mHandler = new Handler();

        /**
         * requests a tick on the next hard-second boundary
         */
        mTicker = new Runnable() {
            synchronized  public void run() {
                if (mTickerStopped) return;
//                mCalendar.setTimeInMillis(System.currentTimeMillis());//
                mCalendar.add(Calendar.SECOND,1);
                setText(DateFormat.format(mFormat, mCalendar));
                invalidate();
                long now = SystemClock.uptimeMillis();
                long next = now + (1000 - now % 1000);
                mHandler.postAtTime(mTicker, next);
            }
        };
        mTicker.run();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mTickerStopped = true;
    }

    private void setFormat() {
        mFormat = mFormat;
    }

    boolean calendarChanger = false;
    public void setCurrentTime(Calendar calendar){
//        calendarChanger = true;
        mCalendar = calendar;
        if(mHandler!=null&&mTicker!=null) {
            mHandler.removeCallbacks(mTicker);
            mTicker.run();
        }
    }
    synchronized  public void setFormat(String format){
        this.mFormat = format;
    }
    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            setFormat();
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(TextViewClock.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(TextViewClock.class.getName());
    }
}
